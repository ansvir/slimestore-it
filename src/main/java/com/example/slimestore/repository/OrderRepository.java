package com.example.slimestore.repository;

import com.example.slimestore.jpa.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    /**
     * Finds orders that contain a specific product by name.
     * This query traverses the relationships from Order -> OrderItem -> Product.
     *
     * @param productName The name of the product to search for.
     * @return A list of orders matching the search criteria.
     */
    List<Order> findByOrderProducts_Product_Name(String productName);
}