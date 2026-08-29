# Back Spring - Notes d'apprentissage

Ce document sert de support de revision.
Objectif: expliquer le backend Spring Boot du projet bloc par bloc, avec une logique pedagogique.

## Comment utiliser ce fichier

- Lire du global vers le detail.
- Revenir sur les sections "A retenir".
- Ajouter de nouvelles notes a chaque evolution du backend.

## 1) Vue d'ensemble de l'architecture actuelle

Le backend Spring est organise en couches:

1. Entree application (demarrage Spring Boot)
2. Configuration (securite, base de donnees)
3. Domaine Product:
- Entity (modele JPA)
- Repository (acces DB)
- Service (logique metier)
- Controller (API REST)
4. Gestion d'exception metier
5. Test de demarrage du contexte

Flux standard d'une requete API:

Client HTTP -> Controller -> Service -> Repository -> PostgreSQL

## 2) Point d'entree Spring Boot

Fichier: src/main/java/com/ecommerce/backend/BackendApplication.java

### Role

Demarre l'application et initialise le contexte Spring.

### Bloc de code important

- `@SpringBootApplication`
  - Active l'auto-configuration.
  - Active le scan des composants du package principal et sous-packages.
- `SpringApplication.run(...)`
  - Lance le serveur et cree les beans.

### A retenir

Sans cette classe, l'application ne demarre pas.

## 3) Maven et dependances

Fichier: pom.xml

### Role

Declare les librairies necessaires et la version Java.

### Dependances principales

- spring-boot-starter-webmvc: creation d'API REST
- spring-boot-starter-data-jpa: persistance SQL via JPA/Hibernate
- spring-boot-starter-security: securite Spring
- spring-boot-starter-validation: validation de donnees
- postgresql: driver de connexion PostgreSQL
- lombok: reduit le boilerplate (getters/setters, etc.)

### Test

- data-jpa-test, security-test, validation-test pour les tests backend.

### A retenir

Le pom definit les capacites techniques du backend.

## 4) Configuration runtime

Fichier: src/main/resources/application.properties

### Role

Parametre l'application: DB, JPA, logs SQL.

### Blocs importants

- `spring.datasource.url`
  - URL JDBC vers PostgreSQL.
- `spring.datasource.username/password`
  - Identifiants de connexion.
  - Le password utilise une variable d'environnement avec fallback.
- `spring.jpa.hibernate.ddl-auto=update`
  - Hibernate cree/ajuste le schema en dev.
- `spring.jpa.show-sql=true`
  - Affiche le SQL genere (utile pour apprendre).

### A retenir

Configuration pratique pour dev/apprentissage, a durcir en production.

## 5) Securite Spring

Fichier: src/main/java/com/ecommerce/backend/config/SecurityConfig.java

### Role

Definit les regles de securite HTTP.

### Bloc actuel

- CSRF desactive.
- Toutes les routes autorisees (`permitAll`).

### Pourquoi

Mode apprentissage/dev pour avancer vite.

### A retenir

Cette config est temporaire. Plus tard: JWT + roles + restrictions par endpoint.

## 6) Domaine Product - explication couche par couche

### 6.1 Entity JPA

Fichier: src/main/java/com/ecommerce/backend/product/Product.java

#### Role

Represente un produit dans le code Java et dans la table SQL.

#### Blocs importants

- `@Entity`: mappe la classe a une table.
- `@Id` + `@GeneratedValue(strategy = IDENTITY)`: cle primaire auto-incrementee.
- Champs:
  - `name`
  - `description`
  - `price` (BigDecimal, bon choix pour montants)
  - `stockQuantity`
- Lombok:
  - `@Getter`, `@Setter`, `@NoArgsConstructor`

#### A retenir

Entity = modele persistant, pas forcement modele expose en API (on ajoutera DTO ensuite).

### 6.2 Repository

Fichier: src/main/java/com/ecommerce/backend/product/ProductRepository.java

#### Role

Expose des operations DB sans ecrire de SQL manuel pour le CRUD de base.

#### Bloc important

- `extends JpaRepository<Product, Long>`

#### Ce que cela fournit automatiquement

- `findAll()`
- `findById(id)`
- `save(entity)`
- `delete(entity)`

#### A retenir

Le Repository est la couche d'acces aux donnees.

### 6.3 Service

Fichier: src/main/java/com/ecommerce/backend/product/ProductService.java

#### Role

Centralise la logique metier.

#### Blocs importants

- `@Service`: bean metier Spring.
- Injection par constructeur de `ProductRepository`.

#### Methodes

- `getAllProducts()`
  - Retourne tous les produits.
- `getProductById(id)`
  - Recherche par id.
  - Si absent, leve `ProductNotFoundException`.
- `createProduct(product)`
  - Persiste le nouveau produit.
- `updateProduct(id, updatedProduct)`
  - Charge l'existant.
  - Copie les champs modifiables.
  - Sauvegarde.
