package com.lastmile.optiroute.service;

import com.lastmile.optiroute.domain.entity.Store;
import com.lastmile.optiroute.dto.LoginRequest;
import com.lastmile.optiroute.dto.LoginResponse;
import com.lastmile.optiroute.dto.RegisterRequest;
import com.lastmile.optiroute.repository.StoreRepository;
import com.lastmile.optiroute.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Testa as regras de negócio do AuthService sem precisar subir o Spring inteiro
// @ExtendWith(MockitoExtension.class) ativa os recursos do Mockito nesta classe
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // @Mock cria uma versão "falsa" da dependência — a gente controla o que ela retorna
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private GeocodingCacheService geocodingCacheService;

    // @InjectMocks cria o AuthService real e injeta os mocks acima dentro dele
    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Registro: deve retornar token JWT quando todos os dados são válidos")
    void registrar_deveRetornarTokenQuandoDadosValidos() {
        // ARRANGE — prepara o cenário do teste
        var request = new RegisterRequest("Loja Teste", "loja@teste.com", "senha123", "Rua Teste, 1");

        // email ainda não existe no banco
        when(storeRepository.findByEmail("loja@teste.com")).thenReturn(Optional.empty());

        // geocoding vai retornar coordenadas fictícias (lon, lat)
        when(geocodingCacheService.getCoordinates("Rua Teste, 1")).thenReturn(new double[]{-43.1, -22.9});

        // quando codificar a senha, retorna um hash qualquer
        when(passwordEncoder.encode("senha123")).thenReturn("$2a$hash_bcrypt");

        // quando salvar a loja no banco, retorna uma loja com ID preenchido
        Store lojaFalsa = Store.builder()
                .id(UUID.randomUUID())
                .name("Loja Teste")
                .email("loja@teste.com")
                .password("$2a$hash_bcrypt")
                .addressText("Rua Teste, 1")
                .build();
        when(storeRepository.save(any(Store.class))).thenReturn(lojaFalsa);

        // quando gerar o token, retorna uma string qualquer
        when(jwtService.generateToken(any(Store.class))).thenReturn("token.jwt.de.teste");

        // ACT — executa o método que estamos testando
        LoginResponse resposta = authService.register(request);

        // ASSERT — verifica se o resultado está correto
        assertThat(resposta).isNotNull();
        assertThat(resposta.token()).isEqualTo("token.jwt.de.teste");

        // verifica se a loja foi salva no banco (exatamente uma vez)
        verify(storeRepository, times(1)).save(any(Store.class));

        // verifica se o geocoding foi chamado com o endereço correto
        verify(geocodingCacheService, times(1)).getCoordinates("Rua Teste, 1");
    }

    @Test
    @DisplayName("Registro: deve lançar exceção quando email já está cadastrado")
    void registrar_deveLancarExcecaoQuandoEmailJaCadastrado() {
        // ARRANGE
        var request = new RegisterRequest("Loja Teste", "duplicado@teste.com", "senha123", "Rua X, 1");

        // simula que o email já existe no banco
        Store lojaExistente = Store.builder().id(UUID.randomUUID()).email("duplicado@teste.com").build();
        when(storeRepository.findByEmail("duplicado@teste.com")).thenReturn(Optional.of(lojaExistente));

        // ACT + ASSERT — espera que uma exceção seja lançada com a mensagem certa
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicado@teste.com");

        // garante que o banco nunca foi chamado para salvar — sem email duplicado
        verify(storeRepository, never()).save(any());

        // garante que geocoding não foi chamado — não faz sentido geocodificar se vai rejeitar mesmo
        verify(geocodingCacheService, never()).getCoordinates(anyString());
    }

    @Test
    @DisplayName("Login: deve retornar token quando email e senha estão corretos")
    void login_deveRetornarTokenQuandoCredenciaisValidas() {
        // ARRANGE
        var request = new LoginRequest("loja@teste.com", "senha123");

        Store loja = Store.builder()
                .id(UUID.randomUUID())
                .email("loja@teste.com")
                .password("$2a$hash_bcrypt")
                .build();

        when(storeRepository.findByEmail("loja@teste.com")).thenReturn(Optional.of(loja));
        // passwordEncoder.matches(senhaDigitada, hashSalvo) → retorna true quando a senha bate
        when(passwordEncoder.matches("senha123", "$2a$hash_bcrypt")).thenReturn(true);
        when(jwtService.generateToken(loja)).thenReturn("token.valido");

        // ACT
        LoginResponse resposta = authService.login(request);

        // ASSERT
        assertThat(resposta.token()).isEqualTo("token.valido");
        verify(jwtService, times(1)).generateToken(loja);
    }

    @Test
    @DisplayName("Login: deve lançar exceção quando email não existe no banco")
    void login_deveLancarExcecaoQuandoEmailNaoEncontrado() {
        var request = new LoginRequest("naoexiste@teste.com", "senha123");
        when(storeRepository.findByEmail("naoexiste@teste.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        // JWT jamais gerado se nem achou a loja
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("Login: deve lançar exceção quando senha está incorreta")
    void login_deveLancarExcecaoQuandoSenhaIncorreta() {
        var request = new LoginRequest("loja@teste.com", "senha_errada");

        Store loja = Store.builder()
                .id(UUID.randomUUID())
                .email("loja@teste.com")
                .password("$2a$hash_bcrypt")
                .build();

        when(storeRepository.findByEmail("loja@teste.com")).thenReturn(Optional.of(loja));
        // senha não bate com o hash → retorna false
        when(passwordEncoder.matches("senha_errada", "$2a$hash_bcrypt")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        // garante que o JWT não foi gerado — senha errada não pode gerar token
        verify(jwtService, never()).generateToken(any());
    }
}
