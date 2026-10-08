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

import static megamek.client.ui.dialogs.randomArmy.ForceGeneratorOptionsView.describeGeneratedForce;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.client.ratgenerator.ForceDescriptor;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitType;
import org.junit.jupiter.api.Test;

/**
 * The [ForceGen][Result] log line names each sub-force and counts its units by type, so a tester can read a roll's
 * outcome from the log.
 */
class ForceGeneratorResultLogTest {

    @Test
    void namesEachSubForceAndCountsItsUnits() {
        ForceDescriptor trinary = formation("Battle Armor Trinary", UnitType.BATTLE_ARMOR);
        trinary.setFaction("CJF");
        trinary.setYear(3150);
        trinary.setRating("FL");
        trinary.setWeightClass(EntityWeightClass.WEIGHT_HEAVY);

        ForceDescriptor nova = formation("Nova", UnitType.MEK);
        nova.setWeightClass(EntityWeightClass.WEIGHT_HEAVY);
        for (int point = 0; point < 5; point++) {
            nova.addSubForce(unit(UnitType.MEK));
            nova.addSubForce(unit(UnitType.BATTLE_ARMOR));
        }
        ForceDescriptor strider = formation("Strider 1", UnitType.BATTLE_ARMOR);
        for (int point = 0; point < 5; point++) {
            strider.addSubForce(unit(UnitType.BATTLE_ARMOR));
        }
        trinary.addSubForce(nova);
        trinary.addSubForce(strider);

        String line = describeGeneratedForce(trinary, null);

        assertTrue(line.contains("weight asked=Random rolled=Heavy"), line);
        assertTrue(line.contains("2 sub-force(s)"), line);
        assertTrue(line.contains("[Nova (Heavy): Battle Armor=5, Mek=5]"), line);
        assertTrue(line.contains("[Strider 1 (no weight): Battle Armor=5]"), line);
        assertTrue(line.contains("totals Battle Armor=10, Mek=5"), line);
    }

    @Test
    void anEmptySubForceSaysSo() {
        ForceDescriptor star = formation("Star", UnitType.MEK);
        star.addSubForce(formation("Point", UnitType.MEK));

        String line = describeGeneratedForce(star, EntityWeightClass.WEIGHT_ASSAULT);

        assertTrue(line.contains("weight asked=Assault"), line);
        assertTrue(line.contains("no units"), line);
    }

    private static ForceDescriptor formation(String name, int unitType) {
        ForceDescriptor formation = new ForceDescriptor();
        formation.setName(name);
        formation.setUnitType(unitType);
        return formation;
    }

    private static ForceDescriptor unit(int unitType) {
        ForceDescriptor unit = formation("unit", unitType);
        unit.setElement(true);
        return unit;
    }
}
