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

package megamek.common;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Iterator;

import megamek.client.ui.Messages;
import megamek.common.weapons.BayWeapon;

/**
 * A utility class for retrieving mech information in a formatted string.
 */
public class MechView {

    private Entity entity;
    private boolean isMech;
    private boolean isInf;
    private boolean isBA;
    private boolean isVehicle;
    private boolean isProto;
    private boolean isGunEmplacement;
    private boolean isAero;
    private boolean isFixedWingSupport;
    private boolean isSquadron;
    private boolean isSmallCraft;
    private boolean isJumpship;
    private boolean isSpaceStation;

    StringBuffer sHead = new StringBuffer();
    StringBuffer sBasic = new StringBuffer();
    StringBuffer sLoadout = new StringBuffer();
    StringBuffer sFluff = new StringBuffer("");

    public MechView(Entity entity, boolean showDetail) {
        this(entity, showDetail, false);
    }
    
    public MechView(Entity entity, boolean showDetail, boolean useAlternateCost) {
        this.entity = entity;
        isMech = entity instanceof Mech;
        isInf = entity instanceof Infantry;
        isBA = entity instanceof BattleArmor;
        isVehicle = entity instanceof Tank;
        isProto = entity instanceof Protomech;
        isGunEmplacement = entity instanceof GunEmplacement;
        isAero = entity instanceof Aero;
        isFixedWingSupport = entity instanceof FixedWingSupport;
        isSquadron = entity instanceof FighterSquadron;
        isSmallCraft = entity instanceof SmallCraft;
        isJumpship = entity instanceof Jumpship;
        isSpaceStation = entity instanceof SpaceStation;

        sLoadout.append(getWeapons(showDetail)).append("<br>"); //$NON-NLS-1$
        if (!entity.usesWeaponBays() || !showDetail) {
            sLoadout.append(getAmmo()).append("<br>"); //$NON-NLS-1$
        }
        if (entity instanceof Aero) {
            sLoadout.append(getBombs()).append("<br>"); //$NON-NLS-1$
        }
        sLoadout.append(getMisc()) // has to occur before basic is processed
                .append("<br>") //$NON-NLS-1$
                .append(getFailed()).append("<br>");
        // sBasic.append(getFluffImage(entity)).append("<br>");
        sHead.append("<font size=+1><b>" + entity.getShortNameRaw()
                + "</b></font>");
        sHead.append("<br>"); //$NON-NLS-1$
        if (!entity.isDesignValid()) {
            sHead.append(Messages.getString("MechView.DesignInvalid"));
            sHead.append("<br>"); //$NON-NLS-1$
        }
        if (entity.isMixedTech()) {
            if (entity.isClan()) {
                sHead.append(Messages.getString("MechView.MixedClan"));
            } else {
                sHead.append(Messages.getString("MechView.MixedIS"));
            }
        } else {
            sHead.append(TechConstants.getLevelDisplayableName(entity
                    .getTechLevel()));
        }
        sHead.append("<br>"); //$NON-NLS-1$
        if (!isInf) {
            sHead.append(Math.round(entity.getWeight())).append(
                    Messages.getString("MechView.tons")); //$NON-NLS-1$
            sHead.append("<br>"); //$NON-NLS-1$
        }
        DecimalFormatSymbols unusualSymbols = new DecimalFormatSymbols();
        unusualSymbols.setDecimalSeparator('.');
        unusualSymbols.setGroupingSeparator(',');
        DecimalFormat dFormatter = new DecimalFormat("#,###.##", unusualSymbols);
        sHead.append("BV: ");
        sHead.append(dFormatter.format(entity.calculateBattleValue(false,
                null == entity.getCrew())));
        sHead.append("<br>"); //$NON-NLS-1$
        double cost = entity.getCost(false);
        sHead.append("Cost: ");
        if(useAlternateCost && entity.getAlternateCost() > 0) {
            cost = entity.getAlternateCost();
        }
        sHead.append(dFormatter.format(cost));
        sHead.append(" C-bills");
        sHead.append("<br>"); //$NON-NLS-1$
        if (!entity.getSource().equals("")){
            sHead.append("Source: "); //$NON-NLS-1$
            sHead.append(entity.getSource());
            sHead.append("<br>"); //$NON-NLS-1$
        }

        if (!isGunEmplacement) {
            sBasic.append("<br>"); //$NON-NLS-1$
            sBasic.append(Messages.getString("MechView.Movement")) //$NON-NLS-1$
                    .append(entity.getWalkMP()).append("/") //$NON-NLS-1$
                    .append(entity.getRunMPasString());
            if (entity.getJumpMP() > 0) {
                sBasic.append("/") //$NON-NLS-1$
                        .append(entity.getJumpMP());
            }
            if (entity.damagedJumpJets() > 0) {
                sBasic.append("<font color='red'> (" + entity.damagedJumpJets()
                        + " damaged jump jets)</font>");
            }
            if (entity.getAllUMUCount() > 0) {
                // Add in Jump MP if it wasn't already printed
                if (entity.getJumpMP() == 0) {
                    sBasic.append("/0"); //$NON-NLS-1$
                }
                sBasic.append("/") //$NON-NLS-1$
                        .append(entity.getActiveUMUCount());
                if ((entity.getAllUMUCount() - entity.getActiveUMUCount()) != 0) {
                    sBasic.append("<font color='red'> ("
                            + (entity.getAllUMUCount() - entity
                                    .getActiveUMUCount())
                            + " damaged UMUs)</font>");
                }
            }
        }
        if (isBA && ((BattleArmor) entity).isBurdened()) {
            sBasic.append("<br><i>(").append(Messages.getString("MechView.Burdened")).append(")</i>"); //$NON-NLS-1$
        }
        if (isBA && ((BattleArmor) entity).hasDWP()) {
            sBasic.append("<br><i>(").append(Messages.getString("MechView.DWPBurdened")).append(")</i>"); //$NON-NLS-1$
        }
        if (isVehicle) {
            sBasic.append(" (") //$NON-NLS-1$
                    .append(Messages
                            .getString("MovementType." + entity.getMovementModeAsString())).append(")"); //$NON-NLS-1$
            if ((((Tank) entity).getMotiveDamage() > 0)
                    || (((Tank) entity).getMotivePenalty() > 0)) {
                sBasic.append("<font color='red'> (motive damage: -"
                        + ((Tank) entity).getMotiveDamage() + "MP/-"
                        + ((Tank) entity).getMotivePenalty()
                        + " piloting)</font>");
            }
        }
        sBasic.append("<br>"); //$NON-NLS-1$
        if (isMech || isVehicle
                || (isAero && !isSmallCraft && !isJumpship && !isSquadron)) {
            sBasic.append(Messages.getString("MechView.Engine")); //$NON-NLS-1$
            sBasic.append(entity.getEngine().getShortEngineName());
            if (entity.getEngineHits() > 0) {
                sBasic.append("<font color='red'> (" + entity.getEngineHits()
                        + " hits)</font>");
            }
            if (isMech){
                if (entity.hasArmoredEngine()) {
                    sBasic.append(" (armored)");
                }
            }
            sBasic.append("<br>"); //$NON-NLS-1$
        }
        if (!entity.hasPatchworkArmor() && entity.hasBARArmor(1)) {
            sBasic.append(Messages.getString("MechView.BARRating")); //$NON-NLS-1$
            sBasic.append(entity.getBARRating(0));
            sBasic.append("<br>"); //$NON-NLS-1$
        }

        if (isAero) {
            Aero a = (Aero) entity;
            sBasic.append(Messages.getString("MechView.HeatSinks")) //$NON-NLS-1$
                    .append(a.getHeatSinks());
            if (a.getHeatCapacity() > a.getHeatSinks()) {
                sBasic.append(" [") //$NON-NLS-1$
                        .append(a.getHeatCapacity()).append("]"); //$NON-NLS-1$
            }
            if (a.getHeatSinkHits() > 0) {
                sBasic.append("<font color='red'> (" + a.getHeatSinkHits()
                        + " damaged)</font>");
            }
            if (a.getCockpitType() != Mech.COCKPIT_STANDARD) {
                sBasic.append("<br>"); //$NON-NLS-1$
                sBasic.append(Messages.getString("MechView.Cockpit"));
                sBasic.append(a.getCockpitTypeString());
            }
        }

        if (isMech) {
            Mech aMech = (Mech) entity;
            sBasic.append(aMech.getHeatSinkTypeName() + "s: ").append(
                    aMech.heatSinks());
            if (aMech.getHeatCapacity() > aMech.heatSinks()) {
                sBasic.append(" [") //$NON-NLS-1$
                        .append(aMech.getHeatCapacity()).append("]"); //$NON-NLS-1$
            }
            if (aMech.damagedHeatSinks() > 0) {
                sBasic.append("<font color='red'> (" + aMech.damagedHeatSinks()
                        + " damaged)</font>");
            }
            if ((aMech.getCockpitType() != Mech.COCKPIT_STANDARD)
                    || aMech.hasArmoredCockpit()) {
                sBasic.append("<br>"); //$NON-NLS-1$
                sBasic.append(Messages.getString("MechView.Cockpit"));
                sBasic.append(aMech.getCockpitTypeString());
                if (aMech.hasArmoredCockpit()){
                    sBasic.append(" (armored)");
                }
            }
            sBasic.append("<br>");
            sBasic.append(Messages.getString("MechView.Gyro"));
            sBasic.append(aMech.getGyroTypeString());
            if (aMech.getGyroHits() > 0) {
                sBasic.append("<font color='red'> (" + aMech.getGyroHits()
                        + " hits)</font>");
            }
            if (aMech.hasArmoredGyro()){
                sBasic.append(" (armored)");
            }
            sBasic.append("<br>");
        }

        if (isAero && !((Aero) entity).getCritDamageString().equals("")) {
            sBasic.append("<br><br>System Damage: <font color='red'>"
                    + ((Aero) entity).getCritDamageString() + "</font>");
        }

        sBasic.append("<br>"); //$NON-NLS-1$
        if (!isGunEmplacement) {
            if (isSquadron) {
                sBasic.append(getArmor());
            } else if (isAero) {
                sBasic.append(getSIandArmor());
            } else {
                sBasic.append(getInternalAndArmor());
            }
        }
        

        if (entity.getFluff().getOverview() != "") {
            sFluff.append("<br>");
            sFluff.append("\n<b>Overview:</b> <br>\n");
            sFluff.append(entity.getFluff().getOverview());
            sFluff.append("<br>");
        }
        
        if (entity.getFluff().getCapabilities() != "") {
            sFluff.append("<br>");
            sFluff.append("\n<b>Capabilities:</b> <br>\n");
            sFluff.append(entity.getFluff().getCapabilities()); 
            sFluff.append("<br>");
        }
        
        if (entity.getFluff().getDeployment() != "") {
            sFluff.append("<br>");
            sFluff.append("\n<b>Deployment:</b> <br>\n");
            sFluff.append(entity.getFluff().getDeployment());
            sFluff.append("<br>");
        }
        
        if (entity.getFluff().getHistory() != "") {
            sFluff.append("<br>");
            sFluff.append("\n<b>History:</b> <br>\n");
            sFluff.append(entity.getFluff().getHistory()); 
            sFluff.append("<br>");
        }
        sFluff.append("<br>");
    }