- `deleteProduct(id)`
  - Supprime apres verification d'existence.

#### A retenir

Le Service protege les regles metier et evite de mettre cette logique dans le Controller.

### 6.4 Controller REST

Fichier: src/main/java/com/ecommerce/backend/product/ProductController.java

#### Role

Expose les endpoints HTTP.

#### Blocs importants

- `@RestController`: reponses JSON.
- `@RequestMapping("/api/products")`: prefixe de route.

#### Endpoints

- `GET /api/products`
- `GET /api/products/{id}`
- `POST /api/products` (201 Created)
- `PUT /api/products/{id}`
- `DELETE /api/products/{id}` (204 No Content)

#### A retenir

Le Controller traduit HTTP <-> metier, puis delegue au Service.

## 7) Gestion d'erreur metier

Fichier: src/main/java/com/ecommerce/backend/product/ProductNotFoundException.java

### Role

Signale qu'un produit n'existe pas.

### Bloc important

- `@ResponseStatus(HttpStatus.NOT_FOUND)`

### Effet

Quand l'exception est levee, Spring renvoie automatiquement HTTP 404.

### A retenir

Bonne base pour des erreurs metier explicites.

## 8) Test de base

Fichier: src/test/java/com/ecommerce/backend/BackendApplicationTests.java

### Role

Verifier que le contexte Spring se charge correctement.

### Bloc important

- `@SpringBootTest`
- `contextLoads()`

### A retenir

Test minimal mais utile pour detecter une casse de configuration.

## 9) Bonnes pratiques deja visibles

- Separation des responsabilites (Controller/Service/Repository)
- Exception metier dediee
- Utilisation de BigDecimal pour les prix
- Variables d'environnement pour le mot de passe DB

## 10) Limites actuelles (normales en apprentissage)

- Le Controller expose l'Entity directement (pas de DTO)
- Peu de validation metier sur les entrees API
- Securite volontairement ouverte
- Peu de tests metier/HTTP

## 11) Prochaine progression recommandee

1. Ajouter DTO + validation (`@NotBlank`, `@Positive`, etc.)
2. Ajouter mapper Entity <-> DTO
3. Ajouter GlobalExceptionHandler pour un format d'erreur JSON uniforme
4. Ajouter tests Service (Mockito)
5. Ajouter tests Controller (MockMvc)
6. Introduire auth JWT

---

## Journal des explications

### Session 1 - Base Spring actuelle

- Architecture generale du backend
- Role de chaque couche Product
- Role de la configuration DB et securite
- Gestion d'exception 404
- Points d'amelioration pour la suite

> Regle de travail: a chaque nouvelle explication backend, on ajoute un nouveau bloc dans ce journal.

### Session 2 - Refactor bonnes pratiques (DTO, validation, erreurs globales)

#### Objectif

Passer d'une API basique a une API plus propre et maintenable:

- ne plus exposer l'Entity directement au client
- valider les entrees HTTP
- uniformiser les erreurs JSON
- clarifier les responsabilites des couches

#### Ce qui a ete implemente

1. DTOs de requete/reponse
- `ProductRequest` (entree API)
- `ProductResponse` (sortie API)

2. Validation Bean Validation sur `ProductRequest`
- `@NotBlank` pour `name`, `description`
- `@NotNull` + `@DecimalMin` pour `price`
- `@NotNull` + `@PositiveOrZero` pour `stockQuantity`

3. Mapper dedie `ProductMapper`
- conversion `ProductRequest -> Product`
- conversion `Product -> ProductResponse`
- methode de mise a jour de l'entity existante

4. Service refactore
- le service retourne des `ProductResponse`
- creation/mise a jour via `ProductRequest`
- methode privee `findByIdOrThrow` pour centraliser la recherche
- `@Transactional(readOnly = true)` au niveau classe
- `@Transactional` sur les methodes d'ecriture

5. Controller refactore
- endpoints consomment `@Valid @RequestBody ProductRequest`
- endpoints renvoient `ProductResponse`

6. Gestion d'erreurs globale
- `GlobalExceptionHandler` avec `@RestControllerAdvice`
- format d'erreur commun `ApiErrorResponse`
- gestion explicite de:
  - `ProductNotFoundException` -> 404
  - `MethodArgumentNotValidException` -> 400 + details par champ
  - `HttpMessageNotReadableException` -> 400
  - `Exception` -> 500

7. Renforcement JPA de l'entity
- `@Table(name = "products")`
- `@Column(nullable = false, ...)` sur les champs critiques

#### Pourquoi c'est mieux

- Contrat API stable (DTOs) meme si l'Entity evolue
- Validation automatique et messages d'erreur exploitables cote Front
- Separation claire: Controller (HTTP), Service (metier), Mapper (conversion)
- Reponses d'erreurs standardisees pour faciliter debug et integration

