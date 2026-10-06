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
package megamek.common.util;

import static megamek.common.SourceBookCode.BMM;
import static megamek.common.SourceBookCode.CORE;
import static megamek.common.SourceBookCode.IO_AE;
import static megamek.common.SourceBookCode.TM;
import static megamek.common.SourceBookCode.TW;
import static megamek.common.equipment.EquipmentType.T_ARMOR_FERRO_FIBROUS;
import static megamek.common.equipment.EquipmentType.T_ARMOR_STANDARD;
import static megamek.common.equipment.EquipmentType.T_STRUCTURE_ENDO_PROTOTYPE;
import static megamek.common.equipment.EquipmentType.T_STRUCTURE_STANDARD;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.loaders.MekSummary;
import megamek.common.units.BipedMek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class UnitRulesRefUtilTest {

    @BeforeAll
    static void initializeEquipmentTypes() {
        EquipmentType.initializeTypes();
    }

    @Test
    void findsMinimalCombinationsForUnitA() {
        assertEquals(List.of(List.of(CORE), List.of(TW, IO_AE)),
              UnitRulesRefUtil.createMinimalCombinations(List.of(
                    List.of(TW, CORE),
                    List.of(CORE, IO_AE),
                    List.of(TW, CORE, TM)), true, false));
    }

    @Test
    void keepsBooksThatAreActuallyRequiredForUnitB() {
        assertEquals(List.of(List.of(CORE), List.of(TW, IO_AE, TM)),
              UnitRulesRefUtil.createMinimalCombinations(List.of(
                    List.of(TW, CORE),
                    List.of(CORE, IO_AE),
                    List.of(CORE, TM)), true, false));
    }

    @Test
    void removesEveryRedundantSuperset() {
        assertEquals(List.of(List.of(CORE)), UnitRulesRefUtil.createMinimalCombinations(List.of(
              List.of(CORE, TW),
              List.of(CORE)), true, false));
    }

    @Test
    void gatesCoreAndBmmByUnitType() {
        assertEquals(List.of(List.of(TW)), UnitRulesRefUtil.createMinimalCombinations(
              List.of(List.of(CORE, BMM, TW)), false, false));
        assertEquals(List.of(List.of(BMM), List.of(TM)), UnitRulesRefUtil.createMinimalCombinations(
              List.of(List.of(CORE, BMM, TM)), true, true));
        assertEquals(List.of(), UnitRulesRefUtil.createMinimalCombinations(
              List.of(List.of(CORE)), false, false));
    }

    @Test
    void treatsComponentsWithoutReferencesAsNeutral() {
        assertEquals(List.of(List.of(TW)), UnitRulesRefUtil.createMinimalCombinations(List.of(
              List.of(),
              List.of(TW)), true, false));
        assertEquals(List.of(), UnitRulesRefUtil.createMinimalCombinations(List.of(List.of()), true, false));
    }

    @Test
    void includesIntrinsicArmorRulesRefsAndPopulatesMekSummary() {
        BipedMek mek = new BipedMek();
        mek.setArmorType(T_ARMOR_FERRO_FIBROUS);
        mek.setStructureType(T_STRUCTURE_STANDARD);
        MekSummary summary = new MekSummary();

        summary.setRulesRefs(mek);

        assertEquals(Set.of(List.of(CORE), List.of(TW), List.of(BMM), List.of(TM)),
              Set.copyOf(summary.getRulesRefs()));
    }

    @Test
    void includesIntrinsicStructureRulesRefs() {
        BipedMek mek = new BipedMek();
        mek.setArmorType(T_ARMOR_STANDARD);
        mek.setStructureType(T_STRUCTURE_ENDO_PROTOTYPE);

        assertEquals(List.of(List.of(IO_AE)), UnitRulesRefUtil.collectRulesRefBuckets(mek));
    }

    @Test
    void leavesIntrinsicSystemsWithoutRulesRefsNeutral() {
        BipedMek mek = new BipedMek();
        mek.setWeight(20);
        mek.setArmorType(T_ARMOR_STANDARD);
        mek.setStructureType(T_STRUCTURE_STANDARD);
        mek.setEngine(new Engine(100, Engine.NORMAL_ENGINE, 0));

        assertEquals(List.of(), UnitRulesRefUtil.collectRulesRefBuckets(mek));
    }
}
