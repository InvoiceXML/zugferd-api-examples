// Embed a CII XML into your own PDF and email the result, in one call.
// Builds the hybrid PDF exactly like POST /v1/embed/zugferd, always with
// full validation, then sends it as an attachment from your own domain.
// This sends a real email. The answer is the message id, not the file.
//
// Before the first run, add and verify your domain:
// https://www.invoicexml.com/account/sending-domains
// Your plan must include email delivery: https://www.invoicexml.com/pricing
//
// Get API key: https://www.invoicexml.com/account/authentication
// Docs:        https://www.invoicexml.com/docs/api/embed/zugferd/email
//
// Dependency: com.squareup.okhttp3:okhttp:4.12.0

import okhttp3.*;
import java.time.Duration;
import java.io.File;

public class SendEmbeddedEmail {
    public static void main(String[] args) throws Exception {
        // Raw key only, without the "Bearer " prefix (it is added below).
        String apiKey = "YOUR_API_KEY";

        RequestBody body = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("pdf", "invoice.pdf",
                RequestBody.create(new File("invoice.pdf"), MediaType.parse("application/pdf")))
            .addFormDataPart("xml", "factur-x.xml",
                RequestBody.create(new File("factur-x.xml"), MediaType.parse("application/xml")))
            .addFormDataPart("email.from", "invoices@yourcompany.com") // any address on your verified domain
            .addFormDataPart("email.fromName", "Acme Billing")
            .addFormDataPart("email.to", "you@yourcompany.com") // send the first test to yourself
            .addFormDataPart("email.subject", "Invoice MIN-001")
            .addFormDataPart("email.message", "Hello,\n\nPlease find attached invoice MIN-001.\n\nKind regards")
            // Optional: email.cc and email.bcc (repeat the field, 2 addresses together) and email.replyTo.
            .build();

        Request request = new Request.Builder()
            .url("https://api.invoicexml.com/v1/embed/zugferd/email")
            .header("Authorization", "Bearer " + apiKey)
            .post(body)
            .build();

        // The API can take up to 90 seconds. With OkHttp's default 10 second read timeout this
        // call could report a failure after the email went out, and a rerun would send it twice.
        OkHttpClient client = new OkHttpClient.Builder().readTimeout(Duration.ofSeconds(120)).build();

        try (Response response = client.newCall(request).execute()) {
            String text = response.body().string();
            if (!response.isSuccessful()) {
                System.err.println("InvoiceXML API error " + response.code() + ": " + text);
                System.exit(1);
            }
            // {"status":"accepted","messageId":"..."}
            System.out.println("Email accepted for delivery: " + text);
        }
    }
}
