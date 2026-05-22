package com.lastmile.optiroute.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// Fallback da Single Page Application (SPA React).
//
// O React Router controla as rotas no navegador (BrowserRouter). Quando o usuário
// digita a URL direto ou aperta F5 em /dashboard, o navegador pede essa rota AO
// SERVIDOR — que não a conhece e devolveria 404. Aqui encaminhamos essas rotas
// para o index.html, e o React assume a navegação a partir daí.
//
// IMPORTANTE: mapeamento explícito das rotas declaradas em frontend/src/main.jsx.
// Ao adicionar uma rota nova no React, adicione-a também aqui.
@Controller
public class SpaForwardController {

    @GetMapping({"/", "/login", "/dashboard"})
    public String spa() {
        return "forward:/index.html";
    }
}
