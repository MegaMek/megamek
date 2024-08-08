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

import megamek.common.BoardLocation;
import megamek.common.strategicBattleSystems.SBFGame;
import megamek.server.sbf.SBFActionHandler;
import megamek.server.sbf.SBFGameManager;

public class SBFUnitToHexAttack extends AbstractSBFAttackAction {

    private final int unitNumber;

    /**
     * Creates a standard attack of an SBF Unit on another formation.
     * The unit number identifies the SBF Unit making the attack, i.e. 1 for the first of the formation's units,
     * 2 for the second etc.
     *
     * @param formationId The attacker's ID
     * @param unitNumber The number of the attacking SBF Unit inside the formation
     * @param target The location to attack
     */
    public SBFUnitToHexAttack(int formationId, int unitNumber, BoardLocation target) {
        super(formationId, target.hashCode());
        this.unitNumber = unitNumber;
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

    @Override
    public SBFActionHandler getHandler(SBFGameManager gameManager) {
        return null;
    }

    @Override
    public boolean isDataValid(SBFGame game) {
        return false;
    }

    //TODO must convert between id and location -> cant use hashcode
}
