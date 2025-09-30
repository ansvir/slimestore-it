package com.example.slimestore.mapper.order;

import com.example.slimestore.jpa.Order;
import com.example.slimestore.mapper.orderproduct.OrderProductMapper;
import com.example.slimestore.model.order.OrderDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = { OrderProductMapper.class })
public interface OrderMapper {

    @Mapping(target = "orderProducts", source = "orderProducts")
    OrderDto toDto(Order order);

    @Mapping(target = "orderProducts", source = "orderProducts")
    Order toEntity(OrderDto orderDto);
}
