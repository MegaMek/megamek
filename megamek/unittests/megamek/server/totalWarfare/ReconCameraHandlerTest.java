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
package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import java.util.List;
import java.util.Vector;

import megamek.common.Player;
import megamek.common.actions.EntityAction;
import megamek.common.actions.ReconCameraSpotAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.exceptions.LocationFullException;
import megamek.common.game.Game;
import megamek.common.game.GameTurn;
import megamek.common.net.enums.PacketCommand;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.Roll;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * The server half of the ground-unit Recon Camera (TO:AUE p.150): the spot rolled at the end of the Off-Board phase,
 * the order's packet path and its owner check, and the camera's side seeing the spotted unit under double-blind.
 */
class ReconCameraHandlerTest {

    private static final String BOARD_DATA = """
          size 20 20
          end""";
    private static final int OWNER_CONNECTION = 0;
    private static final int ENEMY_CONNECTION = 1;
    private static final int TEAMMATE_CONNECTION = 2;

    private TWGameManager gameManager;
    private Game game;
    private Player owner;
    private Player enemy;
    private BipedMek camera;
    private BipedMek target;

    /** The handler with its dice decided by the test. */
    private static final class HandlerUnderTest extends ReconCameraHandler {
        private final int nextRoll;

        HandlerUnderTest(TWGameManager gameManager, int nextRoll) {
            super(gameManager);
            this.nextRoll = nextRoll;
        }

        @Override
        Roll rollSpot() {
            return new FixedRoll(nextRoll);
        }
    }

    /** Two dice that always show the same total. */
    private static final class FixedRoll extends Roll {
        private final int value;

        FixedRoll(int value) {
            super(2, 1);
            this.value = value;
        }

        @Override
        public int getIntValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }

        @Override
        public String getReport() {
            return String.valueOf(value);
        }

