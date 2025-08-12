package de.saarland.events.service;

import de.saarland.events.model.Category;
import de.saarland.events.repository.CategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }


    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }


    @Transactional(readOnly = true)
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    public Category saveCategory(Category category) {

        Optional<Category> existingCategory = categoryRepository.findByName(category.getName());
        if (existingCategory.isPresent()) {

            throw new IllegalArgumentException("Category with name '" + category.getName() + "' already exists.");
        }
        return categoryRepository.save(category);
    }


    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
    @Transactional
    public Category updateCategory(Long id, Category categoryDetails) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category with ID " + id + " not found"));

        existingCategory.setName(categoryDetails.getName());
        existingCategory.setDescription(categoryDetails.getDescription());

        return categoryRepository.save(existingCategory);
    }
}