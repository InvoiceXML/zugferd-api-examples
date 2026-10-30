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
// Requires Node.js 18+. No dependencies.

const fs = require('fs');

// Raw key only, without the "Bearer " prefix (it is added below).
const apiKey = 'YOUR_API_KEY';

(async () => {
    const form = new FormData();
    form.append('pdf', new Blob([fs.readFileSync('invoice.pdf')], { type: 'application/pdf' }), 'invoice.pdf');
    form.append('xml', new Blob([fs.readFileSync('factur-x.xml')], { type: 'application/xml' }), 'factur-x.xml');
    form.append('email.from', 'invoices@yourcompany.com'); // any address on your verified domain
    form.append('email.fromName', 'Acme Billing');
    form.append('email.to', 'you@yourcompany.com'); // send the first test to yourself
    form.append('email.subject', 'Invoice MIN-001');
    form.append('email.message', 'Hello,\n\nPlease find attached invoice MIN-001.\n\nKind regards');
    // Optional: email.cc and email.bcc (repeat the field, 2 addresses together) and email.replyTo.

    const response = await fetch('https://api.invoicexml.com/v1/embed/zugferd/email', {
        method: 'POST',
        headers: { Authorization: `Bearer ${apiKey}` },
        body: form,
    });

    if (!response.ok) {
        // 400 with errorCode 4001 lists every failed business rule.
        console.error(`InvoiceXML API error ${response.status}: ${await response.text()}`);
        process.exit(1);
    }

    const { messageId } = await response.json();
    console.log(`Email accepted for delivery, message id ${messageId}`);
})();
