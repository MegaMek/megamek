/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

/*
 * Author: Reinhard Vicinus
 */

package megamek.common.verifier;

public interface TestEntityOption {
    public final static int CEIL_TARGCOMP_CRITS = 0;
    public final static int ROUND_TARGCOMP_CRITS = 1;
    public final static int FLOOR_TARGCOMP_CRITS = 2;

    public float getWeightCeilingEngine();

    public float getWeightCeilingStructure();

    public float getWeightCeilingArmor();

    public float getWeightCeilingControls();

    public float getWeightCeilingWeapons();

    public float getWeightCeilingTargComp();

    public float getWeightCeilingGyro();

    public float getWeightCeilingTurret();

    public float getWeightCeilingPowerAmp();

    public float getMaxOverweight();

    public boolean showOverweightedEntity();

    public boolean showUnderweightedEntity();

    public boolean showCorrectArmor();

    public boolean showCorrectCritical();

    public boolean showFailedEquip();

    public float getMinUnderweight();

    public boolean ignoreFailedEquip(String name);

    public boolean skip();

    public int getTargCompCrits();

    public int getPrintSize();
}
