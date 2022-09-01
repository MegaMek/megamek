/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.prototypes;

import megamek.common.Game;
import megamek.common.SimpleTechLevel;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.PrototypeLaserHandler;
import megamek.common.weapons.lasers.LaserWeapon;
import megamek.server.GameManager;

/**
 * @author Andrew Hunter
 * @since Sep 12, 2004
 */
public class ISERLaserLargePrototype extends LaserWeapon {
    private static final long serialVersionUID = -4745756742469577788L;

    public ISERLaserLargePrototype() {
        super();
        name = "Prototype ER Large Laser";
        setInternalName("ISERLargeLaserPrototype");
        addLookupName("IS ER Large Laser Prototype");
        shortName = "ER Large Laser (P)";
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
        tonnage = 5.0;
        criticals = 2;
        bv = 163;
        cost = 600000;
        rulesRefs = "103, IO";
        flags = flags.or(F_PROTOTYPE);
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_F, RATING_X, RATING_X)
                .setISAdvancement(3030, DATE_NONE, DATE_NONE, 3037, DATE_NONE)
                .setISApproximate(false, false, false, true, false)
                .setPrototypeFactions(F_FS)
                .setStaticTechLevel(SimpleTechLevel.EXPERIMENTAL);
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
                                              GameManager manager) {
        return new PrototypeLaserHandler(toHit, waa, game, manager);
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

    @Override
    public int getAlphaStrikeHeat() {
        return 18;
    }
}
