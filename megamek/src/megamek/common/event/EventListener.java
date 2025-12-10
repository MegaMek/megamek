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
import java.util.Objects;

import megamek.logging.MMLogger;

class EventListener {
    private static final MMLogger logger = MMLogger.create(EventListener.class);

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
                logger.error("", e);
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
