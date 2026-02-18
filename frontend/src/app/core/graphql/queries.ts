import { gql } from 'apollo-angular';

// Productos
export const GET_ALL_PRODUCTS = gql`
  query GetAllProducts($page: Int, $size: Int) {
    allProducts(page: $page, size: $size) {
      content {
        productId
        name
        brand
        categoryName
        imageUrl
      }
      totalElements
      totalPages
      number
      size
      first
      last
    }
  }
`;

export const GET_PRODUCT_BY_EAN = gql`
  query GetProductByEan($ean: String!) {
    productByEan(ean: $ean) {
      productId
      name
      brand
      categoryName
      imageUrl
    }
  }
`;
export const GET_PRODUCT_COMPARISON = gql`
  query GetProductComparison($productId: ID!) {
    compareProduct(productId: $productId) {
      productId
      name
      brand
      ean
      description  # <--- HE AÑADIDO ESTA LÍNEA
      imageUrl
      categoryName
      storePrices {
        storeId
        storeName
        currentPrice
        externaId
        storeWebsite
        available
        stock
        url
      }
      bestPrice {
        storeId
        currentPrice
      }
    }
  }
`;

export const GET_PRODUCTS_BY_CATEGORY = gql`
  query GetProductsByCategory($categoryId: ID!, $page: Int, $size: Int) {
    productsByCategoryPaginated(categoryId: $categoryId, page: $page, size: $size) {
      content {
        productId
        name
        brand
        categoryName
        imageUrl
      }
      totalElements
      totalPages
      number
      size
      first
      last
    }
  }
`;

export const GET_PRODUCTS_BY_STORE = gql`
  query GetProductsByStore($storeId: ID!, $page: Int, $size: Int) {
    productsByStore(storeId: $storeId, page: $page, size: $size) {
      content {
        productId
        name
        brand
        categoryName
        imageUrl
      }
      totalElements
      totalPages
      number
      size
      first
      last
    }
  }
`;

// Busqueda
export const SEARCH_PRODUCTS = gql`
  query SearchProducts($query: String!, $page: Int, $size: Int) {
    searchProducts(query: $query, page: $page, size: $size) {
      content {
        productId
        name
        brand
        categoryName
        imageUrl
      }
      totalElements
      totalPages
      number
      size
      first
      last
    }
  }
`;

// Categorias
export const GET_ALL_CATEGORIES = gql`
  query GetAllCategories {
    allCategories {
      categoryId
      name
      description
    }
  }
`;

export const GET_CATEGORY_BY_ID = gql`
  query GetCategoryById($id: ID!) {
    categoryById(id: $id) {
      categoryId
      name
      description
    }
  }
`;

// Tiendas
export const GET_ALL_STORES = gql`
  query GetAllStores {
    allStores {
      storeId
      name
      slug
      logo
      website
      active
      productCount
    }
  }
`;

export const GET_STORE_BY_ID = gql`
  query GetStoreById($id: ID!) {
    storeById(id: $id) {
      storeId
      name
      slug
      logo
      website
      active
    }
  }
`;

// ProductStore (productos con precios por tienda)
export const GET_STORES_BY_PRODUCT = gql`
  query GetStoresByProduct($productId: ID!) {
    storesByProduct(productId: $productId) {
      storeProductId
      productName
      productBrand
      ean
      storeName
      currentPrice
      url
      available
      stock
      externaId
      unit
    }
  }
`;

export const GET_PRICE_HISTORY = gql`
  query GetPriceHistory($productId: ID!) {
    getPriceHistory(productId: $productId) {
      price
      recordedAt
      storeName
    }
  }
`;

// Shopping Lists
export const GET_MY_SHOPPING_LISTS = gql`
  query MyShoppingLists {
    myShoppingLists {
      listId
      name
      userId
      username
      groupId
      groupName
      createdAt

      items {
        itemId
        productId
        displayName
        imageUrl
        quantity
        checked
        isGeneric
        cheapestStoreName
      }
    }
  }
`;

export const GET_SHOPPING_LIST_BY_ID = gql`
  query GetShoppingListById($listId: ID!) {
    shoppingListById(listId: $listId) {
      listId
      name
      userId
      username
      groupId
      groupName
      createdAt

      items {
        itemId
        productId
        displayName
        imageUrl
        quantity
        checked
        isGeneric
        cheapestStoreName
      }
    }
  }
`;

