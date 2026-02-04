package com.smartcart.smartcart.modules.category.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartcart.smartcart.modules.category.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

}
