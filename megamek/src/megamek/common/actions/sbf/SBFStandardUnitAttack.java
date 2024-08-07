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
package megamek.common.actions.sbf;

import megamek.common.alphaStrike.ASRange;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFGame;
import megamek.server.sbf.SBFActionHandler;
import megamek.server.sbf.SBFGameManager;
import megamek.server.sbf.SBFStandardUnitAttackHandler;

import java.util.Optional;

import static org.apache.logging.log4j.LogManager.getLogger;

public class SBFStandardUnitAttack extends AbstractSBFAttackAction {

    private final int unitNumber;
    private final ASRange range;

    /**
     * Creates a standard attack of an SBF Unit on another formation.
     * The unit number identifies the SBF Unit making the attack, i.e. 1 for the first of the formation's units,
     * 2 for the second etc.
     *
     * @param formationId The attacker's ID
     * @param unitNumber The number of the attacking SBF Unit inside the formation
     * @param targetId The target's ID
     */
    public SBFStandardUnitAttack(int formationId, int unitNumber, int targetId, ASRange range) {
        super(formationId, targetId);
        this.unitNumber = unitNumber;
        this.range = range;
    }

    /**
     * Returns the number of the SBF Unit making the attack, i.e. 1 for the first of the formation's
     * units, 2 for the second.
     *
     * @return The unit number within the formation
     */
    public int getUnitNumber() {
        return unitNumber;
    }

    public ASRange getRange() {
        return range;
    }

    @Override
    public SBFActionHandler getHandler(SBFGameManager gameManager) {
        return new SBFStandardUnitAttackHandler(this, gameManager);
    }

    public boolean isDataValid(SBFGame game) {
        Optional<SBFFormation> possibleAttacker = game.getFormation(getEntityId());
        Optional<SBFFormation> possibleTarget = game.getFormation(getTargetId());
        if (getEntityId() == getTargetId()) {
            getLogger().error("Formations cannot attack themselves! {}", this);
            return false;
        } else if (possibleAttacker.isEmpty()) {
            getLogger().error("Could not find attacking formation! {}", this);
            return false;
        } else if (possibleTarget.isEmpty()) {
            getLogger().error("Could not find target formation! {}", this);
            return false;
        } else if ((getUnitNumber() >= possibleAttacker.get().getUnits().size())
                || (getUnitNumber() < 0)) {
            getLogger().error("SBF Unit not found! {}", this);
            return false;
        } else if (possibleTarget.get().getUnits().isEmpty()) {
            getLogger().error("Target has no units! {}", this);
            return false;
        }
        return true;
    }
}
