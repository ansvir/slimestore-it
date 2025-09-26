package com.example.slimestore.model.orderproduct;

import com.example.slimestore.model.product.ProductDto;
import lombok.Data;

@Data
public class OrderProductDto {
    private Long id;
    private ProductDto product;
    private int quantity;
}