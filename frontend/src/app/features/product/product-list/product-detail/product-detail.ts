import { Component, OnInit, ChangeDetectorRef, ViewChild, ElementRef } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule, Location } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ProductService } from '../../../../core/services/product.service';
import { FavoriteService } from '../../../../shared/services/favorite.service';
import { ShoppingListService } from '../../../../shared/services/shopping-list.service';
import { SelectShoppingListDialogComponent } from './select-shopping-list.dialog';
import { Chart, registerables } from 'chart.js';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    MatSnackBarModule,
    MatDialogModule
  ],
  templateUrl: './product-detail.html',
  styleUrls: ['./product-detail.css']
})
export class ProductDetailComponent implements OnInit {
  @ViewChild('priceChart') priceChartCanvas!: ElementRef;
  
  priceHistory: any[] = [];
  chart: any;
  showHistory = false;
  loading = true;
  stores: any[] = [];
  bestPrice: number | null = null;
  productId: number | null = null;
  isFavorite = false;
  isInShoppingList = false;
  
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
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  goBack(): void {
  this.location.back(); // Esto vuelve a la página anterior (donde estaba la lista)
}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.productId = parseInt(id);
      
      // Cargar datos del producto
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
      
      // Comprobar si está en favoritos
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

      // Comprobar si está en alguna lista de compra (después de 500ms para dar tiempo a cargar)
      setTimeout(() => {
        this.checkIfInShoppingList();
      }, 500);
    }
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
        
        // Buscar en todas las listas
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
    if (this.showHistory) {
      // Esperamos un ciclo de renderizado para que el canvas exista en el DOM
      setTimeout(() => {
        if (this.priceHistory.length === 0) {
          this.loadHistoryData();
        } else {
          this.renderChart();
        }
      }, 100);
    }
  }
  toggleFavorite(): void {
    if (!this.productId) return;
    
    if (this.isFavorite) {
      this.favoriteService.removeFromFavorites(this.productId).subscribe({
        next: () => {
          this.isFavorite = false;
          this.snackBar.open('Eliminado de favoritos', 'Cerrar', { duration: 2000 });
          this.cdr.detectChanges();
        },
        error: () => {
          this.snackBar.open('Error al eliminar de favoritos', 'Cerrar', { duration: 3000 });
        }
      });
    } else {
      this.favoriteService.addToFavorites(this.productId).subscribe({
        next: () => {
          this.isFavorite = true;
          this.snackBar.open('Añadido a favoritos', 'Cerrar', { duration: 2000 });
          this.cdr.detectChanges();
        },
        error: () => {
          this.snackBar.open('Error al añadir a favoritos', 'Cerrar', { duration: 3000 });
        }
      });
    }
  }

  addToShoppingList(): void {
    if (!this.productId) {
      this.snackBar.open('Error: No se puede añadir el producto', 'Cerrar', { duration: 3000 });
      return;
    }

    // Obtener listas frescas del servidor (network-only para evitar caché)
    this.shoppingListService.getMyLists().subscribe({
      next: (lists: any[]) => {
        // Filtrar listas válidas (que no sean null)
        const validLists = lists.filter(list => list && list.listId && list.name);
        
        if (validLists.length === 0) {
          this.snackBar.open('No tienes listas de compra. Crea una primera.', 'Cerrar', { duration: 3000 });
          return;
        }

        console.log('Listas disponibles:', validLists);

        // Abrir diálogo para seleccionar lista
        this.dialog.open(SelectShoppingListDialogComponent, {
          width: '400px',
          data: { lists: validLists, productId: this.productId, productName: this.product.name }
        }).afterClosed().subscribe(result => {
          if (result && result.success) {
            console.log('✅ Producto añadido exitosamente a', result.addedCount, 'listas');
            
            // Actualizar inmediatamente
            this.isInShoppingList = true;
            this.cdr.detectChanges();
            this.snackBar.open(`Añadido a ${result.addedCount} lista(s)`, 'Cerrar', { duration: 2000 });
            
            // Refrescar después de 1 segundo para confirmar
            setTimeout(() => {
              console.log('🔄 Refrescando verificación...');
              this.checkIfInShoppingList();
            }, 1000);
          }
        });
      },
      error: (err) => {
        console.error('Error al obtener listas:', err);
        this.snackBar.open('Error al obtener tus listas de compra', 'Cerrar', { duration: 3000 });
      }
    });
  }
  loadHistoryData() {
  const id = this.route.snapshot.paramMap.get('id');
  if (!id) return;

  this.productService.getPriceHistory(id).subscribe({
    next: (data: any) => {
      // Si data viene como null, undefined o no es array, forzamos array vacío
      const historyArray = Array.isArray(data) ? data : [];
      
      if (historyArray.length > 0) {
        // Invertimos para que el gráfico vaya de pasado a futuro
        this.priceHistory = [...historyArray].reverse(); 
        this.renderChart();
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
 renderChart() {
  // 1. Validaciones de seguridad:
  // - Comprobamos que el elemento Canvas existe en el DOM
  // - Comprobamos que hay al menos 2 puntos (si no, una línea no se puede dibujar)
  if (!this.priceChartCanvas || this.priceHistory.length < 2) {
    if (this.chart) {
      this.chart.destroy();
      this.chart = null;
    }
    return;
  }

  // 2. Obtención del contexto del Canvas
  const ctx = this.priceChartCanvas.nativeElement.getContext('2d');
  
  // 3. Limpieza: Si ya existía un gráfico, lo destruimos antes de crear el nuevo
  if (this.chart) {
    this.chart.destroy();
  }

  // 4. Creación del gráfico con Chart.js
  this.chart = new Chart(ctx, {
    type: 'line',
    data: {
      // Eje X: Convertimos las fechas ISO a formato local (dd/mm/aaaa)
      labels: this.priceHistory.map(h => new Date(h.recordedAt).toLocaleDateString()),
      datasets: [{
        label: 'Evolución del Precio (€)',
        data: this.priceHistory.map(h => h.price),
        borderColor: '#1a237e',           // Azul oscuro profesional
        backgroundColor: 'rgba(26, 35, 126, 0.1)', // Sombreado bajo la línea
        fill: true,                       // Rellenar el área inferior
        tension: 0.3,                     // Suavizado de la línea (curva)
        pointRadius: 4,                   // Tamaño de los puntos
        pointBackgroundColor: '#1a237e'
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,         // Permite que el CSS controle la altura
      plugins: {
        legend: { display: false }        // Ocultamos la leyenda para un look más limpio
      },
      scales: {
        y: { 
          beginAtZero: false,             // Ajusta el zoom del eje Y al rango de precios
          ticks: { 
            // Añadimos el símbolo del Euro al eje Y
            callback: (value: string | number) => value + '€' 
          }
        }
      }
    }
  });
}
}