/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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

import static megamek.common.bays.Bay.UNSET_BAY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.HashSet;

import megamek.common.TechConstants;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.bays.BattleArmorBay;
import megamek.common.bays.Bay;
import megamek.common.bays.InfantryBay;
import megamek.common.bays.MekBay;
import megamek.common.enums.Faction;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.loaders.BLKFile.ParsedBayInfo;
import megamek.common.units.DropShuttleBay;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Jumpship;
import megamek.common.units.NavalRepairFacility;
import megamek.common.units.PlatoonType;
import megamek.common.units.Tank;
import megamek.common.util.BuildingBlock;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BLKFileTest {

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    /**
     * Strips the bay type identifier from the bay string.
     *
     * @param bay The Bay being parsed
     *
     * @return The part of the bay string containing the parameters (size, doors, num, etc.)
     */
    private String getBayNumbers(Bay bay) {
        String bayString = bay.toString();
        return bayString.substring(bayString.indexOf(Bay.FIELD_SEPARATOR) + 1);
    }

    @Test
    void parseBayDataAssignsMissingBayNumber() throws BLKDecodingException {
        final double SIZE = 2.0;
        final int DOORS = 1;
        String bayString = SIZE + ":" + DOORS;
        HashSet<Integer> bayNums = new HashSet<>();
        bayNums.add(0);
        bayNums.add(1);

        ParsedBayInfo pbi = new ParsedBayInfo(bayString, bayNums);
        assertEquals(SIZE, pbi.getSize(), 0.01);
        assertEquals(DOORS, pbi.getDoors());
        assertEquals(2, pbi.getBayNumber());
    }

    @Test
    void parseBayDataFixesDuplicateBayNumber() throws BLKDecodingException {
        final double SIZE = 2.0;
        final int DOORS = 1;
        Bay bay = new MekBay(SIZE, DOORS, 1);
        HashSet<Integer> bayNums = new HashSet<>();
        bayNums.add(0);
        bayNums.add(1);

        ParsedBayInfo pbi = new ParsedBayInfo(getBayNumbers(bay), bayNums);

        assertEquals(SIZE, pbi.getSize(), 0.01);
        assertEquals(DOORS, pbi.getDoors());
        assertEquals(2, pbi.getBayNumber());
    }

    @Test
    void parseBayTypeIndicatorWithBayNumber() {
        Bay bay = new BattleArmorBay(2.0, 1, 1, false, true);

        try {
            ParsedBayInfo pbi = new ParsedBayInfo(getBayNumbers(bay), new HashSet<>());

            assertTrue(pbi.isComstarBay());
            assertEquals(1, pbi.getBayNumber());
        } catch (BLKDecodingException e) {
            fail("Unexpected exception!");
        }
    }

    @Test
    void parseBayTypeIndicatorWithoutBayNumber() {
        Bay bay = new BattleArmorBay(2.0, 1, 4, false, true);
        String numbers = getBayNumbers(bay).replace(":4", String.format(":%s", UNSET_BAY));
        HashSet<Integer> bayNums = new HashSet<>();
        bayNums.add(0);
        bayNums.add(1);

        try {
            ParsedBayInfo pbi = new ParsedBayInfo(numbers, bayNums);

            assertTrue(pbi.isComstarBay());
            assertEquals(2, pbi.getBayNumber());
        } catch (BLKDecodingException e) {
            fail("Unexpected exception!");
        }
    }

    @Test
    void parseFootInfantryBay() {
        Bay bay = new InfantryBay(2.0, 1, 0, PlatoonType.FOOT);

        try {
            ParsedBayInfo pbi = new ParsedBayInfo(getBayNumbers(bay), new HashSet<>());

            assertEquals(PlatoonType.FOOT, pbi.getPlatoonType());
        } catch (BLKDecodingException e) {
            fail("Unexpected exception!");
        }
    }

    @Test
    void parseJumpInfantryBay() {
        Bay bay = new InfantryBay(2.0, 1, 0, PlatoonType.JUMP);

        try {
            ParsedBayInfo pbi = new ParsedBayInfo(getBayNumbers(bay), new HashSet<>());

            assertEquals(PlatoonType.JUMP, pbi.getPlatoonType());
        } catch (BLKDecodingException e) {
            fail("Unexpected exception!");
        }
    }

    @Test
    void parseMotorizedInfantryBay() {
        Bay bay = new InfantryBay(2.0, 1, 0, PlatoonType.MOTORIZED);

        try {
            ParsedBayInfo pbi = new ParsedBayInfo(getBayNumbers(bay), new HashSet<>());

            assertEquals(PlatoonType.MOTORIZED, pbi.getPlatoonType());
        } catch (BLKDecodingException e) {
            fail("Unexpected exception!");
        }
    }

    @Test
    void parseMechanizedInfantryBay() {
        Bay bay = new InfantryBay(2.0, 1, 0, PlatoonType.MECHANIZED);

        try {
            ParsedBayInfo pbi = new ParsedBayInfo(getBayNumbers(bay), new HashSet<>());

            assertEquals(PlatoonType.MECHANIZED, pbi.getPlatoonType());
        } catch (BLKDecodingException e) {
            fail("Unexpected exception!");
        }
    }

    @Test
    void parseDropShuttleBay() throws BLKDecodingException {
        Bay bay = new DropShuttleBay(1, UNSET_BAY, Jumpship.LOC_AFT);

        ParsedBayInfo pbi = new ParsedBayInfo(getBayNumbers(bay), new HashSet<>());

        assertEquals(1, pbi.getDoors());
        assertEquals(1, pbi.getBayNumber());
        assertEquals(Jumpship.LOC_AFT, pbi.getFacing());
    }

    @Test
    void parseNavalRepairFacility() throws BLKDecodingException {
        final double SIZE = 5000.0;
        final int DOORS = 2;
        Bay bay = new NavalRepairFacility(SIZE, DOORS, UNSET_BAY, Jumpship.LOC_AFT, true);

        ParsedBayInfo pbi = new ParsedBayInfo(getBayNumbers(bay), new HashSet<>());

        assertEquals(SIZE, pbi.getSize(), 0.01);
        assertEquals(DOORS, pbi.getDoors());
        assertEquals(1, pbi.getBayNumber());
        assertEquals(Jumpship.LOC_AFT, pbi.getFacing());
    }

    @Test
    void decodeValidPlatoonTypes() throws BLKDecodingException {
        final String[] types = { "foot", "jump", "mechanized", "motorized", "" };
        final PlatoonType[] platoonTypes = {
              PlatoonType.FOOT,
              PlatoonType.JUMP,
              PlatoonType.MECHANIZED,
              PlatoonType.MOTORIZED,
              PlatoonType.FOOT
        };

        for (int i = 0; i < types.length; i++) {
            assertEquals(platoonTypes[i], ParsedBayInfo.decodePlatoonType(types[i]));
        }
    }

    @Test
    void decodeInvalidPlatoonTypeThrows() {
        assertThrows(
              BLKDecodingException.class,
              () -> ParsedBayInfo.decodePlatoonType("FEeeTS"));
    }

    @Test
    void normalizeInvalidNumbersThrows() {
        String invalidNumbers = "10.0:0:1:c*:extra:fields:throw";
        assertThrows(
              BLKDecodingException.class,
              () -> ParsedBayInfo.normalizeTransporterNumbers(invalidNumbers));
    }

    private boolean confirmTransporterNumbers(String numbers, String[] expNumArray) {
        // Verifies matches between array generated from numbers list and expected array
        try {
            String[] genNumArray = ParsedBayInfo.normalizeTransporterNumbers(numbers);
            assertEquals(expNumArray.length, genNumArray.length);
            for (int i = 0; i < genNumArray.length; i++) {
                assertEquals(expNumArray[i], genNumArray[i],
                      String.format("Checking index %s of '%s' failed", i, numbers));
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Test
    void normalizeNewNumbersFormatReturnsSameValues() {
        String numbers = "1000.0:1:2::-1:2"; // 1000.0 ton Clan BA bay with one door
        String[] expNumArray = numbers.split(Bay.FIELD_SEPARATOR);

        assertTrue(confirmTransporterNumbers(numbers, expNumArray));

    }

    @Test
    void normalizeOldTransporterFormats() {
        // expected format of "numbers" string:
        // 0:1:2:3:4:5
        // Field 0 is the size of the bay, in tons or # of units and is required
        // Field 1 is the number of doors in the bay, and is required
        // Field 2 is the bay number and is required; default value of "-1" is "unset"
        // Field 3 is used to record infantry platoon type; default is an empty string
        // Field 4 is an int recording facing; default is string representation of
        // entity.LOC_NONE
        // Field 5 is a bitmap recording status like tech type, ComStar bay; default is
        // "0"
        // numbersArray is in old format; expNumbersArray is an array of String[] in new
        // format
        String[] numbersArray = {
              "1.0:0", // Size: 1.0; Doors: 0
              "2.0:1", // Size: 2.0; Doors: 1
              "3.0:1:-1", // Size: 3.0; Doors: 1; Bay#: -1 (unset)
              "4.0:2:0", // Size: 4.0; Doors: 2; Bay#: 0
              "5.0:1:1", // Size: 5.0; Doors: 1; Bay#: 1
              "6.0:0:c*", // Size: 6.0; Doors: 0; ComStar type = set
              "7.0:1:Foot", // Size: 7.0; Doors: 1; infantry bay type FOOT
              "8.0:0:1:Jump", // Size: 8.0; Doors: 0; Bay#: 1; infantry bay type JUMP
              "9.0:1:2:f1", // Size: 9.0; Doors: 1; Bay#: 2; Facing: 1
        };
        String[][] expNumbersArray = {
              { "1.0", "0", "-1", "", "-1", "0" },
              { "2.0", "1", "-1", "", "-1", "0" },
              { "3.0", "1", "-1", "", "-1", "0" },
              { "4.0", "2", "0", "", "-1", "0" },
              { "5.0", "1", "1", "", "-1", "0" },
              { "6.0", "0", "-1", "", "-1", "1" },
              { "7.0", "1", "-1", "Foot", "-1", "0" },
              { "8.0", "0", "1", "Jump", "-1", "0" },
              { "9.0", "1", "2", "", "1", "0" }
        };
        boolean matched;
        for (int i = 0; i < numbersArray.length; i++) {
            matched = confirmTransporterNumbers(numbersArray[i], expNumbersArray[i]);
            assertTrue(matched);

        }

    }

    /**
     * Creates a minimal Tank entity for roundtrip testing.
     */
    private Tank createMinimalTank() {
        Tank tank = new Tank();
        tank.setChassis("Test");
        tank.setModel("Tank");
        tank.setWeight(20.0);
        tank.setYear(3025);
        tank.setTechLevel(TechConstants.T_INTRO_BOX_SET);
        tank.setMovementMode(EntityMovementMode.TRACKED);
        tank.setEngine(new Engine(100, Engine.NORMAL_ENGINE, Engine.TANK_ENGINE));
        tank.setOriginalWalkMP(5);
        tank.setArmorType(EquipmentType.T_ARMOR_STANDARD);
        tank.setArmorTechLevel(TechConstants.T_INTRO_BOX_SET);
        tank.autoSetInternal();
        tank.initializeArmor(10, Tank.LOC_FRONT);
        tank.initializeArmor(10, Tank.LOC_RIGHT);
        tank.initializeArmor(10, Tank.LOC_LEFT);
        tank.initializeArmor(10, Tank.LOC_REAR);
        return tank;
    }

    @Test
    void techFactionRoundtripsThroughBLK() throws Exception {
        Tank tank = createMinimalTank();
        tank.setTechFaction(Faction.DC);

        // Save to BuildingBlock and reload
        BuildingBlock blk = BLKFile.getBlock(tank);
        BLKTankFile loader = new BLKTankFile(blk);
        Tank loaded = (Tank) loader.getEntity();

        assertEquals(Faction.DC, loaded.getTechFaction(),
              "Tech faction should survive BLK roundtrip");
    }

    @Test
    void noneFactionIsNotWrittenToBLK() throws Exception {
        Tank tank = createMinimalTank();
        // Faction.NONE is the default; make sure it is not written
        assertEquals(Faction.NONE, tank.getTechFaction());

        BuildingBlock blk = BLKFile.getBlock(tank);
        BLKTankFile loader = new BLKTankFile(blk);
        Tank loaded = (Tank) loader.getEntity();

        assertEquals(Faction.NONE, loaded.getTechFaction(),
              "NONE faction should remain NONE after roundtrip");
    }

    /**
     * Loads a BattleArmor entity from the test resources directory.
     */
    private BattleArmor loadBattleArmor(String filename) throws EntityLoadingException {
        File file = new File("testresources/megamek/common/units/" + filename);
        MekFileParser parser = new MekFileParser(file);
        Entity entity = parser.getEntity();
        assertNotNull(entity, "Failed to load entity from " + filename);
        assertTrue(entity instanceof BattleArmor, "Entity should be BattleArmor");
        return (BattleArmor) entity;
    }

    /**
     * Verifies that a BattleArmor unit with a one-shot weapon (which creates LOC_NONE ammo) can be saved and reloaded
     * without the ammo appearing in the failed equipment list. Regression test for the :Shots# suffix not being
     * stripped in loadSlotlessEquipment().
     */
    @Test
    void battleArmorOneShotAmmoRoundTrip() throws Exception {
        // Load BA with one-shot SRM3 - this creates auto-linked ammo at LOC_NONE
        BattleArmor original = loadBattleArmor("Afreet Med BA (HH) (Sqd4).blk");
        assertFalse(original.getFailedEquipment().hasNext(),
              "Original entity should have no failed equipment");

        // Verify the one-shot ammo exists at LOC_NONE
        boolean hasOneShotAmmo = false;
        for (Mounted<?> mounted : original.getAmmo()) {
            if (mounted.getLocation() == Entity.LOC_NONE
                  && mounted.getType() instanceof AmmoType
                  && mounted.getLinkedBy() != null) {
                hasOneShotAmmo = true;
                break;
            }
        }
        assertTrue(hasOneShotAmmo, "BA should have one-shot ammo at LOC_NONE");

        // Save to BLK and reload
        BuildingBlock blk = BLKFile.getBlock(original);
        BLKBattleArmorFile loader = new BLKBattleArmorFile(blk);
        BattleArmor reloaded = (BattleArmor) loader.getEntity();

        assertNotNull(reloaded, "Reloaded entity should not be null");
        assertFalse(reloaded.getFailedEquipment().hasNext(),
              "Reloaded entity should have no failed equipment");
    }

    /**
     * Verifies backward compatibility: a BA slotless_equipment block containing the old :Shots# suffix format loads
     * correctly without failed equipment.
     */
    @Test
    void battleArmorSlotlessAmmoWithShotsSuffixLoads() throws Exception {
        // Build a BLK that mimics the old save format with :Shots# in slotless_equipment
        BattleArmor original = loadBattleArmor("Afreet Med BA (HH) (Sqd4).blk");
        BuildingBlock blk = BLKFile.getBlock(original);

        // Inject a slotless_equipment entry with the old :Shots# format
        // to simulate files saved before the fix
        blk.writeBlockData("slotless_equipment",
              new String[] { "BAJumpJet", "BA-SRM3 Ammo:Shots1#" });

        BLKBattleArmorFile loader = new BLKBattleArmorFile(blk);
        BattleArmor reloaded = (BattleArmor) loader.getEntity();

        assertNotNull(reloaded, "Reloaded entity should not be null");
        assertFalse(reloaded.getFailedEquipment().hasNext(),
              "Reloaded entity should have no failed equipment even with old :Shots# format");
    }

}
