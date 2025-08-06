/*
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.verifier;

/**
 * @author Reinhard Vicinus
 */
public interface TestEntityOption {
    public static final int CEIL_TARGCOMP_CRITS = 0;
    public static final int ROUND_TARGCOMP_CRITS = 1;
    public static final int FLOOR_TARGCOMP_CRITS = 2;

    public TestEntity.Ceil getWeightCeilingEngine();

    public TestEntity.Ceil getWeightCeilingStructure();

    public TestEntity.Ceil getWeightCeilingArmor();

    public TestEntity.Ceil getWeightCeilingControls();

    public TestEntity.Ceil getWeightCeilingWeapons();

    public TestEntity.Ceil getWeightCeilingTargComp();

    public TestEntity.Ceil getWeightCeilingGyro();

    public TestEntity.Ceil getWeightCeilingTurret();

    public TestEntity.Ceil getWeightCeilingLifting();

    public TestEntity.Ceil getWeightCeilingPowerAmp();

    public double getMaxOverweight();

    public boolean showOverweightedEntity();

    public boolean showUnderweightedEntity();

    public boolean showCorrectArmor();

    public boolean showCorrectCritical();

    public boolean showFailedEquip();

    public boolean showIncorrectIntroYear();

    public int getIntroYearMargin();

    public double getMinUnderweight();

    public boolean ignoreFailedEquip(String name);

    public boolean skip();

    public int getTargCompCrits();

    public int getPrintSize();
}
