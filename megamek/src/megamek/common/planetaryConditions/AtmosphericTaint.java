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

package megamek.common.planetaryConditions;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;

import megamek.common.Messages;

/**
 * How safe the air is to breathe, and in what way it is not, per the Tainted and Toxic Atmospheres rules
 * (TO:AR 6th printing, p. 54).
 * <p>
 * The book describes two independent things: how badly the air is fouled ({@code breathable}, {@code tainted} or
 * {@code toxic}) and what the fouling actually is ({@code caustic}, {@code radiological/poisonous} or
 * {@code flammable}). Those two combine into the seven values here rather than being stored as two separate
 * settings, because only these seven combinations exist - there is no such thing as a caustic atmosphere that is
 * nonetheless breathable, and a value pair would let one be configured.
 * <p>
 * Breathable air is the default and has no effect on play at all.
 * <p>
 * The value names match the {@code atmosphere} field recorded against every world in the planetary system data,
 * so a campaign can hand a planet's recorded air straight to a battle without translating between two
 * vocabularies. {@link Atmosphere}, which carries the air <i>pressure</i> for the same worlds, is read from that
 * data the same way.
 */
public enum AtmosphericTaint {
    /** Safe for prolonged exposure; no effect in game play. */
    BREATHABLE("BREATHABLE", "PlanetaryConditions.DisplayableName.AtmosphericTaint.Breathable", "\u25CB"),
    /** Corrosive or burning to organic tissues, at the level that requires safeguards. */
    TAINTED_CAUSTIC("TAINTED_CAUSTIC",
          "PlanetaryConditions.DisplayableName.AtmosphericTaint.TaintedCaustic",
          "\u25D1"),
    /** Unbreathable or poisoned air (including nuclear fallout), at the level that requires safeguards. */
    TAINTED_POISON("TAINTED_POISON",
          "PlanetaryConditions.DisplayableName.AtmosphericTaint.TaintedPoison",
          "\u25D1"),
    /** Air that is more conducive to starting or spreading fires, at the level that requires safeguards. */
    TAINTED_FLAME("TAINTED_FLAME",
          "PlanetaryConditions.DisplayableName.AtmosphericTaint.TaintedFlame",
          "\u25D1"),
    /** Corrosive or burning to organic tissues, at the level that only full environmental sealing can negate. */
    TOXIC_CAUSTIC("TOXIC_CAUSTIC", "PlanetaryConditions.DisplayableName.AtmosphericTaint.ToxicCaustic", "\u2620"),
    /** Unbreathable or poisoned air, at the level that only full environmental sealing can negate. */
    TOXIC_POISON("TOXIC_POISON", "PlanetaryConditions.DisplayableName.AtmosphericTaint.ToxicPoison", "\u2620"),
    /** Air that is more conducive to starting or spreading fires, at its most extreme. */
    TOXIC_FLAME("TOXIC_FLAME", "PlanetaryConditions.DisplayableName.AtmosphericTaint.ToxicFlame", "\u2620");

    private final String externalId;
    private final String name;
    private final String indicator;

    AtmosphericTaint(final String externalId, final String name, final String indicator) {
        this.externalId = externalId;
        this.name = name;
        this.indicator = indicator;
    }

    public String getIndicator() {
        return indicator;
    }

    public String getExternalId() {
        return externalId;
    }

    @Override
    public String toString() {
        return Messages.getString(name);
    }

    /** @return {@code true} if the air is safe to breathe, so none of these rules apply. */
    public boolean isBreathable() {
        return this == BREATHABLE;
    }

    /** @return {@code true} if the air is fouled to the "tainted" degree - safeguards needed, but survivable. */
    public boolean isTainted() {
        return (this == TAINTED_CAUSTIC) || (this == TAINTED_POISON) || (this == TAINTED_FLAME);
    }

    /** @return {@code true} if the air is fouled to the "toxic" degree - only full environmental sealing helps. */
    public boolean isToxic() {
        return (this == TOXIC_CAUSTIC) || (this == TOXIC_POISON) || (this == TOXIC_FLAME);
    }

    /** @return {@code true} for any atmosphere that is not breathable, whether tainted or toxic. */
    public boolean isTaintedOrToxic() {
        return !isBreathable();
    }

