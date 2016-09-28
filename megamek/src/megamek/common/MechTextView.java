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
public class MechTextView {

    private Entity entity;
    private boolean isMech;
    private boolean isInf;
    private boolean isBA;
    private boolean isVehicle;
    private boolean isProto;
    private boolean isGunEmplacement;
    private boolean isAero;
    private boolean isSquadron;
    private boolean isSmallCraft;
    private boolean isJumpship;
    private boolean isSpaceStation;

    StringBuffer sHead = new StringBuffer();
    StringBuffer sBasic = new StringBuffer();
    StringBuffer sLoadout = new StringBuffer();
    StringBuffer sFluff = new StringBuffer("");

    public MechTextView(Entity entity, boolean showDetail) {
        this.entity = entity;
        isMech = entity instanceof Mech;
        isInf = entity instanceof Infantry;
        isBA = entity instanceof BattleArmor;
        isVehicle = entity instanceof Tank;
        isProto = entity instanceof Protomech;
        isGunEmplacement = entity instanceof GunEmplacement;
        isAero = entity instanceof Aero;
        isSquadron = entity instanceof FighterSquadron;
        isSmallCraft = entity instanceof SmallCraft;
        isJumpship = entity instanceof Jumpship;
        isSpaceStation = entity instanceof SpaceStation;

        sLoadout.append(getWeapons(showDetail)); //$NON-NLS-1$
        if (!entity.usesWeaponBays() || !showDetail) {
            sLoadout.append(getAmmo()); //$NON-NLS-1$
        }
        sLoadout.append(getMisc()) // has to occur before basic is processed
                .append(getFailed());

        sHead.append(entity.getShortNameRaw());
        sHead.append("\n"); //$NON-NLS-1$
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
        sHead.append("\n"); //$NON-NLS-1$
        if (!isInf) {
            sHead.append(Math.round(entity.getWeight())).append(
                    Messages.getString("MechView.tons")); //$NON-NLS-1$
        }
        sHead.append("\n"); //$NON-NLS-1$
        DecimalFormatSymbols unusualSymbols = new DecimalFormatSymbols();
        unusualSymbols.setDecimalSeparator('.');
        unusualSymbols.setGroupingSeparator(',');
        DecimalFormat dFormatter = new DecimalFormat("#,###.##", unusualSymbols);
        sHead.append("BV: ");
        sHead.append(dFormatter.format(entity.calculateBattleValue(false,
                null == entity.getCrew())));
        sHead.append("\n"); //$NON-NLS-1$
        sHead.append("Cost: ");
        sHead.append(dFormatter.format(entity.getCost(false)));
        sHead.append(" C-bills");
        sHead.append("\n"); //$NON-NLS-1$

        if (!isGunEmplacement) {
            sBasic.append("\n"); //$NON-NLS-1$
            sBasic.append(Messages.getString("MechView.Movement")) //$NON-NLS-1$
                    .append(entity.getWalkMP()).append("/") //$NON-NLS-1$
                    .append(entity.getRunMPasString());
            if (entity.getJumpMP() > 0) {
                sBasic.append("/") //$NON-NLS-1$
                        .append(entity.getJumpMP());
            }
            if (entity.damagedJumpJets() > 0) {
                sBasic.append("(" + entity.damagedJumpJets()
                        + " damaged jump jets)");
            }
        }
        if (isBA && ((BattleArmor) entity).isBurdened()) {
            sBasic.append("\n(").append(Messages.getString("MechView.Burdened")).append(")"); //$NON-NLS-1$
        }
        if (isBA && ((BattleArmor) entity).hasDWP()) {
            sBasic.append("\n(").append(Messages.getString("MechView.DWPBurdened")).append(")"); //$NON-NLS-1$
        }
        if (isVehicle) {
            sBasic.append(" (") //$NON-NLS-1$
                    .append(Messages
                            .getString("MovementType." + entity.getMovementModeAsString())).append(")"); //$NON-NLS-1$
            if ((((Tank) entity).getMotiveDamage() > 0)
                    || (((Tank) entity).getMotivePenalty() > 0)) {
                sBasic.append(" (motive damage: -"
                        + ((Tank) entity).getMotiveDamage() + "MP/-"
                        + ((Tank) entity).getMotivePenalty() + " piloting)");
            }
        }
        sBasic.append("\n"); //$NON-NLS-1$
        if (isMech || isVehicle
                || (isAero && !isSmallCraft && !isJumpship && !isSquadron)) {
            sBasic.append(Messages.getString("MechView.Engine")); //$NON-NLS-1$
            sBasic.append(entity.hasEngine() ? entity.getEngine().getShortEngineName() : "(none)");
            if (entity.getEngineHits() > 0) {
                sBasic.append(" (" + entity.getEngineHits() + " hits)");
            }
            sBasic.append("\n"); //$NON-NLS-1$
        }
        if (!entity.hasPatchworkArmor() && entity.hasBARArmor(1)) {
            sBasic.append(Messages.getString("MechView.BARRating")); //$NON-NLS-1$
            sBasic.append(entity.getBARRating(0));
            sBasic.append("\n"); //$NON-NLS-1$
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
                sBasic.append(" (" + a.getHeatSinkHits() + " damaged)");
            }
            if (a.getCockpitType() != Mech.COCKPIT_STANDARD) {
                sBasic.append("\n"); //$NON-NLS-1$
                sBasic.append(Messages.getString("MechView.Cockpit"));
                sBasic.append(a.getCockpitTypeString());
            }
        }

