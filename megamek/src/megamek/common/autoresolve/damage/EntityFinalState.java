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


public enum EntityFinalState {
    ANY(false, false, false),
    CREW_MUST_SURVIVE(true, false, false),
    ENTITY_MUST_SURVIVE(false, true, false),
    CREW_AND_ENTITY_MUST_SURVIVE(true, true, false),
    DAMAGE_ONLY_THE_ENTITY(true, false, true);

    final boolean crewMustSurvive;
    final boolean entityMustSurvive;
    final boolean noCrewDamage;

    EntityFinalState(boolean crewMustSurvive, boolean entityMustSurvive, boolean noCrewDamage) {
        this.crewMustSurvive = crewMustSurvive;
        this.entityMustSurvive = entityMustSurvive;
        this.noCrewDamage = noCrewDamage;
    }
}