    public String getMechReadoutHead() {
        return sHead.toString();
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
        return "<div style='font: 12pt monospaced'>" + getMechReadoutHead()
                + getMechReadoutBasic() + "<br>" + getMechReadoutLoadout()
                + "<br>" + getMechReadoutFluff() + "</div>";
    }

    private String getInternalAndArmor() {
        StringBuffer sIntArm = new StringBuffer();

        int maxArmor = (entity.getTotalInternal() * 2) + 3;
        if (isInf && !isBA) {
            Infantry inf = (Infantry) entity;
            sIntArm.append(Messages.getString("MechView.Men"))
                    .append(entity.getTotalInternal())
                    .append(" (" + inf.getSquadSize() + "/" + inf.getSquadN()
                            + ")");
        } else {
            sIntArm.append(Messages.getString("MechView.Internal")) //$NON-NLS-1$
                    .append(entity.getTotalInternal());
        }
        if (isMech) {
            sIntArm.append(Messages.getString("MechView."
                    + EquipmentType.getStructureTypeName(entity
                            .getStructureType())));
        }
        sIntArm.append("<br>"); //$NON-NLS-1$

        if (isInf && !isBA) {
            Infantry inf = (Infantry) entity;
            sIntArm.append(Messages.getString("MechView.Armor")).append(
                    inf.getArmorDesc());
        } else {
            sIntArm.append(Messages.getString("MechView.Armor")) //$NON-NLS-1$
                    .append(entity.getTotalArmor());

        }
        if (isMech) {
            sIntArm.append("/") //$NON-NLS-1$
                    .append(maxArmor);
        }
        if (!isInf && !isProto && !entity.hasPatchworkArmor()) {
            sIntArm.append(Messages.getString("MechView."
                    + EquipmentType.getArmorTypeName(entity.getArmorType(1))
                            .trim()));
        }
        if (isBA) {
            sIntArm.append(" ").append(
                    EquipmentType.getBaArmorTypeName(entity.getArmorType(1))
                            .trim());
        }
        sIntArm.append("<br>"); //$NON-NLS-1$
        // Walk through the entity's locations.

        if (!(isInf && !isBA)) {
            sIntArm.append("<table cellspacing=0 cellpadding=1 border=0>");
            sIntArm.append("<tr><th></th><th>&nbsp;&nbsp;Internal</th><th>&nbsp;&nbsp;Armor</th><th></th><th></th></tr>");
            for (int loc = 0; loc < entity.locations(); loc++) {
                // Skip empty sections.
                if (entity.getInternal(loc) == IArmorState.ARMOR_NA) {
                    continue;
                }
                // Skip nonexistent turrets by vehicle type, as well as the
                // body location.
                if (isVehicle) {
                    if (loc == Tank.LOC_BODY) {
                        continue;
                    }
                    if ((loc == ((Tank) entity).getLocTurret())
                            && ((Tank) entity).hasNoTurret()) {
                        continue;
                    }

                }
                sIntArm.append("<tr>");
                sIntArm.append("<td>").append(entity.getLocationName(loc)); //$NON-NLS-1$
                sIntArm.append("</td>");
                sIntArm.append(renderArmor(entity.getInternalForReal(loc),
                        entity.getOInternal(loc))); //$NON-NLS-1$
                if (IArmorState.ARMOR_NA != entity.getArmorForReal(loc)) {
                    sIntArm.append(renderArmor(entity.getArmorForReal(loc),
                            entity.getOArmor(loc)));
                }
                if (entity.hasPatchworkArmor()) {
                    sIntArm.append("<td>");
                    sIntArm.append(Messages.getString("MechView."
                            + EquipmentType.getArmorTypeName(entity
                                    .getArmorType(loc)).trim()));
                    sIntArm.append("</td>");
                    if (entity.hasBARArmor(loc)) {
                        sIntArm.append("<td>");
                        sIntArm.append(Messages.getString("MechView.BARRating")); //$NON-NLS-1$
                        sIntArm.append(entity.getBARRating(loc));
                        sIntArm.append("</td>");
                    }
                }
                sIntArm.append("<td><font color='red'> "
                        + entity.getLocationDamage(loc) + "</font></td>");
                sIntArm.append("</tr>"); //$NON-NLS-1$
                if (entity.hasRearArmor(loc)) {
                    sIntArm.append("<tr>"); //$NON-NLS-1$
                    sIntArm.append("<td>").append(entity.getLocationName(loc))
                            .append(" (rear)").append("</td>")
                            .append("<td></td>");
                    sIntArm.append(renderArmor(
                            entity.getArmorForReal(loc, true),
                            entity.getOArmor(loc, true))); //$NON-NLS-1$
                    if (entity.hasPatchworkArmor()) {
                        sIntArm.append("<td>");
                        sIntArm.append(Messages.getString("MechView."
                                + EquipmentType.getArmorTypeName(entity
                                        .getArmorType(loc)).trim()));
                        sIntArm.append("</td>");
                        if (entity.hasBARArmor(loc)) {
                            sIntArm.append("<td>");
                            sIntArm.append(Messages
                                    .getString("MechView.BARRating")); //$NON-NLS-1$
                            sIntArm.append(entity.getBARRating(loc));
                            sIntArm.append("</td>");
                        }
                    }
                    sIntArm.append("</tr>"); //$NON-NLS-1$
                }
            }
            sIntArm.append("</table>");
        }
        return sIntArm.toString();
    }

