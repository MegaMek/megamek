/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common.autoresolve.acar.action;

import megamek.common.alphaStrike.ASRange;
import megamek.common.autoresolve.acar.SimulationContext;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.acar.handler.StandardUnitAttackHandler;
import megamek.common.autoresolve.component.Formation;
import megamek.logging.MMLogger;

import java.util.Optional;

public class StandardUnitAttack extends AbstractAttackAction {
    private static final MMLogger logger = MMLogger.create(StandardUnitAttack.class);

    private final int unitNumber;
    private final ASRange range;
    private final ManeuverResult maneuverResult;

    public enum ManeuverResult {
        SUCCESS,
        FAILURE,
        DRAW
    }

    /**
     * Creates a standard attack of an SBF Unit on another formation.
     * The unit number identifies the SBF Unit making the attack, i.e. 1 for the
     * first of the formation's units,
     * 2 for the second etc.
     *
     * @param formationId The attacker's ID
     * @param unitNumber  The number of the attacking SBF Unit inside the formation
     * @param targetId    The target's ID
     */
    public StandardUnitAttack(int formationId, int unitNumber, int targetId, ASRange range, ManeuverResult maneuverResult) {
        super(formationId, targetId);
        this.unitNumber = unitNumber;
        this.range = range;
        this.maneuverResult = maneuverResult;
    }

    /**
     * Returns the index of the SBF Unit inside the formation, i.e. 0 for the first of
     * the formation's units, 1 for the second, 3 for the third etc.
     *
     * @return The unit index number within the formation
     */
    public int getUnitNumber() {
        return unitNumber;
    }

    public ASRange getRange() {
        return range;
    }

    public ManeuverResult getManeuverResult() {
        return maneuverResult;
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
        } else if (possibleTarget.get().getUnits().isEmpty()) {
            return false;
        }

        return true;
    }
}
