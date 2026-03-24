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

package megamek.ai.dataset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.ai.dataset.GameData.MinefieldData;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for GameDataSerializer class.
 */
class GameDataSerializerTest {

    @Test
    void testSerializeWithUnitStatesAndMinefields() {
        GameData data = new GameData();
        data.put(GameData.Field.ROUND, 1)
              .put(GameData.Field.PHASE, megamek.common.enums.GamePhase.MOVEMENT);

        // Mock a UnitState
        UnitState unitState = new UnitState();
        unitState.put(UnitState.Field.ID, 100);
        unitState.put(UnitState.Field.CHASSIS, "Locust");
        data.put(GameData.Field.ENTITIES, List.of(unitState));

        // Mock a MinefieldData
        megamek.common.equipment.Minefield mockMf = Mockito.mock(megamek.common.equipment.Minefield.class);
        Mockito.when(mockMf.getCoords()).thenReturn(new megamek.common.board.Coords(5, 5));
        Mockito.when(mockMf.getType()).thenReturn(1);
        Mockito.when(mockMf.getPlayerId()).thenReturn(2);
        Mockito.when(mockMf.getDensity()).thenReturn(10);
        MinefieldData mfData = new MinefieldData(mockMf);
        data.put(GameData.Field.MINEFIELDS, List.of(mfData));

        GameDataSerializer serializer = new GameDataSerializer();
        String serialized = serializer.serialize(data);

        String[] lines = serialized.split("\n");
        // UnitState Header, UnitState Data, Minefield Header, Minefield Data
        assertTrue(lines.length >= 4);

        // Check if UnitState header and data are present
        boolean unitStateFound = false;
        boolean minefieldFound = false;
        for (String line : lines) {
            if (line.contains("Locust")) {unitStateFound = true;}
            if (line.contains("MINEFIELD") && line.contains("5\t5\t1\t2\t10")) {minefieldFound = true;}
        }

        assertTrue(unitStateFound);
        assertTrue(minefieldFound);
    }

    @Test
    void testSerializeEmpty() {
        GameData data = new GameData();
        GameDataSerializer serializer = new GameDataSerializer();
        String serialized = serializer.serialize(data);
        assertEquals("", serialized);
    }
}
