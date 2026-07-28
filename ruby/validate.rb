# Validate a ZUGFeRD PDF using the InvoiceXML API.
# The endpoint extracts the embedded XML, detects the declared profile from
# BT-24, and runs the matching XSD plus Schematron rules.
# Uploads the file as multipart/form-data via Net::HTTP's built-in set_form.
#
# Get API key: https://www.invoicexml.com/account/authentication
# Docs:        https://www.invoicexml.com/docs/api/validate/zugferd
#
# Ruby 3.x, standard library only. Set the INVOICEXML_API_KEY
# environment variable to the raw key, without the "Bearer " prefix.
#
# Usage: ruby validate.rb [path/to/invoice.pdf]

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

def validate_zugferd(path)
  uri = URI("#{BASE_URL}/v1/validate/zugferd")
  request = Net::HTTP::Post.new(uri)
  request["Authorization"] = "Bearer #{API_KEY}"

  File.open(path, "rb") do |file|
    request.set_form([["file", file]], "multipart/form-data")
    response = api_http(uri).request(request)

    unless response.is_a?(Net::HTTPSuccess)
      warn "InvoiceXML API error #{response.code}: #{response.body}"
      exit 1
    end

    JSON.parse(response.body)
  end
end

report = validate_zugferd(ARGV.fetch(0, "invoice-zugferd.pdf"))

# A finished validation always answers HTTP 200; the verdict lives in "valid".
if report["valid"]
  puts "Valid ZUGFeRD, profile #{report.dig("data", "profile")}"
else
  report["errors"].each do |finding|
    puts "[#{finding["rule"]}] #{finding["message"]}"
    puts "  fields: #{Array(finding["fields"]).join(", ")}"
  end
  exit 1
end
