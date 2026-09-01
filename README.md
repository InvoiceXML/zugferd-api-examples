# ZUGFeRD REST API Examples: Create, Validate, and Extract ZUGFeRD Invoices

Open-source code examples for working with **ZUGFeRD**, the German hybrid e-invoicing standard combining a human-readable PDF/A-3 with embedded structured XML data. Compliant with **EN 16931**, technically identical to **Factur-X** in France, and ready for Germany's B2B e-invoicing mandate (reception mandatory since January 2025).

Examples in **C#, Java, PHP, JavaScript, Node.js, Python, and Ruby** covering every common ZUGFeRD operation: invoice creation, validation, structured data extraction as JSON, raw XML extraction, and embedding your own CII XML into your own PDF to produce a compliant hybrid PDF/A-3.

Provided and maintained by [InvoiceXML](https://www.invoicexml.com), a complete e-invoice compliance toolkit for electronic invoicing standards, including ZUGFeRD, Factur-X, UBL, Peppol BIS Billing, and XRechnung, available via REST-API, no-code platforms, and MCP server.

## InvoiceXML vs building it yourself

The EN 16931 specification alone runs over 400 pages, PDF/A-3b conformance is stricter than most teams expect, and the validation Schematron updates with every regulatory revision. A realistic in-house build is 6 to 12 weeks of engineering, plus 0.3 to 0.5 FTE permanently allocated to tracking spec changes as more and more EU countries roll out their mandates. InvoiceXML absorbs all of that behind a single REST API covering ZUGFeRD, Factur-X, XRechnung, UBL, CII, and Peppol BIS. EU-hosted, GDPR-compliant, no invoice data persisted between calls, sub-second responses, 99.95% uptime. Your integration keeps calling the same endpoints when the spec changes next quarter, you get the latest compliance out of the box, zero maintenance required.

---

## Table of Contents

- [What is ZUGFeRD?](#what-is-zugferd)
- [Why use this repository](#why-use-this-repository)
- [Operations covered](#operations-covered)
- [Quick start by language](#quick-start-by-language)
  - [Create and validate ZUGFeRD with C# / .NET](#create-and-validate-zugferd-with-c--net)
  - [Create and validate ZUGFeRD with Java](#create-and-validate-zugferd-with-java)
  - [Create and validate ZUGFeRD with PHP](#create-and-validate-zugferd-with-php)
  - [Create and validate ZUGFeRD with JavaScript in the browser](#create-and-validate-zugferd-with-javascript-in-the-browser)
  - [Create and validate ZUGFeRD with Node.js](#create-and-validate-zugferd-with-nodejs)
  - [Create and validate ZUGFeRD with Python](#create-and-validate-zugferd-with-python)
  - [Create and validate ZUGFeRD with Ruby](#create-and-validate-zugferd-with-ruby)
- [Extract structured data from ZUGFeRD as JSON](#extract-structured-data-from-zugferd-as-json)
- [Extract embedded XML from a ZUGFeRD PDF](#extract-embedded-xml-from-a-zugferd-pdf)
- [Embed your own XML into your own PDF](#embed-your-own-xml-into-your-own-pdf)
- [ZUGFeRD profiles and compliance levels](#zugferd-profiles-and-compliance-levels)
- [Related e-invoicing standards](#related-e-invoicing-standards)
- [Frequently asked questions](#frequently-asked-questions)
- [Resources and references](#resources-and-references)
- [Contributing](#contributing)
- [License](#license)

---

## What is ZUGFeRD?

**ZUGFeRD** (Zentraler User Guide des Forums elektronische Rechnung Deutschland) is the German hybrid electronic invoicing standard, maintained by FeRD together with France's FNFE-MPE. A ZUGFeRD document is a **PDF/A-3 file with an embedded XML payload**. The PDF can be read by humans like any normal invoice, while machines can parse the structured XML automatically for accounting, archiving, and tax reporting.

ZUGFeRD 2.x is fully aligned with **EN 16931**, the European norm for electronic invoicing. In France the same specification is published under the name **Factur-X**. The technical content is identical; only the branding differs. Since January 2025, all German businesses must be able to receive electronic invoices, and ZUGFeRD invoices in profile EN 16931 (Comfort) or higher qualify as e-invoices under the German B2B rules.

For a longer introduction including history, profiles, and legal status, see the [ZUGFeRD overview guide on invoicexml.com](https://invoicexml.com/zugferd).

## Why use this repository

Most existing ZUGFeRD libraries are tied to a single language or vendor, and documentation is often only available in German. This repository provides **side-by-side, copy-pasteable code examples** for the languages most commonly used in invoicing, ERP, and accounting systems, so developers can:

- Pick the language already used in their stack
- See exactly which ZUGFeRD library to use and how to call it
- Run the same operations (create, validate, extract JSON, extract XML, embed) in any of the supported languages
- Compare behavior and output across implementations
- Use the produced invoices as test fixtures for their own systems

Every example in this repository produces or consumes invoices that conform to the official ZUGFeRD specification.

## Operations covered

| Operation | Description |
|---|---|
| **[Create ZUGFeRD](https://www.invoicexml.com/docs/api/create/zugferd)** | Build a ZUGFeRD PDF/A-3 invoice from scratch with embedded EN 16931 XML |
| **[Validate ZUGFeRD](https://www.invoicexml.com/docs/api/validate/zugferd)** | Verify a ZUGFeRD file against the official ZUGFeRD and EN 16931 schematron rules |
| **[Extract JSON](https://www.invoicexml.com/docs/api/extract/json)** | Parse the embedded XML and return structured invoice data as JSON for downstream systems |
| **[Extract XML](https://www.invoicexml.com/docs/api/extract/xml)** | Extract the raw XML payload from a ZUGFeRD PDF/A-3 container |
| **[Embed XML into PDF](https://www.invoicexml.com/api/embed/zugferd)** | Attach your own CII XML to your own invoice PDF and get a compliant hybrid PDF/A-3 back |

---

## Quick start by language

Each language has its own folder containing all five operations as standalone, runnable files. Open the folder for your language to see installation steps, dependencies, and usage.

### Create and validate ZUGFeRD with C# / .NET

```
/csharp
  Create.cs         # Build a ZUGFeRD PDF/A-3 with embedded XML
  Validate.cs       # Validate against ZUGFeRD schematron rules
  ExtractJson.cs    # Extract invoice data as JSON
  ExtractXml.cs     # Extract raw XML from a ZUGFeRD PDF
  Embed.cs          # Embed your own CII XML into your own PDF (hybrid PDF/A-3)
```

[Open the C# / .NET examples →](./csharp)

### Create and validate ZUGFeRD with Java

```
/java
  Create.java
  Validate.java
  ExtractJson.java
  ExtractXml.java
  Embed.java
```

[Open the Java examples →](./java)

### Create and validate ZUGFeRD with PHP

```
/php
  create.php
  validate.php
  extract-json.php
  extract-xml.php
  embed.php
```

[Open the PHP examples →](./php)

### Create and validate ZUGFeRD with JavaScript in the browser

Browser-side examples for generating and inspecting ZUGFeRD invoices client-side, useful for web-based invoicing apps that want to avoid round-trips to a server.

```
/javascript
  create.html
  validate.html
  extract-json.html
  extract-xml.html
  embed.html
```

[Open the JavaScript examples →](./javascript)

### Create and validate ZUGFeRD with Node.js

```
/nodejs
  create.js
  validate.js
  extract-json.js
  extract-xml.js
  embed.js
```

[Open the Node.js examples →](./nodejs)

### Create and validate ZUGFeRD with Python

```
/python
  create.py
  validate.py
  extract_json.py
  extract_xml.py
  embed.py
```

[Open the Python examples →](./python)

### Create and validate ZUGFeRD with Ruby

Standard library only (`Net::HTTP`), no gems. The Ruby folder additionally covers AI parsing of old-school PDFs with confidence scores and embedded attachment extraction.

```
/ruby
  create.rb
  validate.rb
  extract_json.rb
  extract_xml.rb
  embed.rb
  parse_json.rb
  extract_attachments.rb
```

[Open the Ruby examples →](./ruby)

---

## Extract structured data from ZUGFeRD as JSON

Many downstream systems (ERPs, accounting software, expense management tools) work natively with JSON rather than XML. The `ExtractJson` examples in every language read the embedded ZUGFeRD XML, parse it according to EN 16931 semantics, and emit a clean JSON response with the invoice document under the `invoice` key: seller, buyer, line items, tax breakdown, totals, and payment details.

A complete sample response with every field of the invoice model populated is in [json/extract-json-response.json](./json/extract-json-response.json); optional fields that are absent from the source document come back as `null` (empty arrays for lists).

This is the fastest way to get invoice data out of a ZUGFeRD PDF into a modern data pipeline.

## Extract embedded XML from a ZUGFeRD PDF

A ZUGFeRD PDF/A-3 carries its XML as an attached file with a specific name and AFRelationship metadata: `factur-x.xml` since ZUGFeRD 2.2 (the name the specification prescribes today), `zugferd-invoice.xml` in legacy 1.0/2.0 documents. The `ExtractXml` examples show how to locate and extract that attachment correctly in each language, handling PDF/A-3 attachment relationships rather than treating it as a generic embedded file.

---

## Embed your own XML into your own PDF

Many teams already have both halves of a hybrid invoice: their accounting system, template engine, or designer-made layout renders the PDF, and their ERP produces the EN 16931 XML. What is left is the packaging, and packaging is where hybrid invoices most often go wrong: PDF/A-3 conformance, the exact attachment name, the AFRelationship value, and the XMP metadata block that declares the ZUGFeRD profile.

The `Embed` examples post both files to [`POST /v1/embed/zugferd`](https://www.invoicexml.com/docs/api/embed/zugferd) and get the finished hybrid PDF/A-3 back:

```
pdf             your rendered invoice PDF (any standard PDF, promoted to PDF/A-3 in place)
xml             your UN/CEFACT CII invoice XML (embedded verbatim as factur-x.xml)
skipValidation  optional, default false
```

Your visual layer is preserved exactly: same layout, fonts, and branding, no re-rendering. The XML is embedded verbatim, so what you send is what your recipient parses. The attachment is named `factur-x.xml`, which is what the ZUGFeRD specification has prescribed since version 2.2 (`zugferd-invoice.xml` was the legacy 1.0/2.0 name); a document declaring the XRechnung reference profile is attached as `xrechnung.xml` instead.

Before anything is embedded, the XML runs through the complete `/v1/validate/zugferd` rule set (the profile's official XSD plus the matching Schematron business rules). Fatal findings reject the request with `errorCode` 4001 and the full list of violated rules, so a non-compliant invoice never reaches the buyer's system; warnings never block. Pass `skipValidation=true` for packaging-only mode, where structural checks (CII root element, official BT-24 profile URN, profile XSD conformance) still apply but business rules are skipped, useful during a migration.

Two things to know:

- **CII only.** The root element must be `<CrossIndustryInvoice>`. UBL documents are rejected; convert them first with [`POST /v1/convert/ubl/to/cii`](https://www.invoicexml.com/docs/api/convert/ubl/to/cii).
- **French conventions.** `/v1/embed/facturx` takes the same request and applies the FNFE-MPE packaging practice instead (always `factur-x.xml` with AFRelationship `Data`, XRechnung and e-reporting wrapped at their EN 16931 baseline). Pick the endpoint matching what your recipient expects.

If you want the API to render the PDF as well, use the [create endpoints](https://www.invoicexml.com/docs/api/create/zugferd) instead, which build both layers from a JSON invoice document.

---

## ZUGFeRD profiles and compliance levels

ZUGFeRD defines five profiles, ordered by how much structured data the XML carries. All five share the same PDF/A-3 container; they differ only in XML completeness.

| Profile | Description | Typical use case |
|---|---|---|
| **MINIMUM** | Header-level totals only, not EN 16931-compliant on its own | Pure archiving |
| **BASIC WL** (Without Lines) | Header data, no line items | Lightweight B2B exchange |
| **BASIC** | EN 16931-compliant subset with line items | Most common B2B invoices |
| **EN 16931** (Comfort) | Full EN 16931 semantic model | B2G in EU member states |
| **EXTENDED** | EN 16931 plus country-specific extensions | Complex cross-border cases |

The examples in this repository default to the **EN 16931** profile, which is the most broadly accepted level for both B2B and B2G use across Europe.

---

## Related e-invoicing standards

ZUGFeRD is one of several formats in the European e-invoicing landscape. If you work with invoices, you will likely encounter the others:

- **Factur-X**: French equivalent of ZUGFeRD 2.x. Technically identical specification, different branding.
- **UBL 2.1**: Universal Business Language. Pure XML (no PDF wrapper). Used by Peppol.
- **Peppol BIS Billing 3.0**: UBL-based subset exchanged over the Peppol network.
- **XRechnung**: German B2G-mandatory profile, available in both UBL and CII syntax.
- **EN 16931**: the European norm that underlies ZUGFeRD, Factur-X, Peppol BIS, and XRechnung.

For a side-by-side comparison of when to use which format, see the [e-invoicing standards comparison on invoicexml.com](https://invoicexml.com/standards).

---

## Frequently asked questions

**Is ZUGFeRD the same as Factur-X?**
For versions 2.x and later, yes: the technical specifications are identical. ZUGFeRD is the German name, Factur-X is the French name. Earlier ZUGFeRD 1.0 used a different XML syntax (Cross-Industry Invoice 16B vs 100) and is not interchangeable.

**Is ZUGFeRD mandatory in Germany?**
Receiving e-invoices has been mandatory for all German businesses since January 2025. Issuing structured e-invoices becomes mandatory for most B2B transactions in phases from 2027 (2028 for smaller businesses). ZUGFeRD invoices in profile EN 16931 (Comfort) or higher qualify; the MINIMUM and BASIC WL profiles do not.

**Which ZUGFeRD version do these examples produce?**
ZUGFeRD 2.3 with the EN 16931 (Comfort) profile, generated via the InvoiceXML API and validated against the full EN 16931 rule set before delivery.

**Can I use ZUGFeRD for B2G invoicing in Germany?**
Yes, ZUGFeRD 2.x in profile EN 16931 or higher is one of the formats accepted by the German federal e-invoicing portals (ZRE/OZG-RE), alongside XRechnung. Many authorities prefer XRechnung as standalone XML; use the /v1/create/xrechnung endpoint for that.

**What is the difference between ZUGFeRD and UBL?**
ZUGFeRD is a hybrid PDF+XML format using the UN/CEFACT Cross-Industry Invoice (CII) XML syntax. UBL is a pure XML format using OASIS Universal Business Language syntax. Both can express EN 16931-compliant invoices, but they are not the same XML.

---

## Resources and references

- [ZUGFeRD official specification (FeRD, Germany)](https://www.ferd-net.de/standards/zugferd)
- [Factur-X specification (FNFE-MPE, France)](https://fnfe-mpe.org/factur-x/)
- [EN 16931 (European Committee for Standardization)](https://www.cencenelec.eu/)
- [Peppol BIS Billing 3.0](https://docs.peppol.eu/poacc/billing/3.0/)
- [invoicexml.com](https://invoicexml.com): guides, tutorials, validators, and standard comparisons for ZUGFeRD, Factur-X, UBL, Peppol, and XRechnung

---

## Contributing

Pull requests are welcome. Useful contributions include:

- Additional languages (Go, Rust, Kotlin, Swift)
- Alternative library implementations in existing languages
- Sample invoice fixtures covering edge cases (multi-currency, intra-EU reverse charge, zero-rated supplies, credit notes)
- Translations of the README and inline comments
- Bug fixes and validation rule clarifications

Please include a working example, pinned dependency versions, and a short note in the relevant language folder's README.
