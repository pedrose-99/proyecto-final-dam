package com.smartcart.smartcart.modules.shoppinglist.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.smartcart.smartcart.modules.product.repository.ProductRepository;
import com.smartcart.smartcart.modules.shoppinglist.dto.ShoppingListDTO;
import com.smartcart.smartcart.modules.shoppinglist.entity.ListItem;
import com.smartcart.smartcart.modules.shoppinglist.entity.ShoppingList;
import com.smartcart.smartcart.modules.shoppinglist.mapper.ShoppingListMapper;
import com.smartcart.smartcart.modules.shoppinglist.repository.ShoppingListRepository;
import com.smartcart.smartcart.modules.user.entity.User;
import com.smartcart.smartcart.modules.user.repository.UserRepository;

import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShoppingListService 
{
    private final ShoppingListRepository slRepository;
    private final ProductRepository productRepository;
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

    public List<ShoppingListDTO> getMyLists()
    {
        Optional<User> user = getCurrentUser();
        if (user.isEmpty())
        {
            return List.of();
        }
        
        try
        {
            return slRepository.findByUser_IdUserOrderByUpdatedAtDesc(user.get().getIdUser()).stream()
                .map(ShoppingListMapper::toDTO)
                .toList();
        }
        catch(RuntimeException e)
        {
            log.error("Listas no encontradas", e.getMessage());
            return List.of();
        }
    }

    @Transactional
    public ShoppingListDTO createList(String name)
    {
        Optional<User> user = getCurrentUser();

        try
        {
            ShoppingList shoppinglist = new ShoppingList();
            shoppinglist.setUser(user.get());
            shoppinglist.setName(name);
            
            return ShoppingListMapper.toDTO(slRepository.save(shoppinglist));
        }
        catch(RuntimeException e)
        {
            throw new RuntimeException("No se pudo crear la lista", e);
        }
    }

    @SuppressWarnings("null")
    @Transactional
    public boolean deleteList(Integer listId)
    {
        Optional<User> user = getCurrentUser();
        try
        {
            Optional<ShoppingList> list = slRepository.findByListIdAndUser_IdUser(listId, user.get().getIdUser());
            slRepository.delete(list.get());
            return true;
        }
        catch(RuntimeException e)
        {
           log.error("No se pudo eliminar la lista", e.getMessage()); 
        }
        return false;
    }

    @Transactional
    public ShoppingListDTO addItem(Integer listId, Integer productId, String genericName, Integer quantity)
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
        ListItem newItem = new ListItem();
        newItem.setShoppingList(list);
        if(quantity == null)
        {
            quantity = 1;
            newItem.setQuantity(quantity);
        }
        newItem.setChecked(false);
        if(productId != null)
        {
            try
            {
                productRepository.findByNameIgnoreCase(genericName);
                newItem.setGenericName(genericName);
            }
            catch(RuntimeException e)
            {
                log.error("Producto no encontrado", e.getMessage());
            } 
        }

       list.getItems().add(newItem);
       return ShoppingListMapper.toDTO(slRepository.save(list));
    }

    @Transactional
    public ShoppingListDTO updateItem(Integer listId, Integer itemId, Integer quantity, Boolean checked)
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
        Optional<ListItem> itemOpt = list.getItems().stream()
            .filter(i -> i.getItemId().equals(itemId))
            .findFirst();

        if (itemOpt.isEmpty())
        {
            throw new RuntimeException("Item no encontrado");
        }

        ListItem item = itemOpt.get();
        if (quantity != null)
        {
            item.setQuantity(quantity);
        }
        if (checked != null)
        {
            item.setChecked(checked);
        }

        slRepository.save(list);
        return ShoppingListMapper.toDTO(list);
    }

    @Transactional
    public ShoppingListDTO removeItem(Integer listId, Integer itemId, Integer quantity, Boolean checked)
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

        list.getItems().removeIf(i -> i.getItemId().equals(itemId));

        slRepository.save(list);
        return ShoppingListMapper.toDTO(list);
    }

}
