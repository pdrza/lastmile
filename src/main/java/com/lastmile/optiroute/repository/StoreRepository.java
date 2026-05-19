package com.lastmile.optiroute.repository;

import com.lastmile.optiroute.domain.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

// Interface de acesso ao banco para as lojas
// o JPA gera o SQL automaticamente a partir dos nomes dos métodos
public interface StoreRepository extends JpaRepository<Store, UUID> {

    // busca loja pelo email — usado no login e para checar duplicatas no cadastro
    Optional<Store> findByEmail(String email);
}
