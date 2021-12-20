/*
 * MegaMek
 * Copyright (c) 2000-2011 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.bot.princess;

import megamek.common.*;
import org.apache.logging.log4j.LogManager;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Method for handling morale with Princess.
 *
 * @author Deric Page <deric dot page at gmail dot com>
 * @since 5/13/14 8:36 AM
 */
public class MoraleUtil {
    private static final DecimalFormat DEC_FORMAT = new DecimalFormat("0.00");

    private final Set<Integer> BROKEN_UNITS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public MoraleUtil() {

    }

    /**
     * If a unit's morale is broken, this method should return TRUE.
     *
     * @param unitId The ID of the {@link Entity} being checked (from {@link Entity#getId()}).
     * @return TRUE if the unit is broken.
     */
    public boolean isUnitBroken(int unitId) {
        return BROKEN_UNITS.contains(unitId);
    }

    /**
     * Triggers the morale check for all units controlled by the Princess bot player.
     *
     * @param forcedWithdrawal Set TRUE if Forced Withdrawal is in effect.
     * @param bravery          The index of the bravery setting in {@link BehaviorSettings}.
     * @param selfPreservation The index of the selfPreservation setting in {@link BehaviorSettings}.
     * @param player           The {@link Player} of the Princess bot.
     * @param game             The game being played.
     */
    public void checkMorale(boolean forcedWithdrawal, int bravery, int selfPreservation,
                            Player player, Game game) {
        StringBuilder logMsg = new StringBuilder("Starting morale checks for ").append(player.getName());

        try {
            // These mods don't vary by unit.
            int bvMod = calcBvRatioMod(player, game, logMsg);
            logMsg.append(" (").append(bvMod >= 0 ? "+" : "").append(bvMod).append(")");

            int braveryMod = calcBehaviorMod(bravery);
            logMsg.append("\n\tBravery ").append(bravery).append(" (").append(braveryMod >= 0 ? "+" : "")
                  .append(braveryMod).append(")");

            int selfPreservationMod = -calcBehaviorMod(selfPreservation);
            logMsg.append("\n\tSelf Preservation ").append(selfPreservation).append(" (")
                  .append(selfPreservationMod >= 0 ? "+" : "").append(selfPreservationMod).append(")");

            // Loop through all the units controlled by this player.
            for (Entity unit : game.getPlayerEntities(player, true)) {

                logMsg.append("\n\tUnit ").append(unit.getDisplayName());

                // If the unit is already off board, it doesn't really matter.
                if (unit.isOffBoard() || (unit.getPosition() == null)) {
                    logMsg.append("\n\t\tIs off board; skipping.");
                    continue;
                }

                // If this unit is already broken, we need to check to see if it will rally.
                int unitId = unit.getId();
                boolean rally = BROKEN_UNITS.contains(unitId);
                logMsg.append("\n\t\tNeeds to rally: ").append(rally);

                // Base target number is 2 for a regular check or 6 for a rally check.
                int targetNumber = rally ? 6 : 2;
                logMsg.append("\n\t\tBase Target Number = ").append(targetNumber);

                // If the unit is crippled and forced withdrawal is in effect, the unit will automatically break.
                targetNumber += calcDamageMod(unit, forcedWithdrawal, logMsg);
                if (targetNumber >= 12) {
                    addBrokenUnit(unitId);
                    continue;
                }

                // Get the other unit-specific mods.
                targetNumber += calcExperienceMod(unit, logMsg);

                // Add in the general mods.
                targetNumber += braveryMod;
                targetNumber += selfPreservationMod;
                targetNumber += bvMod;
                logMsg.append("\n\t\tFinal Target Number = ").append(targetNumber);

                // If the target number is 12+ or 2-, there's no point in rolling.
                if (targetNumber >= 12) {
                    addBrokenUnit(unitId);
                    continue;
                }

                if (targetNumber <= 2) {
                    if (rally) {
                        removeBrokenUnit(unitId);
                    }
                    continue;
                }

                // Roll the morale check.
                int roll = rollDice();
                logMsg.append("\n\t\tRolled ").append(roll);
                if (roll < targetNumber) {
                    addBrokenUnit(unitId);
                } else if (rally) {
                    removeBrokenUnit(unitId);
                }
            }
        } finally {
            LogManager.getLogger().info(logMsg.toString());
        }
    }

