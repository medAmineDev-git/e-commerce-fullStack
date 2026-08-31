# Back Spring API Documentation

## 1. Overview

- Tech: Spring Boot 4.1.0
- Default local base URL: http://localhost:8080
- Main resource: products
- Response format: JSON

## Authentication

### Login admin

- Method: POST
- Path: /api/auth/login
- The identifier accepts either username or email.

```json
{
  "identifier": "admin",
  "password": "admin"
}
```

The development seed account is:
- username: `admin`
- email: `admin@ecommerce.local`
- password: `admin`

The response contains an `accessToken`. Send it for protected requests:

```text
Authorization: Bearer <accessToken>
```

Protected operations include admin product/category/home changes, all order administration, and dev reseed routes.

OpenAPI + Swagger UI (served by the app):
- OpenAPI spec: http://localhost:8080/openapi.yaml
- Swagger UI: http://localhost:8080/swagger.html

## 2. Product Model

### ProductRequest (POST/PUT body)

```json
{
  "name": "Sneaker Urban Pulse",
  "category": "Sneakers",
  "description": "Sneaker polyvalente pour la ville.",
  "price": 99.9,
  "stockQuantity": 25
}
```

Validation rules:
- name: required, non-empty
- category: required, non-empty
- description: required, non-empty
- price: required, > 0
- stockQuantity: required, >= 0

### ProductResponse

```json
{
  "id": 1,
  "name": "Sneaker Urban Pulse",
  "category": "Sneakers",
  "description": "Sneaker polyvalente pour la ville.",
  "price": 99.9,
  "stockQuantity": 25
}
```

## 3. Product Endpoints

### 3.1 Get all products

- Method: GET
- Path: /api/products
- Response: 200 OK, ProductResponse[]

Example:

```bash
curl -X GET "http://localhost:8080/api/products"
```

### 3.2 Get product by id

- Method: GET
- Path: /api/products/{id}
- Response:
  - 200 OK with ProductResponse
  - 404 Not Found if id does not exist

Example:

```bash
curl -X GET "http://localhost:8080/api/products/1"
```

### 3.3 Create product

- Method: POST
- Path: /api/products
- Body: ProductRequest
- Response:
  - 201 Created with ProductResponse
  - 400 Bad Request on validation error or malformed body

Example:

```bash
curl -X POST "http://localhost:8080/api/products" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Robe Lumiere",
    "category": "Femme",
    "description": "Robe fluide et confortable.",
    "price": 89.5,
    "stockQuantity": 18
  }'
```

### 3.4 Update product

- Method: PUT
- Path: /api/products/{id}
- Body: ProductRequest
- Response:
  - 200 OK with ProductResponse
  - 400 Bad Request on validation/malformed body
  - 404 Not Found if id does not exist

Example:

```bash
curl -X PUT "http://localhost:8080/api/products/1" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Robe Lumiere Edition",
    "category": "Femme",
    "description": "Robe fluide edition speciale.",
    "price": 95.0,
    "stockQuantity": 12
  }'
```

### 3.5 Delete product

- Method: DELETE
- Path: /api/products/{id}
- Response:
  - 204 No Content
  - 404 Not Found if id does not exist

Example:

```bash
curl -X DELETE "http://localhost:8080/api/products/1"
```

### 3.6 Upload product image

- Method: POST
- Path: /api/products/images
- Content-Type: multipart/form-data
- Form field: `file`
- Formats: JPEG, PNG, GIF, WebP
- Maximum size: 5 MB
- Response: 201 Created

Example:

```bash
curl -X POST "http://localhost:8080/api/products/images" \
  -F "file=@C:/images/sneaker.png"
```

Example response:

```json
{
  "url": "/uploads/products/550e8400-e29b-41d4-a716-446655440000.png"
}
```

The returned URL can then be added to `imageUrls` when creating or updating a product.

### 3.7 Product category hierarchy

Products support both `category` (parent) and optional `subcategory`.
Categories support an optional `parentId`:

```json
{
  "name": "T-shirt",
  "description": "T-shirts homme",
  "parentId": 1
}
```

When `parentId` is omitted or null, the category is a top-level category.

Products can belong to multiple seasons. Supported values are `Printemps`, `Été`, `Automne` and `Hiver`.

## 4. Home Configuration

### 4.1 Get home configuration

- Method: GET
- Path: /api/home/configuration
- Response: 200 OK

Example response:

```json
{
  "title": "Style urbain, livraison rapide, paiement a la livraison.",
  "text": "Decouvre une selection orientee streetwear premium.",
  "featuredProductId": 1
}
```

### 4.2 Save home configuration

- Method: PUT
- Path: /api/home/configuration
- Body:

```json
{
  "title": "Nouvelle collection automne",
  "text": "Decouvre les pieces fortes de la saison.",
  "featuredProductId": 5
}
```

The selected product must exist. The frontend admin screen is available at `/admin/home`.

## 5. Paged Search Endpoint

### 4.1 Search + category + pagination + sort

