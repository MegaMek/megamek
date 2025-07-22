/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.unitreadout;

import megamek.client.ui.Messages;
import megamek.client.ui.util.ViewFormatting;
import megamek.common.Aero;
import megamek.common.ConvFighter;
import megamek.common.EquipmentType;
import megamek.common.IArmorState;
import megamek.common.Jumpship;
import megamek.common.SmallCraft;
import megamek.common.equipment.ArmorType;

import java.util.ArrayList;
import java.util.List;

class AeroReadout extends GeneralEntityReadout {

    private final Aero aero;

    protected AeroReadout(Aero aero, boolean showDetail, boolean useAlternateCost, boolean ignorePilotBV,
          ViewFormatting formatting) {
        super(aero, showDetail, useAlternateCost, ignorePilotBV, formatting);
        this.aero = aero;
    }

    @Override
    protected List<ViewElement> createArmorElements() {
        List<ViewElement> result = new ArrayList<>();

        result.add(new LabeledElement(Messages.getString("MekView.SI"),
              ReadoutUtils.renderArmor(aero.getSI(), aero.getOSI())));

        // if it is a jumpship get sail and KF integrity
        if (aero instanceof Jumpship jumpship) {

            // TODO: indicate damage.
            if (jumpship.hasSail()) {
                result.add(new LabeledElement(Messages.getString("MekView.SailIntegrity"),
                      String.valueOf(jumpship.getSailIntegrity())));
            }

            if (jumpship.getDriveCoreType() != Jumpship.DRIVE_CORE_NONE) {
                result.add(new LabeledElement(Messages.getString("MekView.KFIntegrity"),
                      String.valueOf(jumpship.getKFIntegrity())));
            }
        }

        String armor = String.valueOf(aero.isCapitalFighter() ? aero.getCapArmor() : aero.getTotalArmor());
        if (aero instanceof Jumpship) {
            armor += Messages.getString("MekView.CapitalArmor");
        }
        if (!aero.hasPatchworkArmor()) {
            armor += " " + ArmorType.forEntity(aero).getName();
        }
        result.add(new LabeledElement(Messages.getString("MekView.Armor"),
              armor));

        // Walk through the entity's locations.
        if (!aero.isCapitalFighter()) {
            TableElement locTable = new TableElement(3);
            locTable.setColNames("", "Armor", "");
            locTable.setJustification(TableElement.JUSTIFIED_LEFT, TableElement.JUSTIFIED_CENTER,
                  TableElement.JUSTIFIED_LEFT);
            for (int loc = 0; loc < aero.locations(); loc++) {

                // Skip empty sections.
                if (IArmorState.ARMOR_NA == aero.getInternal(loc)) {
                    continue;
                }
                // skip broadsides on warships
                if (aero instanceof Jumpship && (loc >= Jumpship.LOC_HULL)) {
                    continue;
                }
                if (aero instanceof SmallCraft && (loc >= SmallCraft.LOC_HULL)) {
                    continue;
                }
                // skip the "Wings" location
                if (!aero.isLargeCraft() && (loc >= Aero.LOC_WINGS)) {
                    continue;
                }
                String[] row = { aero.getLocationName(loc), "", "" };
                if (IArmorState.ARMOR_NA != aero.getArmor(loc)) {
                    row[1] = ReadoutUtils.renderArmor(aero.getArmor(loc), aero.getOArmor(loc));
                }
                if (aero.hasPatchworkArmor()) {
                    row[2] = Messages.getString("MekView."
                          + EquipmentType.getArmorTypeName(aero.getArmorType(loc)).trim());
                    if (aero.hasBARArmor(loc)) {
                        row[2] += Messages.getString("MekView.BARRating") + aero.getBARRating(loc);
                    }
                }
                locTable.addRow(row);
            }
            result.add(new PlainLine());
            result.add(locTable);
        }

        return result;
    }

    @Override
    protected List<ViewElement> createFuelElements() {
        List<ViewElement> result = new ArrayList<>();
        String fuel = String.valueOf(aero.getCurrentFuel());
        if (aero.getCurrentFuel() < aero.getFuel()) {
            fuel += "/" + aero.getFuel();
        }
        result.add(new LabeledElement(Messages.getString("MekView.FuelPoints"),
              String.format(Messages.getString("MekView.Fuel.format"), fuel, aero.getFuelTonnage())));

        // Display Strategic Fuel Use for Small Craft and up
        if (aero instanceof SmallCraft || aero instanceof Jumpship) {
            result.add(new LabeledElement(Messages.getString("MekView.TonsPerBurnDay"),
                  String.format("%2.2f", aero.getStrategicFuelUse())));
        }
        return result;
    }

