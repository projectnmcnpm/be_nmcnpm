package com.nmcnpm.Homestay.repository;

import com.nmcnpm.Homestay.entity.Account;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository {
    Optional<Account> findByEmail(String email);
}
