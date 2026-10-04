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
package megamek.client.ratgenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import megamek.common.units.UnitType;
import org.junit.jupiter.api.Test;

/**
 * The force generator ruleset files identify unit types by the internal names in
 * {@link UnitType}, which {@link AbstractUnitRecord#parseUnitType(String)} turns back into unit type
 * codes. Those internal names are therefore a data format and must not be respelled, even though
 * several of them are not how the game presents the unit type to a player.
 */
class UnitTypeNameParsingTest {

    /**
     * {@link UnitType#BATTLEFIELD_SUPPORT_ASSET} has no case in
     * {@link AbstractUnitRecord#parseUnitType(String)}, so it is excluded here. No force generator
     * ruleset file names it.
     */
    private static final int PARSEABLE_UNIT_TYPE_COUNT = UnitType.BATTLEFIELD_SUPPORT_ASSET;

    @Test
    void parseUnitTypeAcceptsEveryInternalUnitTypeName() {
        for (int unitType = 0; unitType < PARSEABLE_UNIT_TYPE_COUNT; unitType++) {
            String internalName = UnitType.getTypeName(unitType);
            assertEquals(unitType, AbstractUnitRecord.parseUnitType(internalName),
                  "Internal name '" + internalName + "' must parse back to its own unit type code; "
                        + "the force generator ruleset files are written in these names.");
        }
    }

    /**
     * Guards the reason the force generator compares unit type codes rather than names: for several
     * unit types the name a player is shown is deliberately not the name in the data files.
     */
    @Test
    void displayableNameIsNotAlwaysTheInternalName() {
        int[] unitTypesShownUnderADifferentName = { UnitType.DROPSHIP, UnitType.BATTLE_ARMOR,
                                                    UnitType.AEROSPACE_FIGHTER };
        for (int unitType : unitTypesShownUnderADifferentName) {
            assertNotEquals(UnitType.getTypeName(unitType), UnitType.getTypeDisplayableName(unitType),
                  "Unit type " + unitType + " is one of the types whose displayed name differs from "
                        + "the name in the ruleset files, which is why the force generator compares "
                        + "unit type codes instead of names.");
        }
    }
}