#### A retenir

- Entity != DTO
- Toute entree externe doit etre validee
- Une API pro doit avoir un format d'erreur uniforme
- Les transactions doivent etre explicites au niveau service

### Session 3 - Separation des environnements (dev/test/prod)

#### Objectif

Avoir une configuration differente selon l'environnement d'execution.

#### Ce qui a ete implemente

1. `application.properties` minimal
- contient uniquement les proprietes communes
- active le profil via:
  - `spring.profiles.active=${SPRING_PROFILES_ACTIVE:dev}`

2. `application-dev.properties`
- base locale dediee dev (`ecommerce_dev_db`)
- `ddl-auto=update`
- SQL visible pour debug/apprentissage

3. `application-test.properties`
- base dediee test/CI (`ecommerce_test_db`)
- `ddl-auto=validate`
- logs SQL desactives

4. `application-prod.properties`
- credentials obligatoires via variables d'environnement
- `ddl-auto=validate`
- logs SQL desactives

#### Pourquoi c'est mieux

- Evite de melanger donnees dev et prod
- Reduit les risques de mauvaise configuration en production
- Rend la CI plus stable avec un profil test isole

#### Commandes utiles

- Lancer en dev (defaut):
  - `mvnw.cmd spring-boot:run`
- Lancer explicitement en prod:
  - `set SPRING_PROFILES_ACTIVE=prod`
  - `mvnw.cmd spring-boot:run`
- Lancer explicitement en test:
  - `set SPRING_PROFILES_ACTIVE=test`
  - `mvnw.cmd test`

### Session 4 - Tests unitaires et tests web

#### Objectif

Valider la logique metier et le contrat HTTP automatiquement.

#### Ce qui a ete ajoute

1. Tests unitaires `ProductServiceTest` (Mockito)
- mock de `ProductRepository` et `ProductMapper`
- verification des cas:
  - lecture liste/detail
  - creation
  - mise a jour
  - suppression
  - cas introuvable

2. Tests web `ProductControllerTest` (MockMvc)
- tests des endpoints `/api/products`
- verification des statuts HTTP:
  - 200, 201, 204, 400, 404
- verification du format d'erreur JSON du `GlobalExceptionHandler`

3. Profil de test force sur les tests Spring
- `@ActiveProfiles("test")`
- execution sur base H2 en memoire

#### A retenir

- Les tests service valident la logique metier de facon isolee.
- Les tests web valident le contrat API vu par le Front.
- Le profil test doit etre stable et autonome (pas de dependance a une DB externe).

### Session 5 - Recherche, pagination, tri + tests repository

#### Objectif

Exposer une API produit plus proche d'un cas reel de catalogue.

#### Ce qui a ete ajoute

1. Recherche paginee et triee cote API
- nouvel endpoint: `GET /api/products/page`
- params supportes:
  - `q` (recherche texte)
  - `page` (index de page)
  - `size` (taille page)
  - `sortBy` (`id`, `name`, `price`, `stockQuantity`)
  - `sortDirection` (`asc` / `desc`)

2. Reponse paginee dediee
- `ProductPageResponse` avec:
  - `items`, `page`, `size`, `totalElements`, `totalPages`, `last`
  - `sortBy`, `sortDirection`, `query`

3. Repository enrichi
- methode Spring Data:
  - `findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(...)`

4. Service avec garde-fous
- normalisation page/size
- whitelist des champs de tri
- fallback tri par `id desc` si valeurs invalides

5. Tests d'integration repository
- `ProductRepositoryTest` avec `@DataJpaTest`
- validation de:
  - recherche textuelle ignore-case
  - pagination + tri SQL

6. Tests existants etendus
- `ProductServiceTest`: cas recherche paginee
- `ProductControllerTest`: endpoint `/api/products/page`

#### A retenir

- La pagination doit etre geree cote backend pour scaler.
- Le tri doit etre borne par une whitelist pour eviter les abus.
- Les tests repository completent les tests unitaires/mock.

### Session 6 - Filtre categorie backend + synchronisation URL front

#### Objectif

Faire evoluer le catalogue vers une navigation partageable (URL) et un filtrage categorie reel cote serveur.

#### Ce qui a ete ajoute cote backend

1. Nouveau champ domaine
- `Product` contient maintenant `category` (obligatoire, longueur bornee)
- `ProductRequest` et `ProductResponse` exposent `category`
- `ProductMapper` mappe `category` en create/update/read

2. API paginee enrichie
- endpoint `GET /api/products/page` accepte maintenant `category`
- `ProductPageResponse` renvoie aussi la `category` appliquee

3. Repository et service
- nouvelle requete `searchProducts(query, category, pageable)`
- filtre combine:
  - texte (`name` / `description`)
  - categorie exacte ignore-case
