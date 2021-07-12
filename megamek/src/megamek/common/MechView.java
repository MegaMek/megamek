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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

import megamek.client.ui.Messages;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.PilotOptions;
import megamek.common.options.Quirks;
import megamek.common.weapons.bayweapons.BayWeapon;
import megamek.common.weapons.infantry.InfantryWeapon;

/**
 * A utility class for retrieving unit information in a formatted string.
 * 
 * The information is encoded in a series of classes that implement a common {@link ViewElement}
 * interface, which can format the element either in html or in plain text.
 * 
 */
public class MechView {
    
    /**
     * Provides common interface for various ways to present data that can be formatted
     * either as HTML or as plain text.
     * 
     * @see MechView.SingleLine
     * @see MechView.LabeledElement
     * @see MechView.TableElement
     * @see MechView.ItemList
     * @see MechView.Title
     * @see MechView.EmptyElement
     */
    interface ViewElement {
        String toPlainText();
        String toHTML();
    }
    private Entity entity;
    private boolean isMech;
    private boolean isInf;
    private boolean isBA;
    private boolean isVehicle;
    private boolean isProto;
    private boolean isGunEmplacement;
    private boolean isAero;
    private boolean isConvFighter;
    @SuppressWarnings("unused")
    private boolean isFixedWingSupport;
    private boolean isSquadron;
    private boolean isSmallCraft;
    private boolean isJumpship;
    @SuppressWarnings("unused")
    private boolean isSpaceStation;

    private List<ViewElement> sHead = new ArrayList<>();
    private List<ViewElement> sBasic = new ArrayList<>();
    private List<ViewElement> sLoadout = new ArrayList<>();
    private List<ViewElement> sFluff = new ArrayList<>();
    
    private boolean html;

    /**
     * Compiles information about an {@link Entity} useful for showing a summary of its abilities.
     * Produced output formatted in html.
     * 
     * @param entity           The entity to summarize
     * @param showDetail       If true, shows individual weapons that make up weapon bays.
     */
    public MechView(Entity entity, boolean showDetail) {
        this(entity, showDetail, false, true);
    }
    
    /**
     * Compiles information about an {@link Entity} useful for showing a summary of its abilities.
     * Produced output formatted in html.
     * 
     * @param entity           The entity to summarize
     * @param showDetail       If true, shows individual weapons that make up weapon bays.
     * @param useAlternateCost If true, uses alternate cost calculation. This primarily provides an
     *                         equipment-only cost for conventional infantry for MekHQ.
     */
    public MechView(Entity entity, boolean showDetail, boolean useAlternateCost) {
        this(entity, showDetail, useAlternateCost, true);
    }

    /**
     * Compiles information about an {@link Entity} useful for showing a summary of its abilities.
     *
     * @param entity           The entity to summarize
     * @param showDetail       If true, shows individual weapons that make up weapon bays.
     * @param useAlternateCost If true, uses alternate cost calculation. This primarily provides an
     *                         equipment-only cost for conventional infantry for MekHQ.
     * @param html             If true, produces output formatted as html. If false, formats output
     *                         as plain text.
     */
    public MechView(final Entity entity, final boolean showDetail, final boolean useAlternateCost,
                    final boolean html) {
        this(entity, showDetail, useAlternateCost, (entity.getCrew() == null), html);
    }

