package me.dio.client.reqres;

import java.io.IOException;
import java.net.http.HttpClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ReqresProperties.class)
class ReqresClientConfig {

    static final String API_KEY_HEADER = "x-api-key";

    private static final Logger log = LoggerFactory.getLogger(ReqresClient.class);

    @Bean
    RestClient reqresRestClient(RestClient.Builder builder, ReqresProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        if (!StringUtils.hasText(properties.apiKey())) {
            log.info("No reqres.api-key configured; using the reqres.in shared demo quota");
        }

        return builder
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .defaultHeaders(headers -> {
                    if (StringUtils.hasText(properties.apiKey())) {
                        headers.set(API_KEY_HEADER, properties.apiKey());
                    }
                })
                .requestInterceptor(loggingInterceptor())
                .build();
    }

    /**
     * Logs every outbound call with its status and duration. Headers (which carry
     * the API key) and bodies are deliberately not logged.
     */
    private static ClientHttpRequestInterceptor loggingInterceptor() {
        return (request, body, execution) -> {
            long start = System.nanoTime();
            try {
                ClientHttpResponse response = execution.execute(request, body);
                log.debug("{} {} -> {} in {} ms", request.getMethod(), request.getURI(),
                        response.getStatusCode().value(), elapsedMillis(start));
                return response;
            } catch (IOException ex) {
                log.warn("{} {} failed after {} ms: {}", request.getMethod(), request.getURI(),
                        elapsedMillis(start), ex.toString());
                throw ex;
            }
        };
    }

    private static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
