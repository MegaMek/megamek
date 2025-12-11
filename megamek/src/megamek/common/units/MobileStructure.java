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

package megamek.common.units;

import megamek.common.TechAdvancement;
import megamek.common.enums.BuildingType;

/**
 * <b>Not Implemented</b>
 * <br>
 * Implementation of TO:AUE's Mobile Structures.
 * <br>
 * Extends {@link AbstractBuildingEntity}
 */
public class MobileStructure extends AbstractBuildingEntity {

    public MobileStructure(BuildingType type, int bldgClass) {
        super(type, bldgClass);
    }

    /**
     * @see UnitType
     */
    @Override
    public int getUnitType() {
        return UnitType.MOBILE_STRUCTURE;
    }

    /**
     * return - the base construction option tech advancement
     */
    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    /**
     * Returns the name of the type of movement used.
     *
     * @param movementType
     */
    @Override
    public String getMovementString(EntityMovementType movementType) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    /**
     * Returns the abbreviation of the name of the type of movement used.
     *
     * @param movementType
     */
    @Override
    public String getMovementAbbr(EntityMovementType movementType) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    /**
     * Calculates a "generic" Battle Value that is based on the average of all units of this type and tonnage. The
     * purpose of this generic Battle Value is to allow a comparison of this unit's actual BV to that for units of its
     * class. This can be used to balance forces without respect to unit or pilot quality.
     * <p>
     * The generic BV values are calculated by a statistical elasticity model based on all data from the MegaMek
     * database.
     *
     * @return The generic Battle value for this unit based on its tonnage and type
     */
    @Override
    public int getGenericBattleValue() {
        return 0;
    }

    /**
     * The maximum elevation change the entity can cross
     */
    @Override
    public int getMaxElevationChange() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public boolean hasEngine() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
