import { Component, OnInit, ChangeDetectorRef, ViewChild, ElementRef, effect } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule, Location } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';

import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { ProductService } from '../../../../core/services/product.service';
import { FavoriteService } from '../../../../shared/services/favorite.service';
import { ShoppingListService } from '../../../../shared/services/shopping-list.service';
import { ProductCardComponent } from '../../../../shared/components/product-card/product-card.component';
import { AddToListDialogComponent, AddToListDialogResult } from '../../../../shared/components/add-to-list-dialog/add-to-list-dialog.component';
import { ThemeService } from '../../../../core/services/theme.service';
import { Chart, registerables } from 'chart.js';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,

    MatDialogModule,
    ProductCardComponent
  ],
  templateUrl: './product-detail.html',
  styleUrls: ['./product-detail.css']
})
export class ProductDetailComponent implements OnInit {
  @ViewChild('priceChart') priceChartCanvas!: ElementRef;
  
  priceHistory: any[] = [];
  chart: any;
  showHistory = true;
  historyPage = 0;
  historyPageSize = 5;
  loading = true;
  stores: any[] = [];
  bestPrice: number | null = null;
  productId: number | null = null;
  isFavorite = false;
  isInShoppingList = false;
  
  relatedProducts: any[] = [];
  carouselIndex = 0;
  carouselVisible = 3;

  product: any = {
    name: 'Cargando...',
    ean: '',
    description: '',
    imageUrl: '',
    rating: 4.5,
    reviews: 185
  };

  constructor(
    private route: ActivatedRoute,
    private productService: ProductService,
    private favoriteService: FavoriteService,
    private shoppingListService: ShoppingListService,
    private cdr: ChangeDetectorRef,
    private location: Location,
    private dialog: MatDialog,
    private router: Router,
    private themeService: ThemeService
  ) {
    effect(() => {
      this.themeService.isDark();
      if (this.priceHistory.length >= 2) {
        setTimeout(() => this.renderChart(), 50);
      }
    });
  }

