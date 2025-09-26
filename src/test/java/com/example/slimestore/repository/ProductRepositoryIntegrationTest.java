package com.example.slimestore.repository;

import com.example.slimestore.jpa.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Description;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// tests product repository with real postgre container
@DataJpaTest
@Testcontainers
class ProductRepositoryIntegrationTest {

    private static final String POSTGRES_IMAGE = "postgres:13";

    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private ProductRepository productRepository;

    @Test
    @Description("given a product when saved then it can be found by name")
    void givenProduct_whenFindByName_thenProductIsFound() {
        // GIVEN
        Product product = new Product();
        product.setName("Galaxy Slime");
        productRepository.save(product);

        // WHEN
        Optional<Product> foundProduct = productRepository.findByName("Galaxy Slime");

        // THEN
        assertThat(foundProduct).isPresent();
        assertThat(foundProduct.get().getName()).isEqualTo("Galaxy Slime");
    }

    @Test
    @Description("given no product with a name when find by name then no product is found")
    void givenNoProduct_whenFindByName_thenOptionalIsEmpty() {
        // GIVEN && WHEN
        Optional<Product> foundProduct = productRepository.findByName("Non-existent Slime");

        // THEN
        assertThat(foundProduct).isEmpty();
    }

    @Test
    @Description("given multiple products when find by name then only one product is found")
    void givenMultipleProducts_whenFindByName_thenOnlyOneProductIsFound() {
        // GIVEN
        List<Product> products = List.of(createProduct("Galaxy Slime"),
                createProduct("Glitter Slime"));
        productRepository.saveAll(products);

        // WHEN
        Optional<Product> foundProduct = productRepository.findByName("Glitter Slime");

        // THEN
        assertThat(foundProduct).isPresent();
        assertThat(foundProduct.get().getName()).isEqualTo("Glitter Slime");
    }

    private Product createProduct(String name) {
        Product product = new Product();
        product.setName(name);
        return product;
    }
}