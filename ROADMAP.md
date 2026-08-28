# Roadmap — Site E-commerce Vêtements (Angular + Spring Boot)

## Contexte
- Site e-commerce de vente de vêtements.
- Frontend : Angular
- Backend : Java Spring Boot
- Pas de paiement en ligne au démarrage (paiement à la livraison / virement bancaire manuel).
- Paiement en ligne (Stripe/PayPal/CMI...) prévu en phase ultérieure.

## Mode d'apprentissage
- **Frontend (Angular)** : niveau expert → développement rapide, peu d'explications, focus résultat.
- **Backend (Spring Boot)** : niveau débutant total (Java ET Spring Boot) → approche pédagogique, on avance étape par étape.
  - Profil : débutant total en Java (langage) et en Spring Boot.
  - IDE : VS Code (extensions Java + Spring Boot à installer).
  - Rythme : construction **ligne par ligne**, ensemble — pas de génération de gros blocs de code d'un coup.
  - Les concepts Java (POO, classes, annotations, types...) sont expliqués au fur et à mesure, directement appliqués à Spring Boot (apprentissage "par la pratique", pas de cours Java isolé).
  - Chaque brique (entité, repository, service, controller, sécurité...) est expliquée avant/pendant l'implémentation : à quoi ça sert, pourquoi cette couche existe, comment ça s'articule avec le reste.
  - Checkpoints réguliers pour valider la compréhension avant de passer à la suite.

## Avis d'expert
Démarrer sans paiement en ligne est une approche saine et courante : ça évite la complexité PCI-DSS, les intégrations Stripe/PayPal et la gestion des remboursements/webhooks dès le départ. Options de démarrage :
- Paiement à la livraison (COD)
- Virement bancaire manuel avec confirmation par un admin
- Statut de commande "en attente de paiement" géré manuellement

Le paiement en ligne est ajouté en V2 une fois le flux métier validé (approche courante notamment sur les marchés où le COD domine, ex. Maroc/Algérie/Tunisie).

## Stack technique recommandée

| Couche | Techno |
|---|---|
| Frontend | Angular (dernière LTS), Angular Material ou Tailwind CSS |
| Backend | Spring Boot 3.x, Java 17/21 |
| Auth | Spring Security + JWT |
| DB | PostgreSQL (ou MySQL) |
| ORM | Spring Data JPA / Hibernate |
| Upload images | Stockage local au départ, puis S3/Cloudinary en V2 |
| Recherche | Filtres SQL simples au départ, Elasticsearch si besoin plus tard |
| Documentation API | Swagger/OpenAPI |

## Modèle de données de base
- **User** (client / admin, rôles)
- **Product** (nom, description, prix, catégorie, tailles, couleurs, stock, images)
- **Category** (homme, femme, enfant, accessoires...)
- **Cart / CartItem**
- **Order / OrderItem** (statut : EN_ATTENTE, CONFIRMÉE, EXPÉDIÉE, LIVRÉE, ANNULÉE)
- **Address** (livraison/facturation)
- **Review** (avis produit, optionnel V1)

## Roadmap par phases

### Phase 0 — Setup (1-2 jours)
- Init repo Spring Boot (Web, JPA, Security, Validation, PostgreSQL driver)
- Init projet Angular (routing, Material/Tailwind, environments)
- Config CORS, structure des dossiers, Docker-compose (DB) optionnel

### Phase 1 — MVP catalogue & panier (2-3 semaines)
- CRUD produits/catégories (admin uniquement)
- Authentification (inscription/connexion client, JWT)
- Catalogue public : liste produits, filtres (catégorie, prix, taille), fiche produit
- Panier (ajout/suppression/quantité) — persistant en base pour user connecté
- Responsive mobile-first (essentiel pour la mode)

### Phase 2 — Commande sans paiement en ligne (1-2 semaines)
- Tunnel de commande : adresse livraison, choix mode paiement (COD / virement)
- Création commande + récapitulatif email (Spring Mail)
- Espace client : historique commandes, suivi statut
- Espace admin : gestion des commandes (changer statut), gestion stock

### Phase 3 — Amélioration UX & back-office (2 semaines)
- Dashboard admin (stats ventes, produits populaires)
- Gestion des promotions/codes réduction
- Gestion des avis clients
- Notifications email (confirmation commande, expédition)

### Phase 4 — Paiement en ligne (quand prêt)
- Intégration Stripe/CMI/PayPal selon marché
- Webhooks de confirmation de paiement
- Gestion des remboursements

### Phase 5 — Scalabilité & marketing
- SEO (SSR avec Angular Universal)
- Cache (Redis)
- Recommandations produits
- Multi-langue (fr/en/ar si Maghreb)

## Points d'attention sécurité (dès le départ)
- Validation stricte des entrées (Bean Validation côté back, jamais confiance au front)
- JWT avec expiration courte + refresh token
- Rôles ADMIN/CLIENT bien séparés (Spring Security `@PreAuthorize`)
- Protection CSRF/XSS, échapper les entrées utilisateur (avis produits)
- Ne jamais exposer d'IDs internes sensibles sans vérif d'autorisation (IDOR)

## Décisions en attente
- [x] Base de données : PostgreSQL
- [ ] Librairie UI Angular : Angular Material, Tailwind CSS ou PrimeNG ?
- [x] Démarrer le scaffolding du projet (Phase 0) — squelette Spring Boot généré dans `back-spring/`
- [x] Backend unique : Spring Boot

## Etat de la V1
- `back-spring/` : API Spring Boot (Java), PostgreSQL `ecommerce_dev_db`.
- `Front/` : application Angular connectee a l'API Spring via `apiBaseUrl`.
- Le catalogue, le panier, le tunnel de commande et l'administration des produits/categories sont disponibles.
- Les commandes sont persistees, le total est calcule par le serveur et le stock est debite de maniere transactionnelle.
