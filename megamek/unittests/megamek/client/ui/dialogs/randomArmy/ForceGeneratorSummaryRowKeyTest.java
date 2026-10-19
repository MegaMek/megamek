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
package megamek.client.ui.dialogs.randomArmy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.TreeSet;

import megamek.client.ratgenerator.InfantryClass;
import megamek.client.ui.dialogs.randomArmy.ForceGeneratorOptionsView.SummaryRowKey;
import megamek.common.units.UnitType;
import org.junit.jupiter.api.Test;

/**
 * The force summary splits conventional infantry into one row per infantry class.
 */
class ForceGeneratorSummaryRowKeyTest {

    @Test
    void infantryRowsAreLabelledByClass() {
        SummaryRowKey jump = new SummaryRowKey(UnitType.INFANTRY, InfantryClass.JUMP);
        SummaryRowKey unclassified = new SummaryRowKey(UnitType.INFANTRY, null);

        assertEquals(UnitType.getTypeDisplayableName(UnitType.INFANTRY) + " (" + InfantryClass.JUMP.getDisplayName()
              + ")", jump.label());
        assertEquals(UnitType.getTypeDisplayableName(UnitType.INFANTRY), unclassified.label());
    }

    @Test
    void rowsSortByUnitTypeThenClass() {
        TreeSet<SummaryRowKey> rows = new TreeSet<>(List.of(
              new SummaryRowKey(UnitType.INFANTRY, InfantryClass.JUMP),
              new SummaryRowKey(UnitType.MEK, null),
              new SummaryRowKey(UnitType.INFANTRY, InfantryClass.LIGHT),
              new SummaryRowKey(UnitType.INFANTRY, null)));

        List<SummaryRowKey> expected = List.of(
              new SummaryRowKey(UnitType.MEK, null),
              new SummaryRowKey(UnitType.INFANTRY, null),
              new SummaryRowKey(UnitType.INFANTRY, InfantryClass.LIGHT),
              new SummaryRowKey(UnitType.INFANTRY, InfantryClass.JUMP));
        assertEquals(expected, List.copyOf(rows));
    }
}
