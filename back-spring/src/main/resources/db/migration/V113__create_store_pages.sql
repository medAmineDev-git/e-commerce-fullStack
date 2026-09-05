-- Pages de contenu propres a chaque boutique.
--
-- Mentions legales, livraison, retours : un pied de page vide donne
-- l'impression d'une vitrine inachevee, et ces mentions sont attendues des
-- clients comme du legislateur.
--
-- Les textes livres ne sont pas ecrits ici mais dans DefaultStorePages, et
-- installes au demarrage pour les boutiques qui n'ont aucune page. Les tenir
-- a deux endroits les aurait laisses diverger au premier ajustement.
--
-- Le slug est l'adresse publique : unique dans une boutique, libre d'une
-- boutique a l'autre. La suppression suit celle de la boutique, comme pour
-- les produits et les catalogues.

CREATE TABLE IF NOT EXISTS store_pages (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL,
    slug VARCHAR(80) NOT NULL,
    title VARCHAR(160) NOT NULL,
    content TEXT NOT NULL,
    position INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_store_pages_store FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_store_pages_store_slug_unique
    ON store_pages (store_id, lower(slug));

CREATE INDEX IF NOT EXISTS idx_store_pages_store_position
    ON store_pages (store_id, position);
