package com.bolsadeideas.springboot.webflux.app.controllers;

import com.bolsadeideas.springboot.webflux.app.models.documents.Producto;
import com.bolsadeideas.springboot.webflux.app.models.services.ProductoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    @Autowired
    private ProductoService service;

    @Value("${config.uploads.path}") //inyectamos la configuracion de application.properties para las iploads
    private String path;

    @PostMapping("/v2")
    public Mono<ResponseEntity<Producto>> crearConFoto(Producto producto, @RequestPart FilePart file){

        if (producto.getCreateAt() == null){
            producto.setCreateAt(new Date());
        }

        producto.setFoto(UUID.randomUUID()
                .toString()
                .concat("-")
                .concat(file.filename()
                        .replace(" ", "")
                        .replace(":", "")
                        .replace("\\", "")));

        return file.transferTo(new File(path.concat(producto.getFoto()))).then(service.save(producto))
                .map(producto1 -> ResponseEntity.created(URI.create("/api/productos/".concat(producto1.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(producto1))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/upload/{id}") // ruta para buscar el producto por el id y asignarle la foto
    public Mono<ResponseEntity<Producto>> upload(@PathVariable String id, @RequestPart FilePart file){ //para subir archivos de foto
                                                                                    // 'file' debe ser el mismo nombre del KEY en la consulta
        return service.findById(id)
                .flatMap(producto -> { //el flatMap retorna un observable
                    producto.setFoto(UUID.randomUUID()
                            .toString()
                            .concat("-")
                            .concat(file.filename()
                                    .replace(" ", "")
                                    .replace(":", "")
                                    .replace("\\", "")));
                    return file.transferTo(new File(path.concat(producto.getFoto()))).then(service.save(producto));
                }).map(producto -> ResponseEntity.ok(producto))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping
    public Mono<ResponseEntity<Flux<Producto>>> listar(){
        return Mono.just(ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(service.findAll()));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Producto>> ver(@PathVariable String id){
        return  service.findById(id)
                .map(producto -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(producto))
                .defaultIfEmpty(ResponseEntity.notFound().build()); // build() construye la respuesta sin body/contenidp
    }

    @PostMapping                            //hbilitamos la validacion con Valid
    public Mono<ResponseEntity<Map<String, Object>>> crear(@Valid @RequestBody Mono<Producto> monoProducto){

        Map<String, Object> respuesta = new HashMap<String, Object>();

        return monoProducto.flatMap(producto -> {
            if (producto.getCreateAt() == null){ // todo el IF y el RETURN seria el cuerpo de CREAR si el parámetro fuese un Producto y no un Mono<Producto>
                producto.setCreateAt(new Date());
            }

            return service.save(producto)
                    .map(producto1 -> {
                        respuesta.put("producto", producto1);
                        respuesta.put("mensaje", "Producto creado con exito");
                        respuesta.put("timestamp", new Date());
                        return ResponseEntity.created(URI.create("/api/productos/".concat(producto1.getId())))
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(respuesta);
                        }
                    );
                    //.defaultIfEmpty(ResponseEntity.notFound().build());
        }).onErrorResume(throwable -> {
            return Mono.just(throwable)
                    .cast(WebExchangeBindException.class) // convertimos a un tipo mas concreto de Excepci+on
                    .flatMap(e -> Mono.just(e.getFieldErrors()))
                    .flatMapMany(errors -> Flux.fromIterable(errors))
                    .map(fieldError -> "El campo ".concat(fieldError.getField()).concat(" ").concat(fieldError.getDefaultMessage()))
                    .collectList()
                    .flatMap(list -> {
                        respuesta.put("errors", list);
                        respuesta.put("timestamp", new Date());
                        respuesta.put("status", HttpStatus.BAD_REQUEST.value());
                        return Mono.just(ResponseEntity.badRequest().body(respuesta));
                    });
        });
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<Producto>> editar(@RequestBody Producto producto, @PathVariable String id){

        return service.findById(id)
                .flatMap(producto1 -> {
                    producto1.setNombre(producto.getNombre());
                    producto1.setPrecio(producto.getPrecio());
                    producto1.setCategoria(producto.getCategoria());

                    return service.save(producto1);
                }).map(producto1 -> ResponseEntity.created(URI.create("/api/productos/".concat(producto1.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(producto1))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> eliminar(@PathVariable String id){
        /* devolviendo un Mono<ResponseEntity<Object>>
        return service.findById(id)

                .flatMap(producto -> service.delete(producto)
                        .then(Mono.just(ResponseEntity.notFound().build())))
                .defaultIfEmpty(ResponseEntity.notFound().build());
                //.map(unused -> ResponseEntity.accepted().build()); //esto map no se ejecutaria porque el flatmap retorla un Mono<void>
        */
        //retornando un Mono<ResponseEntity<Void>>, podria ser:
        return service.findById(id)
            .flatMap(p -> service.delete(p)
                .then(Mono.just(new ResponseEntity<Void>(HttpStatus.NO_CONTENT)))
            ).defaultIfEmpty(new ResponseEntity<Void>(HttpStatus.NOT_FOUND));

    }
}