    /** @return {@code true} if the taint is corrosive or burning to organic tissues. */
    public boolean isCaustic() {
        return (this == TAINTED_CAUSTIC) || (this == TOXIC_CAUSTIC);
    }

    /** @return {@code true} if the taint is unbreathable air, poison or nuclear fallout. */
    public boolean isRadiological() {
        return (this == TAINTED_POISON) || (this == TOXIC_POISON);
    }

    /** @return {@code true} if the taint makes fires easier to start and spread. */
    public boolean isFlammable() {
        return (this == TAINTED_FLAME) || (this == TOXIC_FLAME);
    }

    /**
     * The change to fire ignition target numbers, TO:AR p.54: a flammable atmosphere increases the likelihood of
     * starting fires by 2 when tainted and by 4 when toxic. Ignition succeeds on a 2D6 roll at or above the target
     * number, so an easier fire is a lower target and therefore a negative modifier.
     *
     * @return the ignition modifier to apply, or {@code 0} for any atmosphere that is not flammable
     */
    public int getIgniteModifier() {
        if (this == TAINTED_FLAME) {
            return -2;
        } else if (this == TOXIC_FLAME) {
            return -4;
        }
        return 0;
    }

    /**
     * @param index the ordinal of the wanted value
     *
     * @return the {@link AtmosphericTaint} with the given ordinal
     */
    public static AtmosphericTaint getAtmosphericTaint(int index) {
        return AtmosphericTaint.values()[index];
    }

    /**
     * @param externalId the external id of the wanted value
     *
     * @return the {@link AtmosphericTaint} with the given external id, or {@link #BREATHABLE} if there is none
     */
    public static AtmosphericTaint getAtmosphericTaint(String externalId) {
        for (AtmosphericTaint condition : AtmosphericTaint.values()) {
            if (condition.getExternalId().equals(externalId)) {
                return condition;
            }
        }
        return AtmosphericTaint.BREATHABLE;
    }

    /**
     * Spellings used in older planetary system data files, kept readable so that player-customised systems written
     * before the underscored names were introduced still load.
     * <p>
     * {@code NONE} is the recorded value for a world with no atmosphere at all. That is not a taint, and the air
     * pressure recorded alongside it already says {@link Atmosphere#VACUUM}, so it maps to {@link #BREATHABLE}
     * rather than becoming an eighth value here. Were it kept, the planetary conditions could hold a standard
     * pressure and an absent atmosphere at the same time and nothing would reject the contradiction.
     */
    private static final Map<String, AtmosphericTaint> LEGACY_ALIASES = Map.of("NONE", BREATHABLE,
          "TAINTEDCAUSTIC", TAINTED_CAUSTIC,
          "TAINTEDPOISON", TAINTED_POISON,
          "TAINTEDFLAME", TAINTED_FLAME,
          "TOXICCAUSTIC", TOXIC_CAUSTIC,
          "TOXICPOISON", TOXIC_POISON,
          "TOXICFLAME", TOXIC_FLAME);

    /**
     * Reads an {@link AtmosphericTaint} from the {@code atmosphere} field of the planetary system data, accepting
     * the canonical names in any casing as well as the legacy spellings in {@link #LEGACY_ALIASES}.
     *
     * @param value the string to parse
     *
     * @return the matching {@link AtmosphericTaint}, never {@code null}
     *
     * @throws IllegalArgumentException if the value is blank or cannot be mapped
     */
    @JsonCreator
    public static AtmosphericTaint fromString(String value) {
        if ((value == null) || value.isBlank()) {
            throw new IllegalArgumentException("Atmospheric taint value must not be null or blank");
        }

        String trimmed = value.strip();

        for (AtmosphericTaint atmosphericTaint : values()) {
            if (atmosphericTaint.name().equalsIgnoreCase(trimmed)) {
                return atmosphericTaint;
            }
        }

        for (Map.Entry<String, AtmosphericTaint> alias : LEGACY_ALIASES.entrySet()) {
            if (alias.getKey().equalsIgnoreCase(trimmed)) {
                return alias.getValue();
            }
        }

        throw new IllegalArgumentException("Unknown atmospheric taint value: '" + value + "'");
    }
}
