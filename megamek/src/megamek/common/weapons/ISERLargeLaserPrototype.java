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
 * Created on Sep 12, 2004
 *
 */
package megamek.common.weapons;

import megamek.common.IGame;
import megamek.common.TechConstants;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;

/**
 * @author Andrew Hunter
 */
public class ISERLargeLaserPrototype extends LaserWeapon {

    /**
     *
     */
    private static final long serialVersionUID = -4745756742469577788L;

    public ISERLargeLaserPrototype() {
        super();
        techLevel.put(2615, TechConstants.T_IS_EXPERIMENTAL);
        name = "ER Large Laser Prototype";
        setInternalName("ISERLargeLaserPrototype");
        addLookupName("IS ER Large Laser Prototype");
        toHitModifier = 1;
        flags = flags.or(F_PROTOTYPE);
        heat = 12;
        damage = 8;
        shortRange = 7;
        mediumRange = 14;
        longRange = 19;
        extremeRange = 28;
        waterShortRange = 3;
        waterMediumRange = 9;
        waterLongRange = 12;
        waterExtremeRange = 18;
        tonnage = 5.0f;
        criticals = 2;
        bv = 163;
        cost = 200000;
        introDate = 2615;
        extinctDate = 2620;
        reintroDate = 3037;
        availRating = new int[] { RATING_E,RATING_F, RATING_D };
        techRating = RATING_E;
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
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new PrototypeLaserHandler(toHit, waa, game, server);
    }

    @Override
    public int getLongRange() {
        GameOptions options = getGameOptions();
        if (options == null) {
            return super.getLongRange();
        } else if (options.getOption(OptionsConstants.ADVCOMBAT_INCREASED_ISERLL_RANGE) == null) {
            return super.getLongRange();
        }
        if (options.getOption(OptionsConstants.ADVCOMBAT_INCREASED_ISERLL_RANGE).booleanValue()) {
            return 21;
        }
        return super.getLongRange();
    }
}
