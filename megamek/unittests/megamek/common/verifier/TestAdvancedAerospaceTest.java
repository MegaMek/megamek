package megamek.common.verifier;

import megamek.common.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.io.File;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestAdvancedAerospaceTest {

    private static EntityVerifier verifier;
    private Vector<Bay> bays;

    @BeforeAll
    public static void beforeAll() {
        File file = new File(TestAdvancedAerospaceTest.class.getResource("empty-verifier-options.xml").getFile());
        verifier = EntityVerifier.getInstance(file);
    }
    
    @BeforeEach
    public void beforeEach() {
        bays = new Vector<>();
    }
    
    private Jumpship createJumpship() {
        Jumpship js = mock(Jumpship.class);
        when(js.getTransportBays()).thenReturn(bays);
        when(js.hasETypeFlag(ArgumentMatchers.anyLong())).thenAnswer(inv ->
                ((Entity.ETYPE_AERO | Entity.ETYPE_JUMPSHIP) & (Long) inv.getArguments()[0]) != 0);
        return js;
    }

    private SpaceStation createStation() {
        SpaceStation ss = mock(SpaceStation.class);
        when(ss.getTransportBays()).thenReturn(bays);
        when(ss.hasETypeFlag(ArgumentMatchers.anyLong())).thenAnswer(inv ->
                ((Entity.ETYPE_AERO | Entity.ETYPE_JUMPSHIP | Entity.ETYPE_SPACE_STATION)
                        & (Long) inv.getArguments()[0]) != 0);
        return ss;
    }

    @Test
    public void correctBaysPassesWithSingleRepair() {
        Jumpship js = createJumpship();
        TestAdvancedAerospace test = new TestAdvancedAerospace(js, verifier.aeroOption, "test");
        
        bays.add(new NavalRepairFacility(500.0, 1, 1, Jumpship.LOC_NOSE, false));
        
        assertTrue(test.correctBays(new StringBuffer()));
    }

    @Test
    public void correctBaysFailsWhenRepairHasNoFacing() {
        Jumpship js = createJumpship();
        TestAdvancedAerospace test = new TestAdvancedAerospace(js, verifier.aeroOption, "test");
        
        bays.add(new NavalRepairFacility(500.0, 1, 1, Jumpship.LOC_NONE, false));
        
        assertFalse(test.correctBays(new StringBuffer()));
    }

    @Test
    public void correctBaysFailsWhenMultipleRepairHaveSameFacing() {
        SpaceStation ss = createStation();
        TestAdvancedAerospace test = new TestAdvancedAerospace(ss, verifier.aeroOption, "test");
        
        bays.add(new NavalRepairFacility(500.0, 1, 1, Jumpship.LOC_NOSE, false));
        bays.add(new NavalRepairFacility(500.0, 1, 1, Jumpship.LOC_NOSE, false));
        
        assertFalse(test.correctBays(new StringBuffer()));
    }

    @Test
    public void correctBaysFailsWhenShipHasMultipleRepair() {
        Jumpship js = createJumpship();
        TestAdvancedAerospace test = new TestAdvancedAerospace(js, verifier.aeroOption, "test");
        bays.add(new NavalRepairFacility(500.0, 1, 1, Jumpship.LOC_NOSE, false));
        bays.add(new NavalRepairFacility(500.0, 1, 1, Jumpship.LOC_AFT, false));
        assertFalse(test.correctBays(new StringBuffer()));
    }

    @Test
    public void correctBaysPassesWhenStationHasMultipleRepair() {
        SpaceStation ss = createStation();
        TestAdvancedAerospace test = new TestAdvancedAerospace(ss, verifier.aeroOption, "test");
        
        bays.add(new NavalRepairFacility(500.0, 1, 1, Jumpship.LOC_NOSE, false));
        bays.add(new NavalRepairFacility(500.0, 1, 1, Jumpship.LOC_AFT, false));
        
        assertTrue(test.correctBays(new StringBuffer()));
    }
}
