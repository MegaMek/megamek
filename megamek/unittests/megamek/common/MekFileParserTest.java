/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.exceptions.LocationFullException;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.BipedMek;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MekFileParserTest {
    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @Test
    void splitMGsBetweenMGAs() throws LocationFullException {
        Mek mek = new BipedMek();
        WeaponMounted machineGunArray1 = (WeaponMounted) mek.addEquipment(EquipmentType.get("ISMGA"),
                Mek.LOC_LEFT_TORSO);
        mek.addEquipment(EquipmentType.get("ISMG"), Mek.LOC_LEFT_TORSO);
        mek.addEquipment(EquipmentType.get("ISMG"), Mek.LOC_LEFT_TORSO);
        WeaponMounted machineGunArray2 = (WeaponMounted) mek.addEquipment(EquipmentType.get("ISMGA"),
                Mek.LOC_LEFT_TORSO);
        mek.addEquipment(EquipmentType.get("ISMG"), Mek.LOC_LEFT_TORSO);
        mek.addEquipment(EquipmentType.get("ISMG"), Mek.LOC_LEFT_TORSO);
        mek.addEquipment(EquipmentType.get("ISMG"), Mek.LOC_LEFT_TORSO);

        MekFileParser.linkMGAs(mek);

        assertEquals(2, machineGunArray1.getBayWeapons().size());
        assertEquals(3, machineGunArray2.getBayWeapons().size());
    }

    @Test
    void loadMGAsFromContiguousBlocks() throws LocationFullException {
        Mek mek = new BipedMek();
        WeaponMounted lightMachineGunArray = (WeaponMounted) mek.addEquipment(EquipmentType.get("ISLMGA"),
                Mek.LOC_LEFT_TORSO);
        WeaponMounted standardMachineGunArray = (WeaponMounted) mek.addEquipment(EquipmentType.get("ISMGA"),
                Mek.LOC_LEFT_TORSO);
        mek.addEquipment(EquipmentType.get("ISMG"), Mek.LOC_LEFT_TORSO);
        mek.addEquipment(EquipmentType.get("ISLightMG"), Mek.LOC_LEFT_TORSO);
        mek.addEquipment(EquipmentType.get("ISLightMG"), Mek.LOC_LEFT_TORSO);
        WeaponMounted lastMG = (WeaponMounted) mek.addEquipment(EquipmentType.get("ISMG"), Mek.LOC_LEFT_TORSO);

        MekFileParser.linkMGAs(mek);

        // The first MGA should load the second and third, and the second MGA only the
        // first
        assertEquals(2, lightMachineGunArray.getBayWeapons().size());
        assertEquals(1, standardMachineGunArray.getBayWeapons().size());
        assertFalse(mek.hasLinkedMGA(lastMG));
    }

    /**
     * Tests for MG Array linking on ProtoMeks, which don't use critical slots. These tests verify the fallback path in
     * linkMGAs() that searches the weapon list directly. Uses LOC_BODY which has no weapon limit for ProtoMeks.
     */
    @Nested
    class ProtoMekMGALinkingTests {

        @Test
        void protoMekMGALinksMGsInSameLocation() throws LocationFullException {
            ProtoMek proto = new ProtoMek();
            WeaponMounted machineGunArray = (WeaponMounted) proto.addEquipment(
                  EquipmentType.get("CLMGA"), ProtoMek.LOC_BODY);
            WeaponMounted mg1 = (WeaponMounted) proto.addEquipment(
                  EquipmentType.get("CLMG"), ProtoMek.LOC_BODY);
            WeaponMounted mg2 = (WeaponMounted) proto.addEquipment(
                  EquipmentType.get("CLMG"), ProtoMek.LOC_BODY);

            MekFileParser.linkMGAs(proto);

            assertEquals(2, machineGunArray.getBayWeapons().size());
            assertTrue(proto.hasLinkedMGA(mg1));
            assertTrue(proto.hasLinkedMGA(mg2));
        }

        @Test
        void protoMekMGAIgnoresMGsInDifferentLocation() throws LocationFullException {
            ProtoMek proto = new ProtoMek();
            WeaponMounted machineGunArray = (WeaponMounted) proto.addEquipment(
                  EquipmentType.get("CLMGA"), ProtoMek.LOC_BODY);
            // MG in same location - should be linked
            WeaponMounted mgBody = (WeaponMounted) proto.addEquipment(
                  EquipmentType.get("CLMG"), ProtoMek.LOC_BODY);
            // MG in different location - should NOT be linked
            WeaponMounted mgArm = (WeaponMounted) proto.addEquipment(
                  EquipmentType.get("CLMG"), ProtoMek.LOC_LEFT_ARM);

            MekFileParser.linkMGAs(proto);

            assertEquals(1, machineGunArray.getBayWeapons().size());
            assertTrue(proto.hasLinkedMGA(mgBody));
            assertFalse(proto.hasLinkedMGA(mgArm));
        }

        @Test
        void protoMekMGAOnlyLinksMatchingRackSize() throws LocationFullException {
            ProtoMek proto = new ProtoMek();
            // Standard MGA (rack size 2) should only link standard MGs
            WeaponMounted machineGunArray = (WeaponMounted) proto.addEquipment(
                  EquipmentType.get("CLMGA"), ProtoMek.LOC_BODY);
            WeaponMounted standardMG = (WeaponMounted) proto.addEquipment(
                  EquipmentType.get("CLMG"), ProtoMek.LOC_BODY);
            // Light MG (rack size 1) should NOT be linked to standard MGA
            WeaponMounted lightMG = (WeaponMounted) proto.addEquipment(
                  EquipmentType.get("CLLightMG"), ProtoMek.LOC_BODY);

            MekFileParser.linkMGAs(proto);

            assertEquals(1, machineGunArray.getBayWeapons().size());
            assertTrue(proto.hasLinkedMGA(standardMG));
            assertFalse(proto.hasLinkedMGA(lightMG));
        }

        @Test
        void protoMekMGALinksMaxFourMGs() throws LocationFullException {
            ProtoMek proto = new ProtoMek();
            WeaponMounted machineGunArray = (WeaponMounted) proto.addEquipment(
                  EquipmentType.get("CLMGA"), ProtoMek.LOC_BODY);
            // Add 5 MGs - only 4 should be linked
            WeaponMounted mg1 = (WeaponMounted) proto.addEquipment(
                  EquipmentType.get("CLMG"), ProtoMek.LOC_BODY);
            WeaponMounted mg2 = (WeaponMounted) proto.addEquipment(
                  EquipmentType.get("CLMG"), ProtoMek.LOC_BODY);
            WeaponMounted mg3 = (WeaponMounted) proto.addEquipment(
                  EquipmentType.get("CLMG"), ProtoMek.LOC_BODY);
            WeaponMounted mg4 = (WeaponMounted) proto.addEquipment(
                  EquipmentType.get("CLMG"), ProtoMek.LOC_BODY);
            WeaponMounted mg5 = (WeaponMounted) proto.addEquipment(
                  EquipmentType.get("CLMG"), ProtoMek.LOC_BODY);

            MekFileParser.linkMGAs(proto);

            assertEquals(4, machineGunArray.getBayWeapons().size());
            // First 4 should be linked, 5th should not
            assertTrue(proto.hasLinkedMGA(mg1));
            assertTrue(proto.hasLinkedMGA(mg2));
            assertTrue(proto.hasLinkedMGA(mg3));
            assertTrue(proto.hasLinkedMGA(mg4));
            assertFalse(proto.hasLinkedMGA(mg5));
        }

        @Test
        void protoMekMultipleMGAsEachMGLinkedOnce() throws LocationFullException {
            // For the fallback path (no critical slots), the first MGA grabs all
            // available MGs up to 4, then the second MGA gets whatever's left.
            // This differs from the critical slot path which uses contiguous blocks.
            ProtoMek proto = new ProtoMek();
            WeaponMounted machineGunArray1 = (WeaponMounted) proto.addEquipment(
                  EquipmentType.get("CLMGA"), ProtoMek.LOC_BODY);
            WeaponMounted mg1 = (WeaponMounted) proto.addEquipment(
                  EquipmentType.get("CLMG"), ProtoMek.LOC_BODY);
            WeaponMounted mg2 = (WeaponMounted) proto.addEquipment(
                  EquipmentType.get("CLMG"), ProtoMek.LOC_BODY);
            WeaponMounted machineGunArray2 = (WeaponMounted) proto.addEquipment(
                  EquipmentType.get("CLMGA"), ProtoMek.LOC_BODY);
            WeaponMounted mg3 = (WeaponMounted) proto.addEquipment(
                  EquipmentType.get("CLMG"), ProtoMek.LOC_BODY);
            WeaponMounted mg4 = (WeaponMounted) proto.addEquipment(
                  EquipmentType.get("CLMG"), ProtoMek.LOC_BODY);
            WeaponMounted mg5 = (WeaponMounted) proto.addEquipment(
                  EquipmentType.get("CLMG"), ProtoMek.LOC_BODY);

            MekFileParser.linkMGAs(proto);

            // First MGA gets first 4 MGs, second MGA gets the 5th
            assertEquals(4, machineGunArray1.getBayWeapons().size());
            assertEquals(1, machineGunArray2.getBayWeapons().size());
            // Each MG should only be linked to one MGA (no double-linking)
            assertTrue(proto.hasLinkedMGA(mg1));
            assertTrue(proto.hasLinkedMGA(mg2));
            assertTrue(proto.hasLinkedMGA(mg3));
            assertTrue(proto.hasLinkedMGA(mg4));
            assertTrue(proto.hasLinkedMGA(mg5));
        }
    }
}
