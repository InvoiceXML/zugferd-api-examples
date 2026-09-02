// End-to-end ZUGFeRD flows for a Spring Boot backend.
//
// Two ways to produce a ZUGFeRD PDF/A-3:
//
//   1. One call. The API renders the PDF and embeds the XML.
//        createZugferd(json)
//
//   2. Two calls, when your system already renders the invoice PDF and that PDF is not
//      reachable from the internet. Your PDF is uploaded directly, no public URL needed.
//        createCiiXml(json) -> embedZugferd(pdf, xml)
//
// Requires Java 15+ for the text block. Change the package to your own.

package com.example.invoicexml;

import java.nio.file.Files;
import java.nio.file.Path;

public class ZugferdFlow {

    private final InvoiceXmlClient api;

    public ZugferdFlow(InvoiceXmlClient api) {
        this.api = api;
    }

    /** Two-step flow: your own PDF keeps its exact layout, fonts and branding. */
    public byte[] fromOwnPdf(String invoiceJson, byte[] ownPdf) {
        byte[] cii = api.createCiiXml(invoiceJson);
        return api.embedZugferd(ownPdf, cii);
    }

    /** One-step flow: the API renders the visual layer for you. */
    public byte[] fromModelOnly(String invoiceJson) {
        return api.createZugferd(invoiceJson);
    }

    // Map your own invoice model onto this shape. The full field reference is at
    // https://www.invoicexml.com/docs/api/create/zugferd
    public static final String EXAMPLE_JSON = """
        {
          "invoice": {
            "invoiceNumber": "RE-2026-000123",
            "issueDate": "2026-05-18",
            "currency": "EUR",
            "seller": {
              "name": "Acme GmbH",
              "vatIdentifier": "DE123456789",
              "legalRegistration": { "identifier": "HRB 12345" },
              "postalAddress": {
                "line1": "Hauptstraße 12",
                "city": "Berlin",
                "postCode": "10115",
                "country": "DE"
              }
            },
            "buyer": {
              "name": "Globex SAS",
              "postalAddress": {
                "line1": "15 rue de Rivoli",
                "city": "Paris",
                "postCode": "75001",
                "country": "FR"
              }
            },
            "paymentDetails": { "paymentAccountIdentifier": "DE89370400440532013000" },
            "lines": [
              {
                "quantity": 10,
                "priceDetails": { "netPrice": 150.00 },
                "vatInformation": { "rate": 19.00 },
                "item": { "name": "Senior consulting" }
              }
            ]
          }
        }
        """;

    // Standalone runner. In a Spring Boot application you would inject InvoiceXmlClient
    // from InvoiceXmlConfig instead of building one here.
    public static void main(String[] args) throws Exception {
        var api = new InvoiceXmlClient(
                org.springframework.web.client.RestClient.builder(),
                System.getenv("INVOICEXML_API_KEY"));

        var flow = new ZugferdFlow(api);

        byte[] ownPdf = Files.readAllBytes(Path.of("invoice.pdf"));
        byte[] hybrid = flow.fromOwnPdf(EXAMPLE_JSON, ownPdf);

        Files.write(Path.of("invoice-zugferd.pdf"), hybrid);
        System.out.println("Saved invoice-zugferd.pdf (" + hybrid.length + " bytes)");

        System.out.println(api.validateZugferd(hybrid));
    }
}
