/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.weapons.missiles;

import megamek.common.Game;
import megamek.common.SimpleTechLevel;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.ThunderBoltWeaponHandler;
import megamek.server.GameManager;

/**
 * @author Sebastian Brocks
 */
public abstract class ThunderBoltWeapon extends MissileWeapon {
    private static final long serialVersionUID = 3496376576886743383L;

    public ThunderBoltWeapon() {
        super();
        rackSize = 1;
        minimumRange = 5;
        shortRange = 6;
        mediumRange = 12;
        longRange = 18;
        extremeRange = 24;
        maxRange = RANGE_MED;
        flags = flags.or(F_LARGEMISSILE);
        atClass = CLASS_THUNDERBOLT;
        rulesRefs = "159, TO:AUE";
        techAdvancement.setTechBase(TECH_BASE_IS)
                .setIntroLevel(false)
                .setUnofficial(false)
                .setTechRating(RATING_E)
                .setAvailability(RATING_X, RATING_X, RATING_F, RATING_E)
                .setISAdvancement(3052, 3072, 3081, DATE_NONE, DATE_NONE)
                .setISApproximate(false, false, false, false, false)
                .setPrototypeFactions(F_FS)
                .setProductionFactions(F_FS, F_LC)
                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, Game game, GameManager manager) {
        return new ThunderBoltWeaponHandler(toHit, waa, game, manager);
    }

    @Override
    public boolean hasIndirectFire() {
        return true;
    }
    
    @Override
    public void adaptToGameOptions(GameOptions gOp) {
        super.adaptToGameOptions(gOp);

        // Indirect Fire
        if (gOp.booleanOption(OptionsConstants.BASE_INDIRECT_FIRE)) {
            addMode("");
            addMode("Indirect");
        } else {
            removeMode("");
            removeMode("Indirect");
        }
    }
}