package com.nmcnpm.Homestay.security;

import com.nmcnpm.Homestay.entity.Account;
import com.nmcnpm.Homestay.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        String authority = "ROLE_" + account.getRole().name();

        return User.builder()
                .username(account.getEmail())
                .password(account.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority(authority)))
                .accountLocked(account.getStatus().name().equals("DISABLED"))
                .build();
    }
}
