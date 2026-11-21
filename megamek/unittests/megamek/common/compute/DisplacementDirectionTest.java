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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import megamek.common.GameBoardTestCase;
import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.rules.RulesManager;
import megamek.common.rules.core.CoreRulesManager;
import megamek.common.rules.totalwarfare.TWRulesManager;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Mek;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Issue #9626: a unit pushed by a death from above, a skid or a crash goes in the direction of the push under every
 * rule set. Only an accidental fall from above lets the rules in play pick the hex.
 */
class DisplacementDirectionTest extends GameBoardTestCase {

    private static final int TARGET_ID = 100;
    /** Hex 0303, the middle of the board, with all six neighbours on the board. */
    private static final Coords CENTRE = new Coords(2, 2);

    static {
        initializeBoard("BOARD_FLAT_5X5", """
              size 5 5
              hex 0101 0 "" ""
              hex 0201 0 "" ""
              hex 0301 0 "" ""
              hex 0401 0 "" ""
              hex 0501 0 "" ""
              hex 0102 0 "" ""
              hex 0202 0 "" ""
              hex 0302 0 "" ""
              hex 0402 0 "" ""
              hex 0502 0 "" ""
              hex 0103 0 "" ""
              hex 0203 0 "" ""
              hex 0303 0 "" ""
              hex 0403 0 "" ""
              hex 0503 0 "" ""
              hex 0104 0 "" ""
              hex 0204 0 "" ""
              hex 0304 0 "" ""
              hex 0404 0 "" ""
              hex 0504 0 "" ""
              hex 0105 0 "" ""
              hex 0205 0 "" ""
              hex 0305 0 "" ""
              hex 0405 0 "" ""
              hex 0505 0 "" ""
              end""");
    }

    private RulesManager originalRulesManager;

    @BeforeEach
    void rememberRulesManager() {
        originalRulesManager = Game.rulesManager;
    }

    @AfterEach
    void restoreRulesManager() {
        Game.rulesManager = originalRulesManager;
    }

    private void placeTargetInCentre() {
        setBoard("BOARD_FLAT_5X5");
        Mek target = new BipedMek();
        target.setChassis("Test");
        target.setModel("Target");
        target.setWeight(100.0);
        target.setOriginalWalkMP(3);
        target.autoSetInternal();
        target.setCrew(new Crew(CrewType.SINGLE));
        target.setId(TARGET_ID);
        target.setPosition(CENTRE);
        target.setElevation(0);
        getGame().addEntity(target);
    }

    private void assertDisplacementFollowsDirection() {
        for (int direction = 0; direction < 6; direction++) {
            // Repeat, so a random pick cannot pass by luck
            for (int attempt = 0; attempt < 20; attempt++) {
                assertEquals(CENTRE.translated(direction),
                      Compute.getValidDisplacement(getGame(), TARGET_ID, CENTRE, direction),
                      "A unit pushed in direction " + direction + " should land in the hex in that direction");
            }
        }
    }

    @Test
    void coreRulesDisplacementFollowsThePushDirection() {
        Game.rulesManager = new CoreRulesManager();
        placeTargetInCentre();

        assertDisplacementFollowsDirection();
    }

    @Test
    void totalWarfareDisplacementFollowsThePushDirection() {
        Game.rulesManager = new TWRulesManager();
        placeTargetInCentre();

        assertDisplacementFollowsDirection();
    }

    @Test
    void coreRulesAccidentalFallDisplacesIntoAnAdjacentHex() {
        Game.rulesManager = new CoreRulesManager();
        placeTargetInCentre();

        Coords destination = Compute.getAccidentalFallDisplacement(getGame(), TARGET_ID, CENTRE, 0, false);

        assertNotNull(destination, "Every neighbouring hex is open, so a displacement hex should be found");
        assertEquals(1, CENTRE.distance(destination), "The unit should be displaced into an adjacent hex");
    }

    @Test
    void coreRulesMissedFallLandsNextToTheHexItFellFrom() {
        Game.rulesManager = new CoreRulesManager();
        placeTargetInCentre();
        // Falling in direction 0 means the faller came from the hex in direction 3
        Coords fellFrom = CENTRE.translated(3);

        Coords destination = Compute.getAccidentalFallDisplacement(getGame(), TARGET_ID, CENTRE, 0, true);

        assertNotNull(destination);
        assertEquals(1, CENTRE.distance(destination), "A missed faller lands in a hex adjacent to its target");
        assertEquals(1, fellFrom.distance(destination),
              "Core p.116: it lands as close as possible to the hex from which it fell");
    }

    @Test
    void totalWarfareAccidentalFallFollowsTheFallDirection() {
        Game.rulesManager = new TWRulesManager();
        placeTargetInCentre();

        assertEquals(CENTRE.translated(3), Compute.getAccidentalFallDisplacement(getGame(), TARGET_ID, CENTRE, 3,
              false));
    }
}
