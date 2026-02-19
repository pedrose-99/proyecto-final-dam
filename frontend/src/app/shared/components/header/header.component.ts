import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatBadgeModule } from '@angular/material/badge';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { Observable, Subject, debounceTime, distinctUntilChanged, switchMap, takeUntil } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { AuthResponse } from '../../../core/models/user.model';
import { ProductService } from '../../services/product.service';
import { NotificationService } from '../../services/notification.service';
import { ProductSearchResult } from '../../../core/models/product.model';
import { AppNotification } from '../../../core/models/group.model';
import { ThemeService } from '../../../core/services/theme.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    MatToolbarModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatBadgeModule,
    MatAutocompleteModule,
    MatInputModule,
    MatFormFieldModule,
    MatDividerModule,
    MatTooltipModule,
    MatChipsModule
  ],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent implements OnInit, OnDestroy {

  searchControl = new FormControl('');
  searchResults: ProductSearchResult[] = [];
  isSearching = false;
  
  // Multi-search terms
  searchTerms: string[] = [];

  currentUser$: Observable<AuthResponse | null>;
  favoritesCount = 0;
  cartCount = 0;

  notifications: AppNotification[] = [];
  unreadCount = 0;

  private destroy$ = new Subject<void>();

  themeService: ThemeService;

  constructor(
    private authService: AuthService,
    private productService: ProductService,
    private notificationService: NotificationService,
    private router: Router,
    private route: ActivatedRoute,
    themeService: ThemeService
  ) {
    this.themeService = themeService;
    this.currentUser$ = this.authService.currentUser$;
  }

  ngOnInit(): void {
    // Cargar los términos de búsqueda de los query params si existen
    this.route.queryParams.pipe(
      takeUntil(this.destroy$)
    ).subscribe(params => {
      if (params['search']) {
        this.searchTerms = params['search'].split(',').map((t: string) => t.trim()).filter((t: string) => t);
      }
    });

    this.setupSearch();
    this.loadNotifications();

    this.notificationService.notifications$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(notifications => {
      this.notifications = notifications;
    });

    this.notificationService.unreadCount$.pipe(
      takeUntil(this.destroy$)
    ).subscribe(count => {
      this.unreadCount = count;
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private setupSearch(): void {
    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(query => {
        if (!query || query.length < 2) {
          this.searchResults = [];
          return [];
        }
        this.isSearching = true;
        return this.productService.searchProducts(query);
      }),
      takeUntil(this.destroy$)
    ).subscribe(results => {
      this.searchResults = results;
      this.isSearching = false;
    });
  }

  onProductSelected(product: ProductSearchResult): void {
    // Agregar término de búsqueda en lugar de navegar al producto
    this.addSearchTerm(product.name);
  }

  addSearchTerm(term: string): void {
    const trimmedTerm = term.trim().toLowerCase();
    console.log('[DEBUG-HEADER] addSearchTerm llamado con:', term, 'trimmed:', trimmedTerm);
    if (trimmedTerm && !this.searchTerms.includes(trimmedTerm)) {
      this.searchTerms.push(trimmedTerm);
      console.log('[DEBUG-HEADER] searchTerms actualizado a:', this.searchTerms);
    }
    this.searchControl.setValue('');
    this.searchResults = [];
    this.navigateWithSearchTerms();
    console.log('[DEBUG-HEADER] navigateWithSearchTerms ejecutado');
  }

  removeSearchTerm(term: string): void {
    this.searchTerms = this.searchTerms.filter(t => t !== term);
    this.navigateWithSearchTerms();
  }

  onSearchSubmit(): void {
    const query = this.searchControl.value;
    if (query && query.length > 0) {
      this.addSearchTerm(query);
    }
  }

  viewAllResults(): void {
    this.onSearchSubmit();
  }

  navigateWithSearchTerms(): void {
    if (this.searchTerms.length > 0) {
      this.router.navigate(['/home'], { queryParams: { search: this.searchTerms.join(',') } });
    } else {
      this.router.navigate(['/home']);
    }
  }

  goToFavorites(): void {
    this.router.navigate(['/favorites']);
  }

  goToCart(): void {
    this.router.navigate(['/lists']);
  }

  goToProfile(): void {
    this.router.navigate(['/profile']);
  }

  goToLists(): void {
    this.router.navigate(['/lists']);
  }

  goToHistory(): void {
    this.router.navigate(['/mis-gastos']);
  }

  goToSettings(): void {
    this.router.navigate(['/settings']);
  }

  goToAdmin(): void {
    this.router.navigate(['/admin/dashboard']);
  }

  goToGroups(): void {
    this.router.navigate(['/grupos']);
  }

  loadNotifications(): void {
    this.notificationService.loadNotifications().pipe(
      takeUntil(this.destroy$)
    ).subscribe();
  }

  respondToInvite(notification: AppNotification, accept: boolean): void {
    this.notificationService.respondToInvite(notification.notificationId, accept).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        this.loadNotifications();
        if (accept) {
          this.router.navigate(['/grupos']);
        }
      }
    });
  }

  deleteNotification(notification: AppNotification): void {
    this.notificationService.deleteNotification(notification.notificationId).pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: () => {
        this.loadNotifications();
      }
    });
  }

  get isAdmin(): boolean {
    return this.authService.getCurrentUser()?.role === 'ADMIN';
  }

  logout(): void {
    this.authService.logout();
  }

  goHome(): void {
    this.router.navigate(['/home']);
  }
}