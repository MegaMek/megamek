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
 * A single thing out there that kills a crew who ejects into it.
 *
 * <p>Kept as separate values rather than one "the conditions are lethal" answer because a crew's armor kit answers
 * some of these and not others. A MekWarrior combat suit supplies air, so it answers a tainted atmosphere, but it
 * has no gloves and never seals, so it does not answer vacuum. Telling a player their crew is safe has to be
 * decided one hazard at a time.</p>
 *
 * @see megamek.common.units.CrewArmorKitRules#unansweredBy
 */
public enum EjectionHazard {

    /** Air too thin to breathe. Only a sealed kit answers it. */
    VACUUM("PlanetaryConditions.LethalToEjectedCrew.Vacuum"),

    /** A tainted atmosphere, which needs air of its own to breathe. */
    TAINTED_AIR("PlanetaryConditions.LethalToEjectedCrew.TaintedAir"),

    /** A toxic atmosphere, the stricter form of the same problem. */
    TOXIC_AIR("PlanetaryConditions.LethalToEjectedCrew.ToxicAir"),

    /** A tornado. No armor kit answers being picked up and thrown. */
    TORNADO("PlanetaryConditions.LethalToEjectedCrew.Tornado"),

    /** A storm. No armor kit answers it either. */
    STORM("PlanetaryConditions.LethalToEjectedCrew.Storm"),

    /** Heat beyond what an unprotected body survives. */
    EXTREME_HEAT("PlanetaryConditions.LethalToEjectedCrew.ExtremeHeat"),

    /** Cold beyond what an unprotected body survives. */
    EXTREME_COLD("PlanetaryConditions.LethalToEjectedCrew.ExtremeCold");

    private final String displayNameKey;

    EjectionHazard(String displayNameKey) {
        this.displayNameKey = displayNameKey;
    }

    /**
     * @return the hazard named so it reads inside a sentence, for example "the tainted air"
     */
    public String getDisplayName() {
        return Messages.getString(displayNameKey);
    }
}
