/*
 * Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2007-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.weapons;

import static megamek.common.game.IGame.LOGGER;

import java.io.Serial;
import java.io.Serializable;

import megamek.common.ToHitData;
import megamek.common.actions.WeaponAttackAction;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.IGameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.weapons.bayWeapons.capital.CapitalLaserBayWeapon;
import megamek.common.weapons.bayWeapons.subCapital.SubCapLaserBayWeapon;
import megamek.common.weapons.handlers.AttackHandler;
import megamek.common.weapons.handlers.WeaponHandler;
import megamek.common.weapons.handlers.lrm.LRMSwarmHandler;
import megamek.server.totalwarfare.TWGameManager;

/**
 * A class representing a weapon.
 *
 * @author Andrew Hunter
 * @since May 10, 2004
 */
public abstract class Weapon extends WeaponType implements Serializable {
    @Serial
    private static final long serialVersionUID = -8781224279449654544L;

    public Weapon() {
        this.ammoType = AmmoType.AmmoTypeEnum.NA;
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
    public static final String MODE_INDIRECT_HEAT = "Indirect/Heat";

    public static final String MODE_PPC_CHARGE = "Charge";

    public static final String MODE_POINT_DEFENSE = "Point Defense";

    public static final String MODE_NORMAL = "Normal";


    public @Nullable AttackHandler fire(WeaponAttackAction weaponAttackAction, Game game, TWGameManager gameManager) {
        ToHitData toHit = weaponAttackAction.toHit(game);
        // FIXME: SUPER DUPER EVIL HACK: swarm missile handlers must be returned even if the have an impossible to
        //  hit, because there might be other targets someone else please please figure out how to do this nice
        AttackHandler attackHandler = getCorrectHandler(toHit, weaponAttackAction, game, gameManager);
        return (attackHandler instanceof LRMSwarmHandler) ? attackHandler
              : (toHit.getValue() == TargetRoll.IMPOSSIBLE) ? null : attackHandler;
    }

    @Nullable
    public AttackHandler getCorrectHandler(ToHitData toHit,
          WeaponAttackAction waa, Game game, TWGameManager gameManager) {
        try {
            return new WeaponHandler(toHit, waa, game, gameManager);
        } catch (EntityLoadingException ignored) {
            LOGGER.warn("Get Correct Handler - Attach Handler Received Null Entity.");
        }
        return null;
    }

    /**
     * Adapt the weapon type to the Game Options such as PPC Field Inhibitors or Dial Down Damage, usually adding or
     * removing modes. <B><I>When overriding this in a weapon subclass, call super()!</I></B>
     *
     * @param gameOptions The GameOptions (game.getOptions())
     *
     * @author Simon (Juliez)
     */
    public void adaptToGameOptions(IGameOptions gameOptions) {
        // Flamers are spread out over all sorts of weapon types not limited to FlamerWeapon.
        // So modes are handled here.
        if (hasFlag(WeaponType.F_FLAMER)) {
            if (!gameOptions.booleanOption(OptionsConstants.BASE_FLAMER_HEAT)) {
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
                    if (gameOptions.booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_AAA_LASER)) {
                        addMode("");
                        addMode("AAA");
                        addEndTurnMode("AAA");
                    } else {
                        removeMode("AAA");
                    }
                }
                if (gameOptions.booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_BRACKET_FIRE)) {
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
                    setInstantModeSwitch(true);
                    addMode(MODE_NORMAL);
                    addMode(MODE_CAP_MISSILE_TELE_OPERATED);
                }

                if (gameOptions.booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_WAYPOINT_LAUNCH)) {
                    setInstantModeSwitch(true);
                    addMode(MODE_NORMAL);
                    addMode(MODE_CAP_MISSILE_WAYPOINT);
                    if (gameOptions.booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_BEARINGS_ONLY_LAUNCH)) {
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

                if (gameOptions.booleanOption(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_BEARINGS_ONLY_LAUNCH)) {
                    setInstantModeSwitch(true);
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
            if (gameOptions.booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_MANUAL_AMS)) {
                addMode(Weapon.MODE_AMS_MANUAL);
            }
            if (gameOptions.booleanOption(OptionsConstants.BASE_AUTO_AMS)) {
                removeMode("Automatic");
            } else {
                addMode("Automatic");
            }
        }
    }
}
