/*
 * Copyright (c) 2002, 2003 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022-2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.advancedsearch;

import java.awt.*;
import java.util.List;

import javax.swing.*;

import com.formdev.flatlaf.extras.components.FlatTriStateCheckBox;
import megamek.client.ui.Messages;

/**
 * Panel that allows the user to create a unit filter.
 *
 * @author Arlith
 * @author Jay Lawson
 * @author Simon (Juliez)
 */
public class TWAdvancedSearchPanel extends JTabbedPane {

    public MekSearchFilter mekFilter = new MekSearchFilter();

    int gameYear;

    private final UnitTypeSearchTab unitTypePanel;
    private final MiscSearchTab basePanel;
    private final QuirksSearchTab quirkPanel;
    private final TransportsSearchTab transportsPanel;
    private final WeaponSearchTab weaponEqPanel;

    private boolean isCanceled = true;

    /**
     * Constructs a new advanced search panel for Total Warfare values
     */
    public TWAdvancedSearchPanel(int year) {
        gameYear = year;

        basePanel = new MiscSearchTab();
        weaponEqPanel = new WeaponSearchTab(this);
        unitTypePanel = new UnitTypeSearchTab();
        quirkPanel = new QuirksSearchTab();
        transportsPanel = new TransportsSearchTab();

        String msg_base = Messages.getString("MekSelectorDialog.Search.Base");
        String msg_weaponEq = Messages.getString("MekSelectorDialog.Search.WeaponEq");
        String msg_unitType = Messages.getString("MekSelectorDialog.Search.unitType");
        String msg_quirkType = Messages.getString("MekSelectorDialog.Search.Quirks");
        String msg_transports = Messages.getString("MekSelectorDialog.Search.Transports");

        addTab(msg_unitType, new StandardScrollPane(unitTypePanel));
        addTab(msg_base, new StandardScrollPane(basePanel));
        // The weapon panel must manage its own scrollpane!
        addTab(msg_weaponEq, weaponEqPanel);
        addTab(msg_transports, new StandardScrollPane(transportsPanel));
        addTab(msg_quirkType, new StandardScrollPane(quirkPanel));
    }

    /**
     * Show the dialog. setVisible(true) blocks until setVisible(false).
     *
     * @return Return the filter that was created with this dialog.
     */
    public MekSearchFilter showDialog() {
        // We need to save a copy since the user can alter the filter state
        // and then click on the cancel button. We want to make sure the
        // original filter state is saved.
        MekSearchFilter currFilter = mekFilter;
        mekFilter = new MekSearchFilter(currFilter);
        weaponEqPanel.txtWEEqExp.setText(mekFilter.getEquipmentExpression());
        weaponEqPanel.adaptTokenButtons();
        setVisible(true);
        if (isCanceled) {
            mekFilter = currFilter;
        } else {
            updateMekSearchFilter();
        }
        return mekFilter;
    }

