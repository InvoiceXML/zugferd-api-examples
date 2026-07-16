# Extract the raw factur-x.xml from a ZUGFeRD PDF using the InvoiceXML API.
# Get API key: https://www.invoicexml.com/account/authentication
# Docs:        https://www.invoicexml.com/docs/api/extract/xml
#
# Dependency:  pip install requests

import requests
import sys

# Raw key only, without the "Bearer " prefix (it is added below).
api_key = "YOUR_API_KEY"

response = requests.post(
    "https://api.invoicexml.com/v1/extract/xml",
    headers={"Authorization": f"Bearer {api_key}"},
    files={"file": ("invoice.pdf", open("invoice.pdf", "rb"), "application/pdf")},
)

if not response.ok:
    sys.stderr.write(f"InvoiceXML API error {response.status_code}: {response.text}\n")
    sys.exit(1)

with open("factur-x.xml", "w", encoding="utf-8") as f:
    f.write(response.text)

print(f"Saved factur-x.xml ({len(response.text)} chars)")
