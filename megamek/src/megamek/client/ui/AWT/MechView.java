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

    private Entity mech;
    private boolean isMech;
    private boolean isInf;
    
    public MechView(Entity entity) {
        mech = entity;
        isMech = entity instanceof Mech;
        isInf = entity instanceof Infantry;
    }

    public boolean isValid() {
        return true;
    }

    public String getMechReadout() {
        StringBuffer sMech = new StringBuffer( mech.getShortName() );
        if ( !isInf ) {
            sMech.append( "    " )
                .append( Math.round(mech.getWeight()) )
                .append(" tons   " );
        }
        if (mech.isClan()) {
            sMech.append( " Clan\n" );
        } else {
            sMech.append( " Inner Sphere\n" );
        }
        sMech.append( "Movement: " )
            .append( mech.getWalkMP() )
            .append( "/" )
            .append( mech.getRunMP() )
            .append( "/" )
            .append( mech.getJumpMP() )
            .append( "    " );
        if ( isMech ) {
            Mech aMech = (Mech) mech;
            sMech.append( "Engine: " )
                .append( aMech.engineRating() );
            if (aMech.hasXL()) {
                sMech.append( " XL" );
            }
            sMech.append( "   Heat Sinks: " )
                .append( aMech.heatSinks() );
            if (aMech.getHeatCapacity() > aMech.heatSinks()) {
                sMech.append( " [" )
                    .append( aMech.getHeatCapacity() )
                    .append( "]" );
            }
        }
        sMech.append( formatArmor() )
            .append( formatWeapons() )
            .append( formatAmmo() )
            .append( formatMisc() );
        return sMech.toString();
    }
    
    private String formatArmor() {
        int maxArmor = mech.getTotalInternal() * 2 + 3;
        StringBuffer sArmor = new StringBuffer( "\nArmor:  " );
        sArmor.append( mech.getTotalArmor() );
        if ( isMech ) {
            sArmor.append( "/" )
                .append( maxArmor );
        }
        sArmor.append( " IS: " )
            .append( mech.getTotalInternal() )
            .append( "\n" );
        // Walk through the entity's locations.
        for ( int loc = 0; loc < mech.locations(); loc++ ) {

            // Skip empty sections.
            if ( Entity.ARMOR_NA == mech.getInternal(loc) ||
                 (!isMech && !isInf && (( loc == Tank.LOC_TURRET &&
                                          ((Tank)mech).hasNoTurret() ) ||
                                        (loc == Tank.LOC_BODY))) ) {
                continue;
            }
            sArmor.append( mech.getLocationAbbr(loc) )
                .append( ": " );
            if ( Entity.ARMOR_NA != mech.getArmor(loc) ) {
                sArmor.append( renderArmor(mech.getArmor(loc)) )
                    .append( " " );
            }
            sArmor.append( renderArmor(mech.getInternal(loc)) );
            if ( mech.hasRearArmor(loc) ) {
                sArmor.append( " (" )
                    .append( renderArmor(mech.getArmor(loc, true)) )
                    .append( ")" );
            }
            sArmor.append( "\n" );
        }
        return sArmor.toString();
    }

    private String formatWeapons() {
        StringBuffer sWeapons = new StringBuffer( "\nWeapons:\n" );
        Vector vWeapons = mech.getWeaponList();
        for (int j = 0; j < vWeapons.size(); j++)       {
            Mounted mounted = (Mounted) vWeapons.elementAt(j);
            sWeapons.append( mounted.getDesc() )
                .append( "  [" )
                .append( mech.getLocationAbbr(mounted.getLocation()) )
                .append( "]\n" );
        }
        return sWeapons.toString();
    }
    
    private String formatAmmo() {
        Enumeration eAmmo = mech.getAmmo();
        if (!eAmmo.hasMoreElements()) {
            return "";

        }
        StringBuffer sAmmo = new StringBuffer( "Ammo:\n" );
        while (eAmmo.hasMoreElements()) {
            Mounted mounted = (Mounted)eAmmo.nextElement();
            sAmmo.append( mounted.getDesc() )
                .append( "  [" )
                .append( mech.getLocationAbbr(mounted.getLocation()) )
                .append( "]\n" );
        }
        return sAmmo.toString();
    }

    private String formatMisc() {
        StringBuffer sMisc = new StringBuffer
            ("\nOther (Heat Sinks, JJ, and clan CASE excluded):\n");
        Enumeration eMisc = mech.getMisc();
        boolean itemFound = false;
        while (eMisc.hasMoreElements()) {
            Mounted mounted = (Mounted)eMisc.nextElement();
            if ( mounted.getDesc().indexOf("Jump Jet") == -1 &&
                 ( mounted.getDesc().indexOf("CASE") == -1 ||
                   !mech.isClan() ) &&
                 mounted.getDesc().indexOf("Heat Sink") == -1 ) {

                sMisc.append( mounted.getDesc() )
                    .append( "  [" )
                    .append( mech.getLocationAbbr(mounted.getLocation()) )
                    .append( "]\n" );
                itemFound = true;
            }
        }
        if ( !itemFound ) {
            return "";
        }
        return sMisc.toString();
    }
    
    private static String renderArmor(int nArmor)
    {
        if (nArmor <= 0) {
            return "xx";
        }
        else {
            return makeLength(String.valueOf(nArmor), 2, true);
        }
    }

    private static final String SPACES = "                                   ";
    private static String makeLength(String s, int n) {
        return makeLength(s, n, false);
    }

    private static String makeLength(String s, int n, boolean bRightJustify)
    {
        int l = s.length();
        if (l == n) {
            return s;
        }
        else if (l < n) {
            if (bRightJustify) {
                return SPACES.substring(0, n - l) + s;
            }
            else {
                return s + SPACES.substring(0, n - l);
            }
        }
        else {
            return s.substring(0, n - 2) + "..";
        }
    }

}
