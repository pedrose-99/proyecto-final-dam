import { Component } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-help',
  standalone: true,
  imports: [CommonModule, RouterModule, MatIconModule],
  templateUrl: './help.component.html',
  styleUrls: ['../support-page.css']
})
export class HelpComponent {
  constructor(private location: Location) {}
  goBack(): void { this.location.back(); }
}
