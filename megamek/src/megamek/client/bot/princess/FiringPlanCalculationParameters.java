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
package megamek.client.bot.princess;

import java.util.Map;
import java.util.Objects;

import megamek.client.bot.princess.coverage.Builder;
import megamek.common.units.Entity;
import megamek.common.units.Targetable;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.WeaponMounted;
import org.apache.logging.log4j.message.ParameterizedMessage;

/**
 * This data structure contains parameters that may be passed to the "determineBestFiringPlan()"
 */
public final class FiringPlanCalculationParameters {
    // The type of firing plan calculation to carry out
    public enum FiringPlanCalculationType {
        /**
         * We're guessing the firing plan based on our estimate of enemy movement
         */
        GUESS,
        /**
         * We're getting a firing plan based on exact known enemy movement results
         */
        GET
    }

    private final Entity shooter;
    private final EntityState shooterState;
    private final Targetable target;
    private final EntityState targetState;
    private final int maxHeat;
    private final Map<WeaponMounted, Double> ammoConservation;
    private final FiringPlanCalculationType calculationType;

    // internal constructor
    public FiringPlanCalculationParameters(final Builder builder) {
        this.shooter = builder.getShooter();
        this.shooterState = builder.getShooterState();
        this.target = builder.getTarget();
        this.targetState = builder.getTargetState();
        maxHeat = Math.max(builder.getMaxHeat(), 0);
        this.ammoConservation = builder.getAmmoConservation();
        this.calculationType = builder.getCalculationType();
    }

    Entity getShooter() {
        return shooter;
    }

    @Nullable
    EntityState getShooterState() {
        return shooterState;
    }

    public Targetable getTarget() {
        return target;
    }

    @Nullable
    EntityState getTargetState() {
        return targetState;
    }

    int getMaxHeat() {
        return maxHeat;
    }

    @Nullable
    Map<WeaponMounted, Double> getAmmoConservation() {
        return ammoConservation;
    }

    FiringPlanCalculationType getCalculationType() {
        return calculationType;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof FiringPlanCalculationParameters that)) {
            return false;
        }

        if (maxHeat != that.maxHeat) {
            return false;
        }
        if (!shooter.equals(that.shooter)) {
            return false;
        }
        if (!Objects.equals(shooterState, that.shooterState)) {
            return false;
        }
        if (!target.equals(that.target)) {
            return false;
        }
        if (!Objects.equals(targetState, that.targetState)) {
            return false;
        }
        // noinspection SimplifiableIfStatement
        if (!Objects.equals(ammoConservation, that.ammoConservation)) {
            return false;
        }
        return calculationType == that.calculationType;
    }

    @Override
    public int hashCode() {
        int result = shooter.hashCode();
        result = 31 * result + (shooterState != null ? shooterState.hashCode() : 0);
        result = 31 * result + target.hashCode();
        result = 31 * result + (targetState != null ? targetState.hashCode() : 0);
        result = 31 * result + maxHeat;
        result = 31 * result + (ammoConservation != null ? ammoConservation.hashCode() : 0);
        result = 31 * result + calculationType.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return new ParameterizedMessage("""
              FiringPlanCalculationParameters{
              \tshooter = {},
              \tshooterState = {},
              \ttarget = {},
              \ttargetState = {},
              \tmaxHeat = {},
              \tammoConservation = {},
              \tcalculationType = {}
              }""",
              shooter,
              shooterState,
              target,
              targetState,
              maxHeat,
              ammoConservation,
              calculationType).getFormattedMessage();
    }
}
