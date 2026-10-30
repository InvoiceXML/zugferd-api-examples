# ZUGFeRD for PHP: InvoiceXML API Examples

PHP code samples for creating, validating, and extracting **ZUGFeRD** electronic invoices using the **[InvoiceXML API](https://www.invoicexml.com)**. Compatible with **PHP 7.0+** (PHP 8.x recommended), works in plain scripts, Laravel, Symfony, WordPress, WooCommerce, Drupal, Magento, or any other PHP framework. **Zero Composer dependencies**: uses PHP's built-in cURL extension.

For background on the ZUGFeRD standard itself (what it is, profiles, legal status), see the [main repository README](../README.md).

## Get your API key

Every example in this folder calls the InvoiceXML REST API. Sign up and generate a free API key here:

**→ [https://www.invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication)**

Pass it as a Bearer token on every request:

```
Authorization: Bearer YOUR_API_KEY
```

**Important:** set `$apiKey` in the examples to the raw key only, without the `Bearer ` prefix. If your account page shows the full header value (e.g. `Bearer ixml_a1b2c3...`), copy only the part after `Bearer `. The code adds the prefix itself when building the `Authorization` header.

## Requirements

- **PHP 7.0 or later** (PHP 8.2+ recommended)
- The **cURL extension** (`php-curl`), bundled with most PHP installations by default
- No Composer, no external libraries

These examples use the built-in `curl_*` functions and `CURLFile` (available since PHP 5.5), so they run on essentially any modern PHP install without `composer install`.

## Files in this folder

| File | Operation | API endpoint |
|---|---|---|
| [`create.php`](./create.php) | Build a ZUGFeRD PDF/A-3 invoice with embedded EN 16931 XML | [`POST /v1/create/zugferd`](https://www.invoicexml.com/docs/api/create/zugferd) |
| [`send-email.php`](./send-email.php) | Create a ZUGFeRD invoice and email it from your own domain | [`POST /v1/create/zugferd/email`](https://www.invoicexml.com/docs/api/create/zugferd/email) |
| [`validate.php`](./validate.php) | Validate a ZUGFeRD file against schematron rules | [`POST /v1/validate/zugferd`](https://www.invoicexml.com/docs/api/validate/zugferd) |
| [`extract-json.php`](./extract-json.php) | Extract ZUGFeRD invoice data as JSON | [`POST /v1/extract/json`](https://www.invoicexml.com/docs/api/extract/json) |
| [`extract-xml.php`](./extract-xml.php) | Extract the raw `factur-x.xml` from a ZUGFeRD PDF | [`POST /v1/extract/xml`](https://www.invoicexml.com/docs/api/extract/xml) |
| [`embed.php`](./embed.php) | Embed your own CII XML into your own PDF as a ZUGFeRD PDF/A-3 | [`POST /v1/embed/zugferd`](https://www.invoicexml.com/docs/api/embed/zugferd) |
| [`send-embedded-email.php`](./send-embedded-email.php) | Embed your own CII XML into your own PDF and email it from your own domain | [`POST /v1/embed/zugferd/email`](https://www.invoicexml.com/docs/api/embed/zugferd/email) |

Each file is standalone and runnable with `php create.php`. Open the file, replace `YOUR_API_KEY` with your real key, and execute.

> **Note on the snippets below:** they are excerpts from those files and assume `$apiKey` is already defined. When in doubt, copy the complete file.

---

## Create a ZUGFeRD invoice in PHP

```php
$payload = [
    'invoice' => [
        'invoiceNumber' => 'MIN-001',
        'issueDate'     => '2026-05-18',
        'currency'      => 'EUR',
        'seller' => [
            'name'              => 'Acme',
            'vatIdentifier'     => 'DE123456789',
            'legalRegistration' => ['identifier' => 'HRB 12345'],
            'postalAddress'     => ['line1' => 'Hauptstraße 12', 'city' => 'Berlin', 'postCode' => '10115', 'country' => 'DE'],
        ],
        'buyer' => [
            'name'          => 'Globex SAS',
            'postalAddress' => ['line1' => '15 rue de Rivoli', 'city' => 'Paris', 'postCode' => '75001', 'country' => 'FR'],
        ],
        'paymentDetails' => ['paymentAccountIdentifier' => 'DE89370400440532013000'],
        'lines' => [[
            'quantity'       => 10,
            'priceDetails'   => ['netPrice' => 150.00],
            'vatInformation' => ['rate' => 19.00],
            'item'           => ['name' => 'Senior consulting'],
        ]],
    ],
];

$ch = curl_init('https://api.invoicexml.com/v1/create/zugferd');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST           => true,
    CURLOPT_POSTFIELDS     => json_encode($payload),
    CURLOPT_HTTPHEADER     => [
        'Content-Type: application/json',
        'Authorization: Bearer ' . $apiKey,
    ],
]);
$pdf = curl_exec($ch);
file_put_contents('invoice-zugferd.pdf', $pdf);
```

The response is a binary PDF/A-3 file with the ZUGFeRD XML already embedded.

[Full example: `create.php`](./create.php) | [API reference](https://www.invoicexml.com/docs/api/create/zugferd)

---

## Validate a ZUGFeRD file in PHP

```php
$ch = curl_init('https://api.invoicexml.com/v1/validate/zugferd');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST           => true,
    CURLOPT_POSTFIELDS     => [
        'file'    => new CURLFile('invoice.pdf', 'application/pdf'),
        'version' => '2.3.2',
        'profile' => 'extended',
    ],
    CURLOPT_HTTPHEADER     => ['Authorization: Bearer ' . $apiKey],
]);
$report = curl_exec($ch);
echo $report;
```

Returns a JSON validation report listing any schematron rule failures (EN 16931 BR-* and BR-CO-* rules).

[Full example: `validate.php`](./validate.php) | [API reference](https://www.invoicexml.com/docs/api/validate/zugferd)

---

## Extract ZUGFeRD data as JSON in PHP

Useful for feeding ZUGFeRD invoices into REST APIs, ERPs, accounting systems, or any pipeline that prefers JSON over XML.

```php
$ch = curl_init('https://api.invoicexml.com/v1/extract/json');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST           => true,
    CURLOPT_POSTFIELDS     => ['file' => new CURLFile('invoice.pdf', 'application/pdf')],
    CURLOPT_HTTPHEADER     => ['Authorization: Bearer ' . $apiKey],
]);
$json = curl_exec($ch);
$data = json_decode($json, true);
// The invoice document sits under the "invoice" key of the response.
echo $data['invoice']['seller']['name'];
```

[Full example: `extract-json.php`](./extract-json.php) | [API reference](https://www.invoicexml.com/docs/api/extract/json) | [Sample response](../json/extract-json-response.json)

---

## Extract embedded XML from a ZUGFeRD PDF in PHP

```php
$ch = curl_init('https://api.invoicexml.com/v1/extract/xml');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST           => true,
    CURLOPT_POSTFIELDS     => ['file' => new CURLFile('invoice.pdf', 'application/pdf')],
    CURLOPT_HTTPHEADER     => ['Authorization: Bearer ' . $apiKey],
]);
$xml = curl_exec($ch);
file_put_contents('factur-x.xml', $xml);
```

Returns the raw `factur-x.xml` payload (UN/CEFACT Cross-Industry Invoice syntax). Use this when you need the structured XML directly, for example to feed an existing UBL or CII pipeline or to archive separately from the PDF.

[Full example: `extract-xml.php`](./extract-xml.php) | [API reference](https://www.invoicexml.com/docs/api/extract/xml)

---

## Embed your own XML into your own PDF

When your application already renders the invoice PDF and already produces the EN 16931 XML, post both files and the API keeps your visual layer exactly as designed, promotes the container to PDF/A-3, and attaches the XML as `factur-x.xml` (the attachment name the ZUGFeRD specification prescribes since version 2.2) with the German (FeRD) AFRelationship and XMP conventions.

```php
$ch = curl_init('https://api.invoicexml.com/v1/embed/zugferd');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST           => true,
    CURLOPT_POSTFIELDS     => [
        'pdf'            => new CURLFile('invoice.pdf', 'application/pdf'),
        'xml'            => new CURLFile('factur-x.xml', 'application/xml'),
        'skipValidation' => 'false',
    ],
    CURLOPT_HTTPHEADER     => ['Authorization: Bearer ' . $apiKey],
]);
$pdf = curl_exec($ch);
file_put_contents('invoice-zugferd.pdf', $pdf);
```

The XML runs through the complete `/v1/validate/zugferd` rule set before anything is embedded, so a non-compliant invoice never leaves the API: fatal findings come back as a 400 with `errorCode` 4001 and the full finding list. Set `skipValidation` to `'true'` for packaging-only mode, where the structural checks (CII root element, official BT-24 profile URN, profile XSD) still apply but the business rules are skipped.

Only UN/CEFACT CII XML is accepted. If your invoice is UBL, convert it first with [`POST /v1/convert/ubl/to/cii`](https://www.invoicexml.com/docs/api/convert/ubl/to/cii). For the French (FNFE-MPE) packaging conventions, call `/v1/embed/facturx` instead, same request shape.

[Full example: `embed.php`](./embed.php) | [API reference](https://www.invoicexml.com/docs/api/embed/zugferd)

---

## Framework integration

### Laravel

Return a ZUGFeRD invoice from a controller or route closure:

```php
use Illuminate\Support\Facades\Route;

Route::get('/invoices/{id}/zugferd', function ($id) {
    $pdf = app(ZugferdService::class)->create($id);
    return response($pdf, 200, [
        'Content-Type'        => 'application/pdf',
        'Content-Disposition' => "attachment; filename=\"invoice-{$id}.pdf\"",
    ]);
});
```

Store the API key in `config/services.php` and read it via `config('services.invoicexml.key')`.

### Symfony

```php
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

class InvoiceController
{
    #[Route('/invoices/{id}/zugferd', name: 'zugferd_download')]
    public function download(int $id): Response
    {
        $pdf = $this->zugferdService->create($id);
        return new Response($pdf, 200, [
            'Content-Type'        => 'application/pdf',
            'Content-Disposition' => "attachment; filename=\"invoice-{$id}.pdf\"",
        ]);
    }
}
```

### WordPress and WooCommerce

Hook into WooCommerce order lifecycle events to generate ZUGFeRD invoices automatically:

```php
add_action('woocommerce_order_status_completed', function ($order_id) {
    $order = wc_get_order($order_id);
    $pdf = create_zugferd_from_order($order);
    file_put_contents(WP_CONTENT_DIR . "/invoices/{$order_id}.pdf", $pdf);
});
```

The same pattern works in Magento, Drupal Commerce, and PrestaShop.

---

## Common issues

- **`HTTP 401 Unauthorized`**: API key missing or invalid. Generate one at [invoicexml.com/account/authentication](https://www.invoicexml.com/account/authentication) and confirm you are sending `Authorization: Bearer YOUR_API_KEY`. A frequent cause: setting `$apiKey` to the whole `Bearer xxx` value, which sends `Bearer Bearer xxx`. Set the raw key only.
- **`HTTP 400 Bad Request` on Create**: a required field is missing or malformed. Frequent causes: `IssueDate` not in ISO format (`YYYY-MM-DD`), `Currency` not in ISO 4217 (`EUR`, `USD`), country codes not in ISO 3166-1 alpha-2 (`DE`, `FR`).
- **`Class "CURLFile" not found`**: PHP cURL extension is missing. Install with `apt-get install php-curl` (Debian/Ubuntu), `yum install php-curl` (RHEL), or enable the extension in `php.ini`.
- **`SSL certificate problem` errors**: the server's CA bundle is out of date. Update OpenSSL on the host, or set `CURLOPT_CAINFO` to a current `cacert.pem` from curl.se/ca/.
- **Schematron BR-CO-* failures on Validate**: line totals do not match the header total, or tax category and tax percentage are inconsistent. Recompute totals server-side before posting.

---

## Resources

- [Get an InvoiceXML API key](https://www.invoicexml.com/account/authentication)
- [Create ZUGFeRD API reference](https://www.invoicexml.com/docs/api/create/zugferd)
- [Validate ZUGFeRD API reference](https://www.invoicexml.com/docs/api/validate/zugferd)
- [Extract JSON API reference](https://www.invoicexml.com/docs/api/extract/json)
- [Extract XML API reference](https://www.invoicexml.com/docs/api/extract/xml)
- [Embed into ZUGFeRD API reference](https://www.invoicexml.com/docs/api/embed/zugferd)
- [PHP cURL documentation](https://www.php.net/manual/en/book.curl.php)
- [Main repository README](../README.md)
