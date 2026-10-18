/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import megamek.common.HexTarget;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.BombLoadout;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.enums.BombType.BombTypeEnum;
import megamek.common.exceptions.LocationFullException;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.ArtilleryHandlerHelper;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The Recon Camera on an airborne aerospace unit (TO:AUE p.150, errata 2013): it spots a ground unit on the map under
 * its low-altitude hex with Gunnery +2 from Altitudes 5 to 10, makes no other attack when it has used the camera,
 * reveals hidden units it flies over in Reveal mode, and can be carried as a bomb pod.
 */
class ReconCameraAerospaceTest {

    private static final int GROUND_BOARD = 0;
    private static final int SKY_BOARD = 1;
    private static final Coords SKY_HEX_OVER_GROUND = new Coords(3, 3);

    private Game game;
    private Player cameraOwner;
    private Player enemy;
    private AeroSpaceFighter fighter;
    private BipedMek groundTarget;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() throws LocationFullException {
        game = new Game();
        cameraOwner = new Player(0, "Camera side");
        cameraOwner.setTeam(1);
        enemy = new Player(1, "Enemy");
        enemy.setTeam(2);
        game.addPlayer(0, cameraOwner);
        game.addPlayer(1, enemy);
        Board groundBoard = BoardLoader.initializeBoard("""
              size 16 17
              hex 0101 0 "woods:1" ""
              end""");
        Board skyBoard = Board.getSkyBoard(8, 8);
        groundBoard.setBoardId(GROUND_BOARD);
        skyBoard.setBoardId(SKY_BOARD);
        game.setBoard(GROUND_BOARD, groundBoard);
        game.setBoard(SKY_BOARD, skyBoard);
        skyBoard.setEmbeddedBoard(GROUND_BOARD, SKY_HEX_OVER_GROUND);
        groundBoard.setEnclosingBoard(SKY_BOARD);
        game.setPhase(GamePhase.OFFBOARD);

        fighter = new AeroSpaceFighter();
        fighter.setGame(game);
        fighter.setId(game.getNextEntityId());
        fighter.setChassis("Spotter");
        fighter.setModel("S-1");
        Crew crew = new Crew(CrewType.SINGLE);
        crew.setGunnery(4, 0);
        fighter.setCrew(crew);
        fighter.setOwner(cameraOwner);
        fighter.setBoardId(SKY_BOARD);
        fighter.setPosition(SKY_HEX_OVER_GROUND);
        fighter.setAltitude(7);
        fighter.setDeployed(true);
        fighter.addEquipment(EquipmentType.get("ISReconCamera"), Aero.LOC_NOSE);
        game.addEntity(fighter);

        groundTarget = placeGroundUnit("Target", enemy, new Coords(4, 4));
    }

    private BipedMek placeGroundUnit(String name, Player owner, Coords position) {
        BipedMek mek = new BipedMek();
        mek.setGame(game);
        mek.setId(game.getNextEntityId());
        mek.setChassis(name);
        mek.setModel("T-1");
        mek.setCrew(new Crew(CrewType.SINGLE));
        mek.setOwner(owner);
        mek.setBoardId(GROUND_BOARD);
        mek.setPosition(position);
        mek.setDeployed(true);
        game.addEntity(mek);
        return mek;
    }

    @Test
    void testAFighterSpotsAGroundUnitOnTheMapBelowItsHex() {
        assertTrue(ReconCameraRules.isAerospaceCamera(fighter));
        assertNull(ReconCameraRules.spotRefusal(game, fighter, groundTarget));
        assertTrue(fighter.isEligibleForOffboard(), "the fighter gets an Off-Board turn to spot");
    }

    @Test
    void testTheFighterRollIsGunneryPlusTwo() {
        assertEquals(6, ReconCameraRules.spotToHit(game, fighter, groundTarget).getValue(),
              "gunnery 4 + 2, with no range modifier");
    }

    @Test
    void testTheCameraWorksFromAltitudeFiveToTen() {
        fighter.setAltitude(4);
        assertNotNull(ReconCameraRules.spotRefusal(game, fighter, groundTarget), "too low at Altitude 4");
        fighter.setAltitude(5);
        assertNull(ReconCameraRules.spotRefusal(game, fighter, groundTarget));
        fighter.setAltitude(10);
        assertNull(ReconCameraRules.spotRefusal(game, fighter, groundTarget),
              "Altitude 9 and 10 are the camera's exception to the Altitude 8 sight limit");
        fighter.setAltitude(11);
        assertNotNull(ReconCameraRules.spotRefusal(game, fighter, groundTarget), "too high at Altitude 11");
    }

    @Test
    void testTheTargetMustBeOnTheMapUnderTheFightersOwnHex() {
        fighter.setPosition(new Coords(5, 5));

        assertNotNull(ReconCameraRules.spotRefusal(game, fighter, groundTarget),
              "the ground map lies under another hex of the low-altitude map");
    }

    @Test
    void testAnAirborneUnitCannotBeSpotted() {
        AeroSpaceFighter enemyFighter = new AeroSpaceFighter();
        enemyFighter.setGame(game);
        enemyFighter.setId(game.getNextEntityId());
        enemyFighter.setCrew(new Crew(CrewType.SINGLE));
        enemyFighter.setOwner(enemy);
        enemyFighter.setBoardId(SKY_BOARD);
        enemyFighter.setPosition(SKY_HEX_OVER_GROUND);
        enemyFighter.setAltitude(6);
        enemyFighter.setDeployed(true);
        game.addEntity(enemyFighter);

        assertNotNull(ReconCameraRules.spotRefusal(game, fighter, enemyFighter));
    }

