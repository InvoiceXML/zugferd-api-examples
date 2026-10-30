# Embed a CII XML into your own PDF and email the result, in one call.
# Builds the hybrid PDF exactly like POST /v1/embed/zugferd, always with
# full validation, then sends it as an attachment from your own domain.
# This sends a real email. The answer is the message id, not the file.
#
# Before the first run, add and verify your domain:
# https://www.invoicexml.com/account/sending-domains
# Your plan must include email delivery: https://www.invoicexml.com/pricing
#
# Get API key: https://www.invoicexml.com/account/authentication
# Docs:        https://www.invoicexml.com/docs/api/embed/zugferd/email
#
# Ruby 3.x, standard library only. Set the INVOICEXML_API_KEY
# environment variable to the raw key, without the "Bearer " prefix.
#
# Usage: ruby send_embedded_email.rb [path/to/invoice.pdf] [path/to/factur-x.xml]

require "net/http"
require "json"
require "uri"

BASE_URL = "https://api.invoicexml.com"
API_KEY  = ENV.fetch("INVOICEXML_API_KEY")

def api_http(uri)
  http = Net::HTTP.new(uri.host, uri.port)
  http.use_ssl = true
  http.open_timeout = 10
  http.read_timeout = 120
  http
end

pdf_path = ARGV.fetch(0, "invoice.pdf")
xml_path = ARGV.fetch(1, "factur-x.xml")

uri = URI("#{BASE_URL}/v1/embed/zugferd/email")
request = Net::HTTP::Post.new(uri)
request["Authorization"] = "Bearer #{API_KEY}"

# Two file parts in one multipart request: "pdf" is the visual layer,
# "xml" is the CII payload. The email goes in plain form fields.
# Optional: email.cc and email.bcc (repeat the field, 2 addresses together) and email.replyTo.
response = File.open(pdf_path, "rb") do |pdf|
  File.open(xml_path, "rb") do |xml|
    request.set_form(
      [
        ["pdf", pdf],
        ["xml", xml],
        ["email.from", "invoices@yourcompany.com"], # any address on your verified domain
        ["email.fromName", "Acme Billing"],
        ["email.to", "you@yourcompany.com"], # send the first test to yourself
        ["email.subject", "Invoice MIN-001"],
        ["email.message", "Hello,\n\nPlease find attached invoice MIN-001.\n\nKind regards"]
      ],
      "multipart/form-data"
    )
    api_http(uri).request(request)
  end
end

unless response.is_a?(Net::HTTPSuccess)
  # 400 with errorCode 4001 lists every failed business rule.
  warn "InvoiceXML API error #{response.code}: #{response.body}"
  exit 1
end

puts "Email accepted for delivery, message id #{JSON.parse(response.body)["messageId"]}"
