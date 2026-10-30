# Create a ZUGFeRD PDF/A-3 invoice and email it with the InvoiceXML API, in one call.
# Builds and validates the invoice exactly like POST /v1/create/zugferd, then
# sends it as an attachment from your own domain. This sends a real email.
# The answer is the message id of the email, not the file.
#
# Before the first run, add and verify your domain:
# https://www.invoicexml.com/account/sending-domains
# Your plan must include email delivery: https://www.invoicexml.com/pricing
#
# Get API key: https://www.invoicexml.com/account/authentication
# Docs:        https://www.invoicexml.com/docs/api/create/zugferd/email
#
# Ruby 3.x, standard library only. Set the INVOICEXML_API_KEY
# environment variable to the raw key, without the "Bearer " prefix.

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

# Totals and the VAT breakdown are calculated from the line items.
invoice = {
  invoice: {
    invoiceNumber: "MIN-001",
    issueDate: "2026-05-18",
    currency: "EUR",
    seller: {
      name: "Acme",
      vatIdentifier: "DE123456789",
      legalRegistration: { identifier: "HRB 12345" },
      postalAddress: {
        line1: "Hauptstraße 12",
        city: "Berlin",
        postCode: "10115",
        country: "DE"
      }
    },
    buyer: {
      name: "Globex SAS",
      postalAddress: {
        line1: "15 rue de Rivoli",
        city: "Paris",
        postCode: "75001",
        country: "FR"
      }
    },
    paymentDetails: { paymentAccountIdentifier: "DE89370400440532013000" },
    lines: [
      {
        quantity: 10,
        priceDetails: { netPrice: 150.00 },
        vatInformation: { rate: 19.00 },
        item: { name: "Senior consulting" }
      }
    ]
  },
  email: {
    from: "invoices@yourcompany.com", # any address on your verified domain
    fromName: "Acme Billing",
    to: "you@yourcompany.com", # send the first test to yourself
    subject: "Invoice MIN-001",
    message: "Hello,\n\nPlease find attached invoice MIN-001.\n\nKind regards"
    # Optional: cc and bcc (2 addresses together) and replyTo.
  }
}

uri = URI("#{BASE_URL}/v1/create/zugferd/email")
request = Net::HTTP::Post.new(uri)
request["Authorization"] = "Bearer #{API_KEY}"
request["Content-Type"]  = "application/json"
request.body = JSON.generate(invoice)

response = api_http(uri).request(request)

unless response.is_a?(Net::HTTPSuccess)
  warn "InvoiceXML API error #{response.code}: #{response.body}"
  exit 1
end

puts "Email accepted for delivery, message id #{JSON.parse(response.body)["messageId"]}"
