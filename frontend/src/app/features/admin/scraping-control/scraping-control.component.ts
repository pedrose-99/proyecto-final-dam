import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AdminService } from '../../../core/services/admin.service';
import { StoreAdmin, ScrapingResponse } from '../../../core/models/admin.model';

interface StoreCard extends StoreAdmin {
    scraping: boolean;
    result: ScrapingResponse | null;
}

@Component({
    selector: 'app-scraping-control',
    standalone: true,
    imports: [
        CommonModule,
        MatCardModule,
        MatButtonModule,
        MatIconModule,
        MatProgressSpinnerModule,
        MatSnackBarModule
    ],
    templateUrl: './scraping-control.component.html',
    styleUrls: ['./scraping-control.component.css']
})
export class ScrapingControlComponent implements OnInit
{
    stores: StoreCard[] = [];
    loading = true;
    scrapingAll = false;

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
                this.stores = stores.map(s => ({ ...s, scraping: false, result: null }));
                this.loading = false;
                this.cdr.detectChanges();
            },
            error: () => {
                this.loading = false;
                this.cdr.detectChanges();
                this.snackBar.open('Error al cargar tiendas', 'Cerrar', { duration: 3000 });
            }
        });
    }

    triggerScraping(store: StoreCard): void
    {
        if (store.scraping) return;
        store.scraping = true;
        store.result = null;

        this.adminService.triggerScraping(store.slug).subscribe({
            next: (result) => {
                store.scraping = false;
                store.result = result;
                this.snackBar.open(
                    `${store.name}: ${result.created} creados, ${result.updated} actualizados, ${result.scrapingErrors + result.syncErrors} errores`,
                    'OK',
                    { duration: 5000 }
                );
                this.loadStores();
            },
            error: (err) => {
                store.scraping = false;
                this.snackBar.open(
                    `Error al scrapear ${store.name}: ${err.error?.message || 'Error desconocido'}`,
                    'Cerrar',
                    { duration: 5000 }
                );
            }
        });
    }

    async triggerAll(): Promise<void>
    {
        this.scrapingAll = true;
        const activeStores = this.stores.filter(s => s.active);

        for (const store of activeStores) {
            try {
                store.scraping = true;
                const result = await this.adminService.triggerScraping(store.slug).toPromise();
                store.result = result ?? null;
                store.scraping = false;
            } catch {
                store.scraping = false;
            }
        }

        this.scrapingAll = false;
        this.snackBar.open('Scraping completo para todas las tiendas', 'OK', { duration: 4000 });
        this.loadStores();
    }

    getStatusLabel(status: string | null): string
    {
        switch (status) {
            case 'COMPLETED': return 'Completado';
            case 'FAILED': return 'Error';
            case 'RUNNING': return 'En curso';
            default: return 'Sin datos';
        }
    }

    getStatusClass(status: string | null): string
    {
        switch (status) {
            case 'COMPLETED': return 'status-completed';
            case 'FAILED': return 'status-failed';
            case 'RUNNING': return 'status-running';
            default: return 'status-none';
        }
    }

    formatDate(dateStr: string | null): string
    {
        if (!dateStr) return 'Nunca';
        const d = new Date(dateStr);
        return d.toLocaleDateString('es-ES', { day: '2-digit', month: '2-digit', year: '2-digit', hour: '2-digit', minute: '2-digit' });
    }

    isAnyScraping(): boolean
    {
        return this.stores.some(s => s.scraping);
    }
}
