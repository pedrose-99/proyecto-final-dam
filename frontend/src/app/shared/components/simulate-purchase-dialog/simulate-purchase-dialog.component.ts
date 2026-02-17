import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';

export interface SimulatePurchaseDialogData {
  listId: number;
  listName: string;
}

export interface SimulatePurchaseDialogResult {
  billName: string;
}

@Component({
  selector: 'app-simulate-purchase-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon class="title-icon">receipt_long</mat-icon>
      Simular Compra
    </h2>
    <mat-dialog-content>
      <p class="subtitle">Lista: <strong>{{ data.listName }}</strong></p>
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Nombre de la compra</mat-label>
        <input matInput [formControl]="billNameControl"
               placeholder="Ej: Compra semanal 15/02"
               (keyup.enter)="confirm()">
        @if (billNameControl.hasError('required') && billNameControl.touched) {
          <mat-error>El nombre es obligatorio</mat-error>
        }
      </mat-form-field>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancelar</button>
      <button mat-flat-button color="primary" (click)="confirm()"
              [disabled]="billNameControl.invalid">
        Confirmar
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .title-icon {
      vertical-align: middle;
      margin-right: 8px;
      color: var(--smartcart-accent);
    }
    .subtitle {
      margin: 0 0 16px;
      font-size: 14px;
      color: var(--smartcart-text-light);
    }
    .full-width {
      width: 100%;
    }
  `]
})
export class SimulatePurchaseDialogComponent {
  billNameControl = new FormControl('', Validators.required);

  constructor(
    @Inject(MAT_DIALOG_DATA) public data: SimulatePurchaseDialogData,
    private dialogRef: MatDialogRef<SimulatePurchaseDialogComponent>
  ) {}

  confirm(): void {
    if (this.billNameControl.invalid) return;
    this.dialogRef.close({ billName: this.billNameControl.value!.trim() } as SimulatePurchaseDialogResult);
  }
}
