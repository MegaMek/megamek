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

package megamek.client.ui.swing;

import java.util.Iterator;

import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.GunEmplacement;
import megamek.common.IArmorState;
import megamek.common.Infantry;
import megamek.common.LargeSupportTank;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.Protomech;
import megamek.common.SupportTank;
import megamek.common.SupportVTOL;
import megamek.common.Tank;
import megamek.common.TechConstants;
import megamek.common.WeaponType;

/**
 * A utility class for retrieving mech information in a formatted string.
 */
public class MechView {

    private Entity entity;
    private boolean isMech;
    private boolean isInf;
    private boolean isVehicle;
    private boolean isProto;
    private boolean isGunEmplacement;
    private boolean isSupportVehicle;
    private boolean isLargeSupportVehicle;
    private boolean isSupportVTOL;

    StringBuffer sBasic = new StringBuffer();
    StringBuffer sLoadout = new StringBuffer();
    StringBuffer sFluff = new StringBuffer("");

    public MechView(Entity entity) {
        this.entity = entity;
        isMech = entity instanceof Mech;
        isInf = entity instanceof Infantry;
        isVehicle = entity instanceof Tank;
        isProto = entity instanceof Protomech;
        isGunEmplacement = entity instanceof GunEmplacement;
        isSupportVehicle = entity instanceof SupportTank;
        isLargeSupportVehicle = entity instanceof LargeSupportTank;
        isSupportVTOL = entity instanceof SupportVTOL;

        sLoadout.append(getWeapons()).append("\r\n")//$NON-NLS-1$
                .append(getAmmo()).append("\r\n")//$NON-NLS-1$
                .append(getMisc())// has to occur before basic is processed
                .append("\r\n")//$NON-NLS-1$
                .append(getFailed());

        sBasic.append(entity.getShortNameRaw());
        sBasic.append("\r\n"); //$NON-NLS-1$
        if (!isInf) {
            sBasic.append(Math.round(entity.getWeight())).append(
                    Messages.getString("MechView.tons")); //$NON-NLS-1$
        }
        if (entity.isMixedTech()) {
            if (entity.isClan()) {
                sBasic.append(Messages.getString("MechView.MixedClan"));
            } else {
                sBasic.append(Messages.getString("MechView.MixedIS"));
            }
        } else {
            sBasic.append(TechConstants.getLevelDisplayableName(entity
                    .getTechLevel()));
        }
        sBasic.append("\n"); //$NON-NLS-1$

        if (!isGunEmplacement) {
            sBasic.append("\n"); //$NON-NLS-1$
            sBasic.append(Messages.getString("MechView.Movement"))//$NON-NLS-1$
                    .append(entity.getWalkMP()).append("/")//$NON-NLS-1$
                    .append(entity.getRunMPasString());
            if (entity.getJumpMP() > 0) {
                sBasic.append("/")//$NON-NLS-1$
                        .append(entity.getJumpMP());
            }
        }
        if (isVehicle) {
            sBasic.append(" (")//$NON-NLS-1$
                    .append(entity.getMovementModeAsString()).append(")"); //$NON-NLS-1$
        }
        sBasic.append("\n"); //$NON-NLS-1$
        if (isMech || isVehicle) {
            sBasic.append(Messages.getString("MechView.Engine")); //$NON-NLS-1$
            sBasic.append(entity.getEngine().getShortEngineName());
            sBasic.append("\n"); //$NON-NLS-1$
        }
        if (isSupportVehicle) {
            sBasic.append(Messages.getString("MechView.BARRating")); //$NON-NLS-1$
            sBasic.append(((SupportTank)entity).getBARRating());
            sBasic.append("\n"); //$NON-NLS-1$
        }
        if (isSupportVTOL) {
            sBasic.append(Messages.getString("MechView.BARRating")); //$NON-NLS-1$
            sBasic.append(((SupportVTOL)entity).getBARRating());
            sBasic.append("\n"); //$NON-NLS-1$
        }
        if (isMech) {
            Mech aMech = (Mech) entity;
            sBasic.append(Messages.getString("MechView.HeatSinks"))//$NON-NLS-1$
                    .append(aMech.heatSinks());
            if (aMech.getHeatCapacity() > aMech.heatSinks()) {
                sBasic.append(" [")//$NON-NLS-1$
                        .append(aMech.getHeatCapacity()).append("]"); //$NON-NLS-1$
            }
            if (aMech.getCockpitType() != Mech.COCKPIT_STANDARD) {
                sBasic.append("\n"); //$NON-NLS-1$
                sBasic.append(Messages.getString("MechView.Cockpit"));
                sBasic.append(aMech.getCockpitTypeString());
            }
            if (aMech.getGyroType() != Mech.GYRO_STANDARD) {
                sBasic.append("\n");
                sBasic.append(Messages.getString("MechView.Gyro"));
                sBasic.append(aMech.getGyroTypeString());
            }
            sBasic.append("\n");
        }
        sBasic.append("\n"); //$NON-NLS-1$
        if (!isGunEmplacement) {
            sBasic.append(getInternalAndArmor());
        } else {
            sBasic.append(Messages.getString("MechView.ConstructionFactor"))
                    .append(
                            renderArmor(entity
                                    .getArmor(GunEmplacement.LOC_BUILDING)))
                    .append('\n'); //$NON-NLS-1$
            if (((GunEmplacement) entity).hasTurret()) {
                sBasic.append(Messages.getString("MechView.TurretArmor"))
                        .append(
                                renderArmor(entity
                                        .getArmor(GunEmplacement.LOC_TURRET)))
                        .append('\n'); //$NON-NLS-1$
            }
        }

        if (entity.getFluff() != null)
            sFluff.append(entity.getFluff());
    }

