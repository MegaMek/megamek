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
 */
public enum AtmosphericTaint {
    /** Safe for prolonged exposure; no effect in game play. */
    BREATHABLE("BREATHABLE", "PlanetaryConditions.DisplayableName.AtmosphericTaint.Breathable", "\u25CB"),
    /** Corrosive or burning to organic tissues, at the level that requires safeguards. */
    CAUSTIC_TAINTED("CAUSTIC_TAINTED",
          "PlanetaryConditions.DisplayableName.AtmosphericTaint.CausticTainted",
          "\u25D1"),
    /** Corrosive or burning to organic tissues, at the level that only full environmental sealing can negate. */
    CAUSTIC_TOXIC("CAUSTIC_TOXIC", "PlanetaryConditions.DisplayableName.AtmosphericTaint.CausticToxic", "\u2620"),
    /** Unbreathable or poisoned air (including nuclear fallout), at the level that requires safeguards. */
    RADIOLOGICAL_TAINTED("RADIOLOGICAL_TAINTED",
          "PlanetaryConditions.DisplayableName.AtmosphericTaint.RadiologicalTainted",
          "\u25D1"),
    /** Unbreathable or poisoned air, at the level that only full environmental sealing can negate. */
    RADIOLOGICAL_TOXIC("RADIOLOGICAL_TOXIC",
          "PlanetaryConditions.DisplayableName.AtmosphericTaint.RadiologicalToxic",
          "\u2620"),
    /** Air that is more conducive to starting or spreading fires, at the level that requires safeguards. */
    FLAMMABLE_TAINTED("FLAMMABLE_TAINTED",
          "PlanetaryConditions.DisplayableName.AtmosphericTaint.FlammableTainted",
          "\u25D1"),
    /** Air that is more conducive to starting or spreading fires, at its most extreme. */
    FLAMMABLE_TOXIC("FLAMMABLE_TOXIC",
          "PlanetaryConditions.DisplayableName.AtmosphericTaint.FlammableToxic",
          "\u2620");

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
        return (this == CAUSTIC_TAINTED) || (this == RADIOLOGICAL_TAINTED) || (this == FLAMMABLE_TAINTED);
    }

    /** @return {@code true} if the air is fouled to the "toxic" degree - only full environmental sealing helps. */
    public boolean isToxic() {
        return (this == CAUSTIC_TOXIC) || (this == RADIOLOGICAL_TOXIC) || (this == FLAMMABLE_TOXIC);
    }

    /** @return {@code true} for any atmosphere that is not breathable, whether tainted or toxic. */
    public boolean isTaintedOrToxic() {
        return !isBreathable();
    }

    /** @return {@code true} if the taint is corrosive or burning to organic tissues. */
    public boolean isCaustic() {
        return (this == CAUSTIC_TAINTED) || (this == CAUSTIC_TOXIC);
    }

    /** @return {@code true} if the taint is unbreathable air, poison or nuclear fallout. */
    public boolean isRadiological() {
        return (this == RADIOLOGICAL_TAINTED) || (this == RADIOLOGICAL_TOXIC);
    }

    /** @return {@code true} if the taint makes fires easier to start and spread. */
    public boolean isFlammable() {
        return (this == FLAMMABLE_TAINTED) || (this == FLAMMABLE_TOXIC);
    }

    /**
     * The change to fire ignition target numbers, TO:AR p.54: a flammable atmosphere increases the likelihood of
     * starting fires by 2 when tainted and by 4 when toxic. Ignition succeeds on a 2D6 roll at or above the target
     * number, so an easier fire is a lower target and therefore a negative modifier.
     *
     * @return the ignition modifier to apply, or {@code 0} for any atmosphere that is not flammable
     */
    public int getIgniteModifier() {
        if (this == FLAMMABLE_TAINTED) {
            return -2;
        } else if (this == FLAMMABLE_TOXIC) {
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
}
