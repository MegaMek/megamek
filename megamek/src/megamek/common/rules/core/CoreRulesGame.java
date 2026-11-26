package megamek.common.rules.core;
/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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


import static megamek.client.ui.clientGUI.calculationReport.CalculationReport.formatForReport;

import jakarta.annotation.Nullable;
import megamek.client.ui.clientGUI.calculationReport.CalculationReport;
import megamek.common.enums.GamePhase;
import megamek.common.rules.RulesGame;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

public class CoreRulesGame extends RulesGame {

    private static final MMLogger logger = MMLogger.create(CoreRulesGame.class);

    /**
     * {@inheritDoc} Ammo dumping is not in Core
     */
    @Override
    public boolean ammoDumping() {
        return false;
    }

    /** TAG surcharge per launcher with Homing ammo (Core errata v0.2 p.1). */
    public static final int HOMING_TAG_BV = 50;

    /**
     * {@inheritDoc} Immobile not eligible in movement Core p.49 RAC Unjamming does not prevent usage (only limits
     * movement) Core p.183 Finding a club can use in physical phase Core p.79
     */
    @Override
    public boolean eligibleForPhase(Entity entity,
          @Nullable GamePhase phase) {
        if (phase != null) {
            if (entity.isImmobile() && phase.isMovement()) {
                return false;
            }
            if (phase.isPhysical() && (entity.isCharging() || entity.isMakingDfa())) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc} Front loaded initiative Core p.41
     */
    @Override
    public int getInitiativeOrder(int[] num_turns,
          int index,
          int min,
          boolean frontLoadOption) {
        return ((int) Math.ceil(((double) num_turns[index]) / (double) min));
    }

    /**
     * {@inheritDoc}
     * 50 BV for every Arrow IV or artillery launcher on a friendly unit with Homing ammo, Asset Arrow IV launchers
     * included, once per TAG on the unit; half that when the unit's only TAG is rear-mounted (Core errata v0.2 p.1,
     * applied to all Homing per the rules team). No other Core munition, semi-guided LRMs included, adds a surcharge.
     * Homing bombs keep their Tactical Operations cost.
     */
    @Override
    public double tagBVBump(Entity entity,
          CalculationReport bvReport,
          double adjustedBV,
          long tagCount,
          boolean onlyRearTag) {
        double tags = tagCount * (onlyRearTag ? 0.5 : 1);
        for (String launcher : homingLaunchers(entity)) {
            adjustedBV += HOMING_TAG_BV * tags;
            bvReport.addLine("- " + launcher,
                  "+ " + formatForReport(tags) + " x " + HOMING_TAG_BV,
                  "= " + formatForReport(adjustedBV));
        }

        for (Entity otherEntity : entity.getGame().getEntitiesVector()) {
            if ((otherEntity.getOwner() == null) || otherEntity.getOwner().isEnemyOf(entity.getOwner())) {
                continue;
            }
            adjustedBV = addHomingBombBV(otherEntity, bvReport, adjustedBV, tags);
        }
        return adjustedBV;
    }

    /**
     * {@inheritDoc} Core rules allows minefields (Core p.156)
     */
    @Override
    public boolean allowMinefields(boolean toMinefields) {
        return true;
    }

    /**
     * {@inheritDoc} Core has walk on deployment by default (Core p.37)
     *
     * @return true
     */
    @Override
    public boolean isWalkOnDeployment() {
        return true;
    }

    /**
     * {@inheritDoc} Core omits immobile units from movement/initiative (Core pp.41, 49)
     *
     * @return false if it should not include them
     */
    @Override
    public boolean includeInMovement(GamePhase phase,
          Entity entity) {
        return !((phase.isMovement() || phase.isInitiative()) && entity.isImmobile());
    }

    /**
     * {@inheritDoc} This is not used by core at all.
     */
    @Override
    public void setWalkOnDeployment(final boolean walkOn) {
        logger.debug("Setting walk on deployment should never occur when running core.");
    }
}
