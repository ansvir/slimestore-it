package com.example.slimestore.model.order;

import com.example.slimestore.model.orderproduct.OrderProductDto;
import lombok.Data;

import java.util.List;

@Data
public class OrderDto {
    private Long id;
    private String customerName;
    private List<OrderProductDto> orderProducts;
}