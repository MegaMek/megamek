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
    private boolean hasEndoSteel;
    private boolean hasFerroFibrous;
    StringBuffer sBasic = new StringBuffer();
    StringBuffer sLoadout = new StringBuffer();

    public MechView(Entity entity) {
        mech = entity;
        isMech = entity instanceof Mech;
        isInf = entity instanceof Infantry;
	hasEndoSteel = false;
	hasFerroFibrous = false;

	sLoadout.append( getWeapons() )
	    .append("\n")
	    .append(getAmmo())
	    .append("\n")
	    .append(getMisc()); //has to occur before basic is processed

        sBasic.append( mech.getShortName() );
	sBasic.append("\n");
        if ( !isInf ) {
            sBasic.append( Math.round(mech.getWeight()) )
                .append(" tons   " );
        }
        if (mech.isClan()) {
            sBasic.append( "Clan" );
        } else {
            sBasic.append( "Inner Sphere" );
        }
	sBasic.append("\n\n");
        sBasic.append( "Movement: " )
            .append( mech.getWalkMP() )
            .append( "/" )
            .append( mech.getRunMP() )
            .append( "/" )
            .append( mech.getJumpMP() )
            .append( "\n" );
        if ( isMech ) {
            Mech aMech = (Mech) mech;
            sBasic.append( "Engine: " )
                .append( aMech.engineRating() );
            if (aMech.hasXL()) {
                sBasic.append( " XL" );
            }
	    sBasic.append("\n");
            sBasic.append( "Heat Sinks: " )
                .append( aMech.heatSinks() );
            if (aMech.getHeatCapacity() > aMech.heatSinks()) {
                sBasic.append( " [" )
                    .append( aMech.getHeatCapacity() )
                    .append( "]" );
            }
	    sBasic.append("\n");
        }
        sBasic.append("\n")
	    .append( getInternalAndArmor() );
    }

    public String getMechReadoutBasic() {
        return sBasic.toString();
    }

    public String getMechReadoutLoadout() {
	return sLoadout.toString();
    }

    public String getMechReadout() {
        return getMechReadoutBasic() + "\n" + getMechReadoutLoadout();
    }
    
    private String getInternalAndArmor() {
        StringBuffer sIntArm = new StringBuffer();

        int maxArmor = mech.getTotalInternal() * 2 + 3;
        sIntArm.append( "Internal: " )
            .append( mech.getTotalInternal() );
	if (hasEndoSteel) {
	    sIntArm.append("  (Endo Steel)");
	}
	sIntArm.append( "\n" );
	sIntArm.append("Armor: ")
	    .append( mech.getTotalArmor() );
        if ( isMech ) {
            sIntArm.append( "/" )
                .append( maxArmor );
	    if (hasFerroFibrous) {
		sIntArm.append("  (Ferro-Fibrous)");
	    }
        }
	sIntArm.append( "\n" );
        sIntArm.append( " IS: " )
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
            sIntArm.append( mech.getLocationAbbr(loc) )
                .append( ":  " );
            if ( Entity.ARMOR_NA != mech.getArmor(loc) ) {
                sIntArm.append( renderArmor(mech.getArmor(loc)) )
                    .append( "  " );
            }
            sIntArm.append( renderArmor(mech.getInternal(loc)) );
            if ( mech.hasRearArmor(loc) ) {
                sIntArm.append( "  (" )
                    .append( renderArmor(mech.getArmor(loc, true)) )
                    .append( ")" );
            }
            sIntArm.append( "\n" );
        }
        return sIntArm.toString();
    }

    private String getWeapons() {
        StringBuffer sWeapons = new StringBuffer();
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
    
    private String getAmmo() {
        Enumeration eAmmo = mech.getAmmo();
        StringBuffer sAmmo = new StringBuffer();
        while (eAmmo.hasMoreElements()) {
            Mounted mounted = (Mounted)eAmmo.nextElement();
            sAmmo.append( mounted.getDesc() )
                .append( "  [" )
                .append( mech.getLocationAbbr(mounted.getLocation()) )
                .append( "]\n" );
        }
        return sAmmo.toString();
    }

    private String getMisc() {
        StringBuffer sMisc = new StringBuffer();
        Enumeration eMisc = mech.getMisc();
        while (eMisc.hasMoreElements()) {
            Mounted mounted = (Mounted)eMisc.nextElement();
            if ( mounted.getDesc().indexOf("Endo Steel") != -1 ) {
                hasEndoSteel = true;
            }
            else if ( mounted.getDesc().indexOf("Ferro-Fibrous") != -1 ) {
                hasFerroFibrous = true;
            }
            else if ( mounted.getDesc().indexOf("Jump Jet") == -1 &&
                      ( !mech.isClan() ||
                        mounted.getDesc().indexOf("CASE") == -1 ) &&
                      mounted.getDesc().indexOf("Heat Sink") == -1 ) {
                sMisc.append( mounted.getDesc() )
                    .append( "  [" )
                    .append( mech.getLocationAbbr(mounted.getLocation()) )
                    .append( "]\n" );
	    }
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