    private String getSIandArmor() {

        Aero a = (Aero) entity;

        StringBuffer sIntArm = new StringBuffer();

        sIntArm.append("<br>"); //$NON-NLS-1$

        // int maxArmor = (int) mech.getWeight() * 8;
        sIntArm.append(Messages.getString("MechView.SI")) //$NON-NLS-1$
                .append(a.getSI());

        sIntArm.append("<br>"); //$NON-NLS-1$

        // if it is a jumpship get sail and KF integrity
        if (isJumpship & !isSpaceStation) {
            Jumpship js = (Jumpship) entity;

            sIntArm.append(Messages.getString("MechView.SailIntegrity")) //$NON-NLS-1$
                    .append(js.getSailIntegrity());

            sIntArm.append("<br>"); //$NON-NLS-1$

            sIntArm.append(Messages.getString("MechView.KFIntegrity")) //$NON-NLS-1$
                    .append(js.getKFIntegrity());

            sIntArm.append("<br>"); //$NON-NLS-1$
        }

        if (entity.isCapitalFighter()) {
            sIntArm.append(Messages.getString("MechView.Armor")) //$NON-NLS-1$
                    .append(a.getCapArmor());
        } else {
            sIntArm.append(Messages.getString("MechView.Armor")) //$NON-NLS-1$
                    .append(entity.getTotalArmor());
        }

        if (isJumpship) {
            sIntArm.append(Messages.getString("MechView.CapitalArmor"));
        }

        if (!entity.hasPatchworkArmor()) {
            sIntArm.append(Messages.getString("MechView."
                    + EquipmentType.getArmorTypeName(entity.getArmorType(1))
                            .trim()));
        }

        sIntArm.append("<br>"); //$NON-NLS-1$
        // Walk through the entity's locations.
        if (!entity.isCapitalFighter()) {
            sIntArm.append("<table cellspacing=0 cellpadding=1 border=0>");
            sIntArm.append("<tr><th></th><th>&nbsp;&nbsp;Armor</th></tr>");
            for (int loc = 0; loc < entity.locations(); loc++) {

                // Skip empty sections.
                if (IArmorState.ARMOR_NA == entity.getInternal(loc)) {
                    continue;
                }
                // skip broadsides on warships
                if ((entity instanceof Warship)
                        && ((loc == Warship.LOC_LBS) || (loc == Warship.LOC_RBS))) {
                    continue;
                }
                // skip the "Wings" location
                if (!a.isLargeCraft() && (loc == Aero.LOC_WINGS)) {
                    continue;
                }
                // Skip the "Body" location on fixed-wing support vees.
                if (isFixedWingSupport && (loc == FixedWingSupport.LOC_BODY)) {
                    continue;
                }

                sIntArm.append("<tr><td>").append(entity.getLocationName(loc))
                        .append("</td>"); //$NON-NLS-1$
                if (IArmorState.ARMOR_NA != entity.getArmor(loc)) {
                    sIntArm.append(renderArmor(entity.getArmor(loc),
                            entity.getOArmor(loc)));
                }
                if (entity.hasPatchworkArmor()) {
                    sIntArm.append("<td>");
                    sIntArm.append(Messages.getString("MechView."
                            + EquipmentType.getArmorTypeName(entity
                                    .getArmorType(loc)).trim()));
                    sIntArm.append("</td>");
                    if (entity.hasBARArmor(loc)) {
                        sIntArm.append("<td>");
                        sIntArm.append(Messages.getString("MechView.BARRating")); //$NON-NLS-1$
                        sIntArm.append(entity.getBARRating(loc));
                        sIntArm.append("</td>");
                    }
                }
                /*
                 * if ( entity.hasRearArmor(loc) ) { sIntArm.append( " (" )
                 * //$NON-NLS-1$ .append( renderArmor(entity.getArmor(loc,
                 * true), entity.getOArmor(loc, true)) ) .append( ")" );
                 * //$NON-NLS-1$ }
                 */
                sIntArm.append("</tr>"); //$NON-NLS-1$
            }
            sIntArm.append("</table>");
        }

        return sIntArm.toString();
    }

