package com.example.slimestore.controller;

import com.example.slimestore.jpa.Order;
import com.example.slimestore.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

/**
 * Controller for managing slime orders.
 */
@Tag(name = "Order Management", description = "Endpoints for creating, retrieving, and deleting orders.")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Creates a new slime order.
     * @param order The order details.
     * @return The created order with a 201 Created status.
     */
    @Operation(summary = "Create a new order", description = "Adds a new order to the SlimeStore.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        Order newOrder = orderService.createOrder(order);
        return new ResponseEntity<>(newOrder, HttpStatus.CREATED);
    }

    /**
     * Deletes an order by its ID.
     * @param id The ID of the order to delete.
     * @return A 204 No Content status.
     */
    @Operation(summary = "Delete an order", description = "Deletes an existing order by its unique ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Order successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Retrieves an order by its ID.
     * @param id The ID of the order to retrieve.
     * @return The order with a 200 OK status, or 404 if not found.
     */
    @Operation(summary = "Get an order by ID", description = "Retrieves a specific order based on its unique ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order found"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        Optional<Order> order = orderService.getOrderById(id);
        return order.map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * Searches for orders by a specific product name.
     * @param itemName The name of the product to filter by.
     * @return A list of orders containing the specified product.
     */
    @Operation(summary = "Search for orders", description = "Finds orders that contain a specific product.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders found"),
            @ApiResponse(responseCode = "400", description = "Invalid search parameter")
    })
    @GetMapping("/search")
    public ResponseEntity<List<Order>> findByItemName(@RequestParam String itemName) {
        List<Order> orders = orderService.findByProductName(itemName);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }
}