- Method: GET
- Path: /api/products/page
- Query params:
  - q: search text (optional)
  - category: category filter (optional)
    - allowed values: Homme, Femme, Sneakers, Accessoires
    - invalid value is ignored (treated as empty)
  - subcategory: exact subcategory filter (optional)
  - season: exact season filter (optional)
  - page: 0-based page index (default 0)
  - size: page size (default 12, max 100)
  - sortBy: id | name | price | stockQuantity (default id)
  - sortDirection: asc | desc (default desc)

Example:

```bash
curl -X GET "http://localhost:8080/api/products/page?q=sneaker&category=Sneakers&page=0&size=8&sortBy=price&sortDirection=asc"
```

Example response:

```json
{
  "items": [
    {
      "id": 11,
      "name": "Sneaker Light",
      "category": "Sneakers",
      "description": "Modele leger.",
      "price": 79.9,
      "stockQuantity": 14
    }
  ],
  "page": 0,
  "size": 8,
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "sortBy": "price",
  "sortDirection": "asc",
  "query": "sneaker",
  "category": "Sneakers"
}
```

## 6. Dev-only Endpoint (Reseed)

This endpoint exists only when the active Spring profile is dev.

### 5.1 Reseed catalog

- Method: POST
- Path: /api/dev/catalog/reseed
- Profile: dev only
- Effect:
  - removes all products
  - inserts a fixed demo dataset
- Response: 200 OK

Example:

```bash
curl -X POST "http://localhost:8080/api/dev/catalog/reseed"
```

Example response:

```json
{
  "message": "Catalogue dev reinitialise",
  "insertedCount": 8
}
```

If profile is not dev:
- endpoint is not registered
- expected result: 404 Not Found

## 7. Category Endpoints

### 6.1 Get all categories

- Method: GET
- Path: /api/categories

### 6.2 Get category by id

- Method: GET
- Path: /api/categories/{id}

### 6.3 Create category

- Method: POST
- Path: /api/categories
- Body:

```json
{
  "name": "Homme",
  "description": "Collection homme"
}
```

### 6.4 Update category

- Method: PUT
- Path: /api/categories/{id}
- Body: same as create

### 6.5 Delete category

- Method: DELETE
- Path: /api/categories/{id}

## 8. Order Endpoint

### 7.1 Place order

- Method: POST
- Path: /api/orders

Example request:

```json
{
  "customerName": "Alice",
  "phone": "0600000000",
  "city": "Paris",
  "address": "10 rue Exemple",
  "note": "Sonner en bas",
  "paymentMethod": "cash_on_delivery",
  "items": [
    { "productId": 1, "quantity": 2 }
  ],
  "total": 199.80
}
```

Example response:

```json
{
  "orderId": "CMD-123456",
  "estimatedDelivery": "24 aout 2026",
  "total": 199.80,
  "status": "confirmed",
  "items": [
    {
      "productId": 1,
      "productName": "Sneaker Urban Pulse",
      "unitPrice": 99.90,
      "quantity": 2
    }
  ]
}
```

### Publisher reference

An optional `publisherRef` can be sent when creating an order. Currently accepted values are `am` and `wa`.

```json
{
  "publisherRef": "am"
}
```

Unknown values are ignored and stored as `null`.

To filter the admin order list by publisher reference:

```text
GET /api/orders?ref=am
GET /api/orders?ref=wa
```

### 8.2 Get order detail

- Method: GET
- Path: /api/orders/{orderId}
- Response: 200 OK with customer, delivery, note, items, total and status

### 8.3 Update order status and note

- Method: PUT
- Path: /api/orders/{orderId}
- Body:

```json
{
  "status": "VALIDEE_PAR_LE_CLIENT",
  "note": "Client confirme par telephone."
}
```

Allowed statuses:
- `EN_ATTENTE_VALIDATION_ADMIN`
- `ANNULEE`
- `VALIDEE_PAR_LE_CLIENT`
- `LIVREE_ET_PAYEE`
- `RETOURNEE_PAR_LE_CLIENT`
- `LIVRAISON_EN_COURS`

Every new order starts with `EN_ATTENTE_VALIDATION_ADMIN`.

## 9. Error Response Contract

All handled errors use this shape:

```json
{
  "timestamp": "2026-08-21T17:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/products",
  "validationErrors": {
    "name": "name is required"
  }
}
```

Status mapping:
- 400: validation error or malformed JSON
- 404: product not found
- 500: unexpected server error

## 10. Quick PowerShell Examples

```powershell
# Get paged sneakers sorted by price asc
Invoke-RestMethod -Method GET -Uri "http://localhost:8080/api/products/page?q=sneaker&category=Sneakers&page=0&size=8&sortBy=price&sortDirection=asc"

# Reseed dev catalog
Invoke-RestMethod -Method POST -Uri "http://localhost:8080/api/dev/catalog/reseed"

# Create a category
Invoke-RestMethod -Method POST -Uri "http://localhost:8080/api/categories" -ContentType "application/json" -Body '{"name":"Homme","description":"Collection homme"}'

# Place an order
Invoke-RestMethod -Method POST -Uri "http://localhost:8080/api/orders" -ContentType "application/json" -Body '{"customerName":"Alice","phone":"0600000000","city":"Paris","address":"10 rue Exemple","note":"","paymentMethod":"cash_on_delivery","items":[{"productId":1,"quantity":1}],"total":99.90}'
```