    private String getArmor() {

        FighterSquadron fs = (FighterSquadron) entity;

        StringBuffer sIntArm = new StringBuffer();

        sIntArm.append("<br>"); //$NON-NLS-1$

        sIntArm.append(Messages.getString("MechView.Armor")) //$NON-NLS-1$
                .append(fs.getTotalArmor());

        sIntArm.append("<br>"); //$NON-NLS-1$

        sIntArm.append(Messages.getString("MechView.ActiveFighters")) //$NON-NLS-1$
                .append(fs.getNFighters());

        sIntArm.append("<br>"); //$NON-NLS-1$

        return sIntArm.toString();
    }

    private String getWeapons(boolean showDetail) {

        StringBuffer sWeapons = new StringBuffer();

        if (isInf && !isBA) {
            Infantry inf = (Infantry) entity;
            sWeapons.append("<table cellspacing=0 cellpadding=1 border=0>");
            sWeapons.append("<tr><td>Primary Weapon:</td> ");
            if (null == inf.getPrimaryWeapon()) {
                sWeapons.append("<td>None</td></tr>");
            } else {
                sWeapons.append("<td>" + inf.getPrimaryWeapon().getDesc()
                        + "</td></tr>");
            }
            sWeapons.append("<tr><td>Secondary Weapon:</td> ");
            if ((null == inf.getSecondaryWeapon())
                    || (inf.getSecondaryN() == 0)) {
                sWeapons.append("<td>None</td></tr>");
            } else {
                sWeapons.append("<td>" + inf.getSecondaryWeapon().getDesc()
                        + " (" + inf.getSecondaryN() + ")</td></tr>");
            }
            sWeapons.append("<tr><td>Damage per trooper:</td><td>")
                    .append((double) Math.round(inf.getDamagePerTrooper() * 1000) / 1000)
                    .append("</td></tr>");
            sWeapons.append("</table><p>");
        }

        if (entity.getWeaponList().size() < 1) {
            return "";
        }
        sWeapons.append("<table cellspacing=0 cellpadding=1 border=0>");
        sWeapons.append("<tr><th align='left'>Weapons</th><th>&nbsp;&nbsp;Loc</th><th>&nbsp;&nbsp;Heat</th></tr>");
        for (Mounted mounted : entity.getWeaponList()) {
            WeaponType wtype = (WeaponType) mounted.getType();

            if (mounted.isDestroyed()) {
                if (mounted.isRepairable()) {
                    sWeapons.append("<tr bgcolor='yellow'>");
                } else {
                    sWeapons.append("<tr bgcolor='red'>");
                }
            } else {
                sWeapons.append("<tr>");
            }
            sWeapons.append("<td>").append(mounted.getDesc()); //$NON-NLS-1$
            if (entity.isClan()
                    && !TechConstants.isClan(mounted.getType().getTechLevel(entity.getYear()))) {
                sWeapons.append(Messages.getString("MechView.IS")); //$NON-NLS-1$
            }
            if (!entity.isClan()
                    && TechConstants.isClan(mounted.getType().getTechLevel(entity.getYear()))) {
                sWeapons.append(Messages.getString("MechView.Clan")); //$NON-NLS-1$
            }
            /*
             * TODO: this should probably go in the ammo table somewhere if
             * (wtype.hasFlag(WeaponType.F_ONESHOT)) { sWeapons.append(" [")
             * //$NON-NLS-1$ .append(mounted.getLinked().getDesc()).append("]");
             * //$NON-NLS-1$ }
             */
            sWeapons.append("</td>");

            sWeapons.append("<td align='right'>").append(
                    entity.getLocationAbbr(mounted.getLocation()));
            if (mounted.isSplit()) {
                sWeapons.append("/") // $NON-NLS-1$
                        .append(entity.getLocationAbbr(mounted
                                .getSecondLocation()));
            }
            sWeapons.append("</td>"); //$NON-NLS-1$

            int heat = wtype.getHeat();
            int bWeapDamaged = 0;
            if (wtype instanceof BayWeapon) {
                // loop through weapons in bay and add up heat
                heat = 0;
                for (int wId : mounted.getBayWeapons()) {
                    Mounted m = entity.getEquipment(wId);
                    if (null == m) {
                        continue;
                    }
                    heat = heat + ((WeaponType) m.getType()).getHeat();
                    if(m.isDestroyed()) {
                        bWeapDamaged++;
                    }
                }
            }
            sWeapons.append("<td align='right'>").append(heat).append("</td>"); //$NON-NLS-1$ //$NON-NLS-2$
            
            if(wtype instanceof BayWeapon && bWeapDamaged > 0 && !showDetail) {
                sWeapons.append("<td><font color='red'>("
                        + bWeapDamaged + Messages.getString("MechView.WeaponDamage") + ")</font><td>");
            } else {
                sWeapons.append("<td></td>"); //$NON-NLS-1$
            }
            sWeapons.append("</tr>");           

            // if this is a weapon bay, then cycle through weapons and ammo           
            if((wtype instanceof BayWeapon) && showDetail) { 
                for(int wId : mounted.getBayWeapons()) { 
                    Mounted m = entity.getEquipment(wId);
                    if(null == m) { 
                        continue; 
                    } 
                      
                    if (m.isDestroyed()) {
                        if (m.isRepairable()) {
                            sWeapons.append("<tr bgcolor='yellow'>");
                        } else {
                            sWeapons.append("<tr bgcolor='red'>");
                        }
                    } else {
                        sWeapons.append("<tr>");
                    }
                    sWeapons.append("<td>").append("&nbsp;&nbsp;>" + m.getDesc()); //$NON-NLS-1$
                    if (entity.isClan()
                            && !TechConstants.isClan(m.getType().getTechLevel(entity.getYear()))) {
                        sWeapons.append(Messages.getString("MechView.IS")); //$NON-NLS-1$
                    }
                    if (!entity.isClan()
                            && TechConstants.isClan(m.getType().getTechLevel(entity.getYear()))) {
                        sWeapons.append(Messages.getString("MechView.Clan")); //$NON-NLS-1$
                    }
                    sWeapons.append("</td>");    
                    sWeapons.append("</tr>");  
                }
                for(int aId : mounted.getBayAmmo()) {
                    Mounted m = entity.getEquipment(aId);
                    if(null == m) { 
                        continue; 
                    }
                    // Ignore ammo for one-shot launchers
                    if ((m.getLinkedBy() != null)
                            && m.getLinkedBy().isOneShot()){
                        continue;
                    }
                    if (m.isDestroyed()) {
                        sWeapons.append("<tr bgcolor='red'>");
                    } else if (m.getUsableShotsLeft() < 1) {
                        sWeapons.append("<tr bgcolor='yellow'>");
                    } else {
                        sWeapons.append("<tr>");
                    }
                    if (mounted.getLocation() != Entity.LOC_NONE) {
                        sWeapons.append("<td>").append("&nbsp;&nbsp;>" + m.getName()).append("</td>");
                        sWeapons.append("<td align='right'>")
                                .append(m.getBaseShotsLeft()).append("</td>");
                        sWeapons.append("<td></td>");
                    }
                    sWeapons.append("</tr>");  
                }
            }
        }
        sWeapons.append("</table>"); //$NON-NLS-1$
        return sWeapons.toString();
    }

