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
package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertEquals;

import megamek.common.MPCalculationSetting;
import megamek.common.TechConstants;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.Mounted;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MekJumpMPTest {
    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @Test
    void frankenMekJumpJetsClampLocationTonnageToCenterTorsoClass() throws Exception {
        Mek mek = createFrankenMek(60, 240);
        EquipmentType jumpJet = EquipmentType.get(EquipmentTypeLookup.JUMP_JET);

        mek.setFrankenMekStructureTonnage(Mek.LOC_RIGHT_TORSO, 25);
        mek.setFrankenMekStructureTonnage(Mek.LOC_RIGHT_LEG, 100);

        mek.addEquipment(jumpJet, Mek.LOC_RIGHT_TORSO);
        mek.addEquipment(jumpJet, Mek.LOC_RIGHT_TORSO);
        mek.addEquipment(jumpJet, Mek.LOC_RIGHT_LEG);

        assertEquals(2, mek.getJumpMP(MPCalculationSetting.NO_GRAVITY));
    }

    @Test
    void frankenMekJumpJetsDropFractionalMovementWhenJetIsDestroyed() throws Exception {
        Mek mek = createFrankenMek(60, 240);
        EquipmentType jumpJet = EquipmentType.get(EquipmentTypeLookup.JUMP_JET);

        mek.setFrankenMekStructureTonnage(Mek.LOC_RIGHT_ARM, 25);
        mek.setFrankenMekStructureTonnage(Mek.LOC_LEFT_ARM, 25);
        mek.setFrankenMekStructureTonnage(Mek.LOC_RIGHT_TORSO, 25);
        mek.setFrankenMekStructureTonnage(Mek.LOC_LEFT_TORSO, 25);

        assertEquals(0.5, jumpJet.getTonnage(mek, Mek.LOC_RIGHT_ARM, 1.0));
        assertEquals(1.0, jumpJet.getTonnage(mek, Mek.LOC_CENTER_TORSO, 1.0));

        Mounted<?> damagedJumpJet = mek.addEquipment(jumpJet, Mek.LOC_RIGHT_ARM);
        mek.addEquipment(jumpJet, Mek.LOC_RIGHT_ARM);
        mek.addEquipment(jumpJet, Mek.LOC_LEFT_ARM);
        mek.addEquipment(jumpJet, Mek.LOC_LEFT_ARM);
        mek.addEquipment(jumpJet, Mek.LOC_RIGHT_TORSO);
        mek.addEquipment(jumpJet, Mek.LOC_LEFT_TORSO);

        assertEquals(3, mek.getJumpMP(MPCalculationSetting.NO_GRAVITY));

        damagedJumpJet.setDestroyed(true);

        assertEquals(2, mek.getJumpMP(MPCalculationSetting.NO_GRAVITY));
    }

    @Test
    void linkedFrankenMekLocationSourceClearsWhenStructureChanges() {
        Mek mek = createFrankenMek(60, 240);

        mek.linkFrankenMekLocationToSource(Mek.LOC_RIGHT_TORSO, "Archer XYZ");
        mek.setFrankenMekStructureTonnage(Mek.LOC_RIGHT_TORSO, 65);

        assertEquals("", mek.getFrankenMekLocationSourceDisplayName(Mek.LOC_RIGHT_TORSO));
    }

    private Mek createFrankenMek(int weight, int engineRating) {
        Mek mek = new BipedMek();
        mek.setTechLevel(TechConstants.T_IS_EXPERIMENTAL);
        mek.setWeight(weight);
        mek.setEngine(new Engine(engineRating, Engine.NORMAL_ENGINE, 0));
        mek.setFrankenMek(true);
        mek.setFrankenMekStructureTonnage(Mek.LOC_CENTER_TORSO, weight);
        return mek;
    }
}