    public String getMechReadoutBasic() {
        return sBasic.toString();
    }

    public String getMechReadoutLoadout() {
        return sLoadout.toString();
    }

    public String getMechReadoutFluff() {
        return sFluff.toString();
    }

    public String getMechReadout() {
        return getMechReadoutBasic()
                + "\n" + getMechReadoutLoadout() + "\n" + getMechReadoutFluff(); //$NON-NLS-1$
    }

    private String getInternalAndArmor() {
        StringBuffer sIntArm = new StringBuffer();

        int maxArmor = entity.getTotalInternal() * 2 + 3;
        sIntArm.append(Messages.getString("MechView.Internal"))//$NON-NLS-1$
                .append(entity.getTotalInternal());
        if (isMech) {
            sIntArm.append(Messages.getString("MechView."
                    + EquipmentType.getStructureTypeName(entity
                            .getStructureType())));
        }
        sIntArm.append("\n"); //$NON-NLS-1$

        sIntArm.append(Messages.getString("MechView.Armor"))//$NON-NLS-1$
                .append(entity.getTotalArmor());
        if (isMech) {
            sIntArm.append("/")//$NON-NLS-1$
                    .append(maxArmor);
        }
        if (!isInf && !isProto) {
            sIntArm.append(Messages.getString("MechView."
                    + EquipmentType.getArmorTypeName(entity.getArmorType())));
        }
        sIntArm.append("\n"); //$NON-NLS-1$
        // Walk through the entity's locations.
        for (int loc = 0; loc < entity.locations(); loc++) {

            // Skip empty sections.
            if (IArmorState.ARMOR_NA == entity.getInternal(loc)
                    || (isVehicle && (((loc == Tank.LOC_TURRET && ((Tank) entity).hasNoTurret()) || (loc == Tank.LOC_BODY))
                    || (isLargeSupportVehicle && (loc == LargeSupportTank.LOC_TURRET && ((LargeSupportTank) entity).hasNoTurret() || (loc == LargeSupportTank.LOC_BODY)))))) {
                continue;
            }

            if (entity.getLocationAbbr(loc).length() < 2) {
                sIntArm.append(" "); //$NON-NLS-1$
            }
            sIntArm.append(entity.getLocationAbbr(loc)).append(": "); //$NON-NLS-1$
            sIntArm.append(renderArmor(entity.getInternal(loc))).append("   "); //$NON-NLS-1$
            if (IArmorState.ARMOR_NA != entity.getArmor(loc)) {
                sIntArm.append(renderArmor(entity.getArmor(loc)));
            }
            if (entity.hasRearArmor(loc)) {
                sIntArm.append(" (")//$NON-NLS-1$
                        .append(renderArmor(entity.getArmor(loc, true))).append(
                                ")"); //$NON-NLS-1$
            }
            sIntArm.append("\n"); //$NON-NLS-1$
        }
        return sIntArm.toString();
    }

