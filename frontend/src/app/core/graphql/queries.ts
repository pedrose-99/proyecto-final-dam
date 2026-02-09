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
