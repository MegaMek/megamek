/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot.princess.coverage;

import static megamek.client.bot.princess.FiringPlanCalculationParameters.FiringPlanCalculationType.GET;
import static megamek.client.bot.princess.FiringPlanCalculationParameters.FiringPlanCalculationType.GUESS;

import java.util.HashMap;
import java.util.Map;

import megamek.client.bot.princess.EntityState;
import megamek.client.bot.princess.FiringPlanCalculationParameters;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.WeaponMounted;
import megamek.logging.MMLogger;

public class Builder {
    private final static MMLogger LOGGER = MMLogger.create(Builder.class);

    private Entity shooter = null;
    private EntityState shooterState = null;
    private Targetable target = null;
    private EntityState targetState = null;
    private int maxHeat = Entity.DOES_NOT_TRACK_HEAT;
    private Map<WeaponMounted, Double> ammoConservation = new HashMap<>();
    private FiringPlanCalculationParameters.FiringPlanCalculationType calculationType = GUESS;

    /**
     * The unit doing the shooting.
     */
    public Builder setShooter(final Entity value) {
        if (null == value) {
            throw new NullPointerException("Must have a shooter.");
        }
        shooter = value;
        return this;
    }

    /**
     * the current state of the shooting unit.
     */
    public Builder setShooterState(@Nullable final EntityState value) {
        shooterState = value;
        return this;
    }

    /**
     * The unit being shot at.
     */
    public Builder setTarget(final Targetable value) {
        if (null == value) {
            throw new NullPointerException("Must have a target.");
        }
        target = value;
        return this;
    }

    /**
     * The current state of the target unit.
     */
    public Builder setTargetState(@Nullable final EntityState value) {
        targetState = value;
        return this;
    }

    /**
     * How much heat we're willing to tolerate. Defaults to {@link Entity#DOES_NOT_TRACK_HEAT}
     */
    public Builder setMaxHeat(final int value) {
        if (value < 0) {
            LOGGER.warn("Invalid max heat: {}", value);
            maxHeat = 0;
            return this;
        }

        maxHeat = value;
        return this;
    }

    /**
     * Ammo conservation biases of the unit's mounted weapons. Defaults to an empty map.
     */
    public Builder setAmmoConservation(@Nullable final Map<WeaponMounted, Double> value) {
        ammoConservation = value;
        return this;
    }

    /**
     * Are we guessing or not? Defaults to {@link FiringPlanCalculationParameters.FiringPlanCalculationType#GUESS}
     */
    public Builder setCalculationType(final FiringPlanCalculationParameters.FiringPlanCalculationType value) {
        if (null == value) {
            throw new NullPointerException("Must have a calculation type.");
        }
        calculationType = value;
        return this;
    }

    /**
     * Builds the new {@link FiringPlanCalculationParameters} based on the builder properties.
     */
    public FiringPlanCalculationParameters build() {
        return new FiringPlanCalculationParameters(this);
    }

    public FiringPlanCalculationParameters buildGuess(final Entity shooter,
          @Nullable final EntityState shooterState,
          final Targetable target,
          @Nullable final EntityState targetState, final int maxHeat,
          @Nullable final Map<WeaponMounted, Double> ammoConservation) {
        return setShooter(shooter).setShooterState(shooterState)
              .setTarget(target)
              .setTargetState(targetState)
              .setMaxHeat(maxHeat)
              .setAmmoConservation(ammoConservation)
              .setCalculationType(FiringPlanCalculationParameters.FiringPlanCalculationType.GUESS)
              .build();
    }

    public FiringPlanCalculationParameters buildExact(final Entity shooter, final Targetable target,
          final Map<WeaponMounted, Double> ammoConservation) {
        return setShooter(shooter).setTarget(target)
              .setAmmoConservation(ammoConservation)
              .setCalculationType(GET)
              .build();
    }

    public Entity getShooter() {
        return shooter;
    }

    public EntityState getShooterState() {
        return shooterState;
    }

    public Targetable getTarget() {
        return target;
    }

    public EntityState getTargetState() {
        return targetState;
    }

    public int getMaxHeat() {
        return maxHeat;
    }

    public Map<WeaponMounted, Double> getAmmoConservation() {
        return ammoConservation;
    }

    public FiringPlanCalculationParameters.FiringPlanCalculationType getCalculationType() {
        return calculationType;
    }
}