  goBack(): void {
  this.location.back(); // Esto vuelve a la página anterior (donde estaba la lista)
}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.loadProduct(id);
      }
    });
  }

  private loadProduct(id: string): void {
    window.scrollTo({ top: 0, behavior: 'instant' });
    document.documentElement.scrollTop = 0;
    document.body.scrollTop = 0;
    this.loading = true;
    this.productId = parseInt(id);
    this.relatedProducts = [];
    this.carouselIndex = 0;
    this.priceHistory = [];
    this.stores = [];
    this.bestPrice = null;
    this.isFavorite = false;
    this.isInShoppingList = false;
    if (this.chart) {
      this.chart.destroy();
      this.chart = null;
    }

    this.productService.getComparison(id).subscribe({
        next: (data: any) => {
          if (data) {
            this.product = {
              name: data.name,
              ean: data.ean,
              brand: data.brand,
              description: data.description || 'Sin descripción disponible para este producto.',
              imageUrl: data.imageUrl,
              rating: 4.5,
              reviews: 185
            };
            this.stores = data.storePrices || [];
            this.bestPrice = data.bestPrice?.currentPrice || null;

            if (data.categoryId) {
              this.productService.getRelatedProducts(data.categoryId, data.productId).subscribe({
                next: (products) => {
                  this.relatedProducts = products.map((p: any) => ({
                    id: p.productId,
                    name: p.name,
                    brand: p.brand,
                    imageUrl: p.imageUrl,
                    categoryName: p.categoryName,
                    isFavorite: false
                  }));
                  this.cdr.detectChanges();
                }
              });
            }
          }
          this.loading = false;
          this.cdr.detectChanges();
        },
        error: (err: any) => {
          console.error('Error cargando producto:', err);
          this.loading = false;
          this.cdr.detectChanges();
        }
      });
      
      this.favoriteService.isFavorite(this.productId).subscribe({
        next: (isFav) => {
          this.isFavorite = isFav;
          console.log('❤️ isFavorite:', this.isFavorite);
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error al comprobar favorito:', err);
        }
      });

      setTimeout(() => {
        this.checkIfInShoppingList();
      }, 500);

      this.loadHistoryData();
  }

  checkIfInShoppingList(): void {
    if (!this.productId) {
      console.log('❌ No hay productId');
      return;
    }

    const productIdNum = Number(this.productId);
    console.log('🔍 Verificando si producto', productIdNum, 'está en alguna lista');

    this.shoppingListService.getMyLists().subscribe({
      next: (lists: any[]) => {
        console.log('📦 Listas obtenidas:', lists.length, 'listas');
        
        let found = false;
        
        for (const list of lists) {
          if (!list || !Array.isArray(list.items)) {
            console.log(`⚠️ Lista ${list?.name} sin items válidos`);
            continue;
          }
          
          for (const item of list.items) {
            const itemProductId = Number(item.productId);
            if (itemProductId === productIdNum) {
              console.log(`✅ ENCONTRADO en lista "${list.name}": ${item.displayName}`);
              found = true;
              break;
            }
          }
          
          if (found) break;
        }
        
        this.isInShoppingList = found;
        console.log('🛒 isInShoppingList =', this.isInShoppingList);
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('❌ Error al verificar listas de compra:', err);
        this.isInShoppingList = false;
      }
    });
  }
  toggleHistory() {
    this.showHistory = !this.showHistory;
    if (this.showHistory && this.priceHistory.length >= 2) {
      setTimeout(() => this.renderChart(), 100);
    }
  }
  toggleFavorite(): void {
    if (!this.productId) return;
    
    if (this.isFavorite) {
      this.favoriteService.removeFromFavorites(this.productId).subscribe({
        next: () => {
          this.isFavorite = false;
          this.cdr.detectChanges();
        },
        error: () => {
        }
      });
    } else {
      this.favoriteService.addToFavorites(this.productId).subscribe({
        next: () => {
          this.isFavorite = true;
          this.cdr.detectChanges();
        },
        error: () => {
        }
      });
    }
  }

  addToShoppingList(): void {
    if (!this.productId) {
      return;
    }

    const dialogRef = this.dialog.open(AddToListDialogComponent, {
      width: '420px',
      data: { productId: this.productId, productName: this.product.name }
    });

    dialogRef.afterClosed().subscribe((result: AddToListDialogResult | undefined) => {
      if (!result) return;

      this.shoppingListService.addItem(result.listId, this.productId!, null, 1).subscribe({
        next: () => {
          this.isInShoppingList = true;
          this.cdr.detectChanges();
        }
      });
    });
  }
  loadHistoryData() {
  const id = this.route.snapshot.paramMap.get('id');
  if (!id) return;

  this.productService.getPriceHistory(id).subscribe({
    next: (data: any) => {
      const historyArray = Array.isArray(data) ? data : [];
      
      if (historyArray.length > 0) {
        this.priceHistory = [...historyArray].reverse();
        setTimeout(() => this.renderChart(), 150);
      } else {
        this.priceHistory = [];
      }
      this.cdr.detectChanges();
    },
    error: (err) => {
      console.error('Error en el historial:', err);
      this.priceHistory = [];
      this.cdr.detectChanges();
    }
  });
}
  get paginatedHistory(): any[] {
    const start = this.historyPage * this.historyPageSize;
    return this.priceHistory.slice(start, start + this.historyPageSize);
  }

  get totalHistoryPages(): number {
    return Math.ceil(this.priceHistory.length / this.historyPageSize);
  }

  historyPrevPage(): void {
    if (this.historyPage > 0) this.historyPage--;
  }

  historyNextPage(): void {
    if (this.historyPage < this.totalHistoryPages - 1) this.historyPage++;
  }

  get visibleRelated(): any[] {
    return this.relatedProducts.slice(this.carouselIndex, this.carouselIndex + this.carouselVisible);
  }

  get canScrollLeft(): boolean {
    return this.carouselIndex > 0;
  }

  get canScrollRight(): boolean {
    return this.carouselIndex + this.carouselVisible < this.relatedProducts.length;
  }

  scrollCarousel(direction: number): void {
    this.carouselIndex += direction;
    if (this.carouselIndex < 0) this.carouselIndex = 0;
    if (this.carouselIndex + this.carouselVisible > this.relatedProducts.length) {
      this.carouselIndex = this.relatedProducts.length - this.carouselVisible;
    }
  }

  goToProduct(product: any): void {
    this.router.navigate(['/producto', product.id]);
  }

 renderChart() {
  if (!this.priceChartCanvas || this.priceHistory.length < 2) {
    if (this.chart) {
      this.chart.destroy();
      this.chart = null;
    }
    return;
  }

  const ctx = this.priceChartCanvas.nativeElement.getContext('2d');

  if (this.chart) {
    this.chart.destroy();
  }

  const isDark = document.documentElement.classList.contains('dark');
  const lineColor = isDark ? '#60a5fa' : '#1a237e';
  const fillColor = isDark ? 'rgba(96, 165, 250, 0.15)' : 'rgba(26, 35, 126, 0.1)';
  const textColor = isDark ? '#e2e8f0' : '#1e293b';
  const gridColor = isDark ? 'rgba(148, 163, 184, 0.2)' : 'rgba(0, 0, 0, 0.1)';

  this.chart = new Chart(ctx, {
    type: 'line',
    data: {
      labels: this.priceHistory.map(h => new Date(h.recordedAt).toLocaleDateString()),
      datasets: [{
        label: 'Evolución del Precio (€)',
        data: this.priceHistory.map(h => h.price),
        borderColor: lineColor,
        backgroundColor: fillColor,
        fill: true,
        tension: 0.3,
        pointRadius: 4,
        pointBackgroundColor: lineColor
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { display: false }
      },
      scales: {
        x: {
          ticks: { color: textColor },
          grid: { color: gridColor }
        },
        y: {
          beginAtZero: false,
          ticks: {
            color: textColor,
            callback: (value: string | number) => Number(value).toFixed(2) + '€'
          },
          grid: { color: gridColor }
        }
      }
    }
  });
}
}