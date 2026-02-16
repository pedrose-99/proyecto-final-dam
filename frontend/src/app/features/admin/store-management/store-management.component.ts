import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AdminService } from '../../../core/services/admin.service';
import { StoreAdmin } from '../../../core/models/admin.model';

@Component({
    selector: 'app-store-management',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        MatCardModule,
        MatTableModule,
        MatSlideToggleModule,
        MatIconModule,
        MatButtonModule,
        MatFormFieldModule,
        MatInputModule,
        MatSnackBarModule,
        MatProgressSpinnerModule
    ],
    templateUrl: './store-management.component.html',
    styleUrls: ['./store-management.component.css']
})
export class StoreManagementComponent implements OnInit
{
    stores: StoreAdmin[] = [];
    loading = true;
    editingStoreId: number | null = null;
    editUrl = '';

    displayedColumns = ['name', 'slug', 'scrapingUrl', 'productCount', 'lastScrapeDate', 'lastScrapeStatus', 'active', 'actions'];

    constructor(
        private adminService: AdminService,
        private snackBar: MatSnackBar,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void
    {
        this.loadStores();
    }

    loadStores(): void
    {
        this.adminService.getStores().subscribe({
            next: (stores) => {
                this.stores = stores;
                this.loading = false;
                this.cdr.detectChanges();
            },
            error: () => { this.loading = false; this.cdr.detectChanges(); }
        });
    }

    toggleActive(store: StoreAdmin): void
    {
        const newActive = !store.active;
        this.adminService.updateStore(store.id, { active: newActive }).subscribe({
            next: (updated) => {
                store.active = updated.active;
                this.snackBar.open(
                    `${store.name} ${newActive ? 'activada' : 'desactivada'}`,
                    'OK',
                    { duration: 3000 }
                );
            },
            error: () => {
                this.snackBar.open('Error al actualizar tienda', 'Cerrar', { duration: 3000 });
            }
        });
    }

    startEditUrl(store: StoreAdmin): void
    {
        this.editingStoreId = store.id;
        this.editUrl = store.scrapingUrl || '';
    }

    cancelEdit(): void
    {
        this.editingStoreId = null;
        this.editUrl = '';
    }

    saveUrl(store: StoreAdmin): void
    {
        this.adminService.updateStore(store.id, { scrapingUrl: this.editUrl }).subscribe({
            next: (updated) => {
                store.scrapingUrl = updated.scrapingUrl;
                this.editingStoreId = null;
                this.editUrl = '';
                this.snackBar.open('URL actualizada', 'OK', { duration: 3000 });
            },
            error: () => {
                this.snackBar.open('Error al actualizar URL', 'Cerrar', { duration: 3000 });
            }
        });
    }

    getStatusLabel(status: string | null): string
    {
        switch (status) {
            case 'COMPLETED': return 'Completado';
            case 'FAILED': return 'Error';
            case 'RUNNING': return 'En curso';
            default: return '-';
        }
    }

    getStatusClass(status: string | null): string
    {
        switch (status) {
            case 'COMPLETED': return 'completed';
            case 'FAILED': return 'failed';
            case 'RUNNING': return 'running';
            default: return '';
        }
    }

    formatDate(dateStr: string | null): string
    {
        if (!dateStr) return '-';
        const d = new Date(dateStr);
        return d.toLocaleDateString('es-ES', { day: '2-digit', month: '2-digit', year: '2-digit', hour: '2-digit', minute: '2-digit' });
    }

    truncateUrl(url: string | null): string
    {
        if (!url) return '-';
        return url.length > 40 ? url.substring(0, 40) + '...' : url;
    }
}
