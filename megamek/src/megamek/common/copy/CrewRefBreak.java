/*
 * Copyright (c) 2024-2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.common.copy;

import megamek.common.Crew;

/**
 * @author Luana Coppio
 */
public class CrewRefBreak implements RefBreak<Crew> {

    private final Crew originalCrew;

    public CrewRefBreak(Crew originalCrew) {
        this.originalCrew = originalCrew;
    }

    @Override
    public Crew copy() {
        var newCrewRef = new Crew(originalCrew.getCrewType(), originalCrew.getName(), originalCrew.getSize(),
            originalCrew.getGunnery(), originalCrew.getPiloting(), originalCrew.getGender(), originalCrew.isClanPilot(),
            originalCrew.getExtraData());

        for (int i = 0; i < originalCrew.getCrewType().getCrewSlots(); i++) {
            newCrewRef.setExternalIdAsString(originalCrew.getExternalIdAsString(i), i);
            newCrewRef.setHits(originalCrew.getHits(i), i);
            newCrewRef.setName(originalCrew.getName(i), i);
            newCrewRef.setNickname(originalCrew.getNickname(i), i);
        }
        return newCrewRef;
    }
}
