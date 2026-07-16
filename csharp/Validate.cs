// Validate a ZUGFeRD PDF against the official EN 16931 schematron rules
// using the InvoiceXML REST API.
//
// Get your API key at: https://www.invoicexml.com/account/authentication
// Full API reference:  https://www.invoicexml.com/docs/api/validate/zugferd
//
// Required NuGet package:
//   dotnet add package Flurl.Http

using Flurl.Http;

public static class ValidateZugferd
{
    private const string Endpoint = "https://api.invoicexml.com/v1/validate/zugferd";

    public static async Task<string> RunAsync(
        string pdfPath = "invoice.pdf",
        string version = "2.3.2",
        string profile = "extended")
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

        try
        {
            // POST the PDF as multipart/form-data alongside version and profile.
            // The response is a JSON validation report. Response structure:
            // https://www.invoicexml.com/docs/api/validate/zugferd
            var report = await Endpoint
                .WithOAuthBearerToken(apiKey)
                .PostMultipartAsync(mp => mp
                    .AddFile("file", pdfPath, "application/pdf")
                    .AddString("version", version)
                    .AddString("profile", profile)
                )
                .ReceiveString();

            Console.WriteLine(report);
            return report;
        }
        catch (FlurlHttpException ex)
        {
            var body = await ex.GetResponseStringAsync();
            Console.Error.WriteLine($"InvoiceXML API error {(int?)ex.StatusCode}: {body}");
            throw;
        }
    }
}
