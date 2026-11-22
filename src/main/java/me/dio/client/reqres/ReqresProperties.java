package me.dio.client.reqres;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Settings for the reqres.in client ({@code reqres.*} in application.yml).
 *
 * @param baseUrl        API base URL, e.g. {@code https://reqres.in/api}
 * @param apiKey         optional API key sent as {@code x-api-key}; without one the shared
 *                       demo quota (low daily limit) applies
 * @param pages          how many pages of users to fetch
 * @param connectTimeout TCP connect timeout
 * @param readTimeout    maximum time to wait for a response
 */
@Validated
@ConfigurationProperties("reqres")
public record ReqresProperties(
        @NotNull @DefaultValue("https://reqres.in/api") URI baseUrl,
        String apiKey,
        @Min(1) @DefaultValue("2") int pages,
        @NotNull @DefaultValue("2s") Duration connectTimeout,
        @NotNull @DefaultValue("3s") Duration readTimeout) {
}
