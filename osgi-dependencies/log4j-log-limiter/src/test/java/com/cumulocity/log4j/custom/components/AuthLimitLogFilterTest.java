package com.cumulocity.log4j.custom.components;

import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.cumulocity.log4j.custom.components.AuthLimitLogFilter.OAUTH_LIMIT_MARKER;
import static com.cumulocity.log4j.custom.components.AuthLimitLogFilter.TENANT_CONTEXT_KEY;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthLimitLogFilterTest {

    private AuthLimitLogFilter filter;
    private final long LIMIT_LOG_NUMBER = 3;
    private final long CACHE_EXPIRATION_SEC = 1;

    @BeforeEach
    public void setUp() {
        filter = new AuthLimitLogFilter.Builder()
                .setCacheExpirationInSeconds(CACHE_EXPIRATION_SEC)
                .setLimitLogNumberPerTenant(LIMIT_LOG_NUMBER).build();
    }

    @Test
    public void shouldNeutralFewerEventsThanLimit() {
        // Given
        final LogEvent logEvent = createLogEvent("someTenant", OAUTH_LIMIT_MARKER, "someMethodName");
        // When -> Then
        sendSuccessfullyEvent(logEvent, LIMIT_LOG_NUMBER);
    }

    @Test
    public void shouldDenyIfLimitExceed() {
        // Given
        LogEvent logEvent = createLogEvent("someTenant", OAUTH_LIMIT_MARKER, "someMethod");
        useFullLogLimit(logEvent);
        // When
        final Result result = this.filter.filter(logEvent);
        // Then
        assertEquals(Result.DENY, result);
    }

    @Test
    public void shouldNeutralWhenContextDontHaveTenant() {
        // Given
        LogEvent logEvent = createLogEvent(null, OAUTH_LIMIT_MARKER, "someMethod");
        // When
        final Result result = this.filter.filter(logEvent);
        // Then
        assertEquals(Result.NEUTRAL, result);
    }

    @Test
    public void shouldNeutralAfterLogLimitExceedAndCacheExpire() throws InterruptedException {
        // Given
        LogEvent logEvent = createLogEvent("myTenant", OAUTH_LIMIT_MARKER, "sourceMethod");
        useFullLogLimit(logEvent);
        expireCache();
        // When
        final Result result = this.filter.filter(logEvent);
        // Then
        assertEquals(Result.NEUTRAL, result);
    }

    @Test
    public void lastCacheWriteShouldNotBeUpdatedInNextEvents() throws InterruptedException {
        // Given
        LogEvent logEvent = createLogEvent("someTenant", OAUTH_LIMIT_MARKER, "someSource");
        // When
        this.filter.filter(logEvent);
        waitHalfCache();
        sendSuccessfullyEvent(logEvent, LIMIT_LOG_NUMBER - 1);
        waitHalfCache();
        final Result result = this.filter.filter(logEvent);
        // Then
        assertEquals(Result.NEUTRAL, result);
    }

    @ParameterizedTest
    @MethodSource("noOauthMarkers")
    public void shouldNeutralOtherMarkers(MarkerManager.Log4jMarker marker) {
        // Given
        LogEvent logEvent = createLogEvent("tenant", marker, "source");
        // When
        final Result result = this.filter.filter(logEvent);
        // Then
        assertEquals(Result.NEUTRAL, result);
    }

    private static Stream<MarkerManager.Log4jMarker> noOauthMarkers() {
        return Stream.of(
                new MarkerManager.Log4jMarker("someRandomMarkers"),
                null
        );
    }

    private void expireCache() throws InterruptedException {
        Thread.sleep(CACHE_EXPIRATION_SEC * 1000);
    }

    private void waitHalfCache() throws InterruptedException {
        Thread.sleep(CACHE_EXPIRATION_SEC * 500);
    }

    private LogEvent createLogEvent(String tenant, MarkerManager.Log4jMarker marker, String sourceMethod) {
        SortedArrayStringMap map = new SortedArrayStringMap(singletonMap(TENANT_CONTEXT_KEY, tenant));
        return new Log4jLogEvent.Builder()
                .setSource(new StackTraceElement("someClass", sourceMethod, "someFile", 1))
                .setMarker(marker)
                .setContextData(map)
                .build();
    }

    private void useFullLogLimit(LogEvent logEvent) {
        sendSuccessfullyEvent(logEvent, LIMIT_LOG_NUMBER);
    }

    private void sendSuccessfullyEvent(LogEvent logEvent, long repetitionCounter) {
        for (int i = 0; i < repetitionCounter; i++) {
            final Result result = this.filter.filter(logEvent);
            assertEquals(Result.NEUTRAL, result);
        }
    }
}