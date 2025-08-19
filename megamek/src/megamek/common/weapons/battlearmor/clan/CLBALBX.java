/*
 * Copyright (c) 2005 - Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons.battlearmor.clan;

import megamek.common.SimpleTechLevel;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.compute.Compute;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.AmmoType;
import megamek.common.game.Game;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.battlearmor.BALBXHandler;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.server.totalwarfare.TWGameManager;

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
        extremeRange = 12;
        tonnage = 0.4;
        criticalSlots = 2;
        toHitModifier = -1;
        // TODO: refactor BA ammo-based weapons to use real AmmoTypes (but not track ammo use)
        ammoType = AmmoType.AmmoTypeEnum.NA;
        bv = 20;
        cost = 70000;
        // TODO: implement F_NO_COUNT_AMMO
        flags = flags.or(F_NO_FIRES)
              .or(F_BA_WEAPON)
              .or(F_BALLISTIC)
              .andNot(F_MEK_WEAPON)
              .andNot(F_TANK_WEAPON)
              .andNot(F_AERO_WEAPON)
              .andNot(F_PROTO_WEAPON);
        rulesRefs = "207, TM";
        techAdvancement.setTechBase(TechBase.CLAN)
              .setTechRating(TechRating.F)
              .setAvailability(AvailabilityValue.X, AvailabilityValue.X, AvailabilityValue.E, AvailabilityValue.D)
              .setClanAdvancement(3075, 3085)
              .setClanApproximate(false, false)
              .setPrototypeFactions(Faction.CNC)
              .setProductionFactions(Faction.CNC)
              .setStaticTechLevel(SimpleTechLevel.ADVANCED);
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit, WeaponAttackAction waa, Game game,
          TWGameManager manager) {
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
            if ((range == AlphaStrikeElement.SHORT_RANGE) && (getMinimumRange() > 0)) {
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
            if ((range == AlphaStrikeElement.SHORT_RANGE) && (getMinimumRange() > 0)) {
                damage = adjustBattleForceDamageForMinRange(damage);
            }
        }
        return damage / 10.0;
    }

    @Override
    public int getBattleForceClass() {
        return BF_CLASS_FLAK;
    }
}
