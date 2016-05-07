/*
 * MMEvent.java - Simple event system helper annotation
 *
 * Copyright (C) 2016 MegaMek Team
 *
 * This file is part of MegaMek
 *
 * Some rights reserved. See megamek/docs/license.txt
 */

package megamek.common.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to put on public methods which wish to receive events.
 * <p>
 * A method annotated with this needs to have exactly one argument,
 * this being some subclass of {@link MMEvent}.
 * An instance of that class then needs to be registered with the event bus
 * via {@link EventBus#registerHandler(Object)} for it to work. The exact
 * name of the method is not important, and neither is how many of
 * such methods are packed into a single class.
 * <p>
 * It's a good idea (but not required) to keep a reference to
 * the instance containing the event handlers yourself after registering it,
 * if only to avoid registering it multiple times.
 * <p>
 * To avoid resource leaks, event handlers need be explicitly unregistered.
 * They can do this safely in their event handler methods.
 */
@Retention(value=RetentionPolicy.RUNTIME)
@Target(value=ElementType.METHOD)
public @interface Subscribe {
    /** Priority of the event handler, default 0 */
    public int priority() default 0;
}
