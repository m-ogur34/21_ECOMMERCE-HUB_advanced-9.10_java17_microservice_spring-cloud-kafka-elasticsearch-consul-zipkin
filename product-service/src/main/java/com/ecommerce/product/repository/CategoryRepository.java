package com.ecommerce.product.repository;

import com.ecommerce.product.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Kategori veritabanı erişim katmanı */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);
    boolean existsByName(String name);

    /** Kök kategoriler — parent = null olanlar */
    @Query("SELECT c FROM Category c WHERE c.parent IS NULL")
    List<Category> findRootCategories();

    /** Belirli bir üst kategorinin alt kategorileri */
    List<Category> findByParentId(Long parentId);
}
