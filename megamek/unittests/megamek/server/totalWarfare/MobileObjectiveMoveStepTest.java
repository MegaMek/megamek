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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.enums.MoveStepType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.ObjectiveMarker;
import megamek.common.equipment.ObjectiveScoringScheme;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.moves.MoveStep;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.rules.RulesManager;
import megamek.common.units.BipedMek;
import megamek.common.units.Mek;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Picking up and setting down a Mobile objective through the server's own move-step handling, the path a client's
 * movement takes. The server's pickup gate is tested here rather than through the client's button logic, because a
 * modified client could send a pickup step the button would never offer.
 */
class MobileObjectiveMoveStepTest {

    private static final Coords MEK_HEX = new Coords(2, 2);

    private RulesManager previousRules;
    private TWGameManager gameManager;
    private Game game;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        previousRules = Game.rulesManager;
        gameManager = spy(new TWGameManager());
        doNothing().when(gameManager).send(any(Packet.class));
        doNothing().when(gameManager).sendServerChat(anyString());
        game = gameManager.getGame();
        game.initializeRulesManager(OptionsConstants.RULES_CORE);
        game.addPlayer(0, new Player(0, "Alice"));
        game.setPhase(GamePhase.MOVEMENT);
        game.setBoard(new Board(5, 5, Stream.generate(Hex::new).limit(25).toArray(Hex[]::new)));
    }

    @AfterEach
    void restoreRules() {
        Game.rulesManager = previousRules;
    }

    @Test
    void testAMekPicksUpAMobileObjectiveInItsHex() {
        ObjectiveMarker flag = flagOnTheGround(true);
        BipedMek mek = mekWithNothingInHand(1);

        pickUp(mek);

        assertEquals(List.of(flag), mek.getDistinctCarriedObjects());
        assertTrue(game.getGroundObjects(MEK_HEX).isEmpty());
    }

    @Test
    void testAnObjectiveThatIsNotMobileStaysWhereItIs() {
        ObjectiveMarker point = flagOnTheGround(false);
        BipedMek mek = mekWithNothingInHand(1);

        pickUp(mek);

        assertTrue(mek.getDistinctCarriedObjects().isEmpty());
        assertEquals(List.of(point), game.getGroundObjects(MEK_HEX));
    }

    @Test
    void testTheServerRefusesASecondObjectiveEvenWithAHandFree() {
        ObjectiveMarker heldFlag = new ObjectiveMarker();
        heldFlag.setName("Held flag");
        heldFlag.setScoringScheme(ObjectiveScoringScheme.raid());
        heldFlag.setMobile(true);
        BipedMek mek = mekWithNothingInHand(1);
        mek.pickupCarryableObject(heldFlag, Mek.LOC_LEFT_ARM);
        ObjectiveMarker secondFlag = flagOnTheGround(true);

        pickUp(mek);

        assertEquals(List.of(heldFlag), mek.getDistinctCarriedObjects(), "one objective per Mek");
        assertEquals(List.of(secondFlag), game.getGroundObjects(MEK_HEX));
    }

    @Test
    void testSettingAnObjectiveDownOnAnotherPushesTheOtherOneHex() {
        ObjectiveMarker lying = flagOnTheGround(true);
        ObjectiveMarker carried = new ObjectiveMarker();
        carried.setName("Carried flag");
        carried.setScoringScheme(ObjectiveScoringScheme.raid());
        carried.setMobile(true);
        BipedMek mek = mekWithNothingInHand(1);
        mek.setFacing(3);
        mek.pickupCarryableObject(carried, Mek.LOC_RIGHT_ARM);

        MovePath path = new MovePath(game, mek);
        path.addStep(MoveStepType.DROP_CARGO, Map.of(MoveStep.CARGO_LOCATION_KEY, Mek.LOC_RIGHT_ARM));
        new MovePathHandler(gameManager, mek, path, null).processMovement();

        assertEquals(List.of(carried), game.getGroundObjects(MEK_HEX));
        assertTrue(game.getGroundObjects(MEK_HEX.translated(3)).contains(lying), "pushed the way the Mek faces");
    }

    private ObjectiveMarker flagOnTheGround(boolean isMobile) {
        ObjectiveMarker flag = new ObjectiveMarker();
        flag.setName("Flag");
        flag.setScoringScheme(ObjectiveScoringScheme.raid());
        flag.setMobile(isMobile);
        game.placeGroundObject(MEK_HEX, flag);
        return flag;
    }

    private BipedMek mekWithNothingInHand(int id) {
        BipedMek mek = new BipedMek();
        mek.setId(id);
        mek.setOwner(game.getPlayer(0));
        mek.setChassis("Carrier");
        mek.setModel("C-" + id);
        mek.setWeight(50);
        mek.setOriginalWalkMP(5);
        for (int location = 0; location < mek.locations(); location++) {
            mek.initializeInternal(10, location);
            mek.initializeArmor(20, location);
            if (mek.hasRearArmor(location)) {
                mek.initializeRearArmor(10, location);
            }
        }
        game.addEntity(mek);
        mek.setPosition(MEK_HEX);
        mek.setDeployed(true);
        return mek;
    }

    private void pickUp(BipedMek mek) {
        MovePath path = new MovePath(game, mek);
        path.addStep(MoveStepType.PICKUP_CARGO,
              Map.of(MoveStep.CARGO_PICKUP_KEY, 0, MoveStep.CARGO_LOCATION_KEY, Mek.LOC_RIGHT_ARM));
        new MovePathHandler(gameManager, mek, path, null).processMovement();
    }
}
