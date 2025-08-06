/*
 * Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.bombs;

import megamek.common.AmmoType;
import megamek.common.BombType.BombTypeEnum;
import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.ASEWMissileWeaponHandler;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.missiles.ThunderBoltWeapon;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Jay Lawson
 */
public class ISASEWMissileWeapon extends ThunderBoltWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -2094737986722961212L;

    public ISASEWMissileWeapon() {
        super();

        this.name = "Anti-Ship Electronic Warfare (ASEW) Missiles";
        this.setInternalName(BombTypeEnum.ASEW.getWeaponName());
        this.heat = 0;
        this.damage = 0;
        this.rackSize = 1;
        this.minimumRange = 7;
        this.shortRange = 14;
        this.mediumRange = 21;
        this.longRange = 28;
        this.extremeRange = 42;
        this.tonnage = 2;
        this.criticals = 0;
        this.hittable = false;
        this.bv = 0;
        this.cost = 20000;
        this.flags = flags.or(F_MISSILE)
              .or(F_LARGEMISSILE)
              .or(F_BOMB_WEAPON)
              .andNot(F_MEK_WEAPON)
              .andNot(F_TANK_WEAPON);
        this.shortAV = 0;
        this.medAV = 0;
        this.longAV = 0;
        this.maxRange = RANGE_MED;
        this.ammoType = AmmoType.AmmoTypeEnum.ASEW_MISSILE;
        this.capital = false;
        this.missileArmor = 30;
        rulesRefs = "358, TO";
        techAdvancement.setTechBase(TechBase.IS)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.E)
              .setISAdvancement(3067, 3073, DATE_NONE, DATE_NONE, DATE_NONE)
              .setISApproximate(false, false, false, false, false)
              .setPrototypeFactions(Faction.LC)
              .setProductionFactions(Faction.LC);
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
          WeaponAttackAction waa, Game game, TWGameManager manager) {
        return new ASEWMissileWeaponHandler(toHit, waa, game, manager);
    }
}
