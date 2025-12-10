/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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

import java.io.Serial;
import java.text.DecimalFormat;

import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.util.RoundWeight;

/**
 * Reinforced naval repair facility allows ship to expend thrust with docked unit. Only available unpressurized. See
 * TacOps 334-5 for rules.
 *
 * @author Neoancient
 */
public class ReinforcedRepairFacility extends NavalRepairFacility {

    @Serial
    private static final long serialVersionUID = -3474202393188929092L;

    /**
     * The default constructor is only for serialization.
     */

    protected ReinforcedRepairFacility() {
        totalSpace = 0;
        currentSpace = 0;
    }

    /**
     * Create a new repair facility
     *
     * @param size   Maximum capacity in tons
     * @param facing The armor facing of the facility
     */
    public ReinforcedRepairFacility(double size, int doors, int bayNumber, int facing) {
        super(size, doors, bayNumber, facing, false);
    }

    @Override
    public boolean isPressurized() {
        return false;
    }

    @Override
    public String getTransporterType() {
        return "Naval Repair Facility (Reinforced)";
    }

    @Override
    public double getWeight() {
        return RoundWeight.nextHalfTon(totalSpace * 0.1);
    }

    @Override
    public String toString() {
        return "reinforcedrepairfacility:"
              + totalSpace + FIELD_SEPARATOR
              + doors + FIELD_SEPARATOR
              + bayNumber + FIELD_SEPARATOR
              + FACING_PREFIX + getFacing();
    }

    @Override
    public String getUnusedString(boolean showRecovery) {
        return "Reinforced Naval Repair Facility: " + DecimalFormat.getInstance().format(totalSpace) + " tons";
    }

    public static TechAdvancement techAdvancement() {
        return new TechAdvancement(TechBase.IS).setAdvancement(2750, DATE_NONE, DATE_NONE, 2766, 3065)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.TH).setReintroductionFactions(Faction.WB).setTechRating(TechRating.C)
              .setAvailability(AvailabilityValue.F, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.F)
              .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
    }

    @Override
    public long getCost() {
        return 30000L * (long) totalSpace;
    }

}
