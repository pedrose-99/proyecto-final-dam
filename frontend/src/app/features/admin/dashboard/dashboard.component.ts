import { Component, OnInit, ElementRef, ViewChild, AfterViewInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AdminService } from '../../../core/services/admin.service';
import { AdminStats, ServiceHealth, ScrapeLogEntry } from '../../../core/models/admin.model';

@Component({
    selector: 'app-admin-dashboard',
    standalone: true,
    imports: [
        CommonModule,
        RouterModule,
        MatCardModule,
        MatIconModule,
        MatChipsModule,
        MatTableModule,
        MatProgressSpinnerModule
    ],
    templateUrl: './dashboard.component.html',
    styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit, AfterViewInit
{
    @ViewChild('chartCanvas') chartCanvas: ElementRef<HTMLCanvasElement> | undefined;

    stats: AdminStats | null = null;
    health: ServiceHealth | null = null;
    loading = true;
    healthLoading = true;
    error = '';

    displayedColumns = ['storeName', 'startTime', 'productsFound', 'status', 'durationSeconds'];

    constructor(private adminService: AdminService, private cdr: ChangeDetectorRef) {}

    ngOnInit(): void
    {
        this.adminService.getStats().subscribe({
            next: (stats) => {
                this.stats = stats;
                this.loading = false;
                this.cdr.detectChanges();
                // Esperar al siguiente ciclo de render para que el canvas exista en el DOM
                setTimeout(() => this.renderChart(), 50);
            },
            error: (err) => {
                this.loading = false;
                this.error = 'Error al cargar estadísticas: ' + (err.error?.message || err.statusText || 'Error de conexión');
                this.cdr.detectChanges();
                console.error('Dashboard stats error:', err);
            }
        });

        this.adminService.getServiceHealth().subscribe({
            next: (health) => {
                this.health = health;
                this.healthLoading = false;
                this.cdr.detectChanges();
            },
            error: () => { this.healthLoading = false; this.cdr.detectChanges(); }
        });
    }

    ngAfterViewInit(): void
    {
        if (this.stats) {
            this.renderChart();
        }
    }

    private renderChart(): void
    {
        if (!this.chartCanvas || !this.stats?.productsByStore?.length) return;

        const canvas = this.chartCanvas.nativeElement;
        const ctx = canvas.getContext('2d');
        if (!ctx) return;

        const data = this.stats.productsByStore;
        const maxVal = Math.max(...data.map(d => d.count), 1);
        const barWidth = Math.min(60, (canvas.width - 80) / data.length - 10);
        const chartHeight = canvas.height - 60;

        ctx.clearRect(0, 0, canvas.width, canvas.height);

        // Draw bars
        const storeColors: Record<string, string> = {
            'mercadona': '#22c55e',
            'alcampo': '#f59e0b',
            'dia': '#ef4444',
            'carrefour': '#3b82f6',
        };
        const defaultColor = '#8b5cf6';
        data.forEach((item, i) => {
            const barHeight = (item.count / maxVal) * chartHeight;
            const x = 50 + i * (barWidth + 10);
            const y = chartHeight - barHeight + 20;

            ctx.fillStyle = storeColors[item.storeSlug?.toLowerCase()] || defaultColor;
            ctx.beginPath();
            ctx.roundRect(x, y, barWidth, barHeight, 4);
            ctx.fill();

            // Label
            ctx.fillStyle = '#64748b';
            ctx.font = '11px Roboto';
            ctx.textAlign = 'center';
            ctx.fillText(item.storeName, x + barWidth / 2, canvas.height - 5);

            // Value
            ctx.fillStyle = '#1e293b';
            ctx.font = 'bold 12px Roboto';
            ctx.fillText(item.count.toString(), x + barWidth / 2, y - 6);
        });
    }

    getStatusColor(status: string): string
    {
        switch (status) {
            case 'COMPLETED': return 'completed';
            case 'FAILED': return 'failed';
            case 'RUNNING': return 'running';
            default: return '';
        }
    }

    getStatusLabel(status: string): string
    {
        switch (status) {
            case 'COMPLETED': return 'Completado';
            case 'FAILED': return 'Error';
            case 'RUNNING': return 'En curso';
            default: return status;
        }
    }

    getHealthColor(status: string): string
    {
        return status === 'UP' ? 'healthy' : 'unhealthy';
    }

    getHealthLabel(status: string): string
    {
        return status === 'UP' ? 'Operativo' : 'Caído';
    }

    formatDuration(seconds: number | null): string
    {
        if (seconds == null) return '-';
        if (seconds < 60) return `${seconds}s`;
        const m = Math.floor(seconds / 60);
        const s = seconds % 60;
        return `${m}m ${s}s`;
    }

    formatDate(dateStr: string): string
    {
        if (!dateStr) return '-';
        const d = new Date(dateStr);
        return d.toLocaleDateString('es-ES', { day: '2-digit', month: '2-digit', year: '2-digit', hour: '2-digit', minute: '2-digit' });
    }
}
