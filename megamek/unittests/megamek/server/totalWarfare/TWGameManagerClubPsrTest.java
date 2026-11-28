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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.util.Vector;

import megamek.common.Hex;
import megamek.common.MMRandom;
import megamek.common.PhysicalResult;
import megamek.common.Player;
import megamek.common.ToHitData;
import megamek.common.actions.ClubAttackAction;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.compute.Compute;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.MiscMounted;
import megamek.common.exceptions.LocationFullException;
import megamek.common.game.Game;
import megamek.common.rolls.PilotingRollData;
import megamek.common.rules.RulesManager;
import megamek.common.rules.core.CoreRulesManager;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;
import megamek.testUtilities.MMTestUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for megamek #9760: under Core Rules, a club attack that misses must not make the target roll to
 * stay standing. Only a hit does (Core p.79).
 *
 * <p>The PSR used to be queued while the attack was set up, before the dice were compared, so a miss still forced it.
 * These tests run the physical phase through the real server path with fixed dice.</p>
 */
class TWGameManagerClubPsrTest {

    /** Dice that roll the same face on every d6. */
    private static final class FixedDice extends MMRandom {
        private final int face;

        private FixedDice(int face) {
            this.face = face;
        }

        @Override
        public int randomInt(int maxValue) {
            return face - 1;
        }

        @Override
        public float randomFloat() {
            return 0.5f;
        }
    }

    private static final int ATTACKER_ID = 5;
    private static final int TARGET_ID = 6;
    private static final String CLUBBED_REASON = "was clubbed";

    private static final Coords ATTACKER_POSITION = new Coords(1, 1);
    private static final Coords TARGET_POSITION = new Coords(1, 2);

    private RulesManager previousRulesManager;
    private TWGameManager gameManager;
    private Game game;
    private Entity target;
    private ClubAttackAction clubAttack;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() throws ReflectiveOperationException, LocationFullException {
        previousRulesManager = Game.rulesManager;
        Game.rulesManager = new CoreRulesManager();

        game = new Game();
        game.setBoard(flatBoard(4, 4));

        Player attackingPlayer = new Player(0, "Attacker");
        attackingPlayer.setTeam(1);
        game.addPlayer(0, attackingPlayer);
        Player defendingPlayer = new Player(1, "Defender");
        defendingPlayer.setTeam(2);
        game.addPlayer(1, defendingPlayer);

        Entity attacker = MMTestUtilities.getEntityForUnitTesting("Shadow Hawk SHD-5D", false);
        assertNotNull(attacker, "Attacking unit could not be loaded");
        attacker.setId(ATTACKER_ID);
        attacker.setOwner(attackingPlayer);
        game.addEntity(attacker);
        attacker.setPosition(ATTACKER_POSITION);
        // Legs and torso facing south, toward the target; the club's arc follows the torso
        attacker.setFacing(3);
        attacker.setSecondaryFacing(3);
        // Undeployed units are not active, and the server drops their attacks
        attacker.setDeployed(true);

        target = MMTestUtilities.getEntityForUnitTesting("Locust LCT-1V", false);
        assertNotNull(target, "Target unit could not be loaded");
        target.setId(TARGET_ID);
        target.setOwner(defendingPlayer);
        game.addEntity(target);
        target.setPosition(TARGET_POSITION);
        target.setFacing(0);
        target.setDeployed(true);

        // Entering the physical phase clears pending actions, so declare the attack after it
        game.setPhase(GamePhase.PHYSICAL);

        // A tree picked up from a wooded hex, as the server adds it
        MiscMounted treeClub = (MiscMounted) attacker.addEquipment(EquipmentType.get(EquipmentTypeLookup.TREE_CLUB),
              Entity.LOC_NONE);
        clubAttack = new ClubAttackAction(ATTACKER_ID,
              Targetable.TYPE_ENTITY,
              TARGET_ID,
              treeClub,
              ToHitData.HIT_NORMAL,
              false);
        game.addAction(clubAttack);

        gameManager = mock(TWGameManager.class);
        doCallRealMethod().when(gameManager).setGame(any());
        doCallRealMethod().when(gameManager).resolvePhysicalAttacks();
        gameManager.setGame(game);
        givePhysicalResultList(gameManager);
    }

    @AfterEach
    void restoreGlobals() {
        Compute.setRNG(MMRandom.R_DEFAULT);
        Game.rulesManager = previousRulesManager;
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

    /** Counts the PSRs queued against the target because it was clubbed. */
    private int countClubbedPsrs() {
        int count = 0;
        for (PilotingRollData psr : game.getPSRsForEntity(target)) {
            if (psr.getPlainDesc().contains(CLUBBED_REASON)) {
                count++;
            }
        }
        return count;
    }

    /**
     * The attack must reach the server and need a normal roll, so the dice alone decide hit or miss. Without this a
     * miss test passes when the attack is dropped or impossible.
     */
    private void assertAttackNeedsARoll() {
        assertTrue(game.getEntity(ATTACKER_ID).isActive(), "Attacker must be active or its attack is dropped");
        int targetNumber = clubAttack.toHit(game).getValue();
        assertTrue((targetNumber > 2) && (targetNumber <= 12),
              "Club attack should need a roll between 3 and 12, but needs " + targetNumber);
    }

    @Test
    void missedClubAttackQueuesNoPsr() {
        assertAttackNeedsARoll();
        // Snake eyes misses any club attack that needs a roll
        Compute.setRNG(new FixedDice(1));

        gameManager.resolvePhysicalAttacks();

        assertEquals(0, countClubbedPsrs(), "A missed club attack must not make the target roll");
    }

    @Test
    void clubAttackThatHitsQueuesOnePsr() {
        assertAttackNeedsARoll();
        assertTrue(target.canFall(), "Target must be able to fall for the PSR to apply");
        // Boxcars hits any club attack that needs a roll
        Compute.setRNG(new FixedDice(6));

        gameManager.resolvePhysicalAttacks();

        assertEquals(1, countClubbedPsrs(), "A club attack that hits must make the target roll once");
    }
}
