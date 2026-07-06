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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.Player;
import megamek.common.battleValue.BVCalculator;
import megamek.common.board.Coords;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link HeatMap}, in particular that {@link HeatMap#getHotSpot(Coords, boolean)} selects the hot-spot
 * nearest the query position (issue #8438).
 */
class HeatMapTest {

    private static final int TEAM_ID = 1;

    /**
     * Builds a trackable ground unit at a position with the given Battle Value and walk MP, so that
     * {@link HeatMap#updateTrackers(List)} records a hot-spot there weighted by that Battle Value.
     */
    private static Entity mockTrackedEntity(int id, Coords position, int battleValue, int walkMP) {
        Entity entity = mock(BipedMek.class);
        Player owner = mock(Player.class);
        when(owner.getTeam()).thenReturn(TEAM_ID);
        when(entity.getOwner()).thenReturn(owner);
        when(entity.getId()).thenReturn(id);
        when(entity.getPosition()).thenReturn(position);
        when(entity.isGround()).thenReturn(true);
        when(entity.isDeployed()).thenReturn(true);
        when(entity.isVisibleToEnemy()).thenReturn(true);
        when(entity.getWalkMP()).thenReturn(walkMP);
        when(entity.getAnyTypeMaxJumpMP()).thenReturn(0);

        BVCalculator bvCalculator = mock(BVCalculator.class);
        when(bvCalculator.retrieveBV()).thenReturn(battleValue);
        when(entity.getBvCalculator()).thenReturn(bvCalculator);
        return entity;
    }

    @Test
    void getHotSpotReturnsTheNearestHotSpotByDistance() {
        HeatMap heatMap = new HeatMap(TEAM_ID);

        // A far, high-value cluster and a near, low-value one. getHotSpot must return the geometrically nearest
        // hot-spot to the query position regardless of value ordering. The old code compared distance but stored a
        // hex direction (0-5), so once the farther (higher-rated, enumerated first) position was seen, the nearer
        // one - more than 5 hexes away - could never replace it. Positions share a column, where the hex distance
        // equals the row difference.
        Entity farHighValue = mockTrackedEntity(1, new Coords(0, 20), 3000, 1);
        Entity nearLowValue = mockTrackedEntity(2, new Coords(0, 8), 500, 1);
        heatMap.updateTrackers(List.of(farHighValue, nearLowValue));

        Coords queryPosition = new Coords(0, 0);
        assertEquals(new Coords(0, 8), heatMap.getHotSpot(queryPosition, false),
              "getHotSpot must return the hot-spot nearest the query position");
    }

    @Test
    void getHotSpotReturnsNullWhenThereIsNoActivity() {
        HeatMap heatMap = new HeatMap(TEAM_ID);
        assertNull(heatMap.getHotSpot(new Coords(0, 0), false),
              "getHotSpot must return null when no hot-spots have been recorded");
    }
}
