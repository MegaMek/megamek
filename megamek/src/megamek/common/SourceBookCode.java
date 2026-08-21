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
package megamek.common;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Compile-time-safe sourcebook abbreviations used by equipment rule references.
 *
 * <p>The values match the {@code abbrev} fields of the runtime sourcebook YAML files.</p>
 */
public enum SourceBookCode {
    ATOW("AToW"),
    ATOW_COMPANION("AToW Companion"),
    CORE("Core"),
    GOTHIC("Gothic"),
    HB_HK("HB:HK"),
    HB_HL("HB:HL"),
    IO_AE("IO:AE"),
    OTP_HC("OTP:HC"),
    PS_AO_URBANFEST("PS:AO-UrbanFest"),
    SO("SO"),
    SHRAPNEL_1("Shrap01"),
    SHRAPNEL_3("Shrap03"),
    SHRAPNEL_5("Shrap05"),
    SHRAPNEL_7("Shrap07"),
    SHRAPNEL_9("Shrap09"),
    TM("TM"),
    TO_AR("TO:AR"),
    TO_AUE("TO:AUE"),
    TW("TW"),
    BMM("BMM"),
    UNOFFICIAL("Unofficial");

    private final String abbrev;

    SourceBookCode(String abbrev) {
        this.abbrev = abbrev;
    }

    /**
     * @return The exact abbreviation stored in the corresponding sourcebook YAML file.
     */
    @JsonValue
    public String getAbbrev() {
        return abbrev;
    }
}
