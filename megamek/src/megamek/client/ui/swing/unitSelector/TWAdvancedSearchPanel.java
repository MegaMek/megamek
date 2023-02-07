/*
 * Copyright (c) 2002, 2003 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing.unitSelector;

import megamek.MMConstants;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.table.MegamekTable;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.Quirks;
import megamek.common.options.WeaponQuirks;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

/**
 * Panel that allows the user to create a unit filter.
 *
 * @author Arlith
 * @author Jay Lawson
 * @author Simon (Juliez)
 */
public class TWAdvancedSearchPanel extends JPanel implements ActionListener, ItemListener,
        KeyListener, ListSelectionListener {

    private boolean isCanceled = true;
    public MechSearchFilter mechFilter;
    private Vector<FilterTokens> filterToks;

    private JButton btnLeftParen = new JButton("(");
    private JButton btnRightParen = new JButton(")");
    private JButton btnAdd = new JButton(Messages.getString("MechSelectorDialog.Search.add"));
    private JButton btnAnd = new JButton(Messages.getString("MechSelectorDialog.Search.and"));
    private JButton btnOr = new JButton(Messages.getString("MechSelectorDialog.Search.or"));
    private JButton btnClear = new JButton(Messages.getString("MechSelectorDialog.Reset"));
    private JButton btnBack = new JButton("Back");
    private JLabel  lblEqExpTxt = new JLabel(Messages.getString("MechSelectorDialog.Search.FilterExpression"));
    private JTextArea  txtEqExp = new JTextArea("");
    private JScrollPane expScroller = new JScrollPane(txtEqExp,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    private JLabel lblWalk = new JLabel(Messages.getString("MechSelectorDialog.Search.Walk"));
    private JTextField tStartWalk = new JTextField(4);
    private JTextField tEndWalk = new JTextField(4);
    private JLabel lblJump = new JLabel(Messages.getString("MechSelectorDialog.Search.Jump"));
    private JTextField tStartJump = new JTextField(4);
    private JTextField tEndJump = new JTextField(4);
    private JLabel lblTankTurrets = new JLabel(Messages.getString("MechSelectorDialog.Search.TankTurrets"));
    private JTextField tStartTankTurrets = new JTextField(4);
    private JTextField tEndTankTurrets= new JTextField(4);
    private JLabel lblTroopSpace = new JLabel(Messages.getString("MechSelectorDialog.Search.TroopSpace"));
    private JTextField tStartTroopSpace  = new JTextField(4);
    private JTextField tEndTroopSpace  = new JTextField(4);
    private JLabel lblASFBays = new JLabel(Messages.getString("MechSelectorDialog.Search.ASFBays"));
    private JTextField tStartASFBays = new JTextField(4);
    private JTextField tEndASFBays  = new JTextField(4);
    private JLabel lblASFDoors = new JLabel(Messages.getString("MechSelectorDialog.Search.Doors"));
    private JTextField tStartASFDoors = new JTextField(4);
    private JTextField tEndASFDoors  = new JTextField(4);
    private JLabel lblASFUnits = new JLabel(Messages.getString("MechSelectorDialog.Search.Units"));
    private JTextField tStartASFUnits = new JTextField(4);
    private JTextField tEndASFUnits  = new JTextField(4);
    private JLabel lblSmallCraftBays = new JLabel(Messages.getString("MechSelectorDialog.Search.SmallCraftBays"));
    private JTextField tStartSmallCraftBays = new JTextField(4);
    private JTextField tEndSmallCraftBays  = new JTextField(4);
    private JLabel lblSmallCraftDoors = new JLabel(Messages.getString("MechSelectorDialog.Search.Doors"));
    private JTextField tStartSmallCraftDoors = new JTextField(4);
    private JTextField tEndSmallCraftDoors  = new JTextField(4);
    private JLabel lblSmallCraftUnits = new JLabel(Messages.getString("MechSelectorDialog.Search.Units"));
    private JTextField tStartSmallCraftUnits = new JTextField(4);
    private JTextField tEndSmallCraftUnits = new JTextField(4);
    private JLabel lblMechBays = new JLabel(Messages.getString("MechSelectorDialog.Search.MechBays"));
    private JTextField tStartMechBays = new JTextField(4);
    private JTextField tEndMechBays  = new JTextField(4);
    private JLabel lblMechDoors = new JLabel(Messages.getString("MechSelectorDialog.Search.Doors"));
    private JTextField tStartMechDoors = new JTextField(4);
    private JTextField tEndMechDoors  = new JTextField(4);
    private JLabel lblMechUnits = new JLabel(Messages.getString("MechSelectorDialog.Search.Units"));
    private JTextField tStartMechUnits = new JTextField(4);
    private JTextField tEndMechUnits  = new JTextField(4);
    private JLabel lblHeavyVehicleBays = new JLabel(Messages.getString("MechSelectorDialog.Search.HeavyVehicleBays"));
    private JTextField tStartHeavyVehicleBays = new JTextField(4);
    private JTextField tEndHeavyVehicleBays  = new JTextField(4);
    private JLabel lblHeavyVehicleDoors = new JLabel(Messages.getString("MechSelectorDialog.Search.Doors"));
    private JTextField tStartHeavyVehicleDoors = new JTextField(4);
    private JTextField tEndHeavyVehicleDoors  = new JTextField(4);
    private JLabel lblHeavyVehicleUnits = new JLabel(Messages.getString("MechSelectorDialog.Search.Units"));
    private JTextField tStartHeavyVehicleUnits = new JTextField(4);
    private JTextField tEndHeavyVehicleUnits  = new JTextField(4);
    private JLabel lblLightVehicleBays = new JLabel(Messages.getString("MechSelectorDialog.Search.LightVehicleBays"));
    private JTextField tStartLightVehicleBays = new JTextField(4);
    private JTextField tEndLightVehicleBays  = new JTextField(4);
    private JLabel lblLightVehicleDoors = new JLabel(Messages.getString("MechSelectorDialog.Search.Doors"));
    private JTextField tStartLightVehicleDoors = new JTextField(4);
    private JTextField tEndLightVehicleDoors  = new JTextField(4);
    private JLabel lblLightVehicleUnits = new JLabel(Messages.getString("MechSelectorDialog.Search.Units"));
    private JTextField tStartLightVehicleUnits = new JTextField(4);
    private JTextField tEndLightVehicleUnits  = new JTextField(4);
    private JLabel lblProtomechBays = new JLabel(Messages.getString("MechSelectorDialog.Search.ProtomechBays"));
    private JTextField tStartProtomechBays = new JTextField(4);
    private JTextField tEndProtomechBays  = new JTextField(4);
    private JLabel lblProtomechDoors = new JLabel(Messages.getString("MechSelectorDialog.Search.Doors"));
    private JTextField tStartProtomechDoors = new JTextField(4);
    private JTextField tEndProtomechDoors  = new JTextField(4);
    private JLabel lblProtomechUnits = new JLabel(Messages.getString("MechSelectorDialog.Search.Units"));
    private JTextField tStartProtomechUnits = new JTextField(4);
    private JTextField tEndProtomechUnits  = new JTextField(4);
    private JLabel lblBattleArmorBays = new JLabel(Messages.getString("MechSelectorDialog.Search.BattleArmorBays"));
    private JTextField tStartBattleArmorBays = new JTextField(4);
    private JTextField tEndBattleArmorBays  = new JTextField(4);
    private JLabel lblBattleArmorDoors = new JLabel(Messages.getString("MechSelectorDialog.Search.Doors"));
    private JTextField tStartBattleArmorDoors = new JTextField(4);
    private JTextField tEndBattleArmorDoors  = new JTextField(4);
    private JLabel lblBattleArmorUnits = new JLabel(Messages.getString("MechSelectorDialog.Search.Units"));
    private JTextField tStartBattleArmorUnits = new JTextField(4);
    private JTextField tEndBattleArmorUnits  = new JTextField(4);
    private JLabel lblInfantryBays = new JLabel(Messages.getString("MechSelectorDialog.Search.InfantryBays"));
    private JTextField tStartInfantryBays = new JTextField(4);
    private JTextField tEndInfantryBays  = new JTextField(4);
    private JLabel lblInfantryDoors = new JLabel(Messages.getString("MechSelectorDialog.Search.Doors"));
    private JTextField tStartInfantryDoors = new JTextField(4);
    private JTextField tEndInfantryDoors  = new JTextField(4);
    private JLabel lblInfantryUnits = new JLabel(Messages.getString("MechSelectorDialog.Search.Units"));
    private JTextField tStartInfantryUnits = new JTextField(4);
    private JTextField tEndInfantryUnits  = new JTextField(4);
    private JLabel lblSuperHeavyVehicleBays = new JLabel(Messages.getString("MechSelectorDialog.Search.SuperHeavyVehicleBays"));
    private JTextField tStartSuperHeavyVehicleBays = new JTextField(4);
    private JTextField tEndSuperHeavyVehicleBays  = new JTextField(4);
    private JLabel lblSuperHeavyVehicleDoors = new JLabel(Messages.getString("MechSelectorDialog.Search.Doors"));
    private JTextField tStartSuperHeavyVehicleDoors = new JTextField(4);
    private JTextField tEndSuperHeavyVehicleDoors  = new JTextField(4);
    private JLabel lblSuperHeavyVehicleUnits = new JLabel(Messages.getString("MechSelectorDialog.Search.Units"));
    private JTextField tStartSuperHeavyVehicleUnits = new JTextField(4);
    private JTextField tEndSuperHeavyVehicleUnits  = new JTextField(4);
    private JLabel lblDropshuttleBays = new JLabel(Messages.getString("MechSelectorDialog.Search.DropshuttleBays"));
    private JTextField tStartDropshuttleBays = new JTextField(4);
    private JTextField tEndDropshuttleBays  = new JTextField(4);
    private JLabel lblDropshuttleDoors = new JLabel(Messages.getString("MechSelectorDialog.Search.Doors"));
    private JTextField tStartDropshuttleDoors = new JTextField(4);
    private JTextField tEndDropshuttleDoors  = new JTextField(4);
    private JLabel lblDropshuttleUnits = new JLabel(Messages.getString("MechSelectorDialog.Search.Units"));
    private JTextField tStartDropshuttleUnits = new JTextField(4);
    private JTextField tEndDropshuttleUnits  = new JTextField(4);
    private JLabel lblDockingCollars = new JLabel(Messages.getString("MechSelectorDialog.Search.DockingCollars"));
    private JTextField tStartDockingCollars = new JTextField(4);
    private JTextField tEndDockingCollars  = new JTextField(4);
    private JLabel lblBattleArmorHandles = new JLabel(Messages.getString("MechSelectorDialog.Search.BattleArmorHandles"));
    private JTextField tStartBattleArmorHandles = new JTextField(4);
    private JTextField tEndBattleArmorHandles  = new JTextField(4);
    private JLabel lblCargoBayUnits = new JLabel(Messages.getString("MechSelectorDialog.Search.CargoBayUnits"));
    private JTextField tStartCargoBayUnits = new JTextField(4);
    private JTextField tEndCargoBayUnits  = new JTextField(4);
    private JLabel lblNavalRepairFacilities = new JLabel(Messages.getString("MechSelectorDialog.Search.NavalRepairFacilities"));
    private JTextField tStartNavalRepairFacilities = new JTextField(4);
    private JTextField tEndNavalRepairFacilities  = new JTextField(4);
    private JLabel lblArmor = new JLabel(Messages.getString("MechSelectorDialog.Search.Armor"));
    private JComboBox<String> cArmor = new JComboBox<>();
    private JLabel lblOmni = new JLabel(Messages.getString("MechSelectorDialog.Search.Omni"));
    private JComboBox<String> cOmni = new JComboBox<>();
    private JLabel lblMilitary = new JLabel(Messages.getString("MechSelectorDialog.Search.Military"));
    private JComboBox<String> cMilitary = new JComboBox<>();
    private JLabel lblOfficial = new JLabel(Messages.getString("MechSelectorDialog.Search.Official"));
    private JComboBox<String> cOfficial = new JComboBox<>();
    private JLabel lblCanon = new JLabel(Messages.getString("MechSelectorDialog.Search.Canon"));
    private JComboBox<String> cCanon = new JComboBox<>();
    private JLabel lblClanEngine = new JLabel(Messages.getString("MechSelectorDialog.Search.ClanEngine"));
    private JComboBox<String> cClanEngine = new JComboBox<>();
    private JLabel lblTableFilters = new JLabel(Messages.getString("MechSelectorDialog.Search.TableFilters"));
    private JLabel lblUnitType = new JLabel(Messages.getString("MechSelectorDialog.Search.UnitType"));
    private JLabel lblTechClass = new JLabel(Messages.getString("MechSelectorDialog.Search.TechClass"));
    private JLabel lblTechLevel = new JLabel(Messages.getString("MechSelectorDialog.Search.TechLevel"));
    private JComboBox<String> cboUnitType = new JComboBox<>();
    private JComboBox<String> cboTechClass = new JComboBox<>();
    private JComboBox<String> cboTechLevel = new JComboBox<>();
    private JLabel lblWeaponClass = new JLabel(Messages.getString("MechSelectorDialog.Search.WeaponClass"));
    private JScrollPane scrTableWeaponType = new JScrollPane();
    private MegamekTable tblWeaponType;
    private WeaponClassTableModel weaponTypesModel;
    private TableRowSorter<WeaponClassTableModel> weaponTypesSorter;
    private JLabel lblWeapons = new JLabel(Messages.getString("MechSelectorDialog.Search.Weapons"));
    private JScrollPane scrTableWeapons = new JScrollPane();
    private MegamekTable tblWeapons;
    private WeaponsTableModel weaponsModel;
    private TableRowSorter<WeaponsTableModel> weaponsSorter;
    private JLabel lblEquipment = new JLabel(Messages.getString("MechSelectorDialog.Search.Equipment"));
    private JScrollPane scrTableEquipment = new JScrollPane();
    private MegamekTable tblEquipment;
    private EquipmentTableModel equipmentModel;
    private TableRowSorter<EquipmentTableModel> equipmentSorter;
    private JLabel lblSource = new JLabel(Messages.getString("MechSelectorDialog.Search.Source"));
    private JTextField tSource = new JTextField(4);
    private JLabel lblYear = new JLabel(Messages.getString("MechSelectorDialog.Search.Year"));
    private JTextField tStartYear = new JTextField(4);
    private JTextField tEndYear = new JTextField(4);
    private JLabel lblTons = new JLabel(Messages.getString("MechSelectorDialog.Search.Tons"));
    private JTextField tStartTons = new JTextField(4);
    private JTextField tEndTons = new JTextField(4);
    private JLabel lblBV = new JLabel(Messages.getString("MechSelectorDialog.Search.BV"));
    private JTextField tStartBV = new JTextField(4);
    private JTextField tEndBV = new JTextField(4);
    private JLabel lblCockpitType  = new JLabel(Messages.getString("MechSelectorDialog.Search.CockpitType"));
    private JList<String> listCockpitType  = new JList<>(new DefaultListModel<String>());
    private JScrollPane spCockpitType = new JScrollPane(listCockpitType);
    private JLabel lblArmorType  = new JLabel(Messages.getString("MechSelectorDialog.Search.ArmorType"));
    private JList<String> listArmorType  = new JList<>(new DefaultListModel<String>());
    private JScrollPane spArmorType = new JScrollPane(listArmorType);
    private JLabel lblInternalsType  = new JLabel(Messages.getString("MechSelectorDialog.Search.InternalsType"));
    private JList<String> listInternalsType  = new JList<>(new DefaultListModel<String>());
    private JScrollPane spInternalsType = new JScrollPane(listInternalsType);
    private JLabel lblEngineType = new JLabel(Messages.getString("MechSelectorDialog.Search.Engine"));
    private JList<String> listEngineType = new JList<>(new DefaultListModel<String>());
    private JScrollPane spEngineType = new JScrollPane(listEngineType);
    private JLabel lblQuirkInclude = new JLabel("\u2611");
    private JComboBox<String> cQuirkInclue = new JComboBox<>();
    private JLabel lblQuirkExclude = new JLabel("\u2612");
    private JComboBox<String> cQuirkExclude = new JComboBox<>();
    private JLabel lblQuirkType = new JLabel(Messages.getString("MechSelectorDialog.Search.Quirk"));
    private JList<String> listQuirkType = new JList<>(new DefaultListModel<String>());
    private JScrollPane spQuirkType = new JScrollPane(listQuirkType);
    private JLabel lblWeaponQuirkInclude = new JLabel("\u2611");
    private JComboBox<String> cWeaponQuirkInclue = new JComboBox<>();
    private JLabel lblWeaponQuirkExclude = new JLabel("\u2612");
    private JComboBox<String> cWeaponQuirkExclude = new JComboBox<>();
    private JLabel lblWeaponQuirkType = new JLabel(Messages.getString("MechSelectorDialog.Search.WeaponQuirk"));
    private JList<String> listWeaponQuirkType = new JList<>(new DefaultListModel<String>());
    private JScrollPane spWeaponQuirkType = new JScrollPane(listWeaponQuirkType);
    private JLabel lblFilterMech= new JLabel(Messages.getString("MechSelectorDialog.Search.Mech"));
    private JButton btnFilterMech = new JButton("\u2610");
    private JLabel lblFilterBipedMech= new JLabel(Messages.getString("MechSelectorDialog.Search.BipedMech"));
    private JButton btnFilterBipedMech = new JButton("\u2610");
    private JLabel lblFilterProtoMech= new JLabel(Messages.getString("MechSelectorDialog.Search.ProtoMech"));
    private JButton btnFilterProtoMech = new JButton("\u2610");
    private JLabel lblFilterLAM = new JLabel(Messages.getString("MechSelectorDialog.Search.LAM"));
    private JButton btnFilterLAM = new JButton("\u2610");
    private JLabel lblFilterTripod = new JLabel(Messages.getString("MechSelectorDialog.Search.Tripod"));
    private JButton btnFilterTripod = new JButton("\u2610");
    private JLabel lblFilterQuad = new JLabel(Messages.getString("MechSelectorDialog.Search.Quad"));
    private JButton btnFilterQuad= new JButton("\u2610");
    private JLabel lblFilterQuadVee = new JLabel(Messages.getString("MechSelectorDialog.Search.QuadVee"));
    private JButton btnFilterQuadVee = new JButton("\u2610");
    private JLabel lblFilterAero = new JLabel(Messages.getString("MechSelectorDialog.Search.Aero"));
    private JButton btnFilterAero = new JButton("\u2610");
    private JLabel lblFilterFixedWingSupport = new JLabel(Messages.getString("MechSelectorDialog.Search.FixedWingSupport"));
    private JButton btnFilterFixedWingSupport = new JButton("\u2610");
    private JLabel lblFilterConvFighter = new JLabel(Messages.getString("MechSelectorDialog.Search.ConvFighter"));
    private JButton btnFilterConvFighter = new JButton("\u2610");
    private JLabel lblFilterSmallCraft = new JLabel(Messages.getString("MechSelectorDialog.Search.SmallCraft"));
    private JButton btnFilterSmallCraft = new JButton("\u2610");
    private JLabel lblFilterDropship = new JLabel(Messages.getString("MechSelectorDialog.Search.Dropship"));
    private JButton btnFilterDropship = new JButton("\u2610");
    private JLabel lblFilterJumpship = new JLabel(Messages.getString("MechSelectorDialog.Search.Jumpship"));
    private JButton btnFilterJumpship = new JButton("\u2610");
    private JLabel lblFilterWarship = new JLabel(Messages.getString("MechSelectorDialog.Search.Warship"));
    private JButton btnFilterWarship = new JButton("\u2610");
    private JLabel lblFilterSpaceStation = new JLabel(Messages.getString("MechSelectorDialog.Search.SpaceStation"));
    private JButton btnFilterSpaceStation = new JButton("\u2610");
    private JLabel lblFilterInfantry = new JLabel(Messages.getString("MechSelectorDialog.Search.Infantry"));
    private JButton btnFilterInfantry = new JButton("\u2610");
    private JLabel lblFilterBattleArmor = new JLabel(Messages.getString("MechSelectorDialog.Search.BattleArmor"));
    private JButton btnFilterBattleArmor = new JButton("\u2610");
    private JLabel lblFilterTank = new JLabel(Messages.getString("MechSelectorDialog.Search.Tank"));
    private JButton btnFilterTank = new JButton("\u2610");
    private JLabel lblFilterVTOL = new JLabel(Messages.getString("MechSelectorDialog.Search.VTOL"));
    private JButton btnFilterVTOL = new JButton("\u2610");
    private JLabel lblFilterSupportVTOL = new JLabel(Messages.getString("MechSelectorDialog.Search.SupportVTOL"));
    private JButton btnFilterSupportVTOL = new JButton("\u2610");
    private JLabel lblFilterGunEmplacement = new JLabel(Messages.getString("MechSelectorDialog.Search.GunEmplacement"));
    private JButton btnFilterGunEmplacement = new JButton("\u2610");
    private JLabel lblFilterSupportTank = new JLabel(Messages.getString("MechSelectorDialog.Search.SupportTank"));
    private JButton btnFilterSupportTank= new JButton("\u2610");
    private JLabel lblFilterLargeSupportTank = new JLabel(Messages.getString("MechSelectorDialog.Search.LargeSupportTank"));
    private JButton btnFilterLargeSupportTank= new JButton("\u2610");
    private JLabel lblFilterSuperHeavyTank = new JLabel(Messages.getString("MechSelectorDialog.Search.SuperHeavyTank"));
    private JButton btnFilterSuperHeavyTank = new JButton("\u2610");

    private JComboBox<String> cboQty = new JComboBox<>();

    /** The game's current year. */
    private int gameYear;

    /**
     * Constructs a new advanced search panel for Total Warfare values
     */
    public TWAdvancedSearchPanel(int year) {
        mechFilter = new MechSearchFilter();
        gameYear = year;

        filterToks = new Vector<>(30);

        // Layout
        setLayout(new BorderLayout());

        JTabbedPane twSearchPane = new JTabbedPane();
        JPanel basePanel = createBasePanel();
        JPanel weaponEqPanel = createWeaponEqPanel();
        JPanel unitTypePanel = createUnitTypePanel();
        JPanel quirkPanel = createQuirkPanel();
        JPanel transportsPanel = createTransportsPanel();
        String msg_base = Messages.getString("MechSelectorDialog.Search.Base");
        String msg_weaponEq = Messages.getString("MechSelectorDialog.Search.WeaponEq");
        String msg_unitType = Messages.getString("MechSelectorDialog.Search.unitType");
        String msg_quirkType = Messages.getString("MechSelectorDialog.Search.Quirks");
        String msg_transports = Messages.getString("MechSelectorDialog.Search.Transports");
        twSearchPane.addTab(msg_unitType, unitTypePanel);
        twSearchPane.addTab(msg_base, basePanel);
        twSearchPane.addTab(msg_weaponEq, weaponEqPanel);
        twSearchPane.addTab(msg_transports, transportsPanel);
        twSearchPane.addTab(msg_quirkType, quirkPanel);
        this.add(twSearchPane, BorderLayout.NORTH);
    }

    private static class NoSelectionModel extends DefaultListSelectionModel {
        @Override
        public void setAnchorSelectionIndex(final int anchorIndex) {}

        @Override
        public void setLeadAnchorNotificationEnabled(final boolean flag) {}

        @Override
        public void setLeadSelectionIndex(final int leadIndex) {}

        @Override
        public void setSelectionInterval(final int index0, final int index1) {}
    }

    private JPanel createBasePanel() {
        // Initialize Items
        btnAnd.addActionListener(this);
        btnAdd.addActionListener(this);
        btnLeftParen.addActionListener(this);
        btnRightParen.addActionListener(this);
        btnOr.addActionListener(this);
        btnClear.addActionListener(this);
        btnBack.addActionListener(this);

        btnBack.setEnabled(false);
        btnAdd.setEnabled(false);

        cArmor.addItem(Messages.getString("MechSelectorDialog.Search.Any"));
        cArmor.addItem(Messages.getString("MechSelectorDialog.Search.Armor25"));
        cArmor.addItem(Messages.getString("MechSelectorDialog.Search.Armor50"));
        cArmor.addItem(Messages.getString("MechSelectorDialog.Search.Armor75"));
        cArmor.addItem(Messages.getString("MechSelectorDialog.Search.Armor90"));

        cOmni.addItem(Messages.getString("MechSelectorDialog.Search.Any"));
        cOmni.addItem(Messages.getString("MechSelectorDialog.Search.Yes"));
        cOmni.addItem(Messages.getString("MechSelectorDialog.Search.No"));

        cMilitary.addItem(Messages.getString("MechSelectorDialog.Search.Any"));
        cMilitary.addItem(Messages.getString("MechSelectorDialog.Search.Yes"));
        cMilitary.addItem(Messages.getString("MechSelectorDialog.Search.No"));

        cOfficial.addItem(Messages.getString("MechSelectorDialog.Search.Any"));
        cOfficial.addItem(Messages.getString("MechSelectorDialog.Search.Yes"));
        cOfficial.addItem(Messages.getString("MechSelectorDialog.Search.No"));

        cCanon.addItem(Messages.getString("MechSelectorDialog.Search.Any"));
        cCanon.addItem(Messages.getString("MechSelectorDialog.Search.Yes"));
        cCanon.addItem(Messages.getString("MechSelectorDialog.Search.No"));

        cClanEngine.addItem(Messages.getString("MechSelectorDialog.Search.Any"));
        cClanEngine.addItem(Messages.getString("MechSelectorDialog.Search.Yes"));
        cClanEngine.addItem(Messages.getString("MechSelectorDialog.Search.No"));

        DefaultListModel dlma  = new DefaultListModel();

        for (int i = 0; i < EquipmentType.armorNames.length; i++) {
            dlma.addElement("\u2610 " + EquipmentType.armorNames[i]);
        }

        listArmorType.setModel(dlma);

        listArmorType.setVisibleRowCount(7);
        listArmorType.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listArmorType.setSelectionModel(new NoSelectionModel());
        listArmorType.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getButton() == MouseEvent.BUTTON1) {
                    JList list = (JList) e.getSource();
                    int index = list.locationToIndex(e.getPoint());
                    toggleText(list, index);
                }
            }
        });

        DefaultListModel dlmc  = new DefaultListModel();

        for (int i = 0; i < Mech.COCKPIT_STRING.length; i++) {
            dlmc.addElement("\u2610 " + Mech.COCKPIT_STRING[i]);
        }

        listCockpitType.setModel(dlmc);

        listCockpitType.setVisibleRowCount(5);
        listCockpitType.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listCockpitType.setSelectionModel(new NoSelectionModel());
        listCockpitType.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getButton() == MouseEvent.BUTTON1) {
                    JList list = (JList) e.getSource();
                    int index = list.locationToIndex(e.getPoint());
                    toggleText(list, index);
                }
            }
        });

        DefaultListModel dlme  = new DefaultListModel();

        for (int i = 0; i < Engine.NUM_ENGINE_TYPES; i++) {
            dlme.addElement("\u2610 " + Engine.getEngineType(i));
        }

        listEngineType.setModel(dlme);

        listEngineType.setVisibleRowCount(5);
        listEngineType.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listEngineType.setSelectionModel(new NoSelectionModel());
        listEngineType.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getButton() == MouseEvent.BUTTON1) {
                    JList list = (JList) e.getSource();
                    int index = list.locationToIndex(e.getPoint());
                    toggleText(list, index);
                }
            }
        });

        DefaultListModel dlmi  = new DefaultListModel();

        for (int i = 0; i < EquipmentType.structureNames.length; i++) {
            dlmi.addElement("\u2610 " + EquipmentType.structureNames[i]);
        }

        listInternalsType.setModel(dlmi);

        listInternalsType.setVisibleRowCount(5);
        listInternalsType.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listInternalsType.setSelectionModel(new NoSelectionModel());
        listInternalsType.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getButton() == MouseEvent.BUTTON1) {
                    JList list = (JList) e.getSource();
                    int index = list.locationToIndex(e.getPoint());
                    toggleText(list, index);
                }
            }
        });

        for (int i = 1; i <= 20; i++) {
            cboQty.addItem(Integer.toString(i));
        }
        cboQty.setSelectedIndex(0);

        JPanel basePanel = new JPanel();
        GridBagConstraints c = new GridBagConstraints();
        basePanel.setLayout(new GridBagLayout());

        c.weighty = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.NONE;
        c.gridwidth  = 1;
        c.insets = new Insets(20, 10, 0, 0);
        c.gridx = 0; c.gridy = 0;
        JPanel p0Panel = new JPanel();
        p0Panel.add(lblOfficial);
        p0Panel.add(cOfficial);
        p0Panel.add(lblCanon);
        p0Panel.add(cCanon);
        basePanel.add(p0Panel, c);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        JPanel sPanel = new JPanel(new BorderLayout());
        sPanel.add(lblSource, BorderLayout.WEST);
        sPanel.add(tSource, BorderLayout.CENTER);
        basePanel.add(sPanel, c);
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(5, 10, 0, 0);
        c.gridx = 0; c.gridy++;
        JPanel p1Panel = new JPanel();
        p1Panel.add(lblOmni);
        p1Panel.add(cOmni);
        p1Panel.add(lblMilitary);
        p1Panel.add(cMilitary);
        basePanel.add(p1Panel, c);
        c.gridx = 1;
        JPanel p1bPanel = new JPanel();
        p1bPanel.add(lblTankTurrets);
        p1bPanel.add(tStartTankTurrets);
        p1bPanel.add(new Label("-"));
        p1bPanel.add(tEndTankTurrets);
        basePanel.add(p1bPanel, c);
        c.gridx = 0; c.gridy++;
        JPanel yearPanel = new JPanel();
        yearPanel.add(lblYear);
        yearPanel.add(tStartYear);
        yearPanel.add(new Label("-"));
        yearPanel.add(tEndYear);
        basePanel.add(yearPanel, c);
        c.gridx = 1;
        JPanel armorPanel = new JPanel();
        armorPanel.add(lblArmor);
        armorPanel.add(cArmor);
        basePanel.add(armorPanel, c);
        c.gridx = 0; c.gridy++;
        JPanel bvPanel = new JPanel();
        bvPanel.add(lblBV);
        bvPanel.add(tStartBV);
        bvPanel.add(new Label("-"));
        bvPanel.add(tEndBV);
        basePanel.add(bvPanel, c);
        c.gridx = 1;
        JPanel tonsPanel = new JPanel();
        tonsPanel.add(lblTons);
        tonsPanel.add(tStartTons);
        tonsPanel.add(new Label("-"));
        tonsPanel.add(tEndTons);
        basePanel.add(tonsPanel, c);
        c.gridx = 0; c.gridy++;
        JPanel walkPanel = new JPanel();
        walkPanel.add(lblWalk);
        walkPanel.add(tStartWalk);
        walkPanel.add(new Label("-"));
        walkPanel.add(tEndWalk);
        basePanel.add(walkPanel, c);
        c.gridx = 1;
        JPanel jumpPanel = new JPanel();
        jumpPanel.add(lblJump);
        jumpPanel.add(tStartJump);
        jumpPanel.add(new Label("-"));
        jumpPanel.add(tEndJump);
        basePanel.add(jumpPanel, c);

        c.gridx = 0; c.gridy++;;
        c.gridwidth  = 1;
        JPanel cockpitPanel = new JPanel(new BorderLayout());
        cockpitPanel.add(lblCockpitType, BorderLayout.NORTH);
        cockpitPanel.add(spCockpitType, BorderLayout.CENTER);
        basePanel.add(cockpitPanel, c);
        c.gridx = 1;
        JPanel internalsPanel = new JPanel(new BorderLayout());
        internalsPanel.add(lblInternalsType, BorderLayout.NORTH);
        internalsPanel.add(spInternalsType, BorderLayout.CENTER);
        basePanel.add(internalsPanel, c);
        c.gridx = 0; c.gridy++;;
        JPanel armorTypePanel = new JPanel(new BorderLayout());
        armorTypePanel.add(lblArmorType, BorderLayout.NORTH);
        armorTypePanel.add(spArmorType, BorderLayout.CENTER);
        basePanel.add(armorTypePanel, c);
        c.gridx = 1;
        JPanel enginePanel = new JPanel(new BorderLayout());
        enginePanel.add(lblEngineType, BorderLayout.NORTH);
        enginePanel.add(spEngineType, BorderLayout.CENTER);
        JPanel clanEnginePanel = new JPanel();
        clanEnginePanel.add(lblClanEngine);
        clanEnginePanel.add(cClanEngine);
        enginePanel.add(clanEnginePanel, BorderLayout.SOUTH);
        basePanel.add(enginePanel, c);

        c.weighty = 1;
        JPanel blankPanel = new JPanel();
        basePanel.add(blankPanel, c);

        return basePanel;
    }

    private JPanel createTransportsPanel() {
        JPanel transportsPanel = new JPanel();
        GridBagConstraints c = new GridBagConstraints();
        transportsPanel.setLayout(new GridBagLayout());

        c.weighty = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(20, 10, 0, 0);

        c.gridwidth  = 1;
        c.gridx = 0; c.gridy++;
        JPanel mbPanel = new JPanel();
        mbPanel.add(lblMechBays);
        mbPanel.add(tStartMechBays);
        mbPanel.add(new Label("-"));
        mbPanel.add(tEndMechBays);
        mbPanel.add(lblMechDoors);
        mbPanel.add(tStartMechDoors);
        mbPanel.add(new Label("-"));
        mbPanel.add(tEndMechDoors);
        mbPanel.add(lblMechUnits);
        mbPanel.add(tStartMechUnits);
        mbPanel.add(new Label("-"));
        mbPanel.add(tEndMechUnits);
        transportsPanel.add(mbPanel, c);
        c.insets = new Insets(5, 10, 0, 0);
        c.gridx = 0; c.gridy++;
        JPanel abPanel = new JPanel();
        abPanel.add(lblASFBays);
        abPanel.add(tStartASFBays);
        abPanel.add(new Label("-"));
        abPanel.add(tEndASFBays);
        abPanel.add(lblASFDoors);
        abPanel.add(tStartASFDoors);
        abPanel.add(new Label("-"));
        abPanel.add(tEndASFDoors);
        abPanel.add(lblASFUnits);
        abPanel.add(tStartASFUnits);
        abPanel.add(new Label("-"));
        abPanel.add(tEndASFUnits);
        transportsPanel.add(abPanel, c);
        c.gridx = 0; c.gridy++;
        JPanel scbPanel = new JPanel();
        scbPanel.add(lblSmallCraftBays);
        scbPanel.add(tStartSmallCraftBays);
        scbPanel.add(new Label("-"));
        scbPanel.add(tEndSmallCraftBays);
        scbPanel.add(lblSmallCraftDoors);
        scbPanel.add(tStartSmallCraftDoors);
        scbPanel.add(new Label("-"));
        scbPanel.add(tEndSmallCraftDoors);
        scbPanel.add(lblSmallCraftUnits);
        scbPanel.add(tStartSmallCraftUnits);
        scbPanel.add(new Label("-"));
        scbPanel.add(tEndSmallCraftUnits);
        transportsPanel.add(scbPanel, c);
        c.gridx = 0; c.gridy++;
        JPanel dPanel = new JPanel();
        dPanel.add(lblDropshuttleBays);
        dPanel.add(tStartDropshuttleBays);
        dPanel.add(new Label("-"));
        dPanel.add(tEndDropshuttleBays);
        dPanel.add(lblDropshuttleDoors);
        dPanel.add(tStartDropshuttleDoors);
        dPanel.add(new Label("-"));
        dPanel.add(tEndDropshuttleDoors);
        dPanel.add(lblDropshuttleUnits);
        dPanel.add(tStartDropshuttleUnits);
        dPanel.add(new Label("-"));
        dPanel.add(tEndDropshuttleUnits);
        transportsPanel.add(dPanel, c);
        c.gridx = 0; c.gridy++;
        JPanel lvPanel = new JPanel();
        lvPanel.add(lblLightVehicleBays);
        lvPanel.add(tStartLightVehicleBays);
        lvPanel.add(new Label("-"));
        lvPanel.add(tEndLightVehicleBays);
        lvPanel.add(lblLightVehicleDoors);
        lvPanel.add(tStartLightVehicleDoors);
        lvPanel.add(new Label("-"));
        lvPanel.add(tEndLightVehicleDoors);
        lvPanel.add(lblLightVehicleUnits);
        lvPanel.add(tStartLightVehicleUnits);
        lvPanel.add(new Label("-"));
        lvPanel.add(tEndLightVehicleUnits);
        transportsPanel.add(lvPanel, c);
        c.gridx = 0; c.gridy++;
        JPanel hvPanel = new JPanel();
        hvPanel.add(lblHeavyVehicleBays);
        hvPanel.add(tStartHeavyVehicleBays);
        hvPanel.add(new Label("-"));
        hvPanel.add(tEndHeavyVehicleBays);
        hvPanel.add(lblHeavyVehicleDoors);
        hvPanel.add(tStartHeavyVehicleDoors);
        hvPanel.add(new Label("-"));
        hvPanel.add(tEndHeavyVehicleDoors);
        hvPanel.add(lblHeavyVehicleUnits);
        hvPanel.add(tStartHeavyVehicleUnits);
        hvPanel.add(new Label("-"));
        hvPanel.add(tEndHeavyVehicleUnits);
        transportsPanel.add(hvPanel, c);
        c.gridx = 0; c.gridy++;
        JPanel shvPanel = new JPanel();
        shvPanel.add(lblSuperHeavyVehicleBays);
        shvPanel.add(tStartSuperHeavyVehicleBays);
        shvPanel.add(new Label("-"));
        shvPanel.add(tEndSuperHeavyVehicleBays);
        shvPanel.add(lblSuperHeavyVehicleDoors);
        shvPanel.add(tStartSuperHeavyVehicleDoors);
        shvPanel.add(new Label("-"));
        shvPanel.add(tEndSuperHeavyVehicleDoors);
        shvPanel.add(lblSuperHeavyVehicleUnits);
        shvPanel.add(tStartSuperHeavyVehicleUnits);
        shvPanel.add(new Label("-"));
        shvPanel.add(tEndSuperHeavyVehicleUnits);
        transportsPanel.add(shvPanel, c);
        c.gridx = 0; c.gridy++;
        JPanel pmPanel = new JPanel();
        pmPanel.add(lblProtomechBays);
        pmPanel.add(tStartProtomechBays);
        pmPanel.add(new Label("-"));
        pmPanel.add(tEndProtomechBays);
        pmPanel.add(lblProtomechDoors);
        pmPanel.add(tStartProtomechDoors);
        pmPanel.add(new Label("-"));
        pmPanel.add(tEndProtomechDoors);
        pmPanel.add(lblProtomechUnits);
        pmPanel.add(tStartProtomechUnits);
        pmPanel.add(new Label("-"));
        pmPanel.add(tEndProtomechUnits);
        transportsPanel.add(pmPanel, c);
        c.gridx = 0; c.gridy++;
        JPanel baPanel = new JPanel();
        baPanel.add(lblBattleArmorBays);
        baPanel.add(tStartBattleArmorBays);
        baPanel.add(new Label("-"));
        baPanel.add(tEndBattleArmorBays);
        baPanel.add(lblBattleArmorDoors);
        baPanel.add(tStartBattleArmorDoors);
        baPanel.add(new Label("-"));
        baPanel.add(tEndBattleArmorDoors);
        baPanel.add(lblBattleArmorUnits);
        baPanel.add(tStartBattleArmorUnits);
        baPanel.add(new Label("-"));
        baPanel.add(tEndBattleArmorUnits);
        transportsPanel.add(baPanel, c);
        c.gridx = 0; c.gridy++;
        JPanel iPanel = new JPanel();
        iPanel.add(lblInfantryBays);
        iPanel.add(tStartInfantryBays);
        iPanel.add(new Label("-"));
        iPanel.add(tEndInfantryBays);
        iPanel.add(lblInfantryDoors);
        iPanel.add(tStartInfantryDoors);
        iPanel.add(new Label("-"));
        iPanel.add(tEndInfantryDoors);
        iPanel.add(lblInfantryUnits);
        iPanel.add(tStartInfantryUnits);
        iPanel.add(new Label("-"));
        iPanel.add(tEndInfantryUnits);
        transportsPanel.add(iPanel, c);
        c.gridx = 0; c.gridy++;
        JPanel dcPanel = new JPanel();
        dcPanel.add(lblDockingCollars);
        dcPanel.add(tStartDockingCollars);
        dcPanel.add(new Label("-"));
        dcPanel.add(tEndDockingCollars);
        transportsPanel.add(dcPanel, c);
        c.gridx = 0; c.gridy++;
        JPanel tsPanel = new JPanel();
        tsPanel.add(lblTroopSpace);
        tsPanel.add(tStartTroopSpace);
        tsPanel.add(new Label("-"));
        tsPanel.add(tEndTroopSpace);
        transportsPanel.add(tsPanel, c);
        c.gridx = 0; c.gridy++;
        JPanel cbPanel = new JPanel();
        cbPanel.add(lblCargoBayUnits);
        cbPanel.add(tStartCargoBayUnits);
        cbPanel.add(new Label("-"));
        cbPanel.add(tEndCargoBayUnits);
        transportsPanel.add(cbPanel, c);
        c.gridx = 0; c.gridy++;
        JPanel nrfPanel = new JPanel();
        nrfPanel.add(lblNavalRepairFacilities);
        nrfPanel.add(tStartNavalRepairFacilities);
        nrfPanel.add(new Label("-"));
        nrfPanel.add(tEndNavalRepairFacilities);
        transportsPanel.add(nrfPanel, c);
        c.gridx = 0; c.gridy++;
        JPanel bahPanel = new JPanel();
        bahPanel.add(lblBattleArmorHandles);
        bahPanel.add(tStartBattleArmorHandles);
        bahPanel.add(new Label("-"));
        bahPanel.add(tEndBattleArmorHandles);
        transportsPanel.add(bahPanel, c);

        c.weighty = 1;
        JPanel blankPanel = new JPanel();
        transportsPanel.add(blankPanel, c);

        return transportsPanel;
    }

    private JPanel createQuirkPanel() {
        cQuirkInclue.addItem(Messages.getString("MechSelectorDialog.Search.and"));
        cQuirkInclue.addItem(Messages.getString("MechSelectorDialog.Search.or"));
        cQuirkExclude.addItem(Messages.getString("MechSelectorDialog.Search.and"));
        cQuirkExclude.addItem(Messages.getString("MechSelectorDialog.Search.or"));
        cQuirkExclude.setSelectedIndex(1);

        Quirks quirks = new Quirks();
        List<String> qs = new ArrayList<>();
        for (final Enumeration<IOptionGroup> optionGroups = quirks.getGroups(); optionGroups.hasMoreElements(); ) {
            final IOptionGroup group = optionGroups.nextElement();
            for (final Enumeration<IOption> options = group.getOptions(); options.hasMoreElements(); ) {
                final IOption option = options.nextElement();
                if (option != null) {
                    qs.add(option.getDisplayableNameWithValue());
                }
            }
        }
        qs = qs.stream().sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());

        DefaultListModel dlmq  = new DefaultListModel();

        for (String q : qs) {
            dlmq.addElement("\u2610 " + q);
        }

        listQuirkType.setModel(dlmq);

        listQuirkType.setVisibleRowCount(25);
        listQuirkType.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listQuirkType.setSelectionModel(new NoSelectionModel());
        listQuirkType.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getButton() == MouseEvent.BUTTON1) {
                    JList list = (JList) e.getSource();
                    int index = list.locationToIndex(e.getPoint());
                    toggleText(list, index);
                }
            }
        });

        cWeaponQuirkInclue.addItem(Messages.getString("MechSelectorDialog.Search.and"));
        cWeaponQuirkInclue.addItem(Messages.getString("MechSelectorDialog.Search.or"));
        cWeaponQuirkExclude.addItem(Messages.getString("MechSelectorDialog.Search.and"));
        cWeaponQuirkExclude.addItem(Messages.getString("MechSelectorDialog.Search.or"));
        cWeaponQuirkExclude.setSelectedIndex(1);

        WeaponQuirks weaponquirks = new WeaponQuirks();
        List<String> wqs = new ArrayList<>();

        for (final Enumeration<IOptionGroup> optionGroups = weaponquirks.getGroups(); optionGroups.hasMoreElements(); ) {
            final IOptionGroup group = optionGroups.nextElement();
            for (final Enumeration<IOption> options = group.getOptions(); options.hasMoreElements(); ) {
                final IOption option = options.nextElement();
                if (option != null) {
                    wqs.add(option.getDisplayableNameWithValue());
                }
            }
        }
        wqs = wqs.stream().sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());

        DefaultListModel dlmw  = new DefaultListModel();

        for (String q : wqs) {
            dlmw.addElement("\u2610 " + q);
        }

        listWeaponQuirkType.setModel(dlmw);

        listWeaponQuirkType.setVisibleRowCount(17);
        listWeaponQuirkType.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listWeaponQuirkType.setSelectionModel(new NoSelectionModel());
        listWeaponQuirkType.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getButton() == MouseEvent.BUTTON1) {
                    JList list = (JList) e.getSource();
                    int index = list.locationToIndex(e.getPoint());
                    toggleText(list, index);
                }
            }
        });

        JPanel quirksPanel = new JPanel();
        GridBagConstraints c = new GridBagConstraints();
        quirksPanel.setLayout(new GridBagLayout());

        c.weighty = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(20, 10, 0, 0);
        c.gridx = 0; c.gridy++;;
        JPanel quirkPanel = new JPanel(new BorderLayout());
        JPanel quirkIEPanel = new JPanel(new FlowLayout());
        quirkIEPanel.add(lblQuirkType);
        quirkIEPanel.add(Box.createHorizontalStrut(15));
        quirkIEPanel.add(lblQuirkInclude);
        quirkIEPanel.add(cQuirkInclue);
        quirkIEPanel.add(lblQuirkExclude);
        quirkIEPanel.add(cQuirkExclude);
        quirkPanel.add(quirkIEPanel, BorderLayout.NORTH);
        quirkPanel.add(spQuirkType, BorderLayout.CENTER);
        quirksPanel.add(quirkPanel, c);
        c.gridx = 1;
        JPanel weaponQuirkPanel = new JPanel(new BorderLayout());
        JPanel weaponQuirkIEPanel = new JPanel(new FlowLayout());
        weaponQuirkIEPanel.add(lblWeaponQuirkType);
        weaponQuirkIEPanel.add(Box.createHorizontalStrut(15));
        weaponQuirkIEPanel.add(lblWeaponQuirkInclude);
        weaponQuirkIEPanel.add(cWeaponQuirkInclue);
        weaponQuirkIEPanel.add(lblWeaponQuirkExclude);
        weaponQuirkIEPanel.add(cWeaponQuirkExclude);
        weaponQuirkPanel.add(weaponQuirkIEPanel, BorderLayout.NORTH);
        weaponQuirkPanel.add(spWeaponQuirkType, BorderLayout.CENTER);
        quirksPanel.add(weaponQuirkPanel, c);
        c.weighty = 1;
        JPanel blankPanel = new JPanel();
        quirksPanel.add(blankPanel, c);

        return quirksPanel;
    }

    private JPanel createUnitTypePanel() {
        Border emptyBorder = BorderFactory.createEmptyBorder();
        btnFilterMech.setBorder(emptyBorder);
        btnFilterMech.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnFilterMech.addActionListener(this);
        btnFilterBipedMech.setBorder(emptyBorder);
        btnFilterBipedMech.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnFilterBipedMech.addActionListener(this);
        btnFilterProtoMech.setBorder(emptyBorder);
        btnFilterProtoMech.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnFilterProtoMech.addActionListener(this);
        btnFilterLAM.setBorder(emptyBorder);
        btnFilterLAM.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnFilterLAM.addActionListener(this);
        btnFilterTripod.setBorder(emptyBorder);
        btnFilterTripod.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnFilterTripod.addActionListener(this);
        btnFilterQuad.setBorder(emptyBorder);
        btnFilterQuad.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnFilterQuad.addActionListener(this);
        btnFilterQuadVee.setBorder(emptyBorder);
        btnFilterQuadVee.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnFilterQuadVee.addActionListener(this);
        btnFilterAero.setBorder(emptyBorder);
        btnFilterAero.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnFilterAero.addActionListener(this);
        btnFilterFixedWingSupport.setBorder(emptyBorder);
        btnFilterFixedWingSupport.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnFilterFixedWingSupport.addActionListener(this);
        btnFilterConvFighter.setBorder(emptyBorder);
        btnFilterConvFighter.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnFilterConvFighter.addActionListener(this);
        btnFilterSmallCraft.setBorder(emptyBorder);
        btnFilterSmallCraft.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnFilterSmallCraft.addActionListener(this);
        btnFilterDropship.setBorder(emptyBorder);
        btnFilterDropship.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnFilterDropship.addActionListener(this);
        btnFilterJumpship.setBorder(emptyBorder);
        btnFilterJumpship.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnFilterJumpship.addActionListener(this);
        btnFilterWarship.setBorder(emptyBorder);
        btnFilterWarship.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnFilterWarship.addActionListener(this);
        btnFilterSpaceStation.setBorder(emptyBorder);
        btnFilterSpaceStation.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnFilterSpaceStation.addActionListener(this);
        btnFilterInfantry.setBorder(emptyBorder);
        btnFilterInfantry.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnFilterInfantry.addActionListener(this);
        btnFilterBattleArmor.setBorder(emptyBorder);
        btnFilterBattleArmor.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnFilterBattleArmor.addActionListener(this);
        btnFilterTank.setBorder(emptyBorder);
        btnFilterTank.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnFilterTank.addActionListener(this);
        btnFilterVTOL.setBorder(emptyBorder);
        btnFilterVTOL.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnFilterVTOL.addActionListener(this);
        btnFilterSupportVTOL.setBorder(emptyBorder);
        btnFilterSupportVTOL.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnFilterSupportVTOL.addActionListener(this);
        btnFilterGunEmplacement.setBorder(emptyBorder);
        btnFilterGunEmplacement.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnFilterGunEmplacement.addActionListener(this);
        btnFilterSupportTank.setBorder(emptyBorder);
        btnFilterSupportTank.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnFilterSupportTank.addActionListener(this);
        btnFilterLargeSupportTank.setBorder(emptyBorder);
        btnFilterLargeSupportTank.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnFilterLargeSupportTank.addActionListener(this);
        btnFilterSuperHeavyTank.setBorder(emptyBorder);
        btnFilterSuperHeavyTank.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnFilterSuperHeavyTank.addActionListener(this);

        JPanel unitTypePanel = new JPanel();
        unitTypePanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.weighty = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(20, 10, 0, 0);
        c.gridwidth  = 1;
        c.gridx = 0; c.gridy = 0;
        JPanel filterProtoMechPanel = new JPanel();
        filterProtoMechPanel.add(btnFilterProtoMech);
        filterProtoMechPanel.add(lblFilterProtoMech);
        unitTypePanel.add(filterProtoMechPanel, c);
        c.insets = new Insets(0, 10, 0, 0);
        c.gridx = 0; c.gridy++;
        JPanel filterMechPanel = new JPanel();
        filterMechPanel.add(btnFilterMech);
        filterMechPanel.add(lblFilterMech);
        unitTypePanel.add(filterMechPanel, c);
        c.gridx = 1;
        JPanel filterBipedMechPanel = new JPanel();
        filterBipedMechPanel.add(btnFilterBipedMech);
        filterBipedMechPanel.add(lblFilterBipedMech);
        unitTypePanel.add(filterBipedMechPanel, c);
        c.gridx = 2;
        JPanel filterLAMMechPanel = new JPanel();
        filterLAMMechPanel.add(btnFilterLAM);
        filterLAMMechPanel.add(lblFilterLAM);
        unitTypePanel.add(filterLAMMechPanel, c);
        c.gridy++;
        c.gridx = 1;
        JPanel filterTripodPanel = new JPanel();
        filterTripodPanel.add(btnFilterTripod);
        filterTripodPanel.add(lblFilterTripod);
        unitTypePanel.add(filterTripodPanel, c);
        c.gridy++;
        JPanel filterQuadPanel = new JPanel();
        filterQuadPanel.add(btnFilterQuad);
        filterQuadPanel.add(lblFilterQuad);
        unitTypePanel.add(filterQuadPanel, c);
        c.gridx = 2;
        JPanel filterQuadVeePanel = new JPanel();
        filterQuadVeePanel.add(btnFilterQuadVee);
        filterQuadVeePanel.add(lblFilterQuadVee);
        unitTypePanel.add(filterQuadVeePanel, c);
        c.gridx = 0; c.gridy++;
        JPanel filterAeroPanel = new JPanel();
        filterAeroPanel.add(btnFilterAero);
        filterAeroPanel.add(lblFilterAero);
        unitTypePanel.add(filterAeroPanel, c);
        c.gridx = 1;
        JPanel filterConvFighterPanel = new JPanel();
        filterConvFighterPanel.add(btnFilterConvFighter);
        filterConvFighterPanel.add(lblFilterConvFighter);
        unitTypePanel.add(filterConvFighterPanel, c);
        c.gridx = 2;
        JPanel filterFixedWingSupportPanel = new JPanel();
        filterFixedWingSupportPanel.add(btnFilterFixedWingSupport);
        filterFixedWingSupportPanel.add(lblFilterFixedWingSupport);
        unitTypePanel.add(filterFixedWingSupportPanel, c);
        c.gridy++;
        c.gridx = 1;
        JPanel filterSmallCraftPanel = new JPanel();
        filterSmallCraftPanel.add(btnFilterSmallCraft);
        filterSmallCraftPanel.add(lblFilterSmallCraft);
        unitTypePanel.add(filterSmallCraftPanel, c);
        c.gridx = 2;
        JPanel filterDropship = new JPanel();
        filterDropship.add(btnFilterDropship);
        filterDropship.add(lblFilterDropship);
        unitTypePanel.add(filterDropship, c);
        c.gridy++;
        c.gridx = 1;
        JPanel filterJumpshipPanel = new JPanel();
        filterJumpshipPanel.add(btnFilterJumpship);
        filterJumpshipPanel.add(lblFilterJumpship);
        unitTypePanel.add(filterJumpshipPanel, c);
        c.gridx = 2;
        JPanel filterWarshipPanel = new JPanel();
        filterWarshipPanel.add(btnFilterWarship);
        filterWarshipPanel.add(lblFilterWarship);
        unitTypePanel.add(filterWarshipPanel, c);
        c.gridy++;
        JPanel filterSpaceStationPanel = new JPanel();
        filterSpaceStationPanel.add(btnFilterSpaceStation);
        filterSpaceStationPanel.add(lblFilterSpaceStation);
        unitTypePanel.add(filterSpaceStationPanel, c);
        c.gridx = 0; c.gridy++;
        JPanel filterInfantryPanel = new JPanel();
        filterInfantryPanel.add(btnFilterInfantry);
        filterInfantryPanel.add(lblFilterInfantry);
        unitTypePanel.add(filterInfantryPanel, c);
        c.gridx = 1;
        JPanel filterBattleArmorPanel = new JPanel();
        filterBattleArmorPanel.add(btnFilterBattleArmor);
        filterBattleArmorPanel.add(lblFilterBattleArmor);
        unitTypePanel.add(filterBattleArmorPanel, c);
        c.gridx = 0; c.gridy++;
        JPanel filterTankPanel = new JPanel();
        filterTankPanel.add(btnFilterTank);
        filterTankPanel.add(lblFilterTank);
        unitTypePanel.add(filterTankPanel, c);
        c.gridx = 1;
        JPanel filterVTOLPanel = new JPanel();
        filterVTOLPanel.add(btnFilterVTOL);
        filterVTOLPanel.add(lblFilterVTOL);
        unitTypePanel.add(filterVTOLPanel, c);
        c.gridx = 2;
        JPanel filterrSupportVTOLPanel = new JPanel();
        filterrSupportVTOLPanel.add(btnFilterSupportVTOL);
        filterrSupportVTOLPanel.add(lblFilterSupportVTOL);
        unitTypePanel.add(filterrSupportVTOLPanel, c);
        c.gridy++;
        c.gridx = 1;
        JPanel filterGunEmplacementPanel = new JPanel();
        filterGunEmplacementPanel.add(btnFilterGunEmplacement);
        filterGunEmplacementPanel.add(lblFilterGunEmplacement);
        unitTypePanel.add(filterGunEmplacementPanel, c);
        c.gridy++;
        JPanel filterSupportTankPanel = new JPanel();
        filterSupportTankPanel.add(btnFilterSupportTank);
        filterSupportTankPanel.add(lblFilterSupportTank);
        unitTypePanel.add(filterSupportTankPanel, c);
        c.gridx = 2;
        JPanel filterrLargeSupportTankPanel = new JPanel();
        filterrLargeSupportTankPanel.add(btnFilterLargeSupportTank);
        filterrLargeSupportTankPanel.add(lblFilterLargeSupportTank);
        unitTypePanel.add(filterrLargeSupportTankPanel, c);
        c.gridy++;
        c.gridx = 1;
        JPanel filterSuperHeavyTankPanel = new JPanel();
        filterSuperHeavyTankPanel.add(btnFilterSuperHeavyTank);
        filterSuperHeavyTankPanel.add(lblFilterSuperHeavyTank);
        unitTypePanel.add(filterSuperHeavyTankPanel, c);
        c.weighty = 1;
        JPanel blankPanel = new JPanel();
        unitTypePanel.add(blankPanel, c);

        return unitTypePanel;
    }

    private JPanel createWeaponEqPanel() {
        // Setup table filter combo boxes
        DefaultComboBoxModel<String> unitTypeModel = new DefaultComboBoxModel<>();
        unitTypeModel.addElement(Messages.getString("MechSelectorDialog.All"));
        unitTypeModel.addElement(UnitType.getTypeDisplayableName(UnitType.MEK));
        unitTypeModel.addElement(UnitType.getTypeDisplayableName(UnitType.TANK));
        unitTypeModel.addElement(UnitType.getTypeDisplayableName(UnitType.BATTLE_ARMOR));
        unitTypeModel.addElement(UnitType.getTypeDisplayableName(UnitType.INFANTRY));
        unitTypeModel.addElement(UnitType.getTypeDisplayableName(UnitType.PROTOMEK));
        unitTypeModel.addElement(UnitType.getTypeDisplayableName(UnitType.AERO));
        unitTypeModel.setSelectedItem(Messages.getString("MechSelectorDialog.All"));

        cboUnitType.setModel(unitTypeModel);
        cboUnitType.addActionListener(this);

        DefaultComboBoxModel<String> techLevelModel = new DefaultComboBoxModel<>();
        for (int i = 0; i < TechConstants.SIZE; i++) {
            techLevelModel.addElement(TechConstants.getLevelDisplayableName(i));
        }
        techLevelModel.setSelectedItem(TechConstants.getLevelDisplayableName(TechConstants.SIZE - 1));
        cboTechLevel.setModel(techLevelModel);
        cboTechLevel.addActionListener(this);

        DefaultComboBoxModel<String> techClassModel = new DefaultComboBoxModel<>();
        techClassModel.addElement("All");
        techClassModel.addElement("Inner Sphere");
        techClassModel.addElement("Clan");
        techClassModel.addElement("IS/Clan");
        techClassModel.addElement("(Unknown Technology Base)");
        techClassModel.setSelectedItem("All");
        cboTechClass.setModel(techClassModel);
        cboTechClass.addActionListener(this);

        // Set up Weapon Class table
        weaponTypesModel = new WeaponClassTableModel();
        tblWeaponType = new MegamekTable(weaponTypesModel, WeaponClassTableModel.COL_NAME);
        TableColumn wpsTypeCol = tblWeaponType.getColumnModel().getColumn(WeaponClassTableModel.COL_QTY);
        wpsTypeCol.setCellEditor(new DefaultCellEditor(cboQty));
        tblWeaponType.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        weaponTypesSorter = new TableRowSorter<>(weaponTypesModel);
        tblWeaponType.setRowSorter(weaponTypesSorter);
        tblWeaponType.addKeyListener(this);
        tblWeaponType.setFont(new Font(MMConstants.FONT_MONOSPACED, Font.PLAIN, 12));
        tblWeaponType.getSelectionModel().addListSelectionListener(this);
        for (int i = 0; i < weaponTypesModel.getColumnCount(); i++) {
            tblWeaponType.getColumnModel().getColumn(i).setPreferredWidth(weaponTypesModel.getPreferredWidth(i));
        }
        scrTableWeaponType.setViewportView(tblWeaponType);

        // Setup Weapons Table
        weaponsModel = new WeaponsTableModel();
        tblWeapons = new MegamekTable(weaponsModel, WeaponsTableModel.COL_NAME);
        TableColumn wpsCol = tblWeapons.getColumnModel().getColumn(WeaponsTableModel.COL_QTY);
        wpsCol.setCellEditor(new DefaultCellEditor(cboQty));
        tblWeapons.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        weaponsSorter = new TableRowSorter<>(weaponsModel);
        tblWeapons.setRowSorter(weaponsSorter);
        tblWeapons.addKeyListener(this);
        tblWeapons.setFont(new Font(MMConstants.FONT_MONOSPACED, Font.PLAIN, 12));
        tblWeapons.getSelectionModel().addListSelectionListener(this);
        for (int i = 0; i < weaponsModel.getColumnCount(); i++) {
            tblWeapons.getColumnModel().getColumn(i).setPreferredWidth(weaponsModel.getPreferredWidth(i));
        }
        scrTableWeapons.setViewportView(tblWeapons);

        // Setup Equipment Table
        equipmentModel = new EquipmentTableModel();
        tblEquipment = new MegamekTable(equipmentModel, EquipmentTableModel.COL_NAME);
        TableColumn eqCol = tblEquipment.getColumnModel().getColumn(EquipmentTableModel.COL_QTY);
        eqCol.setCellEditor(new DefaultCellEditor(cboQty));
        tblEquipment.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        equipmentSorter = new TableRowSorter<>(equipmentModel);
        tblEquipment.setRowSorter(equipmentSorter);
        tblEquipment.addKeyListener(this);
        tblEquipment.setFont(new Font(MMConstants.FONT_MONOSPACED, Font.PLAIN, 12));
        tblEquipment.getSelectionModel().addListSelectionListener(this);
        for (int i = 0; i < equipmentModel.getColumnCount(); i++) {
            tblEquipment.getColumnModel().getColumn(i).setPreferredWidth(equipmentModel.getPreferredWidth(i));
        }
        scrTableEquipment.setViewportView(tblEquipment);

        // Populate Tables
        populateWeaponsAndEquipmentChoices();

        // initialize with the weapons sorted alphabetically by name
        ArrayList<RowSorter.SortKey> sortlist = new ArrayList<>();
        sortlist.add(new RowSorter.SortKey(WeaponsTableModel.COL_NAME, SortOrder.ASCENDING));
        tblWeapons.getRowSorter().setSortKeys(sortlist);
        ((DefaultRowSorter<?, ?>) tblWeapons.getRowSorter()).sort();
        tblWeapons.invalidate(); // force re-layout of window

        // initialize with the equipment sorted alphabetically by chassis
        sortlist = new ArrayList<>();
        sortlist.add(new RowSorter.SortKey(EquipmentTableModel.COL_NAME, SortOrder.ASCENDING));
        tblEquipment.getRowSorter().setSortKeys(sortlist);
        ((DefaultRowSorter<?, ?>) tblEquipment.getRowSorter()).sort();
        tblEquipment.invalidate(); // force re-layout of window

        txtEqExp.setEditable(false);
        txtEqExp.setLineWrap(true);
        txtEqExp.setWrapStyleWord(true);

        // table
        JPanel weaponEqPanel = new JPanel();
        weaponEqPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.weighty = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 0; c.gridy++;
        weaponEqPanel.add(lblTableFilters, c);
        c.gridx = 0; c.gridy++;
        c.gridwidth = 4;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 40, 0, 0);
        JPanel cboPanel = new JPanel();
        cboPanel.add(lblUnitType);
        cboPanel.add(cboUnitType);
        cboPanel.add(lblTechClass);
        cboPanel.add(cboTechClass);
        cboPanel.add(lblTechLevel, c);
        cboPanel.add(cboTechLevel, c);
        weaponEqPanel.add(cboPanel, c);
        c.gridwidth = 1;

        c.gridx = 0; c.gridy++;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 0, 0);
        weaponEqPanel.add(lblWeaponClass, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.gridwidth = 5;
        c.gridx = 0; c.gridy++;
        weaponEqPanel.add(scrTableWeaponType, c);
        c.gridwidth = 1;

        c.fill = GridBagConstraints.NONE;
        c.gridx = 0; c.gridy++;
        c.anchor = GridBagConstraints.WEST;
        weaponEqPanel.add(lblWeapons, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.gridwidth = 5;
        c.gridx = 0; c.gridy++;
        weaponEqPanel.add(scrTableWeapons, c);

        c.fill = GridBagConstraints.NONE;
        c.gridwidth = 1;
        c.gridx = 0; c.gridy++;
        c.anchor = GridBagConstraints.WEST;
        weaponEqPanel.add(lblEquipment, c);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.gridwidth = 5;
        c.gridx = 0; c.gridy++;
        weaponEqPanel.add(scrTableEquipment, c);

        c.insets = new Insets(0, 50, 0, 0);
        c.fill = GridBagConstraints.NONE;
        c.gridx = 0; c.gridy++;
        c.gridwidth = 4;
        JPanel btnPanel = new JPanel();
        btnPanel.add(btnAdd, c);
        btnPanel.add(btnLeftParen, c);
        btnPanel.add(btnRightParen, c);
        btnPanel.add(btnAnd, c);
        btnPanel.add(btnOr, c);
        btnPanel.add(btnBack, c);
        btnPanel.add(btnClear, c);
        weaponEqPanel.add(btnPanel, c);

        // Filter Expression
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 0; c.gridy++;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        weaponEqPanel.add(lblEqExpTxt, c);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridwidth = 3;
        weaponEqPanel.add(expScroller, c);
        c.weighty = 1;
        JPanel blankPanel = new JPanel();
        weaponEqPanel.add(blankPanel, c);

        return weaponEqPanel;
    }

    /**
     * Listener for check box state changes
     */
    @Override
    public void itemStateChanged(ItemEvent e) {

    }

    /**
     * Selection Listener for Weapons and Equipment tables. Checks to see if
     * a row is selected and if it is, enables the corresponding the add button.
     */
    @Override
    public void valueChanged(ListSelectionEvent evt) {
        boolean lastTokIsOperation;
        int tokSize = filterToks.size();
        lastTokIsOperation = ((tokSize == 0) ||
                (filterToks.elementAt(tokSize - 1) instanceof OperationFT));
        if (evt.getSource().equals(tblWeapons.getSelectionModel())) {
            if ((tblWeapons.getSelectedRow() >= 0) && lastTokIsOperation) {
                tblEquipment.clearSelection();
                tblWeaponType.clearSelection();
                btnAdd.setEnabled(true);
            } else if (tblWeapons.getSelectedRow() >= 0) {
                tblEquipment.clearSelection();
                tblWeaponType.clearSelection();
            }
        } else if (evt.getSource().equals(tblEquipment.getSelectionModel())) {
            if ((tblEquipment.getSelectedRow() >= 0) && lastTokIsOperation) {
                tblWeapons.clearSelection();
                tblWeaponType.clearSelection();
                btnAdd.setEnabled(true);
            } else if (tblEquipment.getSelectedRow() >= 0) {
                tblWeapons.clearSelection();
                tblWeaponType.clearSelection();
            }
        } else if (evt.getSource().equals(tblWeaponType.getSelectionModel())) {
            if ((tblWeaponType.getSelectedRow() >= 0) && lastTokIsOperation) {
                tblWeapons.clearSelection();
                tblEquipment.clearSelection();
                btnAdd.setEnabled(true);
            } else if (tblWeaponType.getSelectedRow() >= 0) {
                tblWeapons.clearSelection();
                tblEquipment.clearSelection();
            }
        }
    }

    /**
     * Convenience method for enabling the buttons related to weapon/equipment
     * selection for filtering (btnAddEquipment, btnAddWeapon, etc)
     */
    private void enableSelectionButtons() {
        if ((tblWeapons.getSelectedRow() != -1) ||
                (tblEquipment.getSelectedRow() != -1) ||
                (tblWeaponType.getSelectedRow() != -1)) {
            btnAdd.setEnabled(true);
        }
        btnLeftParen.setEnabled(true);
    }

    /**
     * Convenience method for disabling the buttons related to weapon/equipment
     * selection for filtering (btnAddEquipment, btnAddWeapon, etc)
     */
    private void disableSelectionButtons() {
        btnAdd.setEnabled(false);
        btnLeftParen.setEnabled(false);
    }

    /**
     * Convenience method for enabling the buttons related to filter operations
     * for filtering (btnAnd, btnOr, etc)
     */
    private void enableOperationButtons() {
        btnOr.setEnabled(true);
        btnAnd.setEnabled(true);
        btnRightParen.setEnabled(true);
    }

    /**
     * Convenience method for disabling the buttons related to filter operations
     * for filtering (btnAnd, btnOr, etc)
     */
    private void disableOperationButtons() {
        btnOr.setEnabled(false);
        btnAnd.setEnabled(false);
        btnRightParen.setEnabled(false);
    }

    public void prepareFilter() {
        try {
            mechFilter = new MechSearchFilter(mechFilter);
            mechFilter.createFilterExpressionFromTokens(filterToks);
            updateMechSearchFilter();
        } catch (MechSearchFilter.FilterParsingException e) {
            JOptionPane.showMessageDialog(this,
                    "Error parsing filter expression!\n\n" + e.msg,
                    "Filter Expression Parsing Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     *  Listener for button presses.
     */
    @Override
    public void actionPerformed(java.awt.event.ActionEvent ev) {
        if (ev.getSource().equals(cboUnitType)
                || ev.getSource().equals(cboTechLevel)
                || ev.getSource().equals(cboTechClass)) {
            filterTables();
        } else if (ev.getSource().equals(btnAdd)) {
            int row = tblEquipment.getSelectedRow();
            if (row >= 0) {
                String internalName = (String)
                        tblEquipment.getModel().getValueAt(
                                tblEquipment.convertRowIndexToModel(row),
                                EquipmentTableModel.COL_INTERNAL_NAME);
                String fullName = (String) tblEquipment.getValueAt(row, EquipmentTableModel.COL_NAME);
                int qty = Integer.parseInt((String)
                        tblEquipment.getValueAt(row, EquipmentTableModel.COL_QTY));
                filterToks.add(new EquipmentFT(internalName, fullName, qty));
                txtEqExp.setText(filterExpressionString());
                btnBack.setEnabled(true);
                enableOperationButtons();
                disableSelectionButtons();
            }
            row = tblWeapons.getSelectedRow();
            if (row >= 0) {
                String internalName = (String)
                        tblWeapons.getModel().getValueAt(
                                tblWeapons.convertRowIndexToModel(row),
                                WeaponsTableModel.COL_INTERNAL_NAME);
                String fullName = (String) tblWeapons.getValueAt(row, WeaponsTableModel.COL_NAME);
                int qty = Integer.parseInt((String)
                        tblWeapons.getValueAt(row, WeaponsTableModel.COL_QTY));
                filterToks.add(new EquipmentFT(internalName, fullName, qty));
                txtEqExp.setText(filterExpressionString());
                btnBack.setEnabled(true);
                enableOperationButtons();
                disableSelectionButtons();
            }
            row = tblWeaponType.getSelectedRow();
            if (row >= 0) {
                int qty = Integer.parseInt((String)tblWeaponType.getValueAt(row, WeaponClassTableModel.COL_QTY));
                filterToks.add(new WeaponClassFT((WeaponClass)tblWeaponType.getModel().getValueAt(tblWeaponType.convertRowIndexToModel(row), WeaponClassTableModel.COL_VAL), qty));
                txtEqExp.setText(filterExpressionString());
                btnBack.setEnabled(true);
                enableOperationButtons();
                disableSelectionButtons();
            }
        } else if (ev.getSource().equals(btnLeftParen)) {
            filterToks.add(new ParensFT("("));
            txtEqExp.setText(filterExpressionString());
            btnBack.setEnabled(true);
            disableOperationButtons();
            enableSelectionButtons();
            btnLeftParen.setEnabled(false);
            btnRightParen.setEnabled(false);
        } else if (ev.getSource().equals(btnRightParen)) {
            filterToks.add(new ParensFT(")"));
            txtEqExp.setText(filterExpressionString());
            btnBack.setEnabled(true);
            enableOperationButtons();
            disableSelectionButtons();
            btnLeftParen.setEnabled(false);
            btnRightParen.setEnabled(false);
        } else if (ev.getSource().equals(btnAnd)) {
            filterToks.add(new OperationFT(MechSearchFilter.BoolOp.AND));
            txtEqExp.setText(filterExpressionString());
            btnBack.setEnabled(true);
            disableOperationButtons();
            enableSelectionButtons();
        } else if (ev.getSource().equals(btnOr)) {
            filterToks.add(new OperationFT(MechSearchFilter.BoolOp.OR));
            txtEqExp.setText(filterExpressionString());
            btnBack.setEnabled(true);
            disableOperationButtons();
            enableSelectionButtons();
        } else if (ev.getSource().equals(btnBack)) {
            if (!filterToks.isEmpty()) {
                filterToks.remove(filterToks.size() - 1);
                txtEqExp.setText(filterExpressionString());
                if (filterToks.isEmpty()) {
                    btnBack.setEnabled(false);
                }

                if ((filterToks.isEmpty()) || (filterToks.lastElement() instanceof OperationFT)) {
                    disableOperationButtons();
                    enableSelectionButtons();
                } else {
                    enableOperationButtons();
                    disableSelectionButtons();
                }
            }
        } else if (ev.getSource().equals(btnClear)) {
            filterToks.clear();
            txtEqExp.setText("");
            btnBack.setEnabled(false);
            disableOperationButtons();
            enableSelectionButtons();
        } else if (ev.getSource().equals(btnFilterMech)) {
            toggleText(btnFilterMech);
        } else if (ev.getSource().equals(btnFilterBipedMech)) {
            toggleText(btnFilterBipedMech);
        } else if (ev.getSource().equals(btnFilterProtoMech)) {
            toggleText(btnFilterProtoMech);
        } else if (ev.getSource().equals(btnFilterLAM)) {
            toggleText(btnFilterLAM);
        } else if (ev.getSource().equals(btnFilterTripod)) {
            toggleText(btnFilterTripod);
        } else if (ev.getSource().equals(btnFilterQuad)) {
            toggleText(btnFilterQuad);
        } else if (ev.getSource().equals(btnFilterQuadVee)) {
            toggleText(btnFilterQuadVee);
        } else if (ev.getSource().equals(btnFilterAero)) {
            toggleText(btnFilterAero);
        } else if (ev.getSource().equals(btnFilterFixedWingSupport)) {
            toggleText(btnFilterFixedWingSupport);
        } else if (ev.getSource().equals(btnFilterConvFighter)) {
            toggleText(btnFilterConvFighter);
        } else if (ev.getSource().equals(btnFilterSmallCraft)) {
            toggleText(btnFilterSmallCraft);
        } else if (ev.getSource().equals(btnFilterDropship)) {
            toggleText(btnFilterDropship);
        } else if (ev.getSource().equals(btnFilterJumpship)) {
            toggleText(btnFilterJumpship);
        } else if (ev.getSource().equals(btnFilterWarship)) {
            toggleText(btnFilterWarship);
        } else if (ev.getSource().equals(btnFilterSpaceStation)) {
            toggleText(btnFilterSpaceStation);
        } else if (ev.getSource().equals(btnFilterInfantry)) {
            toggleText(btnFilterInfantry);
        } else if (ev.getSource().equals(btnFilterBattleArmor)) {
            toggleText(btnFilterBattleArmor);
        } else if (ev.getSource().equals(btnFilterTank)) {
            toggleText(btnFilterTank);
        } else if (ev.getSource().equals(btnFilterVTOL)) {
            toggleText(btnFilterVTOL);
        } else if (ev.getSource().equals(btnFilterSupportVTOL)) {
            toggleText(btnFilterSupportVTOL);
        } else if (ev.getSource().equals(btnFilterGunEmplacement)) {
            toggleText(btnFilterGunEmplacement);
        } else if (ev.getSource().equals(btnFilterFixedWingSupport)) {
            toggleText(btnFilterFixedWingSupport);
        } else if (ev.getSource().equals(btnFilterSuperHeavyTank)) {
            toggleText(btnFilterSuperHeavyTank);
        } else if (ev.getSource().equals(btnFilterSupportTank)) {
            toggleText(btnFilterSupportTank);
        } else if (ev.getSource().equals(btnFilterLargeSupportTank)) {
            toggleText(btnFilterLargeSupportTank);
        }
    }

    private void toggleText(JButton b) {
        if (b.getText().equals("\u2610")) {
            b.setText("\u2611");
        } else if (b.getText().equals("\u2611")) {
            b.setText("\u2612");
        } else if (b.getText().equals("\u2612")) {
            b.setText("\u2610");
        } else {
            b.setText("\u2610");
        }
    }

    private void toggleText(JList list, int index) {
        ListModel<String> m = list.getModel();
        DefaultListModel dlm  = new DefaultListModel();

        for (int i = 0; i < m.getSize(); i++) {
            String ms = m.getElementAt(i);

            if (index == i) {
                if (ms.contains("\u2610")) {
                    dlm.addElement("\u2611" + ms.substring(1, ms.length()));
                } else if (ms.contains("\u2611")) {
                    dlm.addElement("\u2612" + ms.substring(1, ms.length()));
                } else if (ms.contains("\u2612")) {
                    dlm.addElement("\u2610" + ms.substring(1, ms.length()));
                }
            } else {
                dlm.addElement(ms);
            }
        }

        list.setModel(dlm);
    }

    private boolean matchTechLvl(int t1, int t2) {
        return ((t1 == TechConstants.T_ALL) || (t1 == t2)
                || ((t1 == TechConstants.T_IS_TW_ALL) && (t2 <= TechConstants.T_IS_TW_NON_BOX)))

                || ((t1 == TechConstants.T_TW_ALL) && (t2 <= TechConstants.T_CLAN_TW))

                || ((t1 == TechConstants.T_ALL_IS) && ((t2 <= TechConstants.T_IS_TW_NON_BOX)
                || (t2 == TechConstants.T_IS_ADVANCED)
                || (t2 == TechConstants.T_IS_EXPERIMENTAL)
                || (t2 == TechConstants.T_IS_UNOFFICIAL)))

                || ((t1 == TechConstants.T_ALL_CLAN)
                && ((t2 == TechConstants.T_CLAN_TW)
                || (t2 == TechConstants.T_CLAN_ADVANCED)
                || (t2 == TechConstants.T_CLAN_EXPERIMENTAL)
                || (t2 == TechConstants.T_CLAN_UNOFFICIAL)));
    }

    private boolean matchTechClass(String t1, String t2) {
        if (t1.equals("All")) {
            return true;
        } else if (t1.equals("IS/Clan")) {
            return t2.equals("Inner Sphere") || t2.equals("Clan") || t1.equals(t2);
        } else {
            return t1.equals(t2);
        }
    }

    private boolean matchUnitType(int unitTypeFilter, EquipmentType eq) {
        // All is selected
        if (unitTypeFilter < 0) {
            return true;
        }

        switch (unitTypeFilter) {
            case 5:
                if (eq.hasFlag(WeaponType.F_AERO_WEAPON)
                        || eq.hasFlag(MiscType.F_FIGHTER_EQUIPMENT)) {
                    return true;
                }
                break;
            case UnitType.BATTLE_ARMOR:
                if (eq.hasFlag(WeaponType.F_BA_WEAPON)
                        || eq.hasFlag(MiscType.F_BA_EQUIPMENT)) {
                    return true;
                }
                break;
            case UnitType.INFANTRY:
                if (eq.hasFlag(WeaponType.F_INFANTRY)) {
                    return true;
                }
                break;
            case UnitType.MEK:
                if (eq.hasFlag(WeaponType.F_MECH_WEAPON)
                        || eq.hasFlag(MiscType.F_MECH_EQUIPMENT)) {
                    return true;
                }
                break;
            case UnitType.TANK:
                if (eq.hasFlag(WeaponType.F_TANK_WEAPON)
                        || eq.hasFlag(MiscType.F_TANK_EQUIPMENT)) {
                    return true;
                }
                break;
            case UnitType.PROTOMEK:
                if (eq.hasFlag(WeaponType.F_PROTO_WEAPON)
                        || eq.hasFlag(MiscType.F_PROTOMECH_EQUIPMENT)) {
                    return true;
                }
                break;
            default:
                return false;
        }
        return false;
    }

    void filterTables() {
        RowFilter<WeaponsTableModel, Integer> weaponFilter;
        RowFilter<EquipmentTableModel, Integer> equipmentFilter;
        final int techLevel = cboTechLevel.getSelectedIndex();
        final String techClass = (String) cboTechClass.getSelectedItem();
        final int unitType = cboUnitType.getSelectedIndex() - 1;
        // If current expression doesn't parse, don't update.
        try {
            weaponFilter = new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends WeaponsTableModel, ? extends Integer> entry) {
                    WeaponsTableModel weapModel = entry.getModel();
                    WeaponType wp = weapModel.getWeaponTypeAt(entry.getIdentifier());
                    String currTechClass = TechConstants.getTechName(wp.getTechLevel(gameYear));

                    boolean techLvlMatch = matchTechLvl(techLevel, wp.getTechLevel(gameYear));
                    boolean techClassMatch = matchTechClass(techClass, currTechClass);
                    boolean unitTypeMatch = matchUnitType(unitType, wp);
                    return techLvlMatch && techClassMatch && unitTypeMatch;
                }
            };
        } catch (java.util.regex.PatternSyntaxException ignored) {
            return;
        }
        weaponsSorter.setRowFilter(weaponFilter);

        try {
            equipmentFilter = new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends EquipmentTableModel, ? extends Integer> entry) {
                    EquipmentTableModel eqModel = entry.getModel();
                    EquipmentType eq = eqModel.getEquipmentTypeAt(entry.getIdentifier());
                    String currTechClass = TechConstants.getTechName(eq.getTechLevel(gameYear));
                    boolean techLvlMatch = matchTechLvl(techLevel, eq.getTechLevel(gameYear));
                    boolean techClassMatch = matchTechClass(techClass, currTechClass);
                    boolean unitTypeMatch = matchUnitType(unitType, eq);
                    return techLvlMatch && techClassMatch && unitTypeMatch;
                }
            };
        } catch (java.util.regex.PatternSyntaxException ignored) {
            return;
        }
        equipmentSorter.setRowFilter(equipmentFilter);
    }

    private String filterExpressionString() {
        // Build the string representation of the new expression
        StringBuilder filterExp = new StringBuilder();
        for (int i = 0; i < filterToks.size(); i++) {
            filterExp.append(" ").append(filterToks.elementAt(i).toString()).append(" ");
        }
        return filterExp.toString();
    }

    /**
     * Show the dialog. setVisible(true) blocks until setVisible(false).
     *
     * @return Return the filter that was created with this dialog.
     */
    public MechSearchFilter showDialog() {
        // We need to save a copy since the user can alter the filter state
        // and then click on the cancel button. We want to make sure the
        // original filter state is saved.
        MechSearchFilter currFilter = mechFilter;
        mechFilter = new MechSearchFilter(currFilter);
        txtEqExp.setText(mechFilter.getEquipmentExpression());
        if ((filterToks == null) || filterToks.isEmpty()
                || (filterToks.lastElement() instanceof OperationFT)) {
            disableOperationButtons();
            enableSelectionButtons();
        } else {
            enableOperationButtons();
            disableSelectionButtons();
        }
        setVisible(true);
        if (isCanceled) {
            mechFilter = currFilter;
        } else {
            updateMechSearchFilter();
        }
        return mechFilter;
    }

    /**
     *  Clear the filter.
     */
    public void clearValues() {
        tStartWalk.setText("");
        tEndWalk.setText("");
        tStartJump.setText("");
        tEndJump.setText("");
        tStartTroopSpace.setText("");
        tEndTroopSpace.setText("");
        tStartASFBays.setText("");
        tEndASFBays.setText("");
        tStartASFDoors.setText("");
        tEndASFDoors.setText("");
        tStartASFUnits.setText("");
        tEndASFUnits.setText("");
        tStartSmallCraftBays.setText("");
        tEndSmallCraftBays.setText("");
        tStartSmallCraftDoors.setText("");
        tEndSmallCraftDoors.setText("");
        tStartSmallCraftUnits.setText("");
        tEndSmallCraftUnits.setText("");
        tStartMechBays.setText("");
        tEndMechBays.setText("");
        tStartMechDoors.setText("");
        tEndMechDoors.setText("");
        tStartMechUnits.setText("");
        tEndMechUnits.setText("");
        tStartHeavyVehicleBays.setText("");
        tEndHeavyVehicleBays.setText("");
        tStartHeavyVehicleDoors.setText("");
        tEndHeavyVehicleDoors.setText("");
        tStartHeavyVehicleUnits.setText("");
        tEndHeavyVehicleUnits.setText("");
        tStartLightVehicleBays.setText("");
        tEndLightVehicleBays.setText("");
        tStartLightVehicleDoors.setText("");
        tEndLightVehicleDoors.setText("");
        tStartLightVehicleUnits.setText("");
        tEndLightVehicleUnits.setText("");
        tStartProtomechBays.setText("");
        tEndProtomechBays.setText("");
        tStartProtomechDoors.setText("");
        tEndProtomechDoors.setText("");
        tStartProtomechUnits.setText("");
        tEndProtomechUnits.setText("");
        tStartBattleArmorBays.setText("");
        tEndBattleArmorBays.setText("");
        tStartBattleArmorDoors.setText("");
        tEndBattleArmorDoors.setText("");
        tStartBattleArmorUnits.setText("");
        tEndBattleArmorUnits.setText("");
        tStartInfantryBays.setText("");
        tEndInfantryBays.setText("");
        tStartInfantryDoors.setText("");
        tEndInfantryDoors.setText("");
        tStartInfantryUnits.setText("");
        tEndInfantryUnits.setText("");
        tStartSuperHeavyVehicleBays.setText("");
        tEndSuperHeavyVehicleBays.setText("");
        tStartSuperHeavyVehicleDoors.setText("");
        tEndSuperHeavyVehicleDoors.setText("");
        tStartSuperHeavyVehicleUnits.setText("");
        tEndSuperHeavyVehicleUnits.setText("");
        tStartDropshuttleBays.setText("");
        tEndDropshuttleBays.setText("");
        tStartDropshuttleDoors.setText("");
        tEndDropshuttleDoors.setText("");
        tStartDropshuttleUnits.setText("");
        tEndDropshuttleUnits.setText("");
        tStartDockingCollars.setText("");
        tEndDockingCollars.setText("");
        tStartBattleArmorHandles.setText("");
        tEndBattleArmorHandles.setText("");
        tStartCargoBayUnits.setText("");
        tEndCargoBayUnits.setText("");
        tStartNavalRepairFacilities.setText("");
        tEndNavalRepairFacilities.setText("");
        cArmor.setSelectedIndex(0);
        cOfficial.setSelectedIndex(0);
        cCanon.setSelectedIndex(0);
        cClanEngine.setSelectedIndex(0);
        cOmni.setSelectedIndex(0);
        cMilitary.setSelectedIndex(0);
        tStartTankTurrets.setText("");
        tEndTankTurrets.setText("");
        tblWeapons.clearSelection();
        tblEquipment.clearSelection();
        txtEqExp.setText("");
        btnFilterMech.setText("\u2610");
        btnFilterBipedMech.setText("\u2610");
        btnFilterProtoMech.setText("\u2610");
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
        tStartYear.setText("");
        tEndYear.setText("");
        tStartTons.setText("");
        tEndTons.setText("");
        tStartBV.setText("");
        tEndBV.setText("");
        tSource.setText("");
        mechFilter = null;
        filterToks.clear();
        btnBack.setEnabled(false);

        cQuirkInclue.setSelectedIndex(0);
        cQuirkExclude.setSelectedIndex(1);
        ListModel<String> m = listQuirkType.getModel();
        DefaultListModel dlmq  = new DefaultListModel();

        for (int i = 0; i < m.getSize(); i++) {
            String ms = m.getElementAt(i);
            dlmq.addElement("\u2610 " + ms.substring(2, ms.length()));
        }

        listQuirkType.setModel(dlmq);

        cWeaponQuirkInclue.setSelectedIndex(0);
        cWeaponQuirkExclude.setSelectedIndex(1);

        m = listWeaponQuirkType.getModel();

        DefaultListModel dlmw  = new DefaultListModel();

        for (int i = 0; i < m.getSize(); i++) {
            String ms = m.getElementAt(i);
            dlmw.addElement("\u2610 " + ms.substring(2, ms.length()));
        }

        listWeaponQuirkType.setModel(dlmw);

        m = listArmorType.getModel();

        DefaultListModel dlmwa  = new DefaultListModel();

        for (int i = 0; i < m.getSize(); i++) {
            String ms = m.getElementAt(i);
            dlmwa.addElement("\u2610 " + ms.substring(2, ms.length()));
        }

        listArmorType.setModel(dlmwa);

        m = listCockpitType.getModel();

        DefaultListModel dlmc  = new DefaultListModel();

        for (int i = 0; i < m.getSize(); i++) {
            String ms = m.getElementAt(i);
            dlmc.addElement("\u2610 " + ms.substring(2, ms.length()));
        }

        listCockpitType.setModel(dlmc);

        m = listEngineType.getModel();

        DefaultListModel dlme  = new DefaultListModel();

        for (int i = 0; i < m.getSize(); i++) {
            String ms = m.getElementAt(i);
            dlme.addElement("\u2610 " + ms.substring(2, ms.length()));
        }

        listEngineType.setModel(dlme);

        m = listInternalsType.getModel();

        DefaultListModel dlmi  = new DefaultListModel();

        for (int i = 0; i < m.getSize(); i++) {
            String ms = m.getElementAt(i);
            dlmi.addElement("\u2610 " + ms.substring(2, ms.length()));
        }

        listInternalsType.setModel(dlmi);

        disableOperationButtons();
        enableSelectionButtons();
    }

    /**
     * Creates collections for all the possible <code>WeaponType</code>s and
     * <code>EquipmentType</code>s. These are used to populate the weapons
     * and equipment tables.
     */
    private void populateWeaponsAndEquipmentChoices() {
        Vector<WeaponType> weapons = new Vector<>();
        Vector<EquipmentType> equipment = new Vector<>();

        for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e.hasMoreElements();) {
            EquipmentType et = e.nextElement();
            if (et instanceof WeaponType) {
                weapons.add((WeaponType) et);
                // Check for C3+Tag and C3 Master Booster
                if (et.hasFlag(WeaponType.F_C3M) || et.hasFlag(WeaponType.F_C3MBS)) {
                    equipment.add(et);
                }
            } else if (et instanceof MiscType) {
                equipment.add(et);
            }
        }
        weaponsModel.setData(weapons);
        equipmentModel.setData(equipment);
    }

    public MechSearchFilter getMechSearchFilter() {
        return mechFilter;
    }

    /**
     * Update the search fields that aren't automatically updated.
     */
    protected void updateMechSearchFilter() {
        mechFilter.isDisabled = false;

        mechFilter.sStartWalk = tStartWalk.getText();
        mechFilter.sEndWalk = tEndWalk.getText();

        mechFilter.sStartJump = tStartJump.getText();
        mechFilter.sEndJump = tEndJump.getText();

        mechFilter.sStartTroopSpace = tStartTroopSpace.getText();
        mechFilter.sEndTroopSpace = tEndTroopSpace.getText();

        mechFilter.sStartASFBays = tStartASFBays.getText();
        mechFilter.sEndASFBays = tEndASFBays.getText();
        mechFilter.sStartASFDoors = tStartASFDoors.getText();
        mechFilter.sEndASFDoors = tEndASFDoors.getText();
        mechFilter.sStartASFUnits = tStartASFUnits.getText();
        mechFilter.sEndASFUnits = tEndASFUnits.getText();

        mechFilter.sStartSmallCraftBays = tStartSmallCraftBays.getText();
        mechFilter.sEndSmallCraftBays = tEndSmallCraftBays.getText();
        mechFilter.sStartSmallCraftDoors = tStartSmallCraftDoors.getText();
        mechFilter.sEndSmallCraftDoors = tEndSmallCraftDoors.getText();
        mechFilter.sStartSmallCraftUnits = tStartSmallCraftUnits.getText();
        mechFilter.sEndSmallCraftUnits = tEndSmallCraftUnits.getText();

        mechFilter.sStartMechBays = tStartMechBays.getText();
        mechFilter.sEndMechBays = tEndMechBays.getText();
        mechFilter.sStartMechDoors = tStartMechDoors.getText();
        mechFilter.sEndMechDoors = tEndMechDoors.getText();
        mechFilter.sStartMechUnits = tStartMechUnits.getText();
        mechFilter.sEndMechUnits = tEndMechUnits.getText();

        mechFilter.sStartHeavyVehicleBays = tStartHeavyVehicleBays.getText();
        mechFilter.sEndHeavyVehicleBays = tEndHeavyVehicleBays.getText();
        mechFilter.sStartHeavyVehicleDoors = tStartHeavyVehicleDoors.getText();
        mechFilter.sEndHeavyVehicleDoors = tEndHeavyVehicleDoors.getText();
        mechFilter.sStartHeavyVehicleUnits = tStartHeavyVehicleUnits.getText();
        mechFilter.sEndHeavyVehicleUnits = tEndHeavyVehicleUnits.getText();

        mechFilter.sStartLightVehicleBays = tStartLightVehicleBays.getText();
        mechFilter.sEndLightVehicleBays = tEndLightVehicleBays.getText();
        mechFilter.sStartLightVehicleDoors = tStartLightVehicleDoors.getText();
        mechFilter.sEndLightVehicleDoors = tEndLightVehicleDoors.getText();
        mechFilter.sStartLightVehicleUnits = tStartLightVehicleUnits.getText();
        mechFilter.sEndLightVehicleUnits = tEndLightVehicleUnits.getText();

        mechFilter.sStartProtomechBays = tStartProtomechBays.getText();
        mechFilter.sEndProtomechBays = tEndProtomechBays.getText();
        mechFilter.sStartProtomechDoors = tStartProtomechDoors.getText();
        mechFilter.sEndProtomechDoors = tEndProtomechDoors.getText();
        mechFilter.sStartProtomechUnits = tStartProtomechUnits.getText();
        mechFilter.sEndProtomechUnits = tEndProtomechUnits.getText();

        mechFilter.sStartBattleArmorBays = tStartBattleArmorBays.getText();
        mechFilter.sEndBattleArmorBays = tEndBattleArmorBays.getText();
        mechFilter.sStartBattleArmorDoors = tStartBattleArmorDoors.getText();
        mechFilter.sEndBattleArmorDoors = tEndBattleArmorDoors.getText();
        mechFilter.sStartBattleArmorUnits = tStartBattleArmorUnits.getText();
        mechFilter.sEndBattleArmorUnits = tEndBattleArmorUnits.getText();

        mechFilter.sStartInfantryBays = tStartInfantryBays.getText();
        mechFilter.sEndInfantryBays = tEndInfantryBays.getText();
        mechFilter.sStartInfantryDoors = tStartInfantryDoors.getText();
        mechFilter.sEndInfantryDoors = tEndInfantryDoors.getText();
        mechFilter.sStartInfantryUnits = tStartInfantryUnits.getText();
        mechFilter.sEndInfantryUnits = tEndInfantryUnits.getText();

        mechFilter.sStartSuperHeavyVehicleBays = tStartSuperHeavyVehicleBays.getText();
        mechFilter.sEndSuperHeavyVehicleBays = tEndSuperHeavyVehicleBays.getText();
        mechFilter.sStartSuperHeavyVehicleDoors = tStartSuperHeavyVehicleDoors.getText();
        mechFilter.sEndSuperHeavyVehicleDoors = tEndSuperHeavyVehicleDoors.getText();
        mechFilter.sStartSuperHeavyVehicleUnits = tStartSuperHeavyVehicleUnits.getText();
        mechFilter.sEndSuperHeavyVehicleUnits = tEndSuperHeavyVehicleUnits.getText();

        mechFilter.sStartDropshuttleBays = tStartDropshuttleBays.getText();
        mechFilter.sEndDropshuttleBays = tEndDropshuttleBays.getText();
        mechFilter.sStartDropshuttleDoors = tStartDropshuttleDoors.getText();
        mechFilter.sEndDropshuttleDoors = tEndDropshuttleDoors.getText();
        mechFilter.sStartDropshuttleUnits = tStartDropshuttleUnits.getText();
        mechFilter.sEndDropshuttleUnits = tEndDropshuttleUnits.getText();

        mechFilter.sStartDockingCollars = tStartDockingCollars.getText();
        mechFilter.sEndDockingCollars = tEndDockingCollars.getText();

        mechFilter.sStartBattleArmorHandles = tStartBattleArmorHandles.getText();
        mechFilter.sEndBattleArmorHandles = tEndBattleArmorHandles.getText();

        mechFilter.sStartCargoBayUnits = tStartCargoBayUnits.getText();
        mechFilter.sEndCargoBayUnits = tEndCargoBayUnits.getText();

        mechFilter.sStartNavalRepairFacilities = tStartNavalRepairFacilities.getText();
        mechFilter.sEndNavalRepairFacilities = tEndNavalRepairFacilities.getText();

        mechFilter.iArmor = cArmor.getSelectedIndex();
        mechFilter.iOmni = cOmni.getSelectedIndex();
        mechFilter.iMilitary = cMilitary.getSelectedIndex();
        mechFilter.sStartTankTurrets = tStartTankTurrets.getText();
        mechFilter.sEndTankTurrets = tEndTankTurrets.getText();
        mechFilter.iOfficial = cOfficial.getSelectedIndex();
        mechFilter.iCanon = cCanon.getSelectedIndex();
        mechFilter.iClanEngine = cClanEngine.getSelectedIndex();

        mechFilter.source = tSource.getText();

        mechFilter.sStartYear = tStartYear.getText();
        mechFilter.sEndYear = tEndYear.getText();

        mechFilter.sStartTons = tStartTons.getText();
        mechFilter.sEndTons = tEndTons.getText();

        mechFilter.sStartBV = tStartBV.getText();
        mechFilter.sEndBV = tEndBV.getText();

        mechFilter.quirkInclude = cQuirkInclue.getSelectedIndex();
        mechFilter.quirkExclude = cQuirkExclude.getSelectedIndex();

        ListModel<String> m = listQuirkType.getModel();

        for (int i = 0; i < m.getSize(); i++) {
            String ms = m.getElementAt(i);
            if (ms.contains("\u2611")) {
                mechFilter.quirkType.add(ms.substring(2, ms.length()));
            } else if (ms.contains("\u2612")) {
                mechFilter.quirkTypeExclude.add(ms.substring(2, ms.length()));
            }
        }

        mechFilter.weaponQuirkInclude = cWeaponQuirkInclue.getSelectedIndex();
        mechFilter.weaponQuirkExclude = cWeaponQuirkExclude.getSelectedIndex();

        m = listWeaponQuirkType.getModel();

        for (int i = 0; i < m.getSize(); i++) {
            String ms = m.getElementAt(i);
            if (ms.contains("\u2611")) {
                mechFilter.weaponQuirkType.add(ms.substring(2, ms.length()));
            } else if (ms.contains("\u2612")) {
                mechFilter.weaponQuirkTypeExclude.add(ms.substring(2, ms.length()));
            }
        }

        m = listArmorType.getModel();

        for (int i = 0; i < m.getSize(); i++) {
            String ms = m.getElementAt(i);
            if (ms.contains("\u2611")) {
                mechFilter.armorType.add(i);
            } else if (ms.contains("\u2612")) {
                mechFilter.armorTypeExclude.add(i);
            }
        }

        m = listCockpitType.getModel();

        for (int i = 0; i < m.getSize(); i++) {
            String ms = m.getElementAt(i);
            if (ms.contains("\u2611")) {
                mechFilter.cockpitType.add(i);
            } else if (ms.contains("\u2612")) {
                mechFilter.cockpitTypeExclude.add(i);
            }
        }

        m = listEngineType.getModel();

        for (int i = 0; i < m.getSize(); i++) {
            String ms = m.getElementAt(i);
            if (ms.contains("\u2611")) {
                mechFilter.engineType.add(ms.substring(2, ms.length()));
            } else if (ms.contains("\u2612")) {
                mechFilter.engineTypeExclude.add(ms.substring(2, ms.length()));
            }
        }

        m = listInternalsType.getModel();

        for (int i = 0; i < m.getSize(); i++) {
            String ms = m.getElementAt(i);
            if (ms.contains("\u2611")) {
                mechFilter.internalsType.add(i);
            } else if (ms.contains("\u2612")) {
                mechFilter.internalsTypeExclude.add(i);
            }
        }

        mechFilter.filterMech = getValue(btnFilterMech);
        mechFilter.filterBipedMech = getValue(btnFilterBipedMech);
        mechFilter.filterProtomech = getValue(btnFilterProtoMech);
        mechFilter.filterLAM = getValue(btnFilterLAM);
        mechFilter.filterTripod = getValue(btnFilterTripod);
        mechFilter.filterQuad = getValue(btnFilterQuad);
        mechFilter.filterQuadVee = getValue(btnFilterQuadVee);
        mechFilter.filterAero = getValue(btnFilterAero);
        mechFilter.filterFixedWingSupport = getValue(btnFilterFixedWingSupport);
        mechFilter.filterConvFighter = getValue(btnFilterConvFighter);
        mechFilter.filterSmallCraft = getValue(btnFilterSmallCraft);
        mechFilter.filterDropship = getValue(btnFilterDropship);
        mechFilter.filterJumpship = getValue(btnFilterJumpship);
        mechFilter.filterWarship = getValue(btnFilterWarship);
        mechFilter.filterSpaceStation = getValue(btnFilterSpaceStation);
        mechFilter.filterInfantry = getValue(btnFilterInfantry);
        mechFilter.filterBattleArmor = getValue(btnFilterBattleArmor);
        mechFilter.filterTank = getValue(btnFilterTank);
        mechFilter.filterVTOL = getValue(btnFilterVTOL);
        mechFilter.filterSupportVTOL = getValue(btnFilterSupportVTOL);
        mechFilter.filterGunEmplacement = getValue(btnFilterGunEmplacement);
        mechFilter.filterSupportTank = getValue(btnFilterSupportTank);
        mechFilter.filterLargeSupportTank = getValue(btnFilterLargeSupportTank);
        mechFilter.filterSuperHeavyTank = getValue(btnFilterSuperHeavyTank);
    }

    public int getValue(JButton b) {
        if (b.getText().equals("\u2610")) {
            return 0;
        } else if (b.getText().equals("\u2611")) {
            return 1;
        } else if (b.getText().equals("\u2612")) {
            return 2;
        } else {
            return -1;
        }
    }

    public class WeaponClassTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;

        private static final int COL_QTY = 0;
        private static final int COL_NAME = 1;
        private static final int N_COL = 2;
        private static final int COL_VAL = 2;


        private int[] qty;

        private Vector<WeaponClass> weaponClasses = new Vector<>();

        public WeaponClassTableModel() {
            for (WeaponClass cl : WeaponClass.values())
            {
                weaponClasses.add(cl);
            }
            qty = new int[weaponClasses.size()];
            Arrays.fill(qty, 1);
        }

        @Override
        public int getRowCount() {
            return weaponClasses.size();
        }

        @Override
        public int getColumnCount() {
            return N_COL;
        }

        public int getPreferredWidth(int col) {
            switch (col) {
                case COL_QTY:
                    return 40;
                case COL_NAME:
                    return 310;
                default:
                    return 0;
            }
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case COL_QTY:
                    return "Qty";
                case COL_NAME:
                    return "Weapon Class";
                default:
                    return "?";
            }
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            switch (col) {
                case COL_QTY:
                    return true;
                default:
                    return false;
            }
        }

        // fill table with values
        // public void setData(Vector<Integer> wps) {
        //     weaponClasses = wps;
        //     qty = new int[wps.size()];
        //     Arrays.fill(qty, 1);
        //     fireTableDataChanged();
        // }

        public WeaponClass getWeaponTypeAt(int row) {
            return weaponClasses.elementAt(row);
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (row >= weaponClasses.size()) {
                return null;
            }
            
            switch (col) {
                case COL_QTY:
                    return qty[row] + "";
                case COL_NAME:
                    return weaponClasses.elementAt(row).toString();
                case COL_VAL:
                    return weaponClasses.elementAt(row);
                default:
                    return "?";
            }
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            switch (col) {
                case COL_QTY:
                    qty[row] = Integer.parseInt((String) value);
                    fireTableCellUpdated(row, col);
                    break;
                default:
                    break;
            }
        }

    }


    /**
     * A table model for displaying weapons
     */
    public class WeaponsTableModel extends AbstractTableModel {

        private static final int COL_QTY = 0;
        private static final int COL_NAME = 1;
        private static final int COL_DMG = 2;
        private static final int COL_HEAT = 3;
        private static final int COL_SHORT = 4;
        private static final int COL_MED = 5;
        private static final int COL_LONG = 6;
        private static final int COL_IS_CLAN = 7;
        private static final int COL_LEVEL = 8;
        private static final int N_COL = 9;
        private static final int COL_INTERNAL_NAME = 9;

        private int[] qty;

        private Vector<WeaponType> weapons = new Vector<>();

        @Override
        public int getRowCount() {
            return weapons.size();
        }

        @Override
        public int getColumnCount() {
            return N_COL;
        }

        public int getPreferredWidth(int col) {
            switch (col) {
                case COL_QTY:
                    return 40;
                case COL_NAME:
                    return 310;
                case COL_IS_CLAN:
                    return 75;
                case COL_DMG:
                    return 50;
                case COL_HEAT:
                    return 50;
                case COL_SHORT:
                    return 50;
                case COL_MED:
                    return 50;
                case COL_LONG:
                    return 50;
                case COL_LEVEL:
                    return 100;
                default:
                    return 0;
            }
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case COL_QTY:
                    return "Qty";
                case COL_NAME:
                    return "Weapon Name";
                case COL_IS_CLAN:
                    return "IS/Clan";
                case COL_DMG:
                    return "DMG";
                case COL_HEAT:
                    return "Heat";
                case COL_SHORT:
                    return "Short";
                case COL_MED:
                    return "Med";
                case COL_LONG:
                    return "Long";
                case COL_LEVEL:
                    return "Lvl";
                default:
                    return "?";
            }
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            switch (col) {
                case COL_QTY:
                    return true;
                default:
                    return false;
            }
        }

        // fill table with values
        public void setData(Vector<WeaponType> wps) {
            weapons = wps;
            qty = new int[wps.size()];
            Arrays.fill(qty, 1);
            fireTableDataChanged();
        }

        public WeaponType getWeaponTypeAt(int row) {
            return weapons.elementAt(row);
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (row >= weapons.size()) {
                return null;
            }
            WeaponType wp = weapons.elementAt(row);
            switch (col) {
                case COL_QTY:
                    return qty[row] + "";
                case COL_NAME:
                    return wp.getName();
                case COL_IS_CLAN:
                    return TechConstants.getTechName(wp.getTechLevel(gameYear));
                case COL_DMG:
                    return wp.getDamage();
                case COL_HEAT:
                    return wp.getHeat();
                case COL_SHORT:
                    return wp.getShortRange();
                case COL_MED:
                    return wp.getMediumRange();
                case COL_LONG:
                    return wp.getLongRange();
                case COL_LEVEL:
                    return TechConstants.getSimpleLevelName(TechConstants
                            .convertFromNormalToSimple(wp
                                    .getTechLevel(gameYear)));
                case COL_INTERNAL_NAME:
                    return wp.getInternalName();
                default:
                    return "?";
            }
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            switch (col) {
                case COL_QTY:
                    qty[row] = Integer.parseInt((String) value);
                    fireTableCellUpdated(row, col);
                    break;
                default:
                    break;
            }
        }

    }

    /**
     * A table model for displaying equipment
     */
    public class EquipmentTableModel extends AbstractTableModel {

        private static final int COL_QTY = 0;
        private static final int COL_NAME = 1;
        private static final int COL_COST = 2;
        private static final int COL_IS_CLAN = 3;
        private static final int COL_LEVEL = 4;
        private static final int N_COL = 5;
        private static final int COL_INTERNAL_NAME = 5;

        private int[] qty;
        private Vector<EquipmentType> equipment = new Vector<>();

        @Override
        public int getRowCount() {
            return equipment.size();
        }

        @Override
        public int getColumnCount() {
            return N_COL;
        }

        public int getPreferredWidth(int col) {
            switch (col) {
                case COL_QTY:
                    return 40;
                case COL_NAME:
                    return 400;
                case COL_IS_CLAN:
                    return 75;
                case COL_COST:
                    return 175;
                case COL_LEVEL:
                    return 100;
                default:
                    return 0;
            }
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case COL_QTY:
                    return "Qty";
                case COL_NAME:
                    return "Name";
                case COL_IS_CLAN:
                    return "IS/Clan";
                case COL_COST:
                    return "Cost";
                case COL_LEVEL:
                    return "Lvl";
                default:
                    return "?";
            }
        }

        @Override
        public Class<?> getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            switch (col) {
                case COL_QTY:
                    return true;
                default:
                    return false;
            }
        }

        // fill table with values
        public void setData(Vector<EquipmentType> eq) {
            equipment = eq;
            qty = new int[eq.size()];
            Arrays.fill(qty, 1);
            fireTableDataChanged();
        }

        public EquipmentType getEquipmentTypeAt(int row) {
            return equipment.elementAt(row);
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (row >= equipment.size()) {
                return null;
            }
            EquipmentType eq = equipment.elementAt(row);
            switch (col) {
                case COL_QTY:
                    return qty[row] + "";
                case COL_NAME:
                    return eq.getName();
                case COL_IS_CLAN:
                    return TechConstants.getTechName(eq.getTechLevel(gameYear));
                case COL_COST:
                    return eq.getRawCost();
                case COL_LEVEL:
                    return TechConstants.getSimpleLevelName(TechConstants
                            .convertFromNormalToSimple(eq
                                    .getTechLevel(gameYear)));
                case COL_INTERNAL_NAME:
                    return eq.getInternalName();
                default:
                    return "?";
            }
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            switch (col) {
                case COL_QTY:
                    qty[row] = Integer.parseInt((String) value);
                    fireTableCellUpdated(row, col);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent evt) {

    }

    @Override
    public void keyReleased(KeyEvent evt) {

    }

    @Override
    public void keyTyped(KeyEvent evt) {
        char keyChar = evt.getKeyChar();
        // Ensure we've got a number or letter pressed
        if (!(((keyChar >= '0') && (keyChar <= '9')) ||
                ((keyChar >= 'a') && (keyChar <='z')) || (keyChar == ' '))) {
            return;
        }

        if (evt.getComponent().equals(tblWeapons)) {
            tblWeapons.keyTyped(evt);
        } else if (evt.getComponent().equals(tblEquipment)) {
            tblEquipment.keyTyped(evt);
        }
    }


    /**
     * Base class for different tokens that can be in a filter expression.
     * @author Arlith
     */
    public static class FilterTokens {

    }

    /**
     * FilterTokens subclass that represents parenthesis.
     * @author Arlith
     */
    public static class ParensFT extends FilterTokens {
        public String parens;

        public ParensFT(String p) {
            parens = p;
        }

        @Override
        public String toString() {
            return parens;
        }
    }

    /**
     * FilterTokens subclass that represents equipment.
     * @author Arlith
     */
    public static class EquipmentFT extends FilterTokens {
        public String internalName;
        public String fullName;
        public int qty;

        public EquipmentFT(String in, String fn, int q) {
            internalName = in;
            fullName = fn;
            qty = q;
        }

        @Override
        public String toString() {
            return qty + " " + fullName + ((qty != 1) ? "s" : "");
        }
    }

    /**
     * FilterTokens subclass that represents a boolean operation.
     * @author Arlith
     */
    public static class OperationFT extends FilterTokens {
        public MechSearchFilter.BoolOp op;

        public OperationFT(MechSearchFilter.BoolOp o) {
            op = o;
        }

        @Override
        public String toString() {
            if (op == MechSearchFilter.BoolOp.AND) {
                return "And";
            } else if (op == MechSearchFilter.BoolOp.OR) {
                return "Or";
            } else {
                return "";
            }
        }
    }

    public class WeaponClassFT extends FilterTokens {
        public WeaponClass weaponClass;
        public int qty;

        public WeaponClassFT(WeaponClass in_class, int in_qty) {
            weaponClass = in_class;
            qty = in_qty;
        }

        @Override
        public String toString() {
            if (qty == 1) {
                return qty + " " + weaponClass.toString();
            } else {
                return qty + " " + weaponClass.toString() + "s";
            }
        }
    }

    public static enum WeaponClass {
        AUTOCANNON {
            public String toString() {
                return "Autocannon";
            }
        },
        RAC,
        ULTRA {
            public String toString() {
                return "Ultra A/C";
            }
        },
        LIGHT {
            public String toString() {
                return "Light A/C";
            }
        },
        MACHINE_GUN {
            public String toString() {
                return "Machine Gun";
            }
        },
        GAUSS {
            public String toString() {
                return "Gauss";
            }
        },
        BALLISTIC {
            public String toString() {
                return "Ballistic";
            }
        },
        PLASMA {
            public String toString() {
                return "Plasma";
            }
        },
        ENERGY {
            public String toString() {
                return "Energy";
            }
        },
        LASER {
            public String toString() {
                return "Laser";
            }
        },
        PULSE {
            public String toString() {
                return "Pulse Laser";
            }
        },
        RE_ENGINEERED {
            public String toString() {
                return "Re-Engineered Laser";
            }
        },
        PPC {
            public String toString() {
                return "PPC";
            }
        },
        TASER {
            public String toString() {
                return "Taser";
            }
        },
        FLAMER {
            public String toString() {
                return "Flamer";
            }
        },
        MISSILE {
            public String toString() {
                return "Missile";
            }
        },
        LRM,
        MRM,
        SRM,
        PHYSICAL {
            public String toString() {
                return "Physical (inc. industrial equipment)";
            }
        },
        AMS,
        PRACTICAL_PHYSICAL {
            public String toString() {
                return "Physical (weapons only)";
            }
        };

        public boolean matches(String name) {
            if (name.toLowerCase().contains("ammo")) {
                return false;
            }
            if (this == PHYSICAL) {
                String lName = name.toLowerCase();

                if (lName.contains("backhoe") || 
                    lName.contains("saw") ||
                    lName.contains("whip") ||
                    lName.contains("claw") ||
                    lName.contains("combine") ||
                    lName.contains("flail") ||
                    lName.contains("hatchet") ||
                    lName.contains("driver") ||
                    lName.contains("lance") ||
                    lName.contains("mace") ||
                    lName.contains("drill") ||
                    lName.contains("ram") ||
                    lName.contains("blade") ||
                    lName.contains("cutter") ||
                    lName.contains("shield") ||
                    lName.contains("welder") ||
                    lName.contains("sword") ||
                    lName.contains("talons") ||
                    lName.contains("wrecking")) {
                    return true;
                }
            } else if (this == PRACTICAL_PHYSICAL) {
                String lName = name.toLowerCase();

                if (lName.contains("claw") ||
                    lName.contains("flail") ||
                    lName.contains("hatchet") ||
                    lName.contains("lance") ||
                    lName.contains("mace") ||
                    lName.contains("blade") ||
                    lName.contains("shield") ||
                    lName.contains("sword") ||
                    lName.contains("talons")) {
                    return true;
                }
            } else if (this == MISSILE) {
                if ((name.toLowerCase().contains("lrm") ||
                    name.toLowerCase().contains("mrm") || 
                    name.toLowerCase().contains("srm")) && 
                    !name.toLowerCase().contains("ammo")) {
                    return true;
                }
            } else if (this == RE_ENGINEERED) { 
                if (name.toLowerCase().contains("engineered")) {
                    return true;
                }
            } else if (this == ENERGY) {
                if (WeaponClass.LASER.matches(name) || WeaponClass.PPC.matches(name) || WeaponClass.FLAMER.matches(name)) {
                    return true;
                }
            } else if (this == MACHINE_GUN) {
                if ((name.toLowerCase().contains("mg") || name.toLowerCase().contains("machine")) && !name.toLowerCase().contains("ammo")) {
                    return true;
                }
            } else if (this == BALLISTIC) {
                return WeaponClass.AUTOCANNON.matches(name) || 
                    WeaponClass.GAUSS.matches(name) || 
                    WeaponClass.MISSILE.matches(name) || 
                    WeaponClass.MACHINE_GUN.matches(name);
            } else if (this == RAC) {
                if (name.toLowerCase().contains("rotary")) {
                    return true;
                }
            } else if (this == ULTRA) {
                if (name.toLowerCase().contains("ultraa")) {
                    return true;
                }
            } else if (name.toLowerCase().contains(this.name().toLowerCase()) && !name.toLowerCase().contains("ammo")) {
                return true;
            }
            return false;
        }
    }

    public void adaptToGUIScale() {
        scrTableWeaponType.setMinimumSize(new Dimension(UIUtil.scaleForGUI(650), UIUtil.scaleForGUI(150)));
        scrTableWeaponType.setPreferredSize(new Dimension(UIUtil.scaleForGUI(650), UIUtil.scaleForGUI(150)));
        scrTableWeapons.setMinimumSize(new Dimension(UIUtil.scaleForGUI(650), UIUtil.scaleForGUI(250)));
        scrTableWeapons.setPreferredSize(new Dimension(UIUtil.scaleForGUI(650), UIUtil.scaleForGUI(250)));
        scrTableEquipment.setMinimumSize(new Dimension(UIUtil.scaleForGUI(650), UIUtil.scaleForGUI(250)));
        scrTableEquipment.setPreferredSize(new Dimension(UIUtil.scaleForGUI(650), UIUtil.scaleForGUI(250)));
    }
}
