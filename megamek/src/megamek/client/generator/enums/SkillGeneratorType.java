/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.generator.enums;

import java.util.ResourceBundle;

import megamek.MegaMek;

public enum SkillGeneratorType {
    //region Enum Declarations
    INNER_SPHERE("SkillGeneratorType.INNER_SPHERE.text", "SkillGeneratorType.INNER_SPHERE.toolTipText"),
    CLAN("SkillGeneratorType.CLAN.text", "SkillGeneratorType.CLAN.toolTipText"),
    MANEI_DOMINI("SkillGeneratorType.MANEI_DOMINI.text", "SkillGeneratorType.MANEI_DOMINI.toolTipText"),
    SOCIETY("SkillGeneratorType.SOCIETY.text", "SkillGeneratorType.SOCIETY.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    //endregion Variable Declarations

    //region Constructors
    SkillGeneratorType(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("megamek.client.messages",
              MegaMek.getMMOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }
    //endregion Constructors

    //region Getters
    public String getToolTipText() {
        return toolTipText;
    }
    //endregion Getters

    //region Boolean Comparisons
    public boolean isInnerSphere() {
        return this == INNER_SPHERE;
    }

    public boolean isClan() {
        return this == CLAN;
    }

    public boolean isManeiDomini() {
        return this == MANEI_DOMINI;
    }

    public boolean isSociety() {
        return this == SOCIETY;
    }
    //endregion Boolean Comparisons

    @Override
    public String toString() {
        return name;
    }
}
