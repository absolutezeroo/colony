package com.akikazu.colony.core.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a reflective subscriber for {@link EventBus}. The method must be public, take exactly one parameter
 * whose type extends {@link Event}, and return {@code void}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe
{
}
