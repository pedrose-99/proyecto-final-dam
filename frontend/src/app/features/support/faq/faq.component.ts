import { Component } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-faq',
  standalone: true,
  imports: [CommonModule, RouterModule, MatIconModule],
  templateUrl: './faq.component.html',
  styleUrls: ['../support-page.css']
})
export class FaqComponent {
  openIndex: number | null = null;

  faqs = [
    {
      question: '¿Como funciona SmartCart?',
      answer: 'SmartCart recopila los precios de productos de diferentes supermercados (Mercadona, Alcampo, Carrefour, Dia, Ahorramas) y te permite compararlos facilmente para que encuentres siempre el mejor precio.'
    },
    {
      question: '¿Los precios estan actualizados?',
      answer: 'Si. Nuestro sistema actualiza los precios automaticamente mediante scraping de las webs oficiales de los supermercados. Los precios se actualizan cada vez que se inicia la aplicacion.'
    },
    {
      question: '¿Puedo compartir mis listas de la compra?',
      answer: 'Si. Puedes crear grupos colaborativos e invitar a otros usuarios. Todos los miembros del grupo pueden ver y editar las listas compartidas en tiempo real.'
    },
    {
      question: '¿Como funciona el optimizador de cesta?',
      answer: 'El optimizador analiza los productos de tu lista y calcula en que tienda te sale mas barata la compra completa, o como repartir los productos entre tiendas para obtener el mejor precio total.'
    },
    {
      question: '¿Puedo establecer limites de gasto?',
      answer: 'Si. Desde la seccion de gastos puedes configurar limites semanales o mensuales. SmartCart te avisara cuando estes cerca de alcanzar tu limite.'
    },
    {
      question: '¿SmartCart es gratuito?',
      answer: 'Si, SmartCart es completamente gratuito. Es un proyecto academico desarrollado como trabajo de fin de grado (DAM).'
    }
  ];

  constructor(private location: Location) {}
  goBack(): void { this.location.back(); }

  toggle(index: number): void {
    this.openIndex = this.openIndex === index ? null : index;
  }
}
