/*
 * Copyright (c) 2016-2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.event;

import org.apache.logging.log4j.LogManager;

import java.lang.reflect.Method;
import java.util.Objects;

class EventListener {
    private final Object handler;
    private final Method method;
    private final Class<? extends MMEvent> eventType;
    private final Subscribe info;

    public EventListener(Object handler, Method method, Class<? extends MMEvent> eventType) {
        this.handler = Objects.requireNonNull(handler);
        this.method = Objects.requireNonNull(method);
        this.eventType = Objects.requireNonNull(eventType);
        this.info = method.getAnnotation(Subscribe.class);
    }
    
    public void trigger(MMEvent event) {
        if (!event.isCancellable() || !event.isCancelled()) {
            try {
                method.invoke(handler, event);
            } catch (Exception e) {
                LogManager.getLogger().error("", e);
            }
        }
    }
    
    public int getPriority() {
        return info.priority();
    }

    public Class<? extends MMEvent> getEventType() {
        return eventType;
    }
}
