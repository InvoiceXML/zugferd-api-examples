// Validate a ZUGFeRD PDF using the InvoiceXML API.
// Get API key: https://www.invoicexml.com/account/authentication
// Docs:        https://www.invoicexml.com/docs/api/validate/zugferd
//
// Requires Node.js 18+. No dependencies.

const fs = require('fs');

// Raw key only, without the "Bearer " prefix (it is added below).
const apiKey = 'YOUR_API_KEY';

(async () => {
    const form = new FormData();
    form.append('file', new Blob([fs.readFileSync('invoice.pdf')], { type: 'application/pdf' }), 'invoice.pdf');
    form.append('version', '2.3.2');
    form.append('profile', 'extended');

    const response = await fetch('https://api.invoicexml.com/v1/validate/zugferd', {
        method: 'POST',
        headers: { Authorization: `Bearer ${apiKey}` },
        body: form,
    });

    const report = await response.text();
    if (!response.ok) {
        console.error(`InvoiceXML API error ${response.status}: ${report}`);
        process.exit(1);
    }

    console.log(report);
})();
