package com.example.slimestore.mapper.orderproduct;

import com.example.slimestore.jpa.OrderProduct;
import com.example.slimestore.mapper.product.ProductMapper;
import com.example.slimestore.model.orderproduct.OrderProductDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", uses = { ProductMapper.class })
public interface OrderProductMapper {

    @Mapping(target = "product", source = "product")
    OrderProductDto toDto(OrderProduct orderProduct);

    @Mapping(target = "order", ignore = true)
    @Mapping(target = "product", source = "product")
    OrderProduct toEntity(OrderProductDto orderProductDto);
}
