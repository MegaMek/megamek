/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.server.sbf;

import megamek.common.actions.EntityAction;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFToHitData;
import org.apache.logging.log4j.LogManager;

import java.util.List;

import static org.apache.logging.log4j.LogManager.getLogger;

record SBFAttackProcessor(SBFGameManager gameManager) implements SBFGameManagerHelper {

    void processAttacks(List<EntityAction> actions, SBFFormation formation) {
        if (!validatePermitted(actions, formation)) {
            return;
        }

        actions.forEach(game()::addAction);
        formation.setDone(true);
        gameManager.sendUnitUpdate(formation);
        gameManager.sendPendingActions();
        gameManager.endCurrentTurn(formation);
    }

    private boolean validatePermitted(List<EntityAction> actions, SBFFormation formation) {
        if (!game().getPhase().isFiring()) {
            LogManager.getLogger().error("Server got attacks packet in wrong phase!");
            return false;
        } else if (formation.isDone()) {
            LogManager.getLogger().error("Formation already done!");
            return false;
        } else if (SBFToHitData.targetsOfFormation(formation, game()).size() > 2) {
            getLogger().error("Formation targeting too many targets!");
            return false;
        }

        return true;
    }
}