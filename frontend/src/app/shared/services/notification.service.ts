import { Injectable } from '@angular/core';
import { Apollo } from 'apollo-angular';
import { BehaviorSubject, Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { GET_NOTIFICATIONS, RESPOND_TO_INVITE } from '../../core/graphql/queries';
import { AppNotification } from '../../core/models/group.model';

@Injectable({
    providedIn: 'root'
})
export class NotificationService
{
    private notificationsSubject = new BehaviorSubject<AppNotification[]>([]);
    notifications$ = this.notificationsSubject.asObservable();

    private unreadCountSubject = new BehaviorSubject<number>(0);
    unreadCount$ = this.unreadCountSubject.asObservable();

    constructor(private apollo: Apollo) {}

    loadNotifications(): Observable<AppNotification[]>
    {
        return this.apollo.query<{ getNotifications: AppNotification[] }>(
            {
                query: GET_NOTIFICATIONS,
                fetchPolicy: 'network-only'
            }
        ).pipe(
            map(result => (result.data?.getNotifications || []) as AppNotification[]),
            tap(notifications =>
            {
                this.notificationsSubject.next(notifications);
                this.unreadCountSubject.next(
                    notifications.filter(n => !n.isRead).length
                );
            })
        );
    }

    respondToInvite(notificationId: number, accept: boolean): Observable<boolean>
    {
        return this.apollo.mutate<{ respondToInvite: boolean }>(
            {
                mutation: RESPOND_TO_INVITE,
                variables: {
                    notificationId: notificationId.toString(),
                    accept
                }
            }
        ).pipe(
            map(result => result.data?.respondToInvite || false)
        );
    }

    getInviteNotifications(): AppNotification[]
    {
        return this.notificationsSubject.value.filter(n => n.type === 'INVITE' && !n.isRead);
    }
}
