package me.dio.client.reqres;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * A tiny real HTTP server standing in for reqres.in, so tests exercise actual
 * timeouts, retries and circuit-breaker behaviour.
 */
public final class FakeReqresServer implements AutoCloseable {

    /** A canned response; {@code delay} simulates a slow upstream. */
    public record Reply(int status, String body, Map<String, String> headers, Duration delay) {
        public static Reply json(String body) {
            return new Reply(200, body, Map.of(), Duration.ZERO);
        }

        public static Reply status(int status) {
            return new Reply(status, "{}", Map.of(), Duration.ZERO);
        }

        public Reply withHeader(String name, String value) {
            return new Reply(status, body, Map.of(name, value), delay);
        }

        public Reply delayedBy(Duration delay) {
            return new Reply(status, body, headers, delay);
        }
    }

    /** Decides the reply for a request to {@code page}; {@code attempt} counts from 1 per page. */
    @FunctionalInterface
    public interface Responder {
        Reply reply(int page, int attempt);
    }

    private final HttpServer server;
    private final Map<Integer, AtomicInteger> hitsByPage = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<String> apiKeys = new ConcurrentLinkedQueue<>();
    private volatile Responder responder = (page, attempt) -> Reply.status(404);

    private FakeReqresServer(HttpServer server) {
        this.server = server;
        server.createContext("/api/users", this::handle);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
    }

    public static FakeReqresServer start() {
        try {
            return new FakeReqresServer(HttpServer.create(new InetSocketAddress("localhost", 0), 0));
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort() + "/api";
    }

    /** Sets how each page is answered and clears recorded requests. */
    public void respondWith(Responder responder) {
        this.responder = responder;
        hitsByPage.clear();
        apiKeys.clear();
    }

    public int hits(int page) {
        AtomicInteger hits = hitsByPage.get(page);
        return hits == null ? 0 : hits.get();
    }

    public int totalHits() {
        return hitsByPage.values().stream().mapToInt(AtomicInteger::get).sum();
    }

    public ConcurrentLinkedQueue<String> apiKeys() {
        return apiKeys;
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            String query = exchange.getRequestURI().getQuery();
            int page = query != null && query.startsWith("page=") ? Integer.parseInt(query.substring(5)) : 1;
            int attempt = hitsByPage.computeIfAbsent(page, p -> new AtomicInteger()).incrementAndGet();
            String apiKey = exchange.getRequestHeaders().getFirst("x-api-key");
            if (apiKey != null) {
                apiKeys.add(apiKey);
            }

            Reply reply = responder.reply(page, attempt);
            if (!reply.delay().isZero()) {
                Thread.sleep(reply.delay());
            }
            byte[] body = reply.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            reply.headers().forEach((k, v) -> exchange.getResponseHeaders().set(k, v));
            exchange.sendResponseHeaders(reply.status(), body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (IOException ignored) {
            // Client gave up (e.g. read timeout) while we were responding.
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
