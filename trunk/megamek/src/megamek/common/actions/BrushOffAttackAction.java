/**
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

package megamek.common.actions;

import megamek.common.Targetable;

/**
 * The attacker brushes the target off.
 */
public class BrushOffAttackAction
    extends AbstractAttackAction
{
    public static final int BOTH = 0;
    public static final int LEFT = 1;
    public static final int RIGHT = 2;
    
    private int arm;
    
    public BrushOffAttackAction(int entityId, int targetId, int arm) {
        super(entityId, targetId);
        this.arm = arm;
    }
    
    public int getArm() {
        return arm;
    }
    
    public void setArm(int arm) {
        this.arm = arm;
    }
}
