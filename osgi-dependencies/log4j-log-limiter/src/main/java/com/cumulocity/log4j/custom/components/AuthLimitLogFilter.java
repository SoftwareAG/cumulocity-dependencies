package com.cumulocity.log4j.custom.components;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Plugin(name = "AuthLimitLogFilter", category = Node.CATEGORY, elementType = Filter.ELEMENT_TYPE, printObject = true)
public class AuthLimitLogFilter extends AbstractFilter {

    public static final MarkerManager.Log4jMarker OAUTH_LIMIT_MARKER = new MarkerManager.Log4jMarker("oauth-per-tenant-limit");

    private static final long DEFAULT_LIMIT = 10;

    private static final long DEFAULT_CACHE_EXPIRATION_SECONDS = 1800; //30 min

    private final long limitLogNumberPerTenant;

    protected final static String TENANT_CONTEXT_KEY = "tenant";

    private final LoadingCache<SourceKey, AtomicInteger> limitLogCache;

    private AuthLimitLogFilter(final Result onMatch, final Result onMismatch, final long limitLogNumberPerTenant, final long cacheExpirationInSeconds) {
        super(onMatch, onMismatch);
        this.limitLogNumberPerTenant = limitLogNumberPerTenant;
        limitLogCache = CacheBuilder.newBuilder()
                .expireAfterWrite(cacheExpirationInSeconds, TimeUnit.SECONDS)
                .maximumSize(5000)
                .concurrencyLevel(Runtime.getRuntime().availableProcessors() * 2)
                .build(new CacheLoader<SourceKey, AtomicInteger>() {
                    @Override
                    public AtomicInteger load(SourceKey key) throws Exception {
                        return new AtomicInteger();
                    }
                });
    }

    @Override
    public Result filter(LogEvent event) {
        final Marker marker = event.getMarker();
        if (marker == null) {
            return onMatch;
        }
        if (OAUTH_LIMIT_MARKER.isInstanceOf(marker.getName())) {
            final Object tenant = event.getContextData().getValue(TENANT_CONTEXT_KEY);
            if (tenant == null) {
                return onMatch;
            } else {
                return updateCounter(SourceKey.of(tenant.toString(), event.getSource()));
            }
        }
        return onMatch;
    }

    private Result updateCounter(SourceKey sourceKey) {
        final AtomicInteger logCounter = limitLogCache.getUnchecked(sourceKey);
        if (logCounter.incrementAndGet() > limitLogNumberPerTenant) {
            return onMismatch;
        } else {
            return onMatch;
        }
    }

    private static class SourceKey {

        private final String tenantId;
        private final int sourceHash;

        public SourceKey(String tenantId, int source) {
            this.tenantId = tenantId;
            this.sourceHash = source;
        }

        static SourceKey of(String tenantId, StackTraceElement source) {
            return new SourceKey(tenantId, source.hashCode());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SourceKey sourceKey = (SourceKey) o;

            if (sourceHash != sourceKey.sourceHash) return false;
            return Objects.equals(tenantId, sourceKey.tenantId);
        }

        @Override
        public int hashCode() {
            int result = tenantId != null ? tenantId.hashCode() : 0;
            result = 31 * result + sourceHash;
            return result;
        }
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        return onMatch;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
        return onMatch;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object... params) {
        return onMatch;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0) {
        return onMatch;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1) {
        return onMatch;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2) {
        return onMatch;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3) {
        return onMatch;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3, Object p4) {
        return onMatch;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5) {
        return onMatch;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6) {
        return onMatch;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {
        return onMatch;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {
        return onMatch;
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {
        return onMatch;
    }

    @PluginBuilderFactory
    public static AuthLimitLogFilter.Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends AbstractFilterBuilder<AuthLimitLogFilter.Builder> implements org.apache.logging.log4j.core.util.Builder<AuthLimitLogFilter> {

        @PluginBuilderAttribute
        private long limitLogNumberPerTenant;

        @PluginBuilderAttribute
        private long cacheExpirationInSeconds;

        /**
         * Sets the maximum number of events that can occur (during the cache) before events are filtered for exceeding the limit.
         *
         * @param limit Sets the maximum number of events. The default is 10.
         * @return this
         */
        public AuthLimitLogFilter.Builder setLimitLogNumberPerTenant(final long limit) {
            this.limitLogNumberPerTenant = limit;
            return this;
        }

        /**
         * Specifies that each entry should be automatically removed from the cache once a duration has elapsed after the entry's creation,
         * or the most recent replacement of its value.
         *
         * @param cacheExpirationInSeconds Determines how long the event is to be stored in memory
         * @return this
         */
        public AuthLimitLogFilter.Builder setCacheExpirationInSeconds(final long cacheExpirationInSeconds) {
            this.cacheExpirationInSeconds = cacheExpirationInSeconds;
            return this;
        }

        @Override
        public AuthLimitLogFilter build() {
            if (this.limitLogNumberPerTenant <= 0) {
                this.limitLogNumberPerTenant = DEFAULT_LIMIT;
            }
            if (this.cacheExpirationInSeconds <= 0) {
                this.cacheExpirationInSeconds = DEFAULT_CACHE_EXPIRATION_SECONDS;
            }
            LOGGER.info("Created AuthLimitLogFilter for limiting log occurrences: limit: {}, cache expiration {} ", this.limitLogNumberPerTenant, this.cacheExpirationInSeconds);
            return new AuthLimitLogFilter(this.getOnMatch(), this.getOnMismatch(), this.limitLogNumberPerTenant, this.cacheExpirationInSeconds);
        }
    }
}
