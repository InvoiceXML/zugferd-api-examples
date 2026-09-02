# ZUGFeRD with Spring Boot: RestClient examples

The same ZUGFeRD operations as the [OkHttp examples](../readme.md) in the parent folder, written against **Spring's `RestClient`** instead. If your backend is Spring Boot, this version drops in without adding a second HTTP client to your dependency tree.

Requires **Spring Framework 6.1+ / Spring Boot 3.2+** (when `RestClient` was introduced) and **Java 17+**.

For background on the ZUGFeRD standard itself, see the [main repository README](../../README.md).

## Get your API key

**→ [https://www.invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication)**

Set `invoicexml.api-key` in `application.properties` to the **raw key only**, without the `Bearer ` prefix. The client adds the prefix itself:

```properties
invoicexml.api-key=ixml_a1b2c3...
```

## Files in this folder

| File | Purpose |
|---|---|
| [`InvoiceXmlClient.java`](./InvoiceXmlClient.java) | The client. One method per endpoint, with problem+json error handling. |
| [`ZugferdFlow.java`](./ZugferdFlow.java) | Both ways to produce a ZUGFeRD PDF/A-3, with a sample payload. |
| [`InvoiceXmlConfig.java`](./InvoiceXmlConfig.java) | Spring wiring, including a pooled connection manager for batch load. |

Change the package declaration (`com.example.invoicexml`) to your own before using them.

## Dependencies

`spring-boot-starter-web` already brings `RestClient`. The only addition is Apache HttpClient 5, used by [`InvoiceXmlConfig.java`](./InvoiceXmlConfig.java) for connection pooling. Skip it if you are happy with the default request factory and low concurrency.

### Maven

```xml
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
</dependency>
```

### Gradle

```groovy
implementation 'org.apache.httpcomponents.client5:httpclient5'
```

---

## Two ways to produce a ZUGFeRD invoice

### One call: the API renders the PDF

Use this when you do not have an invoice PDF of your own. The API renders the visual layer from your invoice model and embeds the XML in the same response.

```java
byte[] pdf = invoiceXmlClient.createZugferd(invoiceJson);
```

[`POST /v1/create/zugferd`](https://www.invoicexml.com/docs/api/create/zugferd)

### Two calls: you keep your own PDF

Use this when your system already renders the invoice PDF and that PDF is not reachable from the internet. The PDF is uploaded directly as multipart, so no public URL is needed.

```java
byte[] cii = invoiceXmlClient.createCiiXml(invoiceJson);   // step 1
byte[] pdf = invoiceXmlClient.embedZugferd(ownPdf, cii);   // step 2
```

Step 1 returns UN/CEFACT CII XML carrying BT-24 `urn:cen.eu:en16931:2017`, which is the ZUGFeRD 2.x EN 16931 (Comfort) profile, so it feeds straight into step 2 with no profile handling in between.

Step 2 preserves your visual layer as-is, promotes the container to PDF/A-3 and attaches the XML as `factur-x.xml`. **Any standard PDF is accepted**, PDF/A-1 input is not required: the endpoint writes the PDF 1.7 body, the XMP packet declaring `pdfaid` part 3, and an sRGB output intent with the ICC profile embedded. Unembedded standard-14 fonts (Helvetica, Arial, Times, Courier) are replaced with metric-compatible embedded faces so the layout does not shift.

Two inputs cannot be repaired automatically and come back as a 400: password-protected PDFs, and PDFs using a CID-keyed (composite) font with no embedded font program (`errorCode` 4015, which names the font). Re-export with fonts embedded in that case.

[`POST /v1/create/cii`](https://www.invoicexml.com/docs/api/create/cii) then [`POST /v1/embed/zugferd`](https://www.invoicexml.com/docs/api/embed/zugferd)

---

## Multipart uploads: the one Spring gotcha

Spring writes a `filename` into the multipart `Content-Disposition` **only when the resource exposes one**, and the API keys off the `.pdf` / `.xml` extension. A plain `ByteArrayResource` has no filename, so the upload is rejected with `errorCode` 4004. Override `getFilename()`:

```java
private static ByteArrayResource part(byte[] data, String filename) {
    return new ByteArrayResource(data) {
        @Override
        public String getFilename() {
            return filename;
        }
    };
}

MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
form.add("pdf", part(pdfBytes, "invoice.pdf"));
form.add("xml", part(xmlBytes, "factur-x.xml"));
form.add("skipValidation", "false");

byte[] hybrid = http.post()
        .uri("/v1/embed/zugferd")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(form)
        .retrieve()
        .body(byte[].class);
```

---

## Error handling

Errors are `application/problem+json` with an `errorCode` field. `RestClient` throws on 4xx and 5xx by default, but the default handler discards the body, which is where the useful part lives: on a rule failure the response lists **every** failed EN 16931 rule, not just the first. The client captures it with `onStatus`:

```java
.retrieve()
.onStatus(HttpStatusCode::isError, (request, response) -> {
    String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
    throw new InvoiceXmlApiException(response.getStatusCode().value(), body);
})
.body(byte[].class);
```

Codes worth branching on:

| Code | Meaning |
|---|---|
| 4001 | EN 16931 business rules failed. The response lists every failed rule. |
| 4002 | The JSON invoice model is incomplete or malformed. |
| 4004 | The upload was not recognised as a PDF or XML file (see the filename note above). |
| 4006 | No XML embedded in the uploaded PDF. |
| 4007 | The PDF could not be processed (malformed, or password-protected). |
| 4015 | The PDF uses a font that cannot be embedded. Re-export with fonts embedded. |
| 4017 | BT-24 is not an official ZUGFeRD profile URN. |

Validation calls are the exception: a **failing** validation is still a `200` with `"valid": false` in the report body, so read the field rather than catching an exception.

---

## Concurrency and timeouts

The default `RestClient` request factory caps parallel connections low enough to serialise a batch. For sustained parallel load, [`InvoiceXmlConfig.java`](./InvoiceXmlConfig.java) swaps in a pooled Apache HttpClient 5 factory:

```java
ConnectionConfig connectionConfig = ConnectionConfig.custom()
        .setConnectTimeout(Timeout.ofSeconds(10))
        .setSocketTimeout(Timeout.ofSeconds(60))
        .build();

PoolingHttpClientConnectionManager pool = PoolingHttpClientConnectionManagerBuilder.create()
        .setDefaultConnectionConfig(connectionConfig)
        .setMaxConnTotal(100)
        .setMaxConnPerRoute(100)
        .build();
```

Timeouts sit on the connection manager rather than on `HttpComponentsClientHttpRequestFactory`, whose own `setReadTimeout` is deprecated from Spring Framework 6.2. A 60 second socket timeout is comfortable: both `create` and `embed` rewrite and re-serialise a whole PDF, so they are the slowest calls in the set.

---

## Using it from a controller

```java
@RestController
public class ZugferdController {

    private final InvoiceXmlClient invoiceXmlClient;

    public ZugferdController(InvoiceXmlClient invoiceXmlClient) {
        this.invoiceXmlClient = invoiceXmlClient;
    }

    @GetMapping(value = "/invoices/{id}/zugferd", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> download(@PathVariable String id) {
        String json = invoiceMapper.toInvoiceXmlModel(invoiceRepository.get(id));
        byte[] ownPdf = pdfRenderer.render(id);

        byte[] cii = invoiceXmlClient.createCiiXml(json);
        byte[] hybrid = invoiceXmlClient.embedZugferd(ownPdf, cii);

        return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"invoice-" + id + ".pdf\"")
            .body(hybrid);
    }
}
```

---

## Resources

- [Get an InvoiceXML API key](https://www.invoicexml.com/account/authentication)
- [Create ZUGFeRD API reference](https://www.invoicexml.com/docs/api/create/zugferd)
- [Embed into ZUGFeRD API reference](https://www.invoicexml.com/docs/api/embed/zugferd)
- [Validate ZUGFeRD API reference](https://www.invoicexml.com/docs/api/validate/zugferd)
- [Spring RestClient documentation](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-restclient)
- [OkHttp examples for plain Java](../readme.md)
- [Main repository README](../../README.md)
