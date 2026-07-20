# ZUGFeRD for JavaScript in the Browser: InvoiceXML API Examples

Browser-side JavaScript examples for creating, validating, and extracting **ZUGFeRD** electronic invoices using the **[InvoiceXML API](https://www.invoicexml.com)**. Self-contained HTML files using vanilla JavaScript with native `fetch` and `FormData`. Compatible with **modern browsers**: Chrome, Firefox, Safari, Edge. **No build step, no framework, no dependencies.**

For background on the ZUGFeRD standard itself (what it is, profiles, legal status), see the [main repository README](../README.md).

## ⚠️ Security warning: API keys in browser code

API keys embedded directly in browser-side JavaScript are visible to every visitor through View Source or DevTools. Anyone can extract the key and use it against your InvoiceXML quota.

These examples are appropriate for:

- **Local prototyping** and testing
- **Internal tools** behind your company's authentication
- **Single-user desktop apps** wrapped in Electron or Tauri

For **public-facing production websites**, route requests through your own backend so the API key stays server-side. See the [Node.js examples](../nodejs) or [PHP examples](../php) for server-side integration patterns.

## Get your API key

Sign up and generate a key here:

**→ [https://www.invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication)**

Set `apiKey` in the examples to the raw key only, without the `Bearer ` prefix. If your account page shows the full header value (e.g. `Bearer ixml_a1b2c3...`), copy only the part after `Bearer `. The code adds the prefix itself when building the `Authorization` header.

## Requirements

- Any modern browser:
  - Chrome 95+
  - Firefox 90+
  - Safari 14+
  - Edge 95+
- An InvoiceXML API key
- Optional: a local web server (`python -m http.server`, `npx serve`, etc.) for testing. The examples also work by opening the `.html` files directly via `file://`.

## Files in this folder

| File | Operation | API endpoint |
|---|---|---|
| [`create.html`](./create.html) | Build a ZUGFeRD PDF/A-3 invoice with embedded EN 16931 XML | [`POST /v1/create/zugferd`](https://www.invoicexml.com/docs/api/create/zugferd) |
| [`validate.html`](./validate.html) | Validate a ZUGFeRD file against schematron rules | [`POST /v1/validate/zugferd`](https://www.invoicexml.com/docs/api/validate/zugferd) |
| [`extract-json.html`](./extract-json.html) | Extract ZUGFeRD invoice data as JSON | [`POST /v1/extract/json`](https://www.invoicexml.com/docs/api/extract/json) |
| [`extract-xml.html`](./extract-xml.html) | Extract the raw `factur-x.xml` from a ZUGFeRD PDF | [`POST /v1/extract/xml`](https://www.invoicexml.com/docs/api/extract/xml) |
| [`ai-convert.html`](./ai-convert.html) | (Experimental) Convert a plain PDF to ZUGFeRD with AI | [`POST /v1/transform/to/zugferd`](https://www.invoicexml.com/docs/api/transform/to/zugferd) |

Each file is a complete, runnable HTML page. Open it in a browser, paste your API key into the `<script>` block, and click the button.

---

## Create a ZUGFeRD invoice in the browser

```html
<button id="run">Generate invoice</button>
<script>
const payload = {
    invoice: {
        invoiceNumber: 'MIN-001',
        issueDate:     '2026-05-18',
        currency:      'EUR',
        seller: { name: 'Acme', vatIdentifier: 'DE123456789', /* ... */ },
        buyer:  { name: 'Globex SAS', /* ... */ },
        paymentDetails: { paymentAccountIdentifier: 'DE89370400440532013000' },
        lines: [{ quantity: 10, priceDetails: { netPrice: 150.00 },
                  vatInformation: { rate: 19.00 }, item: { name: 'Senior consulting' } }],
    },
};

document.getElementById('run').addEventListener('click', async () => {
    const response = await fetch('https://api.invoicexml.com/v1/create/zugferd', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            Authorization: 'Bearer ' + apiKey,
        },
        body: JSON.stringify(payload),
    });
    const blob = await response.blob();
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'invoice-zugferd.pdf';
    a.click();
});
</script>
```

The PDF is downloaded directly to the user's machine via `URL.createObjectURL` and a triggered `<a download>`.

[Full example: `create.html`](./create.html) | [API reference](https://www.invoicexml.com/docs/api/create/zugferd)

---

## Validate a ZUGFeRD file in the browser

```html
<input type="file" id="file" accept="application/pdf">
<button id="run">Validate</button>
<pre id="output"></pre>

<script>
document.getElementById('run').addEventListener('click', async () => {
    const file = document.getElementById('file').files[0];
    const form = new FormData();
    form.append('file', file);
    form.append('version', '2.3.2');
    form.append('profile', 'extended');

    const response = await fetch('https://api.invoicexml.com/v1/validate/zugferd', {
        method: 'POST',
        headers: { Authorization: 'Bearer ' + apiKey },
        body: form,
    });
    document.getElementById('output').textContent = await response.text();
});
</script>
```

The file is read directly from the `<input type="file">` element. No FileReader or Blob conversion needed: `FormData.append(file)` accepts the `File` object directly.

[Full example: `validate.html`](./validate.html) | [API reference](https://www.invoicexml.com/docs/api/validate/zugferd)

---

## Extract ZUGFeRD data as JSON in the browser

Useful for displaying invoice details in a web UI, populating form fields from an uploaded ZUGFeRD PDF, or building drag-and-drop invoice review tools.

```js
const form = new FormData();
form.append('file', fileInput.files[0]);

const response = await fetch('https://api.invoicexml.com/v1/extract/json', {
    method: 'POST',
    headers: { Authorization: 'Bearer ' + apiKey },
    body: form,
});
// The invoice document sits under the "invoice" key of the response.
const { invoice } = await response.json();
```

[Full example: `extract-json.html`](./extract-json.html) | [API reference](https://www.invoicexml.com/docs/api/extract/json) | [Sample response](../json/extract-json-response.json)

---

## Extract embedded XML from a ZUGFeRD PDF in the browser

Returns the raw `factur-x.xml` payload (UN/CEFACT Cross-Industry Invoice syntax). The HTML example also generates a download link so the user can save the XML file locally.

[Full example: `extract-xml.html`](./extract-xml.html) | [API reference](https://www.invoicexml.com/docs/api/extract/xml)

---

## (Experimental) Convert a plain PDF to ZUGFeRD with AI

> **Experimental feature. Human verification required before any production use.**
>
> AI extraction can make subtle mistakes that automated validators may not catch: wrong tax category codes, transposed amounts, missing seller VAT identifiers, incorrect currency formatting. Always review the output PDF before sending it to a customer or tax authority. See the [AI conversion notes in the main README](../README.md#ai-powered-pdf-to-zugferd-conversion-experimental).

[Full example: `ai-convert.html`](./ai-convert.html) | [API reference](https://www.invoicexml.com/docs/api/transform/to/zugferd)

---

## Framework integration

The patterns below assume the API key is held server-side and proxied through your backend. Replace `/api/zugferd/...` with your own proxy endpoints.

### React

```jsx
function CreateZugferdButton() {
    const [status, setStatus] = useState('');

    async function handleClick() {
        setStatus('Generating...');
        const response = await fetch('/api/zugferd/create', { method: 'POST' });
        const blob = await response.blob();
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = 'invoice-zugferd.pdf';
        a.click();
        setStatus('Done');
    }

    return <button onClick={handleClick}>Generate invoice</button>;
}
```

### Vue 3

```vue
<template>
    <button @click="handleClick">Generate invoice</button>
    <p>{{ status }}</p>
</template>

<script setup>
import { ref } from 'vue';

const status = ref('');

async function handleClick() {
    status.value = 'Generating...';
    const response = await fetch('/api/zugferd/create', { method: 'POST' });
    const blob = await response.blob();
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = 'invoice-zugferd.pdf';
    a.click();
    status.value = 'Done';
}
</script>
```

### Svelte

```svelte
<script>
    let status = '';

    async function handleClick() {
        status = 'Generating...';
        const response = await fetch('/api/zugferd/create', { method: 'POST' });
        const blob = await response.blob();
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = 'invoice-zugferd.pdf';
        a.click();
        status = 'Done';
    }
</script>

<button on:click={handleClick}>Generate invoice</button>
<p>{status}</p>
```

### Angular

Use `HttpClient` with `responseType: 'blob'`:

```typescript
this.http.post('/api/zugferd/create', payload, { responseType: 'blob' })
    .subscribe(blob => {
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = 'invoice-zugferd.pdf';
        a.click();
    });
```

---

## Common issues

- **CORS errors in the browser console**: if the browser blocks the request, route through your own backend (proxy) instead of calling the API directly from the browser.
- **API key visible in DevTools**: this is expected when the key is embedded in client-side code. For anything beyond local prototyping, move the call to a server-side proxy.
- **`HTTP 401 Unauthorized`**: API key missing or invalid. Generate one at [invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication). A frequent cause: setting `apiKey` to the whole `Bearer xxx` value, which sends `Bearer Bearer xxx`. Set the raw key only.
- **`HTTP 400 Bad Request` on Create**: a required field is missing or malformed. Frequent causes: `IssueDate` not in ISO format (`YYYY-MM-DD`), `Currency` not in ISO 4217 (`EUR`, `USD`), country codes not in ISO 3166-1 alpha-2 (`DE`, `FR`).
- **File input always empty**: `fileInput.files[0]` is `undefined` until the user actually picks a file. Always check before reading.
- **Mixed content warnings**: if your page is served over `https://`, all API calls must also be `https://`. The examples use `https://api.invoicexml.com` so this is fine, just do not change it to `http://`.
- **`file://` protocol limitations**: opening the HTML directly via `file://` works for these examples but breaks some browser APIs in stricter modes. Use a local server (`python -m http.server`, `npx serve`) if you hit issues.

---

## Resources

- [Get an InvoiceXML API key](https://www.invoicexml.com/account/authentication)
- [Create ZUGFeRD API reference](https://www.invoicexml.com/docs/api/create/zugferd)
- [Validate ZUGFeRD API reference](https://www.invoicexml.com/docs/api/validate/zugferd)
- [Extract JSON API reference](https://www.invoicexml.com/docs/api/extract/json)
- [Extract XML API reference](https://www.invoicexml.com/docs/api/extract/xml)
- [Transform to ZUGFeRD API reference](https://www.invoicexml.com/docs/api/transform/to/zugferd)
- [MDN: Fetch API](https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API)
- [MDN: FormData](https://developer.mozilla.org/en-US/docs/Web/API/FormData)
- [Main repository README](../README.md)
