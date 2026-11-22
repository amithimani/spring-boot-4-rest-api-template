package me.dio.client.reqres;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

/**
 * Low-level client for the reqres.in users API.
 *
 * <p>Each page request is protected by Resilience4j (instance {@value #RESILIENCE_INSTANCE},
 * configured in application.yml). With the default aspect order, {@code Retry} wraps
 * {@code CircuitBreaker}: every attempt is recorded by the circuit breaker, and once it
 * opens, calls fail immediately with {@link CallNotPermittedException} (not retried).</p>
 */
@Component
public class ReqresClient {

    public static final String RESILIENCE_INSTANCE = "reqres";

    private final RestClient restClient;

    public ReqresClient(RestClient reqresRestClient) {
        this.restClient = reqresRestClient;
    }

    /**
     * Fetches one page of users.
     *
     * @throws RestClientException       on HTTP errors, timeouts or unreadable responses
     *                                   (after retries, for transient failures)
     * @throws CallNotPermittedException when the circuit breaker is open
     */
    @Retry(name = RESILIENCE_INSTANCE)
    @CircuitBreaker(name = RESILIENCE_INSTANCE)
    public ReqresUserPage fetchUsersPage(int page) {
        return restClient.get()
                .uri(uri -> uri.path("/users").queryParam("page", page).build())
                .retrieve()
                .body(ReqresUserPage.class);
    }
}
