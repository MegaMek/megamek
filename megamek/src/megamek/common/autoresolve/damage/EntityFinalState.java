/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common.autoresolve.damage;


import megamek.common.IEntityRemovalConditions;

public enum EntityFinalState {
    ANY(false, false, false, false),
    CREW_MUST_SURVIVE(true, false, false, false),
    ENTITY_MUST_SURVIVE(false, true, false, false),
    CREW_AND_ENTITY_MUST_SURVIVE(true, true, false, false),
    DAMAGE_ONLY_THE_ENTITY(true, false, true, false),
    ENTITY_MUST_BE_DEVASTATED(false, false, false, true);

    final boolean crewMustSurvive;
    final boolean entityMustSurvive;
    final boolean noCrewDamage;
    final boolean entityMystBeDevastated;

    EntityFinalState(boolean crewMustSurvive, boolean entityMustSurvive, boolean noCrewDamage, boolean entityMystBeDevastated) {
        this.crewMustSurvive = crewMustSurvive;
        this.entityMustSurvive = entityMustSurvive;
        this.noCrewDamage = noCrewDamage;
        this.entityMystBeDevastated  = entityMystBeDevastated;
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
