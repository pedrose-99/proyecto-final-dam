import { Injectable } from '@angular/core';
import { Apollo } from 'apollo-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
    GET_MY_GROUPS,
    GET_GROUP_DETAILS,
    CREATE_GROUP,
    INVITE_TO_GROUP,
    JOIN_GROUP_BY_CODE
} from '../../core/graphql/queries';
import { Group } from '../../core/models/group.model';

@Injectable({
    providedIn: 'root'
})
export class GroupService
{
    constructor(private apollo: Apollo) {}

    getMyGroups(): Observable<Group[]>
    {
        return this.apollo.query<{ getMyGroups: Group[] }>(
            {
                query: GET_MY_GROUPS,
                fetchPolicy: 'network-only'
            }
        ).pipe(
            map(result => (result.data?.getMyGroups || []) as Group[])
        );
    }

    getGroupDetails(groupId: number): Observable<Group>
    {
        return this.apollo.query<{ getGroupDetails: Group }>(
            {
                query: GET_GROUP_DETAILS,
                variables: { groupId: groupId.toString() },
                fetchPolicy: 'network-only'
            }
        ).pipe(
            map(result => result.data?.getGroupDetails as Group)
        );
    }

    createGroup(name: string): Observable<Group>
    {
        return this.apollo.mutate<{ createGroup: Group }>(
            {
                mutation: CREATE_GROUP,
                variables: { name }
            }
        ).pipe(
            map(result => result.data?.createGroup as Group)
        );
    }

    inviteToGroup(groupId: number, target: string): Observable<boolean>
    {
        return this.apollo.mutate<{ inviteToGroup: boolean }>(
            {
                mutation: INVITE_TO_GROUP,
                variables: {
                    groupId: groupId.toString(),
                    target
                }
            }
        ).pipe(
            map(result => result.data?.inviteToGroup || false)
        );
    }

    joinGroupByCode(code: string): Observable<Group>
    {
        return this.apollo.mutate<{ joinGroupByCode: Group }>(
            {
                mutation: JOIN_GROUP_BY_CODE,
                variables: { code }
            }
        ).pipe(
            map(result => result.data?.joinGroupByCode as Group)
        );
    }
}
