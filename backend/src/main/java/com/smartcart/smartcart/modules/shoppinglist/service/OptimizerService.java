package com.smartcart.smartcart.modules.shoppinglist.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

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

    private Optional<User> getCurrentUser()
    {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        try
        {
           return userRepository.findByEmail(email);
        }
        catch(RuntimeException e)
        {
            log.error("Usuario no encontrado", e.getMessage());
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
        List<ProductStore> candidates;

        if (item.getProduct() != null)
        {
            candidates = psRepository.findByProductId_ProductId(item.getProduct().getProductId());
        }
        else
        {            
            String searchTerm = item.getGenericName();
            if (searchTerm == null || searchTerm.isBlank())
            {
                return null;
            }

            Optional<Product> productOpt = productRepository.findByNameIgnoreCase(searchTerm);
            if (productOpt.isEmpty())
            {
                return null;
            }

            List<Product> matchingProducts = List.of(productOpt.get());

            candidates = new ArrayList<>();
            for (Product p : matchingProducts)
            {
                candidates.addAll(psRepository.findByProductId_ProductId(p.getProductId()));
            }
        }

        return candidates.stream()
                .filter(ps -> storeIds.contains(ps.getStoreId().getStoreId()))
                .filter(ps -> Boolean.TRUE.equals(ps.getAvailable()))
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

    public OptimizedListDTO optimize(Integer listId, List<Integer> storeIds)
    {
        Optional<User> user = getCurrentUser();
        Optional<ShoppingList> list = slRepository.findByListIdAndUser_IdUser(listId,user.get().getIdUser());

        LinkedHashMap<Integer, List<OptimizedItemDTO>> storeItemsMap = new LinkedHashMap<>();
        LinkedHashMap<Integer, String[]> storeInfoMap = new LinkedHashMap<>();
        List<String> notFound = new ArrayList<>();

        for(ListItem item : list.get().getItems())
        {
            BestMatch bestMatch = findBestPrice(item, storeIds);

            if(bestMatch == null)
            {
                notFound.add(item.getDisplayName());
                continue;
            }
            storeInfoMap.putIfAbsent(bestMatch.storeId(), new String[]{bestMatch.storeName(), bestMatch.storeLogo()});

            OptimizedItemDTO optimizedItem = new OptimizedItemDTO(
                bestMatch.productId(),
                bestMatch.productName(),
                bestMatch.imageUrl(),
                bestMatch.price(),
                item.getQuantity(),
                bestMatch.price() * item.getQuantity()
            );

            storeItemsMap.computeIfAbsent(bestMatch.storeId(), key -> new ArrayList<>()).add(optimizedItem);
        }

        List<OptimizedStoreDTO> storeGroups = new ArrayList<>();
        double total = 0.0;

        for(Map.Entry<Integer, List<OptimizedItemDTO>> entry : storeItemsMap.entrySet())
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


}
    