import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { MainLayoutComponent } from './shared/layout/main-layout/main-layout.component';
import { HomeComponent } from './features/home/home.component';
import { authGuard } from './core/guards/auth.guard';
import { noAuthGuard } from './core/guards/no-auth.guard';
import { ProductDetailComponent } from './features/product/product-list/product-detail/product-detail';
import { ShoppingListComponent } from './features/shopping-list/shopping-list.component';

export const routes: Routes = [
  { path: '', redirectTo: '/home', pathMatch: 'full' },
  { path: 'login', component: LoginComponent, canActivate: [noAuthGuard] },
  { path: 'register', component: RegisterComponent, canActivate: [noAuthGuard] },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'home', component: HomeComponent },
      { path: 'producto/:id', component: ProductDetailComponent },
      { path: 'lists', component: ShoppingListComponent }
    ]
  },
  { path: '**', redirectTo: '/login' }
];
