package me.dio.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import me.dio.controller.dto.HeroCreateRequest;
import me.dio.controller.dto.HeroResponse;
import me.dio.controller.dto.HeroUpdateRequest;
import me.dio.model.Hero;
import me.dio.service.HeroService;

@RestController
@RequestMapping("/heroes")
@Tag(name = "Heroes Controller", description = "RESTful API for managing heroes.")
public record HeroController(HeroService heroService) {

    @GetMapping
    @Operation(summary = "Get all heroes", description = "Get a list of all registered heroes, sorted by XP (highest first)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation")
    })
    public ResponseEntity<List<HeroResponse>> findAll() {
        return ResponseEntity.ok(heroService.findAll().stream().map(HeroResponse::from).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a hero by ID", description = "Get a specific hero based on its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation"),
            @ApiResponse(responseCode = "404", description = "Hero not found")
    })
    public ResponseEntity<HeroResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(HeroResponse.from(heroService.findById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a new hero", description = "Create a new hero and returns the created hero")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Hero created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid hero data provided")
    })
    public ResponseEntity<HeroResponse> create(@Valid @RequestBody HeroCreateRequest request) {
        Hero createdHero = heroService.create(request.toModel());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdHero.getId())
                .toUri();
        return ResponseEntity.created(location).body(HeroResponse.from(createdHero));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a hero", description = "Update the name of an existing hero based on its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hero updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid hero data provided"),
            @ApiResponse(responseCode = "404", description = "Hero not found"),
            @ApiResponse(responseCode = "409", description = "Hero was modified concurrently (stale version)"),
            @ApiResponse(responseCode = "422", description = "Body ID does not match the path ID")
    })
    public ResponseEntity<HeroResponse> update(@PathVariable Long id, @Valid @RequestBody HeroUpdateRequest request) {
        return ResponseEntity.ok(HeroResponse.from(heroService.update(id, request.toModel())));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a hero", description = "Delete an existing hero based on its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Hero deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Hero not found")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        heroService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/up")
    @Operation(summary = "Increase hero XP", description = "Up XP to an existing hero based on its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Vote registered successfully"),
            @ApiResponse(responseCode = "404", description = "Hero not found")
    })
    public ResponseEntity<Void> vote(@PathVariable Long id) {
        heroService.increaseXp(id);
        return ResponseEntity.noContent().build();
    }
}
