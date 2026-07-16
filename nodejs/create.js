// Create a ZUGFeRD PDF/A-3 invoice using the InvoiceXML API.
// Sends a JSON invoice model and receives a PDF.
//
// Get API key: https://www.invoicexml.com/account/authentication
// Docs:        https://www.invoicexml.com/docs/api/create/zugferd
//
// Requires Node.js 18+. No dependencies.

const fs = require('fs');

// Raw key only, without the "Bearer " prefix (it is added below).
const apiKey = 'YOUR_API_KEY';

const payload = {
    invoice: {
        invoiceNumber: 'MIN-001',
        issueDate:     '2026-05-18',
        currency:      'EUR',
        seller: {
            name:              'Acme',
            vatIdentifier:     'DE123456789',
            legalRegistration: { identifier: 'HRB 12345' },
            postalAddress: {
                line1:    'Hauptstraße 12',
                city:     'Berlin',
                postCode: '10115',
                country:  'DE',
            },
        },
        buyer: {
            name: 'Globex SAS',
            postalAddress: {
                line1:    '15 rue de Rivoli',
                city:     'Paris',
                postCode: '75001',
                country:  'FR',
            },
        },
        paymentDetails: { paymentAccountIdentifier: 'DE89370400440532013000' },
        lines: [
            {
                quantity:       10,
                priceDetails:   { netPrice: 150.00 },
                vatInformation: { rate: 19.00 },
                item:           { name: 'Senior consulting' },
            },
        ],
    },
};

(async () => {
    const response = await fetch('https://api.invoicexml.com/v1/create/zugferd', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            Authorization: 'Bearer ' + apiKey,
        },
        body: JSON.stringify(payload),
    });

    if (!response.ok) {
        console.error(`InvoiceXML API error ${response.status}: ${await response.text()}`);
        process.exit(1);
    }

    const buffer = Buffer.from(await response.arrayBuffer());
    fs.writeFileSync('invoice-zugferd.pdf', buffer);
    console.log(`Saved invoice-zugferd.pdf (${buffer.length} bytes)`);
})();
