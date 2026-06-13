/*
 * Copyright (c) 2000-2004 - Ben Mazur (bmazur@sev.org).
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.actions;

import java.io.Serial;
import java.util.Enumeration;

import megamek.client.Client;
import megamek.common.ToHitData;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.IlluminationLevel;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Targetable;

/**
 * Abstract superclass for any action where an entity is attacking another entity.
 */
public abstract class AbstractAttackAction extends AbstractEntityAction implements AttackAction {
    @Serial
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
     *
     * @return the entity even if it was destroyed or fled.
     */
    public @Nullable Entity getEntity(Game g) {
        return getEntity(g, getEntityId());
    }

    /**
     * Gets an entity with the given ID, using the passed-in game object.
     *
     * @return the entity even if it was destroyed or fled.
     */
    public @Nullable Entity getEntity(Game g, int entityID) {
        Entity e = g.getEntity(entityID);
        // if we have an arty attack, we might need to get an out-of-game entity if it
        // died or fled
        return (e == null) ? g.getOutOfGameEntity(entityID) : e;
    }

    /**
     * used by the toHit of derived classes ammoType may be null if not using an ammo based weapon
     *
     * @param game The current {@link Game}
     */
    public static ToHitData nightModifiers(Game game, Targetable target, AmmoType ammoType, Entity attacker,
          boolean isWeapon) {
        Entity te = (target.getTargetType() == Targetable.TYPE_ENTITY) ? (Entity) target : null;
        ToHitData toHit = new ToHitData();

        PlanetaryConditions conditions = game.getPlanetaryConditions();
        if (conditions.getLight().isDay()) {
            // It's the day, so just return
            return toHit;
        }

        // Enhanced Imaging (EI) ignores darkness modifiers per IO p.69
        if (attacker.hasActiveEiCockpit()) {
            return toHit;
        }

        // The base night penalty
        final IlluminationLevel hexIlluminationLvl = IlluminationLevel.determineIlluminationLevel(game,
              target.getBoardId(),
              target.getPosition());
        int night_modifier = conditions.getLightHitPenalty(isWeapon);
        toHit.addModifier(night_modifier, conditions.getLight().toString());

        boolean illuminated = false;
        if (te != null) {
            illuminated = te.isIlluminated();
            // hack for unresolved actions so client shows right BTH
            if (!illuminated) {
                for (Enumeration<EntityAction> actions = game.getActions(); actions.hasMoreElements(); ) {
                    EntityAction entityAction = actions.nextElement();
                    if (entityAction instanceof SearchlightAttackAction searchlightAttackAction) {
                        if (searchlightAttackAction.willIlluminate(game, te)) {
                            illuminated = true;
                            break;
                        }
                    }
                }
            }
        }
        // TO:AR 6TH ed. p.56
        // Searchlights reduce the penalty to zero (or 1 for pitch-black)
        // (except for dusk/dawn)
        int searchlightMod = Math.min(3, night_modifier);
        boolean isUsingSearchlight = (te != null) && te.isUsingSearchlight();
        boolean lighted = isUsingSearchlight || illuminated;
        if (conditions.getLight().isFullMoonOrMoonlessOrPitchBack()
              && lighted) {
            if (isUsingSearchlight) {
                toHit.addModifier(-searchlightMod, "target using searchlight");
                night_modifier = night_modifier - searchlightMod;
            } else if (illuminated) {
                toHit.addModifier(-searchlightMod, "target illuminated by searchlight");
                night_modifier = night_modifier - searchlightMod;
            }
        } else if (hexIlluminationLvl.isFlare()) {
            // Flares reduce the night modifier to zero
            toHit.addModifier(-night_modifier, "target illuminated by flare");
            night_modifier = 0;
        } else if (hexIlluminationLvl.isFire()) {
            int fireMod = Math.min(2, night_modifier);
            toHit.addModifier(-fireMod, "target illuminated by fire");
            night_modifier -= fireMod;
        } else if ((conditions.getLight().isFullMoonOrMoonlessOrPitchBack()) && (hexIlluminationLvl.isSearchlight())) {
            toHit.addModifier(-searchlightMod, "target illuminated by searchlight");
            night_modifier -= searchlightMod;
        } else if (ammoType != null) {
            // Certain ammunition reduce the penalty
            if (((ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.AC)
                  || (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.LAC)
                  || (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.AC_IMP)
                  || (ammoType.getAmmoType() == AmmoType.AmmoTypeEnum.PAC))
                  && ((ammoType.getMunitionType().contains(AmmoType.Munitions.M_INCENDIARY_AC))
                  || (ammoType.getMunitionType().contains(AmmoType.Munitions.M_TRACER)))) {
                toHit.addModifier(-1, "incendiary/tracer ammo");
                night_modifier--;
            }
        }

        // Laser heats inks
        if ((night_modifier > 0) && (te instanceof Mek) && ((Mek) te).hasLaserHeatSinks()) {
            boolean lhsUsed = false;
            if (te.heat > 0) {
                toHit.addModifier(-night_modifier, "target overheated with laser heat sinks");
            } else if ((te.heatBuildup > 0) || te.isStealthActive()) {
                // actions that generate heat give a -1 modifier
                lhsUsed = true;
            } else {
                // Unfortunately, we can't just check weapons fired by the target
                // because isUsedThisRound() is not valid if the attacker declared first.
                // therefore, enumerate WeaponAttackActions...
                for (Enumeration<EntityAction> actions = game.getActions(); actions.hasMoreElements(); ) {
                    EntityAction a = actions.nextElement();
                    if (a instanceof WeaponAttackAction weaponAttackAction) {
                        if (weaponAttackAction.getEntityId() == te.getId()) {
                            Mounted<?> weapon = te.getEquipment(weaponAttackAction.getWeaponId());
                            if ((weapon.getCurrentHeat() != 0) || weapon.isRapidFire()) {
                                // target fired a weapon that generates heat
                                lhsUsed = true;
                                break;
                            }
                        }
                    }
                }
            }

            if (lhsUsed) {
                toHit.addModifier(-1, "target uses laser heat sinks");
            }
        }

        // now check for general hit bonuses for heat
        if ((te != null) && !attacker.isConventionalInfantry()) {
            int heatBonus = conditions.getLightHeatBonus(te.heat);
            if (heatBonus < 0) {
                toHit.addModifier(heatBonus, "target excess heat at night");
            }
        }

        if ((toHit.getValue() > 0) && (null != attacker.getCrew())
              && attacker.hasAbility(OptionsConstants.UNOFFICIAL_BLIND_FIGHTER)) {
            toHit.addModifier(-1, "blind fighter");
        }

        return toHit;
    }

    @Override
    public String toAccessibilityDescription(final Client client) {
        final Targetable target = getTarget(client.getGame());
        return (target == null) ? "Attacking Null Target with id " + getTargetId()
              : "Attacking " + target.getDisplayName();
    }

    @Override
    public String toString() {
        return super.toString() + "; Target type/ID: " + targetType + "/" + targetId;
    }
}
