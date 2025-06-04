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
package megamek.ai.utility;

import megamek.common.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Utility class for calculating multiple features from entities.
 * @author Luana Coppio
 */
public class EntityFeatureUtils {

    private EntityFeatureUtils() {}

    /**
     * Get the health stats of the front armor of a Entity unit in percent. (don't count internal)
     * @param unit The Entity unit
     * @return The health stats of the front armor in percent
     */
    public static float getTargetFrontHealthStats(Entity unit) {
        return ArmorCalculator.FRONT.calculateArmorPercent(unit);
    }

    /**
     * Get the health stats of the front armor of a Entity unit.
     * @param unit The Entity unit
     * @return The health stats of the front armor
     */
    public static int getDiscreteTargetFrontHealthStats(Entity unit) {
        return ArmorCalculator.FRONT.calculateArmor(unit);
    }

    /**
     * Get the health stats of the back armor of a Entity unit in percent. (don't count internal)
     * @param unit The Entity unit
     * @return The health stats of the back armor in percent
     */
    public static float getTargetBackHealthStats(Entity unit) {
        return ArmorCalculator.BACK.calculateArmorPercent(unit);
    }

    /**
     * Get the health stats of the back armor of a Entity unit in percent. (don't count internal)
     * @param unit The Entity unit
     * @return The health stats of the back armor in percent
     */
    public static int getDiscreteTargetBackHealthStats(Entity unit) {
        return ArmorCalculator.BACK.calculateArmor(unit);
    }

    /**
     * Get the health stats of the overall armor of a Entity unit in percent. (don't count internal)
     * @param unit The Entity unit
     * @return The health stats of the overall armor in percent
     */
    public static float getTargetOverallHealthStats(Entity unit) {
        return ArmorCalculator.OVERALL.calculateArmorPercent(unit);
    }

    /**
     * Get the health stats of the overall armor of a Entity unit in percent. (don't count internal)
     * @param unit The Entity unit
     * @return The health stats of the overall armor in percent
     */
    public static int getDiscreteTargetOverallHealthStats(Entity unit) {
        return ArmorCalculator.OVERALL.calculateArmor(unit);
    }

    /**
     * Get the health stats of the left armor of a Entity unit in percent. (don't count internal)
     * @param unit The Entity unit
     * @return The health stats of the left side armor in percent
     */
    public static float getTargetLeftSideHealthStats(Entity unit) {
        return ArmorCalculator.LEFT_SIDE.calculateArmorPercent(unit);
    }

    /**
     * Get the health stats of the left armor of a Entity unit in percent. (don't count internal)
     * @param unit The Entity unit
     * @return The health stats of the left side armor in percent
     */
    public static int getDiscreteTargetLeftSideHealthStats(Entity unit) {
        return ArmorCalculator.LEFT_SIDE.calculateArmor(unit);
    }

    /**
     * Get the health stats of the right armor of a targetable unit in percent. (don't count internal)
     * @param unit The targetable unit
     * @return The health stats of the right armor in percent from 0.0 to 1.0
     */
    public static float getTargetRightSideHealthStats(Entity unit) {
        return ArmorCalculator.RIGHT_SIDE.calculateArmorPercent(unit);
    }

    /**
     * Get the health stats of the right armor of a targetable unit in percent. (don't count internal)
     * @param unit The targetable unit
     * @return The health stats of the right armor in percent from 0.0 to 1.0
     */
    public static int getDiscreteTargetRightSideHealthStats(Entity unit) {
        return ArmorCalculator.RIGHT_SIDE.calculateArmor(unit);
    }

