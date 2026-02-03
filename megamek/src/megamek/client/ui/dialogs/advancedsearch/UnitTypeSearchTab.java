/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.advancedsearch;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.formdev.flatlaf.extras.components.FlatTriStateCheckBox;
import megamek.client.ui.Messages;

/**
 * This class is the unit type search panel of the (TW) advanced search, offering selection of e.g. Quad, Tripod, Doomed
 * in Vacuum and other filters beyond the standard unit types.
 */
class UnitTypeSearchTab extends JPanel {

    final JButton clearButton = new JButton(Messages.getString("MekSelectorDialog.ClearTab"));

    final FlatTriStateCheckBox btnFilterProtoMek = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.ProtoMek"));
    final FlatTriStateCheckBox btnFilterMek = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.Mek"));
    final FlatTriStateCheckBox btnFilterBipedMek = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.BipedMek"));
    final FlatTriStateCheckBox btnFilterLAM = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.LAM"));
    final FlatTriStateCheckBox btnFilterTripod = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.Tripod"));
    final FlatTriStateCheckBox btnFilterQuad = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.Quad"));
    final FlatTriStateCheckBox btnFilterQuadVee = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.QuadVee"));
    final FlatTriStateCheckBox btnFilterAero = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.Aero"));
    final FlatTriStateCheckBox btnFilterAerospaceFighter = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.AerospaceFighter"));
    final FlatTriStateCheckBox btnFilterFixedWingSupport = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.FixedWingSupport"));
    final FlatTriStateCheckBox btnFilterConvFighter = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.ConvFighter"));
    final FlatTriStateCheckBox btnFilterSmallCraft = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.SmallCraft"));
    final FlatTriStateCheckBox btnFilterDropship = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.Dropship"));
    final FlatTriStateCheckBox btnFilterJumpship = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.Jumpship"));
    final FlatTriStateCheckBox btnFilterWarship = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.Warship"));
    final FlatTriStateCheckBox btnFilterSpaceStation = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.SpaceStation"));
    final FlatTriStateCheckBox btnFilterInfantry = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.Infantry"));
    final FlatTriStateCheckBox btnFilterBattleArmor = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.BattleArmor"));
    final FlatTriStateCheckBox btnFilterTank = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.Tank"));
    final FlatTriStateCheckBox btnFilterVTOL = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.VTOL"));
    final FlatTriStateCheckBox btnFilterSupportVTOL = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.SupportVTOL"));
    final FlatTriStateCheckBox btnFilterGunEmplacement = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.GunEmplacement"));
    final FlatTriStateCheckBox btnFilterSupportTank = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.SupportTank"));
    final FlatTriStateCheckBox btnFilterLargeSupportTank = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.LargeSupportTank"));
    final FlatTriStateCheckBox btnFilterSuperHeavyTank = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.SuperHeavyTank"));
    final FlatTriStateCheckBox btnFilterOmni = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.Omni"));
    final FlatTriStateCheckBox btnFilterMilitary = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.Military"));
    final FlatTriStateCheckBox btnFilterIndustrial = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.Industrial"));
    final FlatTriStateCheckBox btnFilterMountedInfantry = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.MountedInfantry"));
    final FlatTriStateCheckBox btnFilterWaterOnly = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.WaterOnly"));
    final FlatTriStateCheckBox btnFilterSupportVehicle = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.SupportVehicle"));
    final FlatTriStateCheckBox btnFilterDoomedOnGround = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.DoomedOnGround"));
    final FlatTriStateCheckBox btnFilterDoomedInAtmosphere = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.DoomedInAtmosphere"));
    final FlatTriStateCheckBox btnFilterDoomedInSpace = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.DoomedInSpace"));
    final FlatTriStateCheckBox btnFilterDoomedInExtremeTemp = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.DoomedInExtremeTemp"));
    final FlatTriStateCheckBox btnFilterDoomedInVacuum = new SearchTriStateCheckBox(Messages.getString(
          "MekSelectorDialog.Search.DoomedInVacuum"));

