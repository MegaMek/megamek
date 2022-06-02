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
package megamek.common.weapons;

import java.io.Serializable;

import megamek.common.AmmoType;
import megamek.common.Game;
import megamek.common.TargetRoll;
import megamek.common.ToHitData;
import megamek.common.WeaponType;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.bayweapons.CapitalLaserBayWeapon;
import megamek.common.weapons.bayweapons.SubCapLaserBayWeapon;
import megamek.server.GameManager;

/**
 * A class representing a weapon.
 * @author Andrew Hunter 
 * @since May 10, 2004
 */
public abstract class Weapon extends WeaponType implements Serializable {
    private static final long serialVersionUID = -8781224279449654544L;

    public Weapon() {
        this.ammoType = AmmoType.T_NA;
        this.minimumRange = WEAPON_NA;
    }
    
    //Mode text tokens
    public static final String MODE_FLAMER_DAMAGE = "Damage";
    public static final String MODE_FLAMER_HEAT = "Heat";
    
    public static final String MODE_AMS_ON = "On";
    public static final String MODE_AMS_OFF = "Off";
    public static final String MODE_AMS_MANUAL = "Use as Weapon";
    
    public static final String MODE_CAP_LASER_AAA = "AAA";
    
    public static final String MODE_CAPITAL_BRACKET_80 = "Bracket 80%";
    public static final String MODE_CAPITAL_BRACKET_60 = "Bracket 60%";
    public static final String MODE_CAPITAL_BRACKET_40 = "Bracket 40%";
    
    public static final String MODE_CAP_MISSILE_WAYPOINT_BEARING_EXT = "Waypoint Launch Bearings-Only Extreme Detection Range";
    public static final String MODE_CAP_MISSILE_WAYPOINT_BEARING_LONG = "Waypoint Launch Bearings-Only Long Detection Range";
    public static final String MODE_CAP_MISSILE_WAYPOINT_BEARING_MED = "Waypoint Launch Bearings-Only Medium Detection Range";
    public static final String MODE_CAP_MISSILE_WAYPOINT_BEARING_SHORT = "Waypoint Launch Bearings-Only Short Detection Range";
    public static final String MODE_CAP_MISSILE_WAYPOINT = "Waypoint Launch";
    
    public static final String MODE_CAP_MISSILE_BEARING_EXT = "Bearings-Only Extreme Detection Range";
    public static final String MODE_CAP_MISSILE_BEARING_LONG = "Bearings-Only Long Detection Range";
    public static final String MODE_CAP_MISSILE_BEARING_MED = "Bearings-Only Medium Detection Range";
    public static final String MODE_CAP_MISSILE_BEARING_SHORT = "Bearings-Only Short Detection Range";
    
    public static final String MODE_CAP_MISSILE_TELE_OPERATED = "Tele-Operated";
    
    public static final String MODE_AC_RAPID = "Rapid";
    public static final String MODE_AC_SINGLE = "Single";
    public static final String MODE_UAC_ULTRA = "Ultra";
    public static final String MODE_RAC_TWO_SHOT = "2-shot";
    public static final String MODE_RAC_THREE_SHOT = "3-shot";
    public static final String MODE_RAC_FOUR_SHOT = "4-shot";
    public static final String MODE_RAC_FIVE_SHOT = "5-shot";
    public static final String MODE_RAC_SIX_SHOT = "6-shot";
    
    public static final String MODE_GAUSS_POWERED_DOWN = "Powered Down";
    
    public static final String MODE_MISSILE_INDIRECT = "Indirect";
    
    public static final String MODE_PPC_CHARGE = "Charge";
    
    public static final String MODE_POINT_DEFENSE = "Point Defense";
    
    public static final String MODE_NORMAL = "Normal";
    

