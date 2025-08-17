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

package megamek.common.weapons.bayweapons;

import megamek.common.units.Entity;
import megamek.common.game.Game;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.ToHitData;
import megamek.common.equipment.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.weapons.handlers.artillery.ArtilleryBayWeaponIndirectFireHandler;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.BayWeaponHandler;
import megamek.common.weapons.Weapon;
import megamek.server.totalwarfare.TWGameManager;

/**
 * This is my attempt to get weapon bays treated as normal weapons rather than the current hack in place
 *
 * @author Jay Lawson
 * @since Sep 24, 2004
 */
public abstract class BayWeapon extends Weapon {
    private static final long serialVersionUID = -1787970217528405766L;

    public BayWeapon() {
        super();
        // Tech progression for Small Craft or DropShip, using primitive for production and standard
        // for common.
        techAdvancement = new TechAdvancement(TechBase.ALL)
              .setAdvancement(DATE_ES, 2200, 2400)
              .setProductionFactions(Faction.TA)
              .setTechRating(TechRating.D)
              .setAvailability(AvailabilityValue.C, AvailabilityValue.E, AvailabilityValue.D, AvailabilityValue.C)
              .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    @Override
    public AttackHandler fire(WeaponAttackAction waa, Game game, TWGameManager manager) {
        // Just in case. Often necessary when/if multiple ammo weapons are
        // fired; if this line not present
        // then when one ammo slots run dry the rest silently don't fire.
        return super.fire(waa, game, manager);
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
          TWGameManager manager) {
        if ((isCapital() || isSubCapital()) && waa.isOrbitToSurface(game)) {
            return new ArtilleryBayWeaponIndirectFireHandler(toHit, waa, game, manager);
        } else {
            return new BayWeaponHandler(toHit, waa, game, manager);
        }
    }

    @Override
    public int getMaxRange(WeaponMounted weapon) {
        return getMaxRange(weapon, null);
    }

    @Override
    public int getMaxRange(WeaponMounted weapon, AmmoMounted ammo) {
        int mrange = RANGE_SHORT;
        Entity ae = weapon.getEntity();
        AmmoMounted mAmmo;
        if (null != ae) {
            for (WeaponMounted bayW : weapon.getBayWeapons()) {
                mAmmo = (ammo != null) ? ammo : bayW.getLinkedAmmo();
                WeaponType bayWType = bayW.getType();
                if (bayWType.getMaxRange(bayW, mAmmo) > mrange) {
                    mrange = bayWType.getMaxRange(bayW, mAmmo);
                }
            }
        }
        return mrange;
    }
}
