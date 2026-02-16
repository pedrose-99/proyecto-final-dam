import { Routes } from '@angular/router';
import { AdminLayoutComponent } from './admin-layout/admin-layout.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { ScrapingControlComponent } from './scraping-control/scraping-control.component';
import { ScrapingLogsComponent } from './scraping-logs/scraping-logs.component';
import { UserManagementComponent } from './user-management/user-management.component';
import { StoreManagementComponent } from './store-management/store-management.component';

export const ADMIN_ROUTES: Routes = [
    {
        path: '',
        component: AdminLayoutComponent,
        children: [
            { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
            { path: 'dashboard', component: DashboardComponent },
            { path: 'scraping', component: ScrapingControlComponent },
            { path: 'logs', component: ScrapingLogsComponent },
            { path: 'users', component: UserManagementComponent },
            { path: 'stores', component: StoreManagementComponent }
        ]
    }
];
