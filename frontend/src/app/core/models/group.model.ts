export interface Group
{
    groupId: number;
    name: string;
    groupCode: string;
    ownerUsername: string;
    ownerId: number;
    createdAt: string;
    members: GroupMember[];
    shoppingLists: GroupShoppingList[];
}

export interface GroupMember
{
    id: number;
    userId: number;
    username: string;
    email: string;
    status: 'PENDING' | 'ACCEPTED';
}

export interface AppNotification
{
    notificationId: number;
    message: string;
    type: 'INVITE' | 'UPDATE' | 'SYSTEM' | 'BUDGET_ALERT' | 'PURCHASE';
    isRead: boolean;
    relatedGroupId: number | null;
    relatedGroupName: string | null;
    createdAt: string;
}

export interface GroupShoppingList
{
    listId: number;
    name: string;
    createdAt: string;
}
