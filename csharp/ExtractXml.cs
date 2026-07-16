// Extract the raw factur-x.xml payload (UN/CEFACT Cross-Industry Invoice)
// from a ZUGFeRD PDF/A-3 using the InvoiceXML REST API.
//
// Useful when you need to forward the structured XML directly to an existing
// UBL or CII processing pipeline, EDI partner, or archival system.
//
// Get your API key at: https://www.invoicexml.com/account/authentication
// Full API reference:  https://www.invoicexml.com/docs/api/extract/xml
//
// Required NuGet package:
//   dotnet add package Flurl.Http

using Flurl.Http;

public static class ExtractXml
{
    private const string Endpoint = "https://api.invoicexml.com/v1/extract/xml";

    public static async Task<string> RunAsync(
        string pdfPath = "invoice.pdf",
        string outputPath = "factur-x.xml")
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
            // POST the PDF and receive the raw factur-x.xml content
            // (Cross-Industry Invoice syntax). InvoiceXML handles PDF/A-3
            // attachment extraction and AFRelationship lookup for you.
            var xml = await Endpoint
                .WithOAuthBearerToken(apiKey)
                .PostMultipartAsync(mp => mp
                    .AddFile("file", pdfPath, "application/pdf"))
                .ReceiveString();

            await File.WriteAllTextAsync(outputPath, xml);
            Console.WriteLine($"ZUGFeRD XML extracted to {outputPath} ({xml.Length:N0} chars)");
            return xml;
        }
        catch (FlurlHttpException ex)
        {
            var body = await ex.GetResponseStringAsync();
            Console.Error.WriteLine($"InvoiceXML API error {(int?)ex.StatusCode}: {body}");
            throw;
        }
    }
}