    /**
     * Enumeration of different armor calculation strategies to calculate armor percentage
     * for different sides of a unit.
     *
     * @author Luana Coppio
     */
    private enum ArmorCalculator {
        /**
         * Calculates the front armor percentage of a unit.
         */
        FRONT {
            @Override
            protected void registerCalculators() {
                registerPercent(Aero.class, unit ->
                      unit.getArmor(Aero.LOC_NOSE) / (float) unit.getOArmor(Aero.LOC_NOSE));
                registerDiscrete(Aero.class, unit -> unit.getArmor(Aero.LOC_NOSE));

                registerPercent(Tank.class, unit ->
                      unit.getArmor(Tank.LOC_FRONT) / (float) unit.getOArmor(Tank.LOC_FRONT));
                registerDiscrete(Tank.class, unit ->
                      unit.getArmor(Tank.LOC_FRONT));

                registerPercent(SuperHeavyTank.class, unit ->
                      unit.getArmor(SuperHeavyTank.LOC_FRONT) / (float) unit.getOArmor(SuperHeavyTank.LOC_FRONT));
                registerDiscrete(SuperHeavyTank.class, unit ->
                      unit.getArmor(SuperHeavyTank.LOC_FRONT));

                registerPercent(Mek.class, unit ->
                      (unit.getArmor(Mek.LOC_CT) + unit.getArmor(Mek.LOC_LT) + unit.getArmor(Mek.LOC_RT)) /
                            (float) (unit.getOArmor(Mek.LOC_CT) + unit.getOArmor(Mek.LOC_LT) + unit.getOArmor(Mek.LOC_RT)));
                registerDiscrete(Mek.class, unit ->
                      (unit.getArmor(Mek.LOC_CT) + unit.getArmor(Mek.LOC_LT) + unit.getArmor(Mek.LOC_RT)));

                registerPercent(Jumpship.class, unit ->
                      unit.getArmor(Jumpship.LOC_NOSE) / (float) unit.getOArmor(Jumpship.LOC_NOSE));
                registerDiscrete(Jumpship.class, unit ->
                      unit.getArmor(Jumpship.LOC_NOSE));

                registerPercent(SpaceStation.class, unit ->
                      unit.getArmor(SpaceStation.LOC_NOSE) / (float) unit.getOArmor(SpaceStation.LOC_NOSE));
                registerDiscrete(SpaceStation.class, unit ->
                      unit.getArmor(SpaceStation.LOC_NOSE));

                registerPercent(Warship.class, unit ->
                      unit.getArmor(Warship.LOC_NOSE) / (float) unit.getOArmor(Warship.LOC_NOSE));
                registerDiscrete(Warship.class, unit ->
                      unit.getArmor(Warship.LOC_NOSE));

                registerPercent(GunEmplacement.class, unit -> (float) unit.getArmorRemainingPercent());
                registerDiscrete(GunEmplacement.class, Entity::getTotalArmor);

                registerPercent(Entity.class, unit -> (float) unit.getArmorRemainingPercent());
                registerDiscrete(Entity.class, Entity::getTotalArmor);

                registerPercent(Infantry.class, unit -> (float) unit.getInternalRemainingPercent());
                registerDiscrete(Infantry.class, Entity::getTotalInternal);
            }
        },

        /**
         * Calculates the back armor percentage of a unit.
         */
        BACK {
            @Override
            protected void registerCalculators() {
                registerPercent(Aero.class, unit ->
                                           unit.getArmor(Aero.LOC_AFT) / (float) unit.getOArmor(Aero.LOC_AFT));
                registerDiscrete(Aero.class, unit ->
                      unit.getArmor(Aero.LOC_AFT));

                registerPercent(Tank.class, unit ->
                                           unit.getArmor(Tank.LOC_REAR) / (float) unit.getOArmor(Tank.LOC_REAR));
                registerDiscrete(Tank.class, unit ->
                      unit.getArmor(Tank.LOC_REAR));

                registerPercent(SuperHeavyTank.class, unit ->
                                                     unit.getArmor(SuperHeavyTank.LOC_REAR) / (float) unit.getOArmor(SuperHeavyTank.LOC_REAR));
                registerDiscrete(SuperHeavyTank.class, unit ->
                      unit.getArmor(SuperHeavyTank.LOC_REAR));

                registerPercent(Mek.class, unit ->
                                          (unit.getArmor(Mek.LOC_LT, true) + unit.getArmor(Mek.LOC_CT, true) +
                                                 unit.getArmor(Mek.LOC_RT, true)) /
                                                (float) (unit.getOArmor(Mek.LOC_LT, true) + unit.getOArmor(Mek.LOC_CT, true) +
                                                               unit.getOArmor(Mek.LOC_RT, true)));
                registerDiscrete(Mek.class, unit ->
                      (unit.getArmor(Mek.LOC_LT, true) + unit.getArmor(Mek.LOC_CT, true) +
                            unit.getArmor(Mek.LOC_RT, true)));

                registerPercent(Jumpship.class, unit ->
                                               (unit.getArmor(Jumpship.LOC_AFT) + unit.getArmor(Jumpship.LOC_ARS)) /
                                                     (float) (unit.getOArmor(Jumpship.LOC_FRS) + unit.getOArmor(Jumpship.LOC_ARS)));
                registerDiscrete(Jumpship.class, unit ->
                      (unit.getArmor(Jumpship.LOC_AFT) + unit.getArmor(Jumpship.LOC_ARS)));

                registerPercent(SpaceStation.class, unit ->
                                                   (unit.getArmor(SpaceStation.LOC_FRS) + unit.getArmor(SpaceStation.LOC_ARS)) /
                                                         (float) (unit.getOArmor(SpaceStation.LOC_FRS) + unit.getOArmor(SpaceStation.LOC_ARS)));
                registerDiscrete(SpaceStation.class, unit ->
                      (unit.getArmor(SpaceStation.LOC_FRS) + unit.getArmor(SpaceStation.LOC_ARS)));

                registerPercent(Warship.class, unit ->
                                              (unit.getArmor(Warship.LOC_FRS) + unit.getArmor(Warship.LOC_ARS) +
                                                     unit.getArmor(Warship.LOC_RBS)) /
                                                    (float) (unit.getOArmor(Warship.LOC_FRS) + unit.getOArmor(Warship.LOC_ARS) +
                                                                   unit.getOArmor(Warship.LOC_RBS)));
                registerDiscrete(Warship.class, unit ->
                      (unit.getArmor(Warship.LOC_FRS) + unit.getArmor(Warship.LOC_ARS) +
                            unit.getArmor(Warship.LOC_RBS)));

                registerPercent(GunEmplacement.class, unit -> (float) unit.getArmorRemainingPercent());
                registerDiscrete(GunEmplacement.class, Entity::getTotalArmor);

                registerPercent(Entity.class, unit -> (float) unit.getArmorRemainingPercent());
                registerDiscrete(Entity.class, Entity::getTotalArmor);

                registerPercent(Infantry.class, unit -> (float) unit.getInternalRemainingPercent());
                registerDiscrete(Infantry.class, Entity::getTotalInternal);
            }
        },

