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
package megamek.common.enums;

import megamek.logging.MMLogger;

/**
 * Represents prosthetic limb enhancement types for conventional infantry. These enhancements add bonus damage to normal
 * weapon attacks at range 0. See Interstellar Operations p.84 for rules.
 *
 * @author MegaMek Team
 * @since 0.50.07
 */
public enum ProstheticEnhancementType {
    // Ranged Weapon enhancements (no to-hit modifier)
    LASER("Laser", EnhancementCategory.RANGED, 0.11, 0.24, false, false, 0, 0),
    BALLISTIC("Ballistic", EnhancementCategory.RANGED, 0.01, 0.02, false, false, 0, 0),
    NEEDLER("Needler", EnhancementCategory.RANGED, 0.04, 0.08, false, false, 0, 0),
    SHOTGUN("Shotgun", EnhancementCategory.RANGED, 0.05, 0.08, false, false, 0, 0),
    SONIC_STUNNER("Sonic Stunner", EnhancementCategory.RANGED, 0.05, 0.10, false, true, 0, 0),
    SMG("SMG", EnhancementCategory.RANGED, 0.05, 0.10, false, false, 0, 0),

    // Melee Weapon enhancements (+2 to-hit modifier)
    BLADE("Blade", EnhancementCategory.MELEE, 0.02, 0.02, true, true, 2, 0),
    SHOCKER("Shocker", EnhancementCategory.MELEE, 0.04, 0.04, true, true, 2, 0),
    VIBROBLADE("Vibroblade", EnhancementCategory.MELEE, 0.14, 0.14, true, false, 2, 0),
    RUMAL_GARROTE("Rumal/Garrote", EnhancementCategory.MELEE, 0.14, 0.14, true, true, 2, 0),

    // Non-Weapon enhancements (anti-Mek bonuses)
    // Note: bvPerTrooper and getAntiMekBvMultiplier() are reserved for future BV calculation implementation.
    // Per IO p.84, Grappler and Climbing Claws provide 1.2x multiplier on Anti-Mek Battle Rating instead of flat BV.
    GRAPPLER("Grappler", EnhancementCategory.NON_WEAPON, 0.0, 0.0, false, false, 0, -2),
    CLIMBING_CLAWS("Climbing Claws", EnhancementCategory.NON_WEAPON, 0.02, 0.02, true, true, 0, -1);

    /**
     * Categories for prosthetic enhancements as defined in IO p.84.
     */
    public enum EnhancementCategory {
        RANGED("Ranged Weapons"),
        MELEE("Melee Weapons"),
        NON_WEAPON("Non-Weapons");

        private final String displayName;

        EnhancementCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static final MMLogger LOGGER = MMLogger.create(ProstheticEnhancementType.class);

    private final String displayName;
    private final EnhancementCategory category;
    private final double damagePerTrooper;
    private final double bvPerTrooper;
    private final boolean melee;
    private final boolean conventionalInfantryOnly;
    private final int toHitModifier;
    private final int antiMekModifier;

    /**
     * Creates a prosthetic enhancement type.
     *
     * @param displayName              Display name for the enhancement
     * @param category                 The enhancement category (Ranged, Melee, or Non-Weapon)
     * @param damagePerTrooper         Bonus damage per trooper (multiplied by enhancement count)
     * @param bvPerTrooper             BV modifier per trooper (multiplied by enhancement count)
     * @param melee                    True if this is a melee enhancement (applies +2 to-hit from rules)
     * @param conventionalInfantryOnly True if damage only applies vs conventional infantry
     * @param toHitModifier            To-hit modifier when using this enhancement (typically +2 for melee)
     * @param antiMekModifier          Modifier to anti-Mek (swarm/leg) attack rolls
     */
    ProstheticEnhancementType(String displayName, EnhancementCategory category, double damagePerTrooper,
          double bvPerTrooper, boolean melee, boolean conventionalInfantryOnly, int toHitModifier,
          int antiMekModifier) {
        this.displayName = displayName;
        this.category = category;
        this.damagePerTrooper = damagePerTrooper;
        this.bvPerTrooper = bvPerTrooper;
        this.melee = melee;
        this.conventionalInfantryOnly = conventionalInfantryOnly;
        this.toHitModifier = toHitModifier;
        this.antiMekModifier = antiMekModifier;
    }

    /**
     * @return The display name for this enhancement type
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @return The category for this enhancement (Ranged, Melee, or Non-Weapon)
     */
    public EnhancementCategory getCategory() {
        return category;
    }

    /**
     * @return Bonus damage per trooper per enhancement (multiply by count for total)
     */
    public double getDamagePerTrooper() {
        return damagePerTrooper;
    }

    /**
     * @return BV modifier per trooper per enhancement (multiply by count for total)
     */
    public double getBvPerTrooper() {
        return bvPerTrooper;
    }

    /**
     * @return True if this is a melee enhancement
     */
    public boolean isMelee() {
        return melee;
    }

    /**
     * @return True if damage from this enhancement only affects conventional infantry
     */
    public boolean isConventionalInfantryOnly() {
        return conventionalInfantryOnly;
    }

    /**
     * @return To-hit modifier when using this enhancement (typically +2 for melee types)
     */
    public int getToHitModifier() {
        return toHitModifier;
    }

    /**
     * @return Modifier to anti-Mek (swarm/leg) attack rolls (-2 for Grappler, -1 for Climbing Claws)
     */
    public int getAntiMekModifier() {
        return antiMekModifier;
    }

    /**
     * @return True if this enhancement provides a bonus to anti-Mek attacks
     */
    public boolean hasAntiMekBonus() {
        return antiMekModifier != 0;
    }

    /**
     * @return The Anti-Mek BV multiplier (1.2 for Grappler/Climbing Claws, 1.0 otherwise)
     */
    public double getAntiMekBvMultiplier() {
        return hasAntiMekBonus() ? 1.2 : 1.0;
    }

    /**
     * @return True if this enhancement provides bonus damage
     */
    public boolean hasDamageBonus() {
        return damagePerTrooper > 0;
    }

    /**
     * Parses a string into a ProstheticEnhancementType.
     *
     * @param text The string to parse (enum name or display name)
     *
     * @return The matching enhancement type, or null if not found
     */
    public static ProstheticEnhancementType parseFromString(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        // Try enum name first
        try {
            return valueOf(text.toUpperCase().replace("/", "_").replace(" ", "_"));
        } catch (IllegalArgumentException ignored) {
            // Fall through to display name check
        }

        // Try display name match
        for (ProstheticEnhancementType type : values()) {
            if (type.displayName.equalsIgnoreCase(text)) {
                return type;
            }
        }

        LOGGER.warn("Unable to parse '{}' into a ProstheticEnhancementType", text);
        return null;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