    public void prepareFilter() {
        try {
            mekFilter = new MekSearchFilter(mekFilter);
            mekFilter.createFilterExpressionFromTokens(weaponEqPanel.filterTokens);
            updateMekSearchFilter();
        } catch (MekSearchFilter.FilterParsingException e) {
            JOptionPane.showMessageDialog(this,
                "Error parsing filter expression!\n\n" + e.msg,
                "Filter Expression Parsing Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    public void clearValues() {
        mekFilter = null;
        unitTypePanel.clear();
        basePanel.clear();
        transportsPanel.clear();
        quirkPanel.clear();
        weaponEqPanel.clear();
    }

    public MekSearchFilter getMekSearchFilter() {
        return mekFilter;
    }

    private void updateTriStateItemString(List<String> include, List<String> exclude, JList<TriStateItem> l) {
        ListModel<TriStateItem> m = l.getModel();

        for (int i = 0; i < m.getSize(); i++) {
            TriStateItem ms = m.getElementAt(i);
            if (ms.state.contains("\u2611")) {
                include.add(ms.text);
            } else if (ms.state.contains("\u2612")) {
                exclude.add(ms.text);
            }
        }
    }

    private void updateTriStateItem(List<Integer> include, List<Integer> exclude, JList<TriStateItem> l) {
        ListModel<TriStateItem> m = l.getModel();

        for (int i = 0; i < m.getSize(); i++) {
            TriStateItem ms = m.getElementAt(i);
            if (ms.state.contains("\u2611")) {
                include.add(ms.code);

            } else if (ms.state.contains("\u2612")) {
                exclude.add(ms.code);
            }
        }
    }

    private void updateBase() {
        mekFilter.sStartWalk = basePanel.tStartWalk.getText();
        mekFilter.sEndWalk = basePanel.tEndWalk.getText();

        mekFilter.sStartJump = basePanel.tStartJump.getText();
        mekFilter.sEndJump = basePanel.tEndJump.getText();

        mekFilter.iArmor = basePanel.cArmor.getSelectedIndex();

        mekFilter.sStartTankTurrets = basePanel.tStartTankTurrets.getText();
        mekFilter.sEndTankTurrets = basePanel.tEndTankTurrets.getText();
        mekFilter.sStartLowerArms = basePanel.tStartLowerArms.getText();
        mekFilter.sEndLowerArms = basePanel.tEndLowerArms.getText();
        mekFilter.sStartHands = basePanel.tStartHands.getText();
        mekFilter.sEndHands = basePanel.tEndHands.getText();

        mekFilter.iOfficial = basePanel.cOfficial.getSelectedIndex();
        mekFilter.iCanon = basePanel.cCanon.getSelectedIndex();
        mekFilter.iInvalid = basePanel.cInvalid.getSelectedIndex();
        mekFilter.iFailedToLoadEquipment = basePanel.cFailedToLoadEquipment.getSelectedIndex();
        mekFilter.iClanEngine = basePanel.cClanEngine.getSelectedIndex();
        mekFilter.iPatchwork = basePanel.cPatchwork.getSelectedIndex();

        mekFilter.source = basePanel.tSource.getText();
        mekFilter.mulid = basePanel.tMULId.getText();

        mekFilter.sStartYear = basePanel.tStartYear.getText();
        mekFilter.sEndYear = basePanel.tEndYear.getText();

        mekFilter.sStartTons = basePanel.tStartTons.getText();
        mekFilter.sEndTons = basePanel.tEndTons.getText();

        mekFilter.sStartBV = basePanel.tStartBV.getText();
        mekFilter.sEndBV = basePanel.tEndBV.getText();

        updateTriStateItem(mekFilter.armorType, mekFilter.armorTypeExclude, basePanel.listArmorType);
        updateTriStateItem(mekFilter.cockpitType, mekFilter.cockpitTypeExclude, basePanel.listCockpitType);
        updateTriStateItem(mekFilter.internalsType, mekFilter.internalsTypeExclude, basePanel.listInternalsType);
        updateTriStateItem(mekFilter.engineType, mekFilter.engineTypeExclude, basePanel.listEngineType);
        updateTriStateItem(mekFilter.gyroType, mekFilter.gyroTypeExclude, basePanel.listGyroType);
        updateTriStateItem(mekFilter.techLevel, mekFilter.techLevelExclude, basePanel.listTechLevel);
        updateTriStateItemString(mekFilter.techBase, mekFilter.techBaseExclude, basePanel.listTechBase);
        updateTriStateItemString(mekFilter.movemodes, mekFilter.movemodeExclude, basePanel.listMoveMode);
    }

    private void updateTransports() {
        mekFilter.sStartTroopSpace = transportsPanel.tStartTroopSpace.getText();
        mekFilter.sEndTroopSpace = transportsPanel.tEndTroopSpace.getText();

        mekFilter.sStartASFBays = transportsPanel.tStartASFBays.getText();
        mekFilter.sEndASFBays = transportsPanel.tEndASFBays.getText();
        mekFilter.sStartASFDoors = transportsPanel.tStartASFDoors.getText();
        mekFilter.sEndASFDoors = transportsPanel.tEndASFDoors.getText();
        mekFilter.sStartASFUnits = transportsPanel.tStartASFUnits.getText();
        mekFilter.sEndASFUnits = transportsPanel.tEndASFUnits.getText();

        mekFilter.sStartSmallCraftBays = transportsPanel.tStartSmallCraftBays.getText();
        mekFilter.sEndSmallCraftBays = transportsPanel.tEndSmallCraftBays.getText();
        mekFilter.sStartSmallCraftDoors = transportsPanel.tStartSmallCraftDoors.getText();
        mekFilter.sEndSmallCraftDoors = transportsPanel.tEndSmallCraftDoors.getText();
        mekFilter.sStartSmallCraftUnits = transportsPanel.tStartSmallCraftUnits.getText();
        mekFilter.sEndSmallCraftUnits = transportsPanel.tEndSmallCraftUnits.getText();

        mekFilter.sStartMekBays = transportsPanel.tStartMekBays.getText();
        mekFilter.sEndMekBays = transportsPanel.tEndMekBays.getText();
        mekFilter.sStartMekDoors = transportsPanel.tStartMekDoors.getText();
        mekFilter.sEndMekDoors = transportsPanel.tEndMekDoors.getText();
        mekFilter.sStartMekUnits = transportsPanel.tStartMekUnits.getText();
        mekFilter.sEndMekUnits = transportsPanel.tEndMekUnits.getText();

        mekFilter.sStartHeavyVehicleBays = transportsPanel.tStartHeavyVehicleBays.getText();
        mekFilter.sEndHeavyVehicleBays = transportsPanel.tEndHeavyVehicleBays.getText();
        mekFilter.sStartHeavyVehicleDoors = transportsPanel.tStartHeavyVehicleDoors.getText();
        mekFilter.sEndHeavyVehicleDoors = transportsPanel.tEndHeavyVehicleDoors.getText();
        mekFilter.sStartHeavyVehicleUnits = transportsPanel.tStartHeavyVehicleUnits.getText();
        mekFilter.sEndHeavyVehicleUnits = transportsPanel.tEndHeavyVehicleUnits.getText();

        mekFilter.sStartLightVehicleBays = transportsPanel.tStartLightVehicleBays.getText();
        mekFilter.sEndLightVehicleBays = transportsPanel.tEndLightVehicleBays.getText();
        mekFilter.sStartLightVehicleDoors = transportsPanel.tStartLightVehicleDoors.getText();
        mekFilter.sEndLightVehicleDoors = transportsPanel.tEndLightVehicleDoors.getText();
        mekFilter.sStartLightVehicleUnits = transportsPanel.tStartLightVehicleUnits.getText();
        mekFilter.sEndLightVehicleUnits = transportsPanel.tEndLightVehicleUnits.getText();

        mekFilter.sStartProtomekBays = transportsPanel.tStartProtomekBays.getText();
        mekFilter.sEndProtomekBays = transportsPanel.tEndProtomekBays.getText();
        mekFilter.sStartProtomekDoors = transportsPanel.tStartProtomekDoors.getText();
        mekFilter.sEndProtomekDoors = transportsPanel.tEndProtomekDoors.getText();
        mekFilter.sStartProtomekUnits = transportsPanel.tStartProtomekUnits.getText();
        mekFilter.sEndProtomekUnits = transportsPanel.tEndProtomekUnits.getText();

        mekFilter.sStartBattleArmorBays = transportsPanel.tStartBattleArmorBays.getText();
        mekFilter.sEndBattleArmorBays = transportsPanel.tEndBattleArmorBays.getText();
        mekFilter.sStartBattleArmorDoors = transportsPanel.tStartBattleArmorDoors.getText();
        mekFilter.sEndBattleArmorDoors = transportsPanel.tEndBattleArmorDoors.getText();
        mekFilter.sStartBattleArmorUnits = transportsPanel.tStartBattleArmorUnits.getText();
        mekFilter.sEndBattleArmorUnits = transportsPanel.tEndBattleArmorUnits.getText();

        mekFilter.sStartInfantryBays = transportsPanel.tStartInfantryBays.getText();
        mekFilter.sEndInfantryBays = transportsPanel.tEndInfantryBays.getText();
        mekFilter.sStartInfantryDoors = transportsPanel.tStartInfantryDoors.getText();
        mekFilter.sEndInfantryDoors = transportsPanel.tEndInfantryDoors.getText();
        mekFilter.sStartInfantryUnits = transportsPanel.tStartInfantryUnits.getText();
        mekFilter.sEndInfantryUnits = transportsPanel.tEndInfantryUnits.getText();

        mekFilter.sStartSuperHeavyVehicleBays = transportsPanel.tStartSuperHeavyVehicleBays.getText();
        mekFilter.sEndSuperHeavyVehicleBays = transportsPanel.tEndSuperHeavyVehicleBays.getText();
        mekFilter.sStartSuperHeavyVehicleDoors = transportsPanel.tStartSuperHeavyVehicleDoors.getText();
        mekFilter.sEndSuperHeavyVehicleDoors = transportsPanel.tEndSuperHeavyVehicleDoors.getText();
        mekFilter.sStartSuperHeavyVehicleUnits = transportsPanel.tStartSuperHeavyVehicleUnits.getText();
        mekFilter.sEndSuperHeavyVehicleUnits = transportsPanel.tEndSuperHeavyVehicleUnits.getText();

        mekFilter.sStartDropshuttleBays = transportsPanel.tStartDropshuttleBays.getText();
        mekFilter.sEndDropshuttleBays = transportsPanel.tEndDropshuttleBays.getText();
        mekFilter.sStartDropshuttleDoors = transportsPanel.tStartDropshuttleDoors.getText();
        mekFilter.sEndDropshuttleDoors = transportsPanel.tEndDropshuttleDoors.getText();
        mekFilter.sStartDropshuttleUnits = transportsPanel.tStartDropshuttleUnits.getText();
        mekFilter.sEndDropshuttleUnits = transportsPanel.tEndDropshuttleUnits.getText();

        mekFilter.sStartDockingCollars = transportsPanel.tStartDockingCollars.getText();
        mekFilter.sEndDockingCollars = transportsPanel.tEndDockingCollars.getText();

        mekFilter.sStartBattleArmorHandles = transportsPanel.tStartBattleArmorHandles.getText();
        mekFilter.sEndBattleArmorHandles = transportsPanel.tEndBattleArmorHandles.getText();

        mekFilter.sStartCargoBayUnits = transportsPanel.tStartCargoBayUnits.getText();
        mekFilter.sEndCargoBayUnits = transportsPanel.tEndCargoBayUnits.getText();

        mekFilter.sStartNavalRepairFacilities = transportsPanel.tStartNavalRepairFacilities.getText();
        mekFilter.sEndNavalRepairFacilities = transportsPanel.tEndNavalRepairFacilities.getText();
    }

    private void updateQuirks() {
        mekFilter.quirkInclude = quirkPanel.cQuirkInclue.getSelectedIndex();
        mekFilter.quirkExclude = quirkPanel.cQuirkExclude.getSelectedIndex();

        updateTriStateItemString(mekFilter.quirkType, mekFilter.quirkTypeExclude, quirkPanel.listQuirkType);

        mekFilter.weaponQuirkInclude = quirkPanel.cWeaponQuirkInclue.getSelectedIndex();
        mekFilter.weaponQuirkExclude = quirkPanel.cWeaponQuirkExclude.getSelectedIndex();

        updateTriStateItemString(mekFilter.weaponQuirkType, mekFilter.weaponQuirkTypeExclude, quirkPanel.listWeaponQuirkType);
    }

    private void updateUnitTypes() {
        mekFilter.filterMek = getValue(unitTypePanel.btnFilterMek);
        mekFilter.filterBipedMek = getValue(unitTypePanel.btnFilterBipedMek);
        mekFilter.filterProtomek = getValue(unitTypePanel.btnFilterProtoMek);
        mekFilter.filterLAM = getValue(unitTypePanel.btnFilterLAM);
        mekFilter.filterTripod = getValue(unitTypePanel.btnFilterTripod);
        mekFilter.filterQuad = getValue(unitTypePanel.btnFilterQuad);
        mekFilter.filterQuadVee = getValue(unitTypePanel.btnFilterQuadVee);
        mekFilter.filterAero = getValue(unitTypePanel.btnFilterAero);
        mekFilter.filterFixedWingSupport = getValue(unitTypePanel.btnFilterFixedWingSupport);
        mekFilter.filterConvFighter = getValue(unitTypePanel.btnFilterConvFighter);
        mekFilter.filterSmallCraft = getValue(unitTypePanel.btnFilterSmallCraft);
        mekFilter.filterDropship = getValue(unitTypePanel.btnFilterDropship);
        mekFilter.filterJumpship = getValue(unitTypePanel.btnFilterJumpship);
        mekFilter.filterWarship = getValue(unitTypePanel.btnFilterWarship);
        mekFilter.filterSpaceStation = getValue(unitTypePanel.btnFilterSpaceStation);
        mekFilter.filterInfantry = getValue(unitTypePanel.btnFilterInfantry);
        mekFilter.filterBattleArmor = getValue(unitTypePanel.btnFilterBattleArmor);
        mekFilter.filterTank = getValue(unitTypePanel.btnFilterTank);
        mekFilter.filterVTOL = getValue(unitTypePanel.btnFilterVTOL);
        mekFilter.filterSupportVTOL = getValue(unitTypePanel.btnFilterSupportVTOL);
        mekFilter.filterGunEmplacement = getValue(unitTypePanel.btnFilterGunEmplacement);
        mekFilter.filterSupportTank = getValue(unitTypePanel.btnFilterSupportTank);
        mekFilter.filterLargeSupportTank = getValue(unitTypePanel.btnFilterLargeSupportTank);
        mekFilter.filterSuperHeavyTank = getValue(unitTypePanel.btnFilterSuperHeavyTank);
        mekFilter.iOmni = getValue(unitTypePanel.btnFilterOmni);
        mekFilter.iMilitary = getValue(unitTypePanel.btnFilterMilitary);
        mekFilter.iIndustrial = getValue(unitTypePanel.btnFilterIndustrial);
        mekFilter.iMountedInfantry = getValue(unitTypePanel.btnFilterMountedInfantry);
        mekFilter.iWaterOnly = getValue(unitTypePanel.btnFilterWaterOnly);
        mekFilter.iSupportVehicle = getValue(unitTypePanel.btnFilterSupportVehicle);
        mekFilter.iAerospaceFighter = getValue(unitTypePanel.btnFilterAerospaceFighter);
        mekFilter.iDoomedOnGround = getValue(unitTypePanel.btnFilterDoomedOnGround);
        mekFilter.iDoomedInAtmosphere = getValue(unitTypePanel.btnFilterDoomedInAtmosphere);
        mekFilter.iDoomedInSpace = getValue(unitTypePanel.btnFilterDoomedInSpace);
        mekFilter.iDoomedInExtremeTemp = getValue(unitTypePanel.btnFilterDoomedInExtremeTemp);
        mekFilter.iDoomedInVacuum = getValue(unitTypePanel.btnFilterDoomedInVacuum);
    }

    /**
     * Update the search fields that aren't automatically updated.
     */
    protected void updateMekSearchFilter() {
        mekFilter.isDisabled = false;

        updateBase();
        updateTransports();
        updateQuirks();
        updateUnitTypes();
    }

    public int getValue(JButton b) {
        return switch (b.getText()) {
            case "\u2610" -> 0;
            case "\u2611" -> 1;
            case "\u2612" -> 2;
            default -> -1;
        };
    }

    public int getValue(FlatTriStateCheckBox b) {
        return switch (b.getState()) {
            case INDETERMINATE -> 2;
            case SELECTED -> 1;
            case UNSELECTED -> 0;
        };
    }

    static class StandardScrollPane extends JScrollPane {

        public StandardScrollPane(Component view) {
            super(view, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            getVerticalScrollBar().setUnitIncrement(16);
            getHorizontalScrollBar().setUnitIncrement(16);
            setBorder(null);
        }
    }
}
