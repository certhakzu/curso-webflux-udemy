package com.bolsadeideas.springboot.webflux.app;

import com.bolsadeideas.springboot.webflux.app.handler.ProductoHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;

import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.RequestPredicates.*;

@Configuration // con esto se pueden tener métodos para registrar Componentes de Spring usando la notación @Bean
public class RouterFunctionConf {



    //Parecido a @Component para registrar componentes de Spring, pero con Bean se registra via método. Es decir,
    //lo que retorma se registra en el contenendor
    @Bean
    public RouterFunction<ServerResponse> routes(ProductoHandler handler){
        return route(GET("/api/v2/productos").or(GET("api/v3/productos")), handler::listar)
                // el 'and' se puede usar para que valide que lo que se esta enviado por la ruta tenga un body JSON. Por ejemplo.
                .andRoute(GET("/api/v2/productos/{id}")/*.and(contentType(MediaType.APPLICATION_JSON))*/, handler::ver)
                .andRoute(POST("/api/v2/productos"), handler::crear)
                .andRoute(PUT("/api/v2/productos/{id}"), handler::editar)
                .andRoute(DELETE("/api/v2/productos/{id}"), handler::eliminar);
    }
}
