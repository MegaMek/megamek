/*
 * Copyright (C) 2021 Ben Mazur (bmazur@sev.org)
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

package megamek.common.pathfinder;

import java.util.ArrayList;
import java.util.List;

import megamek.client.bot.princess.AeroPathUtil;
import megamek.common.enums.MoveStepType;
import megamek.common.moves.MovePath;
import megamek.common.options.OptionsConstants;

/**
 * This class handles pathfinding for situations where the unit is prone and wants to remain prone for whatever reason
 * (no leg, getting up will result in exposure to fire, etc.)
 *
 * @author NickAragua
 */
public class PronePathFinder {
    private List<MovePath> pronePaths;

    public List<MovePath> getPronePaths() {
        return pronePaths;
    }

    public void run(MovePath startingEdge) {
        pronePaths = new ArrayList<>();

        // if we're prone, consider staying that way
        if (startingEdge.getFinalProne()) {
            pronePaths.add(startingEdge);
            pronePaths.addAll(AeroPathUtil.generateValidRotations(startingEdge));

            // if we can go hull down, consider doing so - going "hull down" from prone
            // doesn't require a PSR.
            if (startingEdge.getGame()
                  .getOptions()
                  .booleanOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_HULL_DOWN)
                  && startingEdge.getEntity().canGoHullDown()) {
                MovePath hullDown = startingEdge.clone().addStep(MoveStepType.HULL_DOWN);

                if (hullDown.isMoveLegal()) {
                    pronePaths.add(hullDown);
                    pronePaths.addAll(AeroPathUtil.generateValidRotations(hullDown));
                }
            }
        }
    }
}
