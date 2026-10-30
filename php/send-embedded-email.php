<?php
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

// Raw key only, without the "Bearer " prefix (it is added below).
$apiKey = 'YOUR_API_KEY';

$ch = curl_init('https://api.invoicexml.com/v1/embed/zugferd/email');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST           => true,
    CURLOPT_POSTFIELDS     => [
        'pdf' => new CURLFile('invoice.pdf', 'application/pdf'),
        'xml' => new CURLFile('factur-x.xml', 'application/xml'),
        'email.from'     => 'invoices@yourcompany.com', // any address on your verified domain
        'email.fromName' => 'Acme Billing',
        'email.to'       => 'you@yourcompany.com',      // send the first test to yourself
        'email.subject'  => 'Invoice MIN-001',
        'email.message'  => "Hello,\n\nPlease find attached invoice MIN-001.\n\nKind regards",
        // Optional: email.cc[0], email.cc[1], email.bcc[0] (2 addresses together) and email.replyTo.
    ],
    CURLOPT_HTTPHEADER     => ['Authorization: Bearer ' . $apiKey],
]);

$body   = curl_exec($ch);
$status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

if ($status !== 200) {
    fwrite(STDERR, "InvoiceXML API error $status: $body\n");
    exit(1);
}

$messageId = json_decode($body, true)['messageId'];
echo "Email accepted for delivery, message id $messageId\n";