    @Test
    void testAFighterThatUsedItsCameraMakesNoOtherAttack() {
        assertFalse(ReconCameraRules.forbidsOtherAttacks(fighter));

        fighter.setReconCameraSpotResult(groundTarget.getId());

        assertTrue(ReconCameraRules.forbidsOtherAttacks(fighter));
    }

    @Test
    void testAGroundUnitThatSpottedStillAttacks() throws LocationFullException {
        BipedMek groundCamera = placeGroundUnit("Ground camera", cameraOwner, new Coords(2, 2));
        groundCamera.addEquipment(EquipmentType.get("ISReconCamera"), Mek.LOC_CENTER_TORSO);

        groundCamera.setReconCameraSpotResult(groundTarget.getId());

        assertFalse(ReconCameraRules.forbidsOtherAttacks(groundCamera));
    }

    @Test
    void testTheCameraNoLongerGivesAFighterTheOrdinarySpot() {
        assertFalse(fighter.canSpot(), "the camera spots through its own roll now");
    }

    @Test
    void testACameraSpotMarksTheArtilleryTargetHex() {
        fighter.setReconCameraSpotResult(groundTarget.getId());

        assertTrue(ReconCameraRules.isCameraSpottingAt(game, fighter, groundTarget));
        assertTrue(ReconCameraRules.isCameraSpottingAt(game, fighter,
              new HexTarget(groundTarget.getPosition(), GROUND_BOARD, HexTarget.TYPE_HEX_ARTILLERY)));
        assertFalse(ReconCameraRules.isCameraSpottingAt(game, fighter,
              new HexTarget(new Coords(9, 9), GROUND_BOARD, HexTarget.TYPE_HEX_ARTILLERY)));
    }

    @Test
    void testArtilleryAcceptsTheFighterThatCameraSpottedTheTargetHex() {
        HexTarget impactHex = new HexTarget(groundTarget.getPosition(), GROUND_BOARD, HexTarget.TYPE_HEX_ARTILLERY);

        assertTrue(ArtilleryHandlerHelper.findSpotter(List.of(), cameraOwner.getId(), game, impactHex).isEmpty(),
              "an airborne unit is no artillery spotter without a camera spot");

        fighter.setReconCameraSpotResult(groundTarget.getId());

        assertEquals(Optional.of(fighter),
              ArtilleryHandlerHelper.findSpotter(List.of(), cameraOwner.getId(), game, impactHex));
    }

    @Test
    void testInRevealModeTheCameraDoesNotSpot() {
        setCameraMode(ReconCameraRules.MODE_REVEAL);

        assertTrue(ReconCameraRules.isInRevealMode(fighter));
        assertNotNull(ReconCameraRules.spotRefusal(game, fighter, groundTarget));
        assertTrue(ReconCameraRules.canRevealHiddenUnits(fighter));
    }

    @Test
    void testAGroundUnitCannotRevealHiddenUnits() throws LocationFullException {
        BipedMek groundCamera = placeGroundUnit("Ground camera", cameraOwner, new Coords(2, 2));
        groundCamera.addEquipment(EquipmentType.get("ISReconCamera"), Mek.LOC_CENTER_TORSO);
        for (Mounted<?> mounted : groundCamera.getMisc()) {
            mounted.setMode(ReconCameraRules.MODE_REVEAL);
        }

        assertFalse(ReconCameraRules.canRevealHiddenUnits(groundCamera), "only aerospace units reveal (Xotl)");
    }

    @Test
    void testOnlyHostileHiddenUnitsUnderTheFlightPathGetARoll() {
        BipedMek hiddenEnemy = placeGroundUnit("Hidden enemy", enemy, new Coords(8, 8));
        hiddenEnemy.setHidden(true);
        BipedMek hiddenFriend = placeGroundUnit("Hidden friend", cameraOwner, new Coords(9, 9));
        hiddenFriend.setHidden(true);

        List<Entity> underFlight = ReconCameraRules.hiddenUnitsBelowFlightPath(game, fighter);

        assertEquals(List.of(hiddenEnemy), underFlight);
    }

    @Test
    void testTheRevealRollIsNinePlusTerrain() {
        BipedMek inTheOpen = placeGroundUnit("Open", enemy, new Coords(2, 12));
        BipedMek inWoods = placeGroundUnit("Woods", enemy, new Coords(0, 0));

        assertEquals(9, ReconCameraRules.revealTargetNumber(game, inTheOpen).getValue());
        assertEquals(10, ReconCameraRules.revealTargetNumber(game, inWoods).getValue(), "light woods adds 1");
    }

    @Test
    void testABombPodGivesTheFighterACamera() {
        AeroSpaceFighter podCarrier = new AeroSpaceFighter();
        podCarrier.setGame(game);
        podCarrier.setId(game.getNextEntityId());
        podCarrier.setCrew(new Crew(CrewType.SINGLE));
        podCarrier.setOwner(cameraOwner);
        podCarrier.setWeight(50);
        podCarrier.autoSetMaxBombPoints();
        game.getOptions().getOption(OptionsConstants.ALLOWED_TECH_LEVEL).setValue("Advanced");
        BombLoadout loadout = new BombLoadout();
        loadout.put(BombTypeEnum.RECON_CAMERA, 1);
        podCarrier.setExtBombChoices(loadout);

        assertFalse(podCarrier.hasWorkingMisc(MiscType.F_RECON_CAMERA));
        podCarrier.applyBombs();

        assertTrue(podCarrier.hasWorkingMisc(MiscType.F_RECON_CAMERA),
              "the pod mounts a working camera");
        assertTrue(podCarrier.getAmmo().isEmpty(), "a camera pod is equipment, not a bomb to drop");
    }

    private void setCameraMode(String mode) {
        for (Mounted<?> mounted : fighter.getMisc()) {
            mounted.setMode(mode);
        }
    }
}
