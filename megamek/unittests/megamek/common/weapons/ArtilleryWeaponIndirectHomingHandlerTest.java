/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common.weapons;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;
import java.util.stream.Collectors;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.actions.ArtilleryAttackAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.BombLoadout;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.equipment.enums.BombType.BombTypeEnum;
import megamek.common.exceptions.LocationFullException;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.IBomber;
import megamek.common.units.IBuilding;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.weapons.handlers.TAGHandler;
import megamek.common.weapons.handlers.artillery.ArtilleryWeaponIndirectHomingHandler;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ArtilleryWeaponIndirectHomingHandlerTest {
    private Player aPlayer;
    private Player dPlayer;
    private TWGameManager gameManager;
    private Game game;
    private Server server;
    static WeaponType tagType = (WeaponType) EquipmentType.get("IS TAG");

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
        when(options.booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_AMS)).thenReturn(false);
        when(options.booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_ADV_POINT_DEFENSE)).thenReturn(false);
        when(options.stringOption(OptionsConstants.ALLOWED_TECH_LEVEL)).thenReturn("Experimental");
        game.setOptions(options);

        // Board
        Board mockBoard = mock(Board.class);
        when(mockBoard.contains(any(Coords.class))).thenReturn(true);
        Hex mockHex = mock(Hex.class);
        when(mockHex.getLevel()).thenReturn(0);
        when(mockHex.containsTerrain(anyInt())).thenReturn(false);
        when(mockBoard.getHex(any(Coords.class))).thenReturn(mockHex);
        when(mockBoard.getBuildings()).thenReturn(new Vector<IBuilding>().elements());
        when(mockBoard.getSpecialHexDisplayTable()).thenReturn(new Hashtable<>());
        game.setBoard(mockBoard);

        server = ServerFactory.createServer(gameManager);
    }

    @AfterEach
    void tearDown() {
        server.die();
    }

    Mek createMek(String chassis, String model, String crewName, Player owner) {
        return createMek(chassis, model, crewName, owner, 4, 5);
    }

    Mek createMek(String chassis, String model, String crewName, Player owner, int gSkill, int pSkill) {
        // Create a real Mek with some mocked fields
        Mek mockMek = new BipedMek();
        mockMek.setGame(game);
        mockMek.setChassis(chassis);
        mockMek.setModel(model);

        Crew mockCrew = new Crew(CrewType.SINGLE);
        mockCrew.setGunnery(gSkill, mockCrew.getCrewType().getGunnerPos());
        mockCrew.setPiloting(pSkill, mockCrew.getCrewType().getPilotPos());
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
        return createInfantry(chassis, model, crewName, owner, 5, 8);
    }

    Infantry createInfantry(String chassis, String model, String crewName, Player owner, int gSkill, int pSkill) {
        // Create a real Infantry unit with some mocked fields
        Infantry mockInfantry = new Infantry();
        mockInfantry.setGame(game);
        mockInfantry.setChassis(chassis);
        mockInfantry.setModel(model);

        Crew mockCrew = new Crew(CrewType.INFANTRY_CREW);
        mockCrew.setGunnery(gSkill, mockCrew.getCrewType().getGunnerPos());
        mockCrew.setPiloting(pSkill, mockCrew.getCrewType().getPilotPos());
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
            mockInfantry.setPrimaryWeapon((InfantryWeapon) InfantryWeapon.get(EquipmentTypeLookup.INFANTRY_ASSAULT_RIFLE));
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
        mockCrew.setGunnery(gSkill, mockCrew.getCrewType().getGunnerPos());
        mockCrew.setPiloting(pSkill, mockCrew.getCrewType().getPilotPos());
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

    ToHitData makeAutoHitHomingTHD() {
        // Not actually skill, but rather auto-miss cutoff
        return makeTHD(2, "Homing shot (will miss if TAG misses)");
    }

    WeaponAttackAction makeWAA(Entity attacker, Entity defender, Mounted<?> weapon) {
        return new WeaponAttackAction(attacker.getId(), defender.getId(), attacker.getEquipmentNum(weapon));
    }

    ArtilleryAttackAction makeArtilleryWAA(Entity attacker, Entity defender, Mounted<?> weapon) {
        return new ArtilleryAttackAction(attacker.getId(),
              defender.getTargetType(),
              defender.getId(),
              attacker.getEquipmentNum(weapon),
              game);
    }

    void loadBombOnASF(Entity bomber, BombTypeEnum bombType) {
        loadBombsOnASF(bomber, bombType, 1);
    }

    void loadBombsOnASF(Entity bomber, BombTypeEnum bombType, int count) {
        BombLoadout bombsArray = new BombLoadout();
        bombsArray.put(bombType, count);
        ((IBomber) bomber).setExtBombChoices(bombsArray);
        ((IBomber) bomber).applyBombs();
    }

    /**
     * This test actually exists to quickly iterate through artillery handling code, for debugging purposes. It iterates
     * through one Homing Arrow IV bomb launch, the subsequent tagging turn, and the homing attack conversion /
     * handling.
     */
    @Test
    void handleArrowIVHomingBombTargetMekWithTAG() throws LocationFullException, EntityLoadingException {
        // Create and load entities
        AeroSpaceFighter attacker = createASF("ATT-10", "Buzzsaw", "Alyce", aPlayer);
        loadBombOnASF(attacker, BombTypeEnum.HOMING);
        Mek tagger = createMek("TAG-3R", "Taggity", "Taggart", aPlayer, 1, 1);
        Mounted<?> tagWeapon = tagger.addEquipment(tagType, Mek.LOC_CENTER_TORSO);
        Mek defender = createMek("TGT-1A", "Targeto", "Bob", dPlayer);
        Infantry crunchies = createInfantry("LittleGreen", "ArmyMen", "Elgato", dPlayer);

        // Set positions; critical for WAA
        Coords attackerPosition = new Coords(8, 15);
        Coords taggerPosition = new Coords(1, 1);
        Coords defenderPosition = new Coords(8, 1);
        attacker.setPosition(attackerPosition);
        tagger.setPosition(taggerPosition);
        defender.setPosition(defenderPosition);
        crunchies.setPosition(defenderPosition);

        // Create TAG WAA and handler
        WeaponAttackAction tagWAA = makeWAA(tagger, defender, tagWeapon);
        TAGHandler tagHandler = new TAGHandler(makeTHD(2), tagWAA, game, gameManager);

        // Create Artillery WAA and handler
        ArtilleryAttackAction artilleryAttackAction = makeArtilleryWAA(attacker, defender, attacker.getWeapon(0));
        ArtilleryWeaponIndirectHomingHandler artie = new ArtilleryWeaponIndirectHomingHandler(makeAutoHitHomingTHD(),
              artilleryAttackAction,
              game,
              gameManager);

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
        assertFalse(tagHandler.handle(game.getPhase(), reports));
        // This should not signal any further attacks to be processed
        assertFalse(artie.handle(game.getPhase(), reports));

        // Change phase and check that the target was destroyed.
        gameManager.changePhase(GamePhase.FIRING);
        assertTrue(defender.isDestroyed());
        // Infantry shouldn't be fully destroyed, though!
        assertFalse(crunchies.isDestroyed());

        // for debugging purposes only. Use debug mode, place breakpoint above, evaluate manually
        reports.stream().map(Report::text).collect(Collectors.joining(",\n"));

    }

    @Test
    void handleArrowIVHomingBombTargetMekWithASFTAG() throws EntityLoadingException {
        // Create and load entities
        AeroSpaceFighter attacker = createASF("ATT-10", "Buzzsaw", "Alyce", aPlayer);
        loadBombOnASF(attacker, BombTypeEnum.HOMING);
        AeroSpaceFighter tagger = createASF("TAGA-10", "Sky Eye", "MacTaggert", aPlayer);
        loadBombOnASF(tagger, BombTypeEnum.TAG);
        Mek defender = createMek("TGT-1A", "Targeto", "Bob", dPlayer);

        // Set positions; critical for WAA
        Coords attackerPosition = new Coords(8, 15);
        Coords taggerPosition = new Coords(1, 1);
        Coords defenderPosition = new Coords(8, 1);
        attacker.setPosition(attackerPosition);
        tagger.setPosition(taggerPosition);
        defender.setPosition(defenderPosition);

        // Create Artillery WAA and handler
        ArtilleryAttackAction artilleryAttackAction = makeArtilleryWAA(attacker, defender, attacker.getWeapon(0));
        ArtilleryWeaponIndirectHomingHandler artie = new ArtilleryWeaponIndirectHomingHandler(makeAutoHitHomingTHD(),
              artilleryAttackAction,
              game,
              gameManager);

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
        TAGHandler tagHandler = new TAGHandler(makeTHD(2), tagWAA, game, gameManager);

        assertFalse(tagHandler.handle(game.getPhase(), reports));
        assertFalse(artie.handle(game.getPhase(), reports));

        // Change phase and check that the target was destroyed.
        gameManager.changePhase(GamePhase.FIRING);
        assertTrue(defender.isDestroyed());

        // for debugging purposes only. Use debug mode, place breakpoint above, evaluate manually
        reports.stream().map(Report::text).collect(Collectors.joining(",\n"));

    }
}