        /**
         * Calculates the left side armor percentage of a unit.
         */
        LEFT_SIDE {
            @Override
            protected void registerCalculators() {
                registerPercent(Aero.class, unit ->
                                           unit.getArmor(Aero.LOC_LWING) / (float) unit.getOArmor(Aero.LOC_LWING));
                registerDiscrete(Aero.class, unit ->
                      unit.getArmor(Aero.LOC_LWING));

                registerPercent(Tank.class, unit ->
                                           unit.getArmor(Tank.LOC_LEFT) / (float) unit.getOArmor(Tank.LOC_LEFT));
                registerDiscrete(Tank.class, unit ->
                      unit.getArmor(Tank.LOC_LEFT));

                registerPercent(SuperHeavyTank.class, unit ->
                                                     (unit.getArmor(SuperHeavyTank.LOC_FRONTLEFT) + unit.getArmor(SuperHeavyTank.LOC_REARLEFT)) /
                                                           (float) (unit.getOArmor(SuperHeavyTank.LOC_FRONTLEFT) +
                                                                          unit.getOArmor(SuperHeavyTank.LOC_REARLEFT)));
                registerDiscrete(SuperHeavyTank.class, unit ->
                      (unit.getArmor(SuperHeavyTank.LOC_FRONTLEFT) + unit.getArmor(SuperHeavyTank.LOC_REARLEFT)));

                registerPercent(Mek.class, unit ->
                                          (unit.getArmor(Mek.LOC_LT) + unit.getArmor(Mek.LOC_LARM) + unit.getArmor(Mek.LOC_LLEG)) /
                                                (float) (unit.getOArmor(Mek.LOC_LT) + unit.getOArmor(Mek.LOC_LARM) +
                                                               unit.getOArmor(Mek.LOC_LLEG)));
                registerDiscrete(Mek.class, unit ->
                      (unit.getArmor(Mek.LOC_LT) + unit.getArmor(Mek.LOC_LARM) + unit.getArmor(Mek.LOC_LLEG)));

                registerPercent(Jumpship.class, unit ->
                                               (unit.getArmor(Jumpship.LOC_FLS) + unit.getArmor(Jumpship.LOC_ALS)) /
                                                     (float) (unit.getOArmor(Jumpship.LOC_FLS) + unit.getOArmor(Jumpship.LOC_ALS)));
                registerDiscrete(Jumpship.class, unit ->
                      (unit.getArmor(Jumpship.LOC_FLS) + unit.getArmor(Jumpship.LOC_ALS)));

                registerPercent(SpaceStation.class, unit ->
                                                   (unit.getArmor(SpaceStation.LOC_FLS) + unit.getArmor(SpaceStation.LOC_ALS)) /
                                                         (float) (unit.getOArmor(SpaceStation.LOC_FLS) + unit.getOArmor(SpaceStation.LOC_ALS)));
                registerDiscrete(SpaceStation.class, unit ->
                      (unit.getArmor(SpaceStation.LOC_FLS) + unit.getArmor(SpaceStation.LOC_ALS)));

                registerPercent(Warship.class, unit ->
                                              (unit.getArmor(Warship.LOC_FLS) + unit.getArmor(Warship.LOC_ALS) +
                                                     unit.getArmor(Warship.LOC_LBS)) /
                                                    (float) (unit.getOArmor(Warship.LOC_FLS) + unit.getOArmor(Warship.LOC_ALS) +
                                                                   unit.getOArmor(Warship.LOC_LBS)));
                registerDiscrete(Warship.class, unit ->
                      (unit.getArmor(Warship.LOC_FLS) + unit.getArmor(Warship.LOC_ALS) +
                            unit.getArmor(Warship.LOC_LBS)));

                registerPercent(GunEmplacement.class, unit -> (float) unit.getArmorRemainingPercent());
                registerDiscrete(GunEmplacement.class, Entity::getTotalArmor);

                registerPercent(Entity.class, unit -> (float) unit.getArmorRemainingPercent());
                registerDiscrete(Entity.class, Entity::getTotalArmor);

                registerPercent(Infantry.class, unit -> (float) unit.getInternalRemainingPercent());
                registerDiscrete(Infantry.class, Entity::getTotalInternal);
            }
        },

