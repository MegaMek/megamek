/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.server.sbf;

import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFMovePath;
import megamek.logging.MMLogger;

record SBFMovementProcessor(SBFGameManager gameManager) implements SBFGameManagerHelper {
    private static final MMLogger logger = MMLogger.create(SBFMovementProcessor.class);

    void processMovement(SBFMovePath movePath, SBFFormation formation) {
        if (!validatePermitted(movePath, formation)) {
            return;
        }

        formation.setPosition(movePath.getLastPosition());
        formation.setJumpUsedThisTurn(movePath.getJumpUsed());
        formation.setDone(true);
        gameManager.sendUnitUpdate(formation);
        gameManager.endCurrentTurn(formation);
    }

    private boolean validatePermitted(SBFMovePath movePath, SBFFormation formation) {
        if (!game().getPhase().isMovement()) {
            logger.error("Server got movement packet in wrong phase!");
            return false;
        } else if (movePath.isIllegal()) {
            logger.error("Illegal move path!");
            return false;
        } else if (formation.isDone()) {
            logger.error("Formation already done!");
            return false;
        }
        return true;
    }

}
