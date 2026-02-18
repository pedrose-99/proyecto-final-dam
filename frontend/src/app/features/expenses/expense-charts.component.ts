import { Component, Input, OnChanges, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';
import { MonthlyExpenseSummary } from '../../core/models/expense.model';

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
      <button mat-icon-button (click)="onPrev()">
        <mat-icon>chevron_left</mat-icon>
      </button>
      <span class="nav-label">{{ rangeLabel }}</span>
      <button mat-icon-button (click)="onNext()" [disabled]="offset <= 0">
        <mat-icon>chevron_right</mat-icon>
      </button>
    </div>

    <div class="charts-grid">
      <!-- Gráfica de barras -->
      <div class="chart-card" *ngIf="barChartData.labels!.length > 0">
        <h3>Gasto {{ periodLabels[selectedPeriod] }}</h3>
        <canvas baseChart
          [data]="barChartData"
          [options]="barChartOptions"
          [type]="'bar'">
        </canvas>
      </div>

      <!-- Gráfica doughnut -->
      <div class="chart-card" *ngIf="activeLimit && doughnutChartData.labels!.length > 0">
        <h3>Presupuesto {{ periodLabels[selectedPeriod] }}</h3>
        <canvas baseChart
          [data]="doughnutChartData"
          [options]="doughnutChartOptions"
          [type]="'doughnut'">
        </canvas>
        <div class="doughnut-center-label">
          {{ currentPeriodSpent | number:'1.2-2' }}€<br>
          <small>de {{ activeLimit | number:'1.2-2' }}€</small>
        </div>
      </div>

      <!-- Sin datos -->
      <div class="chart-card chart-empty" *ngIf="barChartData.labels!.length === 0">
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
  @Input() weeklyLimit: number | null = null;
  @Input() monthlyLimit: number | null = null;
  @Input() yearlyLimit: number | null = null;
  @Input() selectedPeriod: string = 'MONTHLY';
  @Input() offset: number = 0;
  @Output() periodChange = new EventEmitter<string>();
  @Output() offsetChange = new EventEmitter<number>();

  currentPeriodSpent = 0;
  activeLimit: number | null = null;
  rangeLabel = '';

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

  barChartData: ChartData<'bar'> = { labels: [], datasets: [] };
  barChartOptions: ChartConfiguration<'bar'>['options'] = {
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
    }
  };

  doughnutChartData: ChartData<'doughnut'> = { labels: [], datasets: [] };
  doughnutChartOptions: ChartConfiguration<'doughnut'>['options'] = {
    responsive: true,
    cutout: '65%',
    plugins: {
      legend: { display: true, position: 'bottom' }
    }
  };

  ngOnChanges(): void
  {
    this.activeLimit = this.getLimitForPeriod();
    this.buildRangeLabel();
    this.buildBarChart();
    this.buildDoughnutChart();
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

  private buildRangeLabel(): void
  {
    if (this.summary.length === 0)
    {
      this.rangeLabel = 'Sin datos';
      return;
    }
    const first = this.summary[0].periodLabel;
    const last = this.summary[this.summary.length - 1].periodLabel;
    this.rangeLabel = first === last ? first : `${first} — ${last}`;
  }

  private buildBarChart(): void
  {
    const labels = this.summary.map(s => s.periodLabel);
    const data = this.summary.map(s => s.totalAmount);
    const limit = this.activeLimit;

    const backgroundColors = data.map(amount =>
    {
      if (limit && amount > limit)
      {
        return '#ef4444';
      }
      return '#4ade80';
    });

    this.barChartData = {
      labels,
      datasets: [
        {
          data,
          backgroundColor: backgroundColors,
          borderRadius: 6,
          label: 'Gasto'
        }
      ]
    };
  }

  private buildDoughnutChart(): void
  {
    if (!this.activeLimit || this.summary.length === 0) return;

    const lastEntry = this.summary[this.summary.length - 1];
    this.currentPeriodSpent = lastEntry?.totalAmount || 0;

    const remaining = Math.max(0, this.activeLimit - this.currentPeriodSpent);
    const exceeded = Math.max(0, this.currentPeriodSpent - this.activeLimit);

    if (exceeded > 0)
    {
      this.doughnutChartData = {
        labels: ['Gastado', 'Excedido'],
        datasets: [{
          data: [this.activeLimit, exceeded],
          backgroundColor: ['#4ade80', '#ef4444']
        }]
      };
    }
    else
    {
      this.doughnutChartData = {
        labels: ['Gastado', 'Restante'],
        datasets: [{
          data: [this.currentPeriodSpent, remaining],
          backgroundColor: ['#4ade80', '#e5e7eb']
        }]
      };
    }
  }
}
