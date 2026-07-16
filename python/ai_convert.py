# Experimental: convert a plain PDF invoice to ZUGFeRD using AI.
# Review output manually before sending to a customer or tax authority.
#
# Get API key: https://www.invoicexml.com/account/authentication
# Docs:        https://www.invoicexml.com/docs/api/transform/to/zugferd
#
# Dependency:  pip install requests

import requests
import sys

# Raw key only, without the "Bearer " prefix (it is added below).
api_key = "YOUR_API_KEY"

response = requests.post(
    "https://api.invoicexml.com/v1/transform/to/zugferd",
    headers={"Authorization": f"Bearer {api_key}"},
    files={"file": ("plain-invoice.pdf", open("plain-invoice.pdf", "rb"), "application/pdf")},
    data={
        "version":  "2.3.2",
        "profile":  "extended",
        "syntax":   "cii",
        "embed":    "true",
        "language": "en",
    },
)

if not response.ok:
    sys.stderr.write(f"InvoiceXML API error {response.status_code}: {response.text}\n")
    sys.exit(1)

with open("converted-zugferd.pdf", "wb") as f:
    f.write(response.content)

print(f"Saved converted-zugferd.pdf ({len(response.content)} bytes)")
print("Review the output manually before sending.")
