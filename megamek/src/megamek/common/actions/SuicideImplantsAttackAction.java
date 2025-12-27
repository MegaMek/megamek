/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import megamek.common.ToHitData;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.units.Targetable;
import megamek.logging.MMLogger;

/**
 * Explosive Suicide Implants attack action (IO pg 83).
 * <p>
 * The entity detonates their suicide implants, destroying themselves and causing damage to nearby units depending on
 * entity type:
 * <ul>
 *   <li>Conventional Infantry: 0.57 damage per trooper to all entities in hex</li>
 *   <li>Battle Armor: Destroys selected troopers only, no damage to others</li>
 *   <li>MekWarrior/Pilot: 1 point internal to Mek head (or 1 armor to fighter nose),
 *       plus critical hit roll. Cockpit destroyed for salvage.</li>
 *   <li>Vehicle Crew: Crew Killed result, 1 point internal to all facings,
 *       plus critical hit rolls.</li>
 * </ul>
 */
public class SuicideImplantsAttackAction extends AbstractAttackAction {
    private static final MMLogger LOGGER = MMLogger.create(SuicideImplantsAttackAction.class);

    @Serial
    private static final long serialVersionUID = 7293847562938475629L;

    /** Damage per conventional infantry trooper (per IO pg 83) */
    public static final double DAMAGE_PER_TROOPER = 0.57;

    /** Number of troopers/crew members detonating their implants */
    private int troopersDetonating;

    /**
     * Creates a new suicide implants attack action.
     *
     * @param entityId           the ID of the entity detonating
     * @param troopersDetonating the number of troopers/crew detonating (for infantry/BA)
     */
    public SuicideImplantsAttackAction(int entityId, int troopersDetonating) {
        super(entityId, Targetable.TYPE_ENTITY, entityId); // Target is self
        this.troopersDetonating = troopersDetonating;
    }

    /**
     * @return the number of troopers/crew members detonating their implants
     */
    public int getTroopersDetonating() {
        return troopersDetonating;
    }

    /**
     * Calculate the area-effect damage dealt by suicide implants. Only conventional infantry deals area damage to other
     * entities.
     *
     * @param trooperCount the number of troopers detonating
     *
     * @return the damage dealt (0.57 per trooper for conventional infantry)
     */
    public static int getDamageFor(int trooperCount) {
        return (int) Math.round(trooperCount * DAMAGE_PER_TROOPER);
    }

    /**
     * Returns the damage dealt to the host unit (for Mek/Aero/Vehicle scenarios). Per IO pg 83:
     * <ul>
     *   <li>Mek: 1 point internal structure to head</li>
     *   <li>Aero: 1 point armor to nose</li>
     *   <li>Vehicle: 1 point internal structure to all facings</li>
     * </ul>
     *
     * @return 1 (constant per IO pg 83)
     */
    public static int getHostDamageFor() {
        return 1;
    }

    /**
     * Calculate building CF damage for infantry detonating inside a building. Per IO pg 83: For every 2 suicide charges
     * set off inside a structure, subtract 1 point from the structure's CF.
     *
     * @param trooperCount the number of troopers detonating
     *
     * @return the CF damage to the building
     */
    public static int getBuildingDamageFor(int trooperCount) {
        return trooperCount / 2;
    }

    public ToHitData toHit(Game game) {
        return toHit(game, getEntityId());
    }

    /**
     * Determine if suicide implant detonation is possible. This is always automatic success if the entity has the
     * implants and is eligible.
     *
     * @param game       the current game
     * @param attackerId the ID of the entity attempting to detonate
     *
     * @return ToHitData with AUTOMATIC_SUCCESS if possible, IMPOSSIBLE otherwise
     */
    public static ToHitData toHit(Game game, int attackerId) {
        final Entity attackingEntity = game.getEntity(attackerId);

        // Validate attacker exists
        if (attackingEntity == null) {
            LOGGER.error("Attacker not valid");
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Attacker not valid");
        }

        // Must have Suicide Implants
        if (!attackingEntity.hasAbility(OptionsConstants.MD_SUICIDE_IMPLANTS)) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Unit lacks Explosive Suicide Implants");
        }

        // Entity must be active
        if (!attackingEntity.isActive()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Unit is not active");
        }

        // Crew must be alive and conscious
        if (attackingEntity.getCrew() == null) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Unit has no crew");
        }

        if (attackingEntity.getCrew().isDead()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Crew is dead");
        }

        if (attackingEntity.getCrew().isUnconscious()) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Crew is unconscious");
        }

        // Cannot detonate if being transported (except in buildings)
        if (attackingEntity.getTransportId() != Entity.NONE) {
            return new ToHitData(TargetRoll.IMPOSSIBLE, "Cannot detonate while being transported");
        }

        // Automatic success - no roll needed for suicide implants
        String description = getDetonationDescription(attackingEntity);
        return new ToHitData(TargetRoll.AUTOMATIC_SUCCESS, description);
    }

    /**
     * Gets a description of what will happen when the entity detonates.
     *
     * @param entity the entity that will detonate
     *
     * @return a description of the detonation effects
     */
    private static String getDetonationDescription(Entity entity) {
        if (entity.isConventionalInfantry()) {
            Infantry infantry = (Infantry) entity;
            int maxTroopers = infantry.getShootingStrength();
            int maxDamage = getDamageFor(maxTroopers);
            return "Detonate: up to " + maxDamage + " damage to all units in hex";
        } else if (entity instanceof BattleArmor) {
            return "Detonate: Destroys selected battlesuits only";
        } else if (entity instanceof Mek) {
            return "Detonate: 1 internal damage to head, critical hit, cockpit destroyed";
        } else if (entity instanceof Aero) {
            return "Detonate: 1 armor damage to nose, critical hit, cockpit destroyed";
        } else if (entity instanceof Tank) {
            return "Detonate: Crew Killed, 1 internal damage to all facings";
        }
        return "Detonate suicide implants";
    }

    /**
     * Get the maximum number of troopers that can detonate for this entity.
     *
     * @param entity the entity
     *
     * @return the maximum trooper count, or 1 for non-infantry units
     */
    public static int getMaxTroopersFor(Entity entity) {
        if (entity.isConventionalInfantry()) {
            return ((Infantry) entity).getShootingStrength();
        } else if (entity instanceof BattleArmor battleArmor) {
            return battleArmor.getShootingStrength();
        }
        // Meks, Aeros, Vehicles - fixed at 1 (the pilot/crew)
        return 1;
    }
}
