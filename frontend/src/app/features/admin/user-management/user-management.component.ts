import { Component, OnInit, ViewChild, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormControl } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { AdminService } from '../../../core/services/admin.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserAdmin } from '../../../core/models/admin.model';
import { CreateUserDialogComponent } from './create-user-dialog.component';

@Component({
    selector: 'app-user-management',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        MatCardModule,
        MatTableModule,
        MatPaginatorModule,
        MatFormFieldModule,
        MatInputModule,
        MatSelectModule,
        MatButtonModule,
        MatIconModule,
        MatTooltipModule,
        MatDialogModule,
        MatSnackBarModule,
        MatProgressSpinnerModule
    ],
    templateUrl: './user-management.component.html',
    styleUrls: ['./user-management.component.css']
})
export class UserManagementComponent implements OnInit
{
    @ViewChild(MatPaginator) paginator!: MatPaginator;

    users: UserAdmin[] = [];
    totalElements = 0;
    pageSize = 10;
    pageIndex = 0;
    loading = true;

    searchControl = new FormControl('');
    roleFilter = '';

    displayedColumns = ['id', 'username', 'email', 'roleName', 'createdAt', 'actions'];

    currentUserEmail: string;

    constructor(
        private adminService: AdminService,
        private authService: AuthService,
        private dialog: MatDialog,
        private snackBar: MatSnackBar,
        private cdr: ChangeDetectorRef
    ) {
        this.currentUserEmail = this.authService.getCurrentUser()?.email ?? '';
    }

    ngOnInit(): void
    {
        this.loadUsers();

        this.searchControl.valueChanges.pipe(
            debounceTime(400),
            distinctUntilChanged()
        ).subscribe(() => {
            this.pageIndex = 0;
            if (this.paginator) this.paginator.firstPage();
            this.loadUsers();
        });
    }

    loadUsers(): void
    {
        this.loading = true;
        const search = this.searchControl.value || undefined;
        const role = this.roleFilter || undefined;

        this.adminService.getUsers(this.pageIndex, this.pageSize, role, search).subscribe({
            next: (page) => {
                this.users = page.content;
                this.totalElements = page.totalElements;
                this.loading = false;
                this.cdr.detectChanges();
            },
            error: () => { this.loading = false; this.cdr.detectChanges(); }
        });
    }

    onPageChange(event: PageEvent): void
    {
        this.pageIndex = event.pageIndex;
        this.pageSize = event.pageSize;
        this.loadUsers();
    }

    onRoleFilterChange(): void
    {
        this.pageIndex = 0;
        if (this.paginator) this.paginator.firstPage();
        this.loadUsers();
    }

    openCreateDialog(): void
    {
        const dialogRef = this.dialog.open(CreateUserDialogComponent, {
            width: '460px',
            disableClose: true,
            autoFocus: true,
            hasBackdrop: true
        });

        dialogRef.afterClosed().subscribe(result => {
            if (result) {
                this.loadUsers();
                this.snackBar.open('Usuario creado correctamente', 'OK', { duration: 3000 });
            }
        });
    }

    deleteUser(user: UserAdmin): void
    {
        if (!confirm(`¿Estás seguro de que quieres eliminar al usuario ${user.username}?`)) return;

        this.adminService.deleteUser(user.id).subscribe({
            next: () => {
                this.loadUsers();
                this.snackBar.open('Usuario eliminado', 'OK', { duration: 3000 });
            },
            error: (err) => {
                this.snackBar.open(err.error?.message || 'Error al eliminar usuario', 'Cerrar', { duration: 3000 });
            }
        });
    }

    isSelf(user: UserAdmin): boolean
    {
        return user.email === this.currentUserEmail;
    }

    formatDate(dateStr: string): string
    {
        if (!dateStr) return '-';
        const d = new Date(dateStr);
        return d.toLocaleDateString('es-ES', { day: '2-digit', month: '2-digit', year: 'numeric' });
    }
}