    /**
     * Compiles information about an {@link Entity} useful for showing a summary of its abilities.
     * 
     * @param entity           The entity to summarize
     * @param showDetail       If true, shows individual weapons that make up weapon bays.
     * @param useAlternateCost If true, uses alternate cost calculation. This primarily provides an
     *                         equipment-only cost for conventional infantry for MekHQ.
     * @param ignorePilotBV    If true then the BV calculation is done without including the pilot
     *                         BV modifiers
     * @param html             If true, produces output formatted as html. If false, formats output
     *                         as plain text.
     */
    public MechView(final Entity entity, final boolean showDetail, final boolean useAlternateCost,
                    final boolean ignorePilotBV, final boolean html) {
        this.entity = entity;
        this.html = html;
        isMech = entity instanceof Mech;
        isInf = entity instanceof Infantry;
        isBA = entity instanceof BattleArmor;
        isVehicle = entity instanceof Tank;
        isProto = entity instanceof Protomech;
        isGunEmplacement = entity instanceof GunEmplacement;
        isAero = entity instanceof Aero;
        isConvFighter = entity instanceof ConvFighter;
        isFixedWingSupport = entity instanceof FixedWingSupport;
        isSquadron = entity instanceof FighterSquadron;
        isSmallCraft = entity instanceof SmallCraft;
        isJumpship = entity instanceof Jumpship;
        isSpaceStation = entity instanceof SpaceStation;

        sLoadout.addAll(getWeapons(showDetail));
        sLoadout.add(new SingleLine());
        if ((!entity.usesWeaponBays() || !showDetail)
                && entity.getAmmo().size() > 0) {
            sLoadout.add(getAmmo());
            sLoadout.add(new SingleLine());
        }
        if (entity instanceof IBomber) {
            sLoadout.addAll(getBombs());
            sLoadout.add(new SingleLine());
        }
        sLoadout.addAll(getMisc()); // has to occur before basic is processed
        sLoadout.add(new SingleLine());
        sLoadout.add(getFailed());
        sLoadout.add(new SingleLine());

        if (isInf) {
            Infantry inf = (Infantry) entity;
            if (inf.getSpecializations() > 0) {
                ItemList specList = new ItemList("Infantry Specializations");
                for (int i = 0; i < Infantry.NUM_SPECIALIZATIONS; i++) {
                    int spec = 1 << i;
                    if (inf.hasSpecialization(spec)) {
                        specList.addItem(Infantry.getSpecializationName(spec));
                    }
                }
                sLoadout.add(specList);
            }
            
            if (inf.getCrew() != null) {
	            ArrayList<String> augmentations = new ArrayList<>();
	            for (Enumeration<IOption> e = inf.getCrew().getOptions(PilotOptions.MD_ADVANTAGES);
	            		e.hasMoreElements();) {
	            	final IOption o = e.nextElement();
	            	if (o.booleanValue()) {
	            		augmentations.add(o.getDisplayableName());
	            	}
	            }
	            if (augmentations.size() > 0) {
	                ItemList augList = new ItemList("Augmentations");
	            	for (String aug : augmentations) {
	            		augList.addItem(aug);
	            	}
	            	sLoadout.add(augList);
	            }
            }
        }
        // sBasic.append(getFluffImage(entity)).append("<br>");
        sHead.add(new Title(entity.getShortNameRaw()));
        String techLevel = entity.getStaticTechLevel().toString();
        if (entity.isMixedTech()) {
            if (entity.isClan()) {
                techLevel += Messages.getString("MechView.MixedClan");
            } else {
                techLevel += Messages.getString("MechView.MixedIS");
            }
        } else {
            if (entity.isClan()) {
                techLevel += Messages.getString("MechView.Clan");
            } else {
                techLevel += Messages.getString("MechView.IS");
            }
        }
        sHead.add(new LabeledElement(Messages.getString("MechView.BaseTechLevel"), techLevel));
        if (!entity.isDesignValid()) {
            sHead.add(new SingleLine(Messages.getString("MechView.DesignInvalid")));
        }
        
        TableElement tpTable = new TableElement(2);
        tpTable.setColNames(Messages.getString("MechView.Level"), Messages.getString("MechView.Era"));
        tpTable.setJustification(TableElement.JUSTIFIED_LEFT, TableElement.JUSTIFIED_CENTER);
        tpTable.addRow(TechConstants.getSimpleLevelName(TechConstants.T_SIMPLE_EXPERIMENTAL),
                entity.getExperimentalRange());
        tpTable.addRow(TechConstants.getSimpleLevelName(TechConstants.T_SIMPLE_ADVANCED),
                entity.getAdvancedRange());
        tpTable.addRow(TechConstants.getSimpleLevelName(TechConstants.T_SIMPLE_STANDARD),
                entity.getStandardRange());
        String extinctRange = entity.getExtinctionRange();
        if (extinctRange.length() > 1) {
            tpTable.addRow(Messages.getString("MechView.Extinct"), extinctRange);
        }
        sHead.add(tpTable);
            
        sHead.add(new LabeledElement(Messages.getString("MechView.TechRating"), entity.getFullRatingName()));
        sHead.add(new SingleLine());

        if (!isInf) {
            sHead.add(new LabeledElement(Messages.getString("MechView.Weight"), //$NON-NLS-1$
                    Math.round(entity.getWeight()) + Messages.getString("MechView.tons"))); //$NON-NLS-1$
        }
        DecimalFormatSymbols unusualSymbols = new DecimalFormatSymbols();
        unusualSymbols.setDecimalSeparator('.');
        unusualSymbols.setGroupingSeparator(',');
        DecimalFormat dFormatter = new DecimalFormat("#,###.##", unusualSymbols); //$NON-NLS-1$
        sHead.add(new LabeledElement(Messages.getString("MechView.BV"),
                dFormatter.format(entity.calculateBattleValue(false, ignorePilotBV))));
        double cost = entity.getCost(false);
        if(useAlternateCost && entity.getAlternateCost() > 0) {
            cost = entity.getAlternateCost();
        }
        sHead.add(new LabeledElement(Messages.getString("MechView.Cost"), //$NON-NLS-1$//
                dFormatter.format(cost) + " C-bills"));
        if (!entity.getSource().isEmpty()) {
            sHead.add(new LabeledElement(Messages.getString("MechView.Source"), entity.getSource())); //$NON-NLS-1$//
        }
        UnitRole role = UnitRoleHandler.getRoleFor(entity);
        if (role != UnitRole.UNDETERMINED) {
            sHead.add(new LabeledElement("Role", role.toString()));
        }

        //We may have altered the starting mode during configuration, so we save the current one here to restore it
        int originalMode = entity.getConversionMode();
        entity.setConversionMode(0);
        if (!isGunEmplacement) {
            sBasic.add(new SingleLine());
            StringBuilder moveString = new StringBuilder();
            moveString.append(entity.getWalkMP()).append("/")
                .append(entity.getRunMPasString());
            if (entity.getJumpMP() > 0) {
                moveString.append("/") //$NON-NLS-1$
                        .append(entity.getJumpMP());
            }
            if (entity.damagedJumpJets() > 0) {
                moveString.append("<font color='red'> (").append(entity.damagedJumpJets())
                        .append(" damaged jump jets)</font>");
            }
            if (entity.getAllUMUCount() > 0) {
                // Add in Jump MP if it wasn't already printed
                if (entity.getJumpMP() == 0) {
                    moveString.append("/0"); //$NON-NLS-1$
                }
                moveString.append("/") //$NON-NLS-1$
                        .append(entity.getActiveUMUCount());
                if ((entity.getAllUMUCount() - entity.getActiveUMUCount()) != 0) {
                    moveString.append("<font color='red'> (")
                            .append(entity.getAllUMUCount() - entity.getActiveUMUCount())
                            .append(" damaged UMUs)</font>");
                }
            }
            if (isVehicle) {
                moveString.append(" (").append(Messages
                            .getString("MovementType." + entity.getMovementModeAsString()))
                            .append(")");
                if ((((Tank) entity).getMotiveDamage() > 0)
                        || (((Tank) entity).getMotivePenalty() > 0)) {
                    moveString.append(" ").append(warningStart())
                        .append("(motive damage: -")
                        .append(((Tank) entity).getMotiveDamage())
                        .append("MP/-")
                        .append(((Tank) entity).getMotivePenalty())
                        .append(" piloting)")
                        .append(warningEnd());
                }
            }

            // TODO : Add STOL message as part of the movement line
            if (isConvFighter && ((Aero) entity).isVSTOL()) {
                sBasic.add(new LabeledElement(Messages.getString("MechView.Movement"),
                        moveString.toString().concat(
                                String.format(" (%s)", Messages.getString("MechView.VSTOL"))))); //$NON-NLS-1$
            } else {
                sBasic.add(new LabeledElement(Messages.getString("MechView.Movement"), moveString.toString())); //$NON-NLS-1$
            }
        }
        if (isBA && ((BattleArmor) entity).isBurdened()) {
            sBasic.add(new SingleLine(italicsStart()
                    + Messages.getString("MechView.Burdened")
                    + italicsEnd())); //$NON-NLS-1$
        }
        if (isBA && ((BattleArmor) entity).hasDWP()) {
            sBasic.add(new SingleLine(italicsStart()
                    + Messages.getString("MechView.DWPBurdened")
                    + italicsEnd())); //$NON-NLS-1$
        }
        if (entity instanceof QuadVee) {
            entity.setConversionMode(QuadVee.CONV_MODE_VEHICLE);
            sBasic.add(new LabeledElement(Messages.getString("MovementType." //$NON-NLS-1$
                    + entity.getMovementModeAsString()),
                    entity.getWalkMP() + "/" + entity.getRunMPasString())); //$NON-NLS-1$
            entity.setConversionMode(originalMode);
        } else if (entity instanceof LandAirMech) {
            if (((LandAirMech)entity).getLAMType() == LandAirMech.LAM_STANDARD) {
                sBasic.add(new LabeledElement(Messages.getString("MovementType.AirMech"), //$NON-NLS-1$
                        ((LandAirMech) entity).getAirMechWalkMP() + "/"
                                + ((LandAirMech) entity).getAirMechRunMP() + "/"
                                + ((LandAirMech) entity).getAirMechCruiseMP() + "/"
                                + ((LandAirMech) entity).getAirMechFlankMP()));
            }

            entity.setConversionMode(LandAirMech.CONV_MODE_FIGHTER);
            sBasic.add(new LabeledElement(Messages.getString("MovementType.Fighter"), //$NON-NLS-1$
                    entity.getWalkMP() + "/" + entity.getRunMP())); //$NON-NLS-1$
            entity.setConversionMode(originalMode);
        }

        if (isMech || isVehicle
                || (isAero && !isSmallCraft && !isJumpship && !isSquadron)) {
            String engineName = entity.hasEngine() ? entity.getEngine().getShortEngineName() : "(none)";
            if (entity.getEngineHits() > 0) {
                engineName += " " + warningStart() + "(" + entity.getEngineHits()
                        + " hits)" + warningEnd();
            }
            if (isMech && entity.hasArmoredEngine()) {
                engineName += " (armored)";
            }
            sBasic.add(new LabeledElement(Messages.getString("MechView.Engine"), engineName)); //$NON-NLS-1$
        }
        if (!entity.hasPatchworkArmor() && entity.hasBARArmor(1)) {
            sBasic.add(new LabeledElement(Messages.getString("MechView.BARRating"), //$NON-NLS-1$
                    String.valueOf(entity.getBARRating(0))));
        }

        if (isAero && !isConvFighter) {
            Aero a = (Aero) entity;
            StringBuilder hsString = new StringBuilder(String.valueOf(a.getHeatSinks()));
            if (a.getPodHeatSinks() > 0) {
                hsString.append(" (").append(a.getPodHeatSinks()).append(" ")
                    .append(Messages.getString("MechView.Pod")).append(")"); //$NON-NLS-1$
            }
            if (a.getHeatCapacity() > a.getHeatSinks()) {
                hsString.append(" [") //$NON-NLS-1$
                        .append(a.getHeatCapacity()).append("]"); //$NON-NLS-1$
            }
            if (a.getHeatSinkHits() > 0) {
                hsString.append(warningStart()).append(" (").append(a.getHeatSinkHits())
                        .append(" damaged)").append(warningEnd());
            }
            sBasic.add(new LabeledElement(Messages.getString("MechView.HeatSinks"), hsString.toString())); //$NON-NLS-1$
            
            if (a.getCockpitType() != Mech.COCKPIT_STANDARD) {
                sBasic.add(new LabeledElement(Messages.getString("MechView.Cockpit"), // $NON-NLS-1$
                        a.getCockpitTypeString()));
            }
        }

        if (isMech) {
            Mech aMech = (Mech) entity;
            StringBuilder hsString = new StringBuilder();
            hsString.append(aMech.heatSinks());
            if (aMech.getHeatCapacity() > aMech.heatSinks()) {
                hsString.append(" [") //$NON-NLS-1$
                        .append(aMech.getHeatCapacity()).append("]"); //$NON-NLS-1$
            }
            if (aMech.damagedHeatSinks() > 0) {
                hsString.append(" ").append(warningStart()).append("(")
                        .append(aMech.damagedHeatSinks())
                        .append(" damaged)").append(warningEnd());
            }
            sBasic.add(new LabeledElement(aMech.getHeatSinkTypeName() + "s", hsString.toString()));
            if ((aMech.getCockpitType() != Mech.COCKPIT_STANDARD)
                    || aMech.hasArmoredCockpit()) {
                sBasic.add(new LabeledElement(Messages.getString("MechView.Cockpit"),
                        aMech.getCockpitTypeString()
                                + (aMech.hasArmoredCockpit() ? " (armored)" : "")));
                                
            }
            String gyroString = aMech.getGyroTypeString();
            if (aMech.getGyroHits() > 0) {
                gyroString += " " + warningStart() + "(" + aMech.getGyroHits()
                    + " hits)" + warningEnd();
            }
            if (aMech.hasArmoredGyro()) {
                gyroString += " (armored)";
            }
            sBasic.add(new LabeledElement(Messages.getString("MechView.Gyro"), //$NON-NLS-1$
                    gyroString));
        }

        if (isAero) {
            Aero a = (Aero) entity;
            if (!a.getCritDamageString().isEmpty()) {
                sBasic.add(new LabeledElement(Messages.getString("MechView.SystemDamage"), //$NON-NLS-1$
                        warningStart() + a.getCritDamageString() + warningEnd()));
            }
            
            String fuel = String.valueOf(a.getCurrentFuel());
            if (a.getCurrentFuel() < a.getFuel()) {
                fuel += "/" + a.getFuel(); //$NON-NLS-1$
            }
            sBasic.add(new LabeledElement(Messages.getString("MechView.FuelPoints"), //$NON-NLS-1$
                    String.format(Messages.getString("MechView.Fuel.format"), fuel, a.getFuelTonnage()))); //$NON-NLS-1$

            //Display Strategic Fuel Use for Small Craft and up
            if (isSmallCraft || isJumpship) {
                sBasic.add(new LabeledElement(Messages.getString("MechView.TonsPerBurnDay"), //$NON-NLS-1$
                        String.format("%2.2f", a.getStrategicFuelUse()))); //$NON-NLS-1$
            }
        }
        if (!isGunEmplacement) {
            sBasic.add(new SingleLine());
            if (isSquadron) {
                sBasic.addAll(getArmor());
            } else if (isAero) {
                sBasic.addAll(getSIandArmor());
            } else {
                sBasic.addAll(getInternalAndArmor());
            }
        }
        sBasic.add(new SingleLine());
        
        StringJoiner quirksList = new StringJoiner("<br/>\n");
        Quirks quirks = entity.getQuirks();
        for (Enumeration<IOptionGroup> optionGroups = quirks.getGroups(); optionGroups.hasMoreElements();) {
            IOptionGroup group = optionGroups.nextElement();
            if (quirks.count(group.getKey()) > 0) {
                for (Enumeration<IOption> options = group.getOptions(); options.hasMoreElements();) {
                    IOption option = options.nextElement();
                    if (option != null && option.booleanValue()) {
                        quirksList.add(option.getDisplayableNameWithValue());
                    }
                }
            }
        }
        if (quirksList.length() > 0) {
            ItemList list = new ItemList(Messages.getString("MechView.Quirks")); //$NON-NLS-1$
            list.addItem(quirksList.toString());
            sFluff.add(list);
        }
        
        if (!entity.getFluff().getOverview().isEmpty()) {
            sFluff.add(new LabeledElement("Overview", entity.getFluff().getOverview()));
        }
        
        if (!entity.getFluff().getCapabilities().isEmpty()) {
            sFluff.add(new LabeledElement("Capabilities", entity.getFluff().getCapabilities()));
        }
        
        if (!entity.getFluff().getDeployment().isEmpty()) {
            sFluff.add(new LabeledElement("Deployment", entity.getFluff().getDeployment()));
        }
        
        if (!entity.getFluff().getHistory().isEmpty()) {
            sFluff.add(new LabeledElement("History", entity.getFluff().getHistory()));
        }
        sFluff.add(new SingleLine());
    }
    
