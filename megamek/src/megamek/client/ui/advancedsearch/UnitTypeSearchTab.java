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

import megamek.client.ui.Messages;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * This class is the unit type search panel of the (TW) advanced search, offering selection of e.g. Quad, Tripod, Doomed in Vacuum
 * and other filters beyond the standard unit types.
 */
public class UnitTypeSearchTab extends JPanel {

    JButton btnUnitTypeClear = new JButton(Messages.getString("MekSelectorDialog.ClearTab"));
    JLabel lblFilterMek= new JLabel(Messages.getString("MekSelectorDialog.Search.Mek"));
    JButton btnFilterMek = new JButton("\u2610");
    JLabel lblFilterBipedMek= new JLabel(Messages.getString("MekSelectorDialog.Search.BipedMek"));
    JButton btnFilterBipedMek = new JButton("\u2610");
    JLabel lblFilterProtoMek= new JLabel(Messages.getString("MekSelectorDialog.Search.ProtoMek"));
    JButton btnFilterProtoMek = new JButton("\u2610");
    JLabel lblFilterLAM = new JLabel(Messages.getString("MekSelectorDialog.Search.LAM"));
    JButton btnFilterLAM = new JButton("\u2610");
    JLabel lblFilterTripod = new JLabel(Messages.getString("MekSelectorDialog.Search.Tripod"));
    JButton btnFilterTripod = new JButton("\u2610");
    JLabel lblFilterQuad = new JLabel(Messages.getString("MekSelectorDialog.Search.Quad"));
    JButton btnFilterQuad= new JButton("\u2610");
    JLabel lblFilterQuadVee = new JLabel(Messages.getString("MekSelectorDialog.Search.QuadVee"));
    JButton btnFilterQuadVee = new JButton("\u2610");
    JLabel lblFilterAero = new JLabel(Messages.getString("MekSelectorDialog.Search.Aero"));
    JButton btnFilterAero = new JButton("\u2610");
    JLabel lblFilterFixedWingSupport = new JLabel(Messages.getString("MekSelectorDialog.Search.FixedWingSupport"));
    JButton btnFilterFixedWingSupport = new JButton("\u2610");
    JLabel lblFilterConvFighter = new JLabel(Messages.getString("MekSelectorDialog.Search.ConvFighter"));
    JButton btnFilterConvFighter = new JButton("\u2610");
    JLabel lblFilterSmallCraft = new JLabel(Messages.getString("MekSelectorDialog.Search.SmallCraft"));
    JButton btnFilterSmallCraft = new JButton("\u2610");
    JLabel lblFilterDropship = new JLabel(Messages.getString("MekSelectorDialog.Search.Dropship"));
    JButton btnFilterDropship = new JButton("\u2610");
    JLabel lblFilterJumpship = new JLabel(Messages.getString("MekSelectorDialog.Search.Jumpship"));
    JButton btnFilterJumpship = new JButton("\u2610");
    JLabel lblFilterWarship = new JLabel(Messages.getString("MekSelectorDialog.Search.Warship"));
    JButton btnFilterWarship = new JButton("\u2610");
    JLabel lblFilterSpaceStation = new JLabel(Messages.getString("MekSelectorDialog.Search.SpaceStation"));
    JButton btnFilterSpaceStation = new JButton("\u2610");
    JLabel lblFilterInfantry = new JLabel(Messages.getString("MekSelectorDialog.Search.Infantry"));
    JButton btnFilterInfantry = new JButton("\u2610");
    JLabel lblFilterBattleArmor = new JLabel(Messages.getString("MekSelectorDialog.Search.BattleArmor"));
    JButton btnFilterBattleArmor = new JButton("\u2610");
    JLabel lblFilterTank = new JLabel(Messages.getString("MekSelectorDialog.Search.Tank"));
    JButton btnFilterTank = new JButton("\u2610");
    JLabel lblFilterVTOL = new JLabel(Messages.getString("MekSelectorDialog.Search.VTOL"));
    JButton btnFilterVTOL = new JButton("\u2610");
    JLabel lblFilterSupportVTOL = new JLabel(Messages.getString("MekSelectorDialog.Search.SupportVTOL"));
    JButton btnFilterSupportVTOL = new JButton("\u2610");
    JLabel lblFilterGunEmplacement = new JLabel(Messages.getString("MekSelectorDialog.Search.GunEmplacement"));
    JButton btnFilterGunEmplacement = new JButton("\u2610");
    JLabel lblFilterSupportTank = new JLabel(Messages.getString("MekSelectorDialog.Search.SupportTank"));
    JButton btnFilterSupportTank= new JButton("\u2610");
    JLabel lblFilterLargeSupportTank = new JLabel(Messages.getString("MekSelectorDialog.Search.LargeSupportTank"));
    JButton btnFilterLargeSupportTank= new JButton("\u2610");
    JLabel lblFilterSuperHeavyTank = new JLabel(Messages.getString("MekSelectorDialog.Search.SuperHeavyTank"));
    JButton btnFilterSuperHeavyTank = new JButton("\u2610");
    JLabel lblFilterOmni = new JLabel(Messages.getString("MekSelectorDialog.Search.Omni"));
    JButton btnFilterOmni = new JButton("\u2610");
    JLabel lblFilterMilitary = new JLabel(Messages.getString("MekSelectorDialog.Search.Military"));
    JButton btnFilterMilitary = new JButton("\u2610");
    JLabel lblFilterIndustrial = new JLabel(Messages.getString("MekSelectorDialog.Search.Industrial"));
    JButton btnFilterIndustrial = new JButton("\u2610");
    JLabel lblFilterMountedInfantry = new JLabel(Messages.getString("MekSelectorDialog.Search.MountedInfantry"));
    JButton btnFilterMountedInfantry = new JButton("\u2610");
    JLabel lblFilterWaterOnly = new JLabel(Messages.getString("MekSelectorDialog.Search.WaterOnly"));
    JButton btnFilterWaterOnly = new JButton("\u2610");
    JLabel lblFilterSupportVehicle = new JLabel(Messages.getString("MekSelectorDialog.Search.SupportVehicle"));
    JButton btnFilterSupportVehicle = new JButton("\u2610");
    JLabel lblFilterAerospaceFighter = new JLabel(Messages.getString("MekSelectorDialog.Search.AerospaceFighter"));
    JButton btnFilterAerospaceFighter = new JButton("\u2610");
    JLabel lblFilterDoomedOnGround = new JLabel(Messages.getString("MekSelectorDialog.Search.DoomedOnGround"));
    JButton btnFilterDoomedOnGround = new JButton("\u2610");
    JLabel lblFilterDoomedInAtmosphere = new JLabel(Messages.getString("MekSelectorDialog.Search.DoomedInAtmosphere"));
    JButton btnFilterDoomedInAtmosphere = new JButton("\u2610");
    JLabel lblFilterDoomedInSpace = new JLabel(Messages.getString("MekSelectorDialog.Search.DoomedInSpace"));
    JButton btnFilterDoomedInSpace = new JButton("\u2610");
    JLabel lblFilterDoomedInExtremeTemp = new JLabel(Messages.getString("MekSelectorDialog.Search.DoomedInExtremeTemp"));
    JButton btnFilterDoomedInExtremeTemp = new JButton("\u2610");
    JLabel lblFilterDoomedInVacuum = new JLabel(Messages.getString("MekSelectorDialog.Search.DoomedInVacuum"));
    JButton btnFilterDoomedInVacuum = new JButton("\u2610");

