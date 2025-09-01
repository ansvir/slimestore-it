package com.example.slimestore;

import com.example.slimestore.jpa.Order;
import com.example.slimestore.jpa.Product;
import com.example.slimestore.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Description;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderRepositoryIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @Description("given order with five items when create new order then order saved to db")
    void testCreateOrderWithFiveItems() {
        // GIVEN
        Order order = new Order();
        order.setCustomerName("Ivan Ivanov");

        List<Product> items = Arrays.asList(
                createPosition("Galaxy Slime", 1),
                createPosition("Glitter Slime", 2),
                createPosition("Cloud Slime", 1),
                createPosition("Fluffy Slime", 3),
                createPosition("Butter Slime", 1)
        );
        order.setProducts(items);

        // WHEN
        ResponseEntity<Order> response = restTemplate.postForEntity("/api/orders", order, Order.class);

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();

        Order createdOrder = response.getBody();

        assertNotNull(createdOrder);
        assertThat(createdOrder.getId()).isNotNull();
        assertThat(createdOrder.getProducts().size()).isEqualTo(items.size());
        assertThat(createdOrder.getProducts().getFirst().getName()).isEqualTo("Galaxy Slime");
    }

    private Product createPosition(String itemName, int quantity) {
        Product product = new Product();
        product.setName(itemName);
        product.setQuantity(quantity);
        return product;
    }

    @Test
    @Description("given inactive order when delete order then no order in db")
    void testDeleteInactiveOrder() {
        // GIVEN
        Order inactiveOrder = new Order();
        inactiveOrder.setCustomerName("Cancelled Customer");
        inactiveOrder.setProducts(Collections.emptyList());
        Order savedOrder = orderRepository.save(inactiveOrder);
        Long orderId = savedOrder.getId();

        // WHEN
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/orders/" + orderId, HttpMethod.DELETE, null, Void.class
        );

        // THEN
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        ResponseEntity<Order> getResponse = restTemplate.getForEntity(
                "/api/orders/" + orderId, Order.class
        );
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Description("given order filter when apply filter then specific orders found")
    void testFindOrderByFilter() {
        // GIVEN
        orderRepository.save(createTestOrder("Alice", "Galaxy Slime", 1));
        orderRepository.save(createTestOrder("Bob", "Cloud Slime", 2));
        orderRepository.save(createTestOrder("Charlie", "Fluffy Slime", 3));
        orderRepository.save(createTestOrder("Dave", "Cloud Slime", 1));

        // WHEN
        ResponseEntity<List> response = restTemplate.getForEntity(
                "/api/orders/search?itemName=Cloud Slime", List.class
        );

        // THEN
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertNotNull(response.getBody());
        assertThat(response.getBody().size()).isEqualTo(2);
    }

    private Order createTestOrder(String customerName, String itemName, int quantity) {
        Order order = new Order();
        order.setCustomerName(customerName);
        Product position = new Product();
        position.setName(itemName);
        position.setQuantity(quantity);
        order.setProducts(Collections.singletonList(position));
        return order;
    }

}
