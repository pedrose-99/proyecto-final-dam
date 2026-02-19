import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDividerModule } from '@angular/material/divider';
import { MatSelectModule } from '@angular/material/select';

import { Subject, combineLatest } from 'rxjs';
import { takeUntil, debounceTime, distinctUntilChanged, switchMap, startWith } from 'rxjs/operators';
import { ExpenseService } from '../../shared/services/expense.service';
import { Bill, SpendingLimit, MonthlyExpenseSummary } from '../../core/models/expense.model';
import { ExpenseChartsComponent } from './expense-charts.component';

interface MonthGroup
{
  label: string;
  monthKey: string;
  bills: Bill[];
  totalAmount: number;
}

interface StoreExpense
{
  storeName: string;
  totalAmount: number;
}

@Component({
  selector: 'app-expense-history',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatExpansionModule,
    MatProgressSpinnerModule,
    MatBadgeModule,
    MatDividerModule,
    MatSelectModule,

    ExpenseChartsComponent
  ],
  templateUrl: './expense-history.component.html',
  styleUrls: ['./expense-history.component.css']
})
export class ExpenseHistoryComponent implements OnInit, OnDestroy
{
  bills: Bill[] = [];
  monthGroups: MonthGroup[] = [];
  storeExpenses: StoreExpense[] = [];
  isLoading = false;

  // Filters
  filterControl = new FormControl('');
  monthControl = new FormControl<number | null>(null);
  yearControl = new FormControl<number | null>(null);

  // Spending limits
  limits: SpendingLimit[] = [];
  limitTypeControl = new FormControl('MONTHLY');
  limitAmountControl = new FormControl<number | null>(null);
  isSavingLimit = false;

  // Charts
  summary: MonthlyExpenseSummary[] = [];
  selectedPeriod = 'MONTHLY';
  chartOffset = 0;
  weeklyLimit: number | null = null;
  monthlyLimit: number | null = null;
  yearlyLimit: number | null = null;

  // Month/Year options for filters
  monthOptions = [
    { value: 1, label: 'Enero' }, { value: 2, label: 'Febrero' },
    { value: 3, label: 'Marzo' }, { value: 4, label: 'Abril' },
    { value: 5, label: 'Mayo' }, { value: 6, label: 'Junio' },
    { value: 7, label: 'Julio' }, { value: 8, label: 'Agosto' },
    { value: 9, label: 'Septiembre' }, { value: 10, label: 'Octubre' },
    { value: 11, label: 'Noviembre' }, { value: 12, label: 'Diciembre' }
  ];
  yearOptions: number[] = [];