    UnitTypeSearchTab(ActionListener listener) {
        btnUnitTypeClear.addActionListener(listener);

        Border emptyBorder = BorderFactory.createEmptyBorder(1, 1, 1, 1);
        btnFilterMek.setBorder(emptyBorder);
        btnFilterMek.addActionListener(listener);
        btnFilterBipedMek.setBorder(emptyBorder);
        btnFilterBipedMek.addActionListener(listener);
        btnFilterProtoMek.setBorder(emptyBorder);
        btnFilterProtoMek.addActionListener(listener);
        btnFilterLAM.setBorder(emptyBorder);
        btnFilterLAM.addActionListener(listener);
        btnFilterTripod.setBorder(emptyBorder);
        btnFilterTripod.addActionListener(listener);
        btnFilterQuad.setBorder(emptyBorder);
        btnFilterQuad.addActionListener(listener);
        btnFilterQuadVee.setBorder(emptyBorder);
        btnFilterQuadVee.addActionListener(listener);
        btnFilterAero.setBorder(emptyBorder);
        btnFilterAero.addActionListener(listener);
        btnFilterFixedWingSupport.setBorder(emptyBorder);
        btnFilterFixedWingSupport.addActionListener(listener);
        btnFilterConvFighter.setBorder(emptyBorder);
        btnFilterConvFighter.addActionListener(listener);
        btnFilterSmallCraft.setBorder(emptyBorder);
        btnFilterSmallCraft.addActionListener(listener);
        btnFilterDropship.setBorder(emptyBorder);
        btnFilterDropship.addActionListener(listener);
        btnFilterJumpship.setBorder(emptyBorder);
        btnFilterJumpship.addActionListener(listener);
        btnFilterWarship.setBorder(emptyBorder);
        btnFilterWarship.addActionListener(listener);
        btnFilterSpaceStation.setBorder(emptyBorder);
        btnFilterSpaceStation.addActionListener(listener);
        btnFilterInfantry.setBorder(emptyBorder);
        btnFilterInfantry.addActionListener(listener);
        btnFilterBattleArmor.setBorder(emptyBorder);
        btnFilterBattleArmor.addActionListener(listener);
        btnFilterTank.setBorder(emptyBorder);
        btnFilterTank.addActionListener(listener);
        btnFilterVTOL.setBorder(emptyBorder);
        btnFilterVTOL.addActionListener(listener);
        btnFilterSupportVTOL.setBorder(emptyBorder);
        btnFilterSupportVTOL.addActionListener(listener);
        btnFilterGunEmplacement.setBorder(emptyBorder);
        btnFilterGunEmplacement.addActionListener(listener);
        btnFilterSupportTank.setBorder(emptyBorder);
        btnFilterSupportTank.addActionListener(listener);
        btnFilterLargeSupportTank.setBorder(emptyBorder);
        btnFilterLargeSupportTank.addActionListener(listener);
        btnFilterSuperHeavyTank.setBorder(emptyBorder);
        btnFilterSuperHeavyTank.addActionListener(listener);
        btnFilterOmni.setBorder(emptyBorder);
        btnFilterOmni.addActionListener(listener);
        btnFilterMilitary.setBorder(emptyBorder);
        btnFilterMilitary.addActionListener(listener);
        btnFilterIndustrial.setBorder(emptyBorder);
        btnFilterIndustrial.addActionListener(listener);
        btnFilterMountedInfantry.setBorder(emptyBorder);
        btnFilterMountedInfantry.addActionListener(listener);
        btnFilterWaterOnly.setBorder(emptyBorder);
        btnFilterWaterOnly.addActionListener(listener);
        btnFilterSupportVehicle.setBorder(emptyBorder);
        btnFilterSupportVehicle.addActionListener(listener);
        btnFilterAerospaceFighter.setBorder(emptyBorder);
        btnFilterAerospaceFighter.addActionListener(listener);
        btnFilterDoomedOnGround.setBorder(emptyBorder);
        btnFilterDoomedOnGround.addActionListener(listener);
        btnFilterDoomedInAtmosphere.setBorder(emptyBorder);
        btnFilterDoomedInAtmosphere.addActionListener(listener);
        btnFilterDoomedInSpace.setBorder(emptyBorder);
        btnFilterDoomedInSpace.addActionListener(listener);
        btnFilterDoomedInExtremeTemp.setBorder(emptyBorder);
        btnFilterDoomedInExtremeTemp.addActionListener(listener);
        btnFilterDoomedInVacuum.setBorder(emptyBorder);
        btnFilterDoomedInVacuum.addActionListener(listener);

        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.weighty = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(20, 10, 0, 0);
        c.gridwidth  = 1;
        c.gridx = 0; c.gridy = 0;
        JPanel filterProtoMekPanel = new JPanel();
        filterProtoMekPanel.add(btnFilterProtoMek);
        filterProtoMekPanel.add(lblFilterProtoMek);
        add(filterProtoMekPanel, c);
        c.insets = new Insets(0, 10, 0, 0);

        c.gridx = 0;
        c.gridy++;
        JPanel filterMekPanel = new JPanel();
        filterMekPanel.add(btnFilterMek);
        filterMekPanel.add(lblFilterMek);
        add(filterMekPanel, c);
        c.gridx = 1;
        JPanel filterBipedMekPanel = new JPanel();
        filterBipedMekPanel.add(btnFilterBipedMek);
        filterBipedMekPanel.add(lblFilterBipedMek);
        add(filterBipedMekPanel, c);
        c.gridx = 2;
        JPanel filterLAMMekPanel = new JPanel();
        filterLAMMekPanel.add(btnFilterLAM);
        filterLAMMekPanel.add(lblFilterLAM);
        add(filterLAMMekPanel, c);

        c.gridy++;
        c.gridx = 1;
        JPanel filterTripodPanel = new JPanel();
        filterTripodPanel.add(btnFilterTripod);
        filterTripodPanel.add(lblFilterTripod);
        add(filterTripodPanel, c);
        c.gridy++;
        JPanel filterQuadPanel = new JPanel();
        filterQuadPanel.add(btnFilterQuad);
        filterQuadPanel.add(lblFilterQuad);
        add(filterQuadPanel, c);
        c.gridx = 2;
        JPanel filterQuadVeePanel = new JPanel();
        filterQuadVeePanel.add(btnFilterQuadVee);
        filterQuadVeePanel.add(lblFilterQuadVee);
        add(filterQuadVeePanel, c);

        c.gridx = 0;
        c.gridy++;
        JPanel filterAeroPanel = new JPanel();
        filterAeroPanel.add(btnFilterAero);
        filterAeroPanel.add(lblFilterAero);
        add(filterAeroPanel, c);
        c.gridx = 1;
        JPanel filterAerospaceFighterPanel = new JPanel();
        filterAerospaceFighterPanel.add(btnFilterAerospaceFighter);
        filterAerospaceFighterPanel.add(lblFilterAerospaceFighter);
        add(filterAerospaceFighterPanel, c);
        c.gridx = 2;
        JPanel filterConvFighterPanel = new JPanel();
        filterConvFighterPanel.add(btnFilterConvFighter);
        filterConvFighterPanel.add(lblFilterConvFighter);
        add(filterConvFighterPanel, c);
        c.gridx = 3;
        JPanel filterFixedWingSupportPanel = new JPanel();
        filterFixedWingSupportPanel.add(btnFilterFixedWingSupport);
        filterFixedWingSupportPanel.add(lblFilterFixedWingSupport);
        add(filterFixedWingSupportPanel, c);

        c.gridy++;
        c.gridx = 1;
        JPanel filterSmallCraftPanel = new JPanel();
        filterSmallCraftPanel.add(btnFilterSmallCraft);
        filterSmallCraftPanel.add(lblFilterSmallCraft);
        add(filterSmallCraftPanel, c);
        c.gridx = 2;
        JPanel filterDropship = new JPanel();
        filterDropship.add(btnFilterDropship);
        filterDropship.add(lblFilterDropship);
        add(filterDropship, c);

        c.gridy++;
        c.gridx = 1;
        JPanel filterJumpshipPanel = new JPanel();
        filterJumpshipPanel.add(btnFilterJumpship);
        filterJumpshipPanel.add(lblFilterJumpship);
        add(filterJumpshipPanel, c);
        c.gridx = 2;
        JPanel filterWarshipPanel = new JPanel();
        filterWarshipPanel.add(btnFilterWarship);
        filterWarshipPanel.add(lblFilterWarship);
        add(filterWarshipPanel, c);

        c.gridy++;
        JPanel filterSpaceStationPanel = new JPanel();
        filterSpaceStationPanel.add(btnFilterSpaceStation);
        filterSpaceStationPanel.add(lblFilterSpaceStation);
        add(filterSpaceStationPanel, c);

        c.gridx = 0;
        c.gridy++;
        JPanel filterInfantryPanel = new JPanel();
        filterInfantryPanel.add(btnFilterInfantry);
        filterInfantryPanel.add(lblFilterInfantry);
        add(filterInfantryPanel, c);
        c.gridx = 1;
        JPanel filterBattleArmorPanel = new JPanel();
        filterBattleArmorPanel.add(btnFilterBattleArmor);
        filterBattleArmorPanel.add(lblFilterBattleArmor);
        add(filterBattleArmorPanel, c);

        c.gridx = 0; c.gridy++;
        JPanel filterTankPanel = new JPanel();
        filterTankPanel.add(btnFilterTank);
        filterTankPanel.add(lblFilterTank);
        add(filterTankPanel, c);
        c.gridx = 1;
        JPanel filterVTOLPanel = new JPanel();
        filterVTOLPanel.add(btnFilterVTOL);
        filterVTOLPanel.add(lblFilterVTOL);
        add(filterVTOLPanel, c);
        c.gridx = 2;
        JPanel filterrSupportVTOLPanel = new JPanel();
        filterrSupportVTOLPanel.add(btnFilterSupportVTOL);
        filterrSupportVTOLPanel.add(lblFilterSupportVTOL);
        add(filterrSupportVTOLPanel, c);

        c.gridy++;
        c.gridx = 1;
        JPanel filterGunEmplacementPanel = new JPanel();
        filterGunEmplacementPanel.add(btnFilterGunEmplacement);
        filterGunEmplacementPanel.add(lblFilterGunEmplacement);
        add(filterGunEmplacementPanel, c);

        c.gridy++;
        JPanel filterSupportTankPanel = new JPanel();
        filterSupportTankPanel.add(btnFilterSupportTank);
        filterSupportTankPanel.add(lblFilterSupportTank);
        add(filterSupportTankPanel, c);
        c.gridx = 2;
        JPanel filterrLargeSupportTankPanel = new JPanel();
        filterrLargeSupportTankPanel.add(btnFilterLargeSupportTank);
        filterrLargeSupportTankPanel.add(lblFilterLargeSupportTank);
        add(filterrLargeSupportTankPanel, c);

        c.gridy++;
        c.gridx = 1;
        JPanel filterSuperHeavyTankPanel = new JPanel();
        filterSuperHeavyTankPanel.add(btnFilterSuperHeavyTank);
        filterSuperHeavyTankPanel.add(lblFilterSuperHeavyTank);
        add(filterSuperHeavyTankPanel, c);

        c.gridy++;
        c.gridx = 0;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth  = 4;
        JPanel dotSep = new JPanel();
        dotSep.setLayout(new BoxLayout(dotSep, BoxLayout.PAGE_AXIS));
        dotSep.add(new ASAdvancedSearchPanel.DottedSeparator());
        add(dotSep, c);

        c.gridx = 0; c.gridy++;
        c.fill = GridBagConstraints.NONE;
        c.gridwidth  = 5;
        JPanel filter1Panel = new JPanel();
        filter1Panel.add(btnFilterOmni);
        filter1Panel.add(lblFilterOmni);
        filter1Panel.add(btnFilterMilitary);
        filter1Panel.add(lblFilterMilitary);
        filter1Panel.add(btnFilterIndustrial);
        filter1Panel.add(lblFilterIndustrial);
        filter1Panel.add(btnFilterMountedInfantry);
        filter1Panel.add(lblFilterMountedInfantry);
        filter1Panel.add(btnFilterSupportVehicle);
        filter1Panel.add(lblFilterSupportVehicle);
        add(filter1Panel, c);

        c.gridx = 0; c.gridy++;
        JPanel filter2Panel = new JPanel();
        filter2Panel.add(btnFilterWaterOnly);
        filter2Panel.add(lblFilterWaterOnly);
        filter2Panel.add(btnFilterDoomedInExtremeTemp);
        filter2Panel.add(lblFilterDoomedInExtremeTemp);
        filter2Panel.add(btnFilterDoomedInVacuum);
        filter2Panel.add(lblFilterDoomedInVacuum);
        add(filter2Panel, c);


        c.gridx = 0; c.gridy++;
        JPanel filter3Panel = new JPanel();
        filter3Panel.add(btnFilterDoomedOnGround);
        filter3Panel.add(lblFilterDoomedOnGround);
        filter3Panel.add(btnFilterDoomedInAtmosphere);
        filter3Panel.add(lblFilterDoomedInAtmosphere);
        filter3Panel.add(btnFilterDoomedInSpace);
        filter3Panel.add(lblFilterDoomedInSpace);
        add(filter3Panel, c);

        c.gridx = 0; c.gridy++;
        c.weighty = 1;
        JPanel blankPanel = new JPanel();
        blankPanel.add(btnUnitTypeClear);
        add(blankPanel, c);
    }

