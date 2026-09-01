// Embed an existing UN/CEFACT CII XML into an existing PDF, producing a
// ZUGFeRD 2.x compliant PDF/A-3 hybrid invoice.
//
// Your PDF keeps its exact layout, fonts, and branding; the API promotes the
// container to PDF/A-3 and attaches the XML as factur-x.xml (the attachment
// name the ZUGFeRD specification prescribes since 2.2). The XML is validated
// with the full /v1/validate/zugferd rule set first, so a non-compliant
// invoice never leaves the API.
//
// Get API key: https://www.invoicexml.com/account/authentication
// Docs:        https://www.invoicexml.com/docs/api/embed/zugferd
//
// Requires Node.js 18+. No dependencies.

const fs = require('fs');

// Raw key only, without the "Bearer " prefix (it is added below).
const apiKey = 'YOUR_API_KEY';

(async () => {
    const form = new FormData();
    form.append('pdf', new Blob([fs.readFileSync('invoice.pdf')], { type: 'application/pdf' }), 'invoice.pdf');
    form.append('xml', new Blob([fs.readFileSync('factur-x.xml')], { type: 'application/xml' }), 'factur-x.xml');
    // Optional. 'true' packages the XML without EN 16931 business-rule checks;
    // structural checks (CII root, BT-24 profile URN, XSD) still run.
    form.append('skipValidation', 'false');

    const response = await fetch('https://api.invoicexml.com/v1/embed/zugferd', {
        method: 'POST',
        headers: { Authorization: `Bearer ${apiKey}` },
        body: form,
    });

    if (!response.ok) {
        // 400 with errorCode 4001 lists every failed business rule;
        // 4017 means BT-24 is not an official ZUGFeRD profile URN.
        console.error(`InvoiceXML API error ${response.status}: ${await response.text()}`);
        process.exit(1);
    }

    const buffer = Buffer.from(await response.arrayBuffer());
    fs.writeFileSync('invoice-zugferd.pdf', buffer);
    console.log(`Saved invoice-zugferd.pdf (${buffer.length} bytes)`);
})();
