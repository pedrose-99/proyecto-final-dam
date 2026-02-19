import { Component } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-privacy',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './privacy.component.html',
  styleUrls: ['../support-page.css']
})
export class PrivacyComponent {
  constructor(private location: Location) {}
  goBack(): void { this.location.back(); }
}