  private destroy$ = new Subject<void>();
  private monthNamesFull = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];

  constructor(
    private expenseService: ExpenseService,
    private router: Router
  ) {}

  ngOnInit(): void
  {
    this.buildYearOptions();
    this.loadLimits();
    this.loadSummary();
    this.setupFilteredBills();
  }

  ngOnDestroy(): void
  {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private buildYearOptions(): void
  {
    const currentYear = new Date().getFullYear();
    for (let y = currentYear; y >= currentYear - 3; y--)
    {
      this.yearOptions.push(y);
    }
  }

  private setupFilteredBills(): void
  {
    combineLatest([
      this.filterControl.valueChanges.pipe(startWith(''), debounceTime(300), distinctUntilChanged()),
      this.monthControl.valueChanges.pipe(startWith(null as number | null)),
      this.yearControl.valueChanges.pipe(startWith(null as number | null))
    ]).pipe(
      switchMap(([filter, month, year]) =>
      {
        this.isLoading = true;
        return this.expenseService.getBillsHistory(
          filter || undefined,
          month || undefined,
          year || undefined
        );
      }),
      takeUntil(this.destroy$)
    ).subscribe({
      next: (bills) =>
      {
        this.bills = bills;
        this.monthGroups = this.groupBillsByMonth(bills);
        this.storeExpenses = this.computeStoreExpenses(bills);
        this.isLoading = false;
      },
      error: () =>
      {
        this.bills = [];
        this.monthGroups = [];
        this.isLoading = false;
      }
    });
  }

  loadLimits(): void
  {
    this.expenseService.getSpendingLimits()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (limits) =>
        {
          this.limits = limits;
          const weekly = limits.find(l => l.type === 'WEEKLY');
          const monthly = limits.find(l => l.type === 'MONTHLY');
          const yearly = limits.find(l => l.type === 'YEARLY');
          this.weeklyLimit = weekly ? weekly.amount : null;
          this.monthlyLimit = monthly ? monthly.amount : null;
          this.yearlyLimit = yearly ? yearly.amount : null;
        },
        error: () => { this.limits = []; }
      });
  }

  loadSummary(): void
  {
    this.expenseService.getExpenseSummary(this.selectedPeriod, this.chartOffset)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (summary) => { this.summary = summary; },
        error: () => { this.summary = []; }
      });
  }

  onPeriodChange(period: string): void
  {
    this.selectedPeriod = period;
    this.chartOffset = 0;
    this.loadSummary();
  }

  onOffsetChange(offset: number): void
  {
    this.chartOffset = offset;
    this.loadSummary();
  }

  saveLimit(): void
  {
    const amount = this.limitAmountControl.value;
    const type = this.limitTypeControl.value || 'MONTHLY';
    if (!amount || amount <= 0)
    {
      return;
    }

    this.isSavingLimit = true;
    this.expenseService.saveSpendingLimit(amount, type)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () =>
        {
          this.isSavingLimit = false;
          this.limitAmountControl.reset();
          this.loadLimits();
        },
        error: () =>
        {
          this.isSavingLimit = false;
        }
      });
  }

  clearFilters(): void
  {
    this.filterControl.setValue('');
    this.monthControl.setValue(null);
    this.yearControl.setValue(null);
  }

  goToLists(): void
  {
    this.router.navigate(['/lists']);
  }

  getLimitTypeLabel(type: string): string
  {
    switch (type)
    {
      case 'WEEKLY': return 'Semanal';
      case 'MONTHLY': return 'Mensual';
      case 'YEARLY': return 'Anual';
      default: return type;
    }
  }

  formatDate(dateStr: string): string
  {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toLocaleDateString('es-ES', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  private computeStoreExpenses(bills: Bill[]): StoreExpense[]
  {
    const map = new Map<string, number>();
    for (const bill of bills)
    {
      for (const item of bill.itemsSummary)
      {
        const name = item.storeName || 'Sin tienda';
        map.set(name, (map.get(name) || 0) + item.price * item.quantity);
      }
    }
    return Array.from(map.entries())
      .map(([storeName, totalAmount]) => ({ storeName, totalAmount }))
      .sort((a, b) => b.totalAmount - a.totalAmount);
  }

  getStoreLogo(storeName: string): string
  {
    const slug = storeName?.toLowerCase().replace(/\s+/g, '');
    const localLogos: Record<string, string> = {
      'mercadona': '/assets/images/stores/mercadona.svg',
      'dia': '/assets/images/stores/dia.svg',
      'carrefour': '/assets/images/stores/carrefour.svg',
      'alcampo': '/assets/images/stores/alcampo.svg',
      'ahorramas': '/assets/images/stores/ahorramas.svg',
    };
    return localLogos[slug] || '/assets/images/stores/placeholder.svg';
  }

  private groupBillsByMonth(bills: Bill[]): MonthGroup[]
  {
    const groups = new Map<string, MonthGroup>();

    for (const bill of bills)
    {
      const d = new Date(bill.recordedAt);
      const month = d.getMonth();
      const year = d.getFullYear();
      const key = `${year}-${String(month + 1).padStart(2, '0')}`;

      if (!groups.has(key))
      {
        groups.set(key, {
          label: `${this.monthNamesFull[month]} ${year}`,
          monthKey: key,
          bills: [],
          totalAmount: 0
        });
      }

      const group = groups.get(key)!;
      group.bills.push(bill);
      group.totalAmount += bill.totalAmount;
    }

    return Array.from(groups.values()).sort((a, b) => b.monthKey.localeCompare(a.monthKey));
  }
}
