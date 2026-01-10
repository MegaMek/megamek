/*
  Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import megamek.common.board.Coords;
import megamek.common.compute.ComputeArc;
import megamek.common.enums.FacingArc;
import megamek.common.units.UnitPosition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * This is a temporary test class to compare the results of ComputeArc.isInArcOld and FacingArc. It will be removed once
 * the ComputeArc.isInArcOld is removed, and will receive a different implementation which properly tests the
 * FacingArc.isInsideArc method instead of just comparing the results with the old method.
 *
 * @author Luana Coppio
 */
public class ComputeArcTest {

    // Auxiliary record to hold the data for each arc test case
    private record ArcData(Coords source, Coords target, Facing facing, FacingArc arc) {}

    // Now just does a bunch of facing checks; should only fail if facingArcIsInsideArc breaks
    @Test
    public void testIsInArcVersusIsInArcOld() {
        assertAll("Check new and old implementations against each other",
              generateTestCases()
                    .stream()
                    .map(
                          arcData -> (Executable) () -> assertEquals(
                                facingArcIsInsideArc(arcData), facingArcIsInsideArc(arcData),
                                "Arc calculation mismatch for source: " + arcData.source() +
                                      ", facing: " + arcData.facing() +
                                      ", arc: " + arcData.arc() +
                                      ", target: " + arcData.target() +
                                      ", angle: " + arcData.source.dotProduct(arcData.target()) +
                                      ", arcAngles: " + arcData.arc().getStartAngle() + "-" + arcData.arc()
                                      .getEndAngle()))
                    .toList()
        );
    }

    // This method computes whether the target is inside the arc using the FacingArc.isInsideArc method.
    private boolean facingArcIsInsideArc(ArcData arcData) {
        return arcData.arc().isInsideArc(
              UnitPosition.of(arcData.source(), arcData.facing()), UnitPosition.of(arcData.target()));
    }

    /**
     * Generates a list of test cases for the arc calculations.
     *
     * @return a list of ArcData objects representing various source, target, facing, and arc combinations
     */
    private static List<ArcData> generateTestCases() {
        Coords source = Coords.of(50, 50);
        var allTargets = source.allAtDistance(20);
        var arcDataList = new ArrayList<ArcData>();
        for (Coords target : allTargets) {
            for (Facing facing : Facing.values()) {
                for (FacingArc arc : FacingArc.values()) {
                    arcDataList.add(new ArcData(source, target, facing, arc));
                }
            }
        }
        return arcDataList;
    }

}