- garde-fous service:
  - categorie whitelistee (`Homme`, `Femme`, `Sneakers`, `Accessoires`)
  - fallback categorie vide si invalide

4. Tests backend adaptes
- `ProductServiceTest`: verification query + category + fallback
- `ProductControllerTest`: param `category` et assertions JSON
- `ProductRepositoryTest`: couverture du filtre categorie en integration JPA

#### Ce qui a ete ajoute cote front (liaison progressive)

1. Query params URL synchronises
- `q`, `category`, `sortBy`, `sortDirection`, `page`
- hydratation store depuis URL au chargement
- mise a jour URL lors des actions utilisateur (recherche, tri, pagination, reset)

2. Service catalogue
- envoi du param `category` vers `/api/products/page`
- mapping direct de la categorie renvoyee par Spring
- fallback mock garde aussi le filtre categorie

#### Validation effectuee

- Backend: `mvnw test` OK (22 tests)
- Front: `npm run build` OK
- Front: `npm run test -- --watch=false` OK (59 tests)

#### A retenir

- La categorie doit vivre dans le modele backend, pas etre deduite uniquement cote front.
- Une URL synchronisee rend le catalogue partageable et reproductible.
- L'alignement contrat backend/front (query params + payload) limite les regressions.

### Session 7 - Migration SQL versionnee (Flyway)

#### Objectif

Passer d'une gestion implicite du schema (JPA auto-update) a une gestion explicite, traçable et reproductible.

#### Ce qui a ete fait

1. Integration Flyway
- dependance `org.flywaydb:flyway-core` ajoutee au `pom.xml`

2. Premiere migration versionnee
- fichier cree: `db/migration/V1__create_products_table.sql`
- contient:
  - creation de la table `products` avec `category`
  - indexes sur `name` et `category`

3. Configuration par profil
- `application-dev.properties`
  - `spring.jpa.hibernate.ddl-auto=validate`
  - `spring.flyway.enabled=true`
  - `spring.flyway.baseline-on-migrate=true`
- `application-prod.properties`
  - `spring.jpa.hibernate.ddl-auto=validate`
  - `spring.flyway.enabled=true`
  - `spring.flyway.baseline-on-migrate=true`
- `application-test.properties`
  - `spring.flyway.enabled=false`
  - tests conserves en H2 `create-drop` pour rapidite/isolation

#### Pourquoi `baseline-on-migrate=true`

Permet d'introduire Flyway sur une base dev/prod deja existante sans casser le premier demarrage.

#### Validation

- backend `mvnw test` OK: 22 tests, 0 echec

#### A retenir

- Les migrations versionnees deviennent la source de verite du schema.
- `ddl-auto=validate` protege contre les derives entre entites et base.
- L'adoption progressive (baseline) est utile en projet deja en cours.

### Session 8 - Seed de developpement avec Flyway (sans impacter la prod)

#### Objectif

Avoir des donnees de demo immediates en local, tout en gardant la production propre (pas de seed automatique en prod).

#### Strategie retenue

1. Emplacement Flyway specifique au profil dev
- `application-dev.properties` utilise:
  - `spring.flyway.locations=classpath:db/migration,classpath:db/migration-dev`
- resultat:
  - prod applique uniquement `db/migration`
  - dev applique schema + seed

2. Migration seed dediee
- nouveau fichier:
  - `db/migration-dev/V2__seed_products_dev.sql`
- insertion d'un jeu de produits de demo (4 categories)
- protection anti-doublon:
  - insertion uniquement si la table `products` est vide (`WHERE NOT EXISTS`)

#### Pourquoi c'est mieux

- Pas de pollution des donnees de prod.
- En dev, le catalogue est utilisable des le premier demarrage.
- Le seed reste versionne et partage dans le repo.

#### Validation

- backend `mvnw test` execute apres integration Flyway + seed dev.

### Session 9 - Route dev de reset/reseed du catalogue

#### Objectif

Recharger rapidement des donnees de demo pendant le developpement sans manipulation SQL manuelle.

#### Implementation

1. Composants dedies au profil dev
- `DevCatalogService` (`@Profile("dev")`)
  - supprime tout le catalogue (`deleteAllInBatch`)
  - reinjecte un jeu fixe de produits de demo
- `DevCatalogController` (`@Profile("dev")`)
  - endpoint: `POST /api/dev/catalog/reseed`
  - reponse JSON: message + `insertedCount`

2. Garantie de securite environnementale
- la route n'existe pas hors profil dev
- donc indisponible en prod et en test

#### Usage

- en local (profil dev actif):
  - appel HTTP POST sur `/api/dev/catalog/reseed`
  - le catalogue est reinitialise avec un dataset propre

#### A retenir

- Le tooling dev peut accelerer les tests manuels sans polluer la prod.
- `@Profile` est un garde-fou simple et efficace pour ce type de route.

### Session 10 - Documentation API complete

#### Objectif

