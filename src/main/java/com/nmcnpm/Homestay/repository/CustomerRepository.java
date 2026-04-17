package com.nmcnpm.Homestay.repository;

import com.nmcnpm.Homestay.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByPhone(String phone);
    Optional<Customer> findByCccd(String cccd);
    boolean existsByPhone(String phone);
    boolean existsByCccd(String cccd);
    boolean existsByEmail(String email);
}