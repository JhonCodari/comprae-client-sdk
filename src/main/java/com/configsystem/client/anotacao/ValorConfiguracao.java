package com.configsystem.client.anotacao;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação para injeção automática de valores de configuração
 * 
 * Uso:
 * @ValorConfiguracao("minha.configuracao.chave")
 * private String minhaConfiguracao;
 * 
 * @ValorConfiguracao(value = "app.timeout", defaultValue = "5000")
 * private int timeout;
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValorConfiguracao {
    
    /**
     * Chave da configuração a ser injetada
     */
    String value();
    
    /**
     * Valor padrão caso a configuração não seja encontrada
     */
    String defaultValue() default "";
    
    /**
     * Namespace da configuração (opcional)
     * Se não especificado, usa o namespace padrão do cliente
     */
    String namespace() default "";
    
    /**
     * Ambiente da configuração (opcional)
     * Se não especificado, usa o ambiente padrão do cliente
     */
    String environment() default "";
    
    /**
     * Indica se a configuração é obrigatória
     * Se true e a configuração não for encontrada, uma exceção será lançada
     */
    boolean required() default false;
    
    /**
     * Indica se o valor deve ser atualizado automaticamente quando a configuração mudar
     */
    boolean refreshable() default false;
}
