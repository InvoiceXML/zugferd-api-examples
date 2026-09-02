// Spring RestClient wrapper around the InvoiceXML API for ZUGFeRD.
//
// Every ZUGFeRD operation the OkHttp examples in the parent folder cover, written
// against Spring's RestClient (Spring Framework 6.1+ / Spring Boot 3.2+) so it drops
// straight into a Spring Boot service without adding a second HTTP client.
//
// Get API key: https://www.invoicexml.com/account/authentication
// Docs:        https://www.invoicexml.com/docs/api
//
// Change the package to your own. Wiring: see InvoiceXmlConfig.java.

package com.example.invoicexml;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class InvoiceXmlClient {

    private final RestClient http;

    public InvoiceXmlClient(RestClient.Builder builder, String apiKey) {
        this.http = builder
                .baseUrl("https://api.invoicexml.com")
                // Raw key only. The "Bearer " prefix is added here, so passing a key that
                // already starts with "Bearer " would send it twice and fail with a 401.
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    /**
     * Build a complete ZUGFeRD PDF/A-3 from a JSON invoice model: the API renders the
     * visual layer and embeds the XML in one call. Use this when you do not have a PDF
     * of your own.
     *
     * POST /v1/create/zugferd
     */
    public byte[] createZugferd(String invoiceJson) {
        return http.post()
                .uri("/v1/create/zugferd")
                .contentType(MediaType.APPLICATION_JSON)
                .body(invoiceJson)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::fail)
                .body(byte[].class);
    }

    /**
     * Step 1 of the two-step flow: turn a JSON invoice model into validated UN/CEFACT
     * CII XML. The result carries BT-24 urn:cen.eu:en16931:2017, which is the ZUGFeRD 2.x
     * EN 16931 (COMFORT) profile, so it feeds {@link #embedZugferd} without further work.
     *
     * POST /v1/create/cii
     */
    public byte[] createCiiXml(String invoiceJson) {
        return http.post()
                .uri("/v1/create/cii")
                .contentType(MediaType.APPLICATION_JSON)
                .body(invoiceJson)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::fail)
                .body(byte[].class);
    }

    /**
     * Step 2 of the two-step flow: your own PDF plus CII XML, returned as a ZUGFeRD 2.x
     * compliant PDF/A-3. The visual layer is preserved as-is, the container is promoted
     * to PDF/A-3 and the XML is attached as factur-x.xml. Any standard PDF is accepted;
     * PDF/A-1 input is not required.
     *
     * skipValidation stays false so the full /v1/validate/zugferd rule set runs before
     * anything is packaged. Pass true for packaging-only mode, where the structural
     * checks still apply but EN 16931 business rules are skipped.
     *
     * POST /v1/embed/zugferd
     */
    public byte[] embedZugferd(byte[] pdf, byte[] ciiXml) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("pdf", part(pdf, "invoice.pdf"));
        form.add("xml", part(ciiXml, "factur-x.xml"));
        form.add("skipValidation", "false");

        return http.post()
                .uri("/v1/embed/zugferd")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::fail)
                .body(byte[].class);
    }

    /**
     * Validate a ZUGFeRD PDF (or a bare CII XML) and get a JSON report listing every
     * schematron rule failure. Unlike the other calls a failed validation is still a
     * 200: read the "valid" field of the report rather than catching an exception.
     *
     * POST /v1/validate/zugferd
     */
    public String validateZugferd(byte[] pdf) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", part(pdf, "invoice.pdf"));

        return http.post()
                .uri("/v1/validate/zugferd")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::fail)
                .body(String.class);
    }

    /**
     * Extract a ZUGFeRD PDF into the JSON invoice model, ready to deserialise with
     * Jackson. The response is an envelope: { "invoice": { ... } }.
     *
     * POST /v1/extract/json
     */
    public String extractJson(byte[] pdf) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", part(pdf, "invoice.pdf"));

        return http.post()
                .uri("/v1/extract/json")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::fail)
                .body(String.class);
    }

    /**
     * Extract the raw factur-x.xml payload embedded in a ZUGFeRD PDF.
     *
     * POST /v1/extract/xml
     */
    public byte[] extractXml(byte[] pdf) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", part(pdf, "invoice.pdf"));

        return http.post()
                .uri("/v1/extract/xml")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::fail)
                .body(byte[].class);
    }

    // Errors come back as application/problem+json carrying an "errorCode" field.
    // The ones worth branching on: 4001 EN 16931 business rules failed (every failed
    // rule is listed), 4002 the JSON model is incomplete, 4006 no XML embedded in the
    // PDF, 4007 the PDF could not be processed, 4015 the PDF uses a font that cannot
    // be embedded, 4017 BT-24 is not an official ZUGFeRD profile URN.
    private void fail(HttpRequest request, ClientHttpResponse response) throws IOException {
        String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        throw new InvoiceXmlApiException(response.getStatusCode().value(), body);
    }

    // Spring only writes a filename into the multipart Content-Disposition when the
    // resource exposes one, and the API keys off the .pdf / .xml extension. Without
    // this override the upload is rejected with 4004 (unsupported content type).
    private static ByteArrayResource part(byte[] data, String filename) {
        return new ByteArrayResource(data) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    public static class InvoiceXmlApiException extends RuntimeException {
        private final int status;
        private final String problemJson;

        public InvoiceXmlApiException(int status, String problemJson) {
            super("InvoiceXML API error " + status + ": " + problemJson);
            this.status = status;
            this.problemJson = problemJson;
        }

        public int getStatus() {
            return status;
        }

        /** The raw application/problem+json body, including the errorCode field. */
        public String getProblemJson() {
            return problemJson;
        }
    }
}
