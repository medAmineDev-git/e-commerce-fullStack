-- Normalise les roles existants avant de poser la contrainte.
-- Le compte historique 'admin' devient proprietaire de boutique : c'est ce qu'il
-- est reellement depuis V109, ou il s'est vu attribuer la boutique par defaut.
UPDATE admin_users SET role = 'ROLE_STORE_OWNER' WHERE role IN ('ROLE_ADMIN', 'ADMIN', 'admin');
UPDATE admin_users SET role = 'ROLE_SUPER_ADMIN' WHERE role IN ('SUPER_ADMIN', 'super_admin');
UPDATE admin_users SET role = 'ROLE_STORE_OWNER' WHERE role IN ('STORE_OWNER', 'store_owner');

-- Un role hors de cette liste ne doit plus pouvoir entrer en base : la colonne
-- etait un texte libre alors que trois roles circulaient dans le code.
ALTER TABLE admin_users DROP CONSTRAINT IF EXISTS chk_admin_users_role;
ALTER TABLE admin_users
    ADD CONSTRAINT chk_admin_users_role
    CHECK (role IN ('ROLE_STORE_OWNER', 'ROLE_SUPER_ADMIN'));

-- Decision d'architecture : un compte, une seule boutique.
-- Garantie par la base plutot que par convention, pour que le code n'ait plus
-- a choisir entre findFirstByOwner et findAllByOwner.
DELETE FROM stores s
WHERE s.owner_id IS NOT NULL
  AND s.id <> (SELECT MIN(inner_s.id) FROM stores inner_s WHERE inner_s.owner_id = s.owner_id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_stores_owner_unique
    ON stores(owner_id)
    WHERE owner_id IS NOT NULL;

-- Une boutique sans proprietaire n'est administrable par personne.
-- On bloque la suppression du compte plutot que de detacher la boutique.
ALTER TABLE stores DROP CONSTRAINT IF EXISTS fk_stores_owner;
DO $$
DECLARE
    constraint_name_to_drop TEXT;
BEGIN
    SELECT conname INTO constraint_name_to_drop
    FROM pg_constraint
    WHERE conrelid = 'stores'::regclass
      AND contype = 'f'
      AND confrelid = 'admin_users'::regclass
    LIMIT 1;

    IF constraint_name_to_drop IS NOT NULL THEN
        EXECUTE format('ALTER TABLE stores DROP CONSTRAINT %I', constraint_name_to_drop);
    END IF;

    ALTER TABLE stores
        ADD CONSTRAINT fk_stores_owner
        FOREIGN KEY (owner_id) REFERENCES admin_users(id) ON DELETE RESTRICT;
END $$;
