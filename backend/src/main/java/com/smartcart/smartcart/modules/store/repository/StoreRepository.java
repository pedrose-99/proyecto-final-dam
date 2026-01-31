package com.smartcart.smartcart.modules.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcart.smartcart.modules.store.entity.Store;

public interface StoreRepository extends JpaRepository<Store, Integer> {
}
