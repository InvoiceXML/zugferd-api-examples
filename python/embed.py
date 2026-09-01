# Embed an existing UN/CEFACT CII XML into an existing PDF, producing a
# ZUGFeRD 2.x compliant PDF/A-3 hybrid invoice.
#
# Your PDF keeps its exact layout, fonts, and branding; the API promotes the
# container to PDF/A-3 and attaches the XML as factur-x.xml (the attachment
# name the ZUGFeRD specification prescribes since 2.2). The XML is validated
# with the full /v1/validate/zugferd rule set first, so a non-compliant
# invoice never leaves the API.
#
# Get API key: https://www.invoicexml.com/account/authentication
# Docs:        https://www.invoicexml.com/docs/api/embed/zugferd
#
# Dependency:  pip install requests

import requests
import sys

# Raw key only, without the "Bearer " prefix (it is added below).
api_key = "YOUR_API_KEY"

with open("invoice.pdf", "rb") as pdf, open("factur-x.xml", "rb") as xml:
    response = requests.post(
        "https://api.invoicexml.com/v1/embed/zugferd",
        headers={"Authorization": f"Bearer {api_key}"},
        files={
            "pdf": ("invoice.pdf", pdf, "application/pdf"),
            "xml": ("factur-x.xml", xml, "application/xml"),
        },
        # Optional. "true" packages the XML without EN 16931 business-rule
        # checks; structural checks (CII root, BT-24 profile URN, XSD) still run.
        data={"skipValidation": "false"},
    )

if not response.ok:
    # 400 with errorCode 4001 lists every failed business rule;
    # 4017 means BT-24 is not an official ZUGFeRD profile URN.
    sys.stderr.write(f"InvoiceXML API error {response.status_code}: {response.text}\n")
    sys.exit(1)

with open("invoice-zugferd.pdf", "wb") as f:
    f.write(response.content)

print(f"Saved invoice-zugferd.pdf ({len(response.content)} bytes)")
