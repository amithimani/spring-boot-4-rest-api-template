package me.dio.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import me.dio.client.reqres.ReqresUser;

@Schema(description = "A user provided by reqres.in")
public record UserResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "george.bluth@reqres.in") String email,
        @Schema(example = "George") String firstName,
        @Schema(example = "Bluth") String lastName,
        @Schema(example = "https://reqres.in/img/faces/1-image.jpg") String avatar) {

    public static UserResponse from(ReqresUser user) {
        return new UserResponse(user.id(), user.email(), user.firstName(), user.lastName(), user.avatar());
    }
}