    @Override
    protected List<ViewElement> createSystemsElements() {
        List<ViewElement> result = new ArrayList<>();

        if (!(aero instanceof ConvFighter)) {
            StringBuilder hsString = new StringBuilder(String.valueOf(aero.getHeatSinks()));
            if (aero.getPodHeatSinks() > 0) {
                hsString.append(" (").append(aero.getPodHeatSinks()).append(" ")
                      .append(Messages.getString("MekView.Pod")).append(")");
            }
            if (!aero.formatHeat().equals(Integer.toString(aero.getHeatSinks()))) {
                hsString.append(" [")
                      .append(aero.formatHeat()).append("]");
            }
            if (aero.getHeatSinkHits() > 0) {
                hsString.append(ViewElement.warningStart(formatting)).append(" (").append(aero.getHeatSinkHits())
                      .append(" damaged)").append(ViewElement.warningEnd(formatting));
            }
            result.add(new LabeledElement(Messages.getString("MekView.HeatSinks"), hsString.toString()));

            result.add(new LabeledElement(Messages.getString("MekView.Cockpit"),
                  aero.getCockpitTypeString()));
        }

        if (!aero.getCritDamageString().isEmpty()) {
            result.add(new LabeledElement(Messages.getString("MekView.SystemDamage"),
                  ViewElement.warningStart(formatting)
                        + aero.getCritDamageString()
                        + ViewElement.warningEnd(formatting)));
        }
        return result;
    }

    @Override
    protected ViewElement createEngineElement() {
        if (aero instanceof SmallCraft || aero instanceof Jumpship) {
            return new EmptyElement();
        } else {
            return super.createEngineElement();
        }
    }

    @Override
    protected List<ViewElement> createMiscMovementElements() {
        List<ViewElement> result = new ArrayList<>();
        if (aero instanceof ConvFighter && aero.isVSTOL()) {
            result.add(new LabeledElement(Messages.getString("MekView.TOL"), Messages.getString("MekView.VSTOL")));
        } else if (aero instanceof ConvFighter && aero.isSTOL()) {
            result.add(new LabeledElement(Messages.getString("MekView.TOL"), Messages.getString("MekView.STOL")));
        }
        return result;
    }

    @Override
    protected List<ViewElement> createSpecialMiscElements() {
        List<ViewElement> result = new ArrayList<>();

        // Crew
        if (aero instanceof SmallCraft || aero instanceof Jumpship) {
            TableElement crewTable = new TableElement(2);
            crewTable.setColNames(Messages.getString("MekView.Crew"), "");
            crewTable.setJustification(TableElement.JUSTIFIED_LEFT, TableElement.JUSTIFIED_RIGHT);
            crewTable.addRow(Messages.getString("MekView.Officers"), String.valueOf(aero.getNOfficers()));
            crewTable.addRow(Messages.getString("MekView.Enlisted"),
                  String.valueOf(Math.max(aero.getNCrew()
                        - aero.getBayPersonnel() - aero.getNGunners() - aero.getNOfficers(), 0)));
            crewTable.addRow(Messages.getString("MekView.Gunners"), String.valueOf(aero.getNGunners()));
            crewTable.addRow(Messages.getString("MekView.BayPersonnel"), String.valueOf(aero.getBayPersonnel()));
            if (aero.getNPassenger() > 0) {
                crewTable.addRow(Messages.getString("MekView.Passengers"), String.valueOf(aero.getNPassenger()));
            }
            if (aero.getNMarines() > 0) {
                crewTable.addRow(Messages.getString("MekView.Marines"), String.valueOf(aero.getNMarines()));
            }
            if (aero.getNBattleArmor() > 0) {
                crewTable.addRow(Messages.getString("MekView.BAMarines"), String.valueOf(aero.getNBattleArmor()));
            }
            result.add(new PlainLine());
            result.add(crewTable);
        }

        // Chassis Mods
        result.addAll(ReadoutUtils.createChassisModList(aero));
        return result;
    }

    @Override
    protected String createMovementString() {
        if ((aero instanceof Jumpship jumpship) && !aero.isWarShip() && jumpship.getStationKeepingThrust() > 0) {
            return "%1.1f (Station-keeping)".formatted(jumpship.getStationKeepingThrust());
        } else {
            return super.createMovementString();
        }
    }
}
