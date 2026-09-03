package com.ecommerce.backend.isolation;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Rejoue la migration multi-boutique sur un PostgreSQL reel, sur des donnees
 * anterieures au multi-boutique.
 *
 * La suite de tests applicative tourne sur H2 avec Flyway desactive : la migration
 * V109 n'y est donc jamais exercee. Ce test comble ce trou, car une migration qui
 * echoue en production sur des donnees existantes ne se rattrape pas.
 */
@Testcontainers
@DisplayName("Les migrations multi-boutique se rejouent sur des donnees existantes")
class MultiStoreMigrationTest {

    private static final String LEGACY_SCHEMA_VERSION = "108";

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("ecommerce_migration_test")
                    .withUsername("ecommerce_app")
                    .withPassword("ecommerce_app");

    @BeforeAll
    static void migrateLegacyDataThroughV109() throws SQLException {
        migrateTo(LEGACY_SCHEMA_VERSION);
        seedSingleStoreData();
        migrateTo("latest");
    }

    @Test
    void shouldCreateTheDefaultStore() throws SQLException {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT slug, is_active FROM stores WHERE id = 1")) {

            assertTrue(rows.next(), "La boutique par defaut doit exister apres la migration");
            assertEquals("nova", rows.getString("slug"));
            assertTrue(rows.getBoolean("is_active"));
        }
    }

    @Test
    void shouldAttachTheDefaultStoreToTheSeededAdmin() throws SQLException {
        assertEquals(
                queryLong("SELECT id FROM admin_users WHERE username = 'admin'"),
                queryLong("SELECT owner_id FROM stores WHERE id = 1"),
                "La boutique par defaut doit appartenir a l'administrateur historique"
        );
    }

    @Test
    void shouldAttachEveryExistingRowToTheDefaultStore() throws SQLException {
        for (String table : new String[]{"products", "categories", "orders", "home_configurations"}) {
            assertEquals(
                    0L,
                    queryLong("SELECT COUNT(*) FROM " + table + " WHERE store_id <> 1"),
                    "Toutes les lignes de " + table + " doivent etre rattachees a la boutique 1"
            );
            assertTrue(
                    queryLong("SELECT COUNT(*) FROM " + table) > 0,
                    "Le jeu de donnees de " + table + " ne doit pas etre vide, sinon le test ne prouve rien"
            );
        }
    }

    @Test
    void shouldMakeStoreIdMandatoryEverywhere() throws SQLException {
        for (String table : new String[]{"products", "categories", "orders", "home_configurations"}) {
            assertEquals(
                    "NO",
                    queryString("""
                            SELECT is_nullable FROM information_schema.columns
                            WHERE table_name = '%s' AND column_name = 'store_id'
                            """.formatted(table)),
                    "store_id doit etre obligatoire sur " + table
            );
        }
    }

    @Test
    void shouldScopeUniquenessToTheStoreInsteadOfThePlatform() throws SQLException {
        insertSecondStore();

        // Deux boutiques peuvent porter la meme categorie et la meme reference produit.
        execute("""
                INSERT INTO categories (name, description, store_id)
                VALUES ('Sneakers', 'Categorie homonyme', 2)
                """);
        execute("""
                INSERT INTO products (name, category, description, price, stock_quantity, sku, store_id)
                VALUES ('Autre sneaker', 'Sneakers', 'Description', 79.90, 5, 'SKU-1', 2)
                """);

        assertEquals(2L, queryLong("SELECT COUNT(*) FROM categories WHERE name = 'Sneakers'"));
        assertEquals(2L, queryLong("SELECT COUNT(*) FROM products WHERE sku = 'SKU-1'"));
    }

    /** V110 : le compte historique etait un ROLE_ADMIN, il devient proprietaire. */
    @Test
    void shouldConvertTheLegacyAdminRoleToStoreOwner() throws SQLException {
        assertEquals(
                "ROLE_STORE_OWNER",
                queryString("SELECT role FROM admin_users WHERE username = 'admin'")
        );
        assertEquals(
                0L,
                queryLong("SELECT COUNT(*) FROM admin_users WHERE role = 'ROLE_ADMIN'")
        );
    }

    @Test
    void shouldRejectAnUnknownRole() throws SQLException {
        try {
            execute("""
                    INSERT INTO admin_users (username, email, password_hash, role)
                    VALUES ('pirate', 'pirate@test.local', 'hash', 'ROLE_ROOT')
                    """);
            fail("Un role hors de l'enumeration doit etre refuse par la base");
        } catch (SQLException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    /** Decision : un compte, une boutique. Garantie par la base, pas par convention. */
    @Test
    void shouldRejectASecondStoreForTheSameOwner() throws SQLException {
        long ownerId = queryLong("SELECT id FROM admin_users WHERE username = 'admin'");

        try {
            execute("""
                    INSERT INTO stores (name, slug, is_active, owner_id)
                    VALUES ('Seconde boutique', 'seconde-boutique', TRUE, %d)
                    """.formatted(ownerId));
            fail("Un proprietaire ne doit pas pouvoir detenir deux boutiques");
        } catch (SQLException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    /** Une boutique sans proprietaire n'est administrable par personne. */
    @Test
    void shouldRefuseToDeleteAnOwnerWhoStillHasAStore() throws SQLException {
        try {
            execute("DELETE FROM admin_users WHERE username = 'admin'");
            fail("Supprimer un proprietaire laisserait sa boutique orpheline");
        } catch (SQLException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    @Test
    void shouldStillRejectDuplicatesInsideTheSameStore() throws SQLException {
        try {
            execute("""
                    INSERT INTO categories (name, description, store_id)
                    VALUES ('Sneakers', 'Doublon dans la meme boutique', 1)
                    """);
            fail("Une categorie homonyme dans la meme boutique doit etre refusee");
        } catch (SQLException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Jeu de donnees anterieur au multi-boutique
    // ---------------------------------------------------------------

    private static void seedSingleStoreData() throws SQLException {
        execute("""
                INSERT INTO admin_users (username, email, password_hash, role)
                VALUES ('admin', 'admin@ecommerce.local', 'hash', 'ROLE_ADMIN')
                """);
        execute("""
                INSERT INTO products (name, category, description, price, stock_quantity, sku)
                VALUES ('Sneaker Urban Pulse', 'Sneakers', 'Description', 89.90, 10, 'SKU-1')
                """);
        execute("""
                INSERT INTO categories (name, description)
                VALUES ('Sneakers', 'Categorie historique')
                """);
        execute("""
                INSERT INTO orders (order_number, customer_name, phone, city, address,
                                    payment_method, status, estimated_delivery, total)
                VALUES ('CMD-LEGACY1', 'Alice', '0600000000', 'Paris', '10 rue Exemple',
                        'cash_on_delivery', 'EN_ATTENTE_VALIDATION_ADMIN', CURRENT_DATE, 89.90)
                """);
        execute("""
                INSERT INTO home_configurations (config_key, title, text, featured_product_id)
                SELECT 'home', 'Titre historique', 'Texte historique', MIN(id) FROM products
                """);
    }

    private static void insertSecondStore() throws SQLException {
        execute("""
                INSERT INTO stores (id, name, slug, is_active)
                VALUES (2, 'Atelier Rive Gauche', 'atelier', TRUE)
                ON CONFLICT (id) DO NOTHING
                """);
    }

    // ---------------------------------------------------------------
    // Utilitaires JDBC
    // ---------------------------------------------------------------

    private static void migrateTo(String version) {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .target(version)
                .load()
                .migrate();
    }

    private static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static void execute(String sql) throws SQLException {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static long queryLong(String sql) throws SQLException {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            assertTrue(rows.next(), "Aucun resultat pour: " + sql);
            return rows.getLong(1);
        }
    }

    private static String queryString(String sql) throws SQLException {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            assertTrue(rows.next(), "Aucun resultat pour: " + sql);
            return rows.getString(1);
        }
    }
}
