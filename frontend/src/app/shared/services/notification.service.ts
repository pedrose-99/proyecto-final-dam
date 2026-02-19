import { Injectable } from '@angular/core';
import { Apollo } from 'apollo-angular';
import { BehaviorSubject, Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import {
    GET_NOTIFICATIONS,
    RESPOND_TO_INVITE,
    DELETE_NOTIFICATION,
    MARK_NOTIFICATION_AS_READ,
    MARK_ALL_NOTIFICATIONS_AS_READ,
    GET_NOTIFICATIONS_PAGINATED
} from '../../core/graphql/queries';
import { AppNotification } from '../../core/models/group.model';

export interface NotificationPage
{
    content: AppNotification[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
}

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

    getNotificationsPaginated(page: number, size: number): Observable<NotificationPage>
    {
        return this.apollo.query<{ getNotificationsPaginated: NotificationPage }>(
            {
                query: GET_NOTIFICATIONS_PAGINATED,
                variables: { page, size },
                fetchPolicy: 'network-only'
            }
        ).pipe(
            map(result => result.data!.getNotificationsPaginated)
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

    deleteNotification(notificationId: number): Observable<boolean>
    {
        return this.apollo.mutate<{ deleteNotification: boolean }>(
            {
                mutation: DELETE_NOTIFICATION,
                variables: {
                    notificationId: notificationId.toString()
                }
            }
        ).pipe(
            map(result => result.data?.deleteNotification || false)
        );
    }

    markAsRead(notificationId: number): Observable<boolean>
    {
        return this.apollo.mutate<{ markNotificationAsRead: boolean }>(
            {
                mutation: MARK_NOTIFICATION_AS_READ,
                variables: {
                    notificationId: notificationId.toString()
                }
            }
        ).pipe(
            map(result => result.data?.markNotificationAsRead || false)
        );
    }

    markAllAsRead(): Observable<boolean>
    {
        return this.apollo.mutate<{ markAllNotificationsAsRead: boolean }>(
            {
                mutation: MARK_ALL_NOTIFICATIONS_AS_READ
            }
        ).pipe(
            map(result => result.data?.markAllNotificationsAsRead || false)
        );
    }

    getInviteNotifications(): AppNotification[]
    {
        return this.notificationsSubject.value.filter(n => n.type === 'INVITE' && !n.isRead);
    }
}
