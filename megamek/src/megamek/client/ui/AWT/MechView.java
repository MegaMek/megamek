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
            .append("\r\n") //$NON-NLS-1$
            .append(getAmmo())
            .append("\r\n") //$NON-NLS-1$
            .append(getMisc()) //has to occur before basic is processed
            .append("\r\n") //$NON-NLS-1$
            .append(getFailed());

        sBasic.append( mech.getShortName() );
        sBasic.append("\r\n"); //$NON-NLS-1$
        if ( !isInf ) {
            sBasic.append( Math.round(mech.getWeight()) )
                .append(Messages.getString("MechView.tons") ); //$NON-NLS-1$
        }
        sBasic.append(TechConstants.getLevelName(mech.getTechLevel()));
        sBasic.append("\n"); //$NON-NLS-1$
        if ( mech.hasC3M() || mech.hasC3S() || mech.hasC3i()) {
            sBasic.append( Messages.getString("MechView.Linkedc3bv")); //$NON-NLS-1$
            sBasic.append( mech.calculateBattleValue(true) );
        }
        sBasic.append("\n"); //$NON-NLS-1$
        sBasic.append( Messages.getString("MechView.Movement") ) //$NON-NLS-1$
            .append( mech.getWalkMP() )
            .append( "/" ) //$NON-NLS-1$
            .append( mech.getRunMPasString() );
        if (mech.getJumpMP() > 0) {
            sBasic.append( "/" ) //$NON-NLS-1$
                .append( mech.getJumpMP() );
        }
        if (isVehicle) {
            sBasic.append(" (") //$NON-NLS-1$
                .append(entity.getMovementModeAsString())
                .append(")"); //$NON-NLS-1$
        }
        sBasic.append( "\n" ); //$NON-NLS-1$
        if ( isMech ) {
            Mech aMech = (Mech) mech;
            sBasic.append( Messages.getString("MechView.Engine") ) //$NON-NLS-1$
                .append( aMech.engineRating() );
            if (aMech.hasXL()) {
                sBasic.append( Messages.getString("MechView.XL") ); //$NON-NLS-1$
            }
            if (aMech.hasLightEngine()) {
                sBasic.append( Messages.getString("MechView.Light") ); //$NON-NLS-1$
            }
            sBasic.append("\n"); //$NON-NLS-1$
            sBasic.append( Messages.getString("MechView.HeatSinks") ) //$NON-NLS-1$
                .append( aMech.heatSinks() );
            if (aMech.getHeatCapacity() > aMech.heatSinks()) {
                sBasic.append( " [" ) //$NON-NLS-1$
                    .append( aMech.getHeatCapacity() )
                    .append( "]" ); //$NON-NLS-1$
            }
            sBasic.append("\n"); //$NON-NLS-1$
        }
        sBasic.append("\n") //$NON-NLS-1$
            .append( getInternalAndArmor() );
    }

    public String getMechReadoutBasic() {
        return sBasic.toString();
    }

    public String getMechReadoutLoadout() {
        return sLoadout.toString();
    }

    public String getMechReadout() {
        return getMechReadoutBasic() + "\n" + getMechReadoutLoadout(); //$NON-NLS-1$
    }
    
    private String getInternalAndArmor() {
        StringBuffer sIntArm = new StringBuffer();

        int maxArmor = mech.getTotalInternal() * 2 + 3;
        sIntArm.append( Messages.getString("MechView.Internal") ) //$NON-NLS-1$
            .append( mech.getTotalInternal() );
        if (isMech && ((Mech)mech).hasEndo()) {
            sIntArm.append(Messages.getString("MechView.EndoSteel")); //$NON-NLS-1$
        }
        sIntArm.append( "\n" ); //$NON-NLS-1$
        sIntArm.append(Messages.getString("MechView.Armor")) //$NON-NLS-1$
            .append( mech.getTotalArmor() );
        if ( isMech ) {
            sIntArm.append( "/" ) //$NON-NLS-1$
                .append( maxArmor );
        }
        if (mech.getArmorType() != EquipmentType.T_ARMOR_STANDARD) {
            sIntArm.append(" (");
            sIntArm.append(EquipmentType.getArmorTypeName(mech.getArmorType()));
            sIntArm.append(")");
        }
        sIntArm.append( "\n" ); //$NON-NLS-1$
        // Walk through the entity's locations.
        for ( int loc = 0; loc < mech.locations(); loc++ ) {

            // Skip empty sections.
            if ( IArmorState.ARMOR_NA == mech.getInternal(loc) ||
                 ( isVehicle && (( loc == Tank.LOC_TURRET &&
                                   ((Tank)mech).hasNoTurret() ) ||
                                 (loc == Tank.LOC_BODY))) ) {
                continue;
            }

            if ( mech.getLocationAbbr(loc).length() < 2 ) {
                sIntArm.append( " " ); //$NON-NLS-1$
            }
            sIntArm.append( mech.getLocationAbbr(loc) )
                .append( ": " ); //$NON-NLS-1$
            sIntArm.append( renderArmor(mech.getInternal(loc)) )
                .append("   "); //$NON-NLS-1$
            if ( IArmorState.ARMOR_NA != mech.getArmor(loc) ) {
                sIntArm.append( renderArmor(mech.getArmor(loc)) );
            }
            if ( mech.hasRearArmor(loc) ) {
                sIntArm.append( " (" ) //$NON-NLS-1$
                    .append( renderArmor(mech.getArmor(loc, true)) )
                    .append( ")" ); //$NON-NLS-1$
            }
            sIntArm.append( "\n" ); //$NON-NLS-1$
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
                .append( "  [" ) //$NON-NLS-1$
                .append( mech.getLocationAbbr(mounted.getLocation()) )
                .append( "]" ); //$NON-NLS-1$
            if (mech.isClan() && 
                mounted.getType().getInternalName().substring(0,2).equals("IS")) { //$NON-NLS-1$
                sWeapons.append(Messages.getString("MechView.IS")); //$NON-NLS-1$
            }
            if (!mech.isClan() &&
                mounted.getType().getInternalName().substring(0,2).equals("CL")) { //$NON-NLS-1$
                sWeapons.append(Messages.getString("MechView.Clan")); //$NON-NLS-1$
            }
            if (wtype.hasFlag(WeaponType.F_ONESHOT)) {
                sWeapons.append(" <") //$NON-NLS-1$
                    .append(mounted.getLinked().getDesc())
                    .append(">"); //$NON-NLS-1$
            }
            sWeapons.append(" ").append(wtype.getHeat()).append(Messages.getString("MechView.Heat")); //$NON-NLS-1$ //$NON-NLS-2$
            sWeapons.append("\n"); //$NON-NLS-1$
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
                    .append( "  [" ) //$NON-NLS-1$
                    .append( mech.getLocationAbbr(mounted.getLocation()) )
                    .append( "]\n" ); //$NON-NLS-1$
            }
        }
        return sAmmo.toString();
    }

    private String getMisc() {
        StringBuffer sMisc = new StringBuffer();
        Enumeration eMisc = mech.getMisc();
        while (eMisc.hasMoreElements()) {
            Mounted mounted = (Mounted)eMisc.nextElement();
            String name = mounted.getName();
            if (name.indexOf("Jump Jet") != -1 //$NON-NLS-1$ 
                    ||(name.indexOf("CASE") != -1 && mech.isClan()) //$NON-NLS-1$
                    || name.indexOf("Heat Sink") != -1 //$NON-NLS-1$
                    || name.indexOf("Endo Steel") != -1 //$NON-NLS-1$
                    || name.indexOf("Ferro-Fibrous") != -1) { //$NON-NLS-1$
                // These items are displayed elsewhere, so skip them here.
                continue;
            }
            sMisc.append( mounted.getDesc() )
                .append( "  [" ) //$NON-NLS-1$
                .append( mech.getLocationAbbr(mounted.getLocation()) )
                .append( "]" ); //$NON-NLS-1$
            if (mech.isClan() && 
                mounted.getType().getInternalName().substring(0,2).equals("IS")) { //$NON-NLS-1$
                sMisc.append(Messages.getString("MechView.IS")); //$NON-NLS-1$
            }
            if (!mech.isClan() &&
                mounted.getType().getInternalName().substring(0,2).equals("CL")) { //$NON-NLS-1$
                sMisc.append(Messages.getString("MechView.Clan")); //$NON-NLS-1$
            }
            sMisc.append("\n"); //$NON-NLS-1$
        }

        String capacity = mech.getUnusedString();
        if ( capacity != null && capacity.length() > 0 ) {
            sMisc.append( Messages.getString("MechView.CarringCapacity") ) //$NON-NLS-1$
                .append( capacity )
                .append( "\n" ); //$NON-NLS-1$
        }
        return sMisc.toString();
    }

    private String getFailed() {
        StringBuffer sFailed = new StringBuffer();
        Enumeration eFailed = mech.getFailedEquipment();
        if (eFailed.hasMoreElements()) {
            sFailed.append("The following equipment\n slots failed to load:\n"); //$NON-NLS-1$
            while (eFailed.hasMoreElements()) {
                sFailed.append(eFailed.nextElement()).append("\n"); //$NON-NLS-1$
            }
        }
        return sFailed.toString();
    }    

    private static String renderArmor(int nArmor)
    {
        if (nArmor <= 0) {
            return "xx"; //$NON-NLS-1$
        }
        else {
            return makeLength(String.valueOf(nArmor), 2, true);
        }
    }

    private static final String SPACES = "                                   "; //$NON-NLS-1$
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
            return s.substring(0, n - 2) + ".."; //$NON-NLS-1$
        }
    }

}
