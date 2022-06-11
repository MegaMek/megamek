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
package megamek.common.weapons.battlearmor;

import megamek.common.*;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.Weapon;
import megamek.server.GameManager;

public class CLBALBX extends Weapon {
    private static final long serialVersionUID = 2978911783244524588L;

    public CLBALBX() {
        super();

        name = "Battle Armor LB-X AC";
        setInternalName(name);
        addLookupName("CLBALBX");
        addLookupName("Clan BA LBX");
        heat = 0;
        damage = 4;
        rackSize = 4;
        shortRange = 2;
        mediumRange = 5;
        longRange = 8;
        extremeRange = 10;
        tonnage = 0.4;
        criticals = 2;
        toHitModifier = -1;
        ammoType = AmmoType.T_NA;
        bv = 20;
        cost = 70000;
        flags = flags.or(F_NO_FIRES).or(F_BA_WEAPON).or(F_BALLISTIC).andNot(F_MECH_WEAPON).andNot(F_TANK_WEAPON).andNot(F_AERO_WEAPON).andNot(F_PROTO_WEAPON);
        rulesRefs = "207, TM";
        techAdvancement.setTechBase(TECH_BASE_CLAN)
                .setTechRating(RATING_F)
                .setAvailability(RATING_X, RATING_X, RATING_E, RATING_D)
                .setClanAdvancement(3075, 3085)
                .setClanApproximate(false, false)
                .setPrototypeFactions(F_CNC)
                .setProductionFactions(F_CNC)
                .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
                                              GameManager manager) {
        return new BALBXHandler(toHit, waa, game, manager);
    }

    /**
     * non-squad size version for AlphaStrike base damage
     */
    @Override
    public double getBattleForceDamage(int range) {
        double damage = 0;
        if (range <= getLongRange()) {
            damage = Compute.calculateClusterHitTableAmount(7, getDamage());
            damage *= 1.05; // -1 to hit
            if (range == AlphaStrikeElement.SHORT_RANGE && getMinimumRange() > 0) {
                damage = adjustBattleForceDamageForMinRange(damage);
            }
        }
        return damage / 10.0;
    }

    @Override
    public double getBattleForceDamage(int range, int baSquadSize) {
        double damage = 0;
        if (range <= getLongRange()) {
            damage = Compute.calculateClusterHitTableAmount(7, getDamage() * baSquadSize);
            damage *= 1.05; // -1 to hit
            if (range == AlphaStrikeElement.SHORT_RANGE && getMinimumRange() > 0) {
                damage = adjustBattleForceDamageForMinRange(damage);
            }
        }
        return damage / 10.0;
    }
}
