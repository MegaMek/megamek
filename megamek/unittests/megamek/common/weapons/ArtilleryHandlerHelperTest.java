/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.weapons;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;
import org.junit.jupiter.api.Test;

/**
 * Tests for ArtilleryHandlerHelper utility methods.
 */
class ArtilleryHandlerHelperTest {

    /**
     * Test that findSpotter returns empty when spottersBefore is null.
     */
    @Test
    void findSpotter_withNullList_returnsEmpty() {
        Game game = mock(Game.class);
        Targetable target = mock(Targetable.class);

        Optional<Entity> result = ArtilleryHandlerHelper.findSpotter(null, 0, game, target);

        assertFalse(result.isPresent());
    }

    /**
     * Test that findSpotter returns empty when spottersBefore is empty.
     */
    @Test
    void findSpotter_withEmptyList_returnsEmpty() {
        Game game = mock(Game.class);
        Targetable target = mock(Targetable.class);
        when(game.getSelectedEntities(org.mockito.ArgumentMatchers.any()))
              .thenReturn(Collections.emptyIterator());

        Optional<Entity> result = ArtilleryHandlerHelper.findSpotter(new ArrayList<>(), 0, game, target);

        assertFalse(result.isPresent());
    }

    /**
     * Test that isForwardObserver correctly identifies entities with the FO ability.
     */
    @Test
    void isForwardObserver_withFOAbility_returnsTrue() {
        Entity entity = mock(Entity.class);
        when(entity.hasAbility(OptionsConstants.MISC_FORWARD_OBSERVER)).thenReturn(true);

        assertTrue(ArtilleryHandlerHelper.isForwardObserver(entity));
    }

    /**
     * Test that isForwardObserver returns false for entities without FO ability.
     */
    @Test
    void isForwardObserver_withoutFOAbility_returnsFalse() {
        Entity entity = mock(Entity.class);
        when(entity.hasAbility(OptionsConstants.MISC_FORWARD_OBSERVER)).thenReturn(false);

        assertFalse(ArtilleryHandlerHelper.isForwardObserver(entity));
    }
}
