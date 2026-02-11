import { Component, OnInit, ChangeDetectorRef, ViewChild, ElementRef } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule, Location } from '@angular/common';
import { ProductService } from '../../../../core/services/product.service';
import { Chart, registerables } from 'chart.js';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CommonModule],
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
    private cdr: ChangeDetectorRef,
    private location: Location
  ) {}

  goBack(): void {
  this.location.back(); // Esto vuelve a la página anterior (donde estaba la lista)
}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      // CORRECCIÓN: Quitamos loadProduct(id) porque la lógica está aquí abajo
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
    }
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
    alert('¡Añadido a favoritos!');
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