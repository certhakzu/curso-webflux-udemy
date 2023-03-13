package com.bolsadeideas.springboot.webflux.app.handler;

import com.bolsadeideas.springboot.webflux.app.models.documents.Producto;
import com.bolsadeideas.springboot.webflux.app.models.services.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Date;

@Component
public class ProductoHandler {

    @Autowired
    private ProductoService service;

    public Mono<ServerResponse> listar(ServerRequest request){

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(service.findAll(), Producto.class);
    }

    public Mono<ServerResponse> ver(ServerRequest request){

        String id = request.pathVariable("id");

        return service.findById(id)
                .flatMap(producto -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromObject(producto))
                        .switchIfEmpty(ServerResponse.notFound().build()));

        /* Forma sencilla

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(service.findById(id), Producto.class)
                .switchIfEmpty(ServerResponse.notFound().build());*/
    }

    public Mono<ServerResponse> crear(ServerRequest request){

        /*Producto producto = */
        return request.bodyToMono(Producto.class)
                //Se usa flatMap para poder emitir el nuevo flujo de Producto
                .flatMap(producto -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(service.save(producto), Producto.class));
    }

    public Mono<ServerResponse> editar(ServerRequest request){
        Mono<Producto> producto = request.bodyToMono(Producto.class);
        String id = request.pathVariable("id");
        Mono<Producto> productoDb = service.findById(id);

        return productoDb.zipWith(producto, (db, req) -> {
            db.setNombre(req.getNombre());
            db.setPrecio(req.getPrecio());
            db.setCategoria(req.getCategoria());
            return db;
        }).flatMap(p  -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(service.save(p), Producto.class)) //.body() acepta Mono o Flux, y un Observable o Publisher
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> eliminar(ServerRequest request){
        String id = request.pathVariable("id");
        Mono<Producto> producto = service.findById(id);

        return producto.flatMap(producto1 -> service.delete(producto1)
                .then(ServerResponse.noContent().build())// si lo encuentra, lo elimina y manda una respuesta sin cuerpo (No Content)
                .switchIfEmpty(ServerResponse.notFound().build()) //si no lo encuentra, manda un Not Found
        );
    }


}
