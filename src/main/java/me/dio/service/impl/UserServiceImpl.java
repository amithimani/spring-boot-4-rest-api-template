package me.dio.service.impl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import me.dio.client.reqres.ReqresClient;
import me.dio.client.reqres.ReqresProperties;
import me.dio.client.reqres.ReqresUser;
import me.dio.client.reqres.ReqresUserPage;
import me.dio.exception.ExternalServiceException;
import me.dio.exception.ExternalServiceUnavailableException;
import me.dio.service.UserService;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private static final String UNAVAILABLE_MESSAGE =
            "The users service is temporarily unavailable. Please try again later.";

    private final ReqresClient client;
    private final int pages;
    private final Duration circuitOpenDuration;

    public UserServiceImpl(ReqresClient client, ReqresProperties properties,
            @Value("${resilience4j.circuitbreaker.instances.reqres.wait-duration-in-open-state:30s}")
            Duration circuitOpenDuration) {
        this.client = client;
        this.pages = properties.pages();
        this.circuitOpenDuration = circuitOpenDuration;
    }

    @Override
    public List<ReqresUser> findUsers() {
        long start = System.nanoTime();
        List<ReqresUser> users = new ArrayList<>();
        int page = 1;
        try {
            for (; page <= pages; page++) {
                ReqresUserPage result = client.fetchUsersPage(page);
                if (result == null || result.data() == null) {
                    throw new ExternalServiceException(
                            "reqres.in returned an empty response for page " + page, null);
                }
                users.addAll(result.data());
                if (page >= result.totalPages()) {
                    log.debug("reqres.in has only {} page(s); stopping early", result.totalPages());
                    break;
                }
            }
        } catch (CallNotPermittedException ex) {
            log.warn("reqres.in circuit breaker is open; failing fast without calling upstream");
            throw new ExternalServiceUnavailableException(UNAVAILABLE_MESSAGE, circuitOpenDuration, ex);
        } catch (HttpClientErrorException.TooManyRequests ex) {
            log.warn("reqres.in is rate limiting us (HTTP 429) on page {}", page);
            throw new ExternalServiceUnavailableException(UNAVAILABLE_MESSAGE, retryAfterOf(ex), ex);
        } catch (HttpServerErrorException | ResourceAccessException ex) {
            log.error("reqres.in unavailable on page {} after retries: {}", page, ex.toString());
            throw new ExternalServiceUnavailableException(UNAVAILABLE_MESSAGE, null, ex);
        } catch (HttpClientErrorException ex) {
            // 4xx other than 429 means our request/credentials are wrong: retrying will not help.
            log.error("reqres.in rejected request for page {} with HTTP {}", page, ex.getStatusCode().value());
            throw new ExternalServiceException("The users provider rejected the request.", ex);
        } catch (RestClientException ex) {
            log.error("reqres.in returned an unreadable response for page {}: {}", page, ex.toString());
            throw new ExternalServiceException("The users provider returned an invalid response.", ex);
        }

        log.info("Fetched {} users from reqres.in in {} ms", users.size(), (System.nanoTime() - start) / 1_000_000);
        return users;
    }

    private static Duration retryAfterOf(HttpClientErrorException ex) {
        String header = ex.getResponseHeaders() == null ? null
                : ex.getResponseHeaders().getFirst(HttpHeaders.RETRY_AFTER);
        if (header != null) {
            try {
                return Duration.ofSeconds(Long.parseLong(header.trim()));
            } catch (NumberFormatException ignored) {
                // HTTP-date form or garbage: fall through to "unknown".
            }
        }
        return null;
    }
}
