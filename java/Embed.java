// Embed an existing UN/CEFACT CII XML into an existing PDF, producing a
// ZUGFeRD 2.x compliant PDF/A-3 hybrid invoice.
//
// Your PDF keeps its exact layout, fonts, and branding; the API promotes the
// container to PDF/A-3 and attaches the XML as factur-x.xml (the attachment
// name the ZUGFeRD specification prescribes since 2.2). The XML is validated
// with the full /v1/validate/zugferd rule set first, so a non-compliant
// invoice never leaves the API.
//
// Get API key: https://www.invoicexml.com/account/authentication
// Docs:        https://www.invoicexml.com/docs/api/embed/zugferd
//
// Dependency: com.squareup.okhttp3:okhttp:4.12.0

import okhttp3.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Embed {
    public static void main(String[] args) throws Exception {
        // Raw key only, without the "Bearer " prefix (it is added below).
        String apiKey = "YOUR_API_KEY";

        RequestBody body = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("pdf", "invoice.pdf",
                RequestBody.create(new File("invoice.pdf"), MediaType.parse("application/pdf")))
            .addFormDataPart("xml", "factur-x.xml",
                RequestBody.create(new File("factur-x.xml"), MediaType.parse("application/xml")))
            // Optional. "true" packages the XML without EN 16931 business-rule
            // checks; structural checks (CII root, BT-24 URN, XSD) still run.
            .addFormDataPart("skipValidation", "false")
            .build();

        Request request = new Request.Builder()
            .url("https://api.invoicexml.com/v1/embed/zugferd")
            .header("Authorization", "Bearer " + apiKey)
            .post(body)
            .build();

        try (Response response = new OkHttpClient().newCall(request).execute()) {
            byte[] pdf = response.body().bytes();
            if (!response.isSuccessful()) {
                // 400 with errorCode 4001 lists every failed business rule;
                // 4017 means BT-24 is not an official ZUGFeRD profile URN.
                System.err.println("InvoiceXML API error " + response.code() + ": " + new String(pdf));
                System.exit(1);
            }
            Files.write(Paths.get("invoice-zugferd.pdf"), pdf);
            System.out.println("Saved invoice-zugferd.pdf (" + pdf.length + " bytes)");
        }
    }
}
