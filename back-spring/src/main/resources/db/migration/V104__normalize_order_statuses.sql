UPDATE orders
SET status = 'EN_ATTENTE_VALIDATION_ADMIN'
WHERE status = 'confirmed';
