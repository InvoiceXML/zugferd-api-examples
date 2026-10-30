# ZUGFeRD for Java: InvoiceXML API Examples

Java code samples for creating, validating, and extracting **ZUGFeRD** electronic invoices using the **[InvoiceXML API](https://www.invoicexml.com)**. Requires **Java 15 or later** (Java 17 or 21 LTS recommended): `Create.java` and `SendEmail.java` use text blocks, which were introduced in Java 15. The other examples also compile on Java 8. Runs in Spring Boot, Quarkus, Micronaut, Jakarta EE, Android, AWS Lambda, or plain `public static void main` console apps.

For background on the ZUGFeRD standard itself (what it is, profiles, legal status), see the [main repository README](../README.md).

> **Using Spring Boot?** The examples in this folder use OkHttp. There is a parallel set written against Spring's own `RestClient`, so you do not have to add a second HTTP client to your dependency tree: **[Spring Boot examples →](./spring)**

## Get your API key

Every example in this folder calls the InvoiceXML REST API. Sign up and generate a free API key here:

**→ [https://www.invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication)**

Pass it as a Bearer token on every request:

```
Authorization: Bearer YOUR_API_KEY
```

**Important:** set `apiKey` in the examples to the raw key only, without the `Bearer ` prefix. If your account page shows the full header value (e.g. `Bearer ixml_a1b2c3...`), copy only the part after `Bearer `. The code adds the prefix itself when building the `Authorization` header.

## Requirements

- **Java 15 or later** (Java 17 or 21 LTS recommended). `Create.java` and `SendEmail.java` use text blocks, a Java 15+ feature; the other examples also compile on Java 8.
- **OkHttp 4.x** for clean multipart handling and bearer auth

### Maven

```xml
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.squareup.okhttp3:okhttp:4.12.0'
```

OkHttp is the de facto standard HTTP client for modern Java. Java's built-in `java.net.http.HttpClient` does not support multipart out of the box, so doing this in pure JDK would triple the line count of every example.

## Files in this folder

| File | Operation | API endpoint |
|---|---|---|
| [`Create.java`](./Create.java) | Build a ZUGFeRD PDF/A-3 invoice with embedded EN 16931 XML | [`POST /v1/create/zugferd`](https://www.invoicexml.com/docs/api/create/zugferd) |
| [`SendEmail.java`](./SendEmail.java) | Create a ZUGFeRD invoice and email it from your own domain | [`POST /v1/create/zugferd/email`](https://www.invoicexml.com/docs/api/create/zugferd/email) |
| [`Validate.java`](./Validate.java) | Validate a ZUGFeRD file against schematron rules | [`POST /v1/validate/zugferd`](https://www.invoicexml.com/docs/api/validate/zugferd) |
| [`ExtractJson.java`](./ExtractJson.java) | Extract ZUGFeRD invoice data as JSON | [`POST /v1/extract/json`](https://www.invoicexml.com/docs/api/extract/json) |
| [`ExtractXml.java`](./ExtractXml.java) | Extract the raw `factur-x.xml` from a ZUGFeRD PDF | [`POST /v1/extract/xml`](https://www.invoicexml.com/docs/api/extract/xml) |
| [`Embed.java`](./Embed.java) | Embed your own CII XML into your own PDF as a ZUGFeRD PDF/A-3 | [`POST /v1/embed/zugferd`](https://www.invoicexml.com/docs/api/embed/zugferd) |
| [`SendEmbeddedEmail.java`](./SendEmbeddedEmail.java) | Embed your own CII XML into your own PDF and email it from your own domain | [`POST /v1/embed/zugferd/email`](https://www.invoicexml.com/docs/api/embed/zugferd/email) |

Each file is a standalone `public class` with a `main` method, runnable with `javac` and `java` directly, or as part of any Maven or Gradle project.

> **Note on the snippets below:** they are excerpts from those files. The `try` blocks have no `catch` clause, so the enclosing method must declare `throws Exception` (or at least `throws IOException`), exactly as the full files do with `public static void main(String[] args) throws Exception`. Pasting a snippet into a method without that `throws` clause will not compile ("unreported exception IOException"). When in doubt, copy the complete file.

---

## Create a ZUGFeRD invoice in Java

```java
String json = """
    {
      "invoice": {
        "invoiceNumber": "MIN-001",
        "issueDate": "2026-05-18",
        "currency": "EUR",
        "seller": {
          "name": "Acme",
          "vatIdentifier": "DE123456789",
          "legalRegistration": { "identifier": "HRB 12345" },
          "postalAddress": { "line1": "Hauptstraße 12", "city": "Berlin", "postCode": "10115", "country": "DE" }
        },
        "buyer": {
          "name": "Globex SAS",
          "postalAddress": { "line1": "15 rue de Rivoli", "city": "Paris", "postCode": "75001", "country": "FR" }
        },
        "paymentDetails": { "paymentAccountIdentifier": "DE89370400440532013000" },
        "lines": [{
          "quantity": 10,
          "priceDetails": { "netPrice": 150.00 },
          "vatInformation": { "rate": 19.00 },
          "item": { "name": "Senior consulting" }
        }]
      }
    }
    """;

RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));

Request request = new Request.Builder()
    .url("https://api.invoicexml.com/v1/create/zugferd")
    .header("Authorization", "Bearer " + apiKey)
    .post(body)
    .build();

try (Response response = new OkHttpClient().newCall(request).execute()) {
    byte[] pdf = response.body().bytes();
    Files.write(Paths.get("invoice-zugferd.pdf"), pdf);
}
```

The response is a binary PDF/A-3 file with the ZUGFeRD XML already embedded.

[Full example: `Create.java`](./Create.java) | [API reference](https://www.invoicexml.com/docs/api/create/zugferd)

---

## Validate a ZUGFeRD file in Java

```java
RequestBody body = new MultipartBody.Builder()
    .setType(MultipartBody.FORM)
    .addFormDataPart("file", "invoice.pdf",
        RequestBody.create(new File("invoice.pdf"), MediaType.parse("application/pdf")))
    .addFormDataPart("version", "2.3.2")
    .addFormDataPart("profile", "extended")
    .build();

Request request = new Request.Builder()
    .url("https://api.invoicexml.com/v1/validate/zugferd")
    .header("Authorization", "Bearer " + apiKey)
    .post(body)
    .build();

try (Response response = new OkHttpClient().newCall(request).execute()) {
    System.out.println(response.body().string());
}
```

Returns a JSON validation report listing any schematron rule failures (EN 16931 BR-* and BR-CO-* rules).

[Full example: `Validate.java`](./Validate.java) | [API reference](https://www.invoicexml.com/docs/api/validate/zugferd)

---

## Extract ZUGFeRD data as JSON in Java

Useful for feeding ZUGFeRD invoices into Spring Boot services, message queues, or any pipeline that prefers JSON over XML.

```java
RequestBody body = new MultipartBody.Builder()
    .setType(MultipartBody.FORM)
    .addFormDataPart("file", "invoice.pdf",
        RequestBody.create(new File("invoice.pdf"), MediaType.parse("application/pdf")))
    .build();

try (Response response = new OkHttpClient().newCall(
        new Request.Builder()
            .url("https://api.invoicexml.com/v1/extract/json")
            .header("Authorization", "Bearer " + apiKey)
            .post(body).build()
    ).execute()) {
    String json = response.body().string();
    // Deserialize with Jackson, Gson, or your preferred JSON library
}
```

[Full example: `ExtractJson.java`](./ExtractJson.java) | [API reference](https://www.invoicexml.com/docs/api/extract/json) | [Sample response](../json/extract-json-response.json)

---

## Extract embedded XML from a ZUGFeRD PDF in Java

Returns the raw `factur-x.xml` payload (UN/CEFACT Cross-Industry Invoice syntax). Use this when you need the structured XML to feed an existing UBL or CII pipeline, EDI partner, or archival system.

[Full example: `ExtractXml.java`](./ExtractXml.java) | [API reference](https://www.invoicexml.com/docs/api/extract/xml)

---

## Embed your own XML into your own PDF

When your application already renders the invoice PDF and already produces the EN 16931 XML, post both files and the API keeps your visual layer exactly as designed, promotes the container to PDF/A-3, and attaches the XML as `factur-x.xml` (the attachment name the ZUGFeRD specification prescribes since version 2.2) with the German (FeRD) AFRelationship and XMP conventions.

```java
RequestBody body = new MultipartBody.Builder()
    .setType(MultipartBody.FORM)
    .addFormDataPart("pdf", "invoice.pdf",
        RequestBody.create(new File("invoice.pdf"), MediaType.parse("application/pdf")))
    .addFormDataPart("xml", "factur-x.xml",
        RequestBody.create(new File("factur-x.xml"), MediaType.parse("application/xml")))
    .addFormDataPart("skipValidation", "false")
    .build();
```

The XML runs through the complete `/v1/validate/zugferd` rule set before anything is embedded, so a non-compliant invoice never leaves the API: fatal findings come back as a 400 with `errorCode` 4001 and the full finding list. Set `skipValidation` to `true` for packaging-only mode, where the structural checks (CII root element, official BT-24 profile URN, profile XSD) still apply but the business rules are skipped.

Only UN/CEFACT CII XML is accepted. If your invoice is UBL, convert it first with [`POST /v1/convert/ubl/to/cii`](https://www.invoicexml.com/docs/api/convert/ubl/to/cii). For the French (FNFE-MPE) packaging conventions, call `/v1/embed/facturx` instead, same request shape.

[Full example: `Embed.java`](./Embed.java) | [API reference](https://www.invoicexml.com/docs/api/embed/zugferd)

---

## Framework integration

### Spring Boot

Return a ZUGFeRD invoice from a REST controller:

```java
@RestController
public class ZugferdController {

    @GetMapping(value = "/invoices/{id}/zugferd", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> download(@PathVariable String id) throws Exception {
        byte[] pdf = zugferdService.create(id);
        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"invoice-" + id + ".pdf\"")
            .body(pdf);
    }
}
```

Inject the API key from `application.properties` via `@Value("${invoicexml.api-key}")`.

To drop OkHttp entirely and call the API through Spring's own `RestClient`, use the [Spring Boot examples](./spring) instead: an injectable client, a `@Configuration` bean with connection pooling, and the multipart filename detail that trips most first integrations.

### Quarkus

```java
@Path("/invoices")
public class ZugferdResource {

    @GET
    @Path("/{id}/zugferd")
    @Produces("application/pdf")
    public Response download(@PathParam("id") String id) throws Exception {
        byte[] pdf = zugferdService.create(id);
        return Response.ok(pdf)
            .header("Content-Disposition", "attachment; filename=\"invoice-" + id + ".pdf\"")
            .build();
    }
}
```

### Android

OkHttp is also Square's library for Android, so the examples run on Android 5.0+ unchanged. Move the call off the main thread using `enqueue()`, `CoroutineScope`, or `RxJava`. For Android 9+ ensure your network security config allows TLS to `api.invoicexml.com` (it does by default).

### AWS Lambda

Bundle OkHttp into your Lambda deployment package or layer. Cold-start latency is minimal because OkHttp is small (~700 KB).

---

## Common issues

- **`HTTP 401 Unauthorized`**: API key missing or invalid. Generate one at [invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication) and confirm you are sending `Authorization: Bearer YOUR_API_KEY`. A frequent cause: pasting the whole `Bearer xxx` value as `apiKey`, which sends `Bearer Bearer xxx`. Set `apiKey` to the raw key only.
- **`HTTP 400 Bad Request` on Create**: a required field is missing or malformed. Frequent causes: `IssueDate` not in ISO format (`YYYY-MM-DD`), `Currency` not in ISO 4217 (`EUR`, `USD`), country codes not in ISO 3166-1 alpha-2 (`DE`, `FR`).
- **`RequestBody.create()` argument order**: OkHttp 4.x reversed the parameter order from 3.x. Use `RequestBody.create(File, MediaType)` for 4.x. The 3.x signature `(MediaType, File)` is deprecated but still callable.
- **`Files.writeString` not found**: that method requires Java 11+. The examples use `Files.write(Path, byte[])` which works on Java 8+.
- **TLS handshake failures on old JDKs**: enable TLS 1.2 explicitly on Java 8 with `-Dhttps.protocols=TLSv1.2,TLSv1.3`, or upgrade to Java 11+.
- **Schematron BR-CO-* failures on Validate**: line totals do not match the header total, or tax category and tax percentage are inconsistent. Recompute totals before posting.

---

## Resources

- [Get an InvoiceXML API key](https://www.invoicexml.com/account/authentication)
- [Create ZUGFeRD API reference](https://www.invoicexml.com/docs/api/create/zugferd)
- [Validate ZUGFeRD API reference](https://www.invoicexml.com/docs/api/validate/zugferd)
- [Extract JSON API reference](https://www.invoicexml.com/docs/api/extract/json)
- [Extract XML API reference](https://www.invoicexml.com/docs/api/extract/xml)
- [Embed into ZUGFeRD API reference](https://www.invoicexml.com/docs/api/embed/zugferd)
- [OkHttp documentation](https://square.github.io/okhttp/)
- [Main repository README](../README.md)
