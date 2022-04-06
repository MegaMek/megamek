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
import megamek.client.generator.skillGenerators.*;
import megamek.common.util.EncodeControl;

import java.util.ResourceBundle;

public enum SkillGeneratorMethod {
    //region Enum Declarations
    TOTAL_WARFARE("SkillGeneratorMethod.TOTAL_WARFARE.text", "SkillGeneratorMethod.TOTAL_WARFARE.toolTipText"),
    MODIFIED_TOTAL_WARFARE("SkillGeneratorMethod.MODIFIED_TOTAL_WARFARE.text", "SkillGeneratorMethod.MODIFIED_TOTAL_WARFARE.toolTipText"),
    TAHARQA("SkillGeneratorMethod.TAHARQA.text", "SkillGeneratorMethod.TAHARQA.toolTipText"),
    CONSTANT("SkillGeneratorMethod.CONSTANT.text", "SkillGeneratorMethod.CONSTANT.toolTipText"),
    MODIFIED_CONSTANT("SkillGeneratorMethod.MODIFIED_CONSTANT.text", "SkillGeneratorMethod.MODIFIED_CONSTANT.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    //endregion Variable Declarations

    //region Constructors
    SkillGeneratorMethod(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("megamek.client.messages",
                MegaMek.getMMOptions().getLocale(), new EncodeControl());
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
    public boolean isTotalWarfare() {
        return this == TOTAL_WARFARE;
    }

    public boolean isModifiedTotalWarfare() {
        return this == MODIFIED_TOTAL_WARFARE;
    }

    public boolean isTaharqa() {
        return this == TAHARQA;
    }

    public boolean isConstant() {
        return this == CONSTANT;
    }

    public boolean isModifiedConstant() {
        return this == MODIFIED_CONSTANT;
    }
    //endregion Boolean Comparisons

    public AbstractSkillGenerator getGenerator() {
        switch (this) {
            case TOTAL_WARFARE:
                return new TotalWarfareSkillGenerator();
            case TAHARQA:
                return new TaharqaSkillGenerator();
            case CONSTANT:
                return new ConstantSkillGenerator();
            case MODIFIED_CONSTANT:
                return new ModifiedConstantSkillGenerator();
            case MODIFIED_TOTAL_WARFARE:
            default:
                return new ModifiedTotalWarfareSkillGenerator();
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