    /**
     * Converts a list of {@link ViewElement}s to a String using the selected format.
     * 
     * @param section The elements to format.
     * @return        The formatted data.
     */
    private String getReadout(List<ViewElement> section) {
        Function<ViewElement,String> mapper = html?
                ViewElement::toHTML : ViewElement::toPlainText;
        return section.stream().map(mapper).collect(Collectors.joining());
    }
    
    /**
     * The head section includes the title (unit name), tech level and availability, tonnage, bv, and cost.
     * @return The data from the head section.
     */
    public String getMechReadoutHead() {
        return getReadout(sHead);
    }

    /**
     * The basic section includes general details such as movement, system equipment (cockpit, gyro, etc.)
     * and armor.
     * @return The data from the basic section
     */
    public String getMechReadoutBasic() {
        return getReadout(sBasic);
    }

    /**
     * The loadout includes weapons, ammo, and other equipment broken down by location.
     * @return The data from the loadout section.
     */
    public String getMechReadoutLoadout() {
        return getReadout(sLoadout);
    }

    /**
     * The fluff section includes fluff details like unit history and deployment patterns
     * as well as quirks.
     * @return The data from the fluff section.
     */
    public String getMechReadoutFluff() {
        return getReadout(sFluff);
    }
    
    /**
     * @return A summary including all four sections. 
     */
    public String getMechReadout() {
        String docStart = html?
                "<div style=\"font:12pt monospaced\">" : "";
        String docEnd = html?
                "</div>" : "";
        return docStart + getMechReadoutHead()
                + getMechReadoutBasic() + getMechReadoutLoadout()
                + getMechReadoutFluff() + docEnd;
    }

