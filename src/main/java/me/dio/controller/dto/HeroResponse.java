package me.dio.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import me.dio.model.Hero;

@Schema(description = "A registered hero")
public record HeroResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Batman") String name,
        @Schema(description = "Experience points", example = "42") int xp,
        @Schema(description = "Version used for optimistic locking", example = "3") Integer version) {

    public static HeroResponse from(Hero hero) {
        return new HeroResponse(hero.getId(), hero.getName(), hero.getXp(), hero.getVersion());
    }
}