    private String getWeapons() {
        StringBuffer sWeapons = new StringBuffer();
        for (Mounted mounted : entity.getWeaponList()) {
            WeaponType wtype = (WeaponType) mounted.getType();

            sWeapons.append(mounted.getDesc()).append("  [")//$NON-NLS-1$
                    .append(entity.getLocationAbbr(mounted.getLocation()));
            if (mounted.isSplit()) {
                sWeapons.append("/")// $NON-NLS-1$
                        .append(
                                entity.getLocationAbbr(mounted
                                        .getSecondLocation()));
            }
            sWeapons.append("]"); //$NON-NLS-1$
            if (entity.isClan()
                    && mounted.getType().getInternalName().substring(0, 2)
                            .equals("IS")) { //$NON-NLS-1$
                sWeapons.append(Messages.getString("MechView.IS")); //$NON-NLS-1$
            }
            if (!entity.isClan()
                    && mounted.getType().getInternalName().substring(0, 2)
                            .equals("CL")) { //$NON-NLS-1$
                sWeapons.append(Messages.getString("MechView.Clan")); //$NON-NLS-1$
            }
            if (wtype.hasFlag(WeaponType.F_ONESHOT)) {
                sWeapons.append(" <")//$NON-NLS-1$
                        .append(mounted.getLinked().getDesc()).append(">"); //$NON-NLS-1$
            }
            sWeapons
                    .append(" ").append(wtype.getHeat()).append(Messages.getString("MechView.Heat")); //$NON-NLS-1$ //$NON-NLS-2$
            sWeapons.append("\n"); //$NON-NLS-1$
        }
        return sWeapons.toString();
    }

    private String getAmmo() {
        StringBuffer sAmmo = new StringBuffer();
        for (Mounted mounted : entity.getAmmo()) {
            if (mounted.getLocation() != Entity.LOC_NONE) {
                sAmmo.append(mounted.getDesc()).append("  [")//$NON-NLS-1$
                        .append(entity.getLocationAbbr(mounted.getLocation()))
                        .append("]\n"); //$NON-NLS-1$
            }
        }
        return sAmmo.toString();
    }

    private String getMisc() {
        StringBuffer sMisc = new StringBuffer();
        for (Mounted mounted : entity.getMisc()) {
            String name = mounted.getName();
            if (name.indexOf("Jump Jet") != -1 //$NON-NLS-1$ 
                    || (name.indexOf("CASE") != -1 && entity.isClan()) //$NON-NLS-1$
                    || name.indexOf("Heat Sink") != -1 //$NON-NLS-1$
                    || name.indexOf("Endo Steel") != -1 //$NON-NLS-1$
                    || name.indexOf("Ferro-Fibrous") != -1) { //$NON-NLS-1$
                // These items are displayed elsewhere, so skip them here.
                continue;
            }
            sMisc.append(mounted.getDesc()).append("  [")//$NON-NLS-1$
                    .append(entity.getLocationAbbr(mounted.getLocation()))
                    .append("]"); //$NON-NLS-1$
            if (entity.isClan()
                    && mounted.getType().getInternalName().substring(0, 2)
                            .equals("IS")) { //$NON-NLS-1$
                sMisc.append(Messages.getString("MechView.IS")); //$NON-NLS-1$
            }
            if (!entity.isClan()
                    && mounted.getType().getInternalName().substring(0, 2)
                            .equals("CL")) { //$NON-NLS-1$
                sMisc.append(Messages.getString("MechView.Clan")); //$NON-NLS-1$
            }
            sMisc.append("\n"); //$NON-NLS-1$
        }

        String capacity = entity.getUnusedString();
        if (capacity != null && capacity.length() > 0) {
            sMisc.append(Messages.getString("MechView.CarringCapacity"))//$NON-NLS-1$
                    .append(capacity).append("\n"); //$NON-NLS-1$
        }
        return sMisc.toString();
    }

    private String getFailed() {
        StringBuffer sFailed = new StringBuffer();
        Iterator<String> eFailed = entity.getFailedEquipment();
        if (eFailed.hasNext()) {
            sFailed.append("The following equipment\n slots failed to load:\n"); //$NON-NLS-1$
            while (eFailed.hasNext()) {
                sFailed.append(eFailed.next()).append("\n"); //$NON-NLS-1$
            }
        }
        return sFailed.toString();
    }

    private static String renderArmor(int nArmor) {
        if (nArmor <= 0) {
            return "xx"; //$NON-NLS-1$
        }
        return makeLength(String.valueOf(nArmor), 3, true);
    }

    private static final String SPACES = "                                   "; //$NON-NLS-1$

    private static String makeLength(String s, int n, boolean bRightJustify) {
        int l = s.length();
        if (l == n) {
            return s;
        } else if (l < n) {
            if (bRightJustify) {
                return SPACES.substring(0, n - l) + s;
            }
            return s + SPACES.substring(0, n - l);
        } else {
            return s.substring(0, n - 2) + ".."; //$NON-NLS-1$
        }
    }

}
