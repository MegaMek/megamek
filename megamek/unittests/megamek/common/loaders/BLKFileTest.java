/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.loaders;

import com.sun.mail.util.DecodingException;
import megamek.common.*;
import megamek.common.InfantryBay.PlatoonType;
import megamek.common.loaders.BLKFile.ParsedBayInfo;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

public class BLKFileTest {

    /**
     * Strips the bay type identifier from the bay string.
     *
     * @param bay The Bay being parsed
     * @return The part of the bay string containing the parameters (size, doors, num, etc)
     */
    private String getBayNumbers(Bay bay) {
        String bayString = bay.toString();
        return bayString.substring(bayString.indexOf(Bay.FIELD_SEPARATOR) + 1);
    }

    @Test
    public void parseBayDataAssignsMissingBayNumber() {
        final double SIZE = 2.0;
        final int DOORS = 1;
        String bayString = SIZE + ":" + DOORS;
        HashSet<Integer> bayNums = new HashSet<>();
        bayNums.add(0);
        bayNums.add(1);


        try {
            ParsedBayInfo pbi = new ParsedBayInfo(bayString, bayNums);
            assertEquals(pbi.getSize(), SIZE, 0.01);
            assertEquals(pbi.getDoors(), DOORS);
            assertEquals(pbi.getBayNumber(), 2);
        } catch (DecodingException e) {
            fail("Unexpected exception!");
        }

    }

    @Test
    public void parseBayDataFixesDuplicateBayNumber() {
        final double SIZE = 2.0;
        final int DOORS = 1;
        Bay bay = new MechBay(SIZE, DOORS, 1);
        HashSet<Integer> bayNums = new HashSet<>();
        bayNums.add(0);
        bayNums.add(1);

        try {
            ParsedBayInfo pbi = new ParsedBayInfo(getBayNumbers(bay), bayNums);

            assertEquals(pbi.getSize(), SIZE, 0.01);
            assertEquals(pbi.getDoors(), DOORS);
            assertEquals(pbi.getBayNumber(), 2);
        } catch (DecodingException e) {
            fail("Unexpected exception!");
        }
    }

    @Test
    public void parseBayTypeIndicatorWithBayNumber() {
        Bay bay = new BattleArmorBay(2.0, 1, 1, false, true);

        try {
            ParsedBayInfo pbi = new ParsedBayInfo(getBayNumbers(bay), new HashSet<>());

            assertTrue(pbi.isComstarBay());
            assertEquals(pbi.getBayNumber(), 1);
        } catch (DecodingException e) {
            fail("Unexpected exception!");
        }
    }

    @Test
    public void parseBayTypeIndicatorWithoutBayNumber() {
        Bay bay = new BattleArmorBay(2.0, 1, 4, false, true);
        String numbers = getBayNumbers(bay).replace(":4", ":-1");
        HashSet<Integer> bayNums = new HashSet<>();
        bayNums.add(0);
        bayNums.add(1);

        try {
            ParsedBayInfo pbi = new ParsedBayInfo(numbers, bayNums);

            assertTrue(pbi.isComstarBay());
            assertEquals(pbi.getBayNumber(), 2);
        } catch (DecodingException e) {
            fail("Unexpected exception!");
        }
    }

    @Test
    public void parseFootInfantryBay() {
        Bay bay = new InfantryBay(2.0, 1, 0, PlatoonType.FOOT);

        try {
            ParsedBayInfo pbi = new ParsedBayInfo(getBayNumbers(bay), new HashSet<>());

            assertEquals(pbi.getPlatoonType(), PlatoonType.FOOT);
        } catch (DecodingException e) {
            fail("Unexpected exception!");
        }
    }

    @Test
    public void parseJumpInfantryBay() {
        Bay bay = new InfantryBay(2.0, 1, 0, PlatoonType.JUMP);

        try {
            ParsedBayInfo pbi = new ParsedBayInfo(getBayNumbers(bay), new HashSet<>());

            assertEquals(pbi.getPlatoonType(), PlatoonType.JUMP);
        } catch (DecodingException e) {
            fail("Unexpected exception!");
        }
    }

    @Test
    public void parseMotorizedInfantryBay() {
        Bay bay = new InfantryBay(2.0, 1, 0, PlatoonType.MOTORIZED);

        try {
            ParsedBayInfo pbi = new ParsedBayInfo(getBayNumbers(bay), new HashSet<>());

            assertEquals(pbi.getPlatoonType(), PlatoonType.MOTORIZED);
        } catch (DecodingException e) {
            fail("Unexpected exception!");
        }
    }

    @Test
    public void parseMechanizedInfantryBay() {
        Bay bay = new InfantryBay(2.0, 1, 0, PlatoonType.MECHANIZED);

        try {
            ParsedBayInfo pbi = new ParsedBayInfo(getBayNumbers(bay), new HashSet<>());

            assertEquals(pbi.getPlatoonType(), PlatoonType.MECHANIZED);
        } catch (DecodingException e) {
            fail("Unexpected exception!");
        }
    }

    @Test
    public void parseDropShuttleBay() {
        Bay bay = new DropshuttleBay(1, -1, Jumpship.LOC_AFT);

        try {
            ParsedBayInfo pbi = new ParsedBayInfo(getBayNumbers(bay), new HashSet<>());

            assertEquals(pbi.getDoors(), 1);
            assertEquals(pbi.getBayNumber(), 1);
            assertEquals(pbi.getFacing(), Jumpship.LOC_AFT);
        } catch (DecodingException e) {
            fail(String.format("Unexpected exception (%s)!", e.toString()));
        }
    }

