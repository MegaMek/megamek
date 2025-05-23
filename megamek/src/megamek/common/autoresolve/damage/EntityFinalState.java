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
package megamek.common.autoresolve.damage;


import megamek.common.IEntityRemovalConditions;

public enum EntityFinalState {
    ANY(false, false, false, false),
    CREW_MUST_SURVIVE(true, false, false, false),
    ENTITY_MUST_SURVIVE(false, true, false, false),
    CREW_AND_ENTITY_MUST_SURVIVE(true, true, false, false),
    DAMAGE_ONLY_THE_ENTITY(false, false, true, false),
    ENTITY_MUST_BE_DEVASTATED(false, false, false, true);

    final boolean crewMustSurvive;
    final boolean entityMustSurvive;
    final boolean noCrewDamage;
    final boolean entityMustBeDevastated;

    EntityFinalState(boolean crewMustSurvive, boolean entityMustSurvive, boolean noCrewDamage, boolean entityMustBeDevastated) {
        this.crewMustSurvive = crewMustSurvive;
        this.entityMustSurvive = entityMustSurvive;
        this.noCrewDamage = noCrewDamage;
        this.entityMustBeDevastated = entityMustBeDevastated;
    }

    public static EntityFinalState fromEntityRemovalState(int removalState) {
        return switch (removalState) {
            case IEntityRemovalConditions.REMOVE_SALVAGEABLE,
                 IEntityRemovalConditions.REMOVE_CAPTURED -> ENTITY_MUST_SURVIVE;
            case IEntityRemovalConditions.REMOVE_EJECTED -> CREW_MUST_SURVIVE;
            case IEntityRemovalConditions.REMOVE_DEVASTATED -> ENTITY_MUST_BE_DEVASTATED;
            case IEntityRemovalConditions.REMOVE_IN_RETREAT,
                 IEntityRemovalConditions.REMOVE_NEVER_JOINED,
                 IEntityRemovalConditions.REMOVE_UNKNOWN -> CREW_AND_ENTITY_MUST_SURVIVE;
            case IEntityRemovalConditions.REMOVE_PUSHED -> DAMAGE_ONLY_THE_ENTITY;
            default -> ANY;
        };
    }
}
