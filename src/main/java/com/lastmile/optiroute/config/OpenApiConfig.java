package com.lastmile.optiroute.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Configuração da documentação interativa da API (Swagger UI)
// Acesse em: http://localhost:8080/swagger-ui/index.html
@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI optiRouteOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OptiRoute API — Roteirização de Entregas")
                        .version("1.0.0")
                        .description("""
                                API de otimização de rotas de última milha (last-mile delivery).

                                Fluxo de uso:
                                1. Cadastre uma loja em POST /api/auth/register (devolve um token JWT).
                                2. Faça login em POST /api/auth/login para obter o token.
                                3. Clique em "Authorize" aqui no topo e cole o token.
                                4. Cadastre entregas e gere a rota otimizada.

                                Cada loja só enxerga os próprios dados (isolamento por token).""")
                        .contact(new Contact().name("Projeto Last Mile")))
                // exige o token JWT em todos os endpoints protegidos
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME))
                .components(new Components().addSecuritySchemes(SCHEME_NAME,
                        new SecurityScheme()
                                .name(SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Cole o token JWT devolvido pelo login (sem o prefixo 'Bearer ').")));
    }
}
