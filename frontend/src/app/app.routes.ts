import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { MainLayoutComponent } from './shared/layout/main-layout/main-layout.component';
import { HomeComponent } from './features/home/home.component';
import { authGuard } from './core/guards/auth.guard';
import { noAuthGuard } from './core/guards/no-auth.guard';
import { ProductDetailComponent } from './features/product/product-list/product-detail/product-detail';


export const routes: Routes = [
  { path: '', redirectTo: '/home', pathMatch: 'full' },
  { path: 'login', component: LoginComponent, canActivate: [noAuthGuard] },
  { path: 'register', component: RegisterComponent, canActivate: [noAuthGuard] },
  { path: 'home', component: HomeComponent, canActivate: [authGuard] },
  { path: 'producto/:id', component: ProductDetailComponent },
  { path: '', redirectTo: 'producto/1', pathMatch: 'full' },
  { path: '**', redirectTo: '/login' },
];

export const productRoutes: Routes = [
  { path: '', redirectTo: 'producto/1', pathMatch: 'full' }, // Redirigir a un ejemplo por defecto
  { path: 'producto/:id', component: ProductDetailComponent },
  { path: '**', redirectTo: 'producto/1' } // Manejo de errores
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'home', component: HomeComponent },
      // Tu companera anadira aqui:
      // { path: 'products/:id', component: ProductDetailComponent }
    ]
  },
  { path: '**', redirectTo: '/home' }
];