    private String getAmmo() {
        StringBuffer sAmmo = new StringBuffer();
        if (entity.getAmmo().size() < 1) {
            return "";
        }
        sAmmo.append("<table cellspacing=0 cellpadding=1 border=0>");
        sAmmo.append("<tr><th align='left'>Ammo</th><th>&nbsp;&nbsp;Loc</th><th>&nbsp;&nbsp;Shots</th></tr>");
        for (Mounted mounted : entity.getAmmo()) {
            // Ignore ammo for one-shot launchers
            if ((mounted.getLinkedBy() != null)
                    && mounted.getLinkedBy().isOneShot()){
                continue;
            }
            if (mounted.isDestroyed()) {
                sAmmo.append("<tr bgcolor='red'>");
            } else if (mounted.getUsableShotsLeft() < 1) {
                sAmmo.append("<tr bgcolor='yellow'>");
            } else {
                sAmmo.append("<tr>");
            }
            if (mounted.getLocation() != Entity.LOC_NONE) {
                sAmmo.append("<td>").append(mounted.getName()).append("</td>");
                sAmmo.append("<td align='right'>")
                        .append(entity.getLocationAbbr(mounted.getLocation()))
                        .append("</td>");
                sAmmo.append("<td align='right'>")
                        .append(mounted.getBaseShotsLeft()).append("</td>");
            }
        }
        sAmmo.append("</table>");
        return sAmmo.toString();
    }
    
    