    void clearUnitType() {
        btnFilterMek.setText("\u2610");
        btnFilterBipedMek.setText("\u2610");
        btnFilterProtoMek.setText("\u2610");
        btnFilterLAM.setText("\u2610");
        btnFilterTripod.setText("\u2610");
        btnFilterQuad.setText("\u2610");
        btnFilterQuadVee.setText("\u2610");
        btnFilterAero.setText("\u2610");
        btnFilterFixedWingSupport.setText("\u2610");
        btnFilterConvFighter.setText("\u2610");
        btnFilterSmallCraft.setText("\u2610");
        btnFilterDropship.setText("\u2610");
        btnFilterJumpship.setText("\u2610");
        btnFilterWarship.setText("\u2610");
        btnFilterSpaceStation.setText("\u2610");
        btnFilterInfantry.setText("\u2610");
        btnFilterBattleArmor.setText("\u2610");
        btnFilterTank.setText("\u2610");
        btnFilterVTOL.setText("\u2610");
        btnFilterSupportVTOL.setText("\u2610");
        btnFilterGunEmplacement.setText("\u2610");
        btnFilterSupportTank.setText("\u2610");
        btnFilterLargeSupportTank.setText("\u2610");
        btnFilterSuperHeavyTank.setText("\u2610");
        btnFilterOmni.setText("\u2610");
        btnFilterMilitary.setText("\u2610");
        btnFilterIndustrial.setText("\u2610");
        btnFilterMountedInfantry.setText("\u2610");
        btnFilterWaterOnly.setText("\u2610");
        btnFilterSupportVehicle.setText("\u2610");
        btnFilterAerospaceFighter.setText("\u2610");
        btnFilterDoomedOnGround.setText("\u2610");
        btnFilterDoomedInAtmosphere.setText("\u2610");
        btnFilterDoomedInSpace.setText("\u2610");
        btnFilterDoomedInExtremeTemp.setText("\u2610");
        btnFilterDoomedInVacuum.setText("\u2610");
    }
}
