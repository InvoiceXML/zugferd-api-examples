# ZUGFeRD for C# and .NET: InvoiceXML REST API Examples

C# and .NET code samples for creating, validating, and extracting **ZUGFeRD** electronic invoices using the **[invoicexml.com API](https://www.invoicexml.com)**. Requires **.NET 6 or later** (.NET 8 recommended), and ready to drop into console apps, ASP.NET Core Web APIs, Blazor, MAUI, Azure Functions, or AWS Lambda.

## Get your API key

Every example in this folder calls the InvoiceXML REST API. Sign up and generate a key here:

**→ [https://www.invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication)**

Pass it as a Bearer token on every request:

```
Authorization: Bearer YOUR_API_KEY
```

**Important:** pass the raw key only, without the `Bearer ` prefix. If your account page shows the full header value (e.g. `Bearer ixml_a1b2c3...`), copy only the part after `Bearer `. Flurl's `WithOAuthBearerToken(apiKey)` adds the prefix itself.

## Requirements

- **.NET 6.0 or later** (recommended: .NET 8) with `ImplicitUsings` enabled (the default in new SDK-style projects)
- Runs on Windows, Linux, macOS, Docker, Azure, and AWS Lambda
- On **.NET Framework 4.7.2+** the API calls work too (Flurl.Http targets .NET Standard 2.0), but the files as written need small changes: add the `using System; using System.IO; using System.Threading.Tasks;` directives and replace `File.WriteAllBytesAsync` / `File.WriteAllTextAsync` (not available on .NET Framework) with the synchronous `File.WriteAllBytes` / `File.WriteAllText`
- One NuGet package: [Flurl.Http](https://www.nuget.org/packages/Flurl.Http) for clean multipart uploads and bearer auth

```bash
dotnet add package Flurl.Http
```

If you prefer plain `HttpClient` over Flurl, every example translates directly. Flurl just makes the multipart and authentication wiring a one-liner.

## Files in this folder

| File | Operation | API endpoint |
|---|---|---|
| [`Create.cs`](./Create.cs) | Build a ZUGFeRD PDF/A-3 invoice with embedded EN 16931 XML | [`POST /v1/create/zugferd`](https://www.invoicexml.com/docs/api/create/zugferd) |
| [`Validate.cs`](./Validate.cs) | Validate a ZUGFeRD file against schematron rules | [`POST /v1/validate/zugferd`](https://www.invoicexml.com/docs/api/validate/zugferd) |
| [`ExtractJson.cs`](./ExtractJson.cs) | Extract ZUGFeRD invoice data as JSON | [`POST /v1/extract/json`](https://www.invoicexml.com/docs/api/extract/json) |
| [`ExtractXml.cs`](./ExtractXml.cs) | Extract the raw `factur-x.xml` from a ZUGFeRD PDF | [`POST /v1/extract/xml`](https://www.invoicexml.com/docs/api/extract/xml) |
| [`Embed.cs`](./Embed.cs) | Embed your own CII XML into your own PDF as a ZUGFeRD PDF/A-3 | [`POST /v1/embed/zugferd`](https://www.invoicexml.com/docs/api/embed/zugferd) |

> **Note on the snippets below:** they are excerpts from those files and assume an `apiKey` variable is already defined and that the code runs inside an `async` method. The full files show the complete, compilable versions.

---

## Create a ZUGFeRD invoice in C#

```csharp
using Flurl.Http;

var payload = new
{
    invoice = new
    {
        invoiceNumber = "MIN-001",
        issueDate     = "2026-05-18",
        currency      = "EUR",
        seller = new
        {
            name              = "Acme",
            vatIdentifier     = "DE123456789",
            legalRegistration = new { identifier = "HRB 12345" },
            postalAddress     = new { line1 = "Hauptstraße 12", city = "Berlin", postCode = "10115", country = "DE" }
        },
        buyer = new
        {
            name          = "Globex SAS",
            postalAddress = new { line1 = "15 rue de Rivoli", city = "Paris", postCode = "75001", country = "FR" }
        },
        paymentDetails = new { paymentAccountIdentifier = "DE89370400440532013000" },
        lines = new[]
        {
            new {
                quantity       = 10,
                priceDetails   = new { netPrice = 150.00m },
                vatInformation = new { rate = 19.00m },
                item           = new { name = "Senior consulting" }
            }
        }
    }
};

var pdfBytes = await "https://api.invoicexml.com/v1/create/zugferd"
    .WithOAuthBearerToken(apiKey)
    .PostJsonAsync(payload)
    .ReceiveBytes();

await File.WriteAllBytesAsync("invoice-zugferd.pdf", pdfBytes);
```

The response is a binary PDF/A-3 file with the ZUGFeRD XML already embedded.

[Full example: `Create.cs`](./Create.cs) | [API reference](https://www.invoicexml.com/docs/api/create/zugferd)

---

## Validate a ZUGFeRD file in C#

```csharp
using Flurl.Http;

var report = await "https://api.invoicexml.com/v1/validate/zugferd"
    .WithOAuthBearerToken(apiKey)
    .PostMultipartAsync(mp => mp
        .AddFile("file", "invoice.pdf", "application/pdf")
        .AddString("version", "2.3.2")
        .AddString("profile", "extended")
    )
    .ReceiveString();

Console.WriteLine(report);
```

Returns a JSON validation report listing any schematron rule failures (EN 16931 BR-* and BR-CO-* rules).

[Full example: `Validate.cs`](./Validate.cs) | [API reference](https://www.invoicexml.com/docs/api/validate/zugferd)

---

## Extract ZUGFeRD data as JSON in C#

A common need: get ZUGFeRD invoice data into a JSON-friendly format for REST APIs, ERPs, or downstream pipelines.

```csharp
using Flurl.Http;

var json = await "https://api.invoicexml.com/v1/extract/json"
    .WithOAuthBearerToken(apiKey)
    .PostMultipartAsync(mp => mp
        .AddFile("file", "invoice.pdf", "application/pdf"))
    .ReceiveString();

await File.WriteAllTextAsync("invoice.json", json);
```

[Full example: `ExtractJson.cs`](./ExtractJson.cs) | [API reference](https://www.invoicexml.com/docs/api/extract/json) | [Sample response](../json/extract-json-response.json)

---

## Extract embedded XML from a ZUGFeRD PDF in C#

```csharp
using Flurl.Http;

var xml = await "https://api.invoicexml.com/v1/extract/xml"
    .WithOAuthBearerToken(apiKey)
    .PostMultipartAsync(mp => mp
        .AddFile("file", "invoice.pdf", "application/pdf"))
    .ReceiveString();

await File.WriteAllTextAsync("factur-x.xml", xml);
```

Returns the raw `factur-x.xml` payload (UN/CEFACT Cross-Industry Invoice syntax). Use this when you need the structured XML directly, for example to feed an existing UBL or CII pipeline.

[Full example: `ExtractXml.cs`](./ExtractXml.cs) | [API reference](https://www.invoicexml.com/docs/api/extract/xml)

---

## Embed your own XML into your own PDF

When your application already renders the invoice PDF and already produces the EN 16931 XML, you do not need the full create flow. Post both files and the API keeps your visual layer exactly as designed, promotes the container to PDF/A-3, and attaches the XML as `factur-x.xml` (the attachment name the ZUGFeRD specification prescribes since version 2.2) with the German (FeRD) AFRelationship and XMP conventions.

```csharp
using Flurl.Http;

var pdfBytes = await "https://api.invoicexml.com/v1/embed/zugferd"
    .WithOAuthBearerToken(apiKey)
    .PostMultipartAsync(mp => mp
        .AddFile("pdf", "invoice.pdf", "application/pdf")
        .AddFile("xml", "factur-x.xml", "application/xml")
        .AddString("skipValidation", "false")
    )
    .ReceiveBytes();

await File.WriteAllBytesAsync("invoice-zugferd.pdf", pdfBytes);
```

The XML runs through the complete `/v1/validate/zugferd` rule set before anything is embedded, so a non-compliant invoice never leaves the API: fatal findings come back as a 400 with `errorCode` 4001 and the full finding list. Set `skipValidation` to `true` for packaging-only mode, where the structural checks (CII root element, official BT-24 profile URN, profile XSD) still apply but the business rules are skipped.

Only UN/CEFACT CII XML is accepted. If your invoice is UBL, convert it first with [`POST /v1/convert/ubl/to/cii`](https://www.invoicexml.com/docs/api/convert/ubl/to/cii). For the French (FNFE-MPE) packaging conventions, call `/v1/embed/facturx` instead, same request shape.

[Full example: `Embed.cs`](./Embed.cs) | [API reference](https://www.invoicexml.com/docs/api/embed/zugferd)

---

## Framework integration

### ASP.NET Core / Web API

Return a ZUGFeRD invoice from a controller action by proxying the invoicexml.com API:

```csharp
[HttpGet("invoices/{id}/zugferd")]
public async Task<IActionResult> GetZugferd(string id)
{
    var pdfBytes = await CreateZugferd.RunAsync(/* invoice data from your DB */);
    return File(pdfBytes, "application/pdf", $"invoice-{id}.pdf");
}
```

### Console / Worker Service

Each example exposes a static `RunAsync` method. From `Program.cs`:

```csharp
await CreateZugferd.RunAsync();
await ValidateZugferd.RunAsync("invoice.pdf");
await ExtractJson.RunAsync("invoice.pdf");
```

---

## Common issues

- **401 Unauthorized**: API key missing or invalid. Generate one at [invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication) and confirm you are sending `Authorization: Bearer YOUR_API_KEY`. A frequent cause: passing the whole `Bearer xxx` value as the key, which sends `Bearer Bearer xxx`. Pass the raw key only.
- **400 Bad Request on Create**: a required field is missing or malformed. Frequent causes: `IssueDate` not in ISO format (`YYYY-MM-DD`), `Currency` not in ISO 4217 (`EUR`, `USD`), country codes not in ISO 3166-1 alpha-2 (`DE`, `FR`).
- **Schematron BR-CO-* failures on Validate**: line totals do not match the header total, or tax category and tax percentage are inconsistent. Recompute totals or leave empty for auto-calculation when posting.

---

## Resources

- [Get an API key](https://www.invoicexml.com/account/authentication)
- [Create ZUGFeRD API reference](https://www.invoicexml.com/docs/api/create/zugferd)
- [Validate ZUGFeRD API reference](https://www.invoicexml.com/docs/api/validate/zugferd)
- [Extract JSON API reference](https://www.invoicexml.com/docs/api/extract/json)
- [Extract XML API reference](https://www.invoicexml.com/docs/api/extract/xml)
- [Embed into ZUGFeRD API reference](https://www.invoicexml.com/docs/api/embed/zugferd)
- [Flurl.Http documentation](https://flurl.dev/docs/fluent-http/)
- [Main repository README](../README.md)
