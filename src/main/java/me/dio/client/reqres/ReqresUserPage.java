package me.dio.client.reqres;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** One page of {@code GET /api/users?page=N} from reqres.in. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReqresUserPage(
        int page,
        @JsonProperty("per_page") int perPage,
        int total,
        @JsonProperty("total_pages") int totalPages,
        List<ReqresUser> data) {
}
