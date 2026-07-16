// Experimental: convert a plain PDF invoice to ZUGFeRD using AI.
// Review output manually before sending to a customer or tax authority.
//
// Get API key: https://www.invoicexml.com/account/authentication
// Docs:        https://www.invoicexml.com/docs/api/transform/to/zugferd
//
// Dependency: com.squareup.okhttp3:okhttp:4.12.0

import okhttp3.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class AiConvert {
    public static void main(String[] args) throws Exception {
        // Raw key only, without the "Bearer " prefix (it is added below).
        String apiKey = "YOUR_API_KEY";

        RequestBody body = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "plain-invoice.pdf",
                RequestBody.create(new File("plain-invoice.pdf"), MediaType.parse("application/pdf")))
            .addFormDataPart("version",  "2.3.2")
            .addFormDataPart("profile",  "extended")
            .addFormDataPart("syntax",   "cii")
            .addFormDataPart("embed",    "true")
            .addFormDataPart("language", "en")
            .build();

        Request request = new Request.Builder()
            .url("https://api.invoicexml.com/v1/transform/to/zugferd")
            .header("Authorization", "Bearer " + apiKey)
            .post(body)
            .build();

        try (Response response = new OkHttpClient().newCall(request).execute()) {
            byte[] pdf = response.body().bytes();
            if (!response.isSuccessful()) {
                System.err.println("InvoiceXML API error " + response.code() + ": " + new String(pdf));
                System.exit(1);
            }
            Files.write(Paths.get("converted-zugferd.pdf"), pdf);
            System.out.println("Saved converted-zugferd.pdf (" + pdf.length + " bytes)");
            System.out.println("Review the output manually before sending.");
        }
    }
}
