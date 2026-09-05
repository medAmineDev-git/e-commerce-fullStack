import { Routes } from '@angular/router';
import { anonymousOnlyGuard, platformGuard, storeOwnerGuard } from './core/auth/auth.guard';
import { storeResolverGuard } from './core/services/store.resolver';

/**
 * Quatre espaces distincts :
 *
 *   /                    site vitrine du service, prérendu pour le référencement
 *   /boutique/:slug/**   vitrine d'une boutique, périmètre donné par le slug
 *   /admin/**            back-office du propriétaire
 *   /plateforme          exploitation de la plateforme
 *
 * La racine appartient au site vitrine : c'est le choix du routage par
 * sous-chemin qui la libère, les boutiques vivant sous /boutique/:slug.
 */
export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () =>
      import('./features/landing/landing-page/landing-page').then((m) => m.LandingPage),
  },
  {
    path: 'inscription',
    canActivate: [anonymousOnlyGuard],
    loadComponent: () =>
      import('./features/auth/register-page/register-page').then((m) => m.RegisterPage),
  },
  {
    path: 'connexion',
    canActivate: [anonymousOnlyGuard],
    loadComponent: () => import('./features/auth/login-page/login-page').then((m) => m.LoginPage),
  },
  {
    path: 'boutique-introuvable',
    loadComponent: () =>
      import('./features/public/store-not-found/store-not-found').then((m) => m.StoreNotFound),
  },
  {
    path: 'plateforme',
    canActivate: [platformGuard],
    loadComponent: () =>
      import('./features/platform/platform-console/platform-console').then((m) => m.PlatformConsole),
  },
  {
    path: 'admin',
    canActivate: [storeOwnerGuard],
    loadComponent: () =>
      import('./features/admin/admin-layout/admin-layout').then((m) => m.AdminLayout),
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/admin/dashboard/dashboard').then((m) => m.Dashboard),
      },
      {
        path: 'configuration',
        loadComponent: () =>
          import('./features/admin/store-settings/store-settings').then((m) => m.StoreSettings),
      },
      {
        path: 'pages',
        loadComponent: () =>
          import('./features/admin/page-manager/page-manager').then((m) => m.PageManager),
      },
      {
        path: 'products',
        loadComponent: () =>
          import('./features/admin/product-list/product-list').then((m) => m.ProductList),
      },
      {
        path: 'products/new',
        loadComponent: () =>
          import('./features/admin/product-form/product-form').then((m) => m.ProductForm),
      },
      {
        path: 'products/:id',
        loadComponent: () =>
          import('./features/admin/product-form/product-form').then((m) => m.ProductForm),
      },
      {
        path: 'categories',
        loadComponent: () =>
          import('./features/admin/category-list/category-list').then((m) => m.CategoryList),
      },
      {
        path: 'categories/new',
        loadComponent: () =>
          import('./features/admin/category-form/category-form').then((m) => m.CategoryForm),
      },
      {
        path: 'categories/:id',
        loadComponent: () =>
          import('./features/admin/category-form/category-form').then((m) => m.CategoryForm),
      },
      {
        path: 'orders',
        loadComponent: () => import('./features/admin/order-list/order-list').then((m) => m.OrderList),
      },
      {
        path: 'home',
        loadComponent: () =>
          import('./features/admin/home-configuration/home-configuration').then(
            (m) => m.HomeConfigurationPage,
          ),
      },
    ],
  },
  {
    path: 'boutique/:slug',
    canActivate: [storeResolverGuard],
    loadComponent: () =>
      import('./features/public/public-layout/public-layout').then((m) => m.PublicLayout),
    children: [
      {
        path: '',
        loadComponent: () => import('./features/public/home-page/home-page').then((m) => m.HomePage),
      },
      {
        path: 'product/:id',
        loadComponent: () =>
          import('./features/public/product-detail-page/product-detail-page').then(
            (m) => m.ProductDetailPage,
          ),
      },
      // La page boutique a fusionne avec l'accueil : les anciens liens y menent.
      { path: 'shop', redirectTo: '', pathMatch: 'full' },
      { path: 'category/:category', redirectTo: '', pathMatch: 'full' },
      {
        path: 'cart',
        loadComponent: () => import('./features/public/cart-page/cart-page').then((m) => m.CartPage),
      },
      {
        path: 'checkout',
        loadComponent: () =>
          import('./features/public/checkout-page/checkout-page').then((m) => m.CheckoutPage),
      },
      {
        path: 'order-success',
        loadComponent: () =>
          import('./features/public/order-success-page/order-success-page').then(
            (m) => m.OrderSuccessPage,
          ),
      },
      // Pages de contenu du vendeur : mentions légales, livraison, retours.
      {
        path: 'page/:pageSlug',
        loadComponent: () =>
          import('./features/public/store-page/store-page').then((m) => m.StorePageView),
      },
    ],
  },
  // Anciennes adresses de connexion, conservées le temps que les signets suivent.
  { path: 'login', redirectTo: 'connexion' },
  { path: '**', redirectTo: '' },
];
