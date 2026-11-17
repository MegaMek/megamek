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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import megamek.common.moves.MovePath;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A walk-on deployment move must leave Precognition running (issue #9601). The walk-on path pauses it to rebuild the
 * unit's paths. Before the fix it was never unpaused, so it never cleared its path cache for the next movement phase,
 * and the bot's next move reused a stale path that deployed the unit again at its old entry hex.
 */
class PrincessWalkOnUnpauseTest {

    private Princess princess;
    private Precognition precognition;
    private Entity undeployedUnit;

    @BeforeEach
    void setUp() {
        princess = spy(new Princess("TestPrincess", UUID.randomUUID().toString(), 1));
        precognition = mock(Precognition.class);
        doReturn(precognition).when(princess).getPrecognition();
        undeployedUnit = mock(Entity.class);
        when(undeployedUnit.isDeployed()).thenReturn(false);
    }

    @Test
    void aWalkOnMoveUnpausesPrecognition() {
        MovePath walkOnPath = mock(MovePath.class);
        doReturn(walkOnPath).when(princess).calculateDeploymentPathForMovementPhase(undeployedUnit);

        assertSame(walkOnPath, princess.continueMovementFor(undeployedUnit));
        verify(precognition).unPause();
    }

    @Test
    void aFailedWalkOnMoveStillUnpausesPrecognition() {
        doThrow(new IllegalStateException("test")).when(princess)
              .calculateDeploymentPathForMovementPhase(undeployedUnit);

        assertThrows(IllegalStateException.class, () -> princess.continueMovementFor(undeployedUnit));
        verify(precognition).unPause();
    }
}
