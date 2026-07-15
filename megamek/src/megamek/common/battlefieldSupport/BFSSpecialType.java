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
package megamek.common.battlefieldSupport;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import megamek.common.annotations.Nullable;

/**
 * The registry of Battlefield Support Asset Specials that this build recognizes. Each entry records the canonical code
 * as printed on the Asset card, whether the Special takes a parameter value, and a human-readable display name.
 * <p>
 * This registry is the single source of truth used by the editor (to offer a picker of known Specials) and by
 * rules code (to dispatch Special behavior). Specials not present here are still valid on an Asset: they are
 * preserved and printed, but have no defined behavior (see {@link BFSSpecial}).
 */
public enum BFSSpecialType {

    NO_TURRET("No Turret", false, "No Turret", "NoTurret"),
    NIMBLE("Nimble", false, "Nimble"),
    IMMOBILE("Immobile", false, "Immobile"),
    INDIRECT_FIRE("IF", true, "Indirect Fire", "IndirectFire"),
    SPOTTER("Spotter", false, "Spotter"),
    TAG("TAG", false, "Target Acquisition Gear"),
    COMMANDER("Commander", false, "Commander", "Cmdr"),
    APC("APC", true, "Armored Personnel Carrier"),
    AMS("AMS", false, "Anti-Missile System"),
    ARTILLERY("Artillery", true, "Artillery"),
    SWARM("Swarm", false, "Swarm"),
    ARROW_IV("Arrow", true, "Arrow IV", "ArrowIV"),
    ECM("ECM", true, "Electronic Countermeasures"),
    TARGETING_COMPUTER("TC", false, "Targeting Computer", "TargetingComputer"),
    ANTI_INFANTRY("AI", false, "Anti-Infantry"),
    CRIT_SEEKER("Crit-Seeker", false, "Crit-Seeker", "CritSeeker", "Cst-Seeker"),
    MASH("MASH", true, "Mobile Army Surgical Hospital"),
    MECHANIZED("Mechanized", false, "Mechanized"),
    PROBE("PRB", false, "Active Probe", "Probe"),
    REFLECTIVE_ARMOR("RFA", false, "Reflective Armor", "ReflectiveArmor");

    private static final Map<String, BFSSpecialType> LOOKUP = new HashMap<>();

    static {
        for (BFSSpecialType type : values()) {
            LOOKUP.put(normalize(type.canonicalCode), type);
            for (String alias : type.aliases) {
                LOOKUP.put(normalize(alias), type);
            }
        }
    }

    private final String canonicalCode;
    private final boolean takesValue;
    private final String displayName;
    private final String[] aliases;

    BFSSpecialType(String canonicalCode, boolean takesValue, String displayName, String... aliases) {
        this.canonicalCode = canonicalCode;
        this.takesValue = takesValue;
        this.displayName = displayName;
        this.aliases = aliases;
    }

    /** @return the canonical code as printed on the Asset card (for example {@code IF}, {@code No Turret}) */
    public String canonicalCode() {
        return canonicalCode;
    }

    /** @return true if this Special takes a parameter value (for example {@code IF X}, {@code Artillery (LT)}) */
    public boolean takesValue() {
        return takesValue;
    }

    public String displayName() {
        return displayName;
    }

    /**
     * Looks up a known Special by its code, matching the canonical code or any alias case-insensitively and ignoring
     * spaces, hyphens and dots.
     *
     * @param code the code to look up
     *
     * @return the matching registry entry, or empty if the code is not recognized
     */
    public static Optional<BFSSpecialType> forCode(@Nullable String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(LOOKUP.get(normalize(code)));
    }

    private static String normalize(String value) {
        return value.strip().toUpperCase().replaceAll("[\\s\\-.]", "");
    }
}
