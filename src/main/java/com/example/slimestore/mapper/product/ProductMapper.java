package com.example.slimestore.mapper.product;

import com.example.slimestore.jpa.Product;
import com.example.slimestore.model.product.ProductDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductDto toDto(Product product);

    @Mapping(target = "orderProducts", ignore = true)
    Product toEntity(ProductDto productDto);

}