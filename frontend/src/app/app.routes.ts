import { Routes } from '@angular/router';
import { noAuthGuard } from './core/guards/no-auth.guard';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/home', pathMatch: 'full' },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent),
    canActivate: [noAuthGuard]
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent),
    canActivate: [noAuthGuard]
  },
  {
    path: 'forgot-password',
    loadComponent: () => import('./features/auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent),
    canActivate: [noAuthGuard]
  },
  {
    path: 'admin',
    loadChildren: () => import('./features/admin/admin.routes').then(m => m.ADMIN_ROUTES)
  },
  {
    path: '',
    loadComponent: () => import('./shared/layout/main-layout/main-layout.component').then(m => m.MainLayoutComponent),
    canActivate: [authGuard],
    children: [
      { path: 'home', loadComponent: () => import('./features/home/home.component').then(m => m.HomeComponent) },
      { path: 'producto/:id', loadComponent: () => import('./features/product/product-list/product-detail/product-detail').then(m => m.ProductDetailComponent) },
      { path: 'lists', loadComponent: () => import('./features/shopping-list/shopping-list.component').then(m => m.ShoppingListComponent) },
      { path: 'grupos', loadComponent: () => import('./features/groups/groups.component').then(m => m.GroupsComponent) },
      { path: 'grupos/:id', loadComponent: () => import('./features/groups/group-detail/group-detail.component').then(m => m.GroupDetailComponent) },
      { path: 'settings', loadComponent: () => import('./features/user/settings/settings.component').then(m => m.SettingsComponent) }
    ]
  },
  { path: '**', redirectTo: '/login' }
];
