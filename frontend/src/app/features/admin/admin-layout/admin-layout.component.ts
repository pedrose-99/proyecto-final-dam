import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatMenuModule } from '@angular/material/menu';
import { AuthService } from '../../../core/services/auth.service';

@Component({
    selector: 'app-admin-layout',
    standalone: true,
    imports: [
        CommonModule,
        RouterLink,
        RouterLinkActive,
        RouterOutlet,
        MatIconModule,
        MatButtonModule,
        MatDividerModule,
        MatMenuModule
    ],
    templateUrl: './admin-layout.component.html',
    styleUrls: ['./admin-layout.component.css']
})
export class AdminLayoutComponent
{
    username: string;
    email: string;

    navItems = [
        { label: 'Dashboard', icon: 'dashboard', route: '/admin/dashboard' },
        { label: 'Scraping', icon: 'sync', route: '/admin/scraping' },
        { label: 'Logs Scraping', icon: 'list_alt', route: '/admin/logs' },
        { label: 'Usuarios', icon: 'people', route: '/admin/users' },
        { label: 'Tiendas', icon: 'store', route: '/admin/stores' }
    ];

    constructor(
        private authService: AuthService,
        private router: Router
    ) {
        const user = this.authService.getCurrentUser();
        if (!user || user.role !== 'ADMIN') {
            this.router.navigate(['/home']);
            this.username = '';
            this.email = '';
            return;
        }
        this.username = user.username ?? 'Admin';
        this.email = user.email ?? '';
    }

    logout(): void
    {
        this.authService.logout();
    }
}
