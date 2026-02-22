import { Component, Input, OnChanges, Output, EventEmitter } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';
import { MonthlyExpenseSummary } from '../../core/models/expense.model';

interface StoreExpenseInput {
  storeName: string;
  totalAmount: number;
}

@Component({
  selector: 'app-expense-charts',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule, BaseChartDirective],
  template: `
    <!-- Toggle periodo -->
    <div class="period-toggle">
      <button *ngFor="let p of periods"
              class="toggle-btn"
              [class.active]="p.value === selectedPeriod"
              (click)="onPeriodChange(p.value)">
        {{ p.label }}
      </button>
    </div>

    <!-- Navegación temporal -->
    <div class="nav-row">
      <button mat-icon-button (click)="onPrev()" [disabled]="!hasPrevData">
        <mat-icon>chevron_left</mat-icon>
      </button>
      <span class="nav-label">{{ rangeLabel }}</span>
      <button mat-icon-button (click)="onNext()" [disabled]="offset <= 0">
        <mat-icon>chevron_right</mat-icon>
      </button>
    </div>

    <!-- Resumen rápido -->
    <div class="summary-cards" *ngIf="summary.length > 0">
      <div class="summary-card">
        <span class="summary-label">Gasto Total</span>
        <span class="summary-value">{{ totalSpent | number:'1.2-2' }} &euro;</span>
      </div>
      <div class="summary-card" *ngIf="activeLimit">
        <span class="summary-label">Presupuesto</span>
        <span class="summary-value" [class.exceeded]="currentPeriodSpent > activeLimit">
          {{ currentPeriodSpent | number:'1.2-2' }} / {{ activeLimit | number:'1.2-2' }} &euro;
        </span>
      </div>
    </div>

    <div class="charts-grid">
      <!-- Gráfica de líneas - evolución del gasto -->
      <div class="chart-card" *ngIf="lineChartData.labels!.length > 0">
        <h3>Evolución del gasto — {{ rangeLabel }}</h3>
        <canvas baseChart
          [data]="lineChartData"
          [options]="lineChartOptions"
          [type]="'line'">
        </canvas>
      </div>

      <!-- Gráfica doughnut por supermercado -->
      <div class="chart-card" *ngIf="storeDoughnutData.labels!.length > 0">
        <h3>Gasto por Supermercado</h3>
        <canvas baseChart
          [data]="storeDoughnutData"
          [options]="storeDoughnutOptions"
          [type]="'doughnut'">
        </canvas>
      </div>

      <!-- Gráfica doughnut presupuesto -->
      <div class="chart-card" *ngIf="activeLimit && budgetDoughnutData.labels!.length > 0">
        <h3>Presupuesto — {{ rangeLabel }}</h3>
        <canvas baseChart
          [data]="budgetDoughnutData"
          [options]="budgetDoughnutOptions"
          [type]="'doughnut'">
        </canvas>
        <div class="doughnut-center-label">
          {{ currentPeriodSpent | number:'1.2-2' }}&euro;<br>
          <small>de {{ activeLimit | number:'1.2-2' }}&euro;</small>
        </div>
      </div>

      <!-- Sin datos -->
      <div class="chart-card chart-empty" *ngIf="lineChartData.labels!.length === 0">
        <p>No hay datos para este periodo.</p>
      </div>
    </div>
  `,
  styles: [`
    .period-toggle {
      display: flex;
      gap: 0;
      margin-bottom: 16px;
      border-radius: 24px;
      overflow: hidden;
      border: 1.5px solid var(--smartcart-accent);
      width: fit-content;
    }

    .toggle-btn {
      padding: 8px 20px;
      border: none;
      background: transparent;
      color: var(--smartcart-accent);
      font-size: 14px;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s;
    }

    .toggle-btn.active {
      background: var(--smartcart-accent);
      color: #fff;
    }

    .toggle-btn:hover:not(.active) {
      background: rgba(var(--smartcart-accent-rgb, 76, 175, 80), 0.1);
    }

    .nav-row {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 20px;
    }

    .nav-label {
      font-size: 15px;
      font-weight: 500;
      color: var(--smartcart-text);
      min-width: 180px;
      text-align: center;
    }

    .summary-cards {
      display: flex;
      gap: 16px;
      margin-bottom: 20px;
    }

    .summary-card {
      background: var(--smartcart-card);
      border-radius: 12px;
      padding: 16px 24px;
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .summary-label {
      font-size: 13px;
      color: var(--smartcart-text-light);
      font-weight: 500;
    }

    .summary-value {
      font-size: 22px;
      font-weight: 700;
      color: var(--smartcart-text);
    }

    .summary-value.exceeded {
      color: #ef4444;
    }

    .charts-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 24px;
      margin-bottom: 32px;
    }

    .chart-card {
      background: var(--smartcart-card);
      border-radius: 12px;
      padding: 20px;
      position: relative;
    }

    .chart-card h3 {
      margin: 0 0 16px;
      font-size: 16px;
      font-weight: 500;
      color: var(--smartcart-text);
    }

    .chart-empty {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 200px;
      grid-column: 1 / -1;
    }

    .chart-empty p {
      color: var(--smartcart-text-light);
      font-size: 14px;
    }

    .doughnut-center-label {
      position: absolute;
      top: 58%;
      left: 50%;
      transform: translate(-50%, -50%);
      text-align: center;
      font-size: 14px;
      font-weight: 600;
      color: var(--smartcart-text);
    }

    .doughnut-center-label small {
      font-weight: 400;
      font-size: 12px;
      color: var(--smartcart-text-light);
    }

    @media (max-width: 768px) {
      .charts-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class ExpenseChartsComponent implements OnChanges
{
  @Input() summary: MonthlyExpenseSummary[] = [];
  @Input() storeExpenses: StoreExpenseInput[] = [];
  @Input() weeklyLimit: number | null = null;
  @Input() monthlyLimit: number | null = null;
  @Input() yearlyLimit: number | null = null;
  @Input() selectedPeriod: string = 'MONTHLY';
  @Input() offset: number = 0;
  @Output() periodChange = new EventEmitter<string>();
  @Output() offsetChange = new EventEmitter<number>();

  currentPeriodSpent = 0;
  totalSpent = 0;
  activeLimit: number | null = null;
  rangeLabel = '';
  hasPrevData = true;

  periods = [
    { value: 'WEEKLY', label: 'Semanal' },
    { value: 'MONTHLY', label: 'Mensual' },
    { value: 'YEARLY', label: 'Anual' }
  ];

  periodLabels: Record<string, string> = {
    'WEEKLY': 'semanal',
    'MONTHLY': 'mensual',
    'YEARLY': 'anual'
  };

  lineChartData: ChartData<'line'> = { labels: [], datasets: [] };
  lineChartOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true,
    plugins: {
      legend: { display: false }
    },
    scales: {
      y: {
        beginAtZero: true,
        ticks: {
          callback: (value) => value + '€'
        }
      }
    },
    elements: {
      line: { tension: 0.3 }
    }
  };

  storeDoughnutData: ChartData<'doughnut'> = { labels: [], datasets: [] };
  storeDoughnutOptions: ChartConfiguration<'doughnut'>['options'] = {
    responsive: true,
    cutout: '50%',
    plugins: {
      legend: { display: true, position: 'bottom' }
    }
  };

  budgetDoughnutData: ChartData<'doughnut'> = { labels: [], datasets: [] };
  budgetDoughnutOptions: ChartConfiguration<'doughnut'>['options'] = {
    responsive: true,
    cutout: '65%',
    plugins: {
      legend: { display: true, position: 'bottom' }
    }
  };

  private storeColors = ['#4ade80', '#60a5fa', '#f97316', '#a78bfa', '#f472b6', '#facc15', '#34d399', '#fb923c'];

  ngOnChanges(): void
  {
    this.activeLimit = this.getLimitForPeriod();
    this.hasPrevData = this.summary.length > 0;
    this.totalSpent = this.summary.reduce((sum, s) => sum + s.totalAmount, 0);
    this.currentPeriodSpent = this.totalSpent;
    this.buildRangeLabel();
    this.buildLineChart();
    this.buildStoreDoughnut();
    this.buildBudgetDoughnut();
  }

  onPeriodChange(period: string): void
  {
    this.periodChange.emit(period);
  }

  onPrev(): void
  {
    this.offsetChange.emit(this.offset + 1);
  }

  onNext(): void
  {
    if (this.offset > 0)
    {
      this.offsetChange.emit(this.offset - 1);
    }
  }

  private getLimitForPeriod(): number | null
  {
    switch (this.selectedPeriod)
    {
      case 'WEEKLY': return this.weeklyLimit;
      case 'YEARLY': return this.yearlyLimit;
      default: return this.monthlyLimit;
    }
  }

  private static readonly MONTH_NAMES = [
    'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
    'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
  ];

  private buildRangeLabel(): void
  {
    if (this.summary.length === 0)
    {
      this.rangeLabel = 'Sin datos';
      return;
    }

    const now = new Date();

    switch (this.selectedPeriod)
    {
      case 'WEEKLY':
      {
        // Calculate the Monday of the target week
        const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
        const dow = today.getDay() === 0 ? 7 : today.getDay(); // 1=Mon..7=Sun
        const monday = new Date(today);
        monday.setDate(today.getDate() - (dow - 1) - (this.offset * 7));
        const sunday = new Date(monday);
        sunday.setDate(monday.getDate() + 6);

        const monMonth = ExpenseChartsComponent.MONTH_NAMES[monday.getMonth()];
        const sunMonth = ExpenseChartsComponent.MONTH_NAMES[sunday.getMonth()];
        this.rangeLabel = monMonth === sunMonth
          ? monMonth
          : `${monMonth} — ${sunMonth}`;
        break;
      }
      case 'YEARLY':
      {
        const year = now.getFullYear() - this.offset;
        this.rangeLabel = `${year}`;
        break;
      }
      default: // MONTHLY
      {
        const d = new Date(now.getFullYear(), now.getMonth() - this.offset, 1);
        this.rangeLabel = `${ExpenseChartsComponent.MONTH_NAMES[d.getMonth()]} ${d.getFullYear()}`;
        break;
      }
    }
  }

  private buildLineChart(): void
  {
    const labels = this.summary.map(s => s.periodLabel);
    const data = this.summary.map(s => s.totalAmount);

    this.lineChartData = {
      labels,
      datasets: [
        {
          data,
          borderColor: '#4ade80',
          backgroundColor: 'rgba(74, 222, 128, 0.15)',
          fill: true,
          pointBackgroundColor: '#4ade80',
          pointBorderColor: '#fff',
          pointRadius: 5,
          label: 'Gasto'
        }
      ]
    };

    if (this.activeLimit) {
      this.lineChartData.datasets.push({
        data: Array(labels.length).fill(this.activeLimit),
        borderColor: '#ef4444',
        borderDash: [8, 4],
        pointRadius: 0,
        fill: false,
        label: 'Límite'
      } as any);

      this.lineChartOptions = {
        ...this.lineChartOptions,
        plugins: { legend: { display: true, position: 'bottom' } }
      };
    }
  }

  private buildStoreDoughnut(): void
  {
    if (!this.storeExpenses || this.storeExpenses.length === 0) {
      this.storeDoughnutData = { labels: [], datasets: [] };
      return;
    }

    const labels = this.storeExpenses.map(se => se.storeName);
    const data = this.storeExpenses.map(se => se.totalAmount);
    const colors = labels.map((_, i) => this.storeColors[i % this.storeColors.length]);

    this.storeDoughnutData = {
      labels,
      datasets: [{
        data,
        backgroundColor: colors
      }]
    };
  }

  private buildBudgetDoughnut(): void
  {
    if (!this.activeLimit || this.summary.length === 0) return;

    const remaining = Math.max(0, this.activeLimit - this.currentPeriodSpent);
    const exceeded = Math.max(0, this.currentPeriodSpent - this.activeLimit);

    if (exceeded > 0)
    {
      this.budgetDoughnutData = {
        labels: ['Gastado', 'Excedido'],
        datasets: [{
          data: [this.activeLimit, exceeded],
          backgroundColor: ['#4ade80', '#ef4444']
        }]
      };
    }
    else
    {
      this.budgetDoughnutData = {
        labels: ['Gastado', 'Restante'],
        datasets: [{
          data: [this.currentPeriodSpent, remaining],
          backgroundColor: ['#4ade80', '#e5e7eb']
        }]
      };
    }
  }
}
