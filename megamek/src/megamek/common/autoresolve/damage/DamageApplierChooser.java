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
package megamek.common.autoresolve.damage;

import megamek.common.*;

/**
 * @author Luana Coppio
 */
public class DamageApplierChooser {


    /**
     * Choose the correct DamageHandler for the given entity.
     * A damage handler is a class that handles applying damage on an entity, be it a Mek, Infantry, etc.
     * It can damage internal, armor, cause criticals, kill crew, set limbs as blown-off, can even destroy the entity,
     * @param entity the entity to choose the handler for
     * @return the correct DamageHandler for the given entity
     */
    public static DamageApplier<?> choose(Entity entity) {
        return choose(entity, EntityFinalState.ANY);
    }

    /**
     * Choose the correct DamageHandler for the given entity.
     * A damage handler is a class that handles applying damage on an entity, be it a Mek, Infantry, etc.
     * It can damage internal, armor, cause criticals, kill crew, set limbs as blown-off, can even destroy the entity,
     * This one also accepts parameters to indicate if the crew must survive and if the entity must survive.
     * @param entity the entity to choose the handler for
     * @param entityFinalState if the crew must survive and/or entity must survive
     * @return the correct DamageHandler for the given entity
     */
    public static DamageApplier<?> choose(
        Entity entity, EntityFinalState entityFinalState) {
        if (entity instanceof Infantry) {
            return new InfantryDamageApplier((Infantry) entity, entityFinalState);
        } else if (entity instanceof Mek) {
            return new MekDamageApplier((Mek) entity, entityFinalState);
        } else if (entity instanceof GunEmplacement) {
            return new GunEmplacementDamageApplier((GunEmplacement) entity, entityFinalState);
        } else if (entity instanceof Tank) {
            return new TankDamageApplier((Tank) entity, entityFinalState);
        } else if (entity instanceof Aero) {
            return new AeroDamageApplier((Aero) entity, entityFinalState);
        }
        return new SimpleDamageApplier(entity, entityFinalState);
    }

    /**
     * Automatically applies damage to the entity based on the "removal condition" provided.
     * The damage is calculated as being a percentage of the total armor of the unit, then it is transformed in a roll of dices
     * which the average roll is that amount, then the total damage is calculated and applied in clusters of 5 damage. It rolls a minimum of
     * 1 dice of damage.
     * The removal condition is a code that indicates why the entity is being removed from the game.
     * It will decide if the unit or entity must survive based on the type of removal condition.
     * The removal conditions are:
     * * RETREAT: crew must survive, entity must survive, 80% of the total armor is applied as damage
     * * SALVAGEABLE: crew may die, entity must be destroyed, 75% of the total armor is applied as damage
     * * CAPTURED: crew must survive, entity must be destroyed, 33% of the total armor is applied as damage
     * * EJECTED: crew must survive, entity must be destroyed, 33% of the total armor is applied as damage
     * * DEVASTATED: crew may survive, entity must be destroyed, 500% of the total armor is applied as damage
     * * OTHER: crew may die, entity may be destroyed, 33% of the total armor applied as damage
     * The amount of damage applied present right now was decided arbitrarily and can be changed later, maybe even make it follow
     * a config file, client settings, etc.
     *
     * @param entity           the entity to choose the handler for
     * @param removalCondition the reason why the entity is being removed
     */
    public static void damageRemovedEntity(Entity entity, int removalCondition) {
        EntityFinalState finalState = EntityFinalState.fromEntityRemovalState(removalCondition);
        var numberOfDices = getNumberOfDices(entity, finalState);
        var damage = Compute.d6(numberOfDices);
        var clusterSize = -1;
        DamageApplierChooser.choose(entity, finalState)
            .applyDamageInClusters(damage, clusterSize);
    }

    private static int getNumberOfDices(Entity entity, EntityFinalState finalState) {
        var totalHealth = entity.getTotalOArmor() + entity.getTotalOInternal();
        double targetDamage = switch (finalState) {
            case ANY, CREW_MUST_SURVIVE, CREW_AND_ENTITY_MUST_SURVIVE -> totalHealth * (0.1 + Compute.randomFloat() * 0.5);
            case ENTITY_MUST_SURVIVE, DAMAGE_ONLY_THE_ENTITY -> totalHealth * (0.3 + Compute.randomFloat() * 0.45);
            case ENTITY_MUST_BE_DEVASTATED -> totalHealth * 3;
        };

        return Math.max(1, (int) (targetDamage / 6 / 0.6));
    }

}
