/*
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

package megamek.common.autoresolve.acar.action;

import java.util.Optional;

import megamek.common.alphaStrike.ASRange;
import megamek.common.autoresolve.acar.SimulationContext;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.acar.handler.StandardUnitAttackHandler;
import megamek.common.autoresolve.component.Formation;
import megamek.logging.MMLogger;

public class StandardUnitAttack extends AbstractAttackAction {
    private static final MMLogger logger = MMLogger.create(StandardUnitAttack.class);

    private final int unitNumber;
    private final ASRange range;

    public enum ManeuverResult {
        SUCCESS,
        FAILURE,
        DRAW
    }

    /**
     * Creates a standard attack of an SBF Unit on another formation. The unit number identifies the SBF Unit making the
     * attack, i.e. 1 for the first of the formation's units, 2 for the second etc.
     *
     * @param formationId The attacker's ID
     * @param unitNumber  The number of the attacking SBF Unit inside the formation
     * @param targetId    The target's ID
     */
    public StandardUnitAttack(int formationId, int unitNumber, int targetId, ASRange range) {
        super(formationId, targetId);
        this.unitNumber = unitNumber;
        this.range = range;
    }

    /**
     * Returns the index of the SBF Unit inside the formation, i.e. 0 for the first of the formation's units, 1 for the
     * second, 3 for the third etc.
     *
     * @return The unit index number within the formation
     */
    public int getUnitNumber() {
        return unitNumber;
    }

    public ASRange getRange() {
        return range;
    }

    @Override
    public ActionHandler getHandler(SimulationManager gameManager) {
        return new StandardUnitAttackHandler(this, gameManager);
    }

    @Override
    public boolean isDataValid(SimulationContext context) {
        Optional<Formation> possibleAttacker = context.getFormation(getEntityId());
        Optional<Formation> possibleTarget = context.getFormation(getTargetId());
        if (getEntityId() == getTargetId()) {
            logger.warn("Formations cannot attack themselves! {}", this);
            return false;
        } else if (possibleAttacker.isEmpty() || possibleTarget.isEmpty()) {
            return false;
        } else if ((getUnitNumber() >= possibleAttacker.get().getUnits().size())
              || (getUnitNumber() < 0)) {
            return false;
        } else {
            return !possibleTarget.get().getUnits().isEmpty();
        }
    }
}
