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

package megamek.common.equipment;

import megamek.codeUtilities.MathUtility;
import megamek.common.units.Entity;

/**
 * Cargo Carrier TW p. 261. Unprotected, in slings, strapped to the top, in lightweight containers and so on.
 */
public class RoofRack extends ExternalCargo {

    public RoofRack(double tonnage) {
        super(tonnage);
    }

    /**
     * @param carrier
     *
     * @return the MP reduction due to cargo carried by this transporter
     */
    @Override
    public int getCargoMpReduction(Entity carrier) {
        // TW p. 261 movement penalties
        // A unit carrying up to a quarter its weight loses 3 MP or moves at half speed.
        // A unit carrying more than a quarter moves at half speed.
        if ((carrier != null) && (getCarriedTonnage() > 0) && (carrier.getWeight() > 0)) {
            int entityBaseMP = carrier.getOriginalWalkMP();
            double carrierWeight = carrier.getWeight();

            // If the carrier is carrying objects in its arms, they count towards this penalty too, but only if
            // something is on the roof.
            double additionalWeight = carrier.getDistinctCarriedObjects()
                  .stream()
                  .mapToDouble(ICarryable::getTonnage)
                  .sum();

            double carriedWeightRatio = getCarriedTonnage() / carrierWeight;
            int mpReduction = MathUtility.roundAwayFromZero(entityBaseMP / 2.0);

            if (carriedWeightRatio <= .25) {
                mpReduction = Math.min(entityBaseMP - 3, mpReduction);
            }

            return Math.max(0, entityBaseMP - mpReduction);
        }

        return 0;
    }
}
