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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.PhysicalResult;
import megamek.common.Player;
import megamek.common.actions.DfaAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.rolls.PilotingRollData;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;
import megamek.testUtilities.MMTestUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for a death from above whose target is gone by the time the physical phase resolves it.
 *
 * <p>A DFA is declared in the movement phase but resolved in the physical phase, so anything that removes the target
 * in between - artillery landing on the hex, an ammo explosion - leaves the attack with nothing to hit. The attacker
 * still has to come down. Before this fix the server returned as soon as the target failed to resolve, so the Mek was
 * left at DFA elevation with a stale displacement attack and never landed.</p>
 */
class TWGameManagerDfaMissingTargetTest {

    private static final int ATTACKER_ID = 5;
    private static final int TARGET_ID = 6;
    private static final int MISSING_TARGET_ID = 99;

    private static final Coords ATTACKER_POSITION = new Coords(1, 1);
    private static final Coords TARGET_POSITION = new Coords(1, 2);

    private TWGameManager gameManager;
    private Game game;
    private Entity attacker;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        game = new Game();
        game.setBoard(flatBoard(4, 4));

        Player player = new Player(0, "Attacker");
        player.setTeam(1);
        game.addPlayer(0, player);

        attacker = MMTestUtilities.getEntityForUnitTesting("Shadow Hawk SHD-5D", false);
        assertNotNull(attacker, "Attacking unit could not be loaded");
        attacker.setId(ATTACKER_ID);
        attacker.setOwner(player);
        game.addEntity(attacker);
        attacker.setPosition(ATTACKER_POSITION);
        attacker.setFacing(3);
        // The movement phase leaves a DFA attacker one level above the target hex; landing it again is what the
        // physical phase is responsible for.
        attacker.setElevation(1);

        game.setPhase(GamePhase.PHYSICAL);

        gameManager = mock(TWGameManager.class);
        doCallRealMethod().when(gameManager).setGame(any());
        doCallRealMethod().when(gameManager).resolvePhysicalAttacks();
        gameManager.setGame(game);
        givePhysicalResultList(gameManager);
    }

    /**
     * A mocked manager skips field initialization, so hand it the physical result list its real constructor would
     * have built.
     */
    private static void givePhysicalResultList(TWGameManager manager) throws ReflectiveOperationException {
        Field physicalResults = TWGameManager.class.getDeclaredField("physicalResults");
        physicalResults.setAccessible(true);
        physicalResults.set(manager, new Vector<PhysicalResult>());
    }

    /** A board of the given size with plain level 0 hexes. */
    private static Board flatBoard(int width, int height) {
        Hex[] hexes = new Hex[width * height];
        for (int hex = 0; hex < hexes.length; hex++) {
            hexes[hex] = new Hex();
        }
        return new Board(width, height, hexes);
    }

    /** Declares a death from above by the attacker against the given target id, as the movement phase would. */
    private void declareDeathFromAbove(int targetId) {
        DfaAttackAction deathFromAbove = new DfaAttackAction(ATTACKER_ID,
              Targetable.TYPE_ENTITY,
              targetId,
              TARGET_POSITION);
        attacker.setDisplacementAttack(deathFromAbove);
        game.addDisplacementAttack(deathFromAbove);
    }

    /** Asserts that the attacker was brought down in the target hex and is no longer flagged as making a DFA. */
    private void assertAttackerLanded() {
        verify(gameManager).doEntityDisplacement(eq(attacker),
              eq(ATTACKER_POSITION),
              eq(TARGET_POSITION),
              any(PilotingRollData.class));
        assertFalse(attacker.isMakingDfa(), "Attacker is still flagged as making a death from above");
    }

    @Test
    void attackerLandsWhenTargetIsNoLongerInTheGame() {
        declareDeathFromAbove(MISSING_TARGET_ID);

        gameManager.resolvePhysicalAttacks();

        assertAttackerLanded();
    }

    @Test
    void attackerLandsWhenTargetWasDestroyedButIsStillOnTheBoard() {
        Entity target = MMTestUtilities.getEntityForUnitTesting("Locust LCT-1V", false);
        assertNotNull(target, "Target unit could not be loaded");
        Player enemy = new Player(1, "Defender");
        enemy.setTeam(2);
        game.addPlayer(1, enemy);
        target.setId(TARGET_ID);
        target.setOwner(enemy);
        game.addEntity(target);
        target.setPosition(TARGET_POSITION);
        target.setDestroyed(true);

        declareDeathFromAbove(TARGET_ID);

        gameManager.resolvePhysicalAttacks();

        assertAttackerLanded();
    }
}
