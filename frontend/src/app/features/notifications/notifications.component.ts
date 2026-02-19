import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { Subject, takeUntil } from 'rxjs';
import { NotificationService } from '../../shared/services/notification.service';
import { AppNotification } from '../../core/models/group.model';

type NotificationFilter = 'ALL' | 'INVITE' | 'SYSTEM' | 'BUDGET_ALERT' | 'PURCHASE';
type ReadFilter = 'ALL' | 'UNREAD' | 'READ';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatPaginatorModule,
    MatChipsModule,
    MatTooltipModule,
    MatDividerModule
  ],
  templateUrl: './notifications.component.html',
  styleUrls: ['./notifications.component.css']
})
export class NotificationsComponent implements OnInit, OnDestroy
{
  notifications: AppNotification[] = [];
  filteredNotifications: AppNotification[] = [];
  totalElements = 0;
  totalPages = 0;
  pageSize = 10;
  currentPage = 0;

  activeFilter: NotificationFilter = 'ALL';
  activeReadFilter: ReadFilter = 'ALL';

  filters: { label: string; value: NotificationFilter }[] = [
    { label: 'Todas', value: 'ALL' },
    { label: 'Invitaciones', value: 'INVITE' },
    { label: 'Sistema', value: 'SYSTEM' },
    { label: 'Alertas presupuesto', value: 'BUDGET_ALERT' },
    { label: 'Compras', value: 'PURCHASE' }
  ];

  readFilters: { label: string; value: ReadFilter }[] = [
    { label: 'Todas', value: 'ALL' },
    { label: 'No leidas', value: 'UNREAD' },
    { label: 'Leidas', value: 'READ' }
  ];

  private destroy$ = new Subject<void>();

  constructor(
    private notificationService: NotificationService,
    private router: Router
  ) {}

  ngOnInit(): void
  {
    this.loadNotifications();
  }

  ngOnDestroy(): void
  {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadNotifications(): void
  {
    this.notificationService.getNotificationsPaginated(this.currentPage, this.pageSize).pipe(
      takeUntil(this.destroy$)
    ).subscribe(page =>
    {
      this.notifications = page.content;
      this.totalElements = page.totalElements;
      this.totalPages = page.totalPages;
      this.applyFilter();
    });
  }

  onPageChange(event: PageEvent): void
  {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadNotifications();
  }

  setFilter(filter: NotificationFilter): void
  {
    this.activeFilter = filter;
    this.applyFilter();
  }

  setReadFilter(filter: ReadFilter): void
  {
    this.activeReadFilter = filter;
    this.applyFilter();
  }

  private applyFilter(): void
  {
    let result = this.notifications;

    if (this.activeFilter !== 'ALL') {
      result = result.filter(n => n.type === this.activeFilter);
    }

    if (this.activeReadFilter === 'UNREAD') {
      result = result.filter(n => !n.isRead);
    } else if (this.activeReadFilter === 'READ') {
      result = result.filter(n => n.isRead);
    }

    this.filteredNotifications = result;
  }

  markAsRead(notification: AppNotification): void
  {
    if (notification.isRead) return;
    this.notificationService.markAsRead(notification.notificationId).pipe(
      takeUntil(this.destroy$)
    ).subscribe(() =>
    {
      this.loadNotifications();
      this.notificationService.loadNotifications().pipe(takeUntil(this.destroy$)).subscribe();
    });
  }

  markAllAsRead(): void
  {
    this.notificationService.markAllAsRead().pipe(
      takeUntil(this.destroy$)
    ).subscribe(() =>
    {
      this.loadNotifications();
      this.notificationService.loadNotifications().pipe(takeUntil(this.destroy$)).subscribe();
    });
  }

  deleteNotification(notification: AppNotification): void
  {
    this.notificationService.deleteNotification(notification.notificationId).pipe(
      takeUntil(this.destroy$)
    ).subscribe(() =>
    {
      this.loadNotifications();
      this.notificationService.loadNotifications().pipe(takeUntil(this.destroy$)).subscribe();
    });
  }

  respondToInvite(notification: AppNotification, accept: boolean): void
  {
    this.notificationService.respondToInvite(notification.notificationId, accept).pipe(
      takeUntil(this.destroy$)
    ).subscribe(() =>
    {
      this.loadNotifications();
      this.notificationService.loadNotifications().pipe(takeUntil(this.destroy$)).subscribe();
      if (accept) {
        this.router.navigate(['/grupos']);
      }
    });
  }

  getNotificationIcon(type: string): string
  {
    switch (type) {
      case 'INVITE': return 'group_add';
      case 'UPDATE': return 'update';
      case 'BUDGET_ALERT': return 'account_balance_wallet';
      case 'PURCHASE': return 'shopping_cart';
      case 'SYSTEM': return 'info';
      default: return 'notifications';
    }
  }

  getTypeLabel(type: string): string
  {
    switch (type) {
      case 'INVITE': return 'Invitacion';
      case 'UPDATE': return 'Actualizacion';
      case 'BUDGET_ALERT': return 'Alerta';
      case 'PURCHASE': return 'Compra';
      case 'SYSTEM': return 'Sistema';
      default: return type;
    }
  }
}
