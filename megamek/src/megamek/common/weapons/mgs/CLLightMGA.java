/*
 * MegaMek - Copyright (C) 2004, 2005, 2006, 2007 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
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
import megamek.server.gameManager.GameManager;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class CLLightMGA extends AmmoWeapon {
    private static final long serialVersionUID = 5151562824587975407L;

    public CLLightMGA() {
        super();
        name = "Light Machine Gun Array";
        addLookupName("Clan Light Machine Gun Array");
        setInternalName("CLLMGA");
        sortingName = "MGA B";
        heat = 0;
        damage = 1;
        infDamageClass = WeaponType.WEAPON_BURST_1D6;
        rackSize = 1;
        ammoType = AmmoType.T_MG_LIGHT;
        minimumRange = WEAPON_NA;
        shortRange = 2;
        mediumRange = 4;
        longRange = 6;
        extremeRange = 8;
        tonnage = 0.25;
        criticals = 1;
        bv = 0; // we'll have to calculate this in calculateBV(),
        // because it depends on the number of MGs linked to the MGA
        flags = flags.or(F_MECH_WEAPON).or(F_TANK_WEAPON).or(F_AERO_WEAPON)
                .or(F_PROTO_WEAPON).or(F_BALLISTIC).or(F_BURST_FIRE).or(F_MGA);
        cost = 1250;
        String[] modeStrings = { "Linked", "Off" };
        setModes(modeStrings);
        instantModeSwitch = false;
        rulesRefs = "228, TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_F)
                .setClanAdvancement(DATE_NONE, DATE_NONE, 3069, DATE_NONE, DATE_NONE)
                .setClanApproximate(false, false, false, false, false);
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,GameManager manager) {
        return new MGAWeaponHandler(toHit, waa, game, manager);
    }

    @Override
    public double getBattleForceDamage(int range, Mounted fcs) {
        return 0;
    }

}