        if (isMech) {
            Mech aMech = (Mech) entity;
            sBasic.append(Messages.getString("MechView.HeatSinks")) //$NON-NLS-1$
                    .append(aMech.heatSinks());
            if (aMech.getHeatCapacity() > aMech.heatSinks()) {
                sBasic.append(" [") //$NON-NLS-1$
                        .append(aMech.getHeatCapacity()).append("]"); //$NON-NLS-1$
            }
            if (aMech.damagedHeatSinks() > 0) {
                sBasic.append("(" + aMech.damagedHeatSinks() + " damaged)");
            }
            if (aMech.getCockpitType() != Mech.COCKPIT_STANDARD) {
                sBasic.append("\n"); //$NON-NLS-1$
                sBasic.append(Messages.getString("MechView.Cockpit"));
                sBasic.append(aMech.getCockpitTypeString());
            }
            sBasic.append("\n");
            sBasic.append(Messages.getString("MechView.Gyro"));
            sBasic.append(aMech.getGyroTypeString());
            if (aMech.getGyroHits() > 0) {
                sBasic.append("(" + aMech.getGyroHits() + " hits)");
            }
            sBasic.append("\n");
        }

        if (isAero && !((Aero) entity).getCritDamageString().equals("")) {
            sBasic.append("\n\nSystem Damage: "
                    + ((Aero) entity).getCritDamageString());
        }

        sBasic.append("\n"); //$NON-NLS-1$
        if (!isGunEmplacement) {
            if (isSquadron) {
                // Nothing to do here
            } else if (isAero) {
                sBasic.append(getSIandArmor());
            } else {
                sBasic.append(getInternalAndArmor());
            }
        }
        
        if (entity.getFluff().getOverview() !=("")) {
            sFluff.append(entity.getFluff().getOverview());
        }
        sFluff.append("\n\n");
        
        if (entity.getFluff().getCapabilities() !=("")) {
            sFluff.append(entity.getFluff().getCapabilities());
        }
        sFluff.append("\n\n");
        
        if (entity.getFluff().getDeployment() !=("")) {
            sFluff.append(entity.getFluff().getDeployment());
        }
        sFluff.append("\n\n");
        
        if (entity.getFluff().getHistory() !=("")) {
            sFluff.append(entity.getFluff().getHistory());
        }
        sFluff.append("\n");
   
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
        return getMechReadoutHead() + getMechReadoutBasic() + "\n"
                + getMechReadoutLoadout() + "\n" + getMechReadoutFluff();
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
        sIntArm.append("\n"); //$NON-NLS-1$

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
        sIntArm.append("\n"); //$NON-NLS-1$
        // Walk through the entity's locations.

