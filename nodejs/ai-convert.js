// Experimental: convert a plain PDF invoice to ZUGFeRD using AI.
// Review output manually before sending to a customer or tax authority.
//
// Get API key: https://www.invoicexml.com/account/authentication
// Docs:        https://www.invoicexml.com/docs/api/transform/to/zugferd
//
// Requires Node.js 18+. No dependencies.

const fs = require('fs');

// Raw key only, without the "Bearer " prefix (it is added below).
const apiKey = 'YOUR_API_KEY';

(async () => {
    const form = new FormData();
    form.append('file', new Blob([fs.readFileSync('plain-invoice.pdf')], { type: 'application/pdf' }), 'plain-invoice.pdf');
    form.append('version',  '2.3.2');
    form.append('profile',  'extended');
    form.append('syntax',   'cii');
    form.append('embed',    'true');
    form.append('language', 'en');

    const response = await fetch('https://api.invoicexml.com/v1/transform/to/zugferd', {
        method: 'POST',
        headers: { Authorization: `Bearer ${apiKey}` },
        body: form,
    });

    if (!response.ok) {
        console.error(`InvoiceXML API error ${response.status}: ${await response.text()}`);
        process.exit(1);
    }

    const buffer = Buffer.from(await response.arrayBuffer());
    fs.writeFileSync('converted-zugferd.pdf', buffer);
    console.log(`Saved converted-zugferd.pdf (${buffer.length} bytes)`);
    console.log('Review the output manually before sending.');
})();
