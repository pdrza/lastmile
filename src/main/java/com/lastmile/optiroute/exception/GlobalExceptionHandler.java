package com.lastmile.optiroute.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.stream.Collectors;

// Captura todas as exceções da aplicação e retorna um JSON padronizado (RFC 7807)
// sem isso, erros retornariam HTML ou stack trace para o cliente
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // campos inválidos no request (ex: email mal formatado, campo obrigatório vazio)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create("urn:problem:validation-error"));
        pd.setTitle("Erro de Validação");
        pd.setDetail(ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining(", ")));
        return pd;
    }

    // regra de negócio violada (ex: email já cadastrado)
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create("urn:problem:bad-request"));
        pd.setTitle("Requisição Inválida");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    // email ou senha errados no login
    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        pd.setType(URI.create("urn:problem:unauthorized"));
        pd.setTitle("Não Autorizado");
        pd.setDetail("Email ou senha inválidos");
        return pd;
    }

    // recurso não encontrado no banco (entrega, rota, loja)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setType(URI.create("urn:problem:not-found"));
        pd.setTitle("Não Encontrado");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    // tentativa de acessar recurso de outra loja
    @ExceptionHandler(ForbiddenException.class)
    public ProblemDetail handleForbidden(ForbiddenException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        pd.setType(URI.create("urn:problem:forbidden"));
        pd.setTitle("Acesso Negado");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    // estado inválido (ex: tentar otimizar sem entregas pendentes)
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        pd.setType(URI.create("urn:problem:unprocessable"));
        pd.setTitle("Operação Inválida");
        pd.setDetail(ex.getMessage());
        return pd;
    }

    // erro de comunicação com API externa (ORS geocoding ou otimização)
    @ExceptionHandler(RestClientException.class)
    public ProblemDetail handleRestClient(RestClientException ex) {
        log.error("Erro na API externa: {}", ex.getMessage(), ex);
        String detail = ex instanceof HttpClientErrorException hce
                ? "API externa retornou erro " + hce.getStatusCode() + ": " + hce.getResponseBodyAsString()
                : "Falha ao comunicar com API externa: " + ex.getMessage();
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        pd.setType(URI.create("urn:problem:external-api-error"));
        pd.setTitle("Erro na API Externa");
        pd.setDetail(detail);
        return pd;
    }

    // qualquer outro erro inesperado
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex) {
        log.error("Erro inesperado: {}", ex.getMessage(), ex);
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setType(URI.create("urn:problem:internal-error"));
        pd.setTitle("Erro Interno");
        pd.setDetail("Ocorreu um erro inesperado: " + ex.getMessage());
        return pd;
    }
}
