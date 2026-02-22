package com.smartcart.smartcart.modules.shoppinglist.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.smartcart.smartcart.modules.product.repository.ProductRepository;
import com.smartcart.smartcart.modules.product.repository.ProductStoreRepository;
import com.smartcart.smartcart.modules.shoppinglist.dto.ShoppingListDTO;
import com.smartcart.smartcart.modules.shoppinglist.entity.ListItem;
import com.smartcart.smartcart.modules.shoppinglist.entity.ShoppingList;
import com.smartcart.smartcart.modules.shoppinglist.mapper.ShoppingListMapper;
import com.smartcart.smartcart.modules.shoppinglist.repository.ShoppingListRepository;
import com.smartcart.smartcart.modules.group.entity.Group;
import com.smartcart.smartcart.modules.group.entity.GroupMember;
import com.smartcart.smartcart.modules.group.repository.GroupMemberRepository;
import com.smartcart.smartcart.modules.group.repository.GroupRepository;
import com.smartcart.smartcart.modules.user.entity.User;
import com.smartcart.smartcart.modules.user.repository.UserRepository;

import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShoppingListService 
{
    private final ShoppingListRepository slRepository;
    private final ProductRepository productRepository;
    private final ProductStoreRepository productStoreRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

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
            Integer userId = user.get().getIdUser();

            List<ShoppingList> ownLists = slRepository.findByUser_IdUserOrderByCreatedAtDesc(userId);

            List<GroupMember> memberships = groupMemberRepository.findAcceptedMembershipsByUserId(userId);
            List<Integer> groupIds = memberships.stream()
                    .map(gm -> gm.getGroup().getGroupId())
                    .toList();

            List<ShoppingList> groupLists = groupIds.isEmpty()
                    ? List.of()
                    : slRepository.findByGroup_GroupIdInOrderByCreatedAtDesc(groupIds);

            Set<Integer> seenIds = new LinkedHashSet<>();
            List<ShoppingListDTO> result = new ArrayList<>();

            for (ShoppingList sl : ownLists)
            {
                if (seenIds.add(sl.getListId()))
                {
                    result.add(ShoppingListMapper.toDTO(sl, productStoreRepository));
                }
            }
            for (ShoppingList sl : groupLists)
            {
                if (seenIds.add(sl.getListId()))
                {
                    result.add(ShoppingListMapper.toDTO(sl, productStoreRepository));
                }
            }

            return result;
        }
        catch(RuntimeException e)
        {
            log.error("Listas no encontradas", e.getMessage());
            return List.of();
        }
    }

    public ShoppingListDTO getListById(Integer listId)
    {
        Optional<User> user = getCurrentUser();
        if (user.isEmpty())
        {
            return null;
        }
        return findAccessibleList(listId, user.get())
                .map(sl -> ShoppingListMapper.toDTO(sl, productStoreRepository))
                .orElse(null);
    }

    private Optional<ShoppingList> findAccessibleList(Integer listId, User user)
    {
        Optional<ShoppingList> ownList = slRepository.findByListIdAndUser_IdUser(listId, user.getIdUser());
        if (ownList.isPresent())
        {
            return ownList;
        }

        Optional<ShoppingList> listOpt = slRepository.findByIdWithUser(listId);
        if (listOpt.isPresent() && listOpt.get().getGroup() != null)
        {
            Integer groupId = listOpt.get().getGroup().getGroupId();
            Optional<GroupMember> membership = groupMemberRepository.findAcceptedMember(groupId, user.getIdUser());
            if (membership.isPresent())
            {
                return listOpt;
            }
        }

        return Optional.empty();
    }

    @Transactional
    public ShoppingListDTO createList(String name, Integer groupId)
    {
        Optional<User> user = getCurrentUser();

        try
        {
            ShoppingList shoppinglist = new ShoppingList();
            shoppinglist.setUser(user.get());
            shoppinglist.setName(name);

            if (groupId != null)
            {
                Optional<Group> group = groupRepository.findById(groupId);
                group.ifPresent(shoppinglist::setGroup);
            }

            return ShoppingListMapper.toDTO(slRepository.save(shoppinglist), productStoreRepository);
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
        if (user.isEmpty()) {
            log.error("Usuario no encontrado");
            return false;
        }
        
        try
        {
            Optional<ShoppingList> listOpt = findAccessibleList(listId, user.get());
            if (listOpt.isEmpty()) {
                log.error("Lista no encontrada o no pertenece al usuario");
                return false;
            }
            
            ShoppingList list = listOpt.get();
            
            boolean canDelete = false;
            
            if (list.getGroup() != null) {
                Optional<GroupMember> membership = groupMemberRepository.findAcceptedMember(
                    list.getGroup().getGroupId(), 
                    user.get().getIdUser()
                );
                canDelete = membership.isPresent();
            } else {
                canDelete = list.getUser().getIdUser().equals(user.get().getIdUser());
            }
            
            if (!canDelete) {
                log.error("El usuario no tiene permiso para eliminar la lista");
                return false;
            }
            
            if (list.getGroup() != null) {
                Group group = list.getGroup();
                list.setGroup(null);
                group.getShoppingLists().remove(list);
            }
            
            list.getItems().clear();
            slRepository.flush();
            
            slRepository.delete(list);
            slRepository.flush();
            
            log.info("Lista {} eliminada correctamente", listId);
            return true;
        }
        catch(Exception e)
        {
           log.error("No se pudo eliminar la lista: {}", e.getMessage(), e); 
           throw new RuntimeException("Error al eliminar la lista", e);
        }
    }

    @Transactional
    public ShoppingListDTO addItem(Integer listId, Integer productId, String genericName, Integer quantity)
    {
        Optional<User> user = getCurrentUser();
        if (user.isEmpty())
        {
            throw new RuntimeException("Usuario no encontrado");
        }

        Optional<ShoppingList> listOpt = findAccessibleList(listId, user.get());
        if (listOpt.isEmpty())
        {
            throw new RuntimeException("Lista no encontrada");
        }

        ShoppingList list = listOpt.get();
        ListItem newItem = new ListItem();
        newItem.setShoppingList(list);
        newItem.setQuantity(quantity != null ? quantity : 1);
        newItem.setChecked(false);

        if (productId != null)
        {
            var productOpt = productRepository.findById(productId);
            if (productOpt.isEmpty())
            {
                throw new RuntimeException("Producto no encontrado con id: " + productId);
            }
            newItem.setProduct(productOpt.get());
        }
        else if (genericName != null && !genericName.isBlank())
        {
            newItem.setGenericName(genericName);
        }
        else
        {
            throw new RuntimeException("Se requiere productId o genericName");
        }

        list.getItems().add(newItem);
        return ShoppingListMapper.toDTO(slRepository.save(list), productStoreRepository);
    }

    @Transactional
    public ShoppingListDTO updateItem(Integer listId, Integer itemId, Integer quantity, Boolean checked)
    {
        Optional<User> user = getCurrentUser();
        if (user.isEmpty())
        {
            throw new RuntimeException("Usuario no encontrado");
        }

        Optional<ShoppingList> listOpt = findAccessibleList(listId, user.get());
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
        return ShoppingListMapper.toDTO(list, productStoreRepository);
    }

    @Transactional
    public ShoppingListDTO removeItem(Integer listId, Integer itemId)
    {
        Optional<User> user = getCurrentUser();
        if (user.isEmpty())
        {
            throw new RuntimeException("Usuario no encontrado");
        }

        Optional<ShoppingList> listOpt = findAccessibleList(listId, user.get());
        if (listOpt.isEmpty())
        {
            throw new RuntimeException("Lista no encontrada");
        }
        ShoppingList list = listOpt.get();

        list.getItems().removeIf(i -> i.getItemId().equals(itemId));

        slRepository.save(list);
        return ShoppingListMapper.toDTO(list, productStoreRepository);
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public List<ShoppingListDTO> createSublists(String originalListName, List<Map<String, Object>> sublists)
    {
        Optional<User> user = getCurrentUser();
        if (user.isEmpty())
        {
            throw new RuntimeException("Usuario no encontrado");
        }

        List<ShoppingListDTO> result = new ArrayList<>();

        for (Map<String, Object> sublist : sublists)
        {
            String storeName = (String) sublist.get("storeName");
            List<Map<String, Object>> items = (List<Map<String, Object>>) sublist.get("items");

            ShoppingList shoppingList = new ShoppingList();
            shoppingList.setUser(user.get());
            shoppingList.setName(storeName + " de " + originalListName);

            for (Map<String, Object> item : items)
            {
                Integer productId = Integer.valueOf(item.get("productId").toString());
                Integer quantity = Integer.valueOf(item.get("quantity").toString());

                var productOpt = productRepository.findById(productId);
                if (productOpt.isEmpty())
                {
                    log.warn("Producto no encontrado al crear sublista: {}", productId);
                    continue;
                }

                ListItem listItem = new ListItem();
                listItem.setShoppingList(shoppingList);
                listItem.setProduct(productOpt.get());
                listItem.setQuantity(quantity);
                listItem.setChecked(false);

                shoppingList.getItems().add(listItem);
            }

            result.add(ShoppingListMapper.toDTO(slRepository.save(shoppingList), productStoreRepository));
        }

        return result;
    }

    @Transactional
    public ShoppingListDTO renameList(Integer listId, String newName)
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
        list.setName(newName);
        return ShoppingListMapper.toDTO(slRepository.save(list), productStoreRepository);
    }

}
