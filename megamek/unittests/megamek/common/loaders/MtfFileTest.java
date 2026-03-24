/*
 * Copyright (C) 2020-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common.loaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import megamek.common.CriticalSlot;
import megamek.common.enums.Faction;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.Mounted;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.TripodMek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MtfFileTest {
    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    private MtfFile toMtfFile(Mek mek) throws EntityLoadingException {
        if (!mek.hasEngine() || mek.getEngine().getEngineType() == Engine.NONE) {
            mek.setWeight(20.0);
            mek.setEngine(new Engine(100, Engine.NORMAL_ENGINE, 0));
        }
        String mtf = mek.getMtf();
        byte[] bytes = mtf.getBytes();
        InputStream inputStream = new ByteArrayInputStream(bytes);
        return new MtfFile(inputStream);
    }

    @Test
    void testLoadEquipment() throws Exception {
        Mek mek = new BipedMek();
        Mounted<?> mount = Mounted.createMounted(mek, EquipmentType.get("Medium Laser"));
        mount.setOmniPodMounted(true);
        mount.setMekTurretMounted(true);
        mount.setArmored(true);
        mek.addEquipment(mount, Mek.LOC_LEFT_TORSO, true);

        MtfFile loader = toMtfFile(mek);
        Mounted<?> found = loader.getEntity().getCritical(Mek.LOC_LEFT_TORSO, 0).getMount();

        assertEquals(mount.getType(), found.getType());
        assertTrue(found.isRearMounted());
        assertTrue(found.isMekTurretMounted());
        assertTrue(found.isArmored());
    }

    @Test
    void setVGLFacing() throws Exception {
        Mek mek = new BipedMek();
        EquipmentType vgl = EquipmentType.get("ISVehicularGrenadeLauncher");
        mek.addEquipment(vgl, Mek.LOC_LEFT_TORSO).setFacing(0);
        mek.addEquipment(vgl, Mek.LOC_LEFT_TORSO).setFacing(1);
        mek.addEquipment(vgl, Mek.LOC_LEFT_TORSO).setFacing(2);
        mek.addEquipment(vgl, Mek.LOC_LEFT_TORSO, true).setFacing(3);
        mek.addEquipment(vgl, Mek.LOC_LEFT_TORSO).setFacing(4);
        mek.addEquipment(vgl, Mek.LOC_LEFT_TORSO).setFacing(5);

        MtfFile loader = toMtfFile(mek);
        Entity loaded = loader.getEntity();

        assertEquals(0, loaded.getCritical(Mek.LOC_LEFT_TORSO, 0).getMount().getFacing());
        assertEquals(1, loaded.getCritical(Mek.LOC_LEFT_TORSO, 1).getMount().getFacing());
        assertEquals(2, loaded.getCritical(Mek.LOC_LEFT_TORSO, 2).getMount().getFacing());
        assertEquals(3, loaded.getCritical(Mek.LOC_LEFT_TORSO, 3).getMount().getFacing());
        assertEquals(4, loaded.getCritical(Mek.LOC_LEFT_TORSO, 4).getMount().getFacing());
        assertEquals(5, loaded.getCritical(Mek.LOC_LEFT_TORSO, 5).getMount().getFacing());
    }

    @Test
    void loadSuperheavyDoubleSlot() throws Exception {
        Mek mek = new BipedMek();
        mek.setWeight(120.0);
        mek.setEngine(new Engine(360, Engine.NORMAL_ENGINE, 0));
        EquipmentType hs = EquipmentType.get(EquipmentTypeLookup.SINGLE_HS);
        mek.addEquipment(hs, hs, Mek.LOC_LEFT_TORSO, true, true);

        MtfFile loader = toMtfFile(mek);
        CriticalSlot slot = loader.getEntity().getCritical(Mek.LOC_LEFT_TORSO, 0);

        assertEquals(hs, slot.getMount().getType());
        assertEquals(hs, slot.getMount2().getType());
        assertTrue(slot.getMount().isOmniPodMounted());
        assertTrue(slot.getMount2().isOmniPodMounted());
        assertTrue(slot.isArmored());
    }

    // Exercises new MtfFile.java code
    // We should be able to load a Size 24 CommsGear component into 12 Superheavy
    // slots, filling
    // the Left torso.
    @Test
    void loadSuperheavyVariableSizeSlot() throws Exception {
        Mek mek = new TripodMek();
        double varSize = 24.0;
        mek.setWeight(150.0);
        mek.setEngine(new Engine(300, Engine.NORMAL_ENGINE, 0));
        EquipmentType commsGear = EquipmentType.get("CommsGear");
        Mounted<?> mount = mek.addEquipment(commsGear, Mek.LOC_LEFT_TORSO, false);
        mount.setSize(varSize);

        MtfFile loader = toMtfFile(mek);
        CriticalSlot slot = loader.getEntity().getCritical(Mek.LOC_LEFT_TORSO, 0);

        assertEquals(commsGear, slot.getMount().getType());
        assertEquals(varSize, slot.getMount().getSize());
        assertFalse(slot.getMount().isOmniPodMounted());
        assertFalse(slot.isArmored());
    }

    // Should _not_ allow loading size 25 CommsGear; 25 / 2.0 -> 13 crits, 1 more
    // than allowed
    @Test
    void ExceptionLoadSuperheavyVariableSizeSlot() throws Exception {
        Mek mek = new TripodMek();
        double varSize = 25.0;
        mek.setWeight(150.0);
        mek.setEngine(new Engine(300, Engine.NORMAL_ENGINE, 0));
        EquipmentType commsGear = EquipmentType.get("CommsGear");
        Mounted<?> mount = mek.addEquipment(commsGear, Mek.LOC_LEFT_TORSO, false);
        mount.setSize(varSize);
        MtfFile loader = toMtfFile(mek);

        Exception e = assertThrowsExactly(
              Exception.class,
              () -> loader.getEntity().getCritical(Mek.LOC_LEFT_TORSO, 0));
        assertEquals(
              "java.lang.ArrayIndexOutOfBoundsException: Index 12 out of bounds for length 12",
              e.getMessage());

    }

    private MtfFile toMtfFile(String mtf) throws EntityLoadingException {
        byte[] bytes = mtf.getBytes();
        InputStream inputStream = new ByteArrayInputStream(bytes);
        return new MtfFile(inputStream);
    }

    /**
     * Test that inverted Cockpit/Sensors in the head are corrected on load. Some MTF files have the head layout: LS,
     * Cockpit, Sensors (wrong) instead of the correct: LS, Sensors, Cockpit.
     */
    @Test
    void testInvertedCockpitSensorsInHead() throws Exception {
        Mek mek = new BipedMek();
        mek.setWeight(20.0);
        mek.setEngine(new Engine(100, Engine.NORMAL_ENGINE, 0));
        mek.addCockpit();
        mek.addGyro();
        mek.addEngineCrits();
        String mtf = mek.getMtf();
        // Swap Cockpit and Sensors in the Head section to simulate a broken file
        // Correct order: Life Support / Sensors / Cockpit / ... / Sensors / Life Support
        // Broken order: Life Support / Cockpit / Sensors / ... / Sensors / Life Support
        String correctHead = "Life Support\nSensors\nCockpit\n-Empty-\nSensors\nLife Support";
        assertTrue(mtf.contains(correctHead),
              "Expected correct head layout in generated MTF to set up the test");
        mtf = mtf.replace(correctHead,
              "Life Support\nCockpit\nSensors\n-Empty-\nSensors\nLife Support");

        MtfFile loader = toMtfFile(mtf);
        Entity loaded = loader.getEntity();

        // Verify the correct positions despite the file having them swapped
        assertEquals(Mek.SYSTEM_LIFE_SUPPORT, loaded.getCritical(Mek.LOC_HEAD, 0).getIndex());
        assertEquals(Mek.SYSTEM_SENSORS, loaded.getCritical(Mek.LOC_HEAD, 1).getIndex());
        assertEquals(Mek.SYSTEM_COCKPIT, loaded.getCritical(Mek.LOC_HEAD, 2).getIndex());
        assertEquals(Mek.SYSTEM_SENSORS, loaded.getCritical(Mek.LOC_HEAD, 4).getIndex());
        assertEquals(Mek.SYSTEM_LIFE_SUPPORT, loaded.getCritical(Mek.LOC_HEAD, 5).getIndex());
    }

    /**
     * Test that incorrectly split XL gyro slots in CT are corrected on load. Some files have Gyro at slots 3-6 + 10-11
     * with Engine at 7-9 (split gyro), but the correct XL gyro layout is Gyro at slots 3-8 with Engine at 0-2 and
     * 9-11.
     */
    @Test
    void testSplitXLGyroIsCorrected() throws Exception {
        Mek mek = new BipedMek(Mek.GYRO_XL, Mek.COCKPIT_STANDARD);
        mek.setWeight(50.0);
        mek.setEngine(new Engine(200, Engine.NORMAL_ENGINE, 0));
        mek.addCockpit();
        mek.setCockpitType(Mek.COCKPIT_STANDARD);
        mek.addXLGyro();
        String mtf = mek.getMtf();

        // Correct XL Gyro CT: Engine(0-2), Gyro(3-8), Engine(9-11)
        String correctCT = "Fusion Engine\nFusion Engine\nFusion Engine\n"
              + "Gyro\nGyro\nGyro\nGyro\nGyro\nGyro\n"
              + "Fusion Engine\nFusion Engine\nFusion Engine";
        assertTrue(mtf.contains(correctCT),
              "Expected correct XL Gyro CT layout in generated MTF to set up the test");

        // Broken: Engine(0-2), Gyro(3-6), Engine(7-9), Gyro(10-11) - split gyro
        String brokenCT = "Fusion Engine\nFusion Engine\nFusion Engine\n"
              + "Gyro\nGyro\nGyro\nGyro\n"
              + "Fusion Engine\nFusion Engine\nFusion Engine\n"
              + "Gyro\nGyro";

        mtf = mtf.replace(correctCT, brokenCT);

        MtfFile loader = toMtfFile(mtf);
        Entity loaded = loader.getEntity();

        // Verify ALL CT slots are in the correct XL gyro layout
        for (int i = 0; i <= 2; i++) {
            CriticalSlot slot = loaded.getCritical(Mek.LOC_CENTER_TORSO, i);
            assertEquals(CriticalSlot.TYPE_SYSTEM, slot.getType(), "CT slot " + i + " should be system");
            assertEquals(Mek.SYSTEM_ENGINE, slot.getIndex(), "CT slot " + i + " should be Engine");
        }
        for (int i = 3; i <= 8; i++) {
            CriticalSlot slot = loaded.getCritical(Mek.LOC_CENTER_TORSO, i);
            assertEquals(CriticalSlot.TYPE_SYSTEM, slot.getType(), "CT slot " + i + " should be system");
            assertEquals(Mek.SYSTEM_GYRO, slot.getIndex(), "CT slot " + i + " should be Gyro");
        }
        for (int i = 9; i <= 11; i++) {
            CriticalSlot slot = loaded.getCritical(Mek.LOC_CENTER_TORSO, i);
            assertEquals(CriticalSlot.TYPE_SYSTEM, slot.getType(), "CT slot " + i + " should be system");
            assertEquals(Mek.SYSTEM_ENGINE, slot.getIndex(), "CT slot " + i + " should be Engine");
        }
    }

    /**
     * Test that armored system components preserve their armored status even though system slots are regenerated and
     * not read from file positions.
     */
    @Test
    void testArmoredSystemCritsPreserved() throws Exception {
        Mek mek = new BipedMek();
        mek.setWeight(20.0);
        mek.setEngine(new Engine(100, Engine.NORMAL_ENGINE, 0));
        mek.addCockpit();
        mek.addGyro();
        mek.addEngineCrits();
        // Armor all head system crits
        for (int i = 0; i < mek.getNumberOfCriticalSlots(Mek.LOC_HEAD); i++) {
            CriticalSlot slot = mek.getCritical(Mek.LOC_HEAD, i);
            if (slot != null && slot.getType() == CriticalSlot.TYPE_SYSTEM) {
                slot.setArmored(true);
            }
        }

        MtfFile loader = toMtfFile(mek);
        Entity loaded = loader.getEntity();

        // All head system crits should be armored
        for (int i = 0; i < loaded.getNumberOfCriticalSlots(Mek.LOC_HEAD); i++) {
            CriticalSlot slot = loaded.getCritical(Mek.LOC_HEAD, i);
            if (slot != null && slot.getType() == CriticalSlot.TYPE_SYSTEM) {
                assertTrue(slot.isArmored(), "Head slot " + i + " should be armored");
            }
        }
    }

    /**
     * Test that equipment is preserved when the file has XL engine crits at the END of a side torso instead of the
     * beginning. The remapping should shift equipment to fill the available slots after the correctly-placed engine
     * crits.
     */
    @Test
    void testSideTorsoEngineAtEndPreservesEquipment() throws Exception {
        Mek mek = new BipedMek();
        mek.setWeight(100.0);
        mek.setEngine(new Engine(300, Engine.XL_ENGINE, 0));
        mek.addCockpit();
        mek.addGyro();
        mek.addEngineCrits();
        // Add 7 crits of Gauss Rifle + 2 crits of SRM6 to LT
        EquipmentType gauss = EquipmentType.get("ISGaussRifle");
        EquipmentType srm6 = EquipmentType.get("SRM 6");
        mek.addEquipment(gauss, Mek.LOC_LEFT_TORSO, false);
        mek.addEquipment(srm6, Mek.LOC_LEFT_TORSO, false);
        String mtf = mek.getMtf();

        // Correct layout: Engine at front (slots 0-2), then equipment
        String correctLT = "Fusion Engine\nFusion Engine\nFusion Engine\n"
              + "ISGaussRifle\nISGaussRifle\nISGaussRifle\nISGaussRifle\n"
              + "ISGaussRifle\nISGaussRifle\nISGaussRifle\n"
              + "SRM 6\nSRM 6";
        assertTrue(mtf.contains(correctLT),
              "Expected correct LT layout in generated MTF to set up the test");

        // Broken: equipment first, engine at end (as the user's original file had)
        String brokenLT = "ISGaussRifle\nISGaussRifle\nISGaussRifle\nISGaussRifle\n"
              + "ISGaussRifle\nISGaussRifle\nISGaussRifle\n"
              + "SRM 6\nSRM 6\n"
              + "Fusion Engine\nFusion Engine\nFusion Engine";
        mtf = mtf.replace(correctLT, brokenLT);

        MtfFile loader = toMtfFile(mtf);
        Entity loaded = loader.getEntity();

        // Engine should be at slots 0-2 (correct position)
        for (int i = 0; i <= 2; i++) {
            CriticalSlot slot = loaded.getCritical(Mek.LOC_LEFT_TORSO, i);
            assertEquals(CriticalSlot.TYPE_SYSTEM, slot.getType(), "LT slot " + i + " should be system");
            assertEquals(Mek.SYSTEM_ENGINE, slot.getIndex(), "LT slot " + i + " should be Engine");
        }

        // Equipment should fill slots 3-11 with all 7 Gauss + 2 SRM6 crits preserved
        int gaussCount = 0;
        int srm6Count = 0;
        for (int i = 3; i < loaded.getNumberOfCriticalSlots(Mek.LOC_LEFT_TORSO); i++) {
            CriticalSlot slot = loaded.getCritical(Mek.LOC_LEFT_TORSO, i);
            if (slot != null && slot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                if (slot.getMount().getType().equals(gauss)) {
                    gaussCount++;
                } else if (slot.getMount().getType().equals(srm6)) {
                    srm6Count++;
                }
            }
        }
        assertEquals(7, gaussCount, "All 7 Gauss Rifle crits should be preserved");
        assertEquals(2, srm6Count, "All 2 SRM 6 crits should be preserved");
    }

    /**
     * Test that a GYRO_NONE unit places all 6 engine crits contiguously (0-5) in CT, rather than splitting them around
     * a nonexistent gyro.
     */
    @Test
    void testGyroNoneEngineIsContiguous() throws Exception {
        Mek mek = new BipedMek();
        mek.setWeight(45.0);
        mek.setEngine(new Engine(315, Engine.XL_ENGINE, 0));
        mek.setGyroType(Mek.GYRO_NONE);
        mek.setCockpitType(Mek.COCKPIT_INTERFACE);
        mek.addCockpit();
        mek.addEngineCrits();

        MtfFile loader = toMtfFile(mek);
        Entity loaded = loader.getEntity();

        // CT slots 0-5 should all be engine
        for (int i = 0; i <= 5; i++) {
            CriticalSlot slot = loaded.getCritical(Mek.LOC_CENTER_TORSO, i);
            assertNotNull(slot, "CT slot " + i + " should not be null");
            assertEquals(CriticalSlot.TYPE_SYSTEM, slot.getType(), "CT slot " + i + " should be system");
            assertEquals(Mek.SYSTEM_ENGINE, slot.getIndex(), "CT slot " + i + " should be Engine");
        }
        // CT slot 6 should NOT be engine
        CriticalSlot slot6 = loaded.getCritical(Mek.LOC_CENTER_TORSO, 6);
        assertTrue(slot6 == null || slot6.getIndex() != Mek.SYSTEM_ENGINE,
              "CT slot 6 should not be engine (gyro is None, engine should be contiguous 0-5)");
    }

    /**
     * Test that the techFaction field round-trips through MTF save/load.
     */
    @Test
    void testFactionRoundTrip() throws Exception {
        Mek mek = new BipedMek();
        mek.setTechFaction(Faction.DC);

        MtfFile loader = toMtfFile(mek);
        Entity loaded = loader.getEntity();

        assertEquals(Faction.DC, loaded.getTechFaction(),
              "Tech faction should survive MTF round-trip");
    }

    /**
     * Test that Faction.NONE is not written and loads back as NONE.
     */
    @Test
    void testFactionNoneNotWritten() throws Exception {
        Mek mek = new BipedMek();
        mek.setTechFaction(Faction.NONE);

        String mtf = mek.getMtf();
        assertFalse(mtf.contains(MtfFile.FACTION),
              "NONE faction should not produce a faction: line in MTF output");

        MtfFile loader = toMtfFile(mek);
        Entity loaded = loader.getEntity();

        assertEquals(Faction.NONE, loaded.getTechFaction(),
              "Missing faction line should load as NONE");
    }

    /**
     * Test that the Clan CASE opt-out locations round-trip through MTF save/load with multiple locations.
     */
    @Test
    void testClanCaseOptOutRoundTrip() throws Exception {
        Mek mek = new BipedMek();
        mek.addClanCaseOptOut(Mek.LOC_LEFT_TORSO);
        mek.addClanCaseOptOut(Mek.LOC_RIGHT_TORSO);
        mek.addClanCaseOptOut(Mek.LOC_CENTER_TORSO);

        MtfFile loader = toMtfFile(mek);
        Entity loaded = loader.getEntity();
        Mek loadedMek = (Mek) loaded;

        assertTrue(loadedMek.isClanCaseOptedOut(Mek.LOC_LEFT_TORSO),
              "LT should be opted out of Clan CASE");
        assertTrue(loadedMek.isClanCaseOptedOut(Mek.LOC_RIGHT_TORSO),
              "RT should be opted out of Clan CASE");
        assertTrue(loadedMek.isClanCaseOptedOut(Mek.LOC_CENTER_TORSO),
              "CT should be opted out of Clan CASE");
        assertFalse(loadedMek.isClanCaseOptedOut(Mek.LOC_HEAD),
              "HD should not be opted out of Clan CASE");
        assertFalse(loadedMek.isClanCaseOptedOut(Mek.LOC_LEFT_ARM),
              "LA should not be opted out of Clan CASE");
        assertEquals(3, loadedMek.getClanCaseOptOutLocations().size(),
              "Exactly 3 locations should be opted out");
    }

    /**
     * Test that an empty Clan CASE opt-out set is not written and loads correctly.
     */
    @Test
    void testClanCaseOptOutEmptyNotWritten() throws Exception {
        Mek mek = new BipedMek();
        // No opt-outs added

        String mtf = mek.getMtf();
        assertFalse(mtf.contains(MtfFile.CLAN_CASE_OPT_OUT),
              "Empty opt-out set should not produce a clancaseoptedoutlocs: line");

        MtfFile loader = toMtfFile(mek);
        Mek loadedMek = (Mek) loader.getEntity();

        assertFalse(loadedMek.hasAnyClanCaseOptOut(),
              "Loaded mek should have no Clan CASE opt-outs");
    }

    /**
     * Test that the Clan CASE opt-out line is written in deterministic (sorted) order.
     */
    @Test
    void testClanCaseOptOutSortedOutput() throws Exception {
        Mek mek = new BipedMek();
        // Add in reverse order to exercise sorting
        mek.addClanCaseOptOut(Mek.LOC_RIGHT_LEG);   // index 6
        mek.addClanCaseOptOut(Mek.LOC_LEFT_TORSO);   // index 3
        mek.addClanCaseOptOut(Mek.LOC_CENTER_TORSO);  // index 1

        String mtf = mek.getMtf();

        // Extract the clancaseoptedoutlocs line
        String marker = MtfFile.CLAN_CASE_OPT_OUT;
        int start = mtf.indexOf(marker);
        assertTrue(start >= 0, "MTF should contain " + marker);
        String afterMarker = mtf.substring(start + marker.length());
        String locationLine = afterMarker.split("\\R")[0];
        String[] abbrs = locationLine.split(",");

        // The abbreviations should be sorted by location index: CT(1), LT(3), RL(6)
        assertEquals(3, abbrs.length);
        assertEquals(mek.getLocationAbbr(Mek.LOC_CENTER_TORSO), abbrs[0]);
        assertEquals(mek.getLocationAbbr(Mek.LOC_LEFT_TORSO), abbrs[1]);
        assertEquals(mek.getLocationAbbr(Mek.LOC_RIGHT_LEG), abbrs[2]);
    }

    @Test
    void testFluffDateRoundTrip() throws Exception {
        Mek mek = new BipedMek();
        String expectedDate = "2026-03-01 14:30:00";
        mek.getFluff().setFluffDate(expectedDate);

        MtfFile loader = toMtfFile(mek);
        Entity loaded = loader.getEntity();

        assertEquals(expectedDate, loaded.getFluff().getFluffDate());
    }

    @Test
    void testFluffDateEmittedWhenSet() throws Exception {
        Mek mek = new BipedMek();
        mek.setWeight(20.0);
        mek.setEngine(new Engine(100, Engine.NORMAL_ENGINE, 0));

        mek.getFluff().setFluffDate("2026-03-01 14:30:00");
        String mtf = mek.getMtf();
        assertTrue(mtf.contains("fluffdate:2026-03-01 14:30:00"),
              "MTF output should contain fluffdate field");
    }

    @Test
    void testFluffDateNotEmittedWhenEmpty() throws Exception {
        Mek mek = new BipedMek();
        mek.setWeight(20.0);
        mek.setEngine(new Engine(100, Engine.NORMAL_ENGINE, 0));

        String mtf = mek.getMtf();
        assertFalse(mtf.contains("fluffdate:"),
              "MTF output should not contain fluffdate field when empty");
    }

    @Test
    void testLegacyFluffDateCommentParsed() throws Exception {
        Mek mek = new BipedMek();
        mek.setWeight(20.0);
        mek.setEngine(new Engine(100, Engine.NORMAL_ENGINE, 0));
        String mtf = mek.getMtf();

        // Insert a legacy # Fluff Date: comment line before the first non-empty line
        mtf = "# Fluff Date: 2026-02-26 19:50:04\n" + mtf;

        MtfFile loader = toMtfFile(mtf);
        Entity loaded = loader.getEntity();

        assertEquals("2026-02-26 19:50:04", loaded.getFluff().getFluffDate());
    }

    @Test
    void testFluffDateFieldOverridesLegacyComment() throws Exception {
        Mek mek = new BipedMek();
        mek.setWeight(20.0);
        mek.setEngine(new Engine(100, Engine.NORMAL_ENGINE, 0));

        mek.getFluff().setFluffDate("2026-03-01 10:00:00");
        String mtf = mek.getMtf();

        // Prepend a legacy comment with a different date
        mtf = "# Fluff Date: 2025-01-01 00:00:00\n" + mtf;

        MtfFile loader = toMtfFile(mtf);
        Entity loaded = loader.getEntity();

        // The fluffdate: field should win over the legacy comment
        assertEquals("2026-03-01 10:00:00", loaded.getFluff().getFluffDate());
    }
}