    public @Nullable AttackHandler fire(WeaponAttackAction waa, Game game, GameManager gameManager) {
        ToHitData toHit = waa.toHit(game);
        // FIXME: SUPER DUPER EVIL HACK: swarm missile handlers must be returned even
        // if the have an impossible to hit, because there might be other targets
        // someone else please please figure out how to do this nice
        AttackHandler ah = getCorrectHandler(toHit, waa, game, gameManager);
        return (ah instanceof LRMSwarmHandler) ? ah
                : (toHit.getValue() == TargetRoll.IMPOSSIBLE) ? null : ah;
    }

    protected AttackHandler getCorrectHandler(ToHitData toHit,
            WeaponAttackAction waa, Game game, GameManager gameManager) {
        return new WeaponHandler(toHit, waa, game, gameManager);
    }
    
    /**
     * Adapt the weapon type to the Game Options such as
     * PPC Field Inhibitors or Dial Down Damage, usually
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
            if (!gOp.booleanOption(OptionsConstants.BASE_FLAMER_HEAT)) {
                addMode(MODE_FLAMER_DAMAGE);
                addMode(MODE_FLAMER_HEAT);
            } else {
                removeMode(MODE_FLAMER_DAMAGE);
                removeMode(MODE_FLAMER_HEAT);
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
                if (getAtClass() == WeaponType.CLASS_TELE_MISSILE) {
                    setInstantModeSwitch(false);
                    addMode(MODE_NORMAL);
                    addMode(MODE_CAP_MISSILE_TELE_OPERATED);
                }
                
                if (gOp.booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_WAYPOINT_LAUNCH)) {
                    setInstantModeSwitch(false);
                    addMode(MODE_NORMAL);
                    addMode(MODE_CAP_MISSILE_WAYPOINT);
                    if (gOp.booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_BEARINGS_ONLY_LAUNCH)) {
                        addMode(MODE_CAP_MISSILE_WAYPOINT_BEARING_EXT);
                        addMode(MODE_CAP_MISSILE_WAYPOINT_BEARING_LONG);
                        addMode(MODE_CAP_MISSILE_WAYPOINT_BEARING_MED);
                        addMode(MODE_CAP_MISSILE_WAYPOINT_BEARING_SHORT);
                    } else {
                        removeMode(MODE_CAP_MISSILE_WAYPOINT_BEARING_EXT);
                        removeMode(MODE_CAP_MISSILE_WAYPOINT_BEARING_LONG);
                        removeMode(MODE_CAP_MISSILE_WAYPOINT_BEARING_MED);
                        removeMode(MODE_CAP_MISSILE_WAYPOINT_BEARING_SHORT);
                    }
                } else {
                    removeMode(MODE_CAP_MISSILE_WAYPOINT);
                }

                if (gOp.booleanOption(OptionsConstants.ADVAERORULES_STRATOPS_BEARINGS_ONLY_LAUNCH)) {
                    setInstantModeSwitch(false);
                    addMode(MODE_NORMAL);
                    addMode(MODE_CAP_MISSILE_BEARING_EXT);
                    addMode(MODE_CAP_MISSILE_BEARING_LONG);
                    addMode(MODE_CAP_MISSILE_BEARING_MED);
                    addMode(MODE_CAP_MISSILE_BEARING_SHORT);
                } else {
                    removeMode(MODE_CAP_MISSILE_BEARING_EXT);
                    removeMode(MODE_CAP_MISSILE_BEARING_LONG);
                    removeMode(MODE_CAP_MISSILE_BEARING_MED);
                    removeMode(MODE_CAP_MISSILE_BEARING_SHORT);
                }
            }
        }

        if (hasFlag(WeaponType.F_AMS)) {
            if (gOp.booleanOption(OptionsConstants.ADVCOMBAT_TACOPS_MANUAL_AMS)) {
                addMode(Weapon.MODE_AMS_MANUAL);
            }
            if (gOp.booleanOption(OptionsConstants.BASE_AUTO_AMS)) {
                removeMode("Automatic");
            } else {
                addMode("Automatic");
            }
        }
    }
}
