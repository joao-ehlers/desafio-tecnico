package com.irrah.desafio_tecnico.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bcbOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Big Chat Brasil — API")
                .version("1.0.0")
                .description("""
                        API do desafio BCB com cobrança pré/pós-paga, fila priorizada
                        e processamento assíncrono simulado. Identificação simplificada
                        pelo header X-Client-Document nos endpoints de mensagens e conversas.
                        Não há token JWT ou comprovação de identidade pelo documento.
                        Entrega e leitura são confirmações simuladas.
                        """));
    }
}