    private List<ViewElement> getInternalAndArmor() {
        List<ViewElement> retVal = new ArrayList<>();
        
        int maxArmor = (entity.getTotalInternal() * 2) + 3;
        if (isInf && !isBA) {
            Infantry inf = (Infantry) entity;
            retVal.add(new LabeledElement(Messages.getString("MechView.Men"),
                    entity.getTotalInternal()
                            + " (" + inf.getSquadSize() + "/" + inf.getSquadN()
                            + ")"));
        } else {
            String internal = String.valueOf(entity.getTotalInternal());
            if (isMech) {
                internal += Messages.getString("MechView."
                        + EquipmentType.getStructureTypeName(entity
                                .getStructureType()));
            }
            retVal.add(new LabeledElement(Messages.getString("MechView.Internal"), //$NON-NLS-1$
                    internal));
        }

        if (isInf && !isBA) {
            Infantry inf = (Infantry) entity;
            retVal.add(new LabeledElement(Messages.getString("MechView.Armor"),
                    inf.getArmorDesc()));
        } else {
            String armor = String.valueOf(entity.getTotalArmor());
            if (isMech) {
                armor += "/" + maxArmor;//$NON-NLS-1$
            }
            if (!isInf && !isProto && !entity.hasPatchworkArmor()) {
                armor += Messages.getString("MechView."
                        + EquipmentType.getArmorTypeName(entity.getArmorType(1))
                                .trim());
            }
            if (isBA) {
                armor += " " + EquipmentType.getBaArmorTypeName(entity.getArmorType(1))
                                .trim();
            }
            retVal.add(new LabeledElement(Messages.getString("MechView.Armor"), //$NON-NLS-1$
                    armor));

        }
        // Walk through the entity's locations.

        if (!(isInf && !isBA)) {
            TableElement locTable = new TableElement(5);
            locTable.setColNames("", "Internal", "Armor", "", ""); // last two columns are patchwork armor and location damage
            locTable.setJustification(TableElement.JUSTIFIED_LEFT, TableElement.JUSTIFIED_RIGHT,
                    TableElement.JUSTIFIED_RIGHT, TableElement.JUSTIFIED_LEFT, TableElement.JUSTIFIED_LEFT);
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
                String[] row = {entity.getLocationName(loc),
                        renderArmor(entity.getInternalForReal(loc), entity.getOInternal(loc), html),
                        "", "", "" };
                
                if (IArmorState.ARMOR_NA != entity.getArmorForReal(loc)) {
                    row[2] = renderArmor(entity.getArmorForReal(loc),
                            entity.getOArmor(loc), html);
                }
                if (entity.hasPatchworkArmor()) {
                    row[3] = Messages.getString("MechView."
                            + EquipmentType.getArmorTypeName(entity
                                    .getArmorType(loc)).trim());
                    if (entity.hasBARArmor(loc)) {
                        row[3] += " " + Messages.getString("MechView.BARRating") //$NON-NLS-1$
                                + entity.getBARRating(loc);
                    }
                }
                if (!entity.getLocationDamage(loc).isEmpty()) {
                    row[4] = warningStart() + entity.getLocationDamage(loc) + warningEnd();
                }
                locTable.addRow(row);
                if (entity.hasRearArmor(loc)) {
                    row = new String[] { entity.getLocationName(loc) + " (rear)", "",
                            renderArmor(entity.getArmorForReal(loc, true),
                                    entity.getOArmor(loc, true), html), "", ""};
                    locTable.addRow(row);
                }
            }
            retVal.add(locTable);
        }
        return retVal;
    }

    private List<ViewElement> getSIandArmor() {
        Aero a = (Aero) entity;

        List<ViewElement> retVal = new ArrayList<>();

        retVal.add(new LabeledElement(Messages.getString("MechView.SI"), //$NON-NLS-1$
                renderArmor(a.getSI(), a.get0SI(), html)));

        // if it is a jumpship get sail and KF integrity
        if (isJumpship) {
            Jumpship js = (Jumpship) entity;

            // TODO: indicate damage.
            if (js.hasSail()) {
                retVal.add(new LabeledElement(Messages.getString("MechView.SailIntegrity"), //$NON-NLS-1$
                        String.valueOf(js.getSailIntegrity())));
            }

            if (js.getDriveCoreType() != Jumpship.DRIVE_CORE_NONE) {
                retVal.add(new LabeledElement(Messages.getString("MechView.KFIntegrity"), //$NON-NLS-1$
                        String.valueOf(js.getKFIntegrity())));
            }
        }

        String armor = String.valueOf(entity.isCapitalFighter()? a.getCapArmor() : a.getTotalArmor());
        if (isJumpship) {
            armor += Messages.getString("MechView.CapitalArmor");
        }
        if (!entity.hasPatchworkArmor()) {
            armor += Messages.getString("MechView."
                    + EquipmentType.getArmorTypeName(entity.getArmorType(1))
                            .trim());
        }
        retVal.add(new LabeledElement(Messages.getString("MechView.Armor"), //$NON-NLS-1$
                armor));

        // Walk through the entity's locations.
        if (!entity.isCapitalFighter()) {
            TableElement locTable = new TableElement(3);
            locTable.setColNames("", "Armor", "");
            locTable.setJustification(TableElement.JUSTIFIED_LEFT, TableElement.JUSTIFIED_RIGHT,
                    TableElement.JUSTIFIED_LEFT);
            for (int loc = 0; loc < entity.locations(); loc++) {

                // Skip empty sections.
                if (IArmorState.ARMOR_NA == entity.getInternal(loc)) {
                    continue;
                }
                // skip broadsides on warships
                if (isJumpship && (loc >= Jumpship.LOC_HULL)) {
                    continue;
                }
                if (isSmallCraft && (loc >= SmallCraft.LOC_HULL)) {
                    continue;
                }
                // skip the "Wings" location
                if (!a.isLargeCraft() && (loc >= Aero.LOC_WINGS)) {
                    continue;
                }
                String[] row = { entity.getLocationName(loc), "", "" };
                if (IArmorState.ARMOR_NA != entity.getArmor(loc)) {
                    row[1] = renderArmor(entity.getArmor(loc),
                            entity.getOArmor(loc), html);
                }
                if (entity.hasPatchworkArmor()) {
                    row[2] = Messages.getString("MechView."
                            + EquipmentType.getArmorTypeName(entity
                                    .getArmorType(loc)).trim());
                    if (entity.hasBARArmor(loc)) {
                        row[2] += Messages.getString("MechView.BARRating") //$NON-NLS-1$
                                + entity.getBARRating(loc);
                    }
                }
                locTable.addRow(row);
            }
            retVal.add(locTable);
        }

        return retVal;
    }

    private List<ViewElement> getArmor() {
        FighterSquadron fs = (FighterSquadron) entity;

        List<ViewElement> retVal = new ArrayList<>();

        retVal.add(new LabeledElement(Messages.getString("MechView.Armor"), //$NON-NLS-1$
                String.valueOf(fs.getTotalArmor())));

        retVal.add(new LabeledElement(Messages.getString("MechView.ActiveFighters"), //$NON-NLS-1$
                String.valueOf(fs.getActiveSubEntities().size())));

        return retVal;
    }

    private List<ViewElement> getWeapons(boolean showDetail) {

        List<ViewElement> retVal = new ArrayList<>();

        if (isInf && !isBA) {
            Infantry inf = (Infantry) entity;
            retVal.add(new LabeledElement("Primary Weapon",
                    (null != inf.getPrimaryWeapon()) ?
                            inf.getPrimaryWeapon().getDesc() : "None"));
            retVal.add(new LabeledElement("Secondary Weapon",
                    (null != inf.getSecondaryWeapon()) ?
                            inf.getSecondaryWeapon().getDesc()
                                    + " (" + inf.getSecondaryN() + ")" : "None"));
            retVal.add(new LabeledElement("Damage per trooper",
                    String.format("%3.3f", inf.getDamagePerTrooper())));
            retVal.add(new SingleLine());
        }

        if (entity.getWeaponList().size() < 1) {
            return retVal;
        }
        
        TableElement wpnTable = new TableElement(4);
        wpnTable.setColNames("Weapons", "Loc", "Heat", entity.isOmni()? "Omni" : "");
        wpnTable.setJustification(TableElement.JUSTIFIED_LEFT, TableElement.JUSTIFIED_CENTER,
                TableElement.JUSTIFIED_CENTER, TableElement.JUSTIFIED_LEFT);
        for (Mounted mounted : entity.getWeaponList()) {
            String[] row = { mounted.getDesc(), entity.joinLocationAbbr(mounted.allLocations(), 3), "", "" };
            WeaponType wtype = (WeaponType) mounted.getType();

            if (entity.isClan()
                    && (mounted.getType().getTechBase() == ITechnology.TECH_BASE_IS)) {
                row[0] += Messages.getString("MechView.IS"); //$NON-NLS-1$
            }
            if (!entity.isClan()
                    && (mounted.getType().getTechBase() == ITechnology.TECH_BASE_CLAN)) {
                row[0] += Messages.getString("MechView.Clan"); //$NON-NLS-1$
            }
            /*
             * TODO: this should probably go in the ammo table somewhere if
             * (wtype.hasFlag(WeaponType.F_ONESHOT)) { sWeapons.append(" [")
             * //$NON-NLS-1$ .append(mounted.getLinked().getDesc()).append("]");
             * //$NON-NLS-1$ }
             */

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
                    heat = heat + m.getType().getHeat();
                    if(m.isDestroyed()) {
                        bWeapDamaged++;
                    }
                }
            }
            row[2] = String.valueOf(heat);
            
            if (entity.isOmni()) {
                row[3] = mounted.isOmniPodMounted()?
            		Messages.getString("MechView.Pod") : //$NON-NLS-1$
            		Messages.getString("MechView.Fixed"); //$NON-NLS-1$
            } else if(wtype instanceof BayWeapon && bWeapDamaged > 0 && !showDetail) {
                row[3] = warningStart() + Messages.getString("MechView.WeaponDamage")
                    + ")" + warningEnd();
            }
            if (mounted.isDestroyed()) {
                if (mounted.isRepairable()) {
                    wpnTable.addRowWithBgColor("yellow", row);
                } else {
                    wpnTable.addRowWithBgColor("red", row);
                }
            } else {
                wpnTable.addRow(row);
            }

            // if this is a weapon bay, then cycle through weapons and ammo           
            if((wtype instanceof BayWeapon) && showDetail) { 
                for(int wId : mounted.getBayWeapons()) { 
                    Mounted m = entity.getEquipment(wId);
                    if(null == m) { 
                        continue; 
                    }
                    
                    row = new String[] { m.getDesc(), "", "", "" };
                      
                    if (entity.isClan()
                            && (mounted.getType().getTechBase() == ITechnology.TECH_BASE_IS)) {
                        row[0] += Messages.getString("MechView.IS"); //$NON-NLS-1$
                    }
                    if (!entity.isClan()
                            && (mounted.getType().getTechBase() == ITechnology.TECH_BASE_CLAN)) {
                        row[0] += Messages.getString("MechView.Clan"); //$NON-NLS-1$
                    }
                    if (m.isDestroyed()) {
                        if (m.isRepairable()) {
                            wpnTable.addRowWithBgColor("yellow", row);
                        } else {
                            wpnTable.addRowWithBgColor("red", row);
                        }
                    } else {
                        wpnTable.addRow(row);
                    }
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
                    if (mounted.getLocation() != Entity.LOC_NONE) {
                        row = new String[] { m.getName(), String.valueOf(m.getBaseShotsLeft()), "", "" };
                        if (m.isDestroyed()) {
                            wpnTable.addRowWithBgColor("red", row);
                        } else if (m.getUsableShotsLeft() < 1) {
                            wpnTable.addRowWithBgColor("yellow", row);
                        } else {
                            wpnTable.addRow(row);
                        }
                    }
                }
            }
        }
        retVal.add(wpnTable);
        return retVal;
    }

    private ViewElement getAmmo() {
        TableElement ammoTable = new TableElement(4);
        ammoTable.setColNames("Ammo", "Loc", "Shots", entity.isOmni()? "Omni" : "");
        ammoTable.setJustification(TableElement.JUSTIFIED_LEFT, TableElement.JUSTIFIED_CENTER,
                TableElement.JUSTIFIED_RIGHT, TableElement.JUSTIFIED_LEFT);

        for (Mounted mounted : entity.getAmmo()) {
            // Ignore ammo for one-shot launchers
            if ((mounted.getLinkedBy() != null)
                    && mounted.getLinkedBy().isOneShot()){
                continue;
            }
            // Ignore bay ammo bins for unused munition types
            if (mounted.getSize() == 0) {
                continue;
            }
            
            if (mounted.getLocation() == Entity.LOC_NONE) {
                continue;
            }

            String[] row = { mounted.getName(), entity.getLocationAbbr(mounted.getLocation()),
                    String.valueOf(mounted.getBaseShotsLeft()), "" };
            if (entity.isOmni()) {
                row[3] = mounted.isOmniPodMounted()?
                        Messages.getString("MechView.Pod") : //$NON-NLS-1$
                            Messages.getString("MechView.Fixed"); //$NON-NLS-1$
            }

            if (mounted.isDestroyed()) {
                ammoTable.addRowWithBgColor("red", row);
            } else if (mounted.getUsableShotsLeft() < 1) {
                ammoTable.addRowWithBgColor("yellow", row);
            } else {
                ammoTable.addRow(row);
            }
        }
        if (entity.getWeightClass() == EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
            for (Mounted mounted : entity.getWeaponList()) {
                String[] row = {mounted.getName(),
                        entity.getLocationAbbr(mounted.getLocation()),
                        String.valueOf((int) mounted.getSize() * ((InfantryWeapon) mounted.getType()).getShots()),
                        ""};
                if (entity.isOmni()) {
                    row[3] = mounted.isOmniPodMounted() ?
                            Messages.getString("MechView.Pod") : //$NON-NLS-1$
                            Messages.getString("MechView.Fixed"); //$NON-NLS-1$
                }
                int shotsLeft = 0;
                for (Mounted current = mounted.getLinked(); current != null; current = current.getLinked()) {
                    shotsLeft += current.getUsableShotsLeft();
                }
                if (mounted.isDestroyed()) {
                    ammoTable.addRowWithBgColor("red", row);
                } else if (shotsLeft < 1) {
                    ammoTable.addRowWithBgColor("yellow", row);
                } else {
                    ammoTable.addRow(row);
                }
            }
        }
        return ammoTable;
    }
    
    

    private List<ViewElement> getBombs() {
        List<ViewElement> retVal = new ArrayList<>();
        IBomber b = (IBomber) entity;
        int[] choices = b.getBombChoices();
        for (int type = 0; type < BombType.B_NUM; type++) {
            if (choices[type] > 0) {
                retVal.add(new SingleLine(BombType.getBombName(type) + " ("
                        + choices[type] + ")"));
            }
        }
        return retVal;
    }

    private List<ViewElement> getMisc() {
        List<ViewElement> retVal = new ArrayList<>();
        
        TableElement miscTable = new TableElement(3);
        miscTable.setColNames("Equipment", "Loc", entity.isOmni()? "Omni" : "");
        miscTable.setJustification(TableElement.JUSTIFIED_LEFT, TableElement.JUSTIFIED_CENTER,
                TableElement.JUSTIFIED_LEFT);
        int nEquip = 0;
        for (Mounted mounted : entity.getMisc()) {
            String name = mounted.getName();
            if ((((mounted.getLocation() == Entity.LOC_NONE)
                        // Mechs can have zero-slot equipment in LOC_NONE that needs to be shown.
                        && (!isMech || mounted.getCriticals() > 0)))
                    || name.contains("Jump Jet")
                    || (name.contains("CASE")
                        && !name.contains("II")
                        && entity.isClan())
                    || (name.contains("Heat Sink") 
                        && !name.contains("Radical"))
                    || EquipmentType.isArmorType(mounted.getType())
                    || EquipmentType.isStructureType(mounted.getType())) {
                // These items are displayed elsewhere, so skip them here.
                continue;
            }
            nEquip++;
            
            String[] row = { mounted.getDesc(), entity.joinLocationAbbr(mounted.allLocations(), 3), "" };
            if (entity.isClan()
                    && (mounted.getType().getTechBase() == ITechnology.TECH_BASE_IS)) {
                row[0] += Messages.getString("MechView.IS"); //$NON-NLS-1$
            }
            if (!entity.isClan()
                    && (mounted.getType().getTechBase() == ITechnology.TECH_BASE_CLAN)) {
                row[0] += Messages.getString("MechView.Clan"); //$NON-NLS-1$
            }
            
            if (entity.isOmni()) {
                row[2] = mounted.isOmniPodMounted()?
                        Messages.getString("MechView.Pod") : //$NON-NLS-1$
                            Messages.getString("MechView.Fixed"); //$NON-NLS-1$
            }
            if (mounted.isDestroyed()) {
                miscTable.addRowWithBgColor("red", row);
            } else {
                miscTable.addRow(row);
            }
        }
        if (nEquip > 0) {
            retVal.add(miscTable);
        }

        retVal.add(new SingleLine());
        String capacity = entity.getUnusedString(html);
        if ((capacity != null) && (capacity.length() > 0)) {
            // The entries have already been formatted into a list, but we're still going to
            // format it as a list to get the items under the label.
            ItemList list = new ItemList(Messages.getString("MechView.CarryingCapacity")); //$NON-NLS-1$
            list.addItem(capacity);
            retVal.add(list);
            retVal.add(new SingleLine());
        }
        
        if (isSmallCraft || isJumpship) {
            Aero a = (Aero)entity;
            
            TableElement crewTable = new TableElement(2);
            crewTable.setColNames(Messages.getString("MechView.Crew"), "");
            crewTable.setJustification(TableElement.JUSTIFIED_LEFT, TableElement.JUSTIFIED_RIGHT);
            crewTable.addRow(Messages.getString("MechView.Officers"), String.valueOf(a.getNOfficers())); // $NON-NLS-1$
            crewTable.addRow(Messages.getString("MechView.Enlisted"), //$NON-NLS-1$
                    String.valueOf(Math.max(a.getNCrew()
                    - a.getBayPersonnel() - a.getNGunners() - a.getNOfficers(), 0)));
            crewTable.addRow(Messages.getString("MechView.Gunners"), String.valueOf(a.getNGunners())); // $NON-NLS-1$
            crewTable.addRow(Messages.getString("MechView.BayPersonnel"), String.valueOf(a.getBayPersonnel())); // $NON-NLS-1$
            if (a.getNPassenger() > 0) {
                crewTable.addRow(Messages.getString("MechView.Passengers"), String.valueOf(a.getNPassenger())); // $NON-NLS-1$
            }
            if (a.getNMarines() > 0) {
                crewTable.addRow(Messages.getString("MechView.Marines"), String.valueOf(a.getNMarines())); // $NON-NLS-1$
            }
            if (a.getNBattleArmor() > 0) {
                crewTable.addRow(Messages.getString("MechView.BAMarines"), String.valueOf(a.getNBattleArmor())); // $NON-NLS-1$
            }
            retVal.add(crewTable);
        }
        if (isVehicle && ((Tank) entity).getExtraCrewSeats() > 0) {
            retVal.add(new SingleLine(Messages.getString("MechView.ExtraCrewSeats")
                    + ((Tank) entity).getExtraCrewSeats()));
        }
        return retVal;
    }

    private ViewElement getFailed() {
        Iterator<String> eFailed = entity.getFailedEquipment();
        if (eFailed.hasNext()) {
            ItemList list = new ItemList("The following equipment slots failed to load:");
            while (eFailed.hasNext()) {
                list.addItem(eFailed.next());
            }
            return list;
        }
        return new EmptyElement();
    }

    private static String renderArmor(int nArmor, int origArmor, boolean html) {
        double percentRemaining = ((double) nArmor) / ((double) origArmor);
        String armor = Integer.toString(nArmor);
        if (!html) {
            if (percentRemaining < 0) {
                return "X";
            } else {
                return armor;
            }
        }
        if (percentRemaining < 0) {
            return "<span style='color:white;background-color:black'>X</span>";
        } else if (percentRemaining <= .25) {
            return "<span style='color:white;background-color:red'>" + armor + "</span>";
        } else if (percentRemaining <= .75) {
            return "<span style='color:black;background-color:yellow'>" + armor + "</span>";
        } else if (percentRemaining < 1.00) {
            return "<span style='color:black;background-color:green'>" + armor + "</span>";
        } else {
            return "<span style='color:black;background-color:white'>" + armor + "</span>";
        }
    }

    /**
     * Used when an element is expected but the unit has no data for it. Outputs an empty string.
     */
    private static class EmptyElement implements ViewElement {

        @Override
        public String toPlainText() {
            return "";
        }

        @Override
        public String toHTML() {
            return "";
        }
        
    }
    
    /**
     * Basic one-line entry consisting of a label, a colon, and a value. In html the label is bold.
     *
     */
    private static class LabeledElement implements ViewElement {
        private final String label;
        private final String value;
        
        LabeledElement(String label, String value) {
            this.label = label;
            this.value = value;
        }
        
        @Override
        public String toPlainText() {
            return label + ": " + value + "\n";
        }
        
        @Override
        public String toHTML() {
            return "<b>" + label + "</b>: " + value + "<br/>\n";
        }
    }
    
    /**
     * Data laid out in a table with named columns. The columns are left-justified by default,
     * but justification can be set for columns individually. Plain text output requires a monospace
     * font to line up correctly. For HTML output the background color of an individual row can be set.
     *
     */
    private static class TableElement implements ViewElement {
        
        static final int JUSTIFIED_LEFT   = 0;
        static final int JUSTIFIED_CENTER = 1;
        static final int JUSTIFIED_RIGHT  = 2;
        
        private final int[] justification;
        private final String[] colNames;
        private final List<String[]> data = new ArrayList<>();
        private final Map<Integer,Integer> colWidth = new HashMap<>();
        private final Map<Integer,String> bgColors = new HashMap<>();
        
        TableElement(int colCount) {
            justification = new int[colCount];
            colNames = new String[colCount];
            Arrays.fill(colNames, "");
        }
        
        void setColNames(String... colNames) {
            Arrays.fill(this.colNames, "");
            System.arraycopy(colNames, 0, this.colNames, 0,
                    Math.min(colNames.length, this.colNames.length));
            colWidth.clear();
            for (int i = 0; i < colNames.length; i++) {
                colWidth.put(i, colNames[i].length());
            }
        }
        
        void setJustification(int... justification) {
            Arrays.fill(this.justification, JUSTIFIED_LEFT);
            System.arraycopy(justification, 0, this.justification, 0,
                    Math.min(justification.length, this.justification.length));
        }
        
        void addRow(String... row) {
            data.add(row);
            for (int i = 0; i < row.length; i++) {
                colWidth.merge(i, row[i].length(), Math::max);
            }
        }
        
        void addRowWithBgColor(String color, String... row) {
            addRow(row);
            bgColors.put(data.size() - 1, color);
        }
        
        private String leftPad(String s, int fieldSize) {
            if (fieldSize > 0) {
                return String.format("%" + fieldSize + "s", s);
            } else {
                return "";
            }
        }
        
        private String rightPad(String s, int fieldSize) {
            if (fieldSize > 0) {
                return String.format("%-" + fieldSize + "s", s);
            } else {
                return "";
            }
        }
        
        private String center(String s, int fieldSize) {
            int rightPadding = Math.max(fieldSize - s.length(), 0) / 2;
            return rightPad(leftPad(s, fieldSize - rightPadding), fieldSize);
        }
        
        private String justify(int justification, String s, int fieldSize) {
            if (justification == JUSTIFIED_CENTER) {
                return center(s, fieldSize);
            } else if (justification == JUSTIFIED_LEFT) {
                return rightPad(s, fieldSize);
            } else {
                return leftPad(s, fieldSize);
            }
        }
        
        @Override
        public String toPlainText() {
            final String COL_PADDING = "  ";
            StringBuilder sb = new StringBuilder();
            for (int col = 0; col < colNames.length; col++) {
                sb.append(justify(justification[col], colNames[col], colWidth.get(col)));
                if (col < colNames.length - 1) {
                    sb.append(COL_PADDING);
                }
            }
            sb.append("\n");
            if (colNames.length > 0) {
                int w = sb.length() - 1;
                for (int i = 0; i < w; i++) {
                    sb.append("-");
                }
                sb.append("\n");
            }
            for (String[] row : data) {
                for (int col = 0; col < row.length; col++) {
                    sb.append(justify(justification[col], row[col], colWidth.get(col)));
                    if (col < row.length - 1) {
                        sb.append(COL_PADDING);
                    }
                }
                sb.append("\n");
            }
            return sb.toString();
        }
        
        @Override
        public String toHTML() {
            StringBuilder sb = new StringBuilder("<table cellspacing=\"0\" cellpadding=\"2\" border=\"0\">");
            if (colNames.length > 0) {
                sb.append("<tr>");
                for (int col = 0; col < colNames.length; col++) {
                    if (justification[col] == JUSTIFIED_RIGHT) {
                        sb.append("<th align=\"right\">");
                    } else if (justification[col] == JUSTIFIED_CENTER) {
                        sb.append("<th align=\"center\">");
                    } else {
                        sb.append("<th align=\"left\">");
                    }
                    sb.append(colNames[col]).append("</th>");
                }
                sb.append("</tr>\n");
            }
            for (int r = 0; r < data.size(); r++) {
                if (bgColors.containsKey(r)) {
                    sb.append("<tr bgcolor=\"").append(bgColors.get(r)).append("\">");
                } else {
                    sb.append("<tr>");
                }
                final String[] row = data.get(r);
                for (int col = 0; col < row.length; col++) {
                    if (justification[col] == JUSTIFIED_RIGHT) {
                        sb.append("<td align=\"right\">");
                    } else if (justification[col] == JUSTIFIED_CENTER) {
                        sb.append("<td align=\"center\">");
                    } else {
                        sb.append("<td align=\"left\">");
                    }
                    sb.append(row[col]).append("</td>");
                }
                sb.append("</tr>\n");
            }
            sb.append("</table>\n");
            return sb.toString();
        }
    }
    
    /**
     * Displays a label (bold for html output) followed by a column of items
     *
     */
    private static class ItemList implements ViewElement {
        private final String heading;
        private final List<String> data = new ArrayList<>();
        
        ItemList(String heading) {
            this.heading = heading;
        }
        
        void addItem(String item) {
            data.add(item);
        }
        
        @Override
        public String toPlainText() {
            StringBuilder sb = new StringBuilder();
            if (null != heading) {
                sb.append(heading).append("\n");
                for (int i = 0; i < heading.length(); i++) {
                    sb.append("-");
                }
                sb.append("\n");
            }
            for (String item : data) {
                sb.append(item).append("\n");
            }
            return sb.toString();
        }
        
        @Override
        public String toHTML() {
            StringBuilder sb = new StringBuilder();
            if (null != heading) {
                sb.append("<b>").append(heading).append("</b><br/>\n");
            }
            for (String item : data) {
                sb.append(item).append("<br/>\n");
            }
            return sb.toString();
        }
    }
    
    /**
     * Displays a single line of text. The default constructor is used to insert a new line.
     */
    private static class SingleLine implements ViewElement {
        
        private final String value;
        
        SingleLine(String value) {
            this.value = value;
        }
        
        SingleLine() {
            this("");
        }
        
        @Override
        public String toPlainText() {
            return value + "\n";
        }
        
        @Override
        public String toHTML() {
            return value + "<br/>\n";
        }
    }
    
    /**
     * Displays a single line in bold in a larger font in html. In plain text simply displays a single line.
     */
    private static class Title implements ViewElement {
        
        private final String title;
        
        Title(String title) {
            this.title = title;
        }
        
        @Override
        public String toPlainText() {
            return title + "\n";
        }
        
        @Override
        public String toHTML() {
            return "<font size=\"+1\"><b>" + title + "</b></font><br/>\n";
        }
    }
    
    /**
     * Marks warning text; in html the text is displayed in red. In plain text it is preceded and followed
     * by an asterisk.
     * @return A String that is used to mark the beginning of a warning.
     */
    private String warningStart() {
        if (html) {
            return "<font color=\"red\">";
        } else {
            return "*";
        }
    }
    
    /**
     * Returns the end element of the warning text.
     * @return A String that is used to mark the end of a warning.
     */
    private String warningEnd() {
        if (html) {
            return "</font>";
        } else {
            return "*";
        }
    }
    
    /**
     * Marks the beginning of a section of italicized text if using html output. For plain text
     * returns an empty String.
     * @return The starting element for italicized text.
     */
    private String italicsStart() {
        if (html) {
            return "<i>";
        } else {
            return "";
        }
    }
    
    /**
     * Marks the end of a section of italicized text.
     * @return The ending element for italicized text.
     */
    private String italicsEnd() {
        if (html) {
            return "</i>";
        } else {
            return "";
        }
    }
}
