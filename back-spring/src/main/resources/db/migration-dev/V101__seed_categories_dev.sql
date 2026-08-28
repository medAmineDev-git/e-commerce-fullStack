INSERT INTO categories (name, description)
SELECT *
FROM (
    VALUES
        ('Homme', 'Collection homme'),
        ('Femme', 'Collection femme'),
        ('Sneakers', 'Univers sneakers'),
        ('Accessoires', 'Accessoires du quotidien')
) AS seed(name, description)
WHERE NOT EXISTS (SELECT 1 FROM categories);
