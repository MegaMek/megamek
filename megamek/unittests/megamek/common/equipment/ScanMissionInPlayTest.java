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
package megamek.common.equipment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.board.Coords;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Whether the game offers the Scan order at all. Client and server both ask this one question: the client to show
 * the Scan option and give units a turn in the pre-End declarations phase, the server to accept the order. A
 * candidate that needs confirming has to count, or the scan that confirms it can never be ordered.
 */
class ScanMissionInPlayTest {

    private static final Coords POINT_HEX = new Coords(5, 5);

    private Game game;

    @BeforeEach
    void setUp() {
        game = new Game();
        game.getOptions().getOption(OptionsConstants.VICTORY_USE_OBJECTIVES).setValue(true);
    }

    private ObjectiveMarker placePoint() {
        ObjectiveMarker marker = new ObjectiveMarker();
        marker.setName("Depot");
        game.placeGroundObject(POINT_HEX, marker);
        return marker;
    }

    @Test
    void testAPlainPointGivesNothingToScan() {
        placePoint();

        assertFalse(ScanMission.isInPlay(game));
    }

    @Test
    void testAScanPointIsSomethingToScan() {
        placePoint().setScoringScheme(ObjectiveScoringScheme.scan(ObjectiveScoringScheme.ScanPayout.ON_EXIT));

        assertTrue(ScanMission.isInPlay(game));
    }

    @Test
    void testACandidateWaitingToBeConfirmedIsSomethingToScan() {
        placePoint().setPotential(true);

        assertTrue(ScanMission.isInPlay(game), "a Standard candidate needs a scan to confirm it");
    }

    @Test
    void testAConfirmedCandidateLeavesNothingToScan() {
        ObjectiveMarker candidate = placePoint();
        candidate.setPotential(true);
        candidate.setConfirmed(true);

        assertFalse(ScanMission.isInPlay(game), "once confirmed, scanning it again would find nothing");
    }

    @Test
    void testNothingIsScannedWithObjectivesOff() {
        placePoint().setPotential(true);
        game.getOptions().getOption(OptionsConstants.VICTORY_USE_OBJECTIVES).setValue(false);

        assertFalse(ScanMission.isInPlay(game));
    }
}