    private String getBombs() {
        StringBuffer sBombs = new StringBuffer();
        Aero a = (Aero) entity;
        int[] choices = a.getBombChoices();
        for (int type = 0; type < BombType.B_NUM; type++) {
            if (choices[type] > 0) {
                sBombs.append(BombType.getBombName(type)).append(" (")
                        .append(Integer.toString(choices[type]))
                        .append(")<br>");
            }
        }
        return sBombs.toString();
    }

    private String getMisc() {
        StringBuffer sMisc = new StringBuffer();
        sMisc.append("<table cellspacing=0 cellpadding=1 border=0>");
        sMisc.append("<tr><th align='left'>Equipment</th><th>&nbsp;&nbsp;Loc</th></tr>");
        int nEquip = 0;
        for (Mounted mounted : entity.getMisc()) {
            String name = mounted.getName();
            if ((mounted.getLocation() == Entity.LOC_NONE)
                    || name.contains("Jump Jet")
                    || (name.contains("CASE")
                        && !name.contains("II")
                        && entity.isClan())
                    || (name.contains("Heat Sink") 
                        && !name.contains("Radical"))
                    || name.contains("Endo Steel")
                    || name.contains("Ferro-Fibrous")
                    || name.contains("Reactive")
                    || name.contains("BA Stealth")
                    || name.contains("BA Fire Resistant")
                    || name.contains("BA Mimetic")
                    || name.contains("BA Standard")
                    || name.contains("BA Advanced")
                    || name.contains("Reflective")
                    || name.contains("Ferro-Lamellor")) {
                // These items are displayed elsewhere, so skip them here.
                continue;
            }
            nEquip++;
            if (mounted.isDestroyed()) {
                sMisc.append("<tr bgcolor='red'>");
            } else {
                sMisc.append("<tr>");
            }
            sMisc.append("<td>").append(mounted.getDesc()).append("</td>"); //$NON-NLS-1$
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
            sMisc.append("</td><td align='right'>")
                    .append(entity.getLocationAbbr(mounted.getLocation()))
                    .append("</td>"); //$NON-NLS-1$

            sMisc.append("</tr>"); //$NON-NLS-1$
        }
        sMisc.append("</table>");
        if (nEquip < 1) {
            sMisc = new StringBuffer();
        }

        String capacity = entity.getUnusedString(true);
        if ((capacity != null) && (capacity.length() > 0)) {
            sMisc.append("<br><b>").append(Messages.getString("MechView.CarringCapacity")).append("</b><br>") //$NON-NLS-1$
                    .append(capacity).append("<br>"); //$NON-NLS-1$
        }
        return sMisc.toString();
    }

