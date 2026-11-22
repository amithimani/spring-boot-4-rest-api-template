package me.dio.client.reqres;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** A user as returned by reqres.in. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReqresUser(
        Long id,
        String email,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName,
        String avatar) {
}
