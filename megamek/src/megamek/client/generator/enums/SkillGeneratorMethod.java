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
import megamek.client.generator.skillGenerators.AbstractSkillGenerator;
import megamek.client.generator.skillGenerators.ConstantSkillGenerator;
import megamek.client.generator.skillGenerators.ModifiedConstantSkillGenerator;
import megamek.client.generator.skillGenerators.ModifiedTotalWarfareSkillGenerator;
import megamek.client.generator.skillGenerators.TaharqaSkillGenerator;
import megamek.client.generator.skillGenerators.TotalWarfareSkillGenerator;

public enum SkillGeneratorMethod {
    //region Enum Declarations
    TOTAL_WARFARE("SkillGeneratorMethod.TOTAL_WARFARE.text", "SkillGeneratorMethod.TOTAL_WARFARE.toolTipText"),
    MODIFIED_TOTAL_WARFARE("SkillGeneratorMethod.MODIFIED_TOTAL_WARFARE.text",
          "SkillGeneratorMethod.MODIFIED_TOTAL_WARFARE.toolTipText"),
    TAHARQA("SkillGeneratorMethod.TAHARQA.text", "SkillGeneratorMethod.TAHARQA.toolTipText"),
    CONSTANT("SkillGeneratorMethod.CONSTANT.text", "SkillGeneratorMethod.CONSTANT.toolTipText"),
    MODIFIED_CONSTANT("SkillGeneratorMethod.MODIFIED_CONSTANT.text",
          "SkillGeneratorMethod.MODIFIED_CONSTANT.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    //endregion Variable Declarations

    //region Constructors
    SkillGeneratorMethod(final String name, final String toolTipText) {
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
