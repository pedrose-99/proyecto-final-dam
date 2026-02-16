import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
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
import { Subject, debounceTime, distinctUntilChanged, switchMap, takeUntil } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { ProductService } from '../../services/product.service';
import { ProductSearchResult } from '../../../core/models/product.model';

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
    MatDividerModule
  ],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent implements OnInit, OnDestroy {

  searchControl = new FormControl('');
  searchResults: ProductSearchResult[] = [];
  isSearching = false;

  currentUser: any;
  favoritesCount = 0;
  cartCount = 0;

  private destroy$ = new Subject<void>();

  constructor(
    private authService: AuthService,
    private productService: ProductService,
    private router: Router
  ) {
    this.currentUser = this.authService.getCurrentUser();
  }

  ngOnInit(): void {
    this.setupSearch();
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
    this.router.navigate(['/producto', product.id]);
    this.searchControl.setValue('');
    this.searchResults = [];
  }

  onSearchSubmit(): void {
    const query = this.searchControl.value;
    if (query && query.length > 0) {
      this.router.navigate(['/home'], { queryParams: { search: query } });
      this.searchResults = [];
    }
  }

  viewAllResults(): void {
    this.onSearchSubmit();
  }

  goToFavorites(): void {
    this.router.navigate(['/favorites']);
  }

  goToCart(): void {
    this.router.navigate(['/cart']);
  }

  goToProfile(): void {
    this.router.navigate(['/profile']);
  }

  goToLists(): void {
    this.router.navigate(['/lists']);
  }

  goToHistory(): void {
    this.router.navigate(['/history']);
  }

  goToSettings(): void {
    this.router.navigate(['/settings']);
  }

  goToAdmin(): void {
    this.router.navigate(['/admin/dashboard']);
  }

  get isAdmin(): boolean {
    return this.currentUser?.role === 'ADMIN';
  }

  logout(): void {
    this.authService.logout();
  }

  goHome(): void {
    this.router.navigate(['/home']);
  }
}