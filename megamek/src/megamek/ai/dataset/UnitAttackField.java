/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
 */
package megamek.ai.dataset;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Enum defining the fields for UnitAttackAction serialization.
 * @author Luana Coppio
 */
public enum UnitAttackField {
    ROUND("ROUND"),
    ENTITY_ID("ENTITY_ID"),
    PLAYER_ID("PLAYER_ID"),
    TYPE("TYPE"),
    ROLE("ROLE"),
    X("X"),
    Y("Y"),
    FACING("FACING"),
    TARGET_PLAYER_ID("TARGET_PLAYER_ID"),
    TARGET_ID("TARGET_ID"),
    TARGET_TYPE("TARGET_TYPE"),
    TARGET_ROLE("TARGET_ROLE"),
    TARGET_X("TARGET_X"),
    TARGET_Y("TARGET_Y"),
    TARGET_FACING("TARGET_FACING"),
    AIMING_LOC("AIMING_LOC"),
    AIMING_MODE("AIMING_MODE"),
    WEAPON_ID("WEAPON_ID"),
    AMMO_ID("AMMO_ID"),
    ATA("ATA"), // air to air
    ATG("ATG"), // air to ground
    GTG("GTG"), // ground to ground
    GTA("GTA"), // ground to air
    TO_HIT("TO_HIT"),
    TURNS_TO_HIT("TURNS_TO_HIT"),
    SPOTTER_ID("SPOTTER_ID");

    private final String headerName;

    UnitAttackField(String headerName) {
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
                     .map(UnitAttackField::getHeaderName)
                     .collect(Collectors.joining("\t"));
    }

    /**
     * Builds a partial TSV header line (joined by tabs).
     */
    public static String getPartialHeaderLine(int startsAt, int endsAt) {
        return Arrays.stream(values())
                     .skip(startsAt)
                     .limit(endsAt - startsAt)
                     .map(UnitAttackField::getHeaderName)
                     .collect(Collectors.joining("\t"));
    }
}
