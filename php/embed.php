<?php
// Embed an existing UN/CEFACT CII XML into an existing PDF, producing a
// ZUGFeRD 2.x compliant PDF/A-3 hybrid invoice.
//
// Your PDF keeps its exact layout, fonts, and branding; the API promotes the
// container to PDF/A-3 and attaches the XML as factur-x.xml (the attachment
// name the ZUGFeRD specification prescribes since 2.2). The XML is validated
// with the full /v1/validate/zugferd rule set first, so a non-compliant
// invoice never leaves the API.
//
// Get API key: https://www.invoicexml.com/account/authentication
// Docs:        https://www.invoicexml.com/docs/api/embed/zugferd

// Raw key only, without the "Bearer " prefix (it is added below).
$apiKey = 'YOUR_API_KEY';

$ch = curl_init('https://api.invoicexml.com/v1/embed/zugferd');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST           => true,
    CURLOPT_POSTFIELDS     => [
        'pdf' => new CURLFile('invoice.pdf', 'application/pdf'),
        'xml' => new CURLFile('factur-x.xml', 'application/xml'),
        // Optional. 'true' packages the XML without EN 16931 business-rule
        // checks; structural checks (CII root, BT-24 URN, XSD) still run.
        'skipValidation' => 'false',
    ],
    CURLOPT_HTTPHEADER     => ['Authorization: Bearer ' . $apiKey],
]);

$pdf    = curl_exec($ch);
$status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

if ($status !== 200) {
    // 400 with errorCode 4001 lists every failed business rule;
    // 4017 means BT-24 is not an official ZUGFeRD profile URN.
    fwrite(STDERR, "InvoiceXML API error $status: $pdf\n");
    exit(1);
}

file_put_contents('invoice-zugferd.pdf', $pdf);
echo "Saved invoice-zugferd.pdf (" . strlen($pdf) . " bytes)\n";
