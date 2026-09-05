/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link GameToastEvent} contract: accessors and listener dispatch.
 */
class GameToastEventTest {

    @Test
    @DisplayName("Accessors return the values supplied at construction")
    void accessorsReturnConstructorValues() {
        GameToastEvent event = new GameToastEvent(this, GameToastEvent.Level.SUCCESS, "Fortification complete", 42);

        assertEquals(GameToastEvent.Level.SUCCESS, event.level());
        assertEquals("Fortification complete", event.message());
        assertEquals(42, event.entityId());
    }

    @Test
    @DisplayName("fireEvent dispatches to GameListener.gameToast with the same event")
    void fireEventDispatchesToListener() {
        GameToastEvent event = new GameToastEvent(this, GameToastEvent.Level.WARNING, "No fieldworks equipment", 7);
        AtomicReference<GameToastEvent> received = new AtomicReference<>();
        GameListener listener = new GameListenerAdapter() {
            @Override
            public void gameToast(GameToastEvent toastEvent) {
                received.set(toastEvent);
            }
        };

        event.fireEvent(listener);

        assertSame(event, received.get(), "fireEvent should pass the event to gameToast");
    }
}
