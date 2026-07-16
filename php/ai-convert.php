<?php
// Experimental: convert a plain PDF invoice to ZUGFeRD using AI.
// Review output manually before sending to a customer or tax authority.
//
// Get API key: https://www.invoicexml.com/account/authentication
// Docs:        https://www.invoicexml.com/docs/api/transform/to/zugferd

// Raw key only, without the "Bearer " prefix (it is added below).
$apiKey = 'YOUR_API_KEY';

$ch = curl_init('https://api.invoicexml.com/v1/transform/to/zugferd');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST           => true,
    CURLOPT_POSTFIELDS     => [
        'file'     => new CURLFile('plain-invoice.pdf', 'application/pdf'),
        'version'  => '2.3.2',
        'profile'  => 'extended',
        'syntax'   => 'cii',
        'embed'    => 'true',
        'language' => 'en',
    ],
    CURLOPT_HTTPHEADER     => ['Authorization: Bearer ' . $apiKey],
]);

$pdf    = curl_exec($ch);
$status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

if ($status !== 200) {
    fwrite(STDERR, "InvoiceXML API error $status: $pdf\n");
    exit(1);
}

file_put_contents('converted-zugferd.pdf', $pdf);
echo "Saved converted-zugferd.pdf (" . strlen($pdf) . " bytes)\n";
echo "Review the output manually before sending.\n";
