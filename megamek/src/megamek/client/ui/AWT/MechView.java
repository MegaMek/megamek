/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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
    private boolean isVehicle;
    private boolean isProto;

    StringBuffer sBasic = new StringBuffer();
    StringBuffer sLoadout = new StringBuffer();

    public MechView(Entity entity) {
        mech = entity;
        isMech = entity instanceof Mech;
        isInf = entity instanceof Infantry;
        isVehicle = entity instanceof Tank;
        isProto = entity instanceof Protomech;

        sLoadout.append( getWeapons() )
            .append("\n")
            .append(getAmmo())
            .append("\n")
            .append(getMisc()) //has to occur before basic is processed
            .append("\n")
            .append(getFailed());

        sBasic.append( mech.getShortName() );
        sBasic.append("\n");
        if ( !isInf ) {
            sBasic.append( Math.round(mech.getWeight()) )
                .append(" tons  " );
        }
        sBasic.append(TechConstants.T_NAMES[mech.getTechLevel()]);
        sBasic.append("\n");
        if ( mech.hasC3M() || mech.hasC3S() || mech.hasC3i()) {
            sBasic.append( "Linked c3 BV: ");
            sBasic.append( mech.calculateBattleValue(true) );
        }
        sBasic.append("\n");
        sBasic.append( "Movement: " )
            .append( mech.getWalkMP() )
            .append( "/" )
            .append( mech.getRunMPasString() );
        if (mech.getJumpMP() > 0) {
            sBasic.append( "/" )
                .append( mech.getJumpMP() );
        }
        if (isVehicle) {
            sBasic.append(" (")
                .append(entity.getMovementTypeAsString())
                .append(")");
        }
        sBasic.append( "\n" );
        if ( isMech ) {
            Mech aMech = (Mech) mech;
            sBasic.append( "Engine: " )
                .append( aMech.engineRating() );
            if (aMech.hasXL()) {
                sBasic.append( " XL" );
            }
            if (aMech.hasLightEngine()) {
                sBasic.append( " Light" );
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
        if (isMech && ((Mech)mech).hasEndo()) {
            sIntArm.append(" (Endo Steel)");
        }
        sIntArm.append( "\n" );
        sIntArm.append("Armor: ")
            .append( mech.getTotalArmor() );
        if ( isMech ) {
            sIntArm.append( "/" )
                .append( maxArmor );
            if (((Mech)mech).hasFerro()) {
                sIntArm.append(" (Ferro-Fibrous)");
            }
        }
        sIntArm.append( "\n" );
        // Walk through the entity's locations.
        for ( int loc = 0; loc < mech.locations(); loc++ ) {

            // Skip empty sections.
            if ( Entity.ARMOR_NA == mech.getInternal(loc) ||
                 ( isVehicle && (( loc == Tank.LOC_TURRET &&
                                   ((Tank)mech).hasNoTurret() ) ||
                                 (loc == Tank.LOC_BODY))) ) {
                continue;
            }

            if ( mech.getLocationAbbr(loc).length() < 2 ) {
                sIntArm.append( " " );
            }
            sIntArm.append( mech.getLocationAbbr(loc) )
                .append( ": " );
            sIntArm.append( renderArmor(mech.getInternal(loc)) )
                .append("   ");
            if ( Entity.ARMOR_NA != mech.getArmor(loc) ) {
                sIntArm.append( renderArmor(mech.getArmor(loc)) );
            }
            if ( mech.hasRearArmor(loc) ) {
                sIntArm.append( " (" )
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
            WeaponType wtype = (WeaponType)mounted.getType();

            sWeapons.append( mounted.getDesc() )
                .append( "  [" )
                .append( mech.getLocationAbbr(mounted.getLocation()) )
                .append( "]" );
            if (mech.isClan() && 
                mounted.getType().getInternalName().substring(0,2).equals("IS")) {
                sWeapons.append(" (IS)");
            }
            if (!mech.isClan() &&
                mounted.getType().getInternalName().substring(0,2).equals("CL")) {
                sWeapons.append(" (Clan)");
            }
            if (wtype.hasFlag(WeaponType.F_ONESHOT)) {
                sWeapons.append(" <")
                    .append(mounted.getLinked().getDesc())
                    .append(">");
            }
            sWeapons.append(" ").append(wtype.getHeat()).append(" Heat");
            sWeapons.append("\n");
        }
        return sWeapons.toString();
    }
    
    private String getAmmo() {
        Enumeration eAmmo = mech.getAmmo();
        StringBuffer sAmmo = new StringBuffer();
        while (eAmmo.hasMoreElements()) {
            Mounted mounted = (Mounted)eAmmo.nextElement();
            if (mounted.getLocation() != Entity.LOC_NONE) {
                sAmmo.append( mounted.getDesc() )
                    .append( "  [" )
                    .append( mech.getLocationAbbr(mounted.getLocation()) )
                    .append( "]\n" );
            }
        }
        return sAmmo.toString();
    }

    private String getMisc() {
        StringBuffer sMisc = new StringBuffer();
        Enumeration eMisc = mech.getMisc();
        while (eMisc.hasMoreElements()) {
            Mounted mounted = (Mounted)eMisc.nextElement();
            if ( mounted.getDesc().indexOf("Jump Jet") != -1 ||
                 ( mounted.getDesc().indexOf("CASE") != -1 &&
                   mech.isClan() ) ||
                 mounted.getDesc().indexOf("Heat Sink") != -1  ||
                 mounted.getDesc().indexOf("Endo Steel") != -1 ||
                 mounted.getDesc().indexOf("Ferro-Fibrous") != -1) {
                // These items are displayed elsewhere, so skip them here.
                continue;
            }
            sMisc.append( mounted.getDesc() )
                .append( "  [" )
                .append( mech.getLocationAbbr(mounted.getLocation()) )
                .append( "]" );
            if (mech.isClan() && 
                mounted.getType().getInternalName().substring(0,2).equals("IS")) {
                sMisc.append(" (IS)");
            }
            if (!mech.isClan() &&
                mounted.getType().getInternalName().substring(0,2).equals("CL")) {
                sMisc.append(" (Clan)");
            }
            sMisc.append("\n");
        }

        String capacity = mech.getUnusedString();
        if ( capacity != null && capacity.length() > 0 ) {
            sMisc.append( "\nCarrying Capacity:\n" )
                .append( capacity )
                .append( "\n" );
        }
        return sMisc.toString();
    }

    private String getFailed() {
        StringBuffer sFailed = new StringBuffer();
        Enumeration eFailed = mech.getFailedEquipment();
        if (eFailed.hasMoreElements()) {
            sFailed.append("The following equipment\n slots failed to load:\n");
            while (eFailed.hasMoreElements()) {
                sFailed.append(eFailed.nextElement()).append("\n");
            }
        }
        return sFailed.toString();
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
