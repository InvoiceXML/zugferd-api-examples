# Embed a CII XML into your own PDF and email the result, in one call.
# Builds the hybrid PDF exactly like POST /v1/embed/zugferd, always with
# full validation, then sends it as an attachment from your own domain.
# This sends a real email. The answer is the message id, not the file.
#
# Before the first run, add and verify your domain:
# https://www.invoicexml.com/account/sending-domains
# Your plan must include email delivery: https://www.invoicexml.com/pricing
#
# Get API key: https://www.invoicexml.com/account/authentication
# Docs:        https://www.invoicexml.com/docs/api/embed/zugferd/email
#
# Dependency:  pip install requests

import requests
import sys

# Raw key only, without the "Bearer " prefix (it is added below).
api_key = "YOUR_API_KEY"

with open("invoice.pdf", "rb") as pdf, open("factur-x.xml", "rb") as xml:
    response = requests.post(
        "https://api.invoicexml.com/v1/embed/zugferd/email",
        headers={"Authorization": f"Bearer {api_key}"},
        files={
            "pdf": ("invoice.pdf", pdf, "application/pdf"),
            "xml": ("factur-x.xml", xml, "application/xml"),
        },
        data={
            "email.from":     "invoices@yourcompany.com",  # any address on your verified domain
            "email.fromName": "Acme Billing",
            "email.to":       "you@yourcompany.com",       # send the first test to yourself
            "email.subject":  "Invoice MIN-001",
            "email.message":  "Hello,\n\nPlease find attached invoice MIN-001.\n\nKind regards",
            # Optional: email.cc and email.bcc (repeat the field, 2 addresses together) and email.replyTo.
        },
    )

if not response.ok:
    # 400 with errorCode 4001 lists every failed business rule.
    sys.stderr.write(f"InvoiceXML API error {response.status_code}: {response.text}\n")
    sys.exit(1)

print(f"Email accepted for delivery, message id {response.json()['messageId']}")
