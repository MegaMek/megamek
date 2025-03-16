/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
 */
package megamek.ai.optimizer;

import megamek.client.bot.princess.CardinalEdge;
import megamek.common.Board;

import java.util.Arrays;

public enum CostFunctionChooser {
    Princess,
    Utility,
    ExtendedUtility;

    public static CostFunctionChooser fromString(String str) {
        return switch (str) {
            case "princess" -> Princess;
            case "utility" -> Utility;
            case "extendedUtility" -> ExtendedUtility;
            default -> throw new IllegalArgumentException("Invalid cost function: " + str);
        };
    }

    public static String validCostFunctions() {
        return String.join(", ", Arrays.stream(values()).map(Enum::name).toArray(String[]::new));
    }

    public CostFunction createCostFunction(CardinalEdge edge, Board board) {
        return switch (this) {
            case Princess -> new BasicPathRankerCostFunction(edge, board);
            case Utility -> new UtilityPathRankerCostFunction(edge, new UtilityPathRankerCostFunction.CostFunctionSwarmContext(), board);
            case ExtendedUtility -> new ExtendedCostFunction(new UtilityPathRankerCostFunction(edge, new UtilityPathRankerCostFunction.CostFunctionSwarmContext(), board));
        };
    }
}
