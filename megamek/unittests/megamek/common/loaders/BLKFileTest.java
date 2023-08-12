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

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

public class BLKFileTest {

    /**
     * Strips the bay type identifier from the bay string.
     *
     * @param bay The Bay being parsed
     * @return    The part of the bay string containing the parameters (size, doors, num, etc)
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
        }
        catch(DecodingException e){
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
        }
        catch(DecodingException e) {
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
        }
        catch(DecodingException e) {
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
        }
        catch(DecodingException e) {
            fail("Unexpected exception!");
        }
    }

    @Test
    public void parseFootInfantryBay() {
        Bay bay = new InfantryBay(2.0, 1, 0, PlatoonType.FOOT);

        try {
            ParsedBayInfo pbi = new ParsedBayInfo(getBayNumbers(bay), new HashSet<>());

            assertEquals(pbi.getPlatoonType(), PlatoonType.FOOT);
        }
        catch(DecodingException e) {
            fail("Unexpected exception!");
        }
    }

    @Test
    public void parseJumpInfantryBay() {
        Bay bay = new InfantryBay(2.0, 1, 0, PlatoonType.JUMP);

        try {
            ParsedBayInfo pbi = new ParsedBayInfo(getBayNumbers(bay), new HashSet<>());

            assertEquals(pbi.getPlatoonType(), PlatoonType.JUMP);
        }
        catch(DecodingException e) {
            fail("Unexpected exception!");
        }
    }

    @Test
    public void parseMotorizedInfantryBay() {
        Bay bay = new InfantryBay(2.0, 1, 0, PlatoonType.MOTORIZED);

        try {
            ParsedBayInfo pbi = new ParsedBayInfo(getBayNumbers(bay), new HashSet<>());

            assertEquals(pbi.getPlatoonType(), PlatoonType.MOTORIZED);
        }
        catch(DecodingException e) {
            fail("Unexpected exception!");
        }
    }

    @Test
    public void parseMechanizedInfantryBay() {
        Bay bay = new InfantryBay(2.0, 1, 0, PlatoonType.MECHANIZED);

        try {
            ParsedBayInfo pbi = new ParsedBayInfo(getBayNumbers(bay), new HashSet<>());

            assertEquals(pbi.getPlatoonType(), PlatoonType.MECHANIZED);
        }
        catch(DecodingException e) {
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
        }
        catch(DecodingException e) {
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
        }
        catch(DecodingException e) {
            fail("Unexpected exception!");
        }
    }
}
