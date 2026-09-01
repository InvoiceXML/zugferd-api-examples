# Embed an existing UN/CEFACT CII XML into an existing PDF, producing a
# ZUGFeRD 2.x compliant PDF/A-3 hybrid invoice, using the InvoiceXML API.
#
# Your PDF keeps its exact layout, fonts, and branding; the API promotes the
# container to PDF/A-3 and attaches the XML as factur-x.xml (the attachment
# name the ZUGFeRD specification prescribes since 2.2). The XML is validated
# with the full /v1/validate/zugferd rule set first, so a non-compliant
# invoice never leaves the API.
#
# Get API key: https://www.invoicexml.com/account/authentication
# Docs:        https://www.invoicexml.com/docs/api/embed/zugferd
#
# Ruby 3.x, standard library only. Set the INVOICEXML_API_KEY
# environment variable to the raw key, without the "Bearer " prefix.
#
# Usage: ruby embed.rb [path/to/invoice.pdf] [path/to/factur-x.xml]

require "net/http"
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

uri = URI("#{BASE_URL}/v1/embed/zugferd")
request = Net::HTTP::Post.new(uri)
request["Authorization"] = "Bearer #{API_KEY}"

# Two file parts in one multipart request: "pdf" is the visual layer,
# "xml" is the CII payload. skipValidation is optional: "true" packages the
# XML without EN 16931 business-rule checks, structural checks still run.
response = File.open(pdf_path, "rb") do |pdf|
  File.open(xml_path, "rb") do |xml|
    request.set_form(
      [["pdf", pdf], ["xml", xml], ["skipValidation", "false"]],
      "multipart/form-data"
    )
    api_http(uri).request(request)
  end
end

unless response.is_a?(Net::HTTPSuccess)
  # 400 with errorCode 4001 lists every failed business rule;
  # 4017 means BT-24 is not an official ZUGFeRD profile URN.
  warn "InvoiceXML API error #{response.code}: #{response.body}"
  exit 1
end

File.binwrite("invoice-zugferd.pdf", response.body)
puts "Saved invoice-zugferd.pdf (#{response.body.bytesize} bytes)"
