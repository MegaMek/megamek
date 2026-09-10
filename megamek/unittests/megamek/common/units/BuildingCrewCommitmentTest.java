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
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.compute.MarinePointsScoreCalculator;
import megamek.common.enums.BuildingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A building's crew in an infantry action (TO:AR pp. 169 to 170): they count for nothing until committed,
 * committing them costs crew hits by the share committed, and casualties come off the committed crew.
 */
@DisplayName("Building crew committed to an infantry action")
class BuildingCrewCommitmentTest {

    private BuildingEntity building;

    @BeforeEach
    void beforeEach() {
        building = new BuildingEntity(BuildingType.MEDIUM, IBuilding.GUN_EMPLACEMENT);
        building.getCrew().setSize(4);
        building.getCrew().setCurrentSize(4);
    }

    @Test
    @DisplayName("Uncommitted crew are worth nothing and cost nothing")
    void uncommittedCrewCountForNothing() {
        assertEquals(0, building.getCommittedCrew());
        assertEquals(0.0, MarinePointsScoreCalculator.calculateScore(building, building));
        assertEquals(0, building.getCrew().getHits());
    }

    @Test
    @DisplayName("Committing half the crew is worth half a point each and costs three crew hits")
    void committingHalfTheCrew() {
        int committed = building.commitCrew(2);

        assertEquals(2, committed);
        assertEquals(2, building.getCommittedCrew());
        assertEquals(1.0, MarinePointsScoreCalculator.calculateScore(building, building), "two at 0.5");
        assertEquals(3, building.getCrew().getHits(), "50 percent committed, the 36 to 50 row");
        assertEquals(2, building.getCrewAvailableToCommit());
    }

    @Test
    @DisplayName("Committing more than is available commits what there is")
    void commitmentIsCappedAtTheCrewAvailable() {
        assertEquals(4, building.commitCrew(9));
        assertEquals(0, building.getCrewAvailableToCommit());
        assertEquals(6, building.getCrew().getHits(), "every crew member committed, the 81 to 100 row");
    }

    @Test
    @DisplayName("Committing every crew member costs six hits but does not kill the crew")
    void committingEveryoneIsAPenaltyNotADeath() {
        building.commitCrew(4);

        assertEquals(6, building.getCrew().getHits(), "the 81 to 100 row");
        assertFalse(building.getCrew().isDead(), "six hits from the table are a +6 to fire, not a dead crew");
        assertTrue(building.getCrew().isActive(), "the crew still man the building");
    }

    @Test
    @DisplayName("Casualties come off the committed crew and the crew itself")
    void casualtiesComeOffTheCommittedCrew() {
        building.commitCrew(3);

        building.loseCommittedCrew(2);

        assertEquals(1, building.getCommittedCrew());
        assertEquals(2, building.getCrew().getCurrentSize());
        assertEquals(5, building.getCrew().getHits(), "three of four ever committed is 75 percent, five hits");
    }

    @Test
    @DisplayName("When the action ends only the crew actually lost still count against the building")
    void clearingTheCommitmentLeavesTheLossesOnly() {
        building.commitCrew(4);
        building.loseCommittedCrew(1);

        building.clearCommittedCrew();

        assertEquals(0, building.getCommittedCrew());
        assertEquals(3, building.getCrew().getCurrentSize());
        assertEquals(2, building.getCrew().getHits(), "one of four lost is 25 percent, two hits");
        assertTrue(building.getCrewAvailableToCommit() == 3);
    }
}
