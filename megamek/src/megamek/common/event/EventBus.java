/*
 * EventBus.java - Simple event bus implementation
 *
 * Copyright (C) 2016 MegaMek Team
 *
 * This file is part of MegaMek
 *
 * Some rights reserved. See megamek/docs/license.txt
 */

package megamek.common.event;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
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
    
    private ConcurrentHashMap<Object, List<EventListener>> handlerMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Class<? extends MMEvent>, List<EventListener>> eventMap = new ConcurrentHashMap<>();
    // There is no Java-supplied IdentityHashSet ...
    private Map<Object, Object> unregisterQueue = new IdentityHashMap<>();
    
    public static EventBus getInstance() {
        synchronized(INSTANCE_LOCK) {
            if(null == instance) {
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
        while(null != leaf) {
            result.add(leaf);
            leaf = leaf.getSuperclass();
        }
        return result;
    }
    
    @SuppressWarnings("unchecked")
    public void register(Object handler) {
        if(handlerMap.containsKey(handler)) {
            return;
        }
        
        for(Method method : handler.getClass().getMethods()) {
            for(Class<?> cls : getClasses(handler.getClass())) {
                try {
                    Method realMethod = cls.getDeclaredMethod(method.getName(), method.getParameterTypes());
                    if(realMethod.isAnnotationPresent(Subscribe.class)) {
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        if(parameterTypes.length != 1) {
                            throw new IllegalArgumentException(
                                String.format("@Subscribe annotation requires single-argument method; %s has %d", //$NON-NLS-1$
                                    method, parameterTypes.length));
                        }
                        Class<?> eventType = parameterTypes[0];
                        if(!MMEvent.class.isAssignableFrom(eventType)) {
                            throw new IllegalArgumentException(
                                String.format("@Subscribe annotation of %s requires the argument type to be some subtype of MMEvent, not %s", //$NON-NLS-1$
                                    method, eventType));
                        }
                        internalRegister(handler, realMethod, (Class<? extends MMEvent>) eventType);
                    }
                } catch (NoSuchMethodException e) {
                    // ignore
                }
            }
        }
    }

    private void internalRegister(Object handler, Method method, Class<? extends MMEvent> eventType) {
        synchronized(REGISTER_LOCK) {
            EventListener listener = new EventListener(handler, method, eventType);
            List<EventListener> handlerListeners = handlerMap.get(handler);
            if(null == handlerListeners) {
                handlerListeners = new ArrayList<>();
                handlerMap.put(handler, handlerListeners);
            }
            handlerListeners.add(listener);
            List<EventListener> eventListeners = eventMap.get(eventType);
            if(null == eventListeners) {
                eventListeners = new ArrayList<>();
                eventMap.put(eventType, eventListeners);
            }
            eventListeners.add(listener);
        }
    }
    
    public void unregister(Object handler) {
        synchronized(REGISTER_LOCK) {
            unregisterQueue.put(handler, handler);
        }
    }
    
    private void internalUnregister() {
        synchronized(REGISTER_LOCK) {
            for(Object handler : unregisterQueue.keySet()) {
                List<EventListener> listenerList = handlerMap.remove(handler);
                if(null != listenerList) {
                    for(EventListener listener : listenerList) {
                        List<EventListener> eventListeners = eventMap.get(listener.getEventType());
                        if(null != eventListeners) {
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
        for(Class<?> cls : getClasses(event.getClass())) {
            if(MMEvent.class.isAssignableFrom(cls)) {
                // Run through the triggers for each superclass up to MMEvent itself
                internalTrigger((Class<? extends MMEvent>) cls, event);
            }
        }
        return event.isCancellable() ? event.isCancelled() : false;
    }
    
    private void internalTrigger(Class<? extends MMEvent> eventClass, MMEvent event) {
        List<EventListener> eventListeners = eventMap.get(eventClass);
        if(null != eventListeners) {
            Collections.sort(eventListeners, EVENT_SORTER);
            for(EventListener listener : eventListeners) {
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
