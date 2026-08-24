-- Seed de developpement: execute uniquement avec le profil dev.
-- Le guard NOT EXISTS evite les doublons si la table contient deja des donnees.
INSERT INTO products (name, category, description, price, stock_quantity)
SELECT *
FROM (
    VALUES
        ('Sneaker Urban Pulse', 'Sneakers', 'Sneaker polyvalente pour la ville et les trajets quotidiens.', 99.90, 25),
        ('Veste Atelier Marine', 'Homme', 'Veste legere coupe droite, finition soignee.', 129.00, 12),
        ('Robe Lumiere', 'Femme', 'Robe fluide avec texture douce pour sorties et occasions.', 89.50, 18),
        ('Sac Echo Mini', 'Accessoires', 'Sac compact avec compartiments internes pratiques.', 59.00, 30),
        ('Sneaker Horizon Run', 'Sneakers', 'Modele running amorti pour usage quotidien.', 119.90, 15),
        ('Chemise Studio Blanc', 'Homme', 'Chemise coton respirant, style epure.', 69.00, 20),
        ('Jupe Riviera', 'Femme', 'Jupe midi confortable et elegante.', 74.90, 16),
        ('Ceinture Grain Noir', 'Accessoires', 'Ceinture cuir texture fine, boucle metal.', 39.90, 40)
) AS seed(name, category, description, price, stock_quantity)
WHERE NOT EXISTS (SELECT 1 FROM products);
