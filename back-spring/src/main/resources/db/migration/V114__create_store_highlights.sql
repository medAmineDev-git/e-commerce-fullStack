-- Bandeau de reassurance : livraison, assistance, retours, paiement.
--
-- Trois ou quatre promesses courtes, sous la banniere, repondent aux questions
-- qu'un visiteur se pose avant d'ajouter au panier. Sans elles, la page passe
-- directement de l'image au catalogue.
--
-- L'icone est designee par une cle choisie dans un catalogue ferme, jamais par
-- du balisage : le dessin est livre avec le site, le vendeur choisit lequel.
--
-- Les deux emplacements se commandent depuis la boutique, pas depuis chaque
-- ligne : un vendeur decide une fois ou le bandeau apparait, puis compose son
-- contenu.

CREATE TABLE IF NOT EXISTS store_highlights (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL,
    icon_key VARCHAR(40) NOT NULL,
    label VARCHAR(80) NOT NULL,
    detail VARCHAR(160),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    position INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_store_highlights_store FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_store_highlights_store_position
    ON store_highlights (store_id, position);

ALTER TABLE stores ADD COLUMN IF NOT EXISTS highlights_top_enabled BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE stores ADD COLUMN IF NOT EXISTS highlights_bottom_enabled BOOLEAN NOT NULL DEFAULT FALSE;
