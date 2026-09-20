/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;

import java.util.stream.Stream;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.enums.MoveStepType;
import megamek.common.game.Game;
import megamek.common.moves.MovePath;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rolls.TargetRoll;
import megamek.common.rules.RulesManager;
import megamek.common.units.BipedMek;
import megamek.common.units.ProneCause;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Exercise the authoritative order/fall paths rather than inferring causes in the presentation layer. */
class ProneTransitionsTest {
    private RulesManager previousRules;
    private TWGameManager manager;
    private BipedMek mek;

    @BeforeEach
    void setUp() {
        previousRules = Game.rulesManager;
        manager = spy(new TWGameManager());
        doNothing().when(manager).send(any(Packet.class));
        doNothing().when(manager).sendServerChat(anyString());
        var game = manager.getGame();
        game.initializeRulesManager(OptionsConstants.RULES_CORE);
        game.addPlayer(0, new Player(0, "Test"));
        game.setBoard(new Board(3, 3, Stream.generate(Hex::new).limit(9).toArray(Hex[]::new)));
        game.setPhase(GamePhase.MOVEMENT);
        mek = new BipedMek();
        mek.setId(1);
        mek.setOwner(game.getPlayer(0));
        mek.setWeight(50);
        mek.setOriginalWalkMP(5);
        for (int location = 0; location < mek.locations(); location++) {
            mek.initializeInternal(15, location);
            mek.initializeArmor(40, location);
            if (mek.hasRearArmor(location)) {
                mek.initializeRearArmor(40, location);
            }
        }
        game.addEntity(mek);
        mek.setPosition(new Coords(1, 1));
        mek.setElevation(0);
        mek.setDeployed(true);
    }

    @AfterEach
    void restoreRules() {
        Game.rulesManager = previousRules;
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void humanAndBotProneOrdersAreVoluntary(boolean bot) {
        mek.getOwner().setBot(bot);
        var path = new MovePath(manager.getGame(), mek);
        path.addStep(MoveStepType.GO_PRONE);
        assertTrue(path.isMoveLegal());
        new MovePathHandler(manager, mek, path, null).processMovement();
        assertTrue(mek.isProne());
        assertEquals(ProneCause.VOLUNTARY, mek.getProneCause());
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3, 4, 5 })
    void resolvedFallOverridesEarlierVoluntaryPostureAndTravelsInEntityUpdate(int direction) {
        mek.setProne(ProneCause.VOLUNTARY);
        manager.doEntityFall(mek, mek.getPosition(), 0, direction,
              new PilotingRollData(mek.getId(), TargetRoll.AUTOMATIC_SUCCESS, "test fall"), false, false);
        assertTrue(mek.isProne());
        assertEquals(ProneCause.FORCED, mek.getProneCause());
        assertEquals(megamek.common.units.FallSide.fromDirection(direction), mek.getFallSide());
        var update = manager.createEntityPacket(mek.getId(), null);
        assertEquals(ProneCause.FORCED, ((BipedMek) update.getObject(1)).getProneCause());
        assertEquals(mek.getFallSide(), ((BipedMek) update.getObject(1)).getFallSide());
    }
}
