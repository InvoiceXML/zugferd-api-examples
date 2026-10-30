// Embed a CII XML into your own PDF and email the result, in one call.
// Builds the hybrid PDF exactly like POST /v1/embed/zugferd, always with
// full validation, then sends it as an attachment from your own domain.
// This sends a real email. The answer is the message id, not the file.
//
// Before the first run, add and verify your domain:
// https://www.invoicexml.com/account/sending-domains
// Your plan must include email delivery: https://www.invoicexml.com/pricing
//
// Get your API key at: https://www.invoicexml.com/account/authentication
// Full API reference:  https://www.invoicexml.com/docs/api/embed/zugferd/email
//
// Required NuGet package:
//   dotnet add package Flurl.Http

using Flurl.Http;

public static class SendEmbeddedZugferdEmail
{
    private const string Endpoint = "https://api.invoicexml.com/v1/embed/zugferd/email";

    public static async Task<string> RunAsync(
        string pdfPath = "invoice.pdf",
        string xmlPath = "factur-x.xml")
    {
        // Get an InvoiceXML API key at: https://www.invoicexml.com/account/authentication
        // Raw key only, without the "Bearer " prefix (WithOAuthBearerToken adds it).
        var apiKey = Environment.GetEnvironmentVariable("INVOICEXML_API_KEY")
            ?? throw new InvalidOperationException(
                "Set INVOICEXML_API_KEY environment variable. " +
                "Get an InvoiceXML API key at " +
                "https://www.invoicexml.com/account/authentication");

        if (!File.Exists(pdfPath))
            throw new FileNotFoundException($"Input PDF not found: {pdfPath}");
        if (!File.Exists(xmlPath))
            throw new FileNotFoundException($"Input XML not found: {xmlPath}");

        try
        {
            // Two files in one multipart request: "pdf" is the visual layer,
            // "xml" is the CII payload. The email goes in plain form fields.
            // Optional: email.cc and email.bcc (repeat the field, 2 addresses together) and email.replyTo.
            var answer = await Endpoint
                .WithOAuthBearerToken(apiKey)
                .PostMultipartAsync(mp => mp
                    .AddFile("pdf", pdfPath, "application/pdf")
                    .AddFile("xml", xmlPath, "application/xml")
                    .AddString("email.from", "invoices@yourcompany.com") // any address on your verified domain
                    .AddString("email.fromName", "Acme Billing")
                    .AddString("email.to", "you@yourcompany.com") // send the first test to yourself
                    .AddString("email.subject", "Invoice MIN-001")
                    .AddString("email.message", "Hello,\n\nPlease find attached invoice MIN-001.\n\nKind regards")
                )
                .ReceiveString();

            // {"status":"accepted","messageId":"..."}
            Console.WriteLine($"Email accepted for delivery: {answer}");
            return answer;
        }
        catch (FlurlHttpException ex)
        {
            var body = await ex.GetResponseStringAsync();
            Console.Error.WriteLine($"InvoiceXML API error {(int?)ex.StatusCode}: {body}");
            throw;
        }
    }
}