Centraliser un contrat API lisible pour le front et pour les tests manuels.

#### Livrable

- nouveau fichier: `BACK_SPRING_API.md`
- contenu documente:
  - CRUD produits
  - endpoint pagine `/api/products/page` (q, category, page, size, sort)
  - endpoint dev reseed `/api/dev/catalog/reseed`
  - format d'erreur commun (`ApiErrorResponse`)
  - exemples `curl` et `Invoke-RestMethod`

#### A retenir

- Une doc API versionnee dans le repo reduit les malentendus front/back.
- Documenter explicitement les routes dev evite les usages en mauvais environnement.

### Session 11 - OpenAPI + Swagger UI (doc vivante)

#### Objectif

Rendre la documentation executable et testable depuis le navigateur.

#### Mise en place

1. Spec OpenAPI versionnee
- fichier: `src/main/resources/static/openapi.yaml`
- couvre:
  - CRUD produits
  - `/api/products/page`
  - `/api/dev/catalog/reseed`
  - schemas et erreurs standard

2. Swagger UI servie par l'application
- fichier: `src/main/resources/static/swagger.html`
- charge la spec locale `/openapi.yaml`

3. URLs locales
- spec: `/openapi.yaml`
- UI: `/swagger.html`

#### Pourquoi cette approche

- Pas de dependance runtime supplementaire fragile avec la stack actuelle.
- Doc versionnee, simple a partager et a faire evoluer.
- Le front et les tests manuels ont un point d'entree unique.

### Session 12 - Integration front/back complete (blocages leves)

#### Objectif

Lever les derniers blocages techniques pour que le front Angular et le back Spring fonctionnent ensemble en conditions reelles.

#### Blocages traites

1. CORS navigateur
- ajout d'une configuration CORS dans `SecurityConfig`
- origine autorisee: `http://localhost:4200`
- methodes API usuelles autorisees

2. API Categories manquante
- ajout du module complet `Category`:
  - entity, repository, mapper, service, controller
  - endpoints CRUD `/api/categories`
- gestion d'erreur 404 metier via `CategoryNotFoundException` + handler global

3. Checkout encore mock cote front
- ajout d'un endpoint backend `POST /api/orders`
- ajout DTOs `OrderRequest/OrderResponse`
- branchement du checkout front sur un vrai `OrderService` HTTP (au lieu du mock)

4. Contrat produit aligne front/back
- ajout du champ `category` dans le modele admin produit front
- formulaire admin produit adapte (saisie + validation)
- tests front adaptes

5. Base de donnees / migrations
- migration schema categories: `V3__create_categories_table.sql`
- seed dev categories: `V101__seed_categories_dev.sql`

6. Documentation API
- `BACK_SPRING_API.md` et `openapi.yaml` enrichis avec categories + orders

#### Validation effectuee

- backend `mvnw test`: OK (22 tests)
- front `npm run build`: OK
- front `npm run test -- --watch=false`: OK (59 tests)

#### A retenir

- L'integration front/back reelle depend autant du contrat API que de l'infrastructure HTTP (CORS).
- Les migrations et DTOs stabilisent le comportement entre environnements.
- Remplacer les mocks par des endpoints reels doit se faire avec tests + doc a jour.

### Session 13 - Couverture de tests backend etendue (Category + Order)

#### Objectif

Verifier explicitement les nouveaux modules ajoutes pour l'integration complete front/back.

#### Ce qui a ete ajoute

1. Tests web Category controller
- fichier: `src/test/java/com/ecommerce/backend/category/CategoryControllerTest.java`
- cas couverts:
  - GET liste categories
  - GET by id (200)
  - GET by id (404)
  - POST valide (201)
  - POST invalide (400 + validationErrors)
  - PUT valide (200)
  - DELETE (204)
  - DELETE not found (404)

2. Tests web Order controller
- fichier: `src/test/java/com/ecommerce/backend/order/OrderControllerTest.java`
- cas couverts:
  - POST order valide (200)
  - POST order invalide (400 + validationErrors)

#### Validation

- backend `mvnw test` OK
- total: 32 tests, 0 failures, 0 errors

#### A retenir

- Une integration fonctionnelle doit etre protegee par des tests de contrat API sur chaque nouveau module.
- Les tests web (MockMvc) permettent de valider rapidement statut HTTP + payload + gestion d'erreurs.

### Session 14 - PostgreSQL Docker + pgAdmin

#### Objectif

Pouvoir lancer la base et l'observer graphiquement avec pgAdmin, tout en conservant les donnees entre les redemarrages.

#### Fichier Compose

- fichier racine: `docker-compose.yml`
- services:
  - `postgres`: PostgreSQL 16
  - `pgadmin`: interface graphique PostgreSQL
- volumes persistants:
  - `ecommerce_postgres_data`
  - `ecommerce_pgadmin_data`

#### Commandes

Depuis la racine du projet:

