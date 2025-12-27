/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.battleArmor.innerSphere;

import java.io.Serial;

import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.options.IGameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.artillery.ArtilleryWeapon;

/**
 * @author Sebastian Brocks
 * @since Oct 20, 2004
 */
public class ISBATubeArtillery extends ArtilleryWeapon {
    @Serial
    private static final long serialVersionUID = -2803991494958411097L;

    public ISBATubeArtillery() {
        super();
        name = "Tube Artillery (BA)";
        setInternalName(EquipmentTypeLookup.IS_BA_TUBE_ARTY);
        rackSize = 3;
        ammoType = AmmoType.AmmoTypeEnum.BA_TUBE;
        shortRange = 2;
        mediumRange = 2;
        longRange = 2;
        extremeRange = 2; // No extreme range.
        tonnage = 0.5;
        criticalSlots = 4;
        bv = 27;
        cost = 200000;
        rulesRefs = "96, TO:AUE";
        flags = flags.or(F_BA_WEAPON).andNot(F_MEK_WEAPON).andNot(F_TANK_WEAPON).or(F_MEK_MORTAR).or(F_MISSILE);
        damage = DAMAGE_BY_CLUSTER_TABLE;
        atClass = CLASS_ARTILLERY;
        infDamageClass = WEAPON_CLUSTER_MISSILE;
        techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.E)
              .setISAdvancement(3070, 3075, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.CS)
              .setProductionFactions(Faction.CS);
    }

    @Override
    public boolean hasIndirectFire() {
        return true;
    }

    @Override
    public void adaptToGameOptions(IGameOptions gameOptions) {
        super.adaptToGameOptions(gameOptions);

        // Indirect Fire
        if (gameOptions.booleanOption(OptionsConstants.BASE_INDIRECT_FIRE)) {
            addMode("");
            addMode("Indirect");
        } else {
            removeMode("");
            removeMode("Indirect");
        }
    }
}