    UnitTypeSearchTab() {
        clearButton.addActionListener(e -> clear());

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        add(Box.createVerticalStrut(20));

        gbc.gridwidth = 1;
        gbc.gridy++;
        add(btnFilterProtoMek, gbc);

        gbc.gridy++;
        add(Box.createVerticalStrut(10), gbc);

        gbc.gridy++;
        add(btnFilterMek, gbc);
        add(btnFilterBipedMek, gbc);
        add(btnFilterLAM, gbc);

        gbc.gridy++;
        gbc.gridx = 1;
        add(btnFilterTripod, gbc);

        gbc.gridy++;
        add(btnFilterQuad, gbc);
        gbc.gridx = 2;
        add(btnFilterQuadVee, gbc);

        gbc.gridy++;
        add(Box.createVerticalStrut(10), gbc);

        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy++;
        add(btnFilterAero, gbc);
        add(btnFilterAerospaceFighter, gbc);
        add(btnFilterConvFighter, gbc);
        add(btnFilterFixedWingSupport, gbc);

        gbc.gridy++;
        gbc.gridx = 1;
        add(btnFilterSmallCraft, gbc);
        gbc.gridx = 2;
        add(btnFilterDropship, gbc);

        gbc.gridy++;
        gbc.gridx = 1;
        add(btnFilterJumpship, gbc);
        gbc.gridx = 2;
        add(btnFilterWarship, gbc);

        gbc.gridy++;
        gbc.gridx = 2;
        add(btnFilterSpaceStation, gbc);

        gbc.gridy++;
        add(Box.createVerticalStrut(10), gbc);

        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy++;
        add(btnFilterInfantry, gbc);
        add(btnFilterBattleArmor, gbc);

        gbc.gridy++;
        add(Box.createVerticalStrut(10), gbc);

        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy++;
        add(btnFilterTank, gbc);
        add(btnFilterVTOL, gbc);
        add(btnFilterSupportVTOL, gbc);

        gbc.gridy++;
        gbc.gridx = 1;
        add(btnFilterGunEmplacement, gbc);

        gbc.gridy++;
        add(btnFilterSupportTank, gbc);
        gbc.gridx = 2;
        add(btnFilterLargeSupportTank, gbc);

        gbc.gridy++;
        gbc.gridx = 1;
        add(btnFilterSuperHeavyTank, gbc);

        gbc.gridy++;
        add(Box.createVerticalStrut(10), gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = 4;
        JPanel dotSep = new JPanel();
        dotSep.setLayout(new BoxLayout(dotSep, BoxLayout.PAGE_AXIS));
        dotSep.add(new ASAdvancedSearchPanel.DottedSeparator());
        add(dotSep, gbc);

        gbc.gridy++;
        add(Box.createVerticalStrut(10), gbc);

        gbc.gridx = GridBagConstraints.RELATIVE;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 5;
        JPanel filter1Panel = new JPanel();
        filter1Panel.add(btnFilterOmni);
        filter1Panel.add(btnFilterMilitary);
        filter1Panel.add(btnFilterIndustrial);
        filter1Panel.add(btnFilterMountedInfantry);
        filter1Panel.add(btnFilterSupportVehicle);
        add(filter1Panel, gbc);

        gbc.gridy++;
        JPanel filter2Panel = new JPanel();
        filter2Panel.add(btnFilterWaterOnly);
        filter2Panel.add(btnFilterDoomedInExtremeTemp);
        filter2Panel.add(btnFilterDoomedInVacuum);
        add(filter2Panel, gbc);

        gbc.gridy++;
        JPanel filter3Panel = new JPanel();
        filter3Panel.add(btnFilterDoomedOnGround);
        filter3Panel.add(btnFilterDoomedInAtmosphere);
        filter3Panel.add(btnFilterDoomedInSpace);
        add(filter3Panel, gbc);

        gbc.gridy++;
        gbc.weighty = 1;
        JPanel blankPanel = new JPanel();
        blankPanel.add(clearButton);
        add(blankPanel, gbc);
    }

    void clear() {
        applyState(new AdvSearchState.UnitTypeState());
    }

    /**
     * Adapts FlatLaf's tri state checkbox to start as empty and go from there to checked and then "-" = INDETERMINATE
     */
    static class SearchTriStateCheckBox extends FlatTriStateCheckBox {
        public SearchTriStateCheckBox(String text) {
            super(text, State.UNSELECTED);
            setAltStateCycleOrder(true);
        }
    }

    void applyState(AdvSearchState.UnitTypeState state) {
        btnFilterProtoMek.setState(state.protoMek);
        btnFilterMek.setState(state.mek);
        btnFilterBipedMek.setState(state.bipedMek);
        btnFilterLAM.setState(state.lam);
        btnFilterTripod.setState(state.tripod);
        btnFilterQuad.setState(state.quad);
        btnFilterQuadVee.setState(state.quadVee);
        btnFilterAero.setState(state.aero);
        btnFilterFixedWingSupport.setState(state.fixedWingSupport);
        btnFilterConvFighter.setState(state.convFighter);
        btnFilterSmallCraft.setState(state.smallCraft);
        btnFilterDropship.setState(state.dropship);
        btnFilterJumpship.setState(state.jumpship);
        btnFilterWarship.setState(state.warship);
        btnFilterSpaceStation.setState(state.spaceStation);
        btnFilterInfantry.setState(state.infantry);
        btnFilterAerospaceFighter.setState(state.aerospaceFighter);
        btnFilterBattleArmor.setState(state.battleArmor);
        btnFilterTank.setState(state.tank);
        btnFilterVTOL.setState(state.vtol);
        btnFilterSupportVTOL.setState(state.supportVTOL);
        btnFilterGunEmplacement.setState(state.gunEmplacement);
        btnFilterSupportTank.setState(state.supportTank);
        btnFilterLargeSupportTank.setState(state.largeSupportTank);
        btnFilterSuperHeavyTank.setState(state.superHeavyTank);
        btnFilterOmni.setState(state.omni);
        btnFilterMilitary.setState(state.military);
        btnFilterIndustrial.setState(state.industrial);
        btnFilterMountedInfantry.setState(state.mountedInfantry);
        btnFilterWaterOnly.setState(state.waterOnly);
        btnFilterSupportVehicle.setState(state.supportVehicle);
        btnFilterDoomedOnGround.setState(state.doomedOnGround);
        btnFilterDoomedInAtmosphere.setState(state.doomedInAtmosphere);
        btnFilterDoomedInSpace.setState(state.doomedInSpace);
        btnFilterDoomedInExtremeTemp.setState(state.doomedInExtremeTemp);
        btnFilterDoomedInVacuum.setState(state.doomedInVacuum);
    }

    AdvSearchState.UnitTypeState getState() {
        var state = new AdvSearchState.UnitTypeState();
        state.protoMek = btnFilterProtoMek.getState();
        state.mek = btnFilterMek.getState();
        state.bipedMek = btnFilterBipedMek.getState();
        state.lam = btnFilterLAM.getState();
        state.tripod = btnFilterTripod.getState();
        state.quad = btnFilterQuad.getState();
        state.quadVee = btnFilterQuadVee.getState();
        state.aero = btnFilterAero.getState();
        state.fixedWingSupport = btnFilterFixedWingSupport.getState();
        state.convFighter = btnFilterConvFighter.getState();
        state.smallCraft = btnFilterSmallCraft.getState();
        state.dropship = btnFilterDropship.getState();
        state.jumpship = btnFilterJumpship.getState();
        state.warship = btnFilterWarship.getState();
        state.spaceStation = btnFilterSpaceStation.getState();
        state.infantry = btnFilterInfantry.getState();
        state.aerospaceFighter = btnFilterAerospaceFighter.getState();
        state.battleArmor = btnFilterBattleArmor.getState();
        state.tank = btnFilterTank.getState();
        state.vtol = btnFilterVTOL.getState();
        state.supportVTOL = btnFilterSupportVTOL.getState();
        state.gunEmplacement = btnFilterGunEmplacement.getState();
        state.supportTank = btnFilterSupportTank.getState();
        state.largeSupportTank = btnFilterLargeSupportTank.getState();
        state.superHeavyTank = btnFilterSuperHeavyTank.getState();
        state.omni = btnFilterOmni.getState();
        state.military = btnFilterMilitary.getState();
        state.industrial = btnFilterIndustrial.getState();
        state.mountedInfantry = btnFilterMountedInfantry.getState();
        state.waterOnly = btnFilterWaterOnly.getState();
        state.supportVehicle = btnFilterSupportVehicle.getState();
        state.doomedOnGround = btnFilterDoomedOnGround.getState();
        state.doomedInAtmosphere = btnFilterDoomedInAtmosphere.getState();
        state.doomedInSpace = btnFilterDoomedInSpace.getState();
        state.doomedInExtremeTemp = btnFilterDoomedInExtremeTemp.getState();
        state.doomedInVacuum = btnFilterDoomedInVacuum.getState();
        return state;
    }
}
