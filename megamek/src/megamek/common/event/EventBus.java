/*
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.event;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EventBus {
    private static final Object INSTANCE_LOCK = new Object[0];

    private static EventBus instance;
    private static final EventSorter EVENT_SORTER = new EventSorter();

    private final Object REGISTER_LOCK = new Object[0];

    private final ConcurrentHashMap<Object, List<EventListener>> handlerMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<? extends MMEvent>, List<EventListener>> eventMap = new ConcurrentHashMap<>();
    // There is no Java-supplied IdentityHashSet ...
    private final Map<Object, Object> unregisterQueue = new IdentityHashMap<>();

    public static EventBus getInstance() {
        synchronized (INSTANCE_LOCK) {
            if (null == instance) {
                instance = new EventBus();
            }
        }
        return instance;
    }

    public static void registerHandler(Object handler) {
        getInstance().register(handler);
    }

    public static void unregisterHandler(Object handler) {
        getInstance().unregister(handler);
    }

    public static boolean triggerEvent(MMEvent event) {
        return getInstance().trigger(event);
    }

    public EventBus() {}

    private List<Class<?>> getClasses(Class<?> leaf) {
        List<Class<?>> result = new ArrayList<>();
        while (null != leaf) {
            result.add(leaf);
            leaf = leaf.getSuperclass();
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public void register(Object handler) {
        if (handlerMap.containsKey(handler)) {
            return;
        }

        for (Method method : handler.getClass().getMethods()) {
            for (Class<?> cls : getClasses(handler.getClass())) {
                try {
                    Method realMethod = cls.getDeclaredMethod(method.getName(), method.getParameterTypes());
                    if (realMethod.isAnnotationPresent(Subscribe.class)) {
                        Class<?> eventType = getEventType(method);
                        internalRegister(handler, realMethod, (Class<? extends MMEvent>) eventType);
                    }
                } catch (NoSuchMethodException e) {
                    // ignore
                }
            }
        }
    }

    private static Class<?> getEventType(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != 1) {
            throw new IllegalArgumentException(
                  String.format("@Subscribe annotation requires single-argument method; %s has %d",
                        method, parameterTypes.length));
        }
        Class<?> eventType = parameterTypes[0];
        if (!MMEvent.class.isAssignableFrom(eventType)) {
            throw new IllegalArgumentException(
                  String.format(
                        "@Subscribe annotation of %s requires the argument type to be some subtype of MMEvent, not %s",
                        method,
                        eventType));
        }
        return eventType;
    }

    private void internalRegister(Object handler, Method method, Class<? extends MMEvent> eventType) {
        synchronized (REGISTER_LOCK) {
            EventListener listener = new EventListener(handler, method, eventType);
            List<EventListener> handlerListeners = handlerMap.computeIfAbsent(handler, k -> new ArrayList<>());
            handlerListeners.add(listener);
            List<EventListener> eventListeners = eventMap.computeIfAbsent(eventType, k -> new ArrayList<>());
            eventListeners.add(listener);
        }
    }

    public void unregister(Object handler) {
        synchronized (REGISTER_LOCK) {
            unregisterQueue.put(handler, handler);
        }
    }

    private void internalUnregister() {
        synchronized (REGISTER_LOCK) {
            for (Object handler : unregisterQueue.keySet()) {
                List<EventListener> listenerList = handlerMap.remove(handler);
                if (null != listenerList) {
                    for (EventListener listener : listenerList) {
                        List<EventListener> eventListeners = eventMap.get(listener.getEventType());
                        if (null != eventListeners) {
                            eventListeners.remove(listener);
                        }
                    }
                }
            }
            unregisterQueue.clear();
        }
    }

    /** @return true if the event was cancelled along the way */
    @SuppressWarnings("unchecked")
    public boolean trigger(MMEvent event) {
        internalUnregister(); // Clean up unregister queue
        for (Class<?> cls : getClasses(event.getClass())) {
            if (MMEvent.class.isAssignableFrom(cls)) {
                // Run through the triggers for each superclass up to MMEvent itself
                internalTrigger((Class<? extends MMEvent>) cls, event);
            }
        }
        return event.isCancellable() && event.isCancelled();
    }

    private void internalTrigger(Class<? extends MMEvent> eventClass, MMEvent event) {
        List<EventListener> eventListeners = eventMap.get(eventClass);
        if (null != eventListeners) {
            eventListeners.sort(EVENT_SORTER);
            for (EventListener listener : eventListeners) {
                listener.trigger(event);
            }
        }
    }

    private static class EventSorter implements Comparator<EventListener> {
        @Override
        public int compare(EventListener el1, EventListener el2) {
            // Highest to lowest, by priority
            return Integer.compare(el2.getPriority(), el1.getPriority());
        }
    }
}
