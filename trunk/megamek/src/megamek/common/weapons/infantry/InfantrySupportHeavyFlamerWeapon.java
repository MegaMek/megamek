/**
 * MegaMek - Copyright (C) 2004,2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
/*
 * Created on Sep 7, 2005
 *
 */
package megamek.common.weapons.infantry;

import megamek.common.IGame;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.weapons.AttackHandler;
import megamek.server.Server;

/**
 * @author Sebastian Brocks
 */
public class InfantrySupportHeavyFlamerWeapon extends InfantryWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -5741978934100309295L;

    public InfantrySupportHeavyFlamerWeapon() {
        super();
        techLevel = TechConstants.T_INTRO_BOXSET;
        name = "Flamer (Heavy)";
        setInternalName(name);
        addLookupName("InfantryHeavyFlamer");
        // Flamer (Heavy), TM p. 300
        cost = 200;
        bv = 0.51;
        flags = flags.or(F_DIRECT_FIRE).or(F_FLAMER).or(F_ENERGY).or(F_INF_SUPPORT);
        String[] modeStrings = { "Damage", "Heat" };
        setModes(modeStrings);
        infantryDamage = 0.63;
        infantryRange = 0;
        crew = 2;
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, IGame game, Server server) {
        if ((game.getEntity(waa.getEntityId()).getEquipment(waa.getWeaponId()).curMode().equals("Heat"))) {
            return new InfantryFlamerHeatHandler(toHit, waa, game, server);
        }
        return new InfantryWeaponHandler(toHit, waa, game, server);
    }
}
