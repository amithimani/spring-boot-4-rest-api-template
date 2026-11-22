package me.dio.controller;

import static me.dio.client.reqres.FakeReqresServer.Reply.json;
import static me.dio.client.reqres.FakeReqresServer.Reply.status;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import me.dio.client.reqres.FakeReqresServer;
import me.dio.client.reqres.ReqresClient;

/**
 * Tests {@code GET /users} end to end against a fake reqres.in, including retries,
 * timeouts, circuit breaking and error mapping.
 */
@SpringBootTest(properties = {
        "reqres.api-key=test-key",
        "reqres.read-timeout=300ms",
        "resilience4j.retry.instances.reqres.wait-duration=10ms",
        "resilience4j.retry.instances.reqres.enable-exponential-backoff=false",
        "resilience4j.circuitbreaker.instances.reqres.sliding-window-size=4",
        "resilience4j.circuitbreaker.instances.reqres.minimum-number-of-calls=4",
        "resilience4j.circuitbreaker.instances.reqres.wait-duration-in-open-state=60s",
})
@AutoConfigureMockMvc
class UserControllerIT {

    private static final FakeReqresServer reqres = FakeReqresServer.start();

    @DynamicPropertySource
    static void reqresUrl(DynamicPropertyRegistry registry) {
        registry.add("reqres.base-url", reqres::baseUrl);
    }

    @AfterAll
    static void stopServer() {
        reqres.close();
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CircuitBreakerRegistry circuitBreakers;

    private CircuitBreaker circuitBreaker() {
        return circuitBreakers.circuitBreaker(ReqresClient.RESILIENCE_INSTANCE);
    }

    @BeforeEach
    void resetCircuitBreaker() {
        circuitBreaker().reset();
    }

    private static String page(int page, int totalPages, long... ids) {
        StringBuilder data = new StringBuilder();
        for (long id : ids) {
            if (!data.isEmpty()) {
                data.append(',');
            }
            data.append("""
                    {"id":%d,"email":"user%d@reqres.in","first_name":"First%d","last_name":"Last%d",\
                    "avatar":"https://reqres.in/img/faces/%d-image.jpg"}""".formatted(id, id, id, id, id));
        }
        return """
                {"page":%d,"per_page":%d,"total":%d,"total_pages":%d,"data":[%s],"support":{"url":"x"}}"""
                .formatted(page, ids.length, ids.length * totalPages, totalPages, data);
    }

    @Test
    void returnsUsersFromTheFirstTwoPagesOnly() throws Exception {
        reqres.respondWith((page, attempt) -> json(page(page, 3, page * 10 + 1, page * 10 + 2)));

        mvc.perform(get("/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4))
            .andExpect(jsonPath("$[0].id").value(11))
            .andExpect(jsonPath("$[0].email").value("user11@reqres.in"))
            .andExpect(jsonPath("$[0].firstName").value("First11"))
            .andExpect(jsonPath("$[0].lastName").value("Last11"))
            .andExpect(jsonPath("$[0].avatar").value("https://reqres.in/img/faces/11-image.jpg"))
            .andExpect(jsonPath("$[3].id").value(22));

        assertThat(reqres.hits(1)).isEqualTo(1);
        assertThat(reqres.hits(2)).isEqualTo(1);
        assertThat(reqres.hits(3)).isZero();
        assertThat(reqres.apiKeys()).containsOnly("test-key");
    }

    @Test
    void stopsEarlyWhenFewerPagesExist() throws Exception {
        reqres.respondWith((page, attempt) -> json(page(page, 1, 1, 2, 3)));

        mvc.perform(get("/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3));

        assertThat(reqres.hits(2)).isZero();
    }

    @Test
    void retriesTransientServerErrors() throws Exception {
        reqres.respondWith((page, attempt) -> page == 1 && attempt == 1 ? status(500) : json(page(page, 2, page)));

        mvc.perform(get("/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));

        assertThat(reqres.hits(1)).isEqualTo(2);
    }

    @Test
    void persistentServerErrorsReturn503AfterRetries() throws Exception {
        reqres.respondWith((page, attempt) -> status(503));

        mvc.perform(get("/users"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.title").value("Upstream service unavailable"))
            .andExpect(header().doesNotExist("Retry-After"));

        assertThat(reqres.hits(1)).isEqualTo(3);
        assertThat(reqres.hits(2)).isZero();
    }

    @Test
    void timeoutsAreRetriedThenReturn503() throws Exception {
        reqres.respondWith((page, attempt) -> json(page(page, 2, page)).delayedBy(Duration.ofSeconds(1)));

        mvc.perform(get("/users"))
            .andExpect(status().isServiceUnavailable());

        assertThat(reqres.hits(1)).isEqualTo(3);
    }

    @Test
    void failureOnSecondPageFailsTheWholeRequest() throws Exception {
        reqres.respondWith((page, attempt) -> page == 2 ? status(500) : json(page(page, 2, page)));

        mvc.perform(get("/users"))
            .andExpect(status().isServiceUnavailable());

        assertThat(reqres.hits(2)).isEqualTo(3);
    }

    @Test
    void rateLimitingReturns503WithUpstreamRetryAfter() throws Exception {
        reqres.respondWith((page, attempt) -> status(429).withHeader("Retry-After", "7"));

        mvc.perform(get("/users"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(header().string("Retry-After", "7"));

        assertThat(reqres.hits(1)).isEqualTo(3);
    }

    @Test
    void clientErrorsReturn502WithoutRetrying() throws Exception {
        reqres.respondWith((page, attempt) -> status(401));

        mvc.perform(get("/users"))
            .andExpect(status().isBadGateway())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.detail").value("The users provider rejected the request."));

        assertThat(reqres.hits(1)).isEqualTo(1);
        assertThat(circuitBreaker().getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void malformedResponseReturns502() throws Exception {
        reqres.respondWith((page, attempt) -> json("this is not json"));

        mvc.perform(get("/users"))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.detail").value("The users provider returned an invalid response."));

        assertThat(reqres.hits(1)).isEqualTo(1);
    }

    @Test
    void openCircuitFailsFastWith503AndRetryAfter() throws Exception {
        reqres.respondWith((page, attempt) -> status(500));

        // 1st request: 3 failed attempts. 2nd: the 4th failure opens the circuit (4/4 = 100%).
        mvc.perform(get("/users")).andExpect(status().isServiceUnavailable());
        mvc.perform(get("/users")).andExpect(status().isServiceUnavailable());
        assertThat(circuitBreaker().getState()).isEqualTo(CircuitBreaker.State.OPEN);

        int hitsBefore = reqres.totalHits();
        mvc.perform(get("/users"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(header().string("Retry-After", "60"))
            .andExpect(jsonPath("$.detail").value("The users service is temporarily unavailable. Please try again later."));

        assertThat(reqres.totalHits()).as("open circuit must not call upstream").isEqualTo(hitsBefore);
    }
}
