/*
  Copyright (C) 2004, 2005, 2006, 2007 Ben Mazur (bmazur@sev.org)
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

package megamek.common.weapons.mgs;

import megamek.common.AmmoType;
import megamek.common.Game;
import megamek.common.Mounted;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AmmoWeapon;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.MGAWeaponHandler;
import megamek.server.totalwarfare.TWGameManager;

/**
 * @author Sebastian Brocks
 */
public class CLHeavyMGA extends AmmoWeapon {
    private static final long serialVersionUID = 7515050923963957889L;

    public CLHeavyMGA() {
        super();

        name = "Heavy Machine Gun Array";
        setInternalName("CLHMGA");
        setInternalName("Clan Heavy Machine Gun Array");
        sortingName = "MGA D";
        heat = 0;
        damage = 3;
        infDamageClass = WeaponType.WEAPON_BURST_3D6;
        rackSize = 3;
        ammoType = AmmoType.AmmoTypeEnum.MG_HEAVY;
        minimumRange = WEAPON_NA;
        shortRange = 1;
        mediumRange = 2;
        longRange = 2;
        extremeRange = 4;
        tonnage = 0.25;
        criticals = 1;
        bv = 0; // we'll have to calculate this in calculateBV(),
        // because it depends on the number of MGs linked to
        // the MGA
        flags = flags.or(F_MEK_WEAPON).or(F_TANK_WEAPON).or(F_AERO_WEAPON)
              .or(F_PROTO_WEAPON).or(F_BALLISTIC).or(F_BURST_FIRE).or(F_MGA);
        cost = 1250;
        String[] modeStrings = { "Linked", "Off" };
        setModes(modeStrings);
        instantModeSwitch = false;
        rulesRefs = "228, TM";
        techAdvancement.setTechBase(TechBase.CLAN)
              .setIntroLevel(false)
              .setUnofficial(false)
              .setTechRating(TechRating.E)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.F, AvailabilityValue.F)
              .setClanAdvancement(DATE_NONE, DATE_NONE, 3069, DATE_NONE, DATE_NONE)
              .setClanApproximate(false, false, false, false, false);
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
        return new MGAWeaponHandler(toHit, waa, game, manager);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted<?> fcs) {
        return 0;
    }

}
