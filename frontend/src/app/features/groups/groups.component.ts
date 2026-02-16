import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, takeUntil } from 'rxjs';
import { GroupService } from '../../shared/services/group.service';
import { NotificationService } from '../../shared/services/notification.service';
import { Group, AppNotification } from '../../core/models/group.model';

@Component({
    selector: 'app-groups',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        MatCardModule,
        MatButtonModule,
        MatIconModule,
        MatFormFieldModule,
        MatInputModule,
        MatSnackBarModule,
        MatProgressSpinnerModule,
        MatChipsModule,
        MatDividerModule,
        MatTooltipModule
    ],
    templateUrl: './groups.component.html',
    styleUrls: ['./groups.component.css']
})
export class GroupsComponent implements OnInit, OnDestroy
{
    groups: Group[] = [];
    pendingInvites: AppNotification[] = [];
    isLoading = true;

    showCreateForm = false;
    showJoinForm = false;
    isCreating = false;
    isJoining = false;

    createNameControl = new FormControl('');
    joinCodeControl = new FormControl('');

    private destroy$ = new Subject<void>();

    constructor(
        private groupService: GroupService,
        private notificationService: NotificationService,
        private snackBar: MatSnackBar,
        private router: Router
    ) {}

    ngOnInit(): void
    {
        this.loadGroups();
        this.loadInvites();
    }

    ngOnDestroy(): void
    {
        this.destroy$.next();
        this.destroy$.complete();
    }

    loadGroups(): void
    {
        this.isLoading = true;
        this.groupService.getMyGroups().pipe(
            takeUntil(this.destroy$)
        ).subscribe({
            next: groups =>
            {
                this.groups = groups;
                this.isLoading = false;
            },
            error: () =>
            {
                this.snackBar.open('Error al cargar los grupos', 'Cerrar', { duration: 3000 });
                this.isLoading = false;
            }
        });
    }

    loadInvites(): void
    {
        this.notificationService.loadNotifications().pipe(
            takeUntil(this.destroy$)
        ).subscribe({
            next: () =>
            {
                this.pendingInvites = this.notificationService.getInviteNotifications();
            }
        });
    }

    toggleCreateForm(): void
    {
        this.showCreateForm = !this.showCreateForm;
        this.showJoinForm = false;
        this.createNameControl.setValue('');
    }

    toggleJoinForm(): void
    {
        this.showJoinForm = !this.showJoinForm;
        this.showCreateForm = false;
        this.joinCodeControl.setValue('');
    }

    createGroup(): void
    {
        const name = this.createNameControl.value?.trim();
        if (!name) return;

        this.isCreating = true;
        this.groupService.createGroup(name).pipe(
            takeUntil(this.destroy$)
        ).subscribe({
            next: group =>
            {
                this.groups = [group, ...this.groups];
                this.showCreateForm = false;
                this.createNameControl.setValue('');
                this.isCreating = false;
                this.snackBar.open('Grupo creado correctamente', 'Cerrar', { duration: 3000 });
            },
            error: () =>
            {
                this.isCreating = false;
                this.snackBar.open('Error al crear el grupo', 'Cerrar', { duration: 3000 });
            }
        });
    }

    joinGroup(): void
    {
        const code = this.joinCodeControl.value?.trim();
        if (!code) return;

        this.isJoining = true;
        this.groupService.joinGroupByCode(code).pipe(
            takeUntil(this.destroy$)
        ).subscribe({
            next: group =>
            {
                this.groups = [group, ...this.groups];
                this.showJoinForm = false;
                this.joinCodeControl.setValue('');
                this.isJoining = false;
                this.snackBar.open('Te has unido al grupo correctamente', 'Cerrar', { duration: 3000 });
            },
            error: () =>
            {
                this.isJoining = false;
                this.snackBar.open('Codigo invalido o error al unirse', 'Cerrar', { duration: 3000 });
            }
        });
    }

    respondToInvite(notification: AppNotification, accept: boolean): void
    {
        this.notificationService.respondToInvite(notification.notificationId, accept).pipe(
            takeUntil(this.destroy$)
        ).subscribe({
            next: () =>
            {
                this.pendingInvites = this.pendingInvites.filter(
                    n => n.notificationId !== notification.notificationId
                );
                if (accept)
                {
                    this.loadGroups();
                    this.snackBar.open('Invitacion aceptada', 'Cerrar', { duration: 3000 });
                }
                else
                {
                    this.snackBar.open('Invitacion rechazada', 'Cerrar', { duration: 3000 });
                }
                this.notificationService.loadNotifications().pipe(
                    takeUntil(this.destroy$)
                ).subscribe();
            },
            error: () =>
            {
                this.snackBar.open('Error al responder a la invitacion', 'Cerrar', { duration: 3000 });
            }
        });
    }

    goToGroup(group: Group): void
    {
        this.router.navigate(['/grupos', group.groupId]);
    }

    getAcceptedCount(group: Group): number
    {
        return group.members?.filter(m => m.status === 'ACCEPTED').length || 0;
    }
}
