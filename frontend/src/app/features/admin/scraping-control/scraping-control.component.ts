import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Subscription } from 'rxjs';
import { AdminService } from '../../../core/services/admin.service';
import { StoreAdmin, ScrapingResponse } from '../../../core/models/admin.model';

interface StoreCard extends StoreAdmin {
    scraping: boolean;
    result: ScrapingResponse | null;
    subscription: Subscription | null;
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
export class ScrapingControlComponent implements OnInit, OnDestroy
{
    stores: StoreCard[] = [];
    loading = true;
    scrapingAll = false;
    private cancelledAll = false;

    constructor(
        private adminService: AdminService,
        private snackBar: MatSnackBar,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void
    {
        this.loadStores();
    }

    ngOnDestroy(): void
    {
        this.stores.forEach(s => s.subscription?.unsubscribe());
    }

    loadStores(): void
    {
        this.adminService.getStores().subscribe({
            next: (stores) => {
                this.stores = stores.map(s => ({ ...s, scraping: false, result: null, subscription: null }));
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

        store.subscription = this.adminService.triggerScraping(store.slug).subscribe({
            next: (result) => {
                store.scraping = false;
                store.subscription = null;
                store.result = result;
                this.cdr.detectChanges();
                this.snackBar.open(
                    `${store.name}: ${result.created} creados, ${result.updated} actualizados, ${result.scrapingErrors + result.syncErrors} errores`,
                    'OK',
                    { duration: 5000 }
                );
                this.loadStores();
            },
            error: (err) => {
                store.scraping = false;
                store.subscription = null;
                this.cdr.detectChanges();
                this.snackBar.open(
                    `Error al scrapear ${store.name}: ${err.error?.message || 'Error desconocido'}`,
                    'Cerrar',
                    { duration: 5000 }
                );
            }
        });
    }

    cancelScraping(store: StoreCard): void
    {
        // Cancelar la petición HTTP del frontend
        store.subscription?.unsubscribe();
        store.subscription = null;
        store.scraping = false;
        this.cdr.detectChanges();

        // Pedir al backend que cancele el hilo
        this.adminService.cancelScraping(store.slug).subscribe({
            next: () => {
                this.snackBar.open(`Scraping de ${store.name} cancelado`, 'OK', { duration: 3000 });
                this.loadStores();
            },
            error: () => {
                this.snackBar.open(`Error al cancelar ${store.name}`, 'Cerrar', { duration: 3000 });
            }
        });
    }

    async triggerAll(): Promise<void>
    {
        this.scrapingAll = true;
        this.cancelledAll = false;
        const activeStores = this.stores.filter(s => s.active);

        for (const store of activeStores) {
            if (this.cancelledAll) break;
            try {
                store.scraping = true;
                this.cdr.detectChanges();
                const result = await this.adminService.triggerScraping(store.slug).toPromise();
                store.result = result ?? null;
                store.scraping = false;
                this.cdr.detectChanges();
            } catch {
                store.scraping = false;
                this.cdr.detectChanges();
            }
        }

        this.scrapingAll = false;
        if (!this.cancelledAll) {
            this.snackBar.open('Scraping completo para todas las tiendas', 'OK', { duration: 4000 });
        }
        this.cdr.detectChanges();
        this.loadStores();
    }

    cancelAll(): void
    {
        this.cancelledAll = true;
        const scrapingStores = this.stores.filter(s => s.scraping);
        scrapingStores.forEach(store => this.cancelScraping(store));
        this.scrapingAll = false;
        this.cdr.detectChanges();
        this.snackBar.open('Todos los scrapings cancelados', 'OK', { duration: 3000 });
    }

    getStatusLabel(status: string | null): string
    {
        switch (status) {
            case 'COMPLETED': return 'Completado';
            case 'FAILED': return 'Error';
            case 'RUNNING': return 'En curso';
            case 'CANCELLED': return 'Cancelado';
            default: return 'Sin datos';
        }
    }

    getStatusClass(status: string | null): string
    {
        switch (status) {
            case 'COMPLETED': return 'status-completed';
            case 'FAILED': return 'status-failed';
            case 'RUNNING': return 'status-running';
            case 'CANCELLED': return 'status-cancelled';
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