        /**
         * Calculates the right side armor percentage of a unit.
         */
        RIGHT_SIDE {
            @Override
            protected void registerCalculators() {
                registerPercent(Aero.class, unit ->
                                           unit.getArmor(Aero.LOC_RWING) / (float) unit.getOArmor(Aero.LOC_RWING));
                registerDiscrete(Aero.class, unit ->
                      unit.getArmor(Aero.LOC_RWING));

                registerPercent(Tank.class, unit ->
                                           unit.getArmor(Tank.LOC_RIGHT) / (float) unit.getOArmor(Tank.LOC_RIGHT));
                registerDiscrete(Tank.class, unit ->
                      unit.getArmor(Tank.LOC_RIGHT));

                registerPercent(SuperHeavyTank.class, unit ->
                                                     (unit.getArmor(SuperHeavyTank.LOC_FRONTRIGHT) + unit.getArmor(SuperHeavyTank.LOC_REARRIGHT)) /
                                                           (float) (unit.getOArmor(SuperHeavyTank.LOC_FRONTRIGHT) +
                                                                          unit.getOArmor(SuperHeavyTank.LOC_REARRIGHT)));
                registerDiscrete(SuperHeavyTank.class, unit ->
                      (unit.getArmor(SuperHeavyTank.LOC_FRONTRIGHT) + unit.getArmor(SuperHeavyTank.LOC_REARRIGHT)));

                registerPercent(Mek.class, unit ->
                                          (unit.getArmor(Mek.LOC_RT) + unit.getArmor(Mek.LOC_RARM) + unit.getArmor(Mek.LOC_RLEG)) /
                                                (float) (unit.getOArmor(Mek.LOC_RT) + unit.getOArmor(Mek.LOC_RARM) +
                                                               unit.getOArmor(Mek.LOC_RLEG)));
                registerDiscrete(Mek.class, unit ->
                      (unit.getArmor(Mek.LOC_RT) + unit.getArmor(Mek.LOC_RARM) + unit.getArmor(Mek.LOC_RLEG)));

                registerPercent(Jumpship.class, unit ->
                                               (unit.getArmor(Jumpship.LOC_FRS) + unit.getArmor(Jumpship.LOC_ARS)) /
                                                     (float) (unit.getOArmor(Jumpship.LOC_FRS) + unit.getOArmor(Jumpship.LOC_ARS)));
                registerDiscrete(Jumpship.class, unit ->
                      (unit.getArmor(Jumpship.LOC_FRS) + unit.getArmor(Jumpship.LOC_ARS)));

                registerPercent(SpaceStation.class, unit ->
                                                   (unit.getArmor(SpaceStation.LOC_FRS) + unit.getArmor(SpaceStation.LOC_ARS)) /
                                                         (float) (unit.getOArmor(SpaceStation.LOC_FRS) + unit.getOArmor(SpaceStation.LOC_ARS)));
                registerDiscrete(SpaceStation.class, unit ->
                      (unit.getArmor(SpaceStation.LOC_FRS) + unit.getArmor(SpaceStation.LOC_ARS)));

                registerPercent(Warship.class, unit ->
                                              (unit.getArmor(Warship.LOC_FRS) + unit.getArmor(Warship.LOC_ARS) +
                                                     unit.getArmor(Warship.LOC_RBS)) /
                                                    (float) (unit.getOArmor(Warship.LOC_FRS) + unit.getOArmor(Warship.LOC_ARS) +
                                                                   unit.getOArmor(Warship.LOC_RBS)));
                registerDiscrete(Warship.class, unit ->
                      (unit.getArmor(Warship.LOC_FRS) + unit.getArmor(Warship.LOC_ARS) +
                            unit.getArmor(Warship.LOC_RBS)));
                registerPercent(GunEmplacement.class, unit -> (float) unit.getArmorRemainingPercent());
                registerDiscrete(GunEmplacement.class, Entity::getTotalArmor);

                registerPercent(Entity.class, unit -> (float) unit.getArmorRemainingPercent());
                registerDiscrete(Entity.class, Entity::getTotalArmor);

                registerPercent(Infantry.class, unit -> (float) unit.getInternalRemainingPercent());
                registerDiscrete(Infantry.class, Entity::getTotalInternal);
            }
        },

