package com.example.slimestore.repository;

import com.example.slimestore.jpa.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repository for managing {@link Product} entities.
 * Provides standard CRUD operations and custom query methods.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Finds a {@link Product} by its unique name.
     *
     * @param name The name of the product to find.
     * @return An {@link Optional} containing the found product, or an empty Optional if not found.
     */
    Optional<Product> findByName(String name);
}