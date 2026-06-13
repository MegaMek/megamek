/*
 * Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
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

/*
 * Created on Sep 7, 2005
 *
 */
package megamek.common.weapons.infantry.rifle;

import java.io.Serial;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * @author Ben Grills
 */
public class InfantryRifleFederatedBarrettM61ALaserInfernoWeapon extends InfantryWeapon {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -3164871600230559641L;

    public InfantryRifleFederatedBarrettM61ALaserInfernoWeapon() {
        super();

        name = "Laser Rifle (Federated-Barrett M61A) (Inferno Grenades)";
        setInternalName("InfantryFederatedBarrettM61AInferno");
        addLookupName(name);
        ammoType = AmmoType.AmmoTypeEnum.INFANTRY;
        cost = 2150;
        bv = 1.17;
        tonnage = .006;
        flags = flags.or(F_INFERNO).or(F_DIRECT_FIRE).or(F_ENERGY);
        String[] modeStrings = { "Damage", "Heat" };
        setModes(modeStrings);
        infantryDamage = 0.55;
        infantryRange = 2;
        ammoWeight = 0.0003;
        shots = 6;
        rulesRefs = "273, TM";
        techAdvancement.setTechBase(TechBase.IS).setISAdvancement(3058, 3062, 3085, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FS)
              .setProductionFactions(Faction.FS).setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.D, AvailabilityValue.C);

    }
}
