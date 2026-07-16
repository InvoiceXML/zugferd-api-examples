// Extract the raw factur-x.xml from a ZUGFeRD PDF using the InvoiceXML API.
// Get API key: https://www.invoicexml.com/account/authentication
// Docs:        https://www.invoicexml.com/docs/api/extract/xml
//
// Dependency: com.squareup.okhttp3:okhttp:4.12.0

import okhttp3.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ExtractXml {
    public static void main(String[] args) throws Exception {
        // Raw key only, without the "Bearer " prefix (it is added below).
        String apiKey = "YOUR_API_KEY";

        RequestBody body = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "invoice.pdf",
                RequestBody.create(new File("invoice.pdf"), MediaType.parse("application/pdf")))
            .build();

        Request request = new Request.Builder()
            .url("https://api.invoicexml.com/v1/extract/xml")
            .header("Authorization", "Bearer " + apiKey)
            .post(body)
            .build();

        try (Response response = new OkHttpClient().newCall(request).execute()) {
            String xml = response.body().string();
            if (!response.isSuccessful()) {
                System.err.println("InvoiceXML API error " + response.code() + ": " + xml);
                System.exit(1);
            }
            Files.write(Paths.get("factur-x.xml"), xml.getBytes());
            System.out.println("Saved factur-x.xml (" + xml.length() + " chars)");
        }
    }
}
