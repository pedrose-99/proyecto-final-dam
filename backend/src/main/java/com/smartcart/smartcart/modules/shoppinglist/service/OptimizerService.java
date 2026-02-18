package com.smartcart.smartcart.modules.shoppinglist.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartcart.smartcart.modules.product.entity.Product;
import com.smartcart.smartcart.modules.product.entity.ProductStore;
import com.smartcart.smartcart.modules.product.repository.ProductRepository;
import com.smartcart.smartcart.modules.product.repository.ProductStoreRepository;
import com.smartcart.smartcart.modules.shoppinglist.dto.OptimizedItemDTO;
import com.smartcart.smartcart.modules.shoppinglist.dto.OptimizedListDTO;
import com.smartcart.smartcart.modules.shoppinglist.dto.OptimizedStoreDTO;
import com.smartcart.smartcart.modules.shoppinglist.entity.ListItem;
import com.smartcart.smartcart.modules.shoppinglist.entity.ShoppingList;
import com.smartcart.smartcart.modules.shoppinglist.repository.ShoppingListRepository;
import com.smartcart.smartcart.modules.store.entity.Store;
import com.smartcart.smartcart.modules.store.repository.StoreRepository;
import com.smartcart.smartcart.modules.user.entity.User;
import com.smartcart.smartcart.modules.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OptimizerService
{
    private final ShoppingListRepository slRepository;
    private final ProductRepository productRepository;
    private final ProductStoreRepository psRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;

    private Optional<User> getCurrentUser()
    {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        try
        {
            return userRepository.findByEmail(email);
        }
        catch (RuntimeException e)
        {
            log.error("Usuario no encontrado: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private record BestMatch(
        Integer storeId,
        String storeName,
        String storeLogo,
        Integer productId,
        String productName,
        String imageUrl,
        Double price
    ){}

    private BestMatch findBestPrice(ListItem item, List<Integer> storeIds)
    {
        // Para productos concretos, buscar en qué tiendas está disponible
        if (item.getProduct() != null)
        {
            return psRepository.findByProductId_ProductId(item.getProduct().getProductId()).stream()
                    .filter(ps -> storeIds.contains(ps.getStoreId().getStoreId()))
                    .filter(ps -> ps.getCurrentPrice() != null && ps.getCurrentPrice() > 0)
                    .min(Comparator.comparingDouble(ProductStore::getCurrentPrice))
                    .map(ps -> new BestMatch(
                            ps.getStoreId().getStoreId(),
                            ps.getStoreId().getName(),
                            ps.getStoreId().getLogo(),
                            ps.getProductId().getProductId(),
                            ps.getProductId().getName(),
                            ps.getProductId().getImageUrl(),
                            ps.getCurrentPrice()
                    ))
                    .orElse(null);
        }

        // Para genéricos: buscar el producto más relevante EN CADA tienda
        // y quedarse con el más barato entre todas
        String searchTerm = item.getGenericName();
        if (searchTerm == null || searchTerm.isBlank())
        {
            return null;
        }

        BestMatch best = null;

        for (Integer storeId : storeIds)
        {
            List<Product> results = productRepository
                .searchByTextAndStore(searchTerm, storeId, PageRequest.of(0, 1))
                .getContent();

            if (results.isEmpty()) continue;

            Product product = results.get(0);
            Optional<ProductStore> ps = psRepository.findByProductId_ProductId(product.getProductId()).stream()
                    .filter(p -> p.getStoreId().getStoreId().equals(storeId))
                    .filter(p -> p.getCurrentPrice() != null && p.getCurrentPrice() > 0)
                    .findFirst();

            if (ps.isPresent() && (best == null || ps.get().getCurrentPrice() < best.price()))
            {
                ProductStore found = ps.get();
                best = new BestMatch(
                    found.getStoreId().getStoreId(),
                    found.getStoreId().getName(),
                    found.getStoreId().getLogo(),
                    found.getProductId().getProductId(),
                    found.getProductId().getName(),
                    found.getProductId().getImageUrl(),
                    found.getCurrentPrice()
                );
            }
        }

        return best;
    }

    @Transactional(readOnly = true)
    public OptimizedListDTO optimize(Integer listId, List<Integer> storeIds)
    {
        Optional<User> user = getCurrentUser();
        if (user.isEmpty())
        {
            throw new RuntimeException("Usuario no encontrado");
        }

        Optional<ShoppingList> listOpt = slRepository.findByListIdAndUser_IdUser(listId, user.get().getIdUser());
        if (listOpt.isEmpty())
        {
            throw new RuntimeException("Lista no encontrada");
        }

        ShoppingList list = listOpt.get();
        LinkedHashMap<Integer, List<OptimizedItemDTO>> storeItemsMap = new LinkedHashMap<>();
        LinkedHashMap<Integer, String[]> storeInfoMap = new LinkedHashMap<>();
        List<String> notFound = new ArrayList<>();

        for (ListItem item : list.getItems())
        {
            if (Boolean.TRUE.equals(item.getChecked())) continue;

            BestMatch bestMatch = findBestPrice(item, storeIds);

            if (bestMatch == null)
            {
                notFound.add(item.getProduct() != null ? item.getProduct().getName() : item.getGenericName());
                continue;
            }

            storeInfoMap.putIfAbsent(bestMatch.storeId(), new String[]{bestMatch.storeName(), bestMatch.storeLogo()});

            String searchTerm = item.getProduct() != null ? item.getProduct().getName() : item.getGenericName();
            OptimizedItemDTO optimizedItem = new OptimizedItemDTO(
                bestMatch.productId(),
                bestMatch.productName(),
                bestMatch.imageUrl(),
                bestMatch.price(),
                item.getQuantity(),
                bestMatch.price() * item.getQuantity(),
                searchTerm
            );

            storeItemsMap.computeIfAbsent(bestMatch.storeId(), key -> new ArrayList<>()).add(optimizedItem);
        }

        List<OptimizedStoreDTO> storeGroups = new ArrayList<>();
        double total = 0.0;

        for (Map.Entry<Integer, List<OptimizedItemDTO>> entry : storeItemsMap.entrySet())
        {
            Integer storeId = entry.getKey();
            List<OptimizedItemDTO> items = entry.getValue();
            String[] info = storeInfoMap.get(storeId);

            double subtotal = items.stream()
                    .mapToDouble(OptimizedItemDTO::lineTotal)
                    .sum();

            OptimizedStoreDTO storeGroup = new OptimizedStoreDTO(
                storeId,
                info[0],
                info[1],
                subtotal,
                items
            );

            storeGroups.add(storeGroup);
            total += subtotal;
        }

        return new OptimizedListDTO(total, storeGroups, notFound);
    }

    private BestMatch findProductForStore(ListItem item, Integer storeId)
    {
        // Si es un producto concreto, buscar directamente si existe en esta tienda
        if (item.getProduct() != null)
        {
            return psRepository.findByProductId_ProductId(item.getProduct().getProductId()).stream()
                    .filter(ps -> ps.getStoreId().getStoreId().equals(storeId))
                    .filter(ps -> ps.getCurrentPrice() != null && ps.getCurrentPrice() > 0)
                    .findFirst()
                    .map(ps -> new BestMatch(
                            ps.getStoreId().getStoreId(),
                            ps.getStoreId().getName(),
                            ps.getStoreId().getLogo(),
                            ps.getProductId().getProductId(),
                            ps.getProductId().getName(),
                            ps.getProductId().getImageUrl(),
                            ps.getCurrentPrice()
                    ))
                    .orElse(null);
        }

        // Para genéricos: buscar DENTRO de la tienda directamente
        String searchTerm = item.getGenericName();
        if (searchTerm == null || searchTerm.isBlank())
        {
            return null;
        }

        // Buscar productos que contengan el término Y existan en esta tienda
        List<Product> searchResults = productRepository
            .searchByTextAndStore(searchTerm, storeId, PageRequest.of(0, 1))
            .getContent();

        if (searchResults.isEmpty())
        {
            return null;
        }

        Product bestProduct = searchResults.get(0);
        return psRepository.findByProductId_ProductId(bestProduct.getProductId()).stream()
                .filter(ps -> ps.getStoreId().getStoreId().equals(storeId))
                .filter(ps -> ps.getCurrentPrice() != null && ps.getCurrentPrice() > 0)
                .findFirst()
                .map(ps -> new BestMatch(
                        ps.getStoreId().getStoreId(),
                        ps.getStoreId().getName(),
                        ps.getStoreId().getLogo(),
                        ps.getProductId().getProductId(),
                        ps.getProductId().getName(),
                        ps.getProductId().getImageUrl(),
                        ps.getCurrentPrice()
                ))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<OptimizedStoreDTO> optimizeByStore(Integer listId, List<Integer> storeIds)
    {
        Optional<User> user = getCurrentUser();
        if (user.isEmpty())
        {
            throw new RuntimeException("Usuario no encontrado");
        }

        Optional<ShoppingList> listOpt = slRepository.findByListIdAndUser_IdUser(listId, user.get().getIdUser());
        if (listOpt.isEmpty())
        {
            throw new RuntimeException("Lista no encontrada");
        }

        ShoppingList list = listOpt.get();
        List<OptimizedStoreDTO> result = new ArrayList<>();

        for (Integer storeId : storeIds)
        {
            Store store = storeRepository.findById(storeId).orElse(null);
            if (store == null) continue;

            List<OptimizedItemDTO> items = new ArrayList<>();
            List<String> notFoundInStore = new ArrayList<>();
            double subtotal = 0.0;

            for (ListItem item : list.getItems())
            {
                if (Boolean.TRUE.equals(item.getChecked())) continue;

                BestMatch match = findProductForStore(item, storeId);

                if (match == null)
                {
                    notFoundInStore.add(item.getProduct() != null ? item.getProduct().getName() : item.getGenericName());
                    continue;
                }

                String searchTerm = item.getProduct() != null ? item.getProduct().getName() : item.getGenericName();
                double lineTotal = match.price() * item.getQuantity();
                items.add(new OptimizedItemDTO(
                    match.productId(),
                    match.productName(),
                    match.imageUrl(),
                    match.price(),
                    item.getQuantity(),
                    lineTotal,
                    searchTerm
                ));
                subtotal += lineTotal;
            }

            result.add(new OptimizedStoreDTO(
                storeId,
                store.getName(),
                store.getLogo(),
                subtotal,
                items,
                notFoundInStore
            ));
        }

        return result;
    }
}
