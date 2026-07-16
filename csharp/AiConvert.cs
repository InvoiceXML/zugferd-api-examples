// EXPERIMENTAL: Convert a plain (unstructured) PDF invoice into a fully
// compliant ZUGFeRD PDF/A-3 with embedded XML, using the InvoiceXML
// AI-powered transform API.
//
// WARNING: Real-world PDF invoices are often messy: scanned, irregularly
// formatted, multi-page, multilingual, or missing fields that EN 16931
// requires. AI extraction can make subtle mistakes (wrong tax codes,
// transposed amounts, missing VAT IDs, currency issues) that automated
// validators may not catch. ALWAYS review the output PDF before sending
// it to a customer or tax authority.
//
// Get your API key at: https://www.invoicexml.com/account/authentication
// Full API reference:  https://www.invoicexml.com/docs/api/transform/to/zugferd
//
// Required NuGet package:
//   dotnet add package Flurl.Http


using Flurl.Http;

public static class AiConvert
{
    private const string Endpoint = "https://api.invoicexml.com/v1/transform/to/zugferd";

    public static async Task<byte[]> RunAsync(
        string inputPdfPath = "plain-invoice.pdf",
        string outputPath = "converted-zugferd.pdf",
        string version = "2.3.2",
        string profile = "extended",
        string syntax = "cii",
        string language = "en")
    {
        // Get an InvoiceXML API key at: https://www.invoicexml.com/account/authentication
        // Raw key only, without the "Bearer " prefix (WithOAuthBearerToken adds it).
        var apiKey = Environment.GetEnvironmentVariable("INVOICEXML_API_KEY")
            ?? throw new InvalidOperationException(
                "Set INVOICEXML_API_KEY environment variable. " +
                "Get an InvoiceXML API key at " +
                "https://www.invoicexml.com/account/authentication");

        if (!File.Exists(inputPdfPath))
            throw new FileNotFoundException($"Input PDF not found: {inputPdfPath}");

        try
        {
            // POST the plain PDF; InvoiceXML extracts invoice fields with AI,
            // builds an EN 16931 XML payload, validates it, and embeds it
            // into a PDF/A-3 container. Parameter reference:
            // https://www.invoicexml.com/docs/api/transform/to/zugferd
            var pdfBytes = await Endpoint
                .WithOAuthBearerToken(apiKey)
                .PostMultipartAsync(mp => mp
                    .AddFile("file", inputPdfPath, "application/pdf")
                    .AddString("version",  version)   // e.g. "2.3.2"
                    .AddString("profile",  profile)   // minimum, basicwl, basic, en16931, extended
                    .AddString("syntax",   syntax)    // "cii" for ZUGFeRD / ZUGFeRD
                    .AddString("embed",    "true")    // embed XML into the returned PDF
                    .AddString("language", language)  // hint for AI extraction
                )
                .ReceiveBytes();

            await File.WriteAllBytesAsync(outputPath, pdfBytes);
            Console.WriteLine($"Converted ZUGFeRD saved: {outputPath} ({pdfBytes.Length:N0} bytes)");
            Console.WriteLine("REMINDER: review the output manually before sending to a customer or tax authority.");
            return pdfBytes;
        }
        catch (FlurlHttpException ex)
        {
            var body = await ex.GetResponseStringAsync();
            Console.Error.WriteLine($"InvoiceXML API error {(int?)ex.StatusCode}: {body}");
            throw;
        }
    }
}
