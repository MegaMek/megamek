package megamek.common.loaders;

import static org.junit.Assert.*;

import java.util.HashSet;

import org.junit.Test;

import megamek.common.BattleArmorBay;
import megamek.common.Bay;
import megamek.common.InfantryBay;
import megamek.common.MechBay;
import megamek.common.loaders.BLKFile.ParsedBayInfo;

public class BLKFileTest {
    
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
        String bayString = bay.toString();
        bayString = bayString.substring(bayString.indexOf(":") + 1);
        HashSet<Integer> bayNums = new HashSet<>();
        bayNums.add(0);
        bayNums.add(1);
        
        ParsedBayInfo pbi = new BLKFile.ParsedBayInfo(bayString, bayNums);
        
        assertEquals(pbi.getSize(), SIZE, 0.01);
        assertEquals(pbi.getDoors(), DOORS);
        assertEquals(pbi.getBayNumber(), 2);
    }
    
    @Test
    public void parseBayTypeIndicatorWithBayNumber() {
        Bay bay = new BattleArmorBay(2.0, 1, 1, false, true);
        String bayString = bay.toString();
        bayString = bayString.substring(bayString.indexOf(":") + 1);
        
        ParsedBayInfo pbi = new BLKFile.ParsedBayInfo(bayString, new HashSet<>());
        
        assertTrue(pbi.isComstarBay());
        assertEquals(pbi.getBayNumber(), 1);
    }

    @Test
    public void parseBayTypeIndicatorWithoutBayNumber() {
        Bay bay = new BattleArmorBay(2.0, 1, 4, false, true);
        String bayString = bay.toString();
        bayString = bayString.substring(bayString.indexOf(":") + 1);
        bayString = bayString.replace(":4", "");
        HashSet<Integer> bayNums = new HashSet<>();
        bayNums.add(0);
        bayNums.add(1);
        
        ParsedBayInfo pbi = new BLKFile.ParsedBayInfo(bayString, bayNums);
        
        assertTrue(pbi.isComstarBay());
        assertEquals(pbi.getBayNumber(), 2);
    }

    @Test
    public void parseFootInfantryBay() {
        Bay bay = new InfantryBay(2.0, 1, 0, InfantryBay.PlatoonType.FOOT);
        String bayString = bay.toString();
        bayString = bayString.substring(bayString.indexOf(":") + 1);
        
        ParsedBayInfo pbi = new BLKFile.ParsedBayInfo(bayString, new HashSet<>());
        
        assertEquals(pbi.getPlatoonType(), InfantryBay.PlatoonType.FOOT);
    }

    @Test
    public void parseJumpInfantryBay() {
        Bay bay = new InfantryBay(2.0, 1, 0, InfantryBay.PlatoonType.JUMP);
        String bayString = bay.toString();
        bayString = bayString.substring(bayString.indexOf(":") + 1);
        
        ParsedBayInfo pbi = new BLKFile.ParsedBayInfo(bayString, new HashSet<>());
        
        assertEquals(pbi.getPlatoonType(), InfantryBay.PlatoonType.JUMP);
    }

    @Test
    public void parseMotorizedInfantryBay() {
        Bay bay = new InfantryBay(2.0, 1, 0, InfantryBay.PlatoonType.MOTORIZED);
        String bayString = bay.toString();
        bayString = bayString.substring(bayString.indexOf(":") + 1);
        
        ParsedBayInfo pbi = new BLKFile.ParsedBayInfo(bayString, new HashSet<>());
        
        assertEquals(pbi.getPlatoonType(), InfantryBay.PlatoonType.MOTORIZED);
    }

    @Test
    public void parseMechanizedInfantryBay() {
        Bay bay = new InfantryBay(2.0, 1, 0, InfantryBay.PlatoonType.MECHANIZED);
        String bayString = bay.toString();
        bayString = bayString.substring(bayString.indexOf(":") + 1);
        
        ParsedBayInfo pbi = new BLKFile.ParsedBayInfo(bayString, new HashSet<>());
        
        assertEquals(pbi.getPlatoonType(), InfantryBay.PlatoonType.MECHANIZED);
    }
}