    /**
     * @param unitId The ID of the {@link Entity} to be added to the broken units list.
     */
    protected void addBrokenUnit(int unitId) {
        BROKEN_UNITS.add(unitId);
    }

    /**
     * @param unitId The ID of the {@link Entity} to be removed from the broken units list.
     */
    protected void removeBrokenUnit(int unitId) {
        BROKEN_UNITS.remove(unitId);
    }

    /**
     * @return The result of a 2d6 roll from {@link Compute#d6(int)}
     */
    protected int rollDice() {
        return Compute.d6(2);
    }

    private int calcBvRatioMod(Player player, Game game, StringBuilder logMsg) {
        int friendlyBv = 0;
        int enemyBv = 0;

        // Loop through every entity in the game.
        List<Entity> allEntities = game.getEntitiesVector();
        for (Entity entity : allEntities) {

            // Ignore units not on the board.
            if ((entity.getPosition() == null) || entity.isOffBoard()) {
                continue;
            }

            // If this is an enemy unit add it's BV to the enemy BV total, otherwise add it to the friendly BV total
            // so long as it's not broken and still on the board.
            if (entity.getOwner().isEnemyOf(player)) {
                enemyBv += entity.calculateBattleValue();
            } else if (!BROKEN_UNITS.contains(entity.getId())) {
                friendlyBv += entity.calculateBattleValue();
            }
        }

        // The target number mod is based on the friendly : enemy BV ratio.
        float ratio = (float) friendlyBv / enemyBv;
        logMsg.append("\n\tBV Ratio = ").append(friendlyBv).append(" / ").append(enemyBv).append(" = ")
              .append(DEC_FORMAT.format(ratio));

        if (ratio >= 3.0) {
            return -4;
        } else if (ratio >= 2.5) {
            return -3;
        } else if (ratio >= 2.0) {
            return -2;
        } else if (ratio >= 1.5) {
            return -1;
        } else if (ratio >= 0.67) {
            return 0;
        } else if (ratio > 0.5) {
            return 1;
        } else if (ratio > 0.4) {
            return 2;
        } else if (ratio > 0.33) {
            return 3;
        } else {
            return 4;
        }
    }

    private int calcBehaviorMod(int behavior) {
        if (behavior == 0) {
            return 3;
        } else if (behavior == 1) {
            return 2;
        } else if (behavior <= 3) {
            return 1;
        } else if (behavior <= 6) {
            return 0;
        } else if (behavior <= 8) {
            return -1;
        } else if (behavior == 9) {
            return -2;
        } else {
            return -3;
        }
    }

    private int calcDamageMod(Entity unit, boolean forcedWithdrawal, StringBuilder logMsg) {
        // Crippled units automatically withdraw if Forced Withdrawal is in effect.
        if (unit.isCrippled() && forcedWithdrawal) {
            logMsg.append("\n\t\tCrippled and forced to withdraw.");
            return 12;
        }

        int dmgLevel = unit.getDamageLevel();
        logMsg.append("\n\t\tDamage Level ").append(dmgLevel).append(" (+").append(dmgLevel).append(")");
        return dmgLevel;
    }

    // More experienced pilots are less likely to break.
    private int calcExperienceMod(Entity unit, StringBuilder logMsg) {
        final float greenThreshold = 5.5f;
        final float regularThreshold = 4.0f;
        final float veteranThreshold = 2.5f;

        float skillAverage;
        if ((unit instanceof Infantry) || (unit instanceof Protomech)) {
            skillAverage = unit.getCrew().getGunnery();
        } else {
            skillAverage = (unit.getCrew().getGunnery() + unit.getCrew().getPiloting()) / 2f;
        }

        if (skillAverage >= greenThreshold) {
            logMsg.append("\n\t\tGreen unit (+0)");
            return 0;
        } else if (skillAverage >= regularThreshold) {
            logMsg.append("\n\t\tRegular unit (-1)");
            return -1;
        } else if (skillAverage >= veteranThreshold) {
            logMsg.append("\n\t\tVeteran unit (-2)");
            return -2;
        } else {
            logMsg.append("\n\t\tElite unit (-3)");
            return -3;
        }
    }
}
