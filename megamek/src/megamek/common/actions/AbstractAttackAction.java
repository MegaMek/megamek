/**
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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

package megamek.common.actions;

import java.util.Enumeration;

import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.IGame;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.PlanetaryConditions;
import megamek.common.Targetable;
import megamek.common.ToHitData;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;

/**
 * Abstract superclass for any action where an entity is attacking another
 * entity.
 */
public abstract class AbstractAttackAction extends AbstractEntityAction
        implements AttackAction {
    /**
     *
     */
    private static final long serialVersionUID = -897197664652217134L;
    private int targetType;
    private int targetId;

    // default to attacking an entity, since this is what most of them are
    public AbstractAttackAction(int entityId, int targetId) {
        super(entityId);
        targetType = Targetable.TYPE_ENTITY;
        this.targetId = targetId;
    }

    public AbstractAttackAction(int entityId, int targetType, int targetId) {
        super(entityId);
        this.targetType = targetType;
        this.targetId = targetId;
    }

    public int getTargetType() {
        return targetType;
    }

    public int getTargetId() {
        return targetId;
    }

    public void setTargetType(int targetType) {
        this.targetType = targetType;
    }

    public void setTargetId(int targetId) {
        this.targetId = targetId;
    }

    public Targetable getTarget(IGame g) {
        return g.getTarget(getTargetType(), getTargetId());
    }

    public Entity getEntity(IGame g) {
        Entity e = g.getEntity(getEntityId());
        // if we have an artyattack, we might need to get an out-of-game entity
        // if it died or fled
        if (e == null) {
            e = g.getOutOfGameEntity(getEntityId());
        }
        return e;
    }

    /**
     * used by the toHit of derived classes atype may be null if not using an
     * ammo based weapon
     */
    public static ToHitData nightModifiers(IGame game, Targetable target,
            AmmoType atype, Entity attacker, boolean isWeapon) {
        ToHitData toHit = null;

        Entity te = null;
        if (target.getTargetType() == Targetable.TYPE_ENTITY) {
            te = (Entity) target;
        }
        toHit = new ToHitData();

        int lightCond = game.getPlanetaryConditions().getLight();
        if(lightCond == PlanetaryConditions.L_DAY) {
            //not nighttime so just return
            return toHit;
        }

        // The base night penalty
        int hexIllumLvl = game.isPositionIlluminated(target.getPosition());
        int night_modifier = 0;
        night_modifier = game.getPlanetaryConditions().getLightHitPenalty(
                isWeapon);
        toHit.addModifier(night_modifier, game.getPlanetaryConditions()
                .getLightDisplayableName());

        boolean illuminated = false;
        if (te != null) {
            illuminated = te.isIlluminated();
            // hack for unresolved actions so client shows right BTH
            if (!illuminated) {
                for (Enumeration<EntityAction> actions = game.getActions(); 
                        actions.hasMoreElements();) {
                    EntityAction a = actions.nextElement();
                    if (a instanceof SearchlightAttackAction) {
                        SearchlightAttackAction saa = 
                                (SearchlightAttackAction) a;
                        if (saa.willIlluminate(game, te)) {
                            illuminated = true;
                            break;
                        }
                    }
                }
            }
        }
        // Searchlights reduce the penalty to zero (or 1 for pitch-black) 
        //  (except for dusk/dawn)
        int searchlightMod = Math.min(3, night_modifier);
        if ((te != null) && (lightCond > PlanetaryConditions.L_DUSK)
                && (te.isUsingSpotlight() || illuminated)) {
            if (te.isUsingSpotlight()) {
                toHit.addModifier(-searchlightMod, "target using searchlight");
                night_modifier = night_modifier - searchlightMod;
            } else if (illuminated) {
                toHit.addModifier(-searchlightMod,
                        "target illuminated by searchlight");
                night_modifier = night_modifier - searchlightMod;
            }
        }
        /*
        // Ignored with EI system & implants
        else if (attacker.hasActiveEiCockpit()) {
            toHit.addModifier(-night_modifier, "EI system");
            night_modifier = 0;
        }
        */
        // So do flares
        else if (hexIllumLvl == Game.ILLUMINATED_FLARE) {
            toHit.addModifier(-night_modifier, "target illuminated by flare");
            night_modifier = 0;
        }
        else if (hexIllumLvl == Game.ILLUMINATED_FIRE) {
            int fireMod = Math.min(2, night_modifier);
            toHit.addModifier(-fireMod, "target illuminated by fire");
            night_modifier -= fireMod;
        } 
        else if (hexIllumLvl == Game.ILLUMINATED_LIGHT) {
            toHit.addModifier(-searchlightMod,
                    "target illuminated by searchlight");
            night_modifier -= searchlightMod;
        }
        // Certain ammunitions reduce the penalty
        else if (atype != null) {
            if (((atype.getAmmoType() == AmmoType.T_AC) 
                    || (atype.getAmmoType() == AmmoType.T_LAC))
                    && ((atype.getMunitionType() == AmmoType.M_INCENDIARY_AC) 
                            || (atype.getMunitionType() 
                                    == AmmoType.M_TRACER))) {
                toHit.addModifier(-1, "incendiary/tracer ammo");
                night_modifier--;
            }
        }
        // Laser heatsinks
        if ((night_modifier > 0) && (te != null) && (te instanceof Mech)
                && ((Mech) te).hasLaserHeatSinks()) {
            boolean lhsused = false;
            if (te.heat > 0) {
                toHit.addModifier(-night_modifier,
                        "target overheated with laser heatsinks");
                night_modifier = 0;
            }
            // actions that generate heat give a -1 modifier
            else if ((te.heatBuildup > 0) || te.isStealthActive()) {
                lhsused = true;
            } else {
                // Unfortunately, we can't just check weapons fired by the
                // target
                // because isUsedThisRound() is not valid if the attacker
                // declared first.
                // therefore, enumerate WeaponAttackActions...
                for (Enumeration<EntityAction> actions = game.getActions(); 
                        actions.hasMoreElements();) {
                    EntityAction a = actions.nextElement();
                    if (a instanceof WeaponAttackAction) {
                        WeaponAttackAction waa = (WeaponAttackAction) a;
                        if (waa.getEntityId() == te.getId()) {
                            Mounted weapon = te.getEquipment(waa
                                    .getWeaponId());
                            if ((weapon.getCurrentHeat() != 0)
                                    || weapon.isRapidfire()) {
                                // target fired a weapon that generates heat
                                lhsused = true;
                                break;
                            }
                        }
                    }
                }
            }

            if (lhsused) {
                toHit.addModifier(-1, "target uses laser heatsinks");
            }
        }


        //now check for general hit bonuses for heat
        if((te != null) 
                && !((attacker instanceof Infantry) 
                        && !(attacker instanceof BattleArmor))) {
            int heatBonus = game.getPlanetaryConditions().getLightHeatBonus(
                    te.heat);
            if(heatBonus < 0) {
                toHit.addModifier(heatBonus, "target excess heat at night");
            }
        }
        
        PilotOptions pOpts = attacker.getCrew().getOptions();
        if ((toHit.getValue() > 0) && (null != attacker.getCrew())
                && pOpts.booleanOption(OptionsConstants.UNOFF_BLIND_FIGHTER)) {
            toHit.addModifier(-1, "blind fighter");
        }

        return toHit;
    }

}
