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

package megamek.common;

import java.util.Vector;

public abstract class Fighter extends AeroDyne {
    public static int COCKPIT_STANDARD = 0;

    // locations
    public static final int        LOC_BODY               = 0;
    public static final int        LOC_FRONT              = 1;
    public static final int        LOC_RIGHT              = 2;
    public static final int        LOC_LEFT               = 3;
    public static final int        LOC_REAR               = 4;

    public int getEngineCritHeat() {
        //FIXME
        return 5;
    }

    public int calculateBattleValue() {
        //FIXME
        return calculateBattleValue(false, false);
    }
  
    public int calculateBattleValue(boolean assumeLinkedC3, boolean ignoreC3) {
        //FIXME
        return 0;
    }

    public Vector victoryReport() {
        //FIXME!!!
        Vector vDesc = new Vector();

        Report r = new Report(7025);
        r.type = Report.PUBLIC;
        r.addDesc(this);
        vDesc.addElement(r);

        r = new Report(7030);
        r.type = Report.PUBLIC;
        r.newlines = 0;
        vDesc.addElement(r);
        vDesc.addAll(crew.getDescVector(false));
        r = new Report(7070, Report.PUBLIC);
        r.add(getKillNumber());
        vDesc.addElement(r);
        
        if(isDestroyed()) {
            Entity killer = game.getEntity(killerId);
            if(killer == null) {
                killer = game.getOutOfGameEntity(killerId);
            }
            if(killer != null) {
                r = new Report(7072, Report.PUBLIC);
                r.addDesc(killer);
            } else {
                r = new Report(7073, Report.PUBLIC);
            }
            vDesc.addElement(r);
        }
        r.newlines = 2;

        return vDesc;
    }

    public PilotingRollData addEntityBonuses(PilotingRollData roll) {
        //FIXME
        return roll;
    }

    public int clipSecondaryFacing(int dir) {return -1;}

    //FIXME
    public int getHeatCapacityWithWater() {return 0;}

    //FIXME
    public int getHeatCapacity() {return 0;}

    //FIXME
    public void autoSetInternal() {}

    //FIXME
    public int getRunMPwithoutMASC(boolean gravity) {return 0;}

    //FIXME
    public static String getCockpitTypeString(int inType) {return null;}

    public void setEngine(Engine e) {
        engine = e;
        //FIXME
        if (e.engineValid) {
            //setOriginalWalkMP(calculateWalk());
        }
    }
}
