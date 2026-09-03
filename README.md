# E-commerce multi-boutique

Plateforme de création de boutiques de vêtements en ligne : un site vitrine qui
présente le service, et autant de boutiques que de vendeurs inscrits, chacune
avec son catalogue, ses commandes et son back-office.

Frontend Angular, API Spring Boot, PostgreSQL.

## Espaces

| Adresse | Qui | Rendu |
|---|---|---|
| `/` | Visiteurs — présentation du service | Prérendu à la compilation |
| `/inscription`, `/connexion` | Création de boutique et accès | Client |
| `/boutique/{slug}` | Clients d'une boutique | Client |
| `/admin` | Propriétaire de la boutique | Client |
| `/plateforme` | Exploitation de la plateforme | Client |

Le routage se fait par sous-chemin. Le champ `stores.domain` et la résolution
par domaine existent déjà côté serveur : un domaine propre peut être rattaché
plus tard sans réécrire le frontend.

## Isolation entre boutiques

C'est la propriété structurante du projet, et elle est tenue par des tests.

- Aucune méthode de service ne s'exécute sans boutique : il n'existe pas de
  surcharge non bornée, et aucune requête de dépôt sans `store`.
- Le périmètre d'une requête d'administration vient du jeton signé, puis la
  boutique est relue en vérifiant l'appartenance.
- Une ressource appartenant à une autre boutique répond **404**, jamais 403 :
  le code 403 révélerait son existence.
- Les visuels sont partitionnés par boutique, sous des noms non devinables.
- `StoreIsolationTest` couvre la matrice des accès croisés ;
  `MultiStoreMigrationTest` rejoue les migrations sur un PostgreSQL réel.

## Surfaces d'API

```text
/api/public/stores/{slug}/**   vitrine, anonyme, périmètre donné par le slug
/api/admin/**                  back-office, périmètre donné par le jeton
/api/platform/**               exploitation, réservé au rôle plateforme
/api/auth/**                   connexion, inscription, rafraîchissement
```

Les routes historiques `/api/products`, `/api/categories`, `/api/orders` et
`/api/home/configuration` ont été retirées : elles servaient le catalogue de
toutes les boutiques confondues dès que l'appelant était anonyme.

## Rôles

Deux rôles, contraints en base par `V110` :

- `ROLE_STORE_OWNER` — administre une boutique, et une seule.
- `ROLE_SUPER_ADMIN` — exploite la plateforme.

`ROLE_ADMIN` a disparu : c'était un rôle de boutique qui ouvrait la console
plateforme. Les comptes qui le portaient ont été convertis par la migration.

## Jetons

Jeton d'accès de 15 minutes portant l'identifiant et le slug de la boutique,
jeton de rafraîchissement de 30 jours. Le rafraîchissement est le point où les
droits sont revus : un compte supprimé ou une boutique désactivée cessent
d'obtenir des jetons.

## Démarrage local

1. Créer la base PostgreSQL `ecommerce_dev_db` et un utilisateur correspondant à
   `DB_USER` / `DB_PASSWORD` (par défaut `ecommerce_app`).
2. Lancer l'API : `cd back-spring && ./mvnw spring-boot:run`
3. Lancer le frontend : `cd Front && npm ci && npm start`
4. Ouvrir `http://localhost:4200`

L'API écoute sur `http://localhost:8080`, sa documentation sur
`http://localhost:8080/swagger.html`.

Le profil `dev` crée deux comptes :

```text
admin / admin          proprietaire de boutique
platform / platform    exploitation de la plateforme
```

## Tests

```bash
cd back-spring && ./mvnw test    # Docker requis : Testcontainers pour les migrations
cd Front && npm test -- --run
```

La suite applicative tourne sur H2 avec Flyway désactivé. Les migrations sont
donc vérifiées séparément, sur un PostgreSQL réel, par `MultiStoreMigrationTest`.

## Référencement

Seule la page `/` est prérendue : son contenu est figé dans le HTML livré, donc
lisible sans exécuter de JavaScript. Elle porte ses balises `<meta>`, ses balises
Open Graph et deux blocs JSON-LD (`SoftwareApplication` et `FAQPage`).

Les vitrines restent en rendu client : les prérendre supposerait d'énumérer
toutes les boutiques et tous leurs produits à la compilation, alors qu'une
boutique naît après le déploiement.

`public/robots.txt` et `public/sitemap.xml` demandent le domaine réel au moment
de la mise en ligne — il n'est pas connu à la compilation.

## Déploiement

Le frontend produit un bundle **statique** (`outputMode: static`) : le prérendu
n'exige aucun process Node. Voir `render.yaml` et le `Dockerfile` racine pour le
backend.

Variables attendues par l'API en production :

```text
SPRING_PROFILES_ACTIVE=prod
DB_URL, DB_USER, DB_PASSWORD
JWT_SECRET
APP_CORS_ALLOWED_ORIGINS   origines de base, en plus des domaines enregistrés
```

Les origines CORS sont calculées à partir des domaines des boutiques actives :
rattacher un domaine ne demande pas de redéploiement.
