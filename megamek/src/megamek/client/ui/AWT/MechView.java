/*
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

/*
 * MechView.java
 *
 * Created on January 20, 2003 by Ryan McConnell
 */

package megamek.client;

import java.util.Enumeration;
import java.util.Vector;

import megamek.common.*;

/**
 * A utility class for retrieving mech information in a formatted string.
 *
 */
public class MechView {

    private boolean isValid = true;
    private Mech mech;
    
    public MechView(Entity entity) {
        String objectClass = "" + entity.getClass();
        if (!(entity instanceof Mech)) {
            //Can only display mechs right now, future upgrade for vehicles?
            isValid = false;
            return;
        }
        mech = (Mech)entity;
    }

    public boolean isValid() {
        return isValid;
    }

    public String getMechReadout() {
        String sMech = new String();
        sMech = mech.getChassis() + "  " + mech.getModel() + "    " + Math.round(mech.getWeight()) + " tons    ";
        if (mech.isClan()) {
            sMech += "Clan\n";
        } else {
            sMech += "Inner Sphere\n";
        }
        sMech += "Movement: " + mech.getWalkMP() + "/" + mech.getRunMP() + "/" + mech.getJumpMP() + "     ";
        sMech += "Engine: " + mech.engineRating();
        if (mech.hasXL()) {
            sMech += " XL";
        }
        sMech += "    Heat Sinks: " + mech.heatSinks();
        if (mech.getHeatCapacity() > mech.heatSinks()) {
            sMech += " [" + mech.getHeatCapacity() + "]";
        }
        return sMech + formatArmor() + formatWeapons() + formatAmmo() + formatMisc();
    }
    
    private String formatArmor() {
        int maxArmor = mech.getTotalOInternal() * 2 + 3;
        String sArmor = "\nArmor:  " + mech.getTotalOArmor() + "/" + maxArmor + "\n";
        sArmor += "H: " + mech.getOArmor(0) + "\n";
        sArmor += "LT: " + mech.getOArmor(3) + " (" + mech.getOArmor(3,true) + ")    " + "CT: " + mech.getOArmor(1) + " (" + mech.getOArmor(1,true) + ")    " + "RT: " + mech.getOArmor(2) + " (" + mech.getOArmor(2,true) + ")\n";
        sArmor += "LA: " + mech.getOArmor(5) + "    RA: " + mech.getOArmor(4) + "\n";
        sArmor += "LL: " + mech.getOArmor(7) + "    RL: " + mech.getOArmor(6) + "\n";
        return sArmor;
    }

    private String formatWeapons() {
        String sWeapons = "\nWeapons:\n";
        Vector vWeapons = mech.getWeaponList();
        for (int j = 0; j < vWeapons.size(); j++)       {
            Mounted mounted = (Mounted)vWeapons.elementAt(j);
            sWeapons += mounted.getDesc() + "  [" + mech.getLocationAbbr(mounted.getLocation()) + "]\n";
        }
        return sWeapons;
    }
    
    private String formatAmmo() {
        Enumeration eAmmo = mech.getAmmo();
        if (!eAmmo.hasMoreElements()) {
            return "";

        }
        String sAmmo = "Ammo:\n";
        while (eAmmo.hasMoreElements()) {
            Mounted mounted = (Mounted)eAmmo.nextElement();
            sAmmo += mounted.getDesc() + "  [" + mech.getLocationAbbr(mounted.getLocation()) + "]\n";
        }
        return sAmmo;
    }

    private String formatMisc() {
        String sMisc = "";
        Enumeration eMisc = mech.getMisc();
        while (eMisc.hasMoreElements()) {
            Mounted mounted = (Mounted)eMisc.nextElement();
            if (mounted.getDesc().indexOf("Jump Jet") == -1 && (mounted.getDesc().indexOf("CASE") == -1 || !mech.isClan()) && mounted.getDesc().indexOf("Heat Sink") == -1) {

                sMisc += mounted.getDesc() + "  [" + mech.getLocationAbbr(mounted.getLocation()) + "]\n";
            }
        }
        if (sMisc != "") {
            sMisc = "\nOther (Heat Sinks, JJ, and clan CASE excluded):\n" + sMisc;
        }
        return sMisc;
    }

}
