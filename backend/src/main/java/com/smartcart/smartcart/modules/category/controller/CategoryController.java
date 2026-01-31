package com.smartcart.smartcart.modules.category.controller;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.smartcart.smartcart.modules.category.entity.Category;
import com.smartcart.smartcart.modules.category.service.CategoryService;

@Controller
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @QueryMapping
    public List<Category> allCategories() {
        return categoryService.findAll();
    }

    @QueryMapping
    public Category categoryById(@Argument Integer id) {
        return categoryService.findById(id);
    }

    @MutationMapping
    public Category createCategory(@Argument String name, @Argument String description) {
        return categoryService.create(name, description);
    }

    @MutationMapping
    public Category updateCategory(@Argument Integer id, @Argument String name, @Argument String description) {
        return categoryService.update(id, name, description);
    }

    @MutationMapping
    public Boolean deleteCategory(@Argument Integer id) {
        return categoryService.delete(id);
    }
}