/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.generator.enums;

import megamek.MegaMek;

import java.util.ResourceBundle;

public enum SkillGeneratorType {
    //region Enum Declarations
    INNER_SPHERE("SkillGeneratorType.INNER_SPHERE.text", "SkillGeneratorType.INNER_SPHERE.toolTipText"),
    CLAN("SkillGeneratorType.CLAN.text", "SkillGeneratorType.CLAN.toolTipText"),
    MANEI_DOMINI("SkillGeneratorType.MANEI_DOMINI.text", "SkillGeneratorType.MANEI_DOMINI.toolTipText");
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
    //endregion Boolean Comparisons

    @Override
    public String toString() {
        return name;
    }
}
