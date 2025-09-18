package main.java.domain.repository;

import main.java.domain.product.Product;
import main.java.domain.shared.Code;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    void upsert(Product p);
    Optional<Product> findByCode(Code code);
    List<Product> findAll();
    boolean deleteByCode(Code code);
}