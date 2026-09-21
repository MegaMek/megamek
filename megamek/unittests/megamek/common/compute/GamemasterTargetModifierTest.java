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
package megamek.common.compute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.Player;
import megamek.common.ToHitData;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementType;
import megamek.testUtilities.MMTestUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The gamemaster's target movement modifier change is added to what the unit earned by moving and held between
 * no modifier and the top of the movement table, so attackers never see a negative modifier or one beyond what
 * movement could earn; and it lasts the round it was set in only.
 */
class GamemasterTargetModifierTest {

    private Game game;
    private Entity mek;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        Player owner = new Player(0, "Owner");
        game.addPlayer(0, owner);
        mek = MMTestUtilities.getEntityForUnitTesting("Atlas AS7-D", false);
        assertNotNull(mek, "Test unit could not be loaded");
        mek.setGame(game);
        mek.setOwner(owner);
        game.addEntity(mek);
    }

    /** Puts the unit at the end of a walk of the given length this round. */
    private void walked(int hexes) {
        mek.delta_distance = hexes;
        mek.moved = (hexes > 0) ? EntityMovementType.MOVE_WALK : EntityMovementType.MOVE_NONE;
    }

    private ToHitData targetModifier() {
        return Compute.getTargetMovementModifier(game, mek.getId());
    }

    @Test
    void withoutAChangeTheEarnedModifierStands() {
        walked(5);
        assertEquals(2, targetModifier().getValue(), "5 hexes walked is +2 by the movement table");
    }

    @Test
    void aChangeIsAddedToTheEarnedModifier() {
        walked(5);
        mek.setGamemasterTargetModifier(2);

        ToHitData toHit = targetModifier();

        assertEquals(4, toHit.getValue());
        assertTrue(toHit.getDesc().contains("gamemaster"), "The change is its own line in the breakdown");
    }

    @Test
    void aChangeIsHeldToTheTableCeiling() {
        walked(5);
        mek.setGamemasterTargetModifier(10);

        assertEquals(Compute.maxTargetMovementModifier(game), targetModifier().getValue(),
              "+2 earned plus +10 asked lands on the ceiling, +6 for the table plus +1 for jumping");
        assertEquals(7, Compute.maxTargetMovementModifier(game));
    }

    @Test
    void theCeilingFollowsTheMaxTechMovementTable() {
        game.getOptions().getOption(OptionsConstants.ADVANCED_MAX_TECH_MOVEMENT_MODS).setValue(true);
        walked(5);
        mek.setGamemasterTargetModifier(10);

        assertEquals(8, targetModifier().getValue(), "The MaxTech table tops out one higher");
    }

    @Test
    void aReductionCannotTakeTheModifierBelowZero() {
        walked(5);
        mek.setGamemasterTargetModifier(-5);

        assertEquals(0, targetModifier().getValue(), "+2 earned less 5 asked stops at no modifier");
    }

    @Test
    void aReductionOnAnUnmovedUnitChangesNothing() {
        walked(0);
        mek.setGamemasterTargetModifier(-3);

        ToHitData toHit = targetModifier();

        assertEquals(0, toHit.getValue());
        assertFalse(toHit.getDesc().contains("gamemaster"), "A change that makes no difference adds no line");
    }

    @Test
    void theChangeLastsTheRoundOnly() {
        mek.setGamemasterTargetModifier(3);

        mek.newRound(2);

        assertEquals(0, mek.getGamemasterTargetModifier(), "The next round starts without it");
    }
}
