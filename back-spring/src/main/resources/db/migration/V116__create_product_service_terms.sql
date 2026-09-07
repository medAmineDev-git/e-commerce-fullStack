-- Bloc « Livraison et retours » de la fiche produit, propre a chaque article.
--
-- Ces trois lignes etaient ecrites en dur dans le gabarit de la vitrine,
-- identiques pour toutes les boutiques et tous les articles. Elles varient
-- pourtant d un vendeur a l autre, et parfois d un article a l autre : un
-- meuble ne se retourne pas comme un t-shirt.
--
-- Les produits existants recoivent exactement les valeurs qui s affichaient
-- jusqu ici : leur fiche ne change pas d apparence tant que le vendeur n y
-- touche pas.

CREATE TABLE IF NOT EXISTS product_service_terms (
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    term_label VARCHAR(60) NOT NULL,
    term_value VARCHAR(160) NOT NULL,
    PRIMARY KEY (product_id, position)
);

INSERT INTO product_service_terms (product_id, position, term_label, term_value)
SELECT p.id, t.position, t.label, t.value
FROM products p
CROSS JOIN (
    VALUES
        (0, 'Livraison', '48 à 72 heures'),
        (1, 'Paiement', 'À la livraison ou par virement'),
        (2, 'Retour', 'Sous 7 jours')
) AS t(position, label, value)
WHERE NOT EXISTS (
    SELECT 1 FROM product_service_terms existing WHERE existing.product_id = p.id
);
