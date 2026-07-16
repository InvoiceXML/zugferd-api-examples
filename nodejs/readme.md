# ZUGFeRD for Node.js: InvoiceXML API Examples

Node.js code samples for creating, validating, and extracting **ZUGFeRD** electronic invoices using the **[InvoiceXML API](https://www.invoicexml.com)**. Compatible with **Node.js 18 and later** (native `fetch` and `FormData`). Runs in Express, NestJS, Fastify, Koa, Hapi, Next.js API routes, AWS Lambda, Cloudflare Workers, Vercel Functions, or plain scripts. **Zero npm dependencies.**

For background on the ZUGFeRD standard itself (what it is, profiles, legal status), see the [main repository README](../README.md).

## Get your API key

Every example in this folder calls the InvoiceXML REST API. Sign up and generate a free API key here:

**→ [https://www.invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication)**

Pass it as a Bearer token on every request:

```
Authorization: Bearer YOUR_API_KEY
```

**Important:** set `apiKey` in the examples to the raw key only, without the `Bearer ` prefix. If your account page shows the full header value (e.g. `Bearer ixml_a1b2c3...`), copy only the part after `Bearer `. The code adds the prefix itself when building the `Authorization` header.

## Requirements

- **Node.js 18.0 or later** (current LTS recommended: Node 20 or 22)
- No npm packages, no `package.json` needed

The examples use Node's built-in global `fetch`, `FormData`, `Blob`, and `Buffer`. These have been stable in Node 18+. If you must support Node 16 or older, polyfill `fetch` with `node-fetch` and `FormData` with `form-data`.

## Files in this folder

| File | Operation | API endpoint |
|---|---|---|
| [`create.js`](./create.js) | Build a ZUGFeRD PDF/A-3 invoice with embedded EN 16931 XML | [`POST /v1/create/zugferd`](https://www.invoicexml.com/docs/api/create/zugferd) |
| [`validate.js`](./validate.js) | Validate a ZUGFeRD file against schematron rules | [`POST /v1/validate/zugferd`](https://www.invoicexml.com/docs/api/validate/zugferd) |
| [`extract-json.js`](./extract-json.js) | Extract ZUGFeRD invoice data as JSON | [`POST /v1/extract/json`](https://www.invoicexml.com/docs/api/extract/json) |
| [`extract-xml.js`](./extract-xml.js) | Extract the raw `factur-x.xml` from a ZUGFeRD PDF | [`POST /v1/extract/xml`](https://www.invoicexml.com/docs/api/extract/xml) |
| [`ai-convert.js`](./ai-convert.js) | (Experimental) Convert a plain PDF to ZUGFeRD with AI | [`POST /v1/transform/to/zugferd`](https://www.invoicexml.com/docs/api/transform/to/zugferd) |

Each file is standalone and runnable with `node create.js`. Open the file, replace `YOUR_API_KEY` with your real key, and execute.

> **Note on the snippets below:** they are excerpts from those files and assume `apiKey` (and `fs`) are already defined. They also use `await` at the top level, which is not valid in a plain CommonJS script; the full files wrap the calls in an `async` IIFE. When in doubt, copy the complete file.

---

## Create a ZUGFeRD invoice in Node.js

```js
const payload = {
    invoice: {
        invoiceNumber: 'MIN-001',
        issueDate:     '2026-05-18',
        currency:      'EUR',
        seller: {
            name:              'Acme',
            vatIdentifier:     'DE123456789',
            legalRegistration: { identifier: 'HRB 12345' },
            postalAddress: { line1: 'Hauptstraße 12', city: 'Berlin', postCode: '10115', country: 'DE' },
        },
        buyer: {
            name: 'Globex SAS',
            postalAddress: { line1: '15 rue de Rivoli', city: 'Paris', postCode: '75001', country: 'FR' },
        },
        paymentDetails: { paymentAccountIdentifier: 'DE89370400440532013000' },
        lines: [{
            quantity:       10,
            priceDetails:   { netPrice: 150.00 },
            vatInformation: { rate: 19.00 },
            item:           { name: 'Senior consulting' },
        }],
    },
};

const response = await fetch('https://api.invoicexml.com/v1/create/zugferd', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer ' + apiKey,
    },
    body: JSON.stringify(payload),
});

const buffer = Buffer.from(await response.arrayBuffer());
fs.writeFileSync('invoice-zugferd.pdf', buffer);
```

The response is a binary PDF/A-3 file with the ZUGFeRD XML already embedded.

[Full example: `create.js`](./create.js) | [API reference](https://www.invoicexml.com/docs/api/create/zugferd)

---

## Validate a ZUGFeRD file in Node.js

```js
const fs = require('fs');

const form = new FormData();
form.append('file', new Blob([fs.readFileSync('invoice.pdf')], { type: 'application/pdf' }), 'invoice.pdf');
form.append('version', '2.3.2');
form.append('profile', 'extended');

const response = await fetch('https://api.invoicexml.com/v1/validate/zugferd', {
    method: 'POST',
    headers: { Authorization: 'Bearer ' + apiKey },
    body: form,
});

console.log(await response.text());
```

Returns a JSON validation report listing any schematron rule failures (EN 16931 BR-* and BR-CO-* rules).

[Full example: `validate.js`](./validate.js) | [API reference](https://www.invoicexml.com/docs/api/validate/zugferd)

---

## Extract ZUGFeRD data as JSON in Node.js

Useful for feeding ZUGFeRD invoices into Express controllers, message queues, or any pipeline that prefers JSON over XML.

```js
const fs = require('fs');

const form = new FormData();
form.append('file', new Blob([fs.readFileSync('invoice.pdf')], { type: 'application/pdf' }), 'invoice.pdf');

const response = await fetch('https://api.invoicexml.com/v1/extract/json', {
    method: 'POST',
    headers: { Authorization: 'Bearer ' + apiKey },
    body: form,
});

const invoice = await response.json();
// The invoice fields sit at the root of the response.
console.log(invoice.seller.name, invoice.totals.grandTotalAmount);
```

[Full example: `extract-json.js`](./extract-json.js) | [API reference](https://www.invoicexml.com/docs/api/extract/json)

---

## Extract embedded XML from a ZUGFeRD PDF in Node.js

Returns the raw `factur-x.xml` payload (UN/CEFACT Cross-Industry Invoice syntax). Use this when you need the structured XML to feed an existing UBL or CII pipeline, EDI partner, or archival system.

[Full example: `extract-xml.js`](./extract-xml.js) | [API reference](https://www.invoicexml.com/docs/api/extract/xml)

---

## (Experimental) Convert a plain PDF to ZUGFeRD with AI

> **Experimental feature. Human verification required before any production use.**
>
> Real-world PDF invoices are often messy: scanned at low quality, irregularly formatted, multi-page, or missing fields that EN 16931 requires. AI extraction can make subtle mistakes that automated validators may not catch: wrong tax category codes, transposed amounts, missing seller VAT identifiers, incorrect currency formatting.
>
> Always review the output PDF before sending it to a customer or tax authority. See the [AI conversion notes in the main README](../README.md#ai-powered-pdf-to-zugferd-conversion-experimental).

[Full example: `ai-convert.js`](./ai-convert.js) | [API reference](https://www.invoicexml.com/docs/api/transform/to/zugferd)

---

## Framework integration

### Express

Return a ZUGFeRD invoice from a route handler:

```js
const express = require('express');
const app = express();

app.get('/invoices/:id/zugferd', async (req, res) => {
    const pdf = await createZugferd(req.params.id);
    res.setHeader('Content-Type', 'application/pdf');
    res.setHeader('Content-Disposition', `attachment; filename="invoice-${req.params.id}.pdf"`);
    res.send(pdf);
});
```

Store the API key in `process.env.INVOICEXML_API_KEY` and read it from `dotenv` or your platform's secret manager.

### NestJS

```ts
@Controller('invoices')
export class InvoiceController {
    @Get(':id/zugferd')
    @Header('Content-Type', 'application/pdf')
    async download(@Param('id') id: string, @Res() res: Response) {
        const pdf = await this.zugferdService.create(id);
        res.setHeader('Content-Disposition', `attachment; filename="invoice-${id}.pdf"`);
        res.send(pdf);
    }
}
```

### Next.js App Router

```ts
// app/api/invoices/[id]/zugferd/route.ts
export async function GET(req: Request, { params }: { params: { id: string } }) {
    const pdf = await createZugferd(params.id);
    return new Response(pdf, {
        headers: {
            'Content-Type': 'application/pdf',
            'Content-Disposition': `attachment; filename="invoice-${params.id}.pdf"`,
        },
    });
}
```

### Cloudflare Workers and Vercel Edge

The examples run on Workers and Edge runtimes unchanged because `fetch` and `FormData` are part of the runtime. Replace `fs.readFileSync` with an `await request.arrayBuffer()` from the inbound request and the same pattern works in serverless environments.

### AWS Lambda (Node.js 18+)

Lambda's Node.js 18+ runtime provides native `fetch` and `FormData`, so the examples run as-is.

---

## Common issues

- **`fetch is not defined`**: you are running Node 16 or older. Upgrade to Node 18+ (current LTS), or polyfill with `npm install node-fetch form-data` and import accordingly.
- **`HTTP 401 Unauthorized`**: API key missing or invalid. Generate one at [invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication) and confirm you are sending `Authorization: Bearer YOUR_API_KEY`. A frequent cause: setting `apiKey` to the whole `Bearer xxx` value, which sends `Bearer Bearer xxx`. Set the raw key only.
- **`HTTP 400 Bad Request` on Create**: a required field is missing or malformed. Frequent causes: `IssueDate` not in ISO format (`YYYY-MM-DD`), `Currency` not in ISO 4217 (`EUR`, `USD`), country codes not in ISO 3166-1 alpha-2 (`DE`, `FR`).
- **`ESM` vs `CommonJS`**: the examples use CommonJS (`require`). For ESM (`.mjs` extension or `"type": "module"` in `package.json`), swap `require('fs')` for `import fs from 'node:fs'` and you can use top-level await without the IIFE wrapper.
- **Relative file paths**: `fs.readFileSync('invoice.pdf')` resolves from the current working directory, not the script file. Use `__dirname` (CommonJS) or `import.meta.dirname` (ESM, Node 20.11+) for absolute paths.
- **Schematron BR-CO-* failures on Validate**: line totals do not match the header total, or tax category and tax percentage are inconsistent. Recompute totals before posting.

---

## Resources

- [Get an InvoiceXML API key](https://www.invoicexml.com/account/authentication)
- [Create ZUGFeRD API reference](https://www.invoicexml.com/docs/api/create/zugferd)
- [Validate ZUGFeRD API reference](https://www.invoicexml.com/docs/api/validate/zugferd)
- [Extract JSON API reference](https://www.invoicexml.com/docs/api/extract/json)
- [Extract XML API reference](https://www.invoicexml.com/docs/api/extract/xml)
- [Transform to ZUGFeRD API reference](https://www.invoicexml.com/docs/api/transform/to/zugferd)
- [Node.js fetch API documentation](https://nodejs.org/api/globals.html#fetch)
- [Main repository README](../README.md)
