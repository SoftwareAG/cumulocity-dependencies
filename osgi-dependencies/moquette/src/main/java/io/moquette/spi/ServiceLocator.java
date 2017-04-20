package io.moquette.spi;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import io.moquette.server.config.IConfig;
import lombok.extern.slf4j.Slf4j;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;

public class ServiceLocator {

    public static ServiceLookup getInstance(IConfig config) {
        return new DefaultLookup(config);
    }

    public interface ServiceLookup {
        <T> T lookup(Class<T> requireInstance);

        <T> T lookup(Class<T> requireInstance, Class<? extends T> implementation);
    }

    @Slf4j
    public final static class Composite implements ServiceLookup {
        private final Collection<ServiceLookup> lookups;

        public Composite(ServiceLookup... lookups) {
            this(ImmutableList.copyOf(lookups));
        }

        public Composite(Collection<ServiceLookup> lookups) {
            this.lookups = lookups;
        }

        @Override
        public <T> T lookup(Class<T> requireInstance) {
            for (ServiceLookup lookup : lookups) {
                try {
                    final T found = lookup.lookup(requireInstance);
                    if (found != null) {
                        return found;
                    }
                } catch (Exception ex) {
                    continue;
                }
            }
            throw new IllegalArgumentException("Can't locate " + requireInstance.getName());
        }

        @Override
        public <T> T lookup(Class<T> requireInstance, Class<? extends T> implementation) {
            for (ServiceLookup lookup : lookups) {
                try {
                    final T found = lookup.lookup(requireInstance, implementation);
                    if (found != null) {
                        return found;
                    }
                } catch (Exception ex) {
                    log.debug("Can't locate {} with implementation {} using {}", requireInstance, implementation, lookup, ex);
                }
            }
            throw new IllegalArgumentException("Can't locate " + requireInstance.getName());
        }
    }

    @Slf4j
    private final static class DefaultLookup implements ServiceLookup {

        private final IConfig config;

        private final Cache<Class, Object> instances = CacheBuilder
                                                           .newBuilder()
                                                           .build();

        public DefaultLookup(IConfig config) {
            this.config = config;
        }

        @Override
        public <T> T lookup(final Class<T> requireInstance) {
            try {
                return (T) instances.get(requireInstance, new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        return loadClass(requireInstance, requireInstance, config);
                    }
                });
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public <T> T lookup(final Class<T> requireInstance, final Class<? extends T> implementation) {
            try {
                return (T) instances.get(implementation, new Callable<Object>() {
                    @Override
                    public Object call() throws Exception {
                        return loadClass(requireInstance, implementation, config);
                    }
                });
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        private Object loadClass(Class<?> cls, Class<?> clazz, IConfig props) {

            Object instance = null;
            try {

                // check if method getInstance exists
                Method method = clazz.getMethod("getInstance", new Class[] {});
                try {
                    instance = method.invoke(null, new Object[] {});
                } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException ex) {
                    throw new RuntimeException("Cannot call method " + clazz.getName() + ".getInstance", ex);
                }
            } catch (NoSuchMethodException nsmex) {
                try {
                    // check if constructor with IConfig parameter exists
                    final Constructor<?> declaredConstructor = clazz.getDeclaredConstructor(IConfig.class);
                    declaredConstructor.setAccessible(true);
                    instance = declaredConstructor
                                   .newInstance(props);
                } catch (NoSuchMethodException | InvocationTargetException e) {
                    try {
                        // fallback to default constructor
                        instance = clazz
                                       .newInstance();
                    } catch (InstantiationException | IllegalAccessException ex) {
                        throw new RuntimeException("Cannot load  class " + clazz.getName(), ex);
                    }
                } catch (InstantiationException | IllegalAccessException ex) {
                    throw new RuntimeException("Cannot load class " + clazz.getName(), ex);
                }
            } catch (SecurityException ex) {
                throw new RuntimeException("Cannot call method " + clazz.getName() + ".getInstance", ex);
            }

            return instance;
        }
    }

}
