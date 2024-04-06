package megamek.client.generator;

import megamek.client.Client;
import megamek.client.ui.swing.ClientGUI;
import megamek.common.*;
import megamek.common.containers.MunitionTree;
import megamek.common.options.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
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

    static Team team = new Team(0);
    static Player player = new Player(0, "Test");
    static AmmoType mockLRM15AmmoType = (AmmoType) EquipmentType.get("IS LRM 15 Ammo");
    static AmmoType mockAC20AmmoType = (AmmoType) EquipmentType.get("ISAC20 Ammo");
    static AmmoType mockAC5AmmoType = (AmmoType) EquipmentType.get("ISAC5 Ammo");
    static AmmoType mockSRM6AmmoType = (AmmoType) EquipmentType.get("IS SRM 6 Ammo");

    @BeforeAll
    static void setUpAll() {
        // Need equipment initialized
        EquipmentType.initializeTypes();

        when(cg.getClient()).thenReturn(client);
        when(cg.getClient().getGame()).thenReturn(game);
        when(mockGameOptions.booleanOption(eq(OptionsConstants.ALLOWED_NO_CLAN_PHYSICAL))).thenReturn(false);
        when(mockGameOptions.stringOption(OptionsConstants.ALLOWED_TECHLEVEL)).thenReturn("Experimental");
        when(mockGameOptions.booleanOption(OptionsConstants.ALLOWED_ERA_BASED)).thenReturn(true);
        when(mockGameOptions.booleanOption(OptionsConstants.ALLOWED_SHOW_EXTINCT)).thenReturn(false);
        Option mockTrueBoolOpt = mock(Option.class);
        Option mockFalseBoolOpt = mock(Option.class);
        when(mockTrueBoolOpt.booleanValue()).thenReturn(true);
        when(mockFalseBoolOpt.booleanValue()).thenReturn(false);
        when(mockGameOptions.getOption(anyString())).thenReturn(mockTrueBoolOpt);
        when(mockGameOptions.intOption(OptionsConstants.ALLOWED_YEAR)).thenReturn(3151);
        game.setOptions(mockGameOptions);

        team.addPlayer(player);
        game.addPlayer(0, player);
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

    Mech createMech(String chassis, String model, String crewName) {
        Mech mockMech = new BipedMech();
        mockMech.setChassis(chassis);
        mockMech.setModel(model);

        Crew mockCrew = mock(Crew.class);
        PilotOptions pOpt = new PilotOptions();
        when(mockCrew.getName(anyInt())).thenCallRealMethod();
        when(mockCrew.getNames()).thenReturn(new String[] {crewName});
        when(mockCrew.getOptions()).thenReturn(pOpt);
        mockMech.setCrew(mockCrew);

        return mockMech;
    }

    @Test
    void testReconfigureEntityFallbackAmmoType() throws LocationFullException {
        TeamLoadoutGenerator tlg = new TeamLoadoutGenerator(cg);
        Mech mockMech = createMech("Mauler", "MAL-1K", "Tyson");
        Mounted bin1 = mockMech.addEquipment(mockAC5AmmoType, Mech.LOC_LT);
        Mounted bin2 = mockMech.addEquipment(mockAC5AmmoType, Mech.LOC_LT);
        Mounted bin3 = mockMech.addEquipment(mockAC5AmmoType, Mech.LOC_LT);
        Mounted bin4 = mockMech.addEquipment(mockAC5AmmoType, Mech.LOC_LT);

        // Create a set of imperatives, some of which won't work
        MunitionTree mt = new MunitionTree();
        mt.insertImperative("Mauler", "MAL-1K", "any", "AC/5", "Inferno:Standard:Smoke:Flak");
        tlg.reconfigureEntity(mockMech, mt, "IS");

        // First imperative entry is invalid, so bin1 should get second choice (Standard)
        assertTrue(((AmmoType) bin1.getType()).getMunitionType().contains(AmmoType.Munitions.M_STANDARD));
        // Third choice is invalid, so 2nd bin gets 4th choice, Flak
        assertTrue(((AmmoType) bin2.getType()).getMunitionType().contains(AmmoType.Munitions.M_FLAK));
        // Now two bins are left over, so they're filled with the _new_ default, Standard (choice #2)
        assertTrue(((AmmoType) bin3.getType()).getMunitionType().contains(AmmoType.Munitions.M_STANDARD));
        assertTrue(((AmmoType) bin4.getType()).getMunitionType().contains(AmmoType.Munitions.M_STANDARD));
    }

    @Test
    void testReconfigureEntityMechNoAmmoTypesRequested() throws LocationFullException {
        TeamLoadoutGenerator tlg = new TeamLoadoutGenerator(cg);

        Mech mockMech = createMech("Catapult", "CPLT-C1", "J. Robert Hoppenheimer");
        Mounted bin1 = mockMech.addEquipment(mockLRM15AmmoType, Mech.LOC_LT);
        Mounted bin2 = mockMech.addEquipment(mockLRM15AmmoType, Mech.LOC_RT);
        MunitionTree mt = new MunitionTree();

        // We expect to see no change in loadouts
        tlg.reconfigureEntity(mockMech, mt, "IS");
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
        tlg.reconfigureEntity(mockMech, mt, "IS");
        assertFalse(((AmmoType) bin1.getType()).getMunitionType().contains(AmmoType.Munitions.M_STANDARD));
        assertTrue(((AmmoType) bin1.getType()).getMunitionType().contains(AmmoType.Munitions.M_DEAD_FIRE));
        assertFalse(((AmmoType) bin2.getType()).getMunitionType().contains(AmmoType.Munitions.M_STANDARD));
        assertTrue(((AmmoType) bin2.getType()).getMunitionType().contains(AmmoType.Munitions.M_DEAD_FIRE));

        // Now reset the ammo
        mt.insertImperative("Catapult", "CPLT-C1", "any", "LRM-15", "Standard");
        tlg.reconfigureEntity(mockMech, mt, "IS");
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
        tlg.reconfigureEntity(mockMech, mt, "IS");
        assertTrue(((AmmoType) bin1.getType()).getMunitionType().contains(AmmoType.Munitions.M_SMOKE_WARHEAD));

        // Then reset bins with useful ammo
        mt.insertImperative("Catapult", "CPLT-C1", "any", "LRM-15", "Standard", "Dead-Fire", "Heat-Seeking");

        // We expect that all bins are set to the desired munition type as only one type is provided
        tlg.reconfigureEntity(mockMech, mt, "IS");
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
        tlg.reconfigureEntity(mockMech, mt, "IS");
        assertTrue(((AmmoType) bin1.getType()).getMunitionType().contains(AmmoType.Munitions.M_STANDARD));
        assertTrue(((AmmoType) bin2.getType()).getMunitionType().contains(AmmoType.Munitions.M_DEAD_FIRE));
        assertTrue(((AmmoType) bin3.getType()).getMunitionType().contains(AmmoType.Munitions.M_HEAT_SEEKING));
        assertTrue(((AmmoType) bin4.getType()).getMunitionType().contains(AmmoType.Munitions.M_SMOKE_WARHEAD));

        // John Q. should get the generalized loadout; last bin should be set to Standard
        tlg.reconfigureEntity(mockMech2, mt, "IS");
        assertTrue(((AmmoType) bin5.getType()).getMunitionType().contains(AmmoType.Munitions.M_STANDARD));
        assertTrue(((AmmoType) bin6.getType()).getMunitionType().contains(AmmoType.Munitions.M_SWARM));
        assertTrue(((AmmoType) bin7.getType()).getMunitionType().contains(AmmoType.Munitions.M_SEMIGUIDED));
        assertTrue(((AmmoType) bin8.getType()).getMunitionType().contains(AmmoType.Munitions.M_STANDARD));
    }

    @Test
    void testReconfigureTeamOfMechs()  throws LocationFullException {
        TeamLoadoutGenerator tlg = new TeamLoadoutGenerator(cg);
        Mech mockMech = createMech("Hunchback", "HBK-4G", "Boomstick");
        Mech mockMech2 = createMech("Hunchback", "HBK-4J", "The Shade");
        Mech mockMech3 = createMech("Kintaro", "KTO-18", "Dragonpunch");
        mockMech.setOwner(player);
        mockMech2.setOwner(player);
        mockMech3.setOwner(player);
        game.setEntity(0, mockMech);
        game.setEntity(1, mockMech2);
        game.setEntity(2, mockMech3);


        // Load ammo in 'mechs; locations are for fun
        Mounted bin1 = mockMech.addEquipment(mockAC20AmmoType, Mech.LOC_CT);
        Mounted bin2 = mockMech.addEquipment(mockAC20AmmoType, Mech.LOC_CT);
        Mounted bin3 = mockMech2.addEquipment(mockLRM15AmmoType, Mech.LOC_LT);
        Mounted bin4 = mockMech2.addEquipment(mockLRM15AmmoType, Mech.LOC_RT);
        Mounted bin5 = mockMech3.addEquipment(mockSRM6AmmoType, Mech.LOC_LT);
        Mounted bin6 = mockMech3.addEquipment(mockSRM6AmmoType, Mech.LOC_RT);
        Mounted bin7 = mockMech3.addEquipment(mockSRM6AmmoType, Mech.LOC_CT);

        MunitionTree mt = new MunitionTree();
        HashMap<String, String> imperatives = new HashMap<>();

        // HBK imperatives; both can be inserted at once
        imperatives.put("AC 20", "Caseless");
        imperatives.put("LRM", "Dead-Fire:Standard");
        mt.insertImperatives("Hunchback", "any", "any", imperatives);

        // Kintaro's go under different keys
        mt.insertImperative("Kintaro", "KTO-18", "any", "SRM", "Inferno:Standard");

        tlg.reconfigureTeam(game, team, mt);

        // Check loadouts
        // 1. AC20 HBK should have two tons of Caseless
        assertTrue(((AmmoType) bin1.getType()).getMunitionType().contains(AmmoType.Munitions.M_CASELESS));
        assertTrue(((AmmoType) bin2.getType()).getMunitionType().contains(AmmoType.Munitions.M_CASELESS));

        // 2. LRM HBK should have one each of Dead-Fire and Standard
        assertTrue(((AmmoType) bin3.getType()).getMunitionType().contains(AmmoType.Munitions.M_DEAD_FIRE));
        assertTrue(((AmmoType) bin4.getType()).getMunitionType().contains(AmmoType.Munitions.M_STANDARD));

        // 3. LRM HBK should have two Infernos and one Standard
        assertTrue(((AmmoType) bin5.getType()).getMunitionType().contains(AmmoType.Munitions.M_INFERNO));
        assertTrue(((AmmoType) bin6.getType()).getMunitionType().contains(AmmoType.Munitions.M_STANDARD));
        assertTrue(((AmmoType) bin7.getType()).getMunitionType().contains(AmmoType.Munitions.M_INFERNO));
    }

    @Test
    void testRandomReconfigureBotTeam()  throws LocationFullException {
        TeamLoadoutGenerator tlg = new TeamLoadoutGenerator(cg);
        Mech mockMech = createMech("Hunchback", "HBK-4G", "Boomstick");
        Mech mockMech2 = createMech("Hunchback", "HBK-4J", "The Shade");
        Mech mockMech3 = createMech("Kintaro", "KTO-18", "Dragonpunch");
        mockMech.setOwner(player);
        mockMech2.setOwner(player);
        mockMech3.setOwner(player);
        game.setEntity(0, mockMech);
        game.setEntity(1, mockMech2);
        game.setEntity(2, mockMech3);


        // Load ammo in 'mechs; locations are for fun
        Mounted bin1 = mockMech.addEquipment(mockAC20AmmoType, Mech.LOC_CT);
        Mounted bin2 = mockMech.addEquipment(mockAC20AmmoType, Mech.LOC_CT);
        Mounted bin3 = mockMech2.addEquipment(mockLRM15AmmoType, Mech.LOC_LT);
        Mounted bin4 = mockMech2.addEquipment(mockLRM15AmmoType, Mech.LOC_RT);
        Mounted bin5 = mockMech3.addEquipment(mockSRM6AmmoType, Mech.LOC_LT);
        Mounted bin6 = mockMech3.addEquipment(mockSRM6AmmoType, Mech.LOC_RT);
        Mounted bin7 = mockMech3.addEquipment(mockSRM6AmmoType, Mech.LOC_CT);

        // Just check that the bins are populated still
        tlg.randomizeBotTeamConfiguration(game, team);

        for (Mounted bin: List.of(bin1, bin2, bin3, bin4, bin5, bin6, bin7)) {
            assertNotEquals("", ((AmmoType) bin.getType()).getSubMunitionName());
        }

    }

    @Test
    void testAmmoTypeIllegalBeforeCreation() {
        TeamLoadoutGenerator tlg = new TeamLoadoutGenerator(cg);
        AmmoType aType = (AmmoType) EquipmentType.get("IS Arrow IV Ammo");
        AmmoType mType = AmmoType.getMunitionsFor(aType.getAmmoType()).stream().filter(m -> m.getSubMunitionName().contains("ADA")).findFirst().orElse(null);
        // Should be available by default in 3151
        assertTrue(tlg.checkLegality(mType, "IS", "IS", false));

        // Set year back to 3025
        when(mockGameOptions.intOption(OptionsConstants.ALLOWED_YEAR)).thenReturn(3025);
        tlg.updateOptionValues();
        assertFalse(tlg.checkLegality(mType, "IS", "IS", false));
    }
}