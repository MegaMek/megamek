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
 * Created on May 10, 2004
 *
 */
package megamek.common.weapons;

import java.io.Serializable;

import megamek.common.AmmoType;
import megamek.common.IGame;
import megamek.common.TargetRoll;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.bayweapons.CapitalLaserBayWeapon;
import megamek.common.weapons.bayweapons.SubCapLaserBayWeapon;
import megamek.server.Server;

/**
 * @author Andrew Hunter A class representing a weapon.
 */
public abstract class Weapon extends WeaponType implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -8781224279449654544L;

    public Weapon() {
        this.ammoType = AmmoType.T_NA;
        this.minimumRange = WEAPON_NA;
    }

    public AttackHandler fire(WeaponAttackAction waa, IGame game, Server server) {
        ToHitData toHit = waa.toHit(game);
        // FIXME: SUPER DUPER EVIL HACK: swarm missile handlers must be returned
        // even
        // if the have an impossible to hit, because there might be other
        // targets
        // someone else please please figure out how to do this nice
        AttackHandler ah = getCorrectHandler(toHit, waa, game, server);
        if (ah instanceof LRMSwarmHandler || ah instanceof LRMSwarmIHandler)
            return ah;
        return toHit.getValue() == TargetRoll.IMPOSSIBLE ? null : ah;
    }

    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, IGame game, Server server) {
        return new WeaponHandler(toHit, waa, game, server);
    }
    
    /**
     * Adapt the weapon type to the Game Options such as
     * PPC Field Inhbitiors or Dial Down Damage, usually
     * adding or removing modes. <B><I>When overriding this in a
     * weapon subclass, call super()!</I></B>
     * 
     * @param gOp The GameOptions (game.getOptions())
     * @author Simon (Juliez)
     */
    public void adaptToGameOptions(GameOptions gOp) {
        // Flamers are spread out over all sorts of weapon types not limited to FlamerWeapon.
        // Therefore modes are handled here.
        if (hasFlag(WeaponType.F_FLAMER)) {
            if (gOp.booleanOption(OptionsConstants.BASE_FLAMER_HEAT)) {
                addMode("Damage");
                addMode("Heat");
            } else {
                removeMode("Damage");
                removeMode("Heat");
            }
        }
        
        // Capital weapons are spread out over all sorts of weapons.
        if (isCapital()) {
            if ((getAtClass() != WeaponType.CLASS_CAPITAL_MISSILE)
                    && (getAtClass() != WeaponType.CLASS_TELE_MISSILE)
                    && (getAtClass() != WeaponType.CLASS_AR10)) {

                if ((this instanceof CapitalLaserBayWeapon)
                        || (this instanceof SubCapLaserBayWeapon)) {
                    if (gOp.booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_AAA_LASER)) {
                        addMode("");
                        addMode("AAA");
                        addEndTurnMode("AAA");
                    } else {
                        removeMode("AAA");
                    }
                }
                if (gOp.booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_BRACKET_FIRE)) {
                    addMode("");
                    addMode("Bracket 80%");
                    addMode("Bracket 60%");
                    addMode("Bracket 40%");
                } else {
                    removeMode("Bracket 80%");
                    removeMode("Bracket 60%");
                    removeMode("Bracket 40%");
                }
                // If only the standard mode "" is left, remove that as well
                if (getModesCount() == 1) {
                    clearModes();
                }

            } else {

                if (gOp.booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_BEARINGS_ONLY_LAUNCH)) {
                    setInstantModeSwitch(false);
                    addMode("Bearings-Only Extreme Detection Range");
                    addMode("Bearings-Only Long Detection Range");
                    addMode("Bearings-Only Medium Detection Range");
                    addMode("Bearings-Only Short Detection Range");
                } else {
                    removeMode("Bearings-Only Extreme Detection Range");
                    removeMode("Bearings-Only Long Detection Range");
                    removeMode("Bearings-Only Medium Detection Range");
                    removeMode("Bearings-Only Short Detection Range");
                }
            }
        }

        if (hasFlag(WeaponType.F_AMS)) {
            if (gOp.booleanOption(OptionsConstants.BASE_AUTO_AMS)) {
                removeMode("Automatic");
            } else {
                addMode("Automatic");
            }
        }
    }
}
