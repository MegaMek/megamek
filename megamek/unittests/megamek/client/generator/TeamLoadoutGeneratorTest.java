package megamek.client.generator;

import megamek.client.Client;
import megamek.client.ui.swing.ClientGUI;
import megamek.common.*;
import megamek.common.containers.MunitionTree;
import megamek.common.options.GameOptions;
import megamek.common.options.Option;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import org.apache.commons.collections4.IteratorUtils;
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
    }

    @BeforeEach
    void setUp() {
        when(cg.getClient()).thenReturn(client);
        when(cg.getClient().getGame()).thenReturn(game);
        game.setOptions(mockGameOptions);

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

        team.addPlayer(player);
        game.addPlayer(0, player);
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
        when(mockCrew.getNames()).thenReturn(new String[] { crewName });
        when(mockCrew.getOptions()).thenReturn(pOpt);
        mockMech.setCrew(mockCrew);

        return mockMech;
    }

    @Test
    void testReconfigureEntityFallbackAmmoType() throws LocationFullException {
        TeamLoadoutGenerator tlg = new TeamLoadoutGenerator(game);
        Mech mockMech = createMech("Mauler", "MAL-1K", "Tyson");
        Mounted bin1 = mockMech.addEquipment(mockAC5AmmoType, Mech.LOC_LT);
        Mounted bin2 = mockMech.addEquipment(mockAC5AmmoType, Mech.LOC_LT);
        Mounted bin3 = mockMech.addEquipment(mockAC5AmmoType, Mech.LOC_LT);
        Mounted bin4 = mockMech.addEquipment(mockAC5AmmoType, Mech.LOC_LT);

        // Create a set of imperatives, some of which won't work
        MunitionTree mt = new MunitionTree();
        mt.insertImperative("Mauler", "MAL-1K", "any", "AC/5", "Inferno:Standard:Smoke:Flak");
        tlg.reconfigureEntity(mockMech, mt, "IS");

        // First imperative entry is invalid, so bin1 should get second choice
        // (Standard)
        assertTrue(((AmmoType) bin1.getType()).getMunitionType().contains(AmmoType.Munitions.M_STANDARD));
        // Third choice is invalid, so 2nd bin gets 4th choice, Flak
        assertTrue(((AmmoType) bin2.getType()).getMunitionType().contains(AmmoType.Munitions.M_FLAK));
        // Now two bins are left over, so they're filled with the _new_ default,
        // Standard (choice #2)
        assertTrue(((AmmoType) bin3.getType()).getMunitionType().contains(AmmoType.Munitions.M_STANDARD));
        assertTrue(((AmmoType) bin4.getType()).getMunitionType().contains(AmmoType.Munitions.M_STANDARD));
    }

    @Test
    void testReconfigureEntityMechNoAmmoTypesRequested() throws LocationFullException {
        TeamLoadoutGenerator tlg = new TeamLoadoutGenerator(game);

        Mech mockMech = createMech("Catapult", "CPLT-C1", "J. Robert Hoppenheimer");
        Mounted bin1 = mockMech.addEquipment(mockLRM15AmmoType, Mech.LOC_LT);
        Mounted bin2 = mockMech.addEquipment(mockLRM15AmmoType, Mech.LOC_RT);
        MunitionTree mt = new MunitionTree();

        // We expect to see no change in loadouts
        tlg.reconfigureEntity(mockMech, mt, "IS");
        assertTrue(((AmmoType) bin1.getType()).getMunitionType().contains(AmmoType.Munitions.M_STANDARD));
        assertTrue(((AmmoType) bin2.getType()).getMunitionType().contains(AmmoType.Munitions.M_STANDARD));
    }

    @Test
    void testReconfigureEntityMechOneAmmoType() throws LocationFullException {
        TeamLoadoutGenerator tlg = new TeamLoadoutGenerator(game);

        Mech mockMech = createMech("Catapult", "CPLT-C1", "J. Robert Hoppenheimer");
        Mounted bin1 = mockMech.addEquipment(mockLRM15AmmoType, Mech.LOC_LT);
        Mounted bin2 = mockMech.addEquipment(mockLRM15AmmoType, Mech.LOC_RT);

        MunitionTree mt = new MunitionTree();
        mt.insertImperative("Catapult", "CPLT-C1", "any", "LRM-15", "Dead-Fire");

        // We expect that all bins are set to the desired munition type as only one type
        // is provided
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
        TeamLoadoutGenerator tlg = new TeamLoadoutGenerator(game);

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

        // We expect that all bins are set to the desired munition type as only one type
        // is provided
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
        TeamLoadoutGenerator tlg = new TeamLoadoutGenerator(game);

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

        // Set up two loadouts: one for a named pilot, and one for all LRMs on any
        // Catapults
        MunitionTree mt = new MunitionTree();
        mt.insertImperative("Catapult", "CPLT-C1", "J. Robert Hoppenheimer", "LRM-15", "Standard", "Dead-Fire",
                "Heat-Seeking", "Smoke");
        mt.insertImperative("Catapult", "any", "any", "LRM", "Standard", "Swarm", "Semi-guided");

        // J. Robert H. should get the first loadout
        tlg.reconfigureEntity(mockMech, mt, "IS");
        assertTrue(((AmmoType) bin1.getType()).getMunitionType().contains(AmmoType.Munitions.M_STANDARD));
        assertTrue(((AmmoType) bin2.getType()).getMunitionType().contains(AmmoType.Munitions.M_DEAD_FIRE));
        assertTrue(((AmmoType) bin3.getType()).getMunitionType().contains(AmmoType.Munitions.M_HEAT_SEEKING));
        assertTrue(((AmmoType) bin4.getType()).getMunitionType().contains(AmmoType.Munitions.M_SMOKE_WARHEAD));

        // John Q. should get the generalized loadout; last bin should be set to
        // Standard
        tlg.reconfigureEntity(mockMech2, mt, "IS");
        assertTrue(((AmmoType) bin5.getType()).getMunitionType().contains(AmmoType.Munitions.M_STANDARD));
        assertTrue(((AmmoType) bin6.getType()).getMunitionType().contains(AmmoType.Munitions.M_SWARM));
        assertTrue(((AmmoType) bin7.getType()).getMunitionType().contains(AmmoType.Munitions.M_SEMIGUIDED));
        assertTrue(((AmmoType) bin8.getType()).getMunitionType().contains(AmmoType.Munitions.M_STANDARD));
    }

    @Test
    void testReconfigureTeamOfMechs()  throws LocationFullException {
        TeamLoadoutGenerator tlg = new TeamLoadoutGenerator(game);
        ReconfigurationParameters rp = new ReconfigurationParameters();
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

        tlg.reconfigureEntities(game.getPlayerEntities(player, false), "FS", mt, rp);

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
        TeamLoadoutGenerator tlg = new TeamLoadoutGenerator(game);
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
        tlg.randomizeBotTeamConfiguration(team, "FWL");

        for (Mounted bin : List.of(bin1, bin2, bin3, bin4, bin5, bin6, bin7)) {
            assertNotEquals("", ((AmmoType) bin.getType()).getSubMunitionName());
        }
    }

    @Test
    void testLoadEntityListTwoEntities() throws LocationFullException {
        TeamLoadoutGenerator tlg = new TeamLoadoutGenerator(game);
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

        MunitionTree original = new MunitionTree();
        original.loadEntityList(game.getPlayerEntities(player, false));
        tlg.randomizeBotTeamConfiguration(team, "CCY");
    }

    @Test
    void testReconfigureBotTeamNoEnemyInfo()  throws LocationFullException {
        TeamLoadoutGenerator tlg = new TeamLoadoutGenerator(game);
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
        tlg.reconfigureTeam(team, "CL", "");

        for (Mounted bin : List.of(bin1, bin2, bin3, bin4, bin5, bin6, bin7)) {
            assertNotEquals("", ((AmmoType) bin.getType()).getSubMunitionName());
        }
    }

    // Section: legalityCheck tests
    @Test
    void testAmmoTypeIllegalByTechLevel() {
        TeamLoadoutGenerator tlg = new TeamLoadoutGenerator(game);
        AmmoType aType = (AmmoType) EquipmentType.get("IS Arrow IV Ammo");
        AmmoType mType = AmmoType.getMunitionsFor(aType.getAmmoType()).stream()
                .filter(m -> m.getSubMunitionName().contains("ADA")).findFirst().orElse(null);
        // Set game tech level to Standard and update generator
        when(mockGameOptions.stringOption(OptionsConstants.ALLOWED_TECHLEVEL)).thenReturn("Standard");
        tlg.updateOptionValues();

        // Should not be available to anyone
        assertFalse(tlg.checkLegality(mType, "CC", "IS", false));
        assertFalse(tlg.checkLegality(mType, "FS", "IS", false));
        assertFalse(tlg.checkLegality(mType, "IS", "IS", false));
        assertFalse(tlg.checkLegality(mType, "CLAN", "CL", false));
        assertFalse(tlg.checkLegality(mType, "CLAN", "CL", true));

        // Should be available to everyone
        when(mockGameOptions.stringOption(OptionsConstants.ALLOWED_TECHLEVEL)).thenReturn("Advanced");
        tlg.updateOptionValues();
        assertTrue(tlg.checkLegality(mType, "CC", "IS", false));
        assertTrue(tlg.checkLegality(mType, "FS", "IS", false));
        assertTrue(tlg.checkLegality(mType, "IS", "IS", false));
        assertTrue(tlg.checkLegality(mType, "CLAN", "CL", true));
        assertTrue(tlg.checkLegality(mType, "CLAN", "CL", true));
    }

    @Test
    void testAmmoTypeIllegalBeforeCreation() {
        TeamLoadoutGenerator tlg = new TeamLoadoutGenerator(game);
        AmmoType aType = (AmmoType) EquipmentType.get("IS Arrow IV Ammo");
        AmmoType mType = AmmoType.getMunitionsFor(aType.getAmmoType()).stream()
                .filter(m -> m.getSubMunitionName().contains("ADA")).findFirst().orElse(null);
        // Should be available by default in 3151, including to Clans (using MixTech)
        assertTrue(tlg.checkLegality(mType, "CC", "IS", false));
        assertTrue(tlg.checkLegality(mType, "FS", "IS", false));
        assertTrue(tlg.checkLegality(mType, "IS", "IS", false));
        // Check mixed-tech and regular Clan tech, which should match IS at this point
        assertTrue(tlg.checkLegality(mType, "CLAN", "CL", true));
        assertTrue(tlg.checkLegality(mType, "CLAN", "CL", false));

        // Set year back to 3025
        when(mockGameOptions.intOption(OptionsConstants.ALLOWED_YEAR)).thenReturn(3025);
        tlg.updateOptionValues();
        assertFalse(tlg.checkLegality(mType, "CC", "IS", false));
        assertFalse(tlg.checkLegality(mType, "FS", "IS", false));
        assertFalse(tlg.checkLegality(mType, "IS", "IS", false));
        assertFalse(tlg.checkLegality(mType, "CLAN", "CL", true));

        // Move up to 3070. Because of game settings and lack of "Common" year, ADA
        // becomes available
        // everywhere (at least in the IS) immediately after its inception.
        when(mockGameOptions.intOption(OptionsConstants.ALLOWED_YEAR)).thenReturn(3070);
        tlg.updateOptionValues();
        assertTrue(tlg.checkLegality(mType, "CC", "IS", false));
        assertFalse(tlg.checkLegality(mType, "FS", "IS", false));
        assertFalse(tlg.checkLegality(mType, "IS", "IS", false));
        assertFalse(tlg.checkLegality(mType, "CLAN", "CL", true));
    }

    @Test
    void testMunitionWeightCollectionTopN() {
        MunitionWeightCollection mwc = new MunitionWeightCollection();
        // Default weighting for all munition types.
        // For missiles, "Dead-Fire" is first, followed by "Standard" by default.
        // For other rounds, "Standard" should be first.
        HashMap<String, List<String>> topN = mwc.getTopN(3);

        assertTrue(topN.get("LRM").get(0).contains("Dead-Fire"));
        assertTrue(topN.get("LRM").get(1).contains("Standard"));
        assertTrue(topN.get("SRM").get(0).contains("Dead-Fire"));
        assertTrue(topN.get("SRM").get(1).contains("Standard"));

        assertTrue(topN.get("AC").get(0).contains("Standard"));
        assertTrue(topN.get("Arrow IV").get(0).contains("Standard"));
    }

    @Test
    void testAPMunitionWeightCollectionTopN() {
        MunitionWeightCollection mwc = new MunitionWeightCollection();
        // Assume we're up against reflective and heavy targets, not fliers
        mwc.increaseAPMunitions();
        mwc.decreaseFlakMunitions();

        HashMap<String, List<String>> topN = mwc.getTopN(3);
        assertEquals("Armor-Piercing=3.0", topN.get("AC").get(0));
        assertEquals("Standard=2.0", topN.get("AC").get(1));
        assertEquals("Caseless=1.0", topN.get("AC").get(2));

        assertEquals("Tandem-Charge=3.0", topN.get("SRM").get(0));
        assertEquals("Dead-Fire=3.0", topN.get("SRM").get(1));
        assertEquals("Standard=2.0", topN.get("SRM").get(2));
    }

    @Test
    void testIncreaseAntiTSMWeightOnly() {
        MunitionWeightCollection mwc = new MunitionWeightCollection();
        ArrayList<String> tsmOnly = new ArrayList(List.of("Anti-TSM"));
        mwc.increaseMunitions(tsmOnly);
        mwc.increaseMunitions(tsmOnly);
        mwc.increaseMunitions(tsmOnly);
        assertEquals(15.0, mwc.getSrmWeights().get("Anti-TSM"));
        assertEquals("Anti-TSM=15.0", mwc.getTopN(1).get("SRM").get(0));
    }

    @Test
    void testNukeToggleDecreasesNukeWeightToZero() {
        ReconfigurationParameters rp = new ReconfigurationParameters();
        rp.nukesBannedForMe = true;
        MunitionWeightCollection mwc = new MunitionWeightCollection();
        TeamLoadoutGenerator tlg = new TeamLoadoutGenerator(game);

        // Have the Munition Tree generator use our pre-made mwc so we can see its
        // changes

        ArrayList<Entity> ownTeamEntities = (ArrayList<Entity>) IteratorUtils.toList(game.getTeamEntities(team));
        MunitionTree mt = tlg.generateMunitionTree(rp, ownTeamEntities, "", mwc);

        assertEquals(0.0, mwc.getArtyWeights().get("Davy Crockett-M"));
        assertEquals(0.0, mwc.getBombWeights().get("AlamoMissile Ammo"));
    }

    @Test
    void testClampAmmoShotsReduceAmmoBinsToZero() throws LocationFullException {
        TeamLoadoutGenerator tlg = new TeamLoadoutGenerator(game);

        Mech mockMech = createMech("Catapult", "CPLT-C1", "J. Robert Hoppenheimer");
        Mounted bin1 = mockMech.addEquipment(mockLRM15AmmoType, Mech.LOC_LT);
        Mounted bin2 = mockMech.addEquipment(mockLRM15AmmoType, Mech.LOC_RT);

        tlg.clampAmmoShots(mockMech, 0.0f);
        assertEquals(0, bin1.getUsableShotsLeft());
        assertEquals(0, bin2.getUsableShotsLeft());
    }

    @Test
    void testClampAmmoShotsPositiveSmallFloatGivesOneShot() throws LocationFullException {
        // LRM15s carry 8 shots, the clamp function should give 1 shot at 10% / 0.1f ratio
        TeamLoadoutGenerator tlg = new TeamLoadoutGenerator(game);

        Mech mockMech = createMech("Catapult", "CPLT-C1", "J. Robert Hoppenheimer");
        Mounted bin1 = mockMech.addEquipment(mockLRM15AmmoType, Mech.LOC_LT);
        Mounted bin2 = mockMech.addEquipment(mockLRM15AmmoType, Mech.LOC_RT);

        tlg.clampAmmoShots(mockMech, 0.1f);
        assertEquals(1, bin1.getUsableShotsLeft());
        assertEquals(1, bin2.getUsableShotsLeft());
    }

    @Test
    void testClampAmmoShotsSetToHalf() throws LocationFullException {
        // LRM15s carry 8 shots, the clamp function should give 4 shot at 40% / 0.5f ratio
        TeamLoadoutGenerator tlg = new TeamLoadoutGenerator(game);

        Mech mockMech = createMech("Catapult", "CPLT-C1", "J. Robert Hoppenheimer");
        Mounted bin1 = mockMech.addEquipment(mockLRM15AmmoType, Mech.LOC_LT);
        Mounted bin2 = mockMech.addEquipment(mockLRM15AmmoType, Mech.LOC_RT);

        tlg.clampAmmoShots(mockMech, 0.5f);
        assertEquals(4, bin1.getUsableShotsLeft());
        assertEquals(4, bin2.getUsableShotsLeft());
    }

    @Test
    void testClampAmmoShotsCannotExceedFull() throws LocationFullException {
        // LRM15s carry 8 shots, the clamp function should give 8 shot at 100% or over
        TeamLoadoutGenerator tlg = new TeamLoadoutGenerator(game);

        Mech mockMech = createMech("Catapult", "CPLT-C1", "J. Robert Hoppenheimer");
        Mounted bin1 = mockMech.addEquipment(mockLRM15AmmoType, Mech.LOC_LT);
        Mounted bin2 = mockMech.addEquipment(mockLRM15AmmoType, Mech.LOC_RT);

        tlg.clampAmmoShots(mockMech, 1.5f);
        assertEquals(8, bin1.getUsableShotsLeft());
        assertEquals(8, bin2.getUsableShotsLeft());
    }
}
