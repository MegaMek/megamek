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

package megamek.common.strategicBattleSystems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import megamek.common.event.GameEvent;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.UnitChangedGameEvent;
import org.junit.jupiter.api.Test;

/** Tests client-side removal notifications for SBF visibility updates. */
class SBFGameForgetTest {

    @Test
    void forgettingAKnownUnitRemovesItAndFiresOneChangeEvent() {
        SBFGame game = new SBFGame();
        SBFFormation formation = new SBFFormation();
        formation.setId(17);
        game.addUnit(formation);
        AtomicReference<GameEvent> change = new AtomicReference<>();
        game.addGameListener(new GameListenerAdapter() {
            @Override
            public void gameUnitChange(GameEvent event) {
                change.set(event);
            }
        });

        game.forget(formation.getId());

        assertTrue(game.getFormation(formation.getId()).isEmpty());
        UnitChangedGameEvent event = assertInstanceOf(UnitChangedGameEvent.class, change.get());
        assertSame(game, event.getSource());
        assertSame(formation, event.getOldUnit());
        assertNull(event.getNewUnit());
    }

    @Test
    void forgettingAnUnknownUnitDoesNotFireAChangeEvent() {
        SBFGame game = new SBFGame();
        AtomicInteger changes = new AtomicInteger();
        game.addGameListener(new GameListenerAdapter() {
            @Override
            public void gameUnitChange(GameEvent event) {
                changes.incrementAndGet();
            }
        });

        game.forget(99);

        assertEquals(0, changes.get());
    }
}
