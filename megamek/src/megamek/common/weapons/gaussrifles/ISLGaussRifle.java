/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.gaussrifles;

import megamek.common.AmmoType;
import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.GRHandler;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Andrew Hunter
 * @since Oct 19, 2004
 */
public class ISLGaussRifle extends GaussWeapon {
    private static final long serialVersionUID = 8971550996626387100L;

    public ISLGaussRifle() {
        super();
        name = "Light Gauss Rifle";
        setInternalName("ISLightGaussRifle");
        addLookupName("IS Light Gauss Rifle");
        sortingName = "Gauss B";
        heat = 1;
        damage = 8;
        ammoType = AmmoType.AmmoTypeEnum.GAUSS_LIGHT;
        minimumRange = 3;
        shortRange = 8;
        mediumRange = 17;
        longRange = 25;
        extremeRange = 37;
        tonnage = 12.0;
        criticals = 5;
        bv = 159;
        cost = 275000;
        shortAV = 8;
        medAV = 8;
        longAV = 8;
        extAV = 8;
        maxRange = RANGE_EXT;
        explosionDamage = 16;
        rulesRefs = "219, TM";
        techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setISAdvancement(3049, 3056, 3065, DATE_NONE, DATE_NONE)
              .setISApproximate(true, false, false, false, false)
              .setPrototypeFactions(Faction.FW)
              .setProductionFactions(Faction.FW);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * megamek.common.weapons.Weapon#getCorrectHandler(megamek.common.ToHitData,
     * megamek.common.actions.WeaponAttackAction, megamek.common.Game,
     * megamek.server.Server)
     */
    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
          TWGameManager manager) {
        return new GRHandler(toHit, waa, game, manager);
    }

}
