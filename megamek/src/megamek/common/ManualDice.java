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

package megamek.common;

import java.util.List;
import java.util.function.Supplier;

/**
 * Thread-local prompt state for {@link ManualRandom}. Human weapon attacks set an attack summary and a resolution
 * window; nested combat dice (to-hit, hit location, cluster, criticals) are prompted with a matching label. Initiative
 * and bot attacks stay computer-generated.
 */
public final class ManualDice {
    private static final ThreadLocal<String> PURPOSE = new ThreadLocal<>();
    private static final ThreadLocal<String> ATTACK_SUMMARY = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> WEAPON_RESOLUTION = new ThreadLocal<>();

    private static final String[] SKIP_PREFIXES = {
          "java.",
          "javax.",
          "jdk.",
          "sun.",
          "megamek.common.rolls."
    };

    private static final String[] SKIP_CLASSES = {
          "megamek.common.ManualDice",
          "megamek.common.ManualRandom",
          "megamek.common.MMRandom"
    };

    private static final List<LabelRule> LABEL_RULES = List.of(
          new LabelRule("rollHitLocation", "Hit location"),
          new LabelRule("missilesHit", "Cluster hits"),
          new LabelRule("clusterHits", "Cluster hits"),
          new LabelRule("calcHits", "Cluster hits"),
          new LabelRule("throughArmor", "Through-armor critical"),
          new LabelRule("addCritical", "Critical hit"),
          new LabelRule("rollCrit", "Critical hit"),
          new LabelRule("criticalHit", "Critical hit"),
          new LabelRule("Critical", "Critical hit"),
          new LabelRule("consciousness", "Consciousness"),
          new LabelRule("rollGunnerySkill", "To-hit"),
          new LabelRule("rollWeaponToHit", "To-hit")
    );

    private ManualDice() {
    }

    public static boolean shouldPrompt() {
        if (hasText(PURPOSE.get())) {
            return true;
        }
        return Boolean.TRUE.equals(WEAPON_RESOLUTION.get());
    }

    public static String purpose() {
        if (hasText(PURPOSE.get())) {
            return PURPOSE.get();
        }
        if (!Boolean.TRUE.equals(WEAPON_RESOLUTION.get())) {
            return null;
        }
        String type = inferRollType();
        String summary = ATTACK_SUMMARY.get();
        if (hasText(summary)) {
            return type + "\n\n" + summary;
        }
        return type;
    }

    /**
     * Runs {@code action} with {@code purpose} as the exact prompt text. Nested calls keep the inner purpose.
     */
    public static <T> T withPurpose(String purpose, Supplier<T> action) {
        String previous = PURPOSE.get();
        PURPOSE.set(purpose);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                PURPOSE.remove();
            } else {
                PURPOSE.set(previous);
            }
        }
    }

    /**
     * Marks a human-controlled weapon attack so nested combat dice (location, cluster, crits) prompt with a typed
     * label plus {@code attackSummary}.
     */
    public static <T> T duringWeaponAttack(String attackSummary, Supplier<T> action) {
        String previousSummary = ATTACK_SUMMARY.get();
        Boolean previousActive = WEAPON_RESOLUTION.get();
        ATTACK_SUMMARY.set(attackSummary);
        WEAPON_RESOLUTION.set(Boolean.TRUE);
        try {
            return action.get();
        } finally {
            if (previousSummary == null) {
                ATTACK_SUMMARY.remove();
            } else {
                ATTACK_SUMMARY.set(previousSummary);
            }
            if (previousActive == null) {
                WEAPON_RESOLUTION.remove();
            } else {
                WEAPON_RESOLUTION.set(previousActive);
            }
        }
    }

    public static void popPurpose() {
        PURPOSE.remove();
        ATTACK_SUMMARY.remove();
        WEAPON_RESOLUTION.remove();
    }

    static String inferRollType() {
        for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            String className = frame.getClassName();
            String methodName = frame.getMethodName();
            if (shouldSkip(className, methodName)) {
                continue;
            }
            for (LabelRule rule : LABEL_RULES) {
                if (className.contains(rule.token()) || methodName.contains(rule.token())) {
                    return rule.label();
                }
            }
        }
        return "Weapon attack";
    }

    private static boolean shouldSkip(String className, String methodName) {
        if (className.equals("megamek.common.compute.Compute")
              && (methodName.startsWith("d6") || methodName.startsWith("rollD6") || methodName.startsWith("random"))) {
            return true;
        }
        for (String skipClass : SKIP_CLASSES) {
            if (className.equals(skipClass) || className.startsWith(skipClass + "$")) {
                return true;
            }
        }
        for (String prefix : SKIP_PREFIXES) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasText(String value) {
        return (value != null) && !value.isBlank();
    }

    private record LabelRule(String token, String label) {
    }
}
