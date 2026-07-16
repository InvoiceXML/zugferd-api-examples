// Extract structured invoice data from a ZUGFeRD PDF as JSON using the
// InvoiceXML REST API. Useful for feeding ZUGFeRD invoices into REST
// pipelines, ERPs, or any system that prefers JSON over XML.
//
// Get your API key at: https://www.invoicexml.com/account/authentication
// Full API reference:  https://www.invoicexml.com/docs/api/extract/json
//
// Required NuGet package:
//   dotnet add package Flurl.Http

using Flurl.Http;

public static class ExtractJson
{
    private const string Endpoint = "https://api.invoicexml.com/v1/extract/json";

    public static async Task<string> RunAsync(
        string pdfPath = "invoice.pdf",
        string outputPath = "invoice.json")
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
            // POST the PDF and receive a JSON document containing the parsed
            // EN 16931 invoice fields (seller, buyer, lines, tax breakdown, totals).
            // Response schema:
            // https://www.invoicexml.com/docs/api/extract/json
            var json = await Endpoint
                .WithOAuthBearerToken(apiKey)
                .PostMultipartAsync(mp => mp
                    .AddFile("file", pdfPath, "application/pdf"))
                .ReceiveString();

            await File.WriteAllTextAsync(outputPath, json);
            Console.WriteLine($"ZUGFeRD data extracted to {outputPath}");
            Console.WriteLine(json);
            return json;
        }
        catch (FlurlHttpException ex)
        {
            var body = await ex.GetResponseStringAsync();
            Console.Error.WriteLine($"InvoiceXML API error {(int?)ex.StatusCode}: {body}");
            throw;
        }
    }
}
