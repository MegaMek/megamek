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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.List;

import megamek.common.Player;
import megamek.common.SimpleTechLevel;
import megamek.common.TagInfo;
import megamek.common.ToHitData;
import megamek.common.actions.ReconCameraSpotAction;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.GamePhase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.exceptions.LocationFullException;
import megamek.common.game.Game;
import megamek.common.game.GameTurn;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.Weapon;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The Recon Camera as a ground unit uses it (TO:AUE p.150, errata 2013): who may spot, the roll it takes (a TAG shot's
 * roll, with Light TAG ranges for a ProtoMek), and what a hit does for the units it spots for.
 */
class ReconCameraRulesTest {

    private static final String BOARD_DATA = """
          size 20 20
          end""";
    private static final Coords CAMERA_HEX = new Coords(5, 5);

    private Game game;
    private Player cameraOwner;
    private Player enemy;
    private BipedMek camera;

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
        Board board = BoardLoader.initializeBoard(BOARD_DATA);
        game.setBoard(board);
        game.setPhase(GamePhase.OFFBOARD);

        camera = placeMek("Camera", cameraOwner, CAMERA_HEX);
        camera.addEquipment(EquipmentType.get("ISReconCamera"), Mek.LOC_CENTER_TORSO);
    }

    private BipedMek placeMek(String name, Player owner, Coords position) {
        BipedMek mek = new BipedMek();
        mek.setGame(game);
        mek.setId(game.getNextEntityId());
        mek.setChassis(name);
        mek.setModel("T-1");
        Crew crew = new Crew(CrewType.SINGLE);
        crew.setGunnery(4, 0);
        mek.setCrew(crew);
        mek.setOwner(owner);
        mek.setPosition(position);
        mek.setDeployed(true);
        game.addEntity(mek);
        return mek;
    }

    private BipedMek enemyAt(int distance) {
        return placeMek("Enemy", enemy, new Coords(CAMERA_HEX.getX(), CAMERA_HEX.getY() + distance));
    }

    @Test
    void testTheCameraIsAdvancedTechWithRatingC() {
        MiscType reconCamera = (MiscType) EquipmentType.get("ISReconCamera");

        assertEquals(TechRating.C, reconCamera.getTechRating(), "TO:AUE lists the camera as C/B-B-B");
        assertEquals(SimpleTechLevel.ADVANCED, reconCamera.getStaticTechLevel());
    }

    @Test
    void testAGroundUnitMaySpotAHostileUnitInTagRange() {
        BipedMek target = enemyAt(7);

        assertNull(ReconCameraRules.spotRefusal(game, camera, target));
        assertTrue(ReconCameraRules.hasAnyTarget(game, camera));
        assertTrue(camera.isEligibleForOffboard(), "a camera unit with a target gets an Off-Board turn");
    }

    @Test
    void testTheOffBoardPhaseIsPlayedForACameraWithNoLrmsInTheGame() {
        BipedMek target = enemyAt(7);
        game.setTurnVector(List.of(new GameTurn(cameraOwner.getId())));
        game.setTurnIndex(0, Player.PLAYER_NONE);

        assertTrue(game.isCurrentPhasePlayable(), "nobody carries LRM or homing ammo, but the camera can spot");

        camera.setReconCameraSpotResult(target.getId());
        assertFalse(game.isCurrentPhasePlayable(), "with the camera used and no LRM ammo there is nothing to do");
    }

    @Test
    void testAUnitWithoutACameraCannotSpot() {
        BipedMek noCamera = placeMek("Plain", cameraOwner, new Coords(6, 5));
        BipedMek target = enemyAt(3);

        assertNotNull(ReconCameraRules.spotRefusal(game, noCamera, target));
        assertFalse(noCamera.isEligibleForOffboard());
    }

    @Test
    void testAFriendlyUnitCannotBeSpotted() {
        BipedMek friend = placeMek("Friend", cameraOwner, new Coords(5, 8));

        assertNotNull(ReconCameraRules.spotRefusal(game, camera, friend));
    }

    @Test
    void testATargetBeyondTagLongRangeCannotBeSpotted() {
        BipedMek target = enemyAt(16);

        assertNotNull(ReconCameraRules.spotRefusal(game, camera, target), "TAG long range ends at 15 hexes");
        assertFalse(camera.isEligibleForOffboard(), "no Off-Board turn when there is nothing to spot");
    }

    @Test
    void testTheCameraSpotsOnlyOncePerTurn() {
        BipedMek target = enemyAt(3);
        camera.setReconCameraSpotResult(Entity.NONE);

        assertNotNull(ReconCameraRules.spotRefusal(game, camera, target), "a miss uses up the turn's attempt too");
    }

    @Test
    void testTheRollIsATagShotAtShortRange() {
        BipedMek target = enemyAt(3);

        ToHitData toHit = ReconCameraRules.spotToHit(game, camera, target);

        assertEquals(4, toHit.getValue(), "gunnery 4, short range +0, nobody moved");
    }

    @Test
    void testMediumTagRangeAddsTwo() {
        BipedMek target = enemyAt(7);

        assertEquals(6, ReconCameraRules.spotToHit(game, camera, target).getValue());
    }

    @Test
    void testLongTagRangeAddsFour() {
        BipedMek target = enemyAt(12);

        assertEquals(8, ReconCameraRules.spotToHit(game, camera, target).getValue());
    }

    @Test
    void testAProtoMekUsesLightTagRanges() throws LocationFullException {
        ProtoMek protoMek = new ProtoMek();
        protoMek.setGame(game);
        protoMek.setId(game.getNextEntityId());
        protoMek.setChassis("Proto");
        protoMek.setModel("P-1");
        Crew crew = new Crew(CrewType.SINGLE);
        crew.setGunnery(4, 0);
        protoMek.setCrew(crew);
        protoMek.setOwner(cameraOwner);
        protoMek.setPosition(new Coords(12, 5));
        protoMek.setDeployed(true);
        protoMek.addEquipment(EquipmentType.get("ISReconCamera"), ProtoMek.LOC_TORSO);
        game.addEntity(protoMek);
        BipedMek target = placeMek("Enemy", enemy, new Coords(12, 12));

        assertEquals(8, ReconCameraRules.spotToHit(game, protoMek, target).getValue(),
              "7 hexes is long range for Light TAG (3/6/9)");
        BipedMek farTarget = placeMek("Far enemy", enemy, new Coords(12, 15));
        assertNotNull(ReconCameraRules.spotRefusal(game, protoMek, farTarget), "10 hexes is beyond Light TAG");
    }

    @Test
    void testACameraHitMakesTheUnitTheSpotter() {
        BipedMek target = enemyAt(7);
        BipedMek lrmCarrier = placeMek("LRM carrier", cameraOwner, new Coords(2, 2));

        camera.setReconCameraSpotResult(target.getId());

        assertTrue(ReconCameraRules.isCameraSpotting(camera, target));
        assertSame(camera, Compute.findSpotter(game, lrmCarrier, target));
    }

    @Test
    void testTheIndirectShotSkipsTheSpotterPenaltyWhenTheCameraSpotted() throws LocationFullException {
        game.getOptions().getOption(OptionsConstants.BASE_INDIRECT_FIRE).setValue(true);
        // the carrier can see the target on this open board, so allow indirect fire anyway
        game.getOptions().getOption(OptionsConstants.ADVANCED_COMBAT_INDIRECT_ALWAYS_POSSIBLE).setValue(true);
        Weapon lrmType = (Weapon) EquipmentType.get("ISLRM10");
        lrmType.adaptToGameOptions(game.getOptions());
        BipedMek target = enemyAt(7);
        BipedMek lrmCarrier = placeMek("LRM carrier", cameraOwner, new Coords(5, 2));
        // facing south, towards the target
        lrmCarrier.setFacing(3);
        lrmCarrier.setSecondaryFacing(3);
        WeaponMounted lrm = (WeaponMounted) lrmCarrier.addEquipment(lrmType, Mek.LOC_LEFT_TORSO);
        lrmCarrier.addEquipment(EquipmentType.get("IS Ammo LRM-10"), Mek.LOC_LEFT_TORSO);
        lrmCarrier.loadAllWeapons();
        lrm.setMode("Indirect");
        game.setPhase(GamePhase.FIRING);
        // the spotter fires its own laser this turn, which normally costs the indirect shot +1
        Mounted<?> laser = camera.addEquipment(EquipmentType.get("ISMediumLaser"), Mek.LOC_RIGHT_ARM);
        game.addAction(new WeaponAttackAction(camera.getId(), target.getId(), camera.getEquipmentNum(laser)));
        int lrmId = lrmCarrier.getEquipmentNum(lrm);

        camera.setSpotting(true);
        camera.setSpotTargetId(target.getId());
        ToHitData ordinarySpot = WeaponAttackAction.toHit(game, lrmCarrier.getId(), target, lrmId, false);
        camera.setSpotting(false);
        camera.setSpotTargetId(Entity.NONE);
        camera.setReconCameraSpotResult(target.getId());
        ToHitData cameraSpot = WeaponAttackAction.toHit(game, lrmCarrier.getId(), target, lrmId, false);

        assertEquals(ordinarySpot.getValue() - 1, cameraSpot.getValue(),
              "ordinary spot: " + ordinarySpot.getDesc() + " / camera spot: " + cameraSpot.getDesc());
    }

    @Test
    void testSemiGuidedLrmsGetNoTagBenefitFromACameraSpot() throws LocationFullException {
        game.getOptions().getOption(OptionsConstants.BASE_INDIRECT_FIRE).setValue(true);
        game.getOptions().getOption(OptionsConstants.ADVANCED_COMBAT_INDIRECT_ALWAYS_POSSIBLE).setValue(true);
        Weapon lrmType = (Weapon) EquipmentType.get("ISLRM10");
        lrmType.adaptToGameOptions(game.getOptions());
        BipedMek target = enemyAt(7);
        BipedMek standardCarrier = lrmCarrier(new Coords(4, 2), (AmmoType) EquipmentType.get("IS Ammo LRM-10"));
        BipedMek semiGuidedCarrier = lrmCarrier(new Coords(6, 2), semiGuidedLrm10Ammo());
        game.setPhase(GamePhase.FIRING);
        camera.setReconCameraSpotResult(target.getId());

        int standardToHit = indirectToHit(standardCarrier, target).getValue();
        ToHitData semiGuidedToHit = indirectToHit(semiGuidedCarrier, target);

        assertEquals(standardToHit, semiGuidedToHit.getValue(),
              "a camera spot treats semi-guided missiles as standard LRMs: " + semiGuidedToHit.getDesc());

        // the control: a real TAG designation does change the semi-guided shot, so the comparison above can see one
        game.addTagInfo(new TagInfo(camera.getId(), Targetable.TYPE_ENTITY, target, false));
        target.setTaggedBy(camera.getId());
        assertNotEquals(semiGuidedToHit.getValue(), indirectToHit(semiGuidedCarrier, target).getValue());
    }

    private BipedMek lrmCarrier(Coords position, AmmoType ammo) throws LocationFullException {
        BipedMek carrier = placeMek("LRM carrier", cameraOwner, position);
        carrier.setFacing(3);
        carrier.setSecondaryFacing(3);
        WeaponMounted lrm = (WeaponMounted) carrier.addEquipment(EquipmentType.get("ISLRM10"), Mek.LOC_LEFT_TORSO);
        carrier.addEquipment(ammo, Mek.LOC_LEFT_TORSO);
        carrier.loadAllWeapons();
        lrm.setMode("Indirect");
        return carrier;
    }

    private ToHitData indirectToHit(BipedMek carrier, Targetable target) {
        WeaponMounted lrm = carrier.getWeaponList().getFirst();
        return WeaponAttackAction.toHit(game, carrier.getId(), target, carrier.getEquipmentNum(lrm), false);
    }

    private static AmmoType semiGuidedLrm10Ammo() {
        for (EquipmentType equipment : EquipmentType.allTypes()) {
            boolean isLrm10 = (equipment instanceof AmmoType ammo)
                  && (ammo.getAmmoType() == AmmoType.AmmoTypeEnum.LRM) && (ammo.getRackSize() == 10);
            if (isLrm10 && ((AmmoType) equipment).getMunitionType().contains(AmmoType.Munitions.M_SEMIGUIDED)
                  && !equipment.isClan()) {
                return (AmmoType) equipment;
            }
        }
        throw new IllegalStateException("no Inner Sphere semi-guided LRM-10 ammunition is defined");
    }

    @Test
    void testACameraMissSpotsNothing() {
        BipedMek target = enemyAt(7);
        BipedMek lrmCarrier = placeMek("LRM carrier", cameraOwner, new Coords(2, 2));

        camera.setReconCameraSpotResult(Entity.NONE);

        assertFalse(ReconCameraRules.isCameraSpotting(camera, target));
        assertNull(Compute.findSpotter(game, lrmCarrier, target));
    }

    @Test
    void testAUnitFromAnOlderSaveSpotsNothing() throws ReflectiveOperationException {
        BipedMek target = enemyAt(7);
        // an older save has no camera fields, so loading leaves the id at a default rather than NONE
        Field spotTargetField = Entity.class.getDeclaredField("reconCameraSpotTargetId");
        spotTargetField.setAccessible(true);
        spotTargetField.setInt(camera, target.getId());

        assertFalse(ReconCameraRules.isCameraSpotting(camera, target), "no camera spot was made this turn");
        assertEquals(Entity.NONE, camera.getReconCameraSpotTargetId());
    }

    @Test
    void testTheCameraSpotEndsWithTheTurn() {
        BipedMek target = enemyAt(7);
        camera.setReconCameraSpotResult(target.getId());

        camera.newRound(2);

        assertFalse(ReconCameraRules.isCameraSpotting(camera, target));
        assertFalse(camera.hasReconCameraSpotThisTurn(), "the camera may spot again next turn");
    }

    @Test
    void testTheCameraSideSeesTheSpottedUnit() {
        Player teammate = new Player(2, "Teammate");
        teammate.setTeam(1);
        game.addPlayer(2, teammate);
        BipedMek target = enemyAt(7);

        assertTrue(ReconCameraRules.playersSeeingThroughCameras(game, target).isEmpty());

        camera.setReconCameraSpotResult(target.getId());

        assertTrue(ReconCameraRules.playersSeeingThroughCameras(game, target).contains(cameraOwner));
        assertTrue(ReconCameraRules.playersSeeingThroughCameras(game, target).contains(teammate),
              "the camera's whole side sees what it spotted");
        assertFalse(ReconCameraRules.playersSeeingThroughCameras(game, target).contains(enemy));
    }

    @Test
    void testOnlyTheCameraSideIsShownTheSpot() {
        Player teammate = new Player(2, "Teammate");
        teammate.setTeam(1);
        game.addPlayer(2, teammate);

        assertTrue(ReconCameraRules.isOnCameraSide(camera, cameraOwner));
        assertTrue(ReconCameraRules.isOnCameraSide(camera, teammate));
        assertFalse(ReconCameraRules.isOnCameraSide(camera, enemy), "the enemy is not told it has been spotted");
        assertFalse(ReconCameraRules.isOnCameraSide(camera, null), "no local player, as on a dedicated server");
    }

    @Test
    void testTheSpotOrderAndTheSpotResultSurviveSerialization() throws IOException, ClassNotFoundException {
        BipedMek target = enemyAt(7);
        camera.setReconCameraSpotResult(target.getId());
        ReconCameraSpotAction order = new ReconCameraSpotAction(camera.getId(), target.getId());

        ReconCameraSpotAction orderCopy = roundTrip(order);
        BipedMek cameraCopy = roundTrip(camera);

        assertEquals(target.getId(), orderCopy.getTargetId());
        assertEquals(camera.getId(), orderCopy.getEntityId());
        assertEquals(target.getId(), cameraCopy.getReconCameraSpotTargetId());
        assertTrue(cameraCopy.hasReconCameraSpotThisTurn());
    }

    @SuppressWarnings("unchecked")
    private static <T> T roundTrip(T original) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(original);
        }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (T) input.readObject();
        }
    }
}
