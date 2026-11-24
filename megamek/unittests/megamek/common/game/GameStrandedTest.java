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

package megamek.common.game;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import megamek.common.units.Entity;
import megamek.common.units.VTOL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Game#isEntityStranded(Entity)}. Issue #9389: battle armor aboard a reinforcement VTOL that was
 * immobilized before it entered counted as stranded, so every movement phase opened with an unload-stranded turn.
 */
class GameStrandedTest {

    private static final int TRANSPORT_ID = 93;

    private Game game;
    private VTOL transport;
    private Entity passenger;

    @BeforeEach
    void setUp() {
        game = spy(new Game());
        transport = mock(VTOL.class);
        when(transport.isImmobile()).thenReturn(true);
        doReturn(transport).when(game).getEntity(TRANSPORT_ID);
        passenger = mock(Entity.class);
        when(passenger.getTransportId()).thenReturn(TRANSPORT_ID);
    }

    @Test
    void aUnitAboardAnUndeployedTransportIsNotStranded() {
        when(transport.isDeployed()).thenReturn(false);

        assertFalse(game.isEntityStranded(passenger));
    }

    @Test
    void aUnitAboardAnImmobileTransportOnTheMapIsStranded() {
        when(transport.isDeployed()).thenReturn(true);

        assertTrue(game.isEntityStranded(passenger));
    }
}
