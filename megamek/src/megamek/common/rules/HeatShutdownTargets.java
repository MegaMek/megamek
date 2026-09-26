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
package megamek.common.rules;

import megamek.client.ui.Messages;
import megamek.common.rolls.TargetRoll;

/**
 * The target numbers for heat shutdown: the roll to avoid shutting down, and the roll to restart a unit that did.
 *
 * <p>Both start from the heat scale's Avoid number: 4+ at 14 heat, rising by 2 every 4 heat, to 12+ at 30 on the
 * standard scale and on to 20+ at 46 on the Expanded Heat Scale (TO:AR p.102). The standard scale is the same in
 * both rulesets: Total Warfare (TW p.102) and the Core Rules (Core p.103-104).</p>
 *
 * <p>The optional Avoiding Shutdown rule (TO:AR p.102) turns only the roll to <em>avoid</em> shutdown into a
 * Piloting Skill Roll: the Avoid number with a -5 modifier, then a modifier for the pilot's skill. It is its own
 * optional rule, used with or without the expanded scale and with either ruleset, and never applies to restarting,
 * which is always a plain 2D6 roll against the Avoid number (TW p.102, Core p.105).</p>
 */
public final class HeatShutdownTargets {

    /** The lowest heat at which a unit has to roll to avoid shutdown; below it a shut down unit restarts on its own. */
    public static final int FIRST_SHUTDOWN_HEAT = 14;

    /** The Avoiding Shutdown rule's standard modifier to the Avoid number (TO:AR p.102). */
    static final int AVOIDING_SHUTDOWN_MODIFIER = -5;

    private HeatShutdownTargets() {}

    /**
     * Returns the heat scale's Avoid number for shutdown at the given heat: 4 at 14 heat, then 2 more for every 4 heat
     * above it. The same steps continue up the Expanded Heat Scale (TO:AR p.102).
     *
     * @param heat the unit's heat, at least {@link #FIRST_SHUTDOWN_HEAT}
     *
     * @return the Avoid number
     */
    public static int avoidNumber(int heat) {
        return 4 + (((heat - FIRST_SHUTDOWN_HEAT) / 4) * 2);
    }

    /**
     * Returns the target number to avoid shutting down.
     *
     * @param heat                 the unit's heat, at least {@link #FIRST_SHUTDOWN_HEAT}
     * @param pilotingSkill        the pilot's Piloting skill, used only under the Avoiding Shutdown rule
     * @param hotDogModifier       how much the Hot Dog ability lowers the roll, or 0 without it
     * @param usesAvoidingShutdown {@code true} if the Avoiding Shutdown rule (TO:AR p.102) is in play
     *
     * @return the target number, with each modifier named
     */
    public static TargetRoll shutdownAvoidance(int heat, int pilotingSkill, int hotDogModifier,
          boolean usesAvoidingShutdown) {
        TargetRoll target = new TargetRoll(avoidNumber(heat),
              Messages.getString("HeatShutdown.avoidNumber", heat));
        if (usesAvoidingShutdown) {
            target.addModifier(AVOIDING_SHUTDOWN_MODIFIER, Messages.getString("HeatShutdown.avoidingShutdown"));
            int skillModifier = pilotSkillModifier(pilotingSkill);
            if (skillModifier != 0) {
                target.addModifier(skillModifier, Messages.getString("HeatShutdown.pilotSkill"));
            }
        }
        addHotDog(target, hotDogModifier);
        return target;
    }

    /**
     * Returns the target number to restart a unit shut down by heat: the Avoid number alone, since the Avoiding
     * Shutdown rule covers only avoiding a shutdown (TW p.102, Core p.105, TO:AR p.102).
     *
     * @param heat           the unit's heat, at least {@link #FIRST_SHUTDOWN_HEAT}
     * @param hotDogModifier how much the Hot Dog ability lowers the roll, or 0 without it
     *
     * @return the target number, with each modifier named
     */
    public static TargetRoll restart(int heat, int hotDogModifier) {
        TargetRoll target = new TargetRoll(avoidNumber(heat), Messages.getString("HeatShutdown.avoidNumber", heat));
        addHotDog(target, hotDogModifier);
        return target;
    }

    /**
     * Returns the Avoid Shutdown Pilot Skill Rating Modifier (TO:AR p.102): -2 for skill 0-1, -1 for 2-3, 0 for 4-5
     * and +1 for 6-7.
     *
     * @param pilotingSkill the pilot's Piloting skill
     *
     * @return the modifier
     */
    static int pilotSkillModifier(int pilotingSkill) {
        if (pilotingSkill <= 1) {
            return -2;
        }
        if (pilotingSkill <= 3) {
            return -1;
        }
        if (pilotingSkill <= 5) {
            return 0;
        }
        return 1;
    }

    private static void addHotDog(TargetRoll target, int hotDogModifier) {
        if (hotDogModifier != 0) {
            target.addModifier(-hotDogModifier, Messages.getString("HeatShutdown.hotDog"));
        }
    }
}
