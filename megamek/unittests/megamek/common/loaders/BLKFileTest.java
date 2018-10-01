package megamek.common.loaders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import org.junit.Test;

import megamek.common.BattleArmorBay;
import megamek.common.Bay;
import megamek.common.DropshuttleBay;
import megamek.common.InfantryBay;
import megamek.common.Jumpship;
import megamek.common.MechBay;
import megamek.common.NavalRepairFacility;
import megamek.common.loaders.BLKFile.ParsedBayInfo;

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
        
        ParsedBayInfo pbi = new BLKFile.ParsedBayInfo(bayString, bayNums);
        
        assertEquals(pbi.getSize(), SIZE, 0.01);
        assertEquals(pbi.getDoors(), DOORS);
        assertEquals(pbi.getBayNumber(), 2);
    }

    @Test
    public void parseBayDataFixesDuplicateBayNumber() {
        final double SIZE = 2.0;
        final int DOORS = 1;
        Bay bay = new MechBay(SIZE, DOORS, 1);
        HashSet<Integer> bayNums = new HashSet<>();
        bayNums.add(0);
        bayNums.add(1);
        
        ParsedBayInfo pbi = new BLKFile.ParsedBayInfo(getBayNumbers(bay), bayNums);
        
        assertEquals(pbi.getSize(), SIZE, 0.01);
        assertEquals(pbi.getDoors(), DOORS);
        assertEquals(pbi.getBayNumber(), 2);
    }
    
    @Test
    public void parseBayTypeIndicatorWithBayNumber() {
        Bay bay = new BattleArmorBay(2.0, 1, 1, false, true);
        
        ParsedBayInfo pbi = new BLKFile.ParsedBayInfo(getBayNumbers(bay), new HashSet<>());
        
        assertTrue(pbi.isComstarBay());
        assertEquals(pbi.getBayNumber(), 1);
    }

    @Test
    public void parseBayTypeIndicatorWithoutBayNumber() {
        Bay bay = new BattleArmorBay(2.0, 1, 4, false, true);
        String numbers = getBayNumbers(bay).replace(":4", "");
        HashSet<Integer> bayNums = new HashSet<>();
        bayNums.add(0);
        bayNums.add(1);
        
        ParsedBayInfo pbi = new BLKFile.ParsedBayInfo(numbers, bayNums);
        
        assertTrue(pbi.isComstarBay());
        assertEquals(pbi.getBayNumber(), 2);
    }

    @Test
    public void parseFootInfantryBay() {
        Bay bay = new InfantryBay(2.0, 1, 0, InfantryBay.PlatoonType.FOOT);
        
        ParsedBayInfo pbi = new BLKFile.ParsedBayInfo(getBayNumbers(bay), new HashSet<>());
        
        assertEquals(pbi.getPlatoonType(), InfantryBay.PlatoonType.FOOT);
    }

    @Test
    public void parseJumpInfantryBay() {
        Bay bay = new InfantryBay(2.0, 1, 0, InfantryBay.PlatoonType.JUMP);
        
        ParsedBayInfo pbi = new BLKFile.ParsedBayInfo(getBayNumbers(bay), new HashSet<>());
        
        assertEquals(pbi.getPlatoonType(), InfantryBay.PlatoonType.JUMP);
    }

    @Test
    public void parseMotorizedInfantryBay() {
        Bay bay = new InfantryBay(2.0, 1, 0, InfantryBay.PlatoonType.MOTORIZED);
        
        ParsedBayInfo pbi = new BLKFile.ParsedBayInfo(getBayNumbers(bay), new HashSet<>());
        
        assertEquals(pbi.getPlatoonType(), InfantryBay.PlatoonType.MOTORIZED);
    }

    @Test
    public void parseMechanizedInfantryBay() {
        Bay bay = new InfantryBay(2.0, 1, 0, InfantryBay.PlatoonType.MECHANIZED);
        
        ParsedBayInfo pbi = new BLKFile.ParsedBayInfo(getBayNumbers(bay), new HashSet<>());
        
        assertEquals(pbi.getPlatoonType(), InfantryBay.PlatoonType.MECHANIZED);
    }
    
    @Test
    public void parseDropshuttleBay() {
        Bay bay = new DropshuttleBay(1, -1, Jumpship.LOC_AFT);
        
        ParsedBayInfo pbi = new BLKFile.ParsedBayInfo(getBayNumbers(bay), new HashSet<>());
        
        assertEquals(pbi.getDoors(), 1);
        assertEquals(pbi.getBayNumber(), 1);
        assertEquals(pbi.getFacing(), Jumpship.LOC_AFT);
    }
    
    @Test
    public void parseNavalRepairFacility() {
        final double SIZE = 5000.0;
        final int DOORS = 2;
        Bay bay = new NavalRepairFacility(SIZE, DOORS, -1, Jumpship.LOC_AFT, true);
        
        ParsedBayInfo pbi = new BLKFile.ParsedBayInfo(getBayNumbers(bay), new HashSet<>());
        
        assertEquals(pbi.getSize(), SIZE, 0.01);
        assertEquals(pbi.getDoors(), DOORS);
        assertEquals(pbi.getBayNumber(), 1);
        assertEquals(pbi.getFacing(), Jumpship.LOC_AFT);
    }
}
