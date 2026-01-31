package com.smartcart.smartcart.modules.category.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.smartcart.smartcart.modules.category.entity.Category;
import com.smartcart.smartcart.modules.category.repository.CategoryRepository;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> findAll() { return categoryRepository.findAll(); }

    public Category findById(Integer id) {
        return categoryRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
    }

    public Category create(String name, String description) {
        Category c = new Category();
        c.setName(name);
        c.setDescription(description);
        return categoryRepository.save(c);
    }

    public Category update(Integer id, String name, String description) {
        Category c = findById(id);
        if (name != null) c.setName(name);
        if (description != null) c.setDescription(description);
        return categoryRepository.save(c);
    }

    public Boolean delete(Integer id) {
        categoryRepository.deleteById(id);
        return true;
    }
}