```powershell
docker compose up -d
docker compose ps
docker compose logs -f postgres
```

Arreter les services sans supprimer les donnees:

```powershell
docker compose down
```

Supprimer aussi les donnees (commande destructive):

```powershell
docker compose down -v
```

#### Connexions

Backend Spring depuis Windows:
- host: `127.0.0.1`
- port: `55432`
- database: `ecommerce_dev_db`
- user: `ecommerce_app`
- password: `ecommerce_app_pwd`

pgAdmin dans le navigateur:
- URL: `http://localhost:5050`
- email: `admin@ecommerce.local`
- password: `admin_ecommerce_pwd`

Dans pgAdmin, pour enregistrer PostgreSQL:
- host: `postgres` (nom du service Docker, pas `localhost`)
- port: `5432`
- database: `ecommerce_dev_db`
- user/password: identifiants PostgreSQL ci-dessus

#### A retenir

- Un volume persistant evite de perdre la base quand le conteneur est recree.
- Depuis l'hote Windows, on utilise le port publie `55432`.
- Depuis un autre conteneur Compose, on utilise le nom de service `postgres` et le port interne `5432`.

### Session 15 - Transition vers Docker Compose sans perte de donnees

#### Probleme rencontre

Un conteneur PostgreSQL existait deja avec le nom `ecommerce-postgres`. Compose ne pouvait donc pas recreer un conteneur du meme nom.

#### Solution appliquee

1. Conservation du volume existant
- le volume Docker PostgreSQL existant a ete declare `external` dans `docker-compose.yml`
- les donnees, migrations Flyway et tables existantes sont conservees

2. Remplacement du conteneur uniquement
- ancien conteneur arrete puis supprime
- volume conserve
- nouveau conteneur gere par Compose recree avec le meme port `55432`

3. pgAdmin ajoute au meme Compose
- URL: `http://localhost:5050`
- connexion pgAdmin vers le service Docker `postgres:5432`

4. Volume rendu portable
- les donnees ont ete copiees dans le volume nomme `e-commerce_ecommerce_postgres_data`
- Compose peut maintenant recreer la stack sans dependre d'un identifiant technique de volume

#### Demarrage recommande

```powershell
cd C:\Users\grine\projects\E-commerce
docker compose up -d
```

Puis:
- backend Spring: `back-spring\\mvnw.cmd spring-boot:run`
- frontend Angular: `Front\\npm.cmd start`

#### Validation

- `docker compose config` valide
- `docker compose up -d` execute avec succes
- PostgreSQL healthy
- pgAdmin demarre
- volume de donnees preserve

### Session 16 - Upload d'images produit

#### Objectif

Permettre a l'administrateur de selectionner une image locale depuis le formulaire produit, puis de la reutiliser dans la galerie produit.

#### Implementation backend

- `ProductImageController` expose `POST /api/products/images`
- reception via `multipart/form-data`, champ `file`
- `ProductImageStorageService`:
  - accepte JPEG, PNG, GIF et WebP
  - limite la taille a 5 Mo
  - genere un nom UUID pour eviter les collisions et noms dangereux
  - stocke dans `uploads/products`
- `WebConfig` sert les fichiers via `/uploads/products/**`
- limites multipart configurees dans `application-dev.properties`

#### Implementation front

- `ProductService.uploadImage(file)` envoie le fichier avec `FormData`
- le formulaire admin propose un selecteur de fichier, un apercu et la suppression
- l'URL retournee est ajoutee a `imageUrls`
- l'ajout manuel par URL reste disponible

#### Flux complet

```text
Fichier local -> FormData -> POST /api/products/images
             -> fichier uploads/products
             -> URL retournee
             -> imageUrls du produit
             -> POST/PUT /api/products
```

#### A retenir

- Un upload ne doit pas faire confiance au nom original du fichier.
- La taille et le type MIME doivent etre limites cote backend.
- Les fichiers uploades doivent etre servis par une route controlee.

### Session 17 - Selection de l'image principale

#### Objectif

Permettre plusieurs images par produit et donner le controle de l'image affichee en premier.

#### Fonctionnement

- la galerie conserve les images dans `imageUrls`
- l'action `Definir principale` deplace l'image choisie en position 0
- l'image en position 0 est utilisee comme:
  - image principale dans la fiche produit
  - premiere image de la galerie publique
- l'ordre est persiste cote backend avec `@OrderColumn(name = "position")`

#### A retenir

- La notion d'image principale est geree par l'ordre de la collection.
- Reordonner avant le `POST`/`PUT` permet de conserver le contrat API existant.

### Session 18 - Configuration administrable de la home

#### Objectif

Permettre a l'administrateur de modifier le contenu principal de la home sans changer le code Angular.

#### Donnees configurables

- titre principal
- texte descriptif sous le titre
- produit vedette affiche a droite

#### Backend

