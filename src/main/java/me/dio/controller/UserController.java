package me.dio.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import me.dio.controller.dto.UserResponse;
import me.dio.service.UserService;

@RestController
@RequestMapping("/users")
@Tag(name = "Users Controller", description = "Users fetched from the external reqres.in API.")
public record UserController(UserService userService) {

    @GetMapping
    @Operation(summary = "Get users from reqres.in",
            description = "Fetches and merges the first 2 pages of users from https://reqres.in/api/users. "
                    + "Calls are protected by retries and a circuit breaker.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation"),
            @ApiResponse(responseCode = "502", description = "reqres.in rejected the request or returned an invalid response"),
            @ApiResponse(responseCode = "503", description = "reqres.in is unavailable or the circuit breaker is open (see Retry-After)")
    })
    public ResponseEntity<List<UserResponse>> findAll() {
        return ResponseEntity.ok(userService.findUsers().stream().map(UserResponse::from).toList());
    }
}
