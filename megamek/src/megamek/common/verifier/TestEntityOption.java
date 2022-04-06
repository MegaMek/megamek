/*
 * MegaMek -
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
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
