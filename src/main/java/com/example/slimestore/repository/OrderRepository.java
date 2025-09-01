package com.example.slimestore.repository;

import com.example.slimestore.jpa.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByProducts_Name(String name);
}