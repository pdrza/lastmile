package com.lastmile.optiroute.security;

import com.lastmile.optiroute.domain.entity.Store;
import com.lastmile.optiroute.repository.StoreRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

// Carrega os dados da loja para o Spring Security verificar a autenticação
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final StoreRepository storeRepository;

    public CustomUserDetailsService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    // o Spring Security chama esse método passando o "username", que no nosso caso é o id da loja
    @Override
    public UserDetails loadUserByUsername(String storeId) throws UsernameNotFoundException {
        Store store = storeRepository.findById(UUID.fromString(storeId))
                .orElseThrow(() -> new UsernameNotFoundException("Loja não encontrada: " + storeId));

        // devolve um objeto User do Spring com id, senha e permissão
        return new User(
                store.getId().toString(),
                store.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_STORE"))
        );
    }
}
