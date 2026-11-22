package me.dio.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import me.dio.model.Hero;

/**
 * Payload for updating a hero. Only the name is updatable.
 *
 * @param id      optional; when present it must match the ID in the URL
 * @param name    the new hero name
 * @param version optional; when present the update is rejected with 409 if the
 *                hero was modified since this version was read (optimistic locking)
 */
@Schema(description = "Data used to update a hero")
public record HeroUpdateRequest(
        @Schema(description = "Hero ID (optional, must match the path ID)", example = "1")
        Long id,
        @Schema(description = "New hero name", example = "Bruce Wayne")
        @NotBlank @Size(max = 255) String name,
        @Schema(description = "Last known version (optional, enables optimistic locking)", example = "0")
        Integer version) {

    public Hero toModel() {
        Hero hero = new Hero();
        hero.setId(id);
        hero.setName(name);
        hero.setVersion(version);
        return hero;
    }
}
