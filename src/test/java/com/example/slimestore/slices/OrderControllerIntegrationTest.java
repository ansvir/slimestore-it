package com.example.slimestore.slices;

import com.example.slimestore.controller.OrderController;
import com.example.slimestore.jpa.Order;
import com.example.slimestore.jpa.Product;
import com.example.slimestore.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Description;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Description("given order when created then 201 expected")
    void testCreateOrderEndpoint() throws Exception {
        // GIVEN
        Order newOrder = new Order();
        newOrder.setCustomerName("Ivan Ivanov");
        newOrder.setProducts(Arrays.asList(
            createProduct("Galaxy Slime", 1),
            createProduct("Glitter Slime", 2)
        ));

        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setCustomerName(newOrder.getCustomerName());
        savedOrder.setProducts(newOrder.getProducts());
        when(orderService.createOrder(any(Order.class))).thenReturn(savedOrder);

        // WHEN & THEN
        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newOrder)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.customerName").value("Ivan Ivanov"));

        verify(orderService, times(1)).createOrder(any(Order.class));
    }

    @Test
    @Description("given order when deleted then 204 expected")
    void testDeleteOrderEndpoint() throws Exception {
        // GIVEN
        Long orderId = 1L;
        doNothing().when(orderService).deleteOrder(orderId);

        // WHEN & THEN
        mockMvc.perform(delete("/api/orders/{id}", orderId))
                .andExpect(status().isNoContent());

        verify(orderService, times(1)).deleteOrder(orderId);
    }

    @Test
    @Description("given order when found by filter then 200 expected")
    void testFindOrderByFilterEndpoint() throws Exception {
        // GIVEN
        String itemName = "Cloud Slime";
        Order order1 = new Order();
        order1.setId(1L);
        order1.setCustomerName("Bob");
        order1.setProducts(Collections.singletonList(createProduct(itemName, 2)));

        Order order2 = new Order();
        order2.setId(2L);
        order2.setCustomerName("Dave");
        order2.setProducts(Collections.singletonList(createProduct(itemName, 1)));

        when(orderService.findByProductName(itemName)).thenReturn(Arrays.asList(order1, order2));

        // WHEN & THEN
        mockMvc.perform(get("/api/orders/search")
                .param("itemName", itemName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].customerName").value("Bob"))
                .andExpect(jsonPath("$[1].customerName").value("Dave"));

        verify(orderService, times(1)).findByProductName(itemName);
    }
    
    private Product createProduct(String name, int quantity) {
        Product product = new Product();
        product.setName(name);
        product.setQuantity(quantity);
        return product;
    }
}