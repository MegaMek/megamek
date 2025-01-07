/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
        return choose(entity, entityFinalState.crewMustSurvive, entityFinalState.entityMustSurvive);
    }

    /**
     * Choose the correct DamageHandler for the given entity.
     * A damage handler is a class that handles applying damage on an entity, be it a Mek, Infantry, etc.
     * It can damage internal, armor, cause critical, kill crew, set limbs as blown-off, can even destroy the entity,
     * This one also accepts parameters to indicate if the crew must survive and if the entity must survive.
     * @param entity the entity to choose the handler for
     * @param crewMustSurvive if the crew must survive
     * @param entityMustSurvive if the entity must survive
     * @return the correct DamageHandler for the given entity
     */
    public static DamageApplier<?> choose(
        Entity entity, boolean crewMustSurvive, boolean entityMustSurvive) {

        if (entity instanceof Infantry) {
            return new InfantryDamageApplier((Infantry) entity);
        } else if (entity instanceof Mek) {
            return new MekDamageApplier((Mek) entity, crewMustSurvive, entityMustSurvive);
        } else if (entity instanceof GunEmplacement) {
            return new GunEmplacementDamageApplier((GunEmplacement) entity, crewMustSurvive, entityMustSurvive);
        } else if (entity instanceof Aero) {
            return new AeroDamageApplier((Aero) entity, crewMustSurvive, entityMustSurvive);
        }
        return new SimpleDamageApplier(entity, crewMustSurvive, entityMustSurvive);
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
        var numberOfDices = getNumberOfDices(entity, removalCondition);
        var damage = Compute.d6(numberOfDices);
        var clusterSize = 5;

        var retreating = removalCondition == IEntityRemovalConditions.REMOVE_IN_RETREAT;
        var captured = removalCondition == IEntityRemovalConditions.REMOVE_CAPTURED;
        var ejected = removalCondition == IEntityRemovalConditions.REMOVE_EJECTED;
        var devastated = removalCondition == IEntityRemovalConditions.REMOVE_DEVASTATED;
        var salvageable = removalCondition == IEntityRemovalConditions.REMOVE_SALVAGEABLE;

        var crewMustSurvive = retreating || captured || ejected;
        var entityMustSurvive = !devastated && !salvageable && !ejected;

        DamageApplierChooser.choose(entity, crewMustSurvive, entityMustSurvive)
            .applyDamageInClusters(damage, clusterSize);
    }

    private static int getNumberOfDices(Entity entity, int removalCondition) {
        var totalHealth = entity.getTotalOArmor() + entity.getTotalOInternal();
        double targetDamage = switch (removalCondition) {
            case IEntityRemovalConditions.REMOVE_CAPTURED, IEntityRemovalConditions.REMOVE_EJECTED -> totalHealth * 0.8;
            case IEntityRemovalConditions.REMOVE_DEVASTATED -> totalHealth * 3;
            case IEntityRemovalConditions.REMOVE_IN_RETREAT -> totalHealth * 0.6;
            case IEntityRemovalConditions.REMOVE_SALVAGEABLE -> totalHealth * 0.75 ;
            default -> totalHealth * 0.33;
        };

        return Math.max(1, (int) (targetDamage / 6 / 0.6));
    }

}
