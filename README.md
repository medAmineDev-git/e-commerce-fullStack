# E-commerce

Premiere version d'une boutique de vetements avec un frontend Angular et une API Spring Boot.

## Fonctionnalites

- Catalogue public, recherche, filtres et pagination
- Fiche produit et panier persistant dans le navigateur
- Commande avec paiement a la livraison ou virement manuel
- Gestion admin des produits et categories
- Validation du stock et creation transactionnelle des commandes

## Demarrage local

1. Creer la base PostgreSQL `ecommerce_dev_db` et un utilisateur correspondant a `DB_USER` et `DB_PASSWORD` (par defaut `ecommerce_app`).
2. Lancer l'API : `cd back-spring && ./mvnw spring-boot:run`
3. Lancer le frontend : `cd Front && npm ci && npm start`
4. Ouvrir `http://localhost:4200`.

L'API est disponible sur `http://localhost:8080`, et sa documentation sur `http://localhost:8080/swagger.html`.

## Deploiement Render

Le fichier `render.yaml` force Render a utiliser le runtime Java 21 pour le backend.

Pour un service Render deja cree, configure manuellement:

```text
Runtime: Java
Root Directory: back-spring
Build Command: ./mvnw clean package -DskipTests
Start Command: java -jar target/backend-0.0.1-SNAPSHOT.jar
```

Ajoute les variables `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USER`, `DB_PASSWORD` et `JWT_SECRET` dans Render.

## Tests

```bash
cd back-spring && ./mvnw test
cd Front && npm test -- --run
```