- nouvelle table `home_configurations`
- une configuration unique identifiee par `config_key = home`
- endpoints:
  - `GET /api/home/configuration`
  - `PUT /api/home/configuration`
- le produit selectionne est verifie avant sauvegarde
- migration: `V103__create_home_configurations_table.sql`

#### Frontend

- nouvel ecran admin: `/admin/home`
- formulaire de titre et texte
- selecteur des produits existants
- apercu du produit choisi
- home publique charge la configuration depuis l'API
- fallback sur le contenu par defaut si l'API est indisponible

#### Flux

```text
Admin -> PUT configuration -> PostgreSQL
Visiteur -> GET configuration + GET products -> Home publique
```

#### A retenir

- Le contenu editorial configurable doit etre separe du code de presentation.
- Une reference `featuredProductId` evite de dupliquer les donnees produit.
- Le fallback protege l'experience publique pendant une panne backend.

### Session 19 - Fiabilisation de la sauvegarde home

#### Probleme

Apres un clic sur enregistrer, les anciennes valeurs pouvaient reapparaitre si une reponse de chargement initial arrivait apres la modification.

#### Diagnostic

- le `PUT /api/home/configuration` persistait correctement en base
- le test direct `PUT` puis `GET` confirmait la nouvelle valeur
- le risque se situait donc dans le cycle de vie de l'ecran Angular et ses requetes concurrentes

#### Correction

- l'ecran marque la configuration comme chargee apres une sauvegarde reussie
- une reponse tardive du chargement initial ne remplace plus les valeurs deja sauvegardees
- le bouton reste bloque pendant la sauvegarde avec un libelle explicite

#### Validation

- backend: PUT/GET direct confirme la persistance
- front: 60 tests passants

#### A retenir

- Une sauvegarde asynchrone doit proteger l'etat local contre les reponses obsoletes.
- Tester le backend directement permet de separer un probleme de persistance d'un probleme UI.

### Session 20 - Sauvegarde home sans refresh navigateur

#### Probleme

Le bouton de sauvegarde etait un bouton `submit` dans un formulaire. Le comportement HTML natif pouvait recharger la page avant ou pendant l'appel Angular.

#### Correction

- le formulaire bloque explicitement l'evenement `submit`
- le bouton utilise `type="button"`
- le clic appelle directement `save()`
- aucun `router.navigate` ni refresh n'est execute apres le `PUT`
- les signaux locaux sont mis a jour avec la reponse API
- toaster Material ajoute:
  - succes apres sauvegarde
  - erreur si le `PUT` echoue

#### Validation

- front build: OK
- tests front: 60 tests passants

#### A retenir

- Pour une sauvegarde SPA, eviter le submit HTML natif si aucune navigation n'est necessaire.
- Le feedback utilisateur doit distinguer succes et erreur de l'appel API.

### Session 21 - Conservation du produit sélectionné au chargement

#### Probleme

La configuration API contenait bien `featuredProductId`, mais le select pouvait afficher l'option vide a cause d'une comparaison entre valeurs numeriques et valeurs texte du DOM.

#### Correction

- conversion explicite de l'id en valeur texte pour le `select`
- conversion controlee de la valeur choisie vers un nombre
- attribut `selected` applique explicitement sur l'option correspondante

#### A retenir

- Les valeurs d'un select HTML sont des chaines, meme si les ids metier sont numeriques.
- Aligner explicitement les types evite de perdre une selection deja persistee.

### Session 22 - Detail et workflow de validation des commandes

#### Objectif

Donner a l'admin une vue complete d'une commande et un workflow de suivi modifiable.

#### Backend

- nouveau detail: `GET /api/orders/{orderId}`
- mise a jour admin: `PUT /api/orders/{orderId}`
- payload de mise a jour:
  - `status`
  - `note`
- statuts autorises:
  - `EN_ATTENTE_VALIDATION_ADMIN`
  - `ANNULEE`
  - `VALIDEE_PAR_LE_CLIENT`
  - `LIVREE_ET_PAYEE`
  - `RETOURNEE_PAR_LE_CLIENT`
  - `LIVRAISON_EN_COURS`
- toute nouvelle commande commence par `EN_ATTENTE_VALIDATION_ADMIN`
- migration des anciennes commandes `confirmed` vers ce statut

#### Frontend admin

- bouton `Détail` dans la liste des commandes
- panneau detail sans refresh de page contenant:
  - informations client et livraison
  - articles et total
  - selecteur de statut
  - champ note admin
- sauvegarde asynchrone et toast de succes/erreur
- la ligne de commande est mise a jour localement apres reponse API

#### A retenir

- Les statuts doivent etre bornes par une liste autorisee cote backend.
- Le detail utilise un DTO dedie pour ne pas exposer directement l'entity.
- Une mise a jour UI locale evite un rechargement inutile apres une sauvegarde.

### Session 23 - Sous-categories et navigation catalogue

