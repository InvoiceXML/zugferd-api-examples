// Validate a ZUGFeRD PDF using the InvoiceXML API.
// Get API key: https://www.invoicexml.com/account/authentication
// Docs:        https://www.invoicexml.com/docs/api/validate/zugferd
//
// Dependency: com.squareup.okhttp3:okhttp:4.12.0

import okhttp3.*;
import java.io.File;

public class Validate {
    public static void main(String[] args) throws Exception {
        // Raw key only, without the "Bearer " prefix (it is added below).
        String apiKey = "YOUR_API_KEY";

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
            String report = response.body().string();
            if (!response.isSuccessful()) {
                System.err.println("InvoiceXML API error " + response.code() + ": " + report);
                System.exit(1);
            }
            System.out.println(report);
        }
    }
}
