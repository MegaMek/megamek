/**
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.weapons.mortars;

import megamek.common.AmmoType;
import megamek.common.BattleForceElement;
import megamek.common.Compute;
import megamek.common.Game;
import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.AmmoWeapon;
import megamek.common.weapons.AttackHandler;
import megamek.common.weapons.MekMortarAirburstHandler;
import megamek.common.weapons.MekMortarAntiPersonnelHandler;
import megamek.common.weapons.MekMortarFlareHandler;
import megamek.common.weapons.MekMortarHandler;
import megamek.common.weapons.MekMortarSmokeHandler;
import megamek.server.Server;

/**
 * @author Jason Tighe
 */
public abstract class MekMortarWeapon extends AmmoWeapon {

    private static final long serialVersionUID = -4887277242270179970L;

    public MekMortarWeapon() {
        super();
        ammoType = AmmoType.T_MEK_MORTAR;
        damage = DAMAGE_BY_CLUSTERTABLE;
        atClass = CLASS_NONE;
        flags = flags.or(F_MEK_MORTAR).or(F_MECH_WEAPON).or(F_MISSILE)
                .or(F_TANK_WEAPON);
        infDamageClass = WEAPON_CLUSTER_MISSILE;
    }

    @Override
    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, Game game, Server server) {
        
        AmmoType atype = (AmmoType) game.getEntity(waa.getEntityId())
                .getEquipment(waa.getWeaponId()).getLinked().getType();
        if (atype.getMunitionType() == AmmoType.M_AIRBURST) {
            return new MekMortarAirburstHandler(toHit, waa, game, server);
        } else if (atype.getMunitionType() == AmmoType.M_ANTI_PERSONNEL) {
            return new MekMortarAntiPersonnelHandler(toHit, waa, game, server);
        } else if (atype.getMunitionType() == AmmoType.M_FLARE) {
            return new MekMortarFlareHandler(toHit, waa, game, server);
        } else if (atype.getMunitionType() == AmmoType.M_SEMIGUIDED) {
            // Semi-guided works like shaped-charge, but can benefit from tag
            return new MekMortarHandler(toHit, waa, game, server);
        } else if (atype.getMunitionType() == AmmoType.M_SMOKE_WARHEAD) {
            return new MekMortarSmokeHandler(toHit, waa, game, server);
        }
        // If it doesn't match other types, it's the default armor-piercing
        return new MekMortarHandler(toHit, waa, game, server);
    }
    
    @Override
    public double getBattleForceDamage(int range) {
        if (range > getLongRange()) {
            return 0;
        }
        double damage = Compute.calculateClusterHitTableAmount(7, getRackSize()) * 2;
        if (range == BattleForceElement.SHORT_RANGE && getMinimumRange() > 0) {
            damage = adjustBattleForceDamageForMinRange(damage);
        }
        return damage / 10.0;
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

    @Override
    public String getSortingName() {
        return "Mek Mortar " + rackSize;
    }
}
