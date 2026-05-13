package com.akikazu.colony.core.event;

import com.google.common.eventbus.AllowConcurrentEvents;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Domain event bus. Wraps a Guava {@link com.google.common.eventbus.EventBus} for dispatch while exposing a
 * Colony-flavoured API that uses the Colony-defined {@link Subscribe} annotation.
 *
 * <p>
 * Two subscription styles are supported. {@link #subscribe(Class, Consumer)} is the lambda-friendly form preferred in
 * tests and ad-hoc handlers. {@link #subscribeReflective(Object)} scans an object for methods annotated with
 * {@link Subscribe} and registers each one; this is the form used by long-lived listener classes.
 */
public final class EventBus
{
    private final String name;
    private final com.google.common.eventbus.EventBus delegate;
    private final Map<Object, List<Object>> reflectiveHandlers = new ConcurrentHashMap<>();
    private final Map<ConsumerKey, Object> consumerHandlers = new ConcurrentHashMap<>();

    EventBus(String name)
    {
        this.name = Objects.requireNonNull(name, "name");
        this.delegate = new com.google.common.eventbus.EventBus(name);
    }

    public String name()
    {
        return name;
    }

    public void post(Event event)
    {
        Objects.requireNonNull(event, "event");
        delegate.post(event);
    }

    public <E extends Event> void subscribe(Class<E> type, Consumer<E> consumer)
    {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(consumer, "consumer");

        ConsumerHandler<E> handler = new ConsumerHandler<>(type, consumer);
        consumerHandlers.put(new ConsumerKey(type, consumer), handler);
        delegate.register(handler);
    }

    public <E extends Event> void unsubscribe(Class<E> type, Consumer<E> consumer)
    {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(consumer, "consumer");

        Object handler = consumerHandlers.remove(new ConsumerKey(type, consumer));

        if (handler != null)
        {
            delegate.unregister(handler);
        }
    }

    public void subscribeReflective(Object listener)
    {
        Objects.requireNonNull(listener, "listener");

        List<Object> handlers = new ArrayList<>();

        for (Method method : listener.getClass().getMethods())
        {
            if (!method.isAnnotationPresent(Subscribe.class))
            {
                continue;
            }

            if (method.getParameterCount() != 1)
            {
                throw new IllegalArgumentException(
                        "@Subscribe method %s must declare exactly one parameter".formatted(method));
            }

            Class<?> paramType = method.getParameterTypes()[0];

            if (!Event.class.isAssignableFrom(paramType))
            {
                throw new IllegalArgumentException(
                        "@Subscribe method %s parameter %s must implement Event"
                                .formatted(method, paramType.getName()));
            }

            ReflectiveHandler handler = new ReflectiveHandler(listener, method, paramType);
            handlers.add(handler);
            delegate.register(handler);
        }

        reflectiveHandlers.put(listener, handlers);
    }

    public void unsubscribeReflective(Object listener)
    {
        Objects.requireNonNull(listener, "listener");

        List<Object> handlers = reflectiveHandlers.remove(listener);

        if (handlers == null)
        {
            return;
        }

        for (Object handler : handlers)
        {
            delegate.unregister(handler);
        }
    }

    private record ConsumerKey(Class<?> type, Consumer<?> consumer)
    {
    }

    public static final class ConsumerHandler<E extends Event>
    {
        private final Class<E> type;
        private final Consumer<E> consumer;

        ConsumerHandler(Class<E> type, Consumer<E> consumer)
        {
            this.type = type;
            this.consumer = consumer;
        }

        @com.google.common.eventbus.Subscribe
        @AllowConcurrentEvents
        public void handle(Event event)
        {
            if (type.isInstance(event))
            {
                consumer.accept(type.cast(event));
            }
        }
    }

    public static final class ReflectiveHandler
    {
        private final Object target;
        private final Method method;
        private final Class<?> paramType;

        ReflectiveHandler(Object target, Method method, Class<?> paramType)
        {
            this.target = target;
            this.method = method;
            this.paramType = paramType;
            method.setAccessible(true);
        }

        @com.google.common.eventbus.Subscribe
        @AllowConcurrentEvents
        public void handle(Event event)
        {
            if (!paramType.isInstance(event))
            {
                return;
            }

            try
            {
                method.invoke(target, event);
            }
            catch (IllegalAccessException e)
            {
                throw new IllegalStateException(
                        "Cannot invoke @Subscribe method %s".formatted(method), e);
            }
            catch (InvocationTargetException e)
            {
                Throwable cause = e.getCause();

                if (cause instanceof RuntimeException re)
                {
                    throw re;
                }

                throw new IllegalStateException(
                        "@Subscribe method %s threw".formatted(method), cause);
            }
        }
    }
}
