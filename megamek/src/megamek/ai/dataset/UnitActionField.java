/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
package megamek.ai.dataset;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum UnitActionField {
    PLAYER_ID("PLAYER_ID"),
    ENTITY_ID("ENTITY_ID"),
    CHASSIS("CHASSIS"),
    MODEL("MODEL"),
    FACING("FACING"),
    FROM_X("FROM_X"),
    FROM_Y("FROM_Y"),
    TO_X("TO_X"),
    TO_Y("TO_Y"),
    HEXES_MOVED("HEXES_MOVED"),
    DISTANCE("DISTANCE"),
    MP_USED("MP_USED"),
    MAX_MP("MAX_MP"),
    MP_P("MP_P"),
    HEAT_P("HEAT_P"),
    ARMOR_P("ARMOR_P"),
    INTERNAL_P("INTERNAL_P"),
    JUMPING("JUMPING"),
    PRONE("PRONE"),
    LEGAL("LEGAL"),
    STEPS("STEPS"),
    TEAM_ID("TEAM_ID"),
    CHANCE_OF_FAILURE("CHANCE_OF_FAILURE");

    private final String headerName;

    UnitActionField(String headerName) {
        this.headerName = headerName;
    }

    public String getHeaderName() {
        return headerName;
    }

    /**
     * Builds the TSV header line (joined by tabs) by iterating over all enum constants.
     */
    public static String getHeaderLine() {
        return Arrays.stream(values())
            .map(UnitActionField::getHeaderName)
            .collect(Collectors.joining("\t"));
    }
}