        if (!(isInf && !isBA)) {
            sIntArm.append(String.format("%1$-20s %2$8s %3$8s\n", "",
                    "Internal", "Armor"));
            sIntArm.append("--------------------------------------\n");
            for (int loc = 0; loc < entity.locations(); loc++) {
                // Skip empty sections.
                if ((IArmorState.ARMOR_NA == entity.getInternal(loc))
                        || (isVehicle
                                && (loc == ((Tank) entity).getLocTurret()) && ((Tank) entity)
                                    .hasNoTurret()) || (loc == Tank.LOC_BODY)) {
                    continue;
                }
                String armor = "";
                if (IArmorState.ARMOR_NA != entity.getArmorForReal(loc)) {
                    armor = Integer.toString(entity.getOArmor(loc));
                }
                sIntArm.append(String.format("%1$-20s %2$8d %3$8s\n",
                        entity.getLocationName(loc), entity.getOInternal(loc),
                        armor));
                /*
                 * TODO: add this if (entity.hasPatchworkArmor()) {
                 * sIntArm.append("<td>");
                 * sIntArm.append(Messages.getString("MechView." +
                 * EquipmentType.getArmorTypeName(entity .getArmorType(loc))));
                 * sIntArm.append("</td>"); if (entity.hasBARArmor(loc)) {
                 * sIntArm.append("<td>");
                 * sIntArm.append(Messages.getString("MechView.BARRating"));
                 * //$NON-NLS-1$ sIntArm.append(entity.getBARRating(loc));
                 * sIntArm.append("</td>"); } }
                 */
                if (entity.hasRearArmor(loc)) {
                    sIntArm.append(String.format("%1$-20s %2$8s %3$8d\n",
                            entity.getLocationName(loc) + " (rear)", "",
                            entity.getOArmor(loc, true)));
                    /*
                     * if (entity.hasPatchworkArmor()) { sIntArm.append("<td>");
                     * sIntArm.append(Messages.getString("MechView." +
                     * EquipmentType.getArmorTypeName(entity
                     * .getArmorType(loc)))); sIntArm.append("</td>"); if
                     * (entity.hasBARArmor(loc)) { sIntArm.append("<td>");
                     * sIntArm.append(Messages
                     * .getString("MechView.BARRating")); //$NON-NLS-1$
                     * sIntArm.append(entity.getBARRating(loc));
                     * sIntArm.append("</td>"); } }
                     */
                }
            }
        }
        return sIntArm.toString();
    }

    private String getSIandArmor() {

        Aero a = (Aero) entity;

        StringBuffer sIntArm = new StringBuffer();

        sIntArm.append("\n"); //$NON-NLS-1$

        // int maxArmor = (int) mech.getWeight() * 8;
        sIntArm.append(Messages.getString("MechView.SI")) //$NON-NLS-1$
                .append(a.getSI());

        sIntArm.append("\n"); //$NON-NLS-1$

        // if it is a jumpship get sail and KF integrity
        if (isJumpship & !isSpaceStation) {
            Jumpship js = (Jumpship) entity;

            sIntArm.append(Messages.getString("MechView.SailIntegrity")) //$NON-NLS-1$
                    .append(js.getSailIntegrity());

            sIntArm.append("\n"); //$NON-NLS-1$

            sIntArm.append(Messages.getString("MechView.KFIntegrity")) //$NON-NLS-1$
                    .append(js.getKFIntegrity());

            sIntArm.append("\n"); //$NON-NLS-1$
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

        sIntArm.append("\n"); //$NON-NLS-1$
        // Walk through the entity's locations.
        if (!entity.isCapitalFighter()) {
            sIntArm.append(String.format("%1$-20s %2$8s\n", "", "Armor"));
            sIntArm.append("-----------------------------\n");
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

                String armor = "";
                if (IArmorState.ARMOR_NA != entity.getArmorForReal(loc)) {
                    armor = Integer.toString(entity.getOArmor(loc));
                }
                sIntArm.append(String.format("%1$-20s %2$8s\n",
                        entity.getLocationName(loc), armor));
                /*
                 * if (entity.hasPatchworkArmor()) { sIntArm.append("<td>");
                 * sIntArm.append(Messages.getString("MechView." +
                 * EquipmentType.getArmorTypeName(entity .getArmorType(loc))));
                 * sIntArm.append("</td>"); if (entity.hasBARArmor(loc)) {
                 * sIntArm.append("<td>");
                 * sIntArm.append(Messages.getString("MechView.BARRating"));
                 * //$NON-NLS-1$ sIntArm.append(entity.getBARRating(loc));
                 * sIntArm.append("</td>"); } }
                 */
            }
            sIntArm.append("\n");
        }

        return sIntArm.toString();
    }

    private String getWeapons(boolean showDetail) {

        StringBuffer sWeapons = new StringBuffer();

        if (isInf && !isBA) {
            Infantry inf = (Infantry) entity;
            sWeapons.append("Primary Weapon: ");
            if (null == inf.getPrimaryWeapon()) {
                sWeapons.append("None\n");
            } else {
                sWeapons.append(inf.getPrimaryWeapon().getDesc() + "\n");
            }
            sWeapons.append("Secondary Weapon: ");
            if ((null == inf.getSecondaryWeapon())
                    || (inf.getSecondaryN() == 0)) {
                sWeapons.append("None\n");
            } else {
                sWeapons.append(inf.getSecondaryWeapon().getDesc() + " ("
                        + inf.getSecondaryN() + ")\n");
            }
            sWeapons.append("Damage per trooper: ")
                    .append((double) Math.round(inf.getDamagePerTrooper() * 1000) / 1000)
                    .append("\n\n");
        }

        if (entity.getWeaponList().size() < 1) {
            return "";
        }
        sWeapons.append(String.format("%1$-30s %2$3s %3$5s\n", "Weapon", "Loc",
                "Heat"));
        sWeapons.append("----------------------------------------\n");
        for (Mounted mounted : entity.getWeaponList()) {
            WeaponType wtype = (WeaponType) mounted.getType();

            String wname = mounted.getDesc(); //$NON-NLS-1$
            if (entity.isClan()
                    && mounted.getType().getInternalName().substring(0, 2)
                            .equals("IS")) { //$NON-NLS-1$
                wname += Messages.getString("MechView.IS"); //$NON-NLS-1$
            }
            if (!entity.isClan()
                    && mounted.getType().getInternalName().substring(0, 2)
                            .equals("CL")) { //$NON-NLS-1$
                wname += Messages.getString("MechView.Clan"); //$NON-NLS-1$
            }
            /*
             * TODO: this should probably go in the ammo table somewhere if
             * (wtype.hasFlag(WeaponType.F_ONESHOT)) { sWeapons.append(" [")
             * //$NON-NLS-1$ .append(mounted.getLinked().getDesc()).append("]");
             * //$NON-NLS-1$ }
             */

            String loc = entity.getLocationAbbr(mounted.getLocation());
            if (mounted.isSplit()) {
                loc += "/"
                        + entity.getLocationAbbr(mounted.getSecondLocation());
            }
            int heat = wtype.getHeat();
            if (wtype instanceof BayWeapon) {
                // loop through weapons in bay and add up heat
                heat = 0;
                for (int wId : mounted.getBayWeapons()) {
                    Mounted m = entity.getEquipment(wId);
                    if (null == m) {
                        continue;
                    }
                    heat = heat + ((WeaponType) m.getType()).getHeat();
                }
            }
            sWeapons.append(String.format("%1$-30s %2$3s %3$5d\n", wname, loc,
                    heat));
        }
        sWeapons.append("\n");
        return sWeapons.toString();
    }

    private String getAmmo() {
        StringBuffer sAmmo = new StringBuffer();
        if (entity.getAmmo().size() < 1) {
            return "";
        }
        sAmmo.append(String.format("%1$-30s %2$3s %3$5s\n", "Ammo", "Loc",
                "Shots"));
        sAmmo.append("----------------------------------------\n");
        for (Mounted mounted : entity.getAmmo()) {
            sAmmo.append(String.format("%1$-30s %2$3s %3$5d\n",
                    mounted.getName(),
                    entity.getLocationAbbr(mounted.getLocation()),
                    mounted.getBaseShotsLeft()));
        }
        sAmmo.append("\n");
        return sAmmo.toString();
    }

    private String getMisc() {
        StringBuffer sMisc = new StringBuffer();
        int nEquip = 0;
        sMisc.append(String.format("%1$-30s %2$3s\n", "Equipment", "Loc"));
        sMisc.append("----------------------------------\n");
        for (Mounted mounted : entity.getMisc()) {
            String name = mounted.getName();
            if (name.contains("Jump Jet")
                    || (name.contains("CASE")
                        && !name.contains("II")
                        && entity.isClan())
                    || name.contains("Heat Sink")
                    || name.contains("Endo Steel")
                    || name.contains("Ferro-Fibrous")
                    || name.contains("Ferro-Lamellor")) {
                // These items are displayed elsewhere, so skip them here.
                continue;
            }
            nEquip++;
            String ename = mounted.getDesc();
            if (entity.isClan()
                    && mounted.getType().getInternalName().substring(0, 2)
                            .equals("IS")) { //$NON-NLS-1$
                ename += Messages.getString("MechView.IS"); //$NON-NLS-1$
            }
            if (!entity.isClan()
                    && mounted.getType().getInternalName().substring(0, 2)
                            .equals("CL")) { //$NON-NLS-1$
                ename += Messages.getString("MechView.Clan"); //$NON-NLS-1$
            }
            sMisc.append(String.format("%1$-30s %2$3s\n", ename,
                    entity.getLocationAbbr(mounted.getLocation())));
        }
        if (nEquip < 1) {
            sMisc = new StringBuffer();
        }

        String capacity = entity.getUnusedString(false);
        if ((capacity != null) && (capacity.length() > 0)) {
            sMisc.append("\n").append(Messages.getString("MechView.CarringCapacity")) //$NON-NLS-1$
                    .append(capacity); //$NON-NLS-1$
        }
        return sMisc.toString();
    }

    private String getFailed() {
        StringBuffer sFailed = new StringBuffer();
        Iterator<String> eFailed = entity.getFailedEquipment();
        if (eFailed.hasNext()) {
            sFailed.append("\nThe following equipment slots failed to load:\n"); //$NON-NLS-1$
            while (eFailed.hasNext()) {
                sFailed.append(eFailed.next()).append("\n"); //$NON-NLS-1$
            }
        }
        return sFailed.toString();
    }
}
