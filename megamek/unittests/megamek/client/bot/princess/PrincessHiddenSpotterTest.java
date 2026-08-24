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
package megamek.client.bot.princess;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import megamek.common.actions.SpotAction;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Spotting for indirect fire does not break a hidden unit's concealment (TW p.259). Princess used to broadcast a chat
 * readback naming the spotter whenever it declared a spot, and chat goes to every player, so a hidden bot unit gave
 * itself away the moment it spotted. These tests pin the guard that suppresses that readback for hidden units while
 * leaving it in place for everyone else.
 *
 * @see <a href="https://github.com/MegaMek/megamek/issues/8782">Issue #8782</a>
 */
class PrincessHiddenSpotterTest {

    private static final int SPOTTER_ID = 11;
    private static final int TARGET_ID = 22;

    private Princess princess;
    private Entity target;

    @BeforeEach
    void beforeEach() {
        target = mock(Entity.class);
        when(target.getShortName()).thenReturn("Marauder MAD-3R");

        Game game = mock(Game.class);
        when(game.getEntity(anyInt())).thenReturn(target);

        princess = mock(Princess.class);
        when(princess.getGame()).thenReturn(game);
        doCallRealMethod().when(princess).announceSpotting(any(), any());
    }

    private Entity spotter(boolean hidden) {
        Entity spotter = mock(Entity.class);
        when(spotter.getShortName()).thenReturn("Elemental (Sqd 1)");
        when(spotter.isHidden()).thenReturn(hidden);
        return spotter;
    }

    @Test
    void hiddenSpotterSendsNoChatReadback() {
        princess.announceSpotting(new SpotAction(SPOTTER_ID, TARGET_ID), spotter(true));

        verify(princess, never()).sendChat(anyString(), any(Level.class));
    }

    @Test
    void revealedSpotterStillSendsChatReadback() {
        princess.announceSpotting(new SpotAction(SPOTTER_ID, TARGET_ID), spotter(false));

        verify(princess, times(1)).sendChat(anyString(), any(Level.class));
    }

    @Test
    void nonSpotActionSendsNoChatReadback() {
        princess.announceSpotting(null, spotter(false));

        verify(princess, never()).sendChat(anyString(), any(Level.class));
    }
}
