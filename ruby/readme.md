# ZUGFeRD for Ruby: InvoiceXML API Examples

Ruby code samples for creating, validating, and extracting **ZUGFeRD** electronic invoices using the **[InvoiceXML API](https://www.invoicexml.com)**. Compatible with **Ruby 3.0 and later**, using only `Net::HTTP` from the standard library. Runs in Rails, Sinatra, Hanami, Sidekiq jobs, Rake tasks, or plain scripts. **Zero gems.**

For background on the ZUGFeRD standard itself (what it is, profiles, the German mandate), see the [main repository README](../README.md). A full walkthrough with Rails patterns, including XRechnung for the public sector, is in the guide to [ZUGFeRD and XRechnung in Ruby](https://www.invoicexml.com/blog/zugferd-xrechnung-ruby) on invoicexml.com.

## Get your API key

Every example in this folder calls the InvoiceXML REST API. Sign up and generate a free API key here:

**→ [https://www.invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication)**

Pass it as a Bearer token on every request:

```
Authorization: Bearer YOUR_API_KEY
```

The examples read the key from the `INVOICEXML_API_KEY` environment variable:

```bash
export INVOICEXML_API_KEY=YOUR_API_KEY
```

Or on Windows (PowerShell):

```powershell
$env:INVOICEXML_API_KEY = "YOUR_API_KEY"
```

**Important:** set the variable to the raw key only, without the `Bearer ` prefix. If your account page shows the full header value (e.g. `Bearer ixml_a1b2c3...`), copy only the part after `Bearer `. The code adds the prefix itself when building the `Authorization` header.

## Requirements

- **Ruby 3.0 or later** (any currently supported Ruby)
- No gems, no Gemfile needed

The examples use `Net::HTTP`, `JSON`, and `URI` from the standard library. Ruby has a small advantage here over most languages: `Net::HTTP` supports multipart uploads natively via `request.set_form`, so no multipart helper gem is needed. In a Rails app you can swap in Faraday later without changing the shape of the calls.

There is no ZUGFeRD gem to install because no maintained one exists. The hits on RubyGems date from the ZUGFeRD 1.0 era and are dormant, and a from-scratch build runs into two walls: PDF/A-3 with an embedded attachment and exact XMP metadata (Prawn does not produce PDF/A; HexaPDF can but is AGPL-licensed for commercial use), and validation, where the official Schematron compiles to XSLT 2.0 while Ruby's Nokogiri wraps libxslt and speaks XSLT 1.0 only. Both live server-side here.

## Files in this folder

| File | Operation | API endpoint |
|---|---|---|
| [`create.rb`](./create.rb) | Build a ZUGFeRD PDF/A-3 invoice with embedded EN 16931 XML | [`POST /v1/create/zugferd`](https://www.invoicexml.com/docs/api/create/zugferd) |
| [`validate.rb`](./validate.rb) | Validate a ZUGFeRD file against schematron rules | [`POST /v1/validate/zugferd`](https://www.invoicexml.com/docs/api/validate/zugferd) |
| [`extract_json.rb`](./extract_json.rb) | Extract ZUGFeRD invoice data as JSON (deterministic, no AI) | [`POST /v1/extract/json`](https://www.invoicexml.com/docs/api/extract/json) |
| [`extract_xml.rb`](./extract_xml.rb) | Extract the raw `factur-x.xml` from a ZUGFeRD PDF | [`POST /v1/extract/xml`](https://www.invoicexml.com/docs/api/extract/xml) |
| [`parse_json.rb`](./parse_json.rb) | Parse an old-school invoice PDF (scan, photo) with AI, with confidence scores | [`POST /v1/parse/json`](https://www.invoicexml.com/docs/api/parse/json) |
| [`extract_attachments.rb`](./extract_attachments.rb) | Extract embedded supporting documents (BG-24) as a ZIP | [`POST /v1/extract/attachments`](https://www.invoicexml.com/docs/api/extract/attachments) |

Each file is standalone and runnable with `ruby create.rb`. Set `INVOICEXML_API_KEY` in your environment and execute. The files that take an input document default to `invoice-zugferd.pdf` (the output of `create.rb`), or accept a path as the first argument.

> **Note on the snippets below:** they are excerpts from those files and assume `BASE_URL`, `API_KEY`, and the `api_http` helper are already defined. When in doubt, copy the complete file.

All files share this small setup block:

```ruby
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
```

---

## Create a ZUGFeRD invoice in Ruby

The payload is a plain Ruby hash serialized with `JSON.generate`. Send net prices, quantities, and rates; totals and the per-rate VAT breakdown are calculated from the line items.

```ruby
invoice = {
  invoice: {
    invoiceNumber: "MIN-001",
    issueDate: "2026-05-18",
    currency: "EUR",
    seller: {
      name: "Acme",
      vatIdentifier: "DE123456789",
      legalRegistration: { identifier: "HRB 12345" },
      postalAddress: { line1: "Hauptstraße 12", city: "Berlin", postCode: "10115", country: "DE" }
    },
    buyer: {
      name: "Globex SAS",
      postalAddress: { line1: "15 rue de Rivoli", city: "Paris", postCode: "75001", country: "FR" }
    },
    paymentDetails: { paymentAccountIdentifier: "DE89370400440532013000" },
    lines: [
      { quantity: 10, priceDetails: { netPrice: 150.00 }, vatInformation: { rate: 19.00 }, item: { name: "Senior consulting" } }
    ]
  }
}

uri = URI("#{BASE_URL}/v1/create/zugferd")
request = Net::HTTP::Post.new(uri)
request["Authorization"] = "Bearer #{API_KEY}"
request["Content-Type"]  = "application/json"
request.body = JSON.generate(invoice)

response = api_http(uri).request(request)
raise response.body unless response.is_a?(Net::HTTPSuccess)

File.binwrite("invoice-zugferd.pdf", response.body)
```

The response is a complete ZUGFeRD document: PDF/A-3 container, embedded `factur-x.xml`, XMP metadata declaring the EN 16931 profile, checked against the official Schematron. If the data cannot produce a compliant invoice, the API answers HTTP 400 with the violated rules as structured findings, so an invalid document never leaves your system.

[Full example: `create.rb`](./create.rb) | [API reference](https://www.invoicexml.com/docs/api/create/zugferd)

---

## Validate a ZUGFeRD file in Ruby

Validate before invoices go out, and validate what suppliers send in. The endpoint extracts the embedded XML, detects the declared profile from BT-24, and runs the matching XSD plus Schematron rules. `Net::HTTP` does multipart natively through `set_form`, so the upload is three lines:

```ruby
def validate_zugferd(path)
  uri = URI("#{BASE_URL}/v1/validate/zugferd")
  request = Net::HTTP::Post.new(uri)
  request["Authorization"] = "Bearer #{API_KEY}"

  File.open(path, "rb") do |file|
    request.set_form([["file", file]], "multipart/form-data")
    JSON.parse(api_http(uri).request(request).body)
  end
end

report = validate_zugferd("invoice-zugferd.pdf")

if report["valid"]
  puts "Valid ZUGFeRD, profile #{report.dig("data", "profile")}"
else
  report["errors"].each do |finding|
    puts "[#{finding["rule"]}] #{finding["message"]}"
  end
end
```

Both valid and invalid invoices answer HTTP 200: the verdict lives in `valid`, and non-2xx statuses mean transport problems (bad key, unreadable upload). Findings carry the rule id, a plain-language message, business term codes, and field paths, ready to surface in a UI or forward to a supplier as-is. The same call belongs in your RSpec suite: validate a freshly generated invoice on every build and regressions in your payload mapping surface early.

[Full example: `validate.rb`](./validate.rb) | [API reference](https://www.invoicexml.com/docs/api/validate/zugferd)

---

## Extract ZUGFeRD data as JSON in Ruby

Incoming ZUGFeRD files already carry structured data, and the extract endpoint returns it parsed and normalized, ready for your models. Pure XML parsing, no AI: what the supplier declared is exactly what you import.

```ruby
def extract_invoice(path)
  uri = URI("#{BASE_URL}/v1/extract/json")
  request = Net::HTTP::Post.new(uri)
  request["Authorization"] = "Bearer #{API_KEY}"

  File.open(path, "rb") do |file|
    request.set_form([["file", file]], "multipart/form-data")
    JSON.parse(api_http(uri).request(request).body)
  end
end

# The invoice document sits under the "invoice" key of the response.
invoice = extract_invoice("invoice-zugferd.pdf")["invoice"]
puts "#{invoice["invoiceNumber"]} from #{invoice.dig("seller", "name")}"
puts invoice.dig("totals", "grandTotalAmount")  # BT-112
```

[Full example: `extract_json.rb`](./extract_json.rb) | [API reference](https://www.invoicexml.com/docs/api/extract/json) | [Sample response](../json/extract-json-response.json)

---

## Extract embedded XML from a ZUGFeRD PDF in Ruby

Returns the raw `factur-x.xml` payload (UN/CEFACT Cross-Industry Invoice syntax) as `application/xml`. Use this when you need the structured XML to feed an existing CII pipeline, EDI partner, or archival system, or when you want to walk the tree with Nokogiri yourself.

[Full example: `extract_xml.rb`](./extract_xml.rb) | [API reference](https://www.invoicexml.com/docs/api/extract/xml)

---

## Parse old-school invoice PDFs with AI in Ruby

Receiving is the half of the German mandate that arrived first: since January 2025 you cannot refuse a compliant e-invoice, but suppliers will keep sending ordinary PDFs for years. For typed, scanned, or photographed PDFs with no embedded XML, `POST /v1/parse/json` reads the document with AI and returns the same `InvoiceDocument` shape as the extract endpoint, plus a `confidence` object: an overall score and four area scores (seller identification, buyer identification, tax calculation, line items), each from 0.0 to 1.0.

The two endpoints compose into one intake method: try the deterministic route first, and fall back to AI only when the API reports error code 4006 (no embedded XML). `parse_json.rb` implements exactly that:

```ruby
def read_incoming_invoice(path)
  uri = URI("#{BASE_URL}/v1/extract/json")
  request = Net::HTTP::Post.new(uri)
  request["Authorization"] = "Bearer #{API_KEY}"

  response = File.open(path, "rb") do |file|
    request.set_form([["file", file]], "multipart/form-data")
    api_http(uri).request(request)
  end

  if response.is_a?(Net::HTTPSuccess)
    return { "source" => "embedded-xml" }.merge(JSON.parse(response.body))
  end

  error = JSON.parse(response.body)
  raise response.body unless error["errorCode"] == 4006

  # No embedded XML: an old-school PDF, hand it to the AI parser
  { "source" => "ai" }.merge(parse_invoice_pdf(path))
end
```

Automate the confident reads, route the shaky ones to a human. Because the `invoice` hash matches the `/v1/create/zugferd` request body, a parsed supplier invoice can even be piped back into invoice creation.

[Full example: `parse_json.rb`](./parse_json.rb) | [API reference](https://www.invoicexml.com/docs/api/parse/json)

---

## Extract embedded attachments in Ruby

E-invoices can carry their supporting documents inside the invoice data itself: delivery notes, timesheets, or the original order, embedded base64 in the EN 16931 attachment group (BG-24). `POST /v1/extract/attachments` collects every embedded payload and returns the original files as one ZIP. This is a different thing from `extract_xml.rb`, which pulls the `factur-x.xml` attachment out of the PDF container. An invoice without embedded attachments answers 404 rather than an empty archive, so the example treats that status as a normal outcome.

[Full example: `extract_attachments.rb`](./extract_attachments.rb) | [API reference](https://www.invoicexml.com/docs/api/extract/attachments)

---

## Rails integration

In a Rails app, promote the calls into a service object. Faraday with `faraday-multipart` is the idiomatic client, and Rails credentials keep the key out of environment-variable sprawl:

```ruby
# Gemfile
gem "faraday"
gem "faraday-multipart"

# app/services/zugferd_service.rb
class ZugferdService
  BASE_URL = "https://api.invoicexml.com"

  def create(invoice_attributes)
    response = connection.post("/v1/create/zugferd") do |req|
      req.headers["Content-Type"] = "application/json"
      req.body = JSON.generate(invoice: invoice_attributes)
    end
    raise ZugferdError, response.body unless response.success?
    response.body
  end

  def validate(pdf_bytes)
    part = Faraday::Multipart::FilePart.new(
      StringIO.new(pdf_bytes), "application/pdf", "invoice.pdf"
    )
    JSON.parse(connection.post("/v1/validate/zugferd", { file: part }).body)
  end

  private

  def connection
    @@connection ||= Faraday.new(url: BASE_URL) do |f|
      f.request :multipart
      f.headers["Authorization"] =
        "Bearer #{Rails.application.credentials.dig(:invoicexml, :api_key)}"
    end
  end
end

class ZugferdError < StandardError; end
```

Generation belongs in a background job (ActiveJob over Sidekiq or GoodJob), not a request cycle, and ActiveStorage is the natural home for the returned PDF:

```ruby
class GenerateZugferdJob < ApplicationJob
  queue_as :invoices
  retry_on ZugferdError, wait: :polynomially_longer, attempts: 3

  def perform(invoice_id)
    invoice = Invoice.find(invoice_id)
    pdf = ZugferdService.new.create(invoice.to_api_payload)

    invoice.document.attach(
      io: StringIO.new(pdf),
      filename: "#{invoice.number}-zugferd.pdf",
      content_type: "application/pdf"
    )
  end
end
```

One caution on the retry policy: retry transport failures and 5xx responses, but treat HTTP 400 as terminal. A 400 carries validation findings about your data, and resending the same payload produces the same findings. Log them, surface them, fix the mapping.

---

## Common issues

- **`KeyError: key not found: "INVOICEXML_API_KEY"`**: the environment variable is not set in the shell running the script. Export it first (see [Get your API key](#get-your-api-key)), and remember that `ENV.fetch` reads the environment of the current process, so set it in the same terminal session or in your process manager.
- **`HTTP 401 Unauthorized`**: API key missing or invalid. Generate one at [invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication) and confirm you are sending `Authorization: Bearer YOUR_API_KEY`. A frequent cause: setting the variable to the whole `Bearer xxx` value, which sends `Bearer Bearer xxx`. Set the raw key only.
- **`HTTP 400 Bad Request` on Create**: a required field is missing or malformed. Frequent causes: `issueDate` not in ISO format (`YYYY-MM-DD`), `currency` not in ISO 4217 (`EUR`, `USD`), country codes not in ISO 3166-1 alpha-2 (`DE`, `FR`).
- **Validate "fails" with HTTP 200**: not a failure. Valid and invalid documents both answer 200; branch on the `valid` flag in the JSON body. Non-2xx statuses mean transport problems, not rule violations.
- **A corrupt PDF on disk**: write binary responses with `File.binwrite`, never `File.write`. The examples do this throughout; `File.write` can transcode the bytes and break the PDF.
- **Multipart upload sends an empty file**: open the file in binary mode (`File.open(path, "rb")`) and call `set_form` inside the block, as the examples do, so the handle is still open when the request body is read.
- **`Net::ReadTimeout`**: AI parsing in particular can take 10 to 30 seconds for larger PDFs. The `api_http` helper sets `read_timeout = 120`; keep a generous value for `/v1/parse/json`.
- **Error code 4006 on Extract**: the PDF carries no embedded XML, so it is not a ZUGFeRD hybrid. That is the signal to fall back to `/v1/parse/json`, as `parse_json.rb` shows.
- **Schematron BR-CO-* failures on Validate**: line totals do not match the header total, or tax category and tax percentage are inconsistent. Recompute totals before posting.

---

## Resources

- [Get an InvoiceXML API key](https://www.invoicexml.com/account/authentication)
- [ZUGFeRD and XRechnung in Ruby: complete guide](https://www.invoicexml.com/blog/zugferd-xrechnung-ruby)
- [Create ZUGFeRD API reference](https://www.invoicexml.com/docs/api/create/zugferd)
- [Validate ZUGFeRD API reference](https://www.invoicexml.com/docs/api/validate/zugferd)
- [Extract JSON API reference](https://www.invoicexml.com/docs/api/extract/json)
- [Extract XML API reference](https://www.invoicexml.com/docs/api/extract/xml)
- [Parse JSON (AI) API reference](https://www.invoicexml.com/docs/api/parse/json)
- [Extract attachments API reference](https://www.invoicexml.com/docs/api/extract/attachments)
- [Ruby Net::HTTP documentation](https://docs.ruby-lang.org/en/master/Net/HTTP.html)
- [Main repository README](../README.md)