#### Objectif

Organiser les produits par categorie parent et sous-categorie, par exemple `Homme > T-shirt`.

#### Backend

- `Category` accepte un parent via `parent_id`
- `CategoryRequest` accepte `parentId`
- `CategoryResponse` renvoie `parentId` et `parentName`
- `Product` accepte `subcategory`
- recherche paginee accepte `subcategory`
- migration: `V105__add_category_hierarchy.sql`

#### Admin

- creation/modification d'une categorie:
  - choix `Catégorie principale`
  - ou choix d'une categorie parent
- formulaire produit:
  - choix du catalogue parent
  - choix de la sous-categorie dependante
- liste categories affiche la relation parent/enfant

#### Boutique publique

- `/category/Homme` ouvre le catalogue filtre sur Homme
- la page `/shop` propose un filtre sous-categorie
- les URLs peuvent contenir `category` et `subcategory`
- le backend applique le filtre en base avant pagination

#### A retenir

- Une sous-categorie est une categorie avec un parent, pas un simple texte isole.
- Le produit conserve le parent et la sous-categorie pour un affichage et un filtrage simples.
- Le filtrage doit etre applique avant la pagination pour afficher des resultats coherents.

### Session 24 - Sous-categories chargees depuis la base

#### Probleme observe

Le select des sous-categories etait grise car le store public cherchait les sous-categories uniquement dans les produits deja charges sur la page courante.

#### Correction

- ajout de `listCategories()` dans le service catalogue public
- le store charge `/api/categories` et conserve `availableCategories`
- les sous-categories sont calculees depuis `parentId`, independamment de la page produits
- le select envoie ensuite `subcategory` a `/api/products/page`
- sur `/category/Homme`, le filtre parent `Tous/Homme` est masque car la categorie est deja imposee par la route

#### Flux attendu

```text
/category/Homme
  -> category = Homme
  -> GET /api/categories
  -> sous-categories parentId = id(Homme)
  -> selection T-Shirt
  -> GET /api/products/page?category=Homme&subcategory=T-Shirt
```

#### A retenir

- Les options de filtre doivent venir de la ressource de reference (`categories`), pas seulement des resultats pagines.
- Une page de categorie ne doit pas afficher un filtre qui contredit son contexte.

### Session 25 - Sauvegarde produit admin sans rechargement

#### Probleme

Le formulaire produit utilisait encore un `submit` HTML natif. Le navigateur pouvait recharger la page avant que la creation ou la modification Angular soit correctement traitee.

#### Correction

- formulaire configure avec `(submit)="$event.preventDefault()"`
- bouton configure en `type="button"`
- appel explicite de `save()` au clic
- protection contre les doubles clics pendant `saving()`
- toaster de validation si les champs sont invalides
- toaster de succes distinct pour creation et modification
- toaster d'erreur si le store/API echoue

#### A retenir

- Dans une SPA, une action API doit rester controlee par Angular.
- Le message de succes ne doit apparaitre qu'apres la reponse positive du backend.

### Session 26 - Modification sous-categorie produit sans redirection

#### Probleme

La modification generale du produit fonctionnait, mais la sous-categorie n'etait pas traitee par une action explicite dans le formulaire et la sauvegarde redirigeait automatiquement.

#### Correction

- ajout de `updateSubcategory(value)` dans le formulaire
- la valeur est conservee dans `model().subcategory`
- `toPayload()` transmet cette valeur au `PUT /api/products/{id}`
- `ProductMapper` mappe la sous-categorie aussi bien en creation qu'en modification
- apres succes:
  - toast de confirmation
  - aucune navigation automatique
  - retour vers la liste uniquement via le bouton retour manuel

#### A retenir

- Chaque champ important doit avoir un flux de mise a jour clair jusqu'au DTO.
- Une edition admin ne doit pas perdre le contexte de travail apres une sauvegarde.

### Session 27 - Saisons multiples et filtre catalogue

#### Objectif

Associer plusieurs saisons a un produit, par exemple `Hiver` et `Été`, puis filtrer le catalogue public.

#### Backend

- ajout de la collection `Product.seasons`
- stockage dans `product_seasons` avec ordre preserve
- `ProductRequest` et `ProductResponse` exposent `seasons`
- migration: `V106__add_product_seasons.sql`
- endpoint pagine accepte `season`
- valeurs autorisees:
  - `Printemps`
  - `Été`
  - `Automne`
  - `Hiver`

#### Frontend

- formulaire produit admin:
  - boutons multi-selection comme pour les tailles
  - plusieurs saisons peuvent etre cochees
- page `/shop`:
  - select `Toutes les saisons`
  - filtrage par saison
  - parametre URL `season`

#### A retenir

- Une relation multi-valeurs se modelise par une collection, pas par une chaine concatenee.
- Le filtre saison est applique cote serveur avant pagination.