        /**
         * Calculates the overall armor percentage of a unit.
         */
        OVERALL {
            @Override
            protected void registerCalculators() {
                // For overall armor, we use the default Entity method for most types
                registerPercent(GunEmplacement.class, unit -> (float) unit.getArmorRemainingPercent());
                registerDiscrete(GunEmplacement.class, Entity::getTotalArmor);

                registerPercent(Infantry.class, unit -> (float) unit.getInternalRemainingPercent());
                registerDiscrete(Infantry.class, Entity::getTotalInternal);

                registerPercent(Entity.class, unit -> (float) unit.getArmorRemainingPercent());
                registerDiscrete(Entity.class, Entity::getTotalArmor);
            }
        };

        // Maps to store the calculation logic for each entity type
        private final Map<Class<?>, Function<Entity, Float>> calculatorsPercent = new ConcurrentHashMap<>();
        private final Map<Class<?>, Function<Entity, Integer>> calculatorsDiscrete = new ConcurrentHashMap<>();

        /**
         * Initialize the enum constants by registering their calculator functions.
         */
        ArmorCalculator() {
            registerCalculators();
        }

        /**
         * Abstract method to be implemented by each enum constant to register
         * appropriate calculator functions for different entity types.
         */
        protected abstract void registerCalculators();

        /**
         * Registers a calculator function for a specific entity type.
         *
         * @param <T> the entity type
         * @param entityClass the class of the entity
         * @param calculator the function to calculate armor percentage
         */
        protected <T extends Entity> void registerPercent(Class<T> entityClass, Function<T, Float> calculator) {
            calculatorsPercent.put(entityClass, entity -> calculator.apply(entityClass.cast(entity)));
        }

        /**
         * Registers a calculator function for a specific entity type.
         *
         * @param <T> the entity type
         * @param entityClass the class of the entity
         * @param calculator the function to calculate armor discrete
         */
        protected <T extends Entity> void registerDiscrete(Class<T> entityClass, Function<T, Integer> calculator) {
            calculatorsDiscrete.put(entityClass, entity -> calculator.apply(entityClass.cast(entity)));
        }

        /**
         * Calculates the armor percentage for the given entity.
         * Uses a type hierarchy dispatch to find the most specific calculator.
         *
         * @param entity the Entity
         * @return the armor percentage as a float value
         * @throws IllegalArgumentException if no suitable calculator is found
         */
        public float calculateArmorPercent(Entity entity) {
            Class<?> entityClass = entity.getClass();

            // Look for calculator matching this class or its superclasses following hierarchy
            while (entityClass != null) {
                Function<Entity, Float> calculator = calculatorsPercent.get(entityClass);
                if (calculator != null) {
                    return calculator.apply(entity);
                }
                entityClass = entityClass.getSuperclass();
            }

            // If no specific calculator found, use the Entity calculator
            Function<Entity, Float> defaultCalculator = calculatorsPercent.get(Entity.class);
            if (defaultCalculator != null) {
                return defaultCalculator.apply(entity);
            }

            throw new IllegalArgumentException("No calculator found for: " + entity.getClass());
        }

        public int calculateArmor(Entity entity) {
            Class<?> entityClass = entity.getClass();

            // Look for calculator matching this class or its superclasses following hierarchy
            while (entityClass != null) {
                Function<Entity, Integer> calculator = calculatorsDiscrete.get(entityClass);
                if (calculator != null) {
                    return calculator.apply(entity);
                }
                entityClass = entityClass.getSuperclass();
            }

            // If no specific calculator found, use the Entity calculator
            Function<Entity, Integer> defaultCalculator = calculatorsDiscrete.get(Entity.class);
            if (defaultCalculator != null) {
                return defaultCalculator.apply(entity);
            }

            throw new IllegalArgumentException("No calculator found for: " + entity.getClass());
        }
    }
}
