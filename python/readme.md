# ZUGFeRD for Python: InvoiceXML API Examples

Python code samples for creating, validating, and extracting **ZUGFeRD** electronic invoices using the **[InvoiceXML API](https://www.invoicexml.com)**. Compatible with **Python 3.7+** (3.10+ recommended). Runs in Django, Flask, FastAPI, Pandas pipelines, Jupyter notebooks, AWS Lambda, Google Cloud Functions, Azure Functions, or plain scripts.

For background on the ZUGFeRD standard itself (what it is, profiles, legal status), see the [main repository README](../README.md).

## Get your API key

Every example in this folder calls the InvoiceXML REST API. Sign up and generate a free API key here:

**→ [https://www.invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication)**

Pass it as a Bearer token on every request:

```
Authorization: Bearer YOUR_API_KEY
```

**Important:** set `api_key` in the examples to the raw key only, without the `Bearer ` prefix. If your account page shows the full header value (e.g. `Bearer ixml_a1b2c3...`), copy only the part after `Bearer `. The code adds the prefix itself when building the `Authorization` header.

## Requirements

- **Python 3.7 or later** (3.10+ recommended)
- The **`requests`** library

```bash
pip install requests
```

Or with [uv](https://github.com/astral-sh/uv) (the modern alternative):

```bash
uv pip install requests
```

`requests` is the standard HTTP client for Python: clean multipart support, automatic JSON decoding, and the most familiar API in the ecosystem. If you prefer the modern async alternative, `httpx` translates almost line-for-line.

## Files in this folder

| File | Operation | API endpoint |
|---|---|---|
| [`create.py`](./create.py) | Build a ZUGFeRD PDF/A-3 invoice with embedded EN 16931 XML | [`POST /v1/create/zugferd`](https://www.invoicexml.com/docs/api/create/zugferd) |
| [`validate.py`](./validate.py) | Validate a ZUGFeRD file against schematron rules | [`POST /v1/validate/zugferd`](https://www.invoicexml.com/docs/api/validate/zugferd) |
| [`extract_json.py`](./extract_json.py) | Extract ZUGFeRD invoice data as JSON | [`POST /v1/extract/json`](https://www.invoicexml.com/docs/api/extract/json) |
| [`extract_xml.py`](./extract_xml.py) | Extract the raw `factur-x.xml` from a ZUGFeRD PDF | [`POST /v1/extract/xml`](https://www.invoicexml.com/docs/api/extract/xml) |
| [`embed.py`](./embed.py) | Embed your own CII XML into your own PDF as a ZUGFeRD PDF/A-3 | [`POST /v1/embed/zugferd`](https://www.invoicexml.com/docs/api/embed/zugferd) |

Each file is standalone and runnable with `python create.py`. Open the file, replace `YOUR_API_KEY` with your real key, and execute.

> **Note on the snippets below:** they are excerpts from those files and assume `api_key` is already defined. When in doubt, copy the complete file.

---

## Create a ZUGFeRD invoice in Python

```python
import requests

payload = {
    "invoice": {
        "invoiceNumber": "MIN-001",
        "issueDate":     "2026-05-18",
        "currency":      "EUR",
        "seller": {
            "name":              "Acme",
            "vatIdentifier":     "DE123456789",
            "legalRegistration": {"identifier": "HRB 12345"},
            "postalAddress": {"line1": "Hauptstraße 12", "city": "Berlin", "postCode": "10115", "country": "DE"},
        },
        "buyer": {
            "name": "Globex SAS",
            "postalAddress": {"line1": "15 rue de Rivoli", "city": "Paris", "postCode": "75001", "country": "FR"},
        },
        "paymentDetails": {"paymentAccountIdentifier": "DE89370400440532013000"},
        "lines": [{
            "quantity":       10,
            "priceDetails":   {"netPrice": 150.00},
            "vatInformation": {"rate": 19.00},
            "item":           {"name": "Senior consulting"},
        }],
    },
}

response = requests.post(
    "https://api.invoicexml.com/v1/create/zugferd",
    headers={"Authorization": f"Bearer {api_key}"},
    json=payload,
)

with open("invoice-zugferd.pdf", "wb") as f:
    f.write(response.content)
```

The response is a binary PDF/A-3 file with the ZUGFeRD XML already embedded.

[Full example: `create.py`](./create.py) | [API reference](https://www.invoicexml.com/docs/api/create/zugferd)

---

## Validate a ZUGFeRD file in Python

```python
import requests

response = requests.post(
    "https://api.invoicexml.com/v1/validate/zugferd",
    headers={"Authorization": f"Bearer {api_key}"},
    files={"file": ("invoice.pdf", open("invoice.pdf", "rb"), "application/pdf")},
    data={"version": "2.3.2", "profile": "extended"},
)
print(response.text)
```

Returns a JSON validation report listing any schematron rule failures (EN 16931 BR-* and BR-CO-* rules).

[Full example: `validate.py`](./validate.py) | [API reference](https://www.invoicexml.com/docs/api/validate/zugferd)

---

## Extract ZUGFeRD data as JSON in Python

Useful for Pandas pipelines, Django/Flask models, or any system that prefers JSON over XML.

```python
import requests

response = requests.post(
    "https://api.invoicexml.com/v1/extract/json",
    headers={"Authorization": f"Bearer {api_key}"},
    files={"file": ("invoice.pdf", open("invoice.pdf", "rb"), "application/pdf")},
)

# The invoice document sits under the "invoice" key of the response.
invoice = response.json()["invoice"]
print(invoice["seller"]["name"], invoice["totals"]["grandTotalAmount"])
```

[Full example: `extract_json.py`](./extract_json.py) | [API reference](https://www.invoicexml.com/docs/api/extract/json) | [Sample response](../json/extract-json-response.json)

---

## Extract embedded XML from a ZUGFeRD PDF in Python

Returns the raw `factur-x.xml` payload (UN/CEFACT Cross-Industry Invoice syntax). Use this when you need the structured XML to feed an existing UBL or CII pipeline, EDI partner, or archival system.

```python
import requests

response = requests.post(
    "https://api.invoicexml.com/v1/extract/xml",
    headers={"Authorization": f"Bearer {api_key}"},
    files={"file": ("invoice.pdf", open("invoice.pdf", "rb"), "application/pdf")},
)

with open("factur-x.xml", "w", encoding="utf-8") as f:
    f.write(response.text)
```

[Full example: `extract_xml.py`](./extract_xml.py) | [API reference](https://www.invoicexml.com/docs/api/extract/xml)

---

## Embed your own XML into your own PDF

When your application already renders the invoice PDF and already produces the EN 16931 XML, post both files and the API keeps your visual layer exactly as designed, promotes the container to PDF/A-3, and attaches the XML as `factur-x.xml` (the attachment name the ZUGFeRD specification prescribes since version 2.2) with the German (FeRD) AFRelationship and XMP conventions.

```python
import requests

with open("invoice.pdf", "rb") as pdf, open("factur-x.xml", "rb") as xml:
    response = requests.post(
        "https://api.invoicexml.com/v1/embed/zugferd",
        headers={"Authorization": f"Bearer {api_key}"},
        files={
            "pdf": ("invoice.pdf", pdf, "application/pdf"),
            "xml": ("factur-x.xml", xml, "application/xml"),
        },
        data={"skipValidation": "false"},
    )

with open("invoice-zugferd.pdf", "wb") as f:
    f.write(response.content)
```

The XML runs through the complete `/v1/validate/zugferd` rule set before anything is embedded, so a non-compliant invoice never leaves the API: fatal findings come back as a 400 with `errorCode` 4001 and the full finding list. Set `skipValidation` to `"true"` for packaging-only mode, where the structural checks (CII root element, official BT-24 profile URN, profile XSD) still apply but the business rules are skipped.

Only UN/CEFACT CII XML is accepted. If your invoice is UBL, convert it first with [`POST /v1/convert/ubl/to/cii`](https://www.invoicexml.com/docs/api/convert/ubl/to/cii). For the French (FNFE-MPE) packaging conventions, call `/v1/embed/facturx` instead, same request shape.

[Full example: `embed.py`](./embed.py) | [API reference](https://www.invoicexml.com/docs/api/embed/zugferd)

---

## Framework integration

### Django

Return a ZUGFeRD invoice from a view:

```python
from django.http import HttpResponse

def download_zugferd(request, invoice_id):
    pdf_bytes = create_zugferd_for(invoice_id)
    response = HttpResponse(pdf_bytes, content_type="application/pdf")
    response["Content-Disposition"] = f'attachment; filename="invoice-{invoice_id}.pdf"'
    return response
```

Store the API key in `settings.py` via `INVOICEXML_API_KEY = os.environ["INVOICEXML_API_KEY"]`.

### Flask

```python
from flask import send_file
from io import BytesIO

@app.route("/invoices/<int:invoice_id>/zugferd")
def get_zugferd(invoice_id):
    pdf_bytes = create_zugferd_for(invoice_id)
    return send_file(BytesIO(pdf_bytes), mimetype="application/pdf",
                     as_attachment=True, download_name=f"invoice-{invoice_id}.pdf")
```

### FastAPI

```python
from fastapi.responses import Response

@app.get("/invoices/{invoice_id}/zugferd")
async def get_zugferd(invoice_id: int):
    pdf_bytes = create_zugferd_for(invoice_id)
    return Response(
        content=pdf_bytes,
        media_type="application/pdf",
        headers={"Content-Disposition": f'attachment; filename="invoice-{invoice_id}.pdf"'},
    )
```

### Pandas and data pipelines

The `extract_json.py` example fits naturally into a Pandas pipeline: walk a folder of ZUGFeRD PDFs, extract each to JSON, and load into a DataFrame for analysis or bulk archival.

```python
import pandas as pd, requests, os

rows = []
for pdf in os.listdir("invoices/"):
    with open(f"invoices/{pdf}", "rb") as f:
        data = requests.post(
            "https://api.invoicexml.com/v1/extract/json",
            headers={"Authorization": f"Bearer {api_key}"},
            files={"file": (pdf, f, "application/pdf")},
        ).json()
    rows.append(data)

df = pd.DataFrame(rows)
```

### AWS Lambda

The `requests` library works in Lambda directly. Include it via Lambda layers or in your deployment zip.

---

## Common issues

- **`HTTP 401 Unauthorized`**: API key missing or invalid. Generate one at [invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication) and confirm you are sending `Authorization: Bearer YOUR_API_KEY`. A frequent cause: setting `api_key` to the whole `Bearer xxx` value, which sends `Bearer Bearer xxx`. Set the raw key only.
- **`HTTP 400 Bad Request` on Create**: a required field is missing or malformed. Frequent causes: `IssueDate` not in ISO format (`YYYY-MM-DD`), `Currency` not in ISO 4217 (`EUR`, `USD`), country codes not in ISO 3166-1 alpha-2 (`DE`, `FR`).
- **`SSL: CERTIFICATE_VERIFY_FAILED` on macOS**: run `/Applications/Python\ 3.x/Install\ Certificates.command` to install Python's CA bundle. Do not disable SSL verification in production.
- **`ConnectionError` or timeout**: add an explicit `timeout=30` parameter to `requests.post()`. AI conversion in particular can take 10 to 30 seconds for larger PDFs.
- **File handle warnings**: the examples use inline `open()` for brevity. For production code, prefer `with open(...) as f:` to ensure file handles close deterministically.
- **Schematron BR-CO-* failures on Validate**: line totals do not match the header total, or tax category and tax percentage are inconsistent. Recompute totals before posting.

---

## Resources

- [Get an InvoiceXML API key](https://www.invoicexml.com/account/authentication)
- [Create ZUGFeRD API reference](https://www.invoicexml.com/docs/api/create/zugferd)
- [Validate ZUGFeRD API reference](https://www.invoicexml.com/docs/api/validate/zugferd)
- [Extract JSON API reference](https://www.invoicexml.com/docs/api/extract/json)
- [Extract XML API reference](https://www.invoicexml.com/docs/api/extract/xml)
- [Embed into ZUGFeRD API reference](https://www.invoicexml.com/docs/api/embed/zugferd)
- [requests library documentation](https://requests.readthedocs.io/)
- [Main repository README](../README.md)
