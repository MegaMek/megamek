/*
 * Copyright (c) 2000-2004 - Ben Mazur (bmazur@sev.org).
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.actions;

import megamek.client.Client;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.enums.IlluminationLevel;
import megamek.common.options.OptionsConstants;

import java.util.Enumeration;

/**
 * Abstract superclass for any action where an entity is attacking another entity.
 */
public abstract class AbstractAttackAction extends AbstractEntityAction implements AttackAction {
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

    @Override
    public int getTargetType() {
        return targetType;
    }

    @Override
    public int getTargetId() {
        return targetId;
    }

    @Override
    public void setTargetType(int targetType) {
        this.targetType = targetType;
    }

    @Override
    public void setTargetId(int targetId) {
        this.targetId = targetId;
    }

    public @Nullable Targetable getTarget(final Game game) {
        return game.getTarget(getTargetType(), getTargetId());
    }

    /**
     * Gets the entity associated with this attack action, using the passed-in game object.
     * @return the entity even if it was destroyed or fled.
     */
    public @Nullable Entity getEntity(Game g) {
        return getEntity(g, getEntityId());
    }
    
    /**
     * Gets an entity with the given ID, using the passed-in game object.
     * @return the entity even if it was destroyed or fled.
     */
    public @Nullable Entity getEntity(Game g, int entityID) {
        Entity e = g.getEntity(entityID);
        // if we have an artyattack, we might need to get an out-of-game entity if it died or fled
        return (e == null) ? g.getOutOfGameEntity(entityID) : e;
    }

    /**
     * used by the toHit of derived classes atype may be null if not using an
     * ammo based weapon
     *
     * @param game The current {@link Game}
     */
    public static ToHitData nightModifiers(Game game, Targetable target, AmmoType atype,
                                           Entity attacker, boolean isWeapon) {
        Entity te = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target : null;
        ToHitData toHit = new ToHitData();

        int lightCond = game.getPlanetaryConditions().getLight();
        if (lightCond == PlanetaryConditions.L_DAY) {
            // It's the day, so just return
            return toHit;
        }

        // The base night penalty
        final IlluminationLevel hexIllumLvl = IlluminationLevel.determineIlluminationLevel(game,
                target.getPosition());
        int night_modifier = game.getPlanetaryConditions().getLightHitPenalty(isWeapon);
        toHit.addModifier(night_modifier, game.getPlanetaryConditions().getLightDisplayableName());

        boolean illuminated = false;
        if (te != null) {
            illuminated = te.isIlluminated();
            // hack for unresolved actions so client shows right BTH
            if (!illuminated) {
                for (Enumeration<EntityAction> actions = game.getActions(); actions.hasMoreElements();) {
                    EntityAction a = actions.nextElement();
                    if (a instanceof SearchlightAttackAction) {
                        SearchlightAttackAction saa = (SearchlightAttackAction) a;
                        if (saa.willIlluminate(game, te)) {
                            illuminated = true;
                            break;
                        }
                    }
                }
            }
        }
        // Searchlights reduce the penalty to zero (or 1 for pitch-black) 
        // (except for dusk/dawn)
        int searchlightMod = Math.min(3, night_modifier);
        if ((te != null) && (lightCond > PlanetaryConditions.L_DUSK)
                && (te.isUsingSearchlight() || illuminated)) {
            if (te.isUsingSearchlight()) {
                toHit.addModifier(-searchlightMod, "target using searchlight");
                night_modifier = night_modifier - searchlightMod;
            } else if (illuminated) {
                toHit.addModifier(-searchlightMod, "target illuminated by searchlight");
                night_modifier = night_modifier - searchlightMod;
            }
        } else if (hexIllumLvl.isFlare()) {
            // Flares reduce the night modifier to zero
            toHit.addModifier(-night_modifier, "target illuminated by flare");
            night_modifier = 0;
        } else if (hexIllumLvl.isFire()) {
            int fireMod = Math.min(2, night_modifier);
            toHit.addModifier(-fireMod, "target illuminated by fire");
            night_modifier -= fireMod;
        } else if (hexIllumLvl.isSearchlight()) {
            toHit.addModifier(-searchlightMod, "target illuminated by searchlight");
            night_modifier -= searchlightMod;
        } else if (atype != null) {
            // Certain ammunitions reduce the penalty
            if (((atype.getAmmoType() == AmmoType.T_AC) 
                    || (atype.getAmmoType() == AmmoType.T_LAC)
                    || (atype.getAmmoType() == AmmoType.T_AC_IMP)
                    || (atype.getAmmoType() == AmmoType.T_PAC))
                    && ((atype.getMunitionType() == AmmoType.M_INCENDIARY_AC) 
                            || (atype.getMunitionType() == AmmoType.M_TRACER))) {
                toHit.addModifier(-1, "incendiary/tracer ammo");
                night_modifier--;
            }
        }

        // Laser heatsinks
        if ((night_modifier > 0) && (te instanceof Mech) && ((Mech) te).hasLaserHeatSinks()) {
            boolean lhsused = false;
            if (te.heat > 0) {
                toHit.addModifier(-night_modifier, "target overheated with laser heatsinks");
            } else if ((te.heatBuildup > 0) || te.isStealthActive()) {
                // actions that generate heat give a -1 modifier
                lhsused = true;
            } else {
                // Unfortunately, we can't just check weapons fired by the target
                // because isUsedThisRound() is not valid if the attacker declared first.
                // therefore, enumerate WeaponAttackActions...
                for (Enumeration<EntityAction> actions = game.getActions(); actions.hasMoreElements();) {
                    EntityAction a = actions.nextElement();
                    if (a instanceof WeaponAttackAction) {
                        WeaponAttackAction waa = (WeaponAttackAction) a;
                        if (waa.getEntityId() == te.getId()) {
                            Mounted weapon = te.getEquipment(waa.getWeaponId());
                            if ((weapon.getCurrentHeat() != 0) || weapon.isRapidfire()) {
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

        // now check for general hit bonuses for heat
        if ((te != null) && !attacker.isConventionalInfantry()) {
            int heatBonus = game.getPlanetaryConditions().getLightHeatBonus(te.heat);
            if (heatBonus < 0) {
                toHit.addModifier(heatBonus, "target excess heat at night");
            }
        }

        if ((toHit.getValue() > 0) && (null != attacker.getCrew())
                && attacker.hasAbility(OptionsConstants.UNOFF_BLIND_FIGHTER)) {
            toHit.addModifier(-1, "blind fighter");
        }

        return toHit;
    }

    @Override
    public String toDisplayableString(final Client client) {
        final Targetable target = getTarget(client.getGame());
        return (target == null) ? "Attacking Null Target with id " + getTargetId()
                : "Attacking " + target.getDisplayName();
    }
}