export const CREATE_SHOPPING_LIST = gql`
  mutation CreateShoppingList($name: String!, $groupId: ID) {
    createShoppingList(name: $name, groupId: $groupId) {
      listId
      name
      createdAt

      items {
        itemId
        productId
        displayName
        imageUrl
        quantity
        checked
        isGeneric
        cheapestStoreName
      }
    }
  }
`;

export const DELETE_SHOPPING_LIST = gql`
  mutation DeleteShoppingList($listId: ID!) {
    deleteShoppingList(listId: $listId)
  }
`;

export const ADD_ITEM_TO_LIST = gql`
  mutation AddItemToList($listId: ID!, $productId: ID, $genericName: String, $quantity: Int) {
    addItemToList(listId: $listId, productId: $productId, genericName: $genericName, quantity: $quantity) {
      listId
      name
      createdAt

      items {
        itemId
        productId
        displayName
        imageUrl
        quantity
        checked
        isGeneric
        cheapestStoreName
      }
    }
  }
`;

export const UPDATE_LIST_ITEM = gql`
  mutation UpdateListItem($listId: ID!, $itemId: ID!, $quantity: Int, $checked: Boolean) {
    updateListItem(listId: $listId, itemId: $itemId, quantity: $quantity, checked: $checked) {
      listId
      name
      createdAt

      items {
        itemId
        productId
        displayName
        imageUrl
        quantity
        checked
        isGeneric
        cheapestStoreName
      }
    }
  }
`;

export const REMOVE_LIST_ITEM = gql`
  mutation RemoveListItem($listId: ID!, $itemId: ID!) {
    removeListItem(listId: $listId, itemId: $itemId) {
      listId
      name
      createdAt

      items {
        itemId
        productId
        displayName
        imageUrl
        quantity
        checked
        isGeneric
        cheapestStoreName
      }
    }
  }
`;

export const CREATE_SUBLISTS = gql`
  mutation CreateSublists($originalListName: String!, $sublists: [SublistInput!]!) {
    createSublists(originalListName: $originalListName, sublists: $sublists) {
      listId
      name
      createdAt

      items {
        itemId
        productId
        displayName
        imageUrl
        quantity
        checked
        isGeneric
        cheapestStoreName
      }
    }
  }
`;

export const RENAME_SHOPPING_LIST = gql`
  mutation RenameShoppingList($listId: ID!, $name: String!) {
    renameShoppingList(listId: $listId, name: $name) {
      listId
      name
      createdAt

      items {
        itemId
        productId
        displayName
        imageUrl
        quantity
        checked
        isGeneric
        cheapestStoreName
      }
    }
  }
`;

export const OPTIMIZE_SHOPPING_LIST = gql`
  query OptimizeShoppingList($listId: ID!, $storeIds: [ID!]!) {
    optimizeShoppingList(listId: $listId, storeIds: $storeIds) {
      totalCost
      storeGroups {
        storeId
        storeName
        storeLogo
        subtotal
        items {
          productId
          productName
          imageUrl
          unitPrice
          quantity
          lineTotal
          searchTerm
        }
      }
      notFound
    }
  }
`;

export const OPTIMIZE_BY_STORE = gql`
  query OptimizeByStore($listId: ID!, $storeIds: [ID!]!) {
    optimizeByStore(listId: $listId, storeIds: $storeIds) {
      storeId
      storeName
      storeLogo
      subtotal
      items {
        productId
        productName
        imageUrl
        unitPrice
        quantity
        lineTotal
        searchTerm
      }
      notFound
    }
  }
`;

export const SEARCH_PRODUCTS_BY_STORE = gql`
  query SearchProductsByStore($query: String!, $storeId: ID!, $page: Int, $size: Int) {
    searchProductsByStore(query: $query, storeId: $storeId, page: $page, size: $size) {
      content {
        productId
        name
        brand
        categoryName
        imageUrl
        currentPrice
      }
      totalElements
      totalPages
      number
      size
      first
      last
    }
  }
`;

// Grupos colaborativos
export const GET_MY_GROUPS = gql`
  query GetMyGroups {
    getMyGroups {
      groupId
      name
      groupCode
      ownerUsername
      ownerId
      createdAt
      members {
        id
        userId
        username
        email
        status
      }
    }
  }
`;

