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
package megamek.client.ui.panels.phaseDisplay;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import megamek.common.units.Entity;
import megamek.common.units.Targetable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests that the right-click menu can pick a target without opening the aim window (#9472). The aim window takes
 * focus, which closes the right-click menu on some window managers.
 * <p>
 * {@code target()} itself needs a full client, so it is replaced by its last step: set the target, then ask for the
 * aim window. The methods under test run for real.
 */
@DisplayName("FiringDisplay aim window from the right-click menu")
class FiringDisplayAimWindowTest {

    private FiringDisplay firingDisplay;
    private AimedShotHandler aimedShotHandler;

    @BeforeEach
    void setUp() {
        firingDisplay = mock(FiringDisplay.class);
        aimedShotHandler = mock(AimedShotHandler.class);
        firingDisplay.ash = aimedShotHandler;
        doAnswer(invocation -> {
            firingDisplay.target = invocation.getArgument(0);
            firingDisplay.showAimWindow();
            return null;
        }).when(firingDisplay).target(any());
        doCallRealMethod().when(firingDisplay).targetWithoutAimWindow(any());
        doCallRealMethod().when(firingDisplay).showAimWindow();
    }

    private static Entity createTarget(int id, String name) {
        Entity entity = mock(Entity.class);
        when(entity.getId()).thenReturn(id);
        when(entity.getTargetType()).thenReturn(Targetable.TYPE_ENTITY);
        when(entity.getDisplayName()).thenReturn(name);
        return entity;
    }

    @Test
    @DisplayName("A normal target pick opens the aim window")
    void normalTargetOpensAimWindow() {
        Entity atlas = createTarget(2, "Atlas AS7-D");

        firingDisplay.target(atlas);

        verify(aimedShotHandler, times(1)).showDialog();
    }

    @Test
    @DisplayName("A pick from the right-click menu sets the target without opening the aim window")
    void menuPickDoesNotOpenAimWindow() {
        Entity atlas = createTarget(2, "Atlas AS7-D");

        firingDisplay.targetWithoutAimWindow(atlas);

        assertSame(atlas, firingDisplay.target);
        verify(aimedShotHandler, never()).showDialog();
    }

    @Test
    @DisplayName("After a menu pick, the next normal pick opens the aim window again")
    void aimWindowReturnsAfterMenuPick() {
        Entity atlas = createTarget(2, "Atlas AS7-D");
        firingDisplay.targetWithoutAimWindow(atlas);

        firingDisplay.target(atlas);

        verify(aimedShotHandler, times(1)).showDialog();
    }

    @Test
    @DisplayName("A menu pick of a new target closes the aim window left open for the earlier one")
    void menuPickOfNewTargetClosesEarlierAimWindow() {
        firingDisplay.target(createTarget(2, "Atlas AS7-D"));

        firingDisplay.targetWithoutAimWindow(createTarget(3, "Marauder MAD-3R"));

        verify(aimedShotHandler, times(1)).closeDialog();
    }

    @Test
    @DisplayName("A menu pick of the same target keeps its open aim window and chosen location")
    void menuPickOfSameTargetKeepsAimWindow() {
        firingDisplay.target(createTarget(2, "Atlas AS7-D"));

        firingDisplay.targetWithoutAimWindow(createTarget(2, "Atlas AS7-D"));

        verify(aimedShotHandler, never()).closeDialog();
    }
}
