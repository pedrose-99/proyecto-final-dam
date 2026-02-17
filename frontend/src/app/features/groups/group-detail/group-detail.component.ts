import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Clipboard, ClipboardModule } from '@angular/cdk/clipboard';
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
import { MatListModule } from '@angular/material/list';
import { Subject, takeUntil } from 'rxjs';
import { GroupService } from '../../../shared/services/group.service';
import { ShoppingListService } from '../../../shared/services/shopping-list.service';
import { Group, GroupMember } from '../../../core/models/group.model';

@Component({
    selector: 'app-group-detail',
    standalone: true,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        ClipboardModule,
        MatCardModule,
        MatButtonModule,
        MatIconModule,
        MatFormFieldModule,
        MatInputModule,
        MatSnackBarModule,
        MatProgressSpinnerModule,
        MatChipsModule,
        MatDividerModule,
        MatTooltipModule,
        MatListModule
    ],
    templateUrl: './group-detail.component.html',
    styleUrls: ['./group-detail.component.css']
})
export class GroupDetailComponent implements OnInit, OnDestroy
{
    group: Group | null = null;
    isLoading = true;

    showInviteForm = false;
    isInviting = false;
    inviteTargetControl = new FormControl('');

    showCreateListForm = false;
    isCreatingList = false;
    listNameControl = new FormControl('');

    private destroy$ = new Subject<void>();

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private groupService: GroupService,
        private shoppingListService: ShoppingListService,
        private clipboard: Clipboard,
        private snackBar: MatSnackBar
    ) {}

    ngOnInit(): void
    {
        const id = this.route.snapshot.paramMap.get('id');
        if (id)
        {
            this.loadGroup(+id);
        }
    }

    ngOnDestroy(): void
    {
        this.destroy$.next();
        this.destroy$.complete();
    }

    loadGroup(groupId: number): void
    {
        this.isLoading = true;
        this.groupService.getGroupDetails(groupId).pipe(
            takeUntil(this.destroy$)
        ).subscribe({
            next: group =>
            {
                this.group = group;
                this.isLoading = false;
            },
            error: () =>
            {
                this.snackBar.open('Error al cargar el grupo', 'Cerrar', { duration: 3000 });
                this.isLoading = false;
            }
        });
    }

    copyCode(): void
    {
        if (this.group?.groupCode)
        {
            this.clipboard.copy(this.group.groupCode);
            this.snackBar.open('Codigo copiado al portapapeles', 'Cerrar', { duration: 2000 });
        }
    }

    toggleInviteForm(): void
    {
        this.showInviteForm = !this.showInviteForm;
        this.inviteTargetControl.setValue('');
    }

    inviteUser(): void
    {
        const target = this.inviteTargetControl.value?.trim();
        if (!target || !this.group) return;

        this.isInviting = true;
        this.groupService.inviteToGroup(this.group.groupId, target).pipe(
            takeUntil(this.destroy$)
        ).subscribe({
            next: () =>
            {
                this.showInviteForm = false;
                this.inviteTargetControl.setValue('');
                this.isInviting = false;
                this.snackBar.open('Invitacion enviada correctamente', 'Cerrar', { duration: 3000 });
                this.loadGroup(this.group!.groupId);
            },
            error: (err) =>
            {
                this.isInviting = false;
                const message = err?.graphQLErrors?.[0]?.message || 'Error al enviar la invitacion';
                this.snackBar.open(message, 'Cerrar', { duration: 3000 });
            }
        });
    }

    get acceptedMembers(): GroupMember[]
    {
        return this.group?.members?.filter(m => m.status === 'ACCEPTED') || [];
    }

    get pendingMembers(): GroupMember[]
    {
        return this.group?.members?.filter(m => m.status === 'PENDING') || [];
    }

    toggleCreateListForm(): void
    {
        this.showCreateListForm = !this.showCreateListForm;
        this.listNameControl.setValue('');
    }

    createList(): void
    {
        const name = this.listNameControl.value?.trim();
        if (!name || !this.group) return;

        this.isCreatingList = true;
        this.shoppingListService.createList(name, this.group.groupId).pipe(
            takeUntil(this.destroy$)
        ).subscribe({
            next: () =>
            {
                this.showCreateListForm = false;
                this.listNameControl.setValue('');
                this.isCreatingList = false;
                this.snackBar.open('Lista creada correctamente', 'Cerrar', { duration: 3000 });
                this.loadGroup(this.group!.groupId);
            },
            error: () =>
            {
                this.isCreatingList = false;
                this.snackBar.open('Error al crear la lista', 'Cerrar', { duration: 3000 });
            }
        });
    }

    goBack(): void
    {
        this.router.navigate(['/grupos']);
    }

    goToList(listId: number): void
    {
        this.router.navigate(['/lists'], { queryParams: { listId } });
    }
}
