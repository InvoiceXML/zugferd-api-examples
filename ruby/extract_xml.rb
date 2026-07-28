# Extract the raw factur-x.xml from a ZUGFeRD PDF using the InvoiceXML API.
# Returns the embedded CII document as application/xml, ready for Nokogiri
# or an existing EDI / archival pipeline.
#
# Get API key: https://www.invoicexml.com/account/authentication
# Docs:        https://www.invoicexml.com/docs/api/extract/xml
#
# Ruby 3.x, standard library only. Set the INVOICEXML_API_KEY
# environment variable to the raw key, without the "Bearer " prefix.
#
# Usage: ruby extract_xml.rb [path/to/invoice.pdf]

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

uri = URI("#{BASE_URL}/v1/extract/xml")
request = Net::HTTP::Post.new(uri)
request["Authorization"] = "Bearer #{API_KEY}"

response = File.open(ARGV.fetch(0, "invoice-zugferd.pdf"), "rb") do |file|
  request.set_form([["file", file]], "multipart/form-data")
  api_http(uri).request(request)
end

unless response.is_a?(Net::HTTPSuccess)
  warn "InvoiceXML API error #{response.code}: #{response.body}"
  exit 1
end

File.binwrite("factur-x.xml", response.body)
puts "Saved factur-x.xml (#{response.body.bytesize} bytes)"
