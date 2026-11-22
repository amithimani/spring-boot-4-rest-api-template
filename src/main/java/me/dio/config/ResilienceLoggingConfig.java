package me.dio.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;

/**
 * Logs Resilience4j events for every circuit breaker and retry instance:
 * circuit state transitions, rejected calls, and retry attempts.
 */
@Configuration(proxyBeanMethods = false)
public class ResilienceLoggingConfig {

    private static final Logger log = LoggerFactory.getLogger("me.dio.resilience");

    @Bean
    RegistryEventConsumer<CircuitBreaker> circuitBreakerEventLogger() {
        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> event) {
                event.getAddedEntry().getEventPublisher()
                        .onStateTransition(e -> log.warn("Circuit breaker '{}' changed state: {}",
                                e.getCircuitBreakerName(), e.getStateTransition()))
                        .onCallNotPermitted(e -> log.debug("Circuit breaker '{}' is OPEN; call rejected",
                                e.getCircuitBreakerName()));
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> event) {
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> event) {
            }
        };
    }

    @Bean
    RegistryEventConsumer<Retry> retryEventLogger() {
        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<Retry> event) {
                event.getAddedEntry().getEventPublisher()
                        .onRetry(e -> log.warn("Retry '{}' attempt {} in {} ms after: {}",
                                e.getName(), e.getNumberOfRetryAttempts(), e.getWaitInterval().toMillis(),
                                String.valueOf(e.getLastThrowable())))
                        .onError(e -> log.warn("Retry '{}' gave up after {} attempts: {}",
                                e.getName(), e.getNumberOfRetryAttempts(), String.valueOf(e.getLastThrowable())));
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<Retry> event) {
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<Retry> event) {
            }
        };
    }
}
