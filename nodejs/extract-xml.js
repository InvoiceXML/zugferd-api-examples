// Extract the raw factur-x.xml from a ZUGFeRD PDF using the InvoiceXML API.
// Get API key: https://www.invoicexml.com/account/authentication
// Docs:        https://www.invoicexml.com/docs/api/extract/xml
//
// Requires Node.js 18+. No dependencies.

const fs = require('fs');

// Raw key only, without the "Bearer " prefix (it is added below).
const apiKey = 'YOUR_API_KEY';

(async () => {
    const form = new FormData();
    form.append('file', new Blob([fs.readFileSync('invoice.pdf')], { type: 'application/pdf' }), 'invoice.pdf');

    const response = await fetch('https://api.invoicexml.com/v1/extract/xml', {
        method: 'POST',
        headers: { Authorization: `Bearer ${apiKey}` },
        body: form,
    });

    const xml = await response.text();
    if (!response.ok) {
        console.error(`InvoiceXML API error ${response.status}: ${xml}`);
        process.exit(1);
    }

    fs.writeFileSync('factur-x.xml', xml);
    console.log(`Saved factur-x.xml (${xml.length} chars)`);
})();