    @Test
    public void parseNavalRepairFacility() {
        final double SIZE = 5000.0;
        final int DOORS = 2;
        Bay bay = new NavalRepairFacility(SIZE, DOORS, -1, Jumpship.LOC_AFT, true);

        try {
            ParsedBayInfo pbi = new ParsedBayInfo(getBayNumbers(bay), new HashSet<>());

            assertEquals(pbi.getSize(), SIZE, 0.01);
            assertEquals(pbi.getDoors(), DOORS);
            assertEquals(pbi.getBayNumber(), 1);
            assertEquals(pbi.getFacing(), Jumpship.LOC_AFT);
        } catch (DecodingException e) {
            fail("Unexpected exception!");
        }
    }

    @Test
    public void decodeValidPlatoonTypes() throws DecodingException {
        final String[] types = {"foot", "jump", "mechanized", "motorized", ""};
        final PlatoonType[] ptypes = {
                PlatoonType.FOOT,
                PlatoonType.JUMP,
                PlatoonType.MECHANIZED,
                PlatoonType.MOTORIZED,
                PlatoonType.FOOT
        };

        for (int i = 0; i < types.length; i++) {
            assertEquals(ptypes[i], ParsedBayInfo.decodePlatoonType(types[i]));
        }
    }

    @Test
    public void decodeInvalidPlatoonTypeThrows(){
        assertThrows(DecodingException.class,
                () -> {
                    ParsedBayInfo.decodePlatoonType("FEeeTS");
                }
        );
    }

    @Test
    public void normalizeInvalidNumbersThrows(){
        String invalidNumbers = "10.0:0:1:c*:extra:fields:throw";
        assertThrows(DecodingException.class,
                () -> {
                    ParsedBayInfo.normalizeTransporterNumbers(invalidNumbers);
                });
    }

    public boolean confirmTransporterNumbers(String numbers, String[] expNumArray) {
        // Verifies matches between array generated from numbers list and expected array
        try {
            String[] genNumArray = ParsedBayInfo.normalizeTransporterNumbers(numbers);
            assertEquals(expNumArray.length, genNumArray.length);
            for(int i=0; i < genNumArray.length; i++){
                assertEquals(expNumArray[i], genNumArray[i],
                        String.format("Checking index %s of '%s' failed", i, numbers)
                );
            }
        } catch (Exception e){
            return false;
        }
        return true;
    }

    @Test
    public void normalizeNewNumbersFormatReturnsSameValues() {
        String numbers = "1000.0:1:2::-1:2"; // 1000.0 ton Clan BA bay with one door
        String[] expNumArray = numbers.split(Bay.FIELD_SEPARATOR);

        assertTrue(confirmTransporterNumbers(numbers, expNumArray));

    }
    @Test
    public void normalizeOldTransporterFormats(){
        // expected format of "numbers" string:
        // 0:1:2:3:4:5
        // Field 0 is the size of the bay, in tons or # of units and is required
        // Field 1 is the number of doors in the bay, and is required
        // Field 2 is the bay number and is required; default value of "-1" is "unset"
        // Field 3 is used to record infantry platoon type; default is an empty string
        // Field 4 is an int recording facing; default is string representation of entity.LOC_NONE
        // Field 5 is a bitmap recording status like tech type, ComStar bay; default is "0"
        // numbersArray is in old format; expNumbersArray is an array of String[] in new format
        String[] numbersArray = {
                "1.0:0",        // Size: 1.0; Doors: 0
                "2.0:1",        // Size: 2.0; Doors: 1
                "3.0:1:-1",     // Size: 3.0; Doors: 1; Bay#: -1 (unset)
                "4.0:2:0",      // Size: 4.0; Doors: 2; Bay#: 0
                "5.0:1:1",      // Size: 5.0; Doors: 1; Bay#: 1
                "6.0:0:c*",     // Size: 6.0; Doors: 0; ComStar type = set
                "7.0:1:Foot",   // Size: 7.0; Doors: 1; infantry bay type FOOT
                "8.0:0:1:Jump", // Size: 8.0; Doors: 0; Bay#: 1; infantry bay type JUMP
                "9.0:1:2:f1",   // Size: 9.0; Doors: 1; Bay#: 2; Facing: 1
        };
        String[][] expNumbersArray = {
                {"1.0", "0", "-1", "", "-1", "0"},
                {"2.0", "1", "-1", "", "-1", "0"},
                {"3.0", "1", "-1", "", "-1", "0"},
                {"4.0", "2", "0", "", "-1", "0"},
                {"5.0", "1", "1", "", "-1", "0"},
                {"6.0", "0", "-1", "", "-1", "1"},
                {"7.0", "1", "-1", "Foot", "-1", "0"},
                {"8.0", "0", "1", "Jump", "-1", "0"},
                {"9.0", "1", "2", "", "1", "0"}
        };
        boolean matched = false;
        for(int i=0; i<numbersArray.length; i++){
            matched = confirmTransporterNumbers(numbersArray[i], expNumbersArray[i]);
            assertTrue(matched);

        }

    }

}