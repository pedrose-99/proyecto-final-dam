import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Subject, takeUntil, debounceTime } from 'rxjs';
import { ProductService } from '../../shared/services/product.service';
import { ShoppingListService } from '../../shared/services/shopping-list.service';
import { ProductCardComponent } from '../../shared/components/product-card/product-card.component';
import { AddToListDialogComponent, AddToListDialogResult } from '../../shared/components/add-to-list-dialog/add-to-list-dialog.component';
import { Product, ProductFilters, ProductPage } from '../../core/models/product.model';
import { Category } from '../../core/models/category.model';
import { Store } from '../../core/models/store.model';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatSelectModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatAutocompleteModule,
    MatChipsModule,
    MatDialogModule,
    ProductCardComponent
  ],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent implements OnInit, OnDestroy {

  // Data
  products: Product[] = [];
  categories: Category[] = [];
  stores: Store[] = [];
  productsInList: Set<number> = new Set();

  // Pagination
  totalProducts = 0;
  pageSize = 24;
  pageIndex = 0;
  pageSizeOptions = [12, 24, 48];

  // Loading
  isLoading = false;

  // Filters
  filters: ProductFilters = {};
  activeFilters: { key: string; label: string }[] = [];

  // Form
  filterForm = new FormGroup({
    sortBy: new FormControl<string>('relevance')
  });

  // Categorías
  categorySearchControl = new FormControl<string>('');
  filteredCategories: Category[] = [];
  selectedCategories: Category[] = [];

  // Tiendas
  storeSearchControl = new FormControl<string>('');
  filteredStores: Store[] = [];
  selectedStores: Store[] = [];

  private destroy$ = new Subject<void>();

  constructor(
    private productService: ProductService,
    private shoppingListService: ShoppingListService,
    private dialog: MatDialog,
    private route: ActivatedRoute,
    private router: Router,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadInitialData();
    this.setupFilterSubscription();
    this.handleRouteParams();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadInitialData(): void {
    this.productService.getCategories().subscribe({
      next: (categories) => {
        this.categories = categories;
        this.filteredCategories = categories;
        this.setupCategorySearch();
      },
      error: (err) => {
        console.error('Error al cargar categorías:', err);
      }
    });

    this.productService.getStores().subscribe({
      next: (stores) => {
        // Solo mostrar tiendas activas (con productos)
        this.stores = stores.filter(s => s.productCount && s.productCount > 0);
        this.filteredStores = this.stores;
        this.setupStoreSearch();
      },
      error: (err) => {
        console.error('Error al cargar tiendas:', err);
      }
    });
  }

  private setupCategorySearch(): void {
    this.categorySearchControl.valueChanges.pipe(
      debounceTime(200),
      takeUntil(this.destroy$)
    ).subscribe(searchTerm => {
      this.filterCategories(searchTerm || '');
    });
  }

  private filterCategories(searchTerm: string): void {
    if (!searchTerm) {
      // Mostrar categorías no seleccionadas
      this.filteredCategories = this.categories.filter(
        c => !this.selectedCategories.some(sc => sc.id === c.id)
      );
    } else {
      const term = searchTerm.toLowerCase();
      this.filteredCategories = this.categories.filter(c =>
        c.name.toLowerCase().includes(term) &&
        !this.selectedCategories.some(sc => sc.id === c.id)
      );
    }
  }

  private setupStoreSearch(): void {
    this.storeSearchControl.valueChanges.pipe(
      debounceTime(200),
      takeUntil(this.destroy$)
    ).subscribe(searchTerm => {
      this.filterStores(searchTerm || '');
    });
  }

  private filterStores(searchTerm: string): void {
    if (!searchTerm) {
      this.filteredStores = this.stores.filter(
        s => !this.selectedStores.some(ss => ss.id === s.id)
      );
    } else {
      const term = searchTerm.toLowerCase();
      this.filteredStores = this.stores.filter(s =>
        s.name.toLowerCase().includes(term) &&
        !this.selectedStores.some(ss => ss.id === s.id)
      );
    }
  }

  private setupFilterSubscription(): void {
    this.filterForm.valueChanges.pipe(
      debounceTime(300),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.pageIndex = 0;
      this.applyFilters();
    });
  }

  private handleRouteParams(): void {
    this.route.queryParams.pipe(
      takeUntil(this.destroy$)
    ).subscribe(params => {
      if (params['search']) {
        this.filters.search = params['search'];
      }
      if (params['categoryId']) {
        const categoryId = +params['categoryId'];
        const category = this.categories.find(c => c.id === categoryId);
        if (category && !this.selectedCategories.some(sc => sc.id === categoryId)) {
          this.selectedCategories.push(category);
        }
      }
      this.loadProducts();
    });
  }

  loadProducts(): void {
    this.isLoading = true;
    this.buildFiltersFromForm();

    this.productService.getProducts(this.filters, this.pageIndex, this.pageSize)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (page: ProductPage) => {
          this.products = page.content;
          this.totalProducts = page.totalElements;
          this.isLoading = false;
          this.updateActiveFilters();
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error al cargar productos:', err);
          this.isLoading = false;
          this.snackBar.open('Error al cargar los productos', 'Cerrar', {
            duration: 3000
          });
        }
      });
  }

  private buildFiltersFromForm(): void {
    const formValue = this.filterForm.value;
    this.filters.sortBy = formValue.sortBy as ProductFilters['sortBy'];

    // Usar múltiples categorías por ID
    if (this.selectedCategories.length > 0) {
      this.filters.categoryIds = this.selectedCategories.map(c => c.id);
      this.filters.categoryId = undefined;
      this.filters.categoryNames = undefined;
    } else {
      this.filters.categoryIds = undefined;
      this.filters.categoryId = undefined;
      this.filters.categoryNames = undefined;
    }

    // Usar múltiples tiendas por ID
    if (this.selectedStores.length > 0) {
      this.filters.storeIds = this.selectedStores.map(s => s.id);
    } else {
      this.filters.storeIds = undefined;
    }
  }

  private updateActiveFilters(): void {
    this.activeFilters = [];

    if (this.filters.search) {
      this.activeFilters.push({ key: 'search', label: `"${this.filters.search}"` });
    }
  }

  applyFilters(): void {
    this.loadProducts();
  }

  onCategoryInputFocus(): void {
    // Mostrar todas las categorías disponibles cuando el usuario hace focus
    this.filterCategories(this.categorySearchControl.value || '');
  }

  onCategorySelected(event: MatAutocompleteSelectedEvent): void {
    const category = event.option.value as Category;

    // Añadir a seleccionadas si no está ya
    if (!this.selectedCategories.some(c => c.id === category.id)) {
      this.selectedCategories.push(category);
    }

    // Limpiar el input
    this.categorySearchControl.setValue('');
    this.filterCategories('');

    // Recargar productos
    this.pageIndex = 0;
    this.loadProducts();
  }

  removeCategory(category: Category): void {
    this.selectedCategories = this.selectedCategories.filter(c => c.id !== category.id);
    this.filterCategories(this.categorySearchControl.value || '');
    this.pageIndex = 0;
    this.loadProducts();
  }

  onStoreInputFocus(): void {
    this.filterStores(this.storeSearchControl.value || '');
  }

  onStoreSelected(event: MatAutocompleteSelectedEvent): void {
    const store = event.option.value as Store;

    if (!this.selectedStores.some(s => s.id === store.id)) {
      this.selectedStores.push(store);
    }

    this.storeSearchControl.setValue('');
    this.filterStores('');

    this.pageIndex = 0;
    this.loadProducts();
  }

  removeStore(store: Store): void {
    this.selectedStores = this.selectedStores.filter(s => s.id !== store.id);
    this.filterStores(this.storeSearchControl.value || '');
    this.pageIndex = 0;
    this.loadProducts();
  }

  removeFilter(filterKey: string): void {
    if (filterKey === 'search') {
      this.filters.search = undefined;
      this.router.navigate([], { queryParams: { search: null }, queryParamsHandling: 'merge' });
    }
    this.loadProducts();
  }

  clearAllFilters(): void {
    this.filters = {};
    this.selectedCategories = [];
    this.selectedStores = [];
    this.categorySearchControl.setValue('');
    this.storeSearchControl.setValue('');
    this.filterCategories('');
    this.filterStores('');
    this.filterForm.reset({
      sortBy: 'relevance'
    });
    this.router.navigate([], { queryParams: {} });
    this.loadProducts();
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadProducts();
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  onAddToList(product: Product): void {
    const dialogRef = this.dialog.open(AddToListDialogComponent, {
      width: '420px',
      data: { productId: product.id, productName: product.name }
    });

    dialogRef.afterClosed().pipe(
      takeUntil(this.destroy$)
    ).subscribe((result: AddToListDialogResult | undefined) => {
      if (!result) return;

      this.shoppingListService.addItem(result.listId, product.id, null, 1).subscribe({
        next: () => {
          this.productsInList.add(product.id);
          this.snackBar.open('Producto añadido a la lista', 'Cerrar', { duration: 2000 });
        },
        error: () => {
          this.snackBar.open('Error al añadir el producto', 'Cerrar', { duration: 3000 });
        }
      });
    });
  }

  onToggleFavorite(product: Product): void {
    if (product.isFavorite) {
      this.productService.removeFromFavorites(product.id).subscribe({
        next: () => {
          product.isFavorite = false;
          this.snackBar.open('Eliminado de favoritos', 'Cerrar', { duration: 2000 });
        }
      });
    } else {
      this.productService.addToFavorites(product.id).subscribe({
        next: () => {
          product.isFavorite = true;
          this.snackBar.open('Añadido a favoritos', 'Cerrar', { duration: 2000 });
        }
      });
    }
  }

  onViewProduct(product: Product): void {
    this.router.navigate(['/producto', product.id]);
  }

  isProductInList(productId: number): boolean {
    return this.productsInList.has(productId);
  }
}