    private String getFailed() {
        StringBuffer sFailed = new StringBuffer();
        Iterator<String> eFailed = entity.getFailedEquipment();
        if (eFailed.hasNext()) {
            sFailed.append("<br><b>The following equipment slots failed to load:</b><br>"); //$NON-NLS-1$
            while (eFailed.hasNext()) {
                sFailed.append(eFailed.next()).append("<br>"); //$NON-NLS-1$
            }
        }
        return sFailed.toString();
    }

    private static String renderArmor(int nArmor, int origArmor) {
        double percentRemaining = ((double) nArmor) / ((double) origArmor);
        String armor = Integer.toString(nArmor);
        if (percentRemaining < 0) {
            return "<td align='center' bgcolor='black'><font color='white'>"
                    + "X" + "</font>";
        } else if (percentRemaining <= .25) {
            return "<td align='right' bgcolor='red'><font color='white'>"
                    + armor + "</font>";
        } else if (percentRemaining <= .75) {
            return "<td align='right' bgcolor='yellow'><font color='black'>"
                    + armor + "</font>";
        } else if (percentRemaining < 1.00) {
            return "<td align='right' bgcolor='green'><font color='white'>"
                    + armor + "</font>";
        } else {
            return "<td align='right' bgcolor='white'><font color='black'>"
                    + armor + "</font>";
        }
    }
}
