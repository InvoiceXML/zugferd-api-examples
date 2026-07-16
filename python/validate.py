# Validate a ZUGFeRD PDF using the InvoiceXML API.
# Get API key: https://www.invoicexml.com/account/authentication
# Docs:        https://www.invoicexml.com/docs/api/validate/zugferd
#
# Dependency:  pip install requests

import requests
import sys

# Raw key only, without the "Bearer " prefix (it is added below).
api_key = "YOUR_API_KEY"

response = requests.post(
    "https://api.invoicexml.com/v1/validate/zugferd",
    headers={"Authorization": f"Bearer {api_key}"},
    files={"file": ("invoice.pdf", open("invoice.pdf", "rb"), "application/pdf")},
    data={"version": "2.3.2", "profile": "extended"},
)

if not response.ok:
    sys.stderr.write(f"InvoiceXML API error {response.status_code}: {response.text}\n")
    sys.exit(1)

print(response.text)
