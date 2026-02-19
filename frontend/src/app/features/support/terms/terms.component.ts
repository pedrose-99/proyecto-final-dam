import { Component } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-terms',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './terms.component.html',
  styleUrls: ['../support-page.css']
})
export class TermsComponent {
  constructor(private location: Location) {}
  goBack(): void { this.location.back(); }
}
