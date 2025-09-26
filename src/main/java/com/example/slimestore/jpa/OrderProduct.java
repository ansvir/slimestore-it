package com.example.slimestore.jpa;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single product with a specific quantity in an order.
 * This is the joining table for the many-to-many relationship between Order and Product.
 */
@Entity
@Data
@Table(name = "order_products")
@AllArgsConstructor
@NoArgsConstructor
public class OrderProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The order this item belongs to.
     */
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    /**
     * The product from the catalog.
     */
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    /**
     * The quantity of the product in the order.
     */
    private int quantity;
}