export const GET_GROUP_DETAILS = gql`
  query GetGroupDetails($groupId: ID!) {
    getGroupDetails(groupId: $groupId) {
      groupId
      name
      groupCode
      ownerUsername
      ownerId
      createdAt
      members {
        id
        userId
        username
        email
        status
      }
      shoppingLists {
        listId
        name
        createdAt
      }
    }
  }
`;

export const CREATE_GROUP = gql`
  mutation CreateGroup($name: String!) {
    createGroup(name: $name) {
      groupId
      name
      groupCode
      ownerUsername
      ownerId
      createdAt
      members {
        id
        userId
        username
        email
        status
      }
    }
  }
`;

export const INVITE_TO_GROUP = gql`
  mutation InviteToGroup($groupId: ID!, $target: String!) {
    inviteToGroup(groupId: $groupId, target: $target)
  }
`;

export const JOIN_GROUP_BY_CODE = gql`
  mutation JoinGroupByCode($code: String!) {
    joinGroupByCode(code: $code) {
      groupId
      name
      groupCode
      ownerUsername
      ownerId
      createdAt
      members {
        id
        userId
        username
        email
        status
      }
    }
  }
`;

// Notificaciones
export const GET_NOTIFICATIONS = gql`
  query GetNotifications {
    getNotifications {
      notificationId
      message
      type
      isRead
      relatedGroupId
      relatedGroupName
      createdAt
    }
  }
`;

export const RESPOND_TO_INVITE = gql`
  mutation RespondToInvite($notificationId: ID!, $accept: Boolean!) {
    respondToInvite(notificationId: $notificationId, accept: $accept)
  }
`;

export const DELETE_NOTIFICATION = gql`
  mutation DeleteNotification($notificationId: ID!) {
    deleteNotification(notificationId: $notificationId)
  }
`;

export const DELETE_GROUP = gql`
  mutation DeleteGroup($groupId: ID!) {
    deleteGroup(groupId: $groupId)
  }
`;

export const LEAVE_GROUP = gql`
  mutation LeaveGroup($groupId: ID!) {
    leaveGroup(groupId: $groupId)
  }
`;

export const REMOVE_GROUP_MEMBER = gql`
  mutation RemoveGroupMember($groupId: ID!, $userId: Int!) {
    removeGroupMember(groupId: $groupId, userId: $userId)
  }
`;

// Favoritos
export const GET_MY_FAVORITES = gql`
  query MyFavorites {
    myFavorites {
      productId
      name
      brand
      imageUrl
      categoryName
      isFavorite
    }
  }
`;

export const IS_FAVORITE = gql`
  query IsFavorite($productId: ID!) {
    isFavorite(productId: $productId)
  }
`;

export const ADD_TO_FAVORITES = gql`
  mutation AddToFavorites($productId: ID!) {
    addToFavorites(productId: $productId)
  }
`;

export const REMOVE_FROM_FAVORITES = gql`
  mutation RemoveFromFavorites($productId: ID!) {
    removeFromFavorites(productId: $productId)
  }
`;
// Gastos / Historial
export const CREATE_BILL_FROM_LIST = gql`
  mutation CreateBillFromList($listId: ID!, $billName: String!) {
    createBillFromList(listId: $listId, billName: $billName) {
      billsHistoryId
      name
      recordedAt
      totalAmount
      exceededLimit
      itemsSummary {
        productName
        price
        quantity
        storeName
      }
    }
  }
`;

export const GET_BILLS_HISTORY = gql`
  query GetBillsHistory($filter: String, $month: Int, $year: Int) {
    getBillsHistory(filter: $filter, month: $month, year: $year) {
      billsHistoryId
      name
      recordedAt
      totalAmount
      exceededLimit
      itemsSummary {
        productName
        price
        quantity
        storeName
      }
    }
  }
`;

export const GET_SPENDING_LIMITS = gql`
  query GetSpendingLimits {
    getSpendingLimits {
      limitId
      amount
      type
      isActive
    }
  }
`;

export const SAVE_SPENDING_LIMIT = gql`
  mutation SaveSpendingLimit($amount: Float!, $type: String!) {
    saveSpendingLimit(amount: $amount, type: $type) {
      limitId
      amount
      type
      isActive
    }
  }
`;

export const GET_EXPENSE_SUMMARY = gql`
  query GetExpenseSummary($period: String, $offset: Int) {
    getExpenseSummary(period: $period, offset: $offset) {
      periodLabel
      totalAmount
      billCount
      exceededCount
    }
  }
`;

