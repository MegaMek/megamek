package megamek.client.generator;

import megamek.client.Client;
import megamek.client.ui.swing.ClientGUI;
import megamek.common.*;
import megamek.common.containers.MunitionTree;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TeamLoadoutGeneratorTest {

    static GameOptions mockGameOptions = mock(GameOptions.class);
    static ClientGUI cg = mock(ClientGUI.class);
    static Client client = mock(Client.class);
    static Game game = new Game();
    static Mounted mockAmmoSrm6 = mock(Mounted.class);
    static AmmoType mockSRM6AmmoType = mock(AmmoType.class);
    static Mounted mockAmmoLrm15no1 = mock(Mounted.class);
    static Mounted mockAmmoLrm15no2 = mock(Mounted.class);
    static AmmoType mockLRM15AmmoType = (AmmoType) EquipmentType.get("IS LRM 15 Ammo");

    @BeforeAll
    static void setUpAll() {
        // Need equipment initialized
        EquipmentType.initializeTypes();

        when(cg.getClient()).thenReturn(client);
        when(cg.getClient().getGame()).thenReturn(game);
        when(mockGameOptions.booleanOption(eq(OptionsConstants.ALLOWED_NO_CLAN_PHYSICAL))).thenReturn(false);
        game.setOptions(mockGameOptions);

        when(mockAmmoSrm6.getType()).thenReturn(mockSRM6AmmoType);
        when(mockAmmoSrm6.isAmmoUsable()).thenReturn(true);

        when(mockAmmoLrm15no1.getType()).thenReturn(mockLRM15AmmoType);
        when(mockAmmoLrm15no2.getType()).thenReturn(mockLRM15AmmoType);
        when(mockAmmoLrm15no1.isAmmoUsable()).thenReturn(true);
        when(mockAmmoLrm15no2.isAmmoUsable()).thenReturn(true);
    }

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void generateParameters() {
    }

    @Test
    void generateMunitionTree() {
    }

    @Test
    void reconfigureBotTeam() {
    }

    @Test
    void reconfigureTeam() {
    }

    @Test
    void testReconfigureTeam() {
    }

    Mech createMech(String chassis, String model, String crewName) {
        Mech mockMech = new BipedMech();
        mockMech.setChassis(chassis);
        mockMech.setModel(model);

        Crew mockCrew = mock(Crew.class);
        when(mockCrew.getName(anyInt())).thenCallRealMethod();
        when(mockCrew.getNames()).thenReturn(new String[] {crewName});
        mockMech.setCrew(mockCrew);

        return mockMech;
    }

    @Test
    void testReconfigureEntityMechNoAmmoTypesRequested() throws LocationFullException {
        TeamLoadoutGenerator tlg = new TeamLoadoutGenerator(cg);

        Mech mockMech = createMech("Catapult", "CPLT-C1", "J. Robert Hoppenheimer");
        Mounted bin1 = mockMech.addEquipment(mockLRM15AmmoType, Mech.LOC_LT);
        Mounted bin2 = mockMech.addEquipment(mockLRM15AmmoType, Mech.LOC_RT);
        MunitionTree mt = new MunitionTree();

        // We expect to see no change in loadouts
        tlg.reconfigureEntity(mockMech, mt);
        assertTrue(((AmmoType) bin1.getType()).getMunitionType().contains(AmmoType.Munitions.M_STANDARD));
    }

        @Test
    void testReconfigureEntityMechOneAmmoType() throws LocationFullException {
        TeamLoadoutGenerator tlg = new TeamLoadoutGenerator(cg);

        Mech mockMech = createMech("Catapult", "CPLT-C1", "J. Robert Hoppenheimer");
        Mounted bin1 = mockMech.addEquipment(mockLRM15AmmoType, Mech.LOC_LT);
        Mounted bin2 = mockMech.addEquipment(mockLRM15AmmoType, Mech.LOC_RT);

        MunitionTree mt = new MunitionTree();
        mt.insertImperative("Catapult", "CPLT-C1", "any", "LRM-15", "Dead-Fire");

        // We expect that all bins are set to the desired munition type as only one type is provided
        tlg.reconfigureEntity(mockMech, mt);
        assertFalse(((AmmoType) bin1.getType()).getMunitionType().contains(AmmoType.Munitions.M_STANDARD));
        assertTrue(((AmmoType) bin1.getType()).getMunitionType().contains(AmmoType.Munitions.M_DEAD_FIRE));
        assertFalse(((AmmoType) bin2.getType()).getMunitionType().contains(AmmoType.Munitions.M_STANDARD));
        assertTrue(((AmmoType) bin2.getType()).getMunitionType().contains(AmmoType.Munitions.M_DEAD_FIRE));

        // Now reset the ammo
        mt.insertImperative("Catapult", "CPLT-C1", "any", "LRM-15", "Standard");
        tlg.reconfigureEntity(mockMech, mt);
        assertTrue(((AmmoType) bin1.getType()).getMunitionType().contains(AmmoType.Munitions.M_STANDARD));
        assertFalse(((AmmoType) bin1.getType()).getMunitionType().contains(AmmoType.Munitions.M_DEAD_FIRE));
        assertTrue(((AmmoType) bin2.getType()).getMunitionType().contains(AmmoType.Munitions.M_STANDARD));
        assertFalse(((AmmoType) bin2.getType()).getMunitionType().contains(AmmoType.Munitions.M_DEAD_FIRE));
    }

    @Test
    void testReconfigureEntityMechThreeAmmoTypesFourBins() throws LocationFullException {
        TeamLoadoutGenerator tlg = new TeamLoadoutGenerator(cg);

        Mech mockMech = createMech("Catapult", "CPLT-C1", "J. Robert Hoppenheimer");
        Mounted bin1 = mockMech.addEquipment(mockLRM15AmmoType, Mech.LOC_LT);
        Mounted bin2 = mockMech.addEquipment(mockLRM15AmmoType, Mech.LOC_LT);
        Mounted bin3 = mockMech.addEquipment(mockLRM15AmmoType, Mech.LOC_RT);
        Mounted bin4 = mockMech.addEquipment(mockLRM15AmmoType, Mech.LOC_RT);

        MunitionTree mt = new MunitionTree();
        // First, set all bins to Smoke
        mt.insertImperative("Catapult", "CPLT-C1", "any", "LRM-15", "Smoke");
        tlg.reconfigureEntity(mockMech, mt);
        assertTrue(((AmmoType) bin1.getType()).getMunitionType().contains(AmmoType.Munitions.M_SMOKE_WARHEAD));

        // Then reset bins with useful ammo
        mt.insertImperative("Catapult", "CPLT-C1", "any", "LRM-15", "Standard", "Dead-Fire", "Heat-Seeking");

        // We expect that all bins are set to the desired munition type as only one type is provided
        tlg.reconfigureEntity(mockMech, mt);
        assertTrue(((AmmoType) bin1.getType()).getMunitionType().contains(AmmoType.Munitions.M_STANDARD));
        assertFalse(((AmmoType) bin2.getType()).getMunitionType().contains(AmmoType.Munitions.M_STANDARD));
        assertTrue(((AmmoType) bin2.getType()).getMunitionType().contains(AmmoType.Munitions.M_DEAD_FIRE));
        assertFalse(((AmmoType) bin3.getType()).getMunitionType().contains(AmmoType.Munitions.M_STANDARD));
        assertTrue(((AmmoType) bin3.getType()).getMunitionType().contains(AmmoType.Munitions.M_HEAT_SEEKING));
        // The final bin should be reset to the default, in this case "Standard"
        assertTrue(((AmmoType) bin4.getType()).getMunitionType().contains(AmmoType.Munitions.M_STANDARD));
    }

    @Test
    void testReconfigureTwoEntityMechsGenericAndNamed() throws LocationFullException {
        TeamLoadoutGenerator tlg = new TeamLoadoutGenerator(cg);

        Mech mockMech = createMech("Catapult", "CPLT-C1", "J. Robert Hoppenheimer");
        Mech mockMech2 = createMech("Catapult", "CPLT-C1", "John Q. Public");
        Mounted bin1 = mockMech.addEquipment(mockLRM15AmmoType, Mech.LOC_LT);
        Mounted bin2 = mockMech.addEquipment(mockLRM15AmmoType, Mech.LOC_LT);
        Mounted bin3 = mockMech.addEquipment(mockLRM15AmmoType, Mech.LOC_RT);
        Mounted bin4 = mockMech.addEquipment(mockLRM15AmmoType, Mech.LOC_RT);
        Mounted bin5 = mockMech2.addEquipment(mockLRM15AmmoType, Mech.LOC_RT);
        Mounted bin6 = mockMech2.addEquipment(mockLRM15AmmoType, Mech.LOC_RT);
        Mounted bin7 = mockMech2.addEquipment(mockLRM15AmmoType, Mech.LOC_RT);
        Mounted bin8 = mockMech2.addEquipment(mockLRM15AmmoType, Mech.LOC_RT);

        // Set up two loadouts: one for a named pilot, and one for all LRMs on any Catapults
        MunitionTree mt = new MunitionTree();
        mt.insertImperative("Catapult", "CPLT-C1", "J. Robert Hoppenheimer", "LRM-15", "Standard", "Dead-Fire", "Heat-Seeking", "Smoke");
        mt.insertImperative("Catapult", "any", "any", "LRM", "Standard", "Swarm", "Semi-guided");

        // J. Robert H. should get the first loadout
        tlg.reconfigureEntity(mockMech, mt);
        assertTrue(((AmmoType) bin1.getType()).getMunitionType().contains(AmmoType.Munitions.M_STANDARD));
        assertTrue(((AmmoType) bin2.getType()).getMunitionType().contains(AmmoType.Munitions.M_DEAD_FIRE));
        assertTrue(((AmmoType) bin3.getType()).getMunitionType().contains(AmmoType.Munitions.M_HEAT_SEEKING));
        assertTrue(((AmmoType) bin4.getType()).getMunitionType().contains(AmmoType.Munitions.M_SMOKE_WARHEAD));

        // John Q. should get the generalized loadout
        tlg.reconfigureEntity(mockMech2, mt);
        assertTrue(((AmmoType) bin5.getType()).getMunitionType().contains(AmmoType.Munitions.M_STANDARD));
        assertTrue(((AmmoType) bin6.getType()).getMunitionType().contains(AmmoType.Munitions.M_SWARM));
        assertTrue(((AmmoType) bin7.getType()).getMunitionType().contains(AmmoType.Munitions.M_SEMIGUIDED));
        assertTrue(((AmmoType) bin8.getType()).getMunitionType().contains(AmmoType.Munitions.M_STANDARD));
    }
}