        @Override
        public int[] getIntValues() {
            return new int[] { value };
        }
    }

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() throws LocationFullException {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).send(Mockito.anyInt(), any(Packet.class));
        game = gameManager.getGame();
        owner = new Player(OWNER_CONNECTION, "Owner");
        owner.setTeam(1);
        enemy = new Player(ENEMY_CONNECTION, "Enemy");
        enemy.setTeam(2);
        game.addPlayer(OWNER_CONNECTION, owner);
        game.addPlayer(ENEMY_CONNECTION, enemy);
        Board board = BoardLoader.initializeBoard(BOARD_DATA);
        game.setBoard(board);
        game.setPhase(GamePhase.OFFBOARD);

        camera = placeMek("Camera", owner, new Coords(5, 5));
        camera.addEquipment(EquipmentType.get("ISReconCamera"), Mek.LOC_CENTER_TORSO);
        // 7 hexes: medium TAG range, so the spot needs gunnery 4 + 2 = 6
        target = placeMek("Target", enemy, new Coords(5, 12));
    }

    private BipedMek placeMek(String name, Player unitOwner, Coords position) {
        BipedMek mek = new BipedMek();
        mek.setGame(game);
        mek.setId(game.getNextEntityId());
        mek.setChassis(name);
        mek.setModel("T-1");
        Crew crew = new Crew(CrewType.SINGLE);
        crew.setGunnery(4, 0);
        mek.setCrew(crew);
        mek.setOwner(unitOwner);
        mek.setPosition(position);
        mek.setDeployed(true);
        game.addEntity(mek);
        return mek;
    }

    @Test
    void testAHitSpotsTheTarget() {
        new HandlerUnderTest(gameManager, 6).resolveSpot(camera,
              new ReconCameraSpotAction(camera.getId(), target.getId()));

        assertEquals(target.getId(), camera.getReconCameraSpotTargetId(), "6 meets the 6 needed");
        assertTrue(camera.hasReconCameraSpotThisTurn());
    }

    @Test
    void testAMissSpotsNothingAndUsesUpTheTurn() {
        new HandlerUnderTest(gameManager, 5).resolveSpot(camera,
              new ReconCameraSpotAction(camera.getId(), target.getId()));

        assertEquals(Entity.NONE, camera.getReconCameraSpotTargetId(), "5 is short of the 6 needed");
        assertTrue(camera.hasReconCameraSpotThisTurn(), "a miss uses up the turn's attempt");
    }

    @Test
    void testASpotOutsideTheOffBoardPhaseIsIgnored() {
        game.setPhase(GamePhase.FIRING);

        new HandlerUnderTest(gameManager, 12).resolveSpot(camera,
              new ReconCameraSpotAction(camera.getId(), target.getId()));

        assertEquals(Entity.NONE, camera.getReconCameraSpotTargetId());
        assertFalse(camera.hasReconCameraSpotThisTurn(), "an ignored order does not use up the camera");
    }

    @Test
    void testASpotOnAFriendlyUnitIsRefused() {
        BipedMek friend = placeMek("Friend", owner, new Coords(5, 8));

        new HandlerUnderTest(gameManager, 12).resolveSpot(camera,
              new ReconCameraSpotAction(camera.getId(), friend.getId()));

        assertEquals(Entity.NONE, camera.getReconCameraSpotTargetId());
        assertFalse(camera.hasReconCameraSpotThisTurn(), "a refused order does not use up the camera");
    }

    @Test
    void testASpotOnAUnitThatHasLeftTheGameIsIgnored() {
        int missingUnitId = target.getId() + 100;

        new HandlerUnderTest(gameManager, 12).resolveSpot(camera,
              new ReconCameraSpotAction(camera.getId(), missingUnitId));

        assertEquals(Entity.NONE, camera.getReconCameraSpotTargetId());
        assertFalse(camera.hasReconCameraSpotThisTurn(), "an order for a vanished unit does not use up the camera");
    }

    @Test
    void testTheOwnersOrderIsRolledAtTheEndOfTheOffBoardPhase() {
        // gunnery 0 at short range with nobody moving needs 0: any roll spots
        camera.getCrew().setGunnery(0, 0);
        target.setPosition(new Coords(5, 8));
        sendOrder(OWNER_CONNECTION);

        gameManager.resolveAllButWeaponAttacks();

        assertEquals(target.getId(), camera.getReconCameraSpotTargetId());
    }

    @Test
    void testAnotherPlayersOrderForTheCameraIsRefused() {
        camera.getCrew().setGunnery(0, 0);
        target.setPosition(new Coords(5, 8));
        sendOrder(ENEMY_CONNECTION);

        gameManager.resolveAllButWeaponAttacks();

        assertEquals(Entity.NONE, camera.getReconCameraSpotTargetId(), "only the camera's owner may order a spot");
        assertFalse(camera.hasReconCameraSpotThisTurn());
    }

    @Test
    void testTheCameraSideSeesTheSpottedUnitUnderDoubleBlind() {
        game.getOptions().getOption(OptionsConstants.ADVANCED_DOUBLE_BLIND).setValue(true);
        game.getOptions().getOption(OptionsConstants.ADVANCED_TEAM_VISION).setValue(false);
        Player teammate = new Player(TEAMMATE_CONNECTION, "Teammate");
        teammate.setTeam(1);
        game.addPlayer(TEAMMATE_CONNECTION, teammate);

        assertFalse(gameManager.whoCanSee(target).contains(teammate), "the teammate has no unit that sees it");

        camera.setReconCameraSpotResult(target.getId());

        assertTrue(gameManager.whoCanSee(target).contains(teammate), "the camera's whole side sees what it spotted");
    }

    @Test
    void testTheCameraSideIsSentTheSpottedUnitUnderDoubleBlind() {
        game.getOptions().getOption(OptionsConstants.ADVANCED_DOUBLE_BLIND).setValue(true);
        game.getOptions().getOption(OptionsConstants.ADVANCED_TEAM_VISION).setValue(false);
        Player teammate = new Player(TEAMMATE_CONNECTION, "Teammate");
        teammate.setTeam(1);
        game.addPlayer(TEAMMATE_CONNECTION, teammate);

        assertFalse(unitsSentTo(teammate).contains(target), "the teammate's client does not know about the target");

        new HandlerUnderTest(gameManager, 12).resolveSpot(camera,
              new ReconCameraSpotAction(camera.getId(), target.getId()));

        assertTrue(unitsSentTo(teammate).contains(target), "the unit list sent to the teammate includes the target");
        assertFalse(unitsSentTo(enemy).isEmpty(), "the enemy still gets its own units");
    }

    @SuppressWarnings("unchecked")
    private List<Entity> unitsSentTo(Player viewer) {
        return (List<Entity>) gameManager.createFilteredFullEntitiesPacket(viewer, null).getObject(0);
    }

    private void sendOrder(int connection) {
        game.setTurnVector(List.of(new GameTurn(game.getEntity(camera.getId()).getOwnerId())));
        game.setTurnIndex(0, Player.PLAYER_NONE);
        Vector<EntityAction> actions = new Vector<>();
        actions.add(new ReconCameraSpotAction(camera.getId(), target.getId()));
        Mockito.doNothing().when(gameManager).endCurrentTurn(any());
        gameManager.handlePacket(connection, new Packet(PacketCommand.ENTITY_ATTACK, camera.getId(), actions));
    }
}
