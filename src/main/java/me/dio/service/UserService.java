package me.dio.service;

import java.util.List;

import me.dio.client.reqres.ReqresUser;

public interface UserService {

    /**
     * Fetches the users from the first pages of reqres.in (2 by default, see
     * {@code reqres.pages}), merged in page order.
     *
     * @throws me.dio.exception.ExternalServiceUnavailableException when reqres.in is down,
     *         rate limiting, timing out, or its circuit breaker is open
     * @throws me.dio.exception.ExternalServiceException when reqres.in rejects the request
     *         or returns an invalid response
     */
    List<ReqresUser> findUsers();
}
