import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'admin',
    loadComponent: () => import('./features/admin/admin-layout/admin-layout').then((m) => m.AdminLayout),
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/admin/dashboard/dashboard').then((m) => m.Dashboard),
      },
      {
        path: 'products',
        loadComponent: () => import('./features/admin/product-list/product-list').then((m) => m.ProductList),
      },
      {
        path: 'products/new',
        loadComponent: () => import('./features/admin/product-form/product-form').then((m) => m.ProductForm),
      },
      {
        path: 'products/:id',
        loadComponent: () => import('./features/admin/product-form/product-form').then((m) => m.ProductForm),
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
    path: '',
    loadComponent: () =>
      import('./features/public/public-layout/public-layout').then((m) => m.PublicLayout),
    children: [
      {
        path: '',
        loadComponent: () => import('./features/public/home-page/home-page').then((m) => m.HomePage),
      },
      {
        path: 'shop',
        loadComponent: () => import('./features/public/shop-page/shop-page').then((m) => m.ShopPage),
      },
      {
        path: 'category/:category',
        loadComponent: () => import('./features/public/shop-page/shop-page').then((m) => m.ShopPage),
      },
      {
        path: 'product/:id',
        loadComponent: () =>
          import('./features/public/product-detail-page/product-detail-page').then(
            (m) => m.ProductDetailPage,
          ),
      },
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
    ],
  },
  { path: '**', redirectTo: '' },
];
