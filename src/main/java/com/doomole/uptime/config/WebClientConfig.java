package com.doomole.uptime.config;

import com.doomole.uptime.filter.CorrelationIdFilter;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class WebClientConfig {
    private static final int  CONNECT_TIMEOUT_MS = 5_000;
    private static final long RESPONSE_TIMEOUT_S = 15;
    private static final int  READ_TIMEOUT_S     = 15;
    private static final int  WRITE_TIMEOUT_S    = 10;

    private static final long WC_SLOW_MS         = 1_000; // 느린 외부호출 기준

    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
                .responseTimeout(Duration.ofSeconds(RESPONSE_TIMEOUT_S))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(READ_TIMEOUT_S, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(WRITE_TIMEOUT_S, TimeUnit.SECONDS))
                );

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                // corrId 헤더 전파 + 요약 로깅(느리거나 오류면 경고)
                .filter(correlationAndLogging())
                .build();
    }

    /**
     * 하나의 필터에서:
     * 1) MDC corrId를 X-Correlation-Id 헤더로 전파
     * 2) 성공: 상태코드/지연시간 요약 (DEBUG), 느림/4xx/5xx는 WARN
     * 3) 실패: ERROR
     */
    private ExchangeFilterFunction correlationAndLogging() {
        return (request, next) -> {
            long startNs = System.nanoTime();

            // corrId 가져와서 헤더로 전파
            String corrId = Optional.ofNullable(MDC.get(CorrelationIdFilter.MDC_KEY))
                    .orElse(null);

            ClientRequest mutated = ClientRequest.from(request)
                    .headers(h -> {
                        if (corrId != null) {
                            h.add(CorrelationIdFilter.HDR, corrId);
                        }
                    })
                    .build();

            return next.exchange(mutated)
                    .doOnSuccess(resp -> {
                        long tookMs = (System.nanoTime() - startNs) / 1_000_000;
                        int sc = resp.statusCode().value();

                        // 느리거나 오류면 WARN, 그 외 성공은 DEBUG
                        if (sc >= 400 || tookMs > WC_SLOW_MS) {
                            log.warn("WC {} {} -> {} in {}ms corrId={}",
                                    request.method(), request.url(), sc, tookMs, corrId);
                        } else if (log.isDebugEnabled()) {
                            log.debug("WC {} {} -> {} in {}ms corrId={}",
                                    request.method(), request.url(), sc, tookMs, corrId);
                        }
                    })
                    .doOnError(ex -> {
                        long tookMs = (System.nanoTime() - startNs) / 1_000_000;
                        log.error("WC {} {} FAILED in {}ms: {} corrId={}",
                                request.method(), request.url(), tookMs, ex.toString(), corrId);
                    });
        };
    }
}
