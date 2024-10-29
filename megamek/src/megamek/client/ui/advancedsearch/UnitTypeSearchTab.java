/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

import com.formdev.flatlaf.extras.components.FlatTriStateCheckBox;
import megamek.client.ui.Messages;

import java.util.List;
import javax.swing.*;
import java.awt.*;

/**
 * This class is the unit type search panel of the (TW) advanced search, offering selection of e.g. Quad, Tripod, Doomed in Vacuum
 * and other filters beyond the standard unit types.
 */
class UnitTypeSearchTab extends JPanel {

    final JButton clearButton = new JButton(Messages.getString("MekSelectorDialog.ClearTab"));

    final FlatTriStateCheckBox btnFilterProtoMek = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.ProtoMek"));
    final FlatTriStateCheckBox btnFilterMek = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.Mek"));
    final FlatTriStateCheckBox btnFilterBipedMek = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.BipedMek"));
    final FlatTriStateCheckBox btnFilterLAM = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.LAM"));
    final FlatTriStateCheckBox btnFilterTripod = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.Tripod"));
    final FlatTriStateCheckBox btnFilterQuad = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.Quad"));
    final FlatTriStateCheckBox btnFilterQuadVee = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.QuadVee"));
    final FlatTriStateCheckBox btnFilterAero = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.Aero"));
    final FlatTriStateCheckBox btnFilterAerospaceFighter = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.AerospaceFighter"));
    final FlatTriStateCheckBox btnFilterFixedWingSupport = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.FixedWingSupport"));
    final FlatTriStateCheckBox btnFilterConvFighter = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.ConvFighter"));
    final FlatTriStateCheckBox btnFilterSmallCraft = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.SmallCraft"));
    final FlatTriStateCheckBox btnFilterDropship = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.Dropship"));
    final FlatTriStateCheckBox btnFilterJumpship = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.Jumpship"));
    final FlatTriStateCheckBox btnFilterWarship = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.Warship"));
    final FlatTriStateCheckBox btnFilterSpaceStation = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.SpaceStation"));
    final FlatTriStateCheckBox btnFilterInfantry = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.Infantry"));
    final FlatTriStateCheckBox btnFilterBattleArmor = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.BattleArmor"));
    final FlatTriStateCheckBox btnFilterTank = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.Tank"));
    final FlatTriStateCheckBox btnFilterVTOL = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.VTOL"));
    final FlatTriStateCheckBox btnFilterSupportVTOL = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.SupportVTOL"));
    final FlatTriStateCheckBox btnFilterGunEmplacement = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.GunEmplacement"));
    final FlatTriStateCheckBox btnFilterSupportTank = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.SupportTank"));
    final FlatTriStateCheckBox btnFilterLargeSupportTank = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.LargeSupportTank"));
    final FlatTriStateCheckBox btnFilterSuperHeavyTank = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.SuperHeavyTank"));
    final FlatTriStateCheckBox btnFilterOmni = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.Omni"));
    final FlatTriStateCheckBox btnFilterMilitary = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.Military"));
    final FlatTriStateCheckBox btnFilterIndustrial = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.Industrial"));
    final FlatTriStateCheckBox btnFilterMountedInfantry = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.MountedInfantry"));
    final FlatTriStateCheckBox btnFilterWaterOnly = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.WaterOnly"));
    final FlatTriStateCheckBox btnFilterSupportVehicle = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.SupportVehicle"));
    final FlatTriStateCheckBox btnFilterDoomedOnGround = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.DoomedOnGround"));
    final FlatTriStateCheckBox btnFilterDoomedInAtmosphere = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.DoomedInAtmosphere"));
    final FlatTriStateCheckBox btnFilterDoomedInSpace = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.DoomedInSpace"));
    final FlatTriStateCheckBox btnFilterDoomedInExtremeTemp = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.DoomedInExtremeTemp"));
    final FlatTriStateCheckBox btnFilterDoomedInVacuum = new SearchTriStateCheckBox(Messages.getString("MekSelectorDialog.Search.DoomedInVacuum"));

    final List<FlatTriStateCheckBox> checkBoxes = List.of(btnFilterProtoMek, btnFilterMek, btnFilterBipedMek, btnFilterLAM, btnFilterTripod,
        btnFilterQuad, btnFilterQuadVee, btnFilterAero, btnFilterFixedWingSupport, btnFilterConvFighter, btnFilterSmallCraft,
        btnFilterDropship, btnFilterJumpship, btnFilterWarship, btnFilterSpaceStation, btnFilterInfantry, btnFilterAerospaceFighter,
        btnFilterBattleArmor, btnFilterBattleArmor, btnFilterTank, btnFilterVTOL, btnFilterGunEmplacement, btnFilterSupportTank,
        btnFilterLargeSupportTank, btnFilterSuperHeavyTank, btnFilterOmni, btnFilterMilitary, btnFilterIndustrial,
        btnFilterMountedInfantry, btnFilterWaterOnly, btnFilterSupportVehicle, btnFilterDoomedOnGround, btnFilterDoomedInAtmosphere,
        btnFilterDoomedInSpace, btnFilterDoomedInExtremeTemp, btnFilterDoomedInVacuum);

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

        gbc.gridx = -1;
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

        gbc.gridx = -1;
        gbc.gridy++;
        add(btnFilterInfantry, gbc);
        add(btnFilterBattleArmor, gbc);

        gbc.gridy++;
        add(Box.createVerticalStrut(10), gbc);

        gbc.gridx = -1;
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
        gbc.gridwidth  = 4;
        JPanel dotSep = new JPanel();
        dotSep.setLayout(new BoxLayout(dotSep, BoxLayout.PAGE_AXIS));
        dotSep.add(new ASAdvancedSearchPanel.DottedSeparator());
        add(dotSep, gbc);

        gbc.gridy++;
        add(Box.createVerticalStrut(10), gbc);

        gbc.gridx = -1;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth  = 5;
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
        checkBoxes.forEach(checkBox -> checkBox.setSelected(false));
    }

    /**
     * Adapts FlatLaf's tri state checkbox to start as empty and go from there to checked and then "-" = UNDETERMINATE
     */
    static class SearchTriStateCheckBox extends FlatTriStateCheckBox {
        public SearchTriStateCheckBox(String text) {
            super(text, State.UNSELECTED);
            setAltStateCycleOrder(true);
        }
    }
}
