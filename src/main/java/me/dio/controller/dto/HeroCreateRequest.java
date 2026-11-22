package me.dio.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import me.dio.model.Hero;

/**
 * Payload for creating a hero. Only the name can be chosen by clients:
 * ID, XP and version are always controlled by the server.
 */
@Schema(description = "Data required to create a hero")
public record HeroCreateRequest(
        @Schema(description = "Hero name", example = "Batman")
        @NotBlank @Size(max = 255) String name) {

    public Hero toModel() {
        Hero hero = new Hero();
        hero.setName(name);
        return hero;
    }
}
