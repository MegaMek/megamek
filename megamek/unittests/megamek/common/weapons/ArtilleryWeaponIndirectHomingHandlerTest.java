package megamek.common.weapons;

import megamek.client.ui.Messages;
import megamek.common.*;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.GamePhase;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.logging.MMLogger;
import megamek.server.Server;
import megamek.server.totalwarfare.TWGameManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;
import java.util.stream.Collectors;

import static megamek.MMConstants.MAX_PORT;
import static megamek.MMConstants.MIN_PORT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArtilleryWeaponIndirectHomingHandlerTest {
    private Player aPlayer;
    private Player dPlayer;
    private TWGameManager gameManager;
    private Game game;
    private Server server;
    private Random random = new Random();
    static WeaponType tagType = (WeaponType) EquipmentType.get("IS TAG");
    private static final MMLogger logger = MMLogger.create(ArtilleryWeaponIndirectHomingHandlerTest.class);

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() throws IOException {
        // Players
        aPlayer = new Player(0, "Attacker");
        dPlayer = new Player(1, "Defender");
        gameManager = new TWGameManager();

        game = gameManager.getGame();
        game.createVictoryConditions();
        game.addPlayer(aPlayer.getId(), aPlayer);
        game.addPlayer(dPlayer.getId(), dPlayer);

        // GameOptions
        GameOptions options = mock(GameOptions.class);
        when(options.booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_AMS)).thenReturn(false);
        when(options.booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_ADV_POINTDEF)).thenReturn(false);
        when(options.stringOption(OptionsConstants.ALLOWED_TECHLEVEL)).thenReturn("Experimental");
        game.setOptions(options);

        // Board
        Board mockBoard = mock(Board.class);
        Hex mockHex = mock(Hex.class);
        when(mockHex.getLevel()).thenReturn(0);
        when(mockHex.containsTerrain(anyInt())).thenReturn(false);
        when(mockBoard.getHex(any(Coords.class))).thenReturn(mockHex);
        when(mockBoard.getBuildings()).thenReturn(new Vector<Building>().elements());
        when(mockBoard.getSpecialHexDisplayTable()).thenReturn(new Hashtable<>());
        game.setBoard(mockBoard);

        server = new Server(null, random.nextInt(MIN_PORT, MAX_PORT), gameManager, false, "", null, true);
    }

    Mek createMek(String chassis, String model, String crewName, Player owner) {
        Mek unit = createMek(chassis, model, crewName, owner, 4, 5);
        return unit;
    }

    Mek createMek(String chassis, String model, String crewName, Player owner, int gSkill, int pSkill) {
        // Create a real Mek with some mocked fields
        Mek mockMek = new BipedMek();
        mockMek.setGame(game);
        mockMek.setChassis(chassis);
        mockMek.setModel(model);

        Crew mockCrew = new Crew(CrewType.SINGLE);
        mockCrew.setGunnery(gSkill);
        mockCrew.setPiloting(pSkill);
        PilotOptions pOpt = new PilotOptions();
        mockCrew.setName(crewName, 0);
        mockCrew.setOptions(pOpt);
        mockMek.setCrew(mockCrew);

        mockMek.setId(game.getNextEntityId());
        game.addEntity(mockMek);
        mockMek.setOwner(owner);

        mockMek.setDeployed(true);

        return mockMek;
    }

    Infantry createInfantry(String chassis, String model, String crewName, Player owner) {
        Infantry unit = createInfantry(chassis, model, crewName, owner, 5, 8);
        return unit;
    }

    Infantry createInfantry(String chassis, String model, String crewName, Player owner, int gSkill, int pSkill) {
        // Create a real Infantry unit with some mocked fields
        Infantry mockInfantry = new Infantry();
        mockInfantry.setGame(game);
        mockInfantry.setChassis(chassis);
        mockInfantry.setModel(model);

        Crew mockCrew = new Crew(CrewType.INFANTRY_CREW);
        mockCrew.setGunnery(gSkill);
        mockCrew.setPiloting(pSkill);
        PilotOptions pOpt = new PilotOptions();
        mockCrew.setName(crewName, 0);
        mockCrew.setOptions(pOpt);
        mockInfantry.setCrew(mockCrew);

        mockInfantry.setSquadSize(7);
        mockInfantry.setSquadCount(4);
        mockInfantry.autoSetInternal();
        try {
            mockInfantry.addEquipment(EquipmentType.get(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE),
                Infantry.LOC_INFANTRY);
            mockInfantry.setPrimaryWeapon((InfantryWeapon) InfantryWeapon
                .get(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE));
        } catch (LocationFullException ex) {
            // do nothing
        }

        mockInfantry.setId(game.getNextEntityId());
        game.addEntity(mockInfantry);
        mockInfantry.setOwner(owner);

        mockInfantry.setDeployed(true);

        return mockInfantry;
    }

    AeroSpaceFighter createASF(String chassis, String model, String crewName, Player owner) {
        AeroSpaceFighter unit = createASF(chassis, model, crewName, owner, 3, 4);
        unit.setOwner(owner);
        return unit;
    }

    AeroSpaceFighter createASF(String chassis, String model, String crewName, Player owner, int gSkill, int pSkill) {
        // Create a real AeroSpaceFighter unit with some mocked fields
        AeroSpaceFighter mockAeroSpaceFighter = new AeroSpaceFighter();
        mockAeroSpaceFighter.setGame(game);
        mockAeroSpaceFighter.setChassis(chassis);
        mockAeroSpaceFighter.setModel(model);

        Crew mockCrew = new Crew(CrewType.SINGLE);
        mockCrew.setGunnery(gSkill);
        mockCrew.setPiloting(pSkill);
        PilotOptions pOpt = new PilotOptions();
        mockCrew.setName(crewName, 0);
        mockCrew.setOptions(pOpt);
        mockAeroSpaceFighter.setCrew(mockCrew);
        mockAeroSpaceFighter.setOwner(owner);

        mockAeroSpaceFighter.setId(game.getNextEntityId());
        game.addEntity(mockAeroSpaceFighter);

        mockAeroSpaceFighter.setDeployed(true);

        return mockAeroSpaceFighter;
    }

    ToHitData makeTHD(int skill) {
        return makeTHD(skill, "Gunnery Skill");
    }

    ToHitData makeTHD(int skill, String message) {
        ToHitData thd = new ToHitData();
        thd.addModifier(skill, message);
        return thd;
    }

    ToHitData makeIndirectTHD(int skill) {
        ToHitData iTHD = makeTHD(skill);
        iTHD.addModifier(7, Messages.getString("WeaponAttackAction.IndirectArty"));
        return iTHD;
    }

    ToHitData makeIndirectHomingTHD() {
        // Not actually skill, but rather auto-miss cutoff
        return makeTHD(4, "Homing shot (will miss if TAG misses)");
    }

    ToHitData makeAutoHitHomingTHD() {
        // Not actually skill, but rather auto-miss cutoff
        return makeTHD(2, "Homing shot (will miss if TAG misses)");
    }

    WeaponAttackAction makeWAA(Entity attacker, Entity defender, Mounted weapon) {
        return new WeaponAttackAction(
            attacker.getId(),
            defender.getId(),
            attacker.getEquipmentNum(weapon)
        );
    }

    ArtilleryAttackAction makeArtilleryWAA(Entity attacker, Entity defender, Mounted weapon) {
        return new ArtilleryAttackAction(
            attacker.getId(),
            defender.getTargetType(),
            defender.getId(),
            attacker.getEquipmentNum(weapon),
            game
        );
    }


    void loadBombOnASF(Entity bomber, int bombType) {
        loadBombsOnASF(bomber, bombType, 1);
    }

    void loadBombsOnASF(Entity bomber, int bombType, int count) {
        int[] bombsArray = new int[BombType.B_NUM];
        bombsArray[bombType] = count;
        ((IBomber) bomber).setExtBombChoices(bombsArray);
        ((IBomber) bomber).applyBombs();
    }

    /**
     * This test actually exists to quickly iterate through artillery handling code,
     * for debugging purposes.
     * It iterates through one Homing Arrow IV bomb launch, the subsequent tagging turn,
     * and the homing attack conversion / handling.
     */
    @Test
    void handleArrowIVHomingBombTargetMekWithTAG() throws LocationFullException {
        // Create and load entities
        AeroSpaceFighter attacker = createASF("ATT-10", "Buzzsaw", "Alyce", aPlayer);
        loadBombOnASF(attacker, BombType.B_HOMING);
        Mek tagger = createMek("TAG-3R", "Taggity", "Taggart", aPlayer, 1, 1);
        Mounted<?> tagWeapon = tagger.addEquipment(tagType, Mek.LOC_CT);
        Mek defender = createMek("TGT-1A", "Targeto", "Bob", dPlayer);
        Infantry crunchies = createInfantry("LittleGreen", "ArmyMen", "Elgato", dPlayer);

        // Set positions; critical for WAA
        Coords attackerPosition = new Coords(8,15);
        Coords taggerPosition = new Coords(1,1);
        Coords defenderPosition = new Coords(8,1);
        attacker.setPosition(attackerPosition);
        tagger.setPosition(taggerPosition);
        defender.setPosition(defenderPosition);
        crunchies.setPosition(defenderPosition);

        // Create TAG WAA and handler
        WeaponAttackAction tagWAA = makeWAA(tagger, defender, tagWeapon);
        TAGHandler taggie = new TAGHandler(makeTHD(2), tagWAA, game, gameManager);

        // Create Artillery WAA and handler
        ArtilleryAttackAction awaa = makeArtilleryWAA(attacker, defender, attacker.getWeapon(0));
        ArtilleryWeaponIndirectHomingHandler artie = new ArtilleryWeaponIndirectHomingHandler(
            makeAutoHitHomingTHD(), awaa, game, gameManager);

        // Set game phase and run handler to simulate initial firing
        game.setPhase(GamePhase.TARGETING);
        Vector<Report> reports = new Vector<>();
        assertTrue(artie.handle(game.getPhase(), reports));

        // set phase to offboard to increment TurnsTilHit
        game.setPhase(GamePhase.OFFBOARD);
        assertTrue(artie.handle(game.getPhase(), reports));

        // Now handle hit turn!
        game.setPhase(GamePhase.OFFBOARD);
        // This should not signal any further attacks to be processed
        assertFalse(taggie.handle(game.getPhase(), reports));
        // This should not signal any further attacks to be processed
        assertFalse(artie.handle(game.getPhase(), reports));

        // Change phase and check that the target was destroyed.
        gameManager.changePhase(GamePhase.FIRING);
        assertTrue(defender.isDestroyed());
        // Infantry shouldn't be fully destroyed, though!
        assertFalse(crunchies.isDestroyed());

        // for debugging purposes only.  Use debug mode, place breakpoint above,
        // evaluate manually
        reports.stream()
            .map(Report::text)
            .collect(Collectors.joining(",\n"));

    }

    @Test
    void handleArrowIVHomingBombTargetMekWithASFTAG() throws LocationFullException {
        // Create and load entities
        AeroSpaceFighter attacker = createASF("ATT-10", "Buzzsaw", "Alyce", aPlayer);
        loadBombOnASF(attacker, BombType.B_HOMING);
        AeroSpaceFighter tagger = createASF("TAGA-10", "Sky Eye", "MacTaggert", aPlayer);
        loadBombOnASF(tagger, BombType.B_TAG);
        Mek defender = createMek("TGT-1A", "Targeto", "Bob", dPlayer);

        // Set positions; critical for WAA
        Coords attackerPosition = new Coords(8,15);
        Coords taggerPosition = new Coords(1,1);
        Coords defenderPosition = new Coords(8,1);
        attacker.setPosition(attackerPosition);
        tagger.setPosition(taggerPosition);
        defender.setPosition(defenderPosition);

        // Create Artillery WAA and handler
        ArtilleryAttackAction awaa = makeArtilleryWAA(attacker, defender, attacker.getWeapon(0));
        ArtilleryWeaponIndirectHomingHandler artie = new ArtilleryWeaponIndirectHomingHandler(
            makeAutoHitHomingTHD(), awaa, game, gameManager);

        // Set game phase and run handler to simulate initial firing
        game.setPhase(GamePhase.TARGETING);
        Vector<Report> reports = new Vector<>();
        assertTrue(artie.handle(game.getPhase(), reports));

        // set phase to offboard to increment TurnsTilHit
        game.setPhase(GamePhase.OFFBOARD);
        assertTrue(artie.handle(game.getPhase(), reports));

        // Now handle hit turn!
        game.setPhase(GamePhase.OFFBOARD);

        // Create TAG WAA and handler after Artillery shot (as a test)
        WeaponAttackAction tagWAA = makeWAA(tagger, defender, tagger.getWeapon(0));
        TAGHandler taggie = new TAGHandler(makeTHD(2), tagWAA, game, gameManager);

        assertFalse(taggie.handle(game.getPhase(), reports));
        assertFalse(artie.handle(game.getPhase(), reports));

        // Change phase and check that the target was destroyed.
        gameManager.changePhase(GamePhase.FIRING);
        assertTrue(defender.isDestroyed());

        // for debugging purposes only.  Use debug mode, place breakpoint above,
        // evaluate manually
        reports.stream()
            .map(Report::text)
            .collect(Collectors.joining(",\n"));

    }
}
