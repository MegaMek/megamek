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
import megamek.common.options.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
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

    // Weapons / Equipment
    private JButton btnWELeftParen = new JButton("(");
    private JButton btnWERightParen = new JButton(")");
    private JButton btnWEAdd = new JButton(Messages.getString("MechSelectorDialog.Search.add"));
    private JButton btnWEAnd = new JButton(Messages.getString("MechSelectorDialog.Search.and"));
    private JButton btnWEOr = new JButton(Messages.getString("MechSelectorDialog.Search.or"));
    private JButton btnWEClear = new JButton(Messages.getString("MechSelectorDialog.Reset"));
    private JButton btnWEBack = new JButton("Back");
    private JLabel  lblWEEqExpTxt = new JLabel(Messages.getString("MechSelectorDialog.Search.FilterExpression"));
    private JTextArea  txtWEEqExp = new JTextArea("");
    private JScrollPane expWEScroller = new JScrollPane(txtWEEqExp,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    private JLabel lblUnitType = new JLabel(Messages.getString("MechSelectorDialog.Search.UnitType"));
    private JLabel lblTechClass = new JLabel(Messages.getString("MechSelectorDialog.Search.TechClass"));
    private JLabel lblTechLevelBase = new JLabel(Messages.getString("MechSelectorDialog.Search.TechLevel"));
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
    private JComboBox<String> cboQty = new JComboBox<>();

    // Base
    private JButton btnBaseClear = new JButton(Messages.getString("MechSelectorDialog.ClearTab"));
    private JLabel lblWalk = new JLabel(Messages.getString("MechSelectorDialog.Search.Walk"));
    private JTextField tStartWalk = new JTextField(4);
    private JTextField tEndWalk = new JTextField(4);
    private JLabel lblJump = new JLabel(Messages.getString("MechSelectorDialog.Search.Jump"));
    private JTextField tStartJump = new JTextField(4);
    private JTextField tEndJump = new JTextField(4);
    private JLabel lblTankTurrets = new JLabel(Messages.getString("MechSelectorDialog.Search.TankTurrets"));
    private JTextField tStartTankTurrets = new JTextField(4);
    private JTextField tEndTankTurrets= new JTextField(4);
    private JLabel lblLowerArms = new JLabel(Messages.getString("MechSelectorDialog.Search.LowerArms"));
    private JTextField tStartLowerArms = new JTextField(4);
    private JTextField tEndLowerArms = new JTextField(4);
    private JLabel lblHands = new JLabel(Messages.getString("MechSelectorDialog.Search.Hands"));
    private JTextField tStartHands = new JTextField(4);
    private JTextField tEndHands = new JTextField(4);
    private JLabel lblArmor = new JLabel(Messages.getString("MechSelectorDialog.Search.Armor"));
    private JComboBox<String> cArmor = new JComboBox<>();
    private JLabel lblOfficial = new JLabel(Messages.getString("MechSelectorDialog.Search.Official"));
    private JComboBox<String> cOfficial = new JComboBox<>();
    private JLabel lblCanon = new JLabel(Messages.getString("MechSelectorDialog.Search.Canon"));
    private JComboBox<String> cCanon = new JComboBox<>();
    private JLabel lblPatchwork = new JLabel(Messages.getString("MechSelectorDialog.Search.Patchwork"));
    private JComboBox<String> cPatchwork = new JComboBox<>();
    private JLabel lblInvalid = new JLabel(Messages.getString("MechSelectorDialog.Search.Invalid"));
    private JComboBox<String> cInvalid = new JComboBox<>();
    private JLabel lblFailedToLoadEquipment = new JLabel(Messages.getString("MechSelectorDialog.Search.FailedToLoadEquipment"));
    private JComboBox<String> cFailedToLoadEquipment = new JComboBox<>();
    private JLabel lblClanEngine = new JLabel(Messages.getString("MechSelectorDialog.Search.ClanEngine"));
    private JComboBox<String> cClanEngine = new JComboBox<>();
    private JLabel lblTableFilters = new JLabel(Messages.getString("MechSelectorDialog.Search.TableFilters"));
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
    private JLabel lblCockpitType = new JLabel(Messages.getString("MechSelectorDialog.Search.CockpitType"));
    private JList<TriStateItem> listCockpitType = new JList<>(new DefaultListModel<TriStateItem>());
    private JScrollPane spCockpitType = new JScrollPane(listCockpitType);
    private JLabel lblArmorType = new JLabel(Messages.getString("MechSelectorDialog.Search.ArmorType"));
    private JList<TriStateItem> listArmorType = new JList<>(new DefaultListModel<TriStateItem>());
    private JScrollPane spArmorType = new JScrollPane(listArmorType);
    private JLabel lblInternalsType = new JLabel(Messages.getString("MechSelectorDialog.Search.InternalsType"));
    private JList<TriStateItem> listInternalsType = new JList<>(new DefaultListModel<TriStateItem>());
    private JScrollPane spInternalsType = new JScrollPane(listInternalsType);
    private JLabel lblEngineType = new JLabel(Messages.getString("MechSelectorDialog.Search.Engine"));
    private JList<TriStateItem> listEngineType = new JList<>(new DefaultListModel<TriStateItem>());
    private JScrollPane spEngineType = new JScrollPane(listEngineType);
    private JLabel lblGyroType = new JLabel(Messages.getString("MechSelectorDialog.Search.Gyro"));
    private JList<TriStateItem> listGyroType = new JList<>(new DefaultListModel<TriStateItem>());
    private JScrollPane spGyroType = new JScrollPane(listGyroType);
    private JLabel lblTechLevel = new JLabel(Messages.getString("MechSelectorDialog.Search.TechLevel"));
    private JList<TriStateItem> listTechLevel = new JList<>(new DefaultListModel<TriStateItem>());
    private JScrollPane spTechLevel = new JScrollPane(listTechLevel);
    private JLabel lblTechBase = new JLabel(Messages.getString("MechSelectorDialog.Search.TechBase"));
    private JList<TriStateItem> listTechBase = new JList<>(new DefaultListModel<TriStateItem>());
    private JScrollPane spTechBase = new JScrollPane(listTechBase);

    // Transports
    private JButton btnTransportsClear = new JButton(Messages.getString("MechSelectorDialog.ClearTab"));
    private JLabel lblTroopSpace = new JLabel(Messages.getString("MechSelectorDialog.Search.TroopSpace"));
    private JTextField tStartTroopSpace  = new JTextField(4);
    private JTextField tEndTroopSpace = new JTextField(4);
    private JLabel lblASFBays = new JLabel(Messages.getString("MechSelectorDialog.Search.ASFBays"));
    private JTextField tStartASFBays = new JTextField(4);
    private JTextField tEndASFBays = new JTextField(4);
    private JLabel lblASFDoors = new JLabel(Messages.getString("MechSelectorDialog.Search.Doors"));
    private JTextField tStartASFDoors = new JTextField(4);
    private JTextField tEndASFDoors = new JTextField(4);
    private JLabel lblASFUnits = new JLabel(Messages.getString("MechSelectorDialog.Search.Units"));
    private JTextField tStartASFUnits = new JTextField(4);
    private JTextField tEndASFUnits = new JTextField(4);
    private JLabel lblSmallCraftBays = new JLabel(Messages.getString("MechSelectorDialog.Search.SmallCraftBays"));
    private JTextField tStartSmallCraftBays = new JTextField(4);
    private JTextField tEndSmallCraftBays = new JTextField(4);
    private JLabel lblSmallCraftDoors = new JLabel(Messages.getString("MechSelectorDialog.Search.Doors"));
    private JTextField tStartSmallCraftDoors = new JTextField(4);
    private JTextField tEndSmallCraftDoors = new JTextField(4);
    private JLabel lblSmallCraftUnits = new JLabel(Messages.getString("MechSelectorDialog.Search.Units"));
    private JTextField tStartSmallCraftUnits = new JTextField(4);
    private JTextField tEndSmallCraftUnits = new JTextField(4);
    private JLabel lblMechBays = new JLabel(Messages.getString("MechSelectorDialog.Search.MechBays"));
    private JTextField tStartMechBays = new JTextField(4);
    private JTextField tEndMechBays = new JTextField(4);
    private JLabel lblMechDoors = new JLabel(Messages.getString("MechSelectorDialog.Search.Doors"));
    private JTextField tStartMechDoors = new JTextField(4);
    private JTextField tEndMechDoors = new JTextField(4);
    private JLabel lblMechUnits = new JLabel(Messages.getString("MechSelectorDialog.Search.Units"));
    private JTextField tStartMechUnits = new JTextField(4);
    private JTextField tEndMechUnits = new JTextField(4);
    private JLabel lblHeavyVehicleBays = new JLabel(Messages.getString("MechSelectorDialog.Search.HeavyVehicleBays"));
    private JTextField tStartHeavyVehicleBays = new JTextField(4);
    private JTextField tEndHeavyVehicleBays = new JTextField(4);
    private JLabel lblHeavyVehicleDoors = new JLabel(Messages.getString("MechSelectorDialog.Search.Doors"));
    private JTextField tStartHeavyVehicleDoors = new JTextField(4);
    private JTextField tEndHeavyVehicleDoors = new JTextField(4);
    private JLabel lblHeavyVehicleUnits = new JLabel(Messages.getString("MechSelectorDialog.Search.Units"));
    private JTextField tStartHeavyVehicleUnits = new JTextField(4);
    private JTextField tEndHeavyVehicleUnits = new JTextField(4);
    private JLabel lblLightVehicleBays = new JLabel(Messages.getString("MechSelectorDialog.Search.LightVehicleBays"));
    private JTextField tStartLightVehicleBays = new JTextField(4);
    private JTextField tEndLightVehicleBays = new JTextField(4);
    private JLabel lblLightVehicleDoors = new JLabel(Messages.getString("MechSelectorDialog.Search.Doors"));
    private JTextField tStartLightVehicleDoors = new JTextField(4);
    private JTextField tEndLightVehicleDoors = new JTextField(4);
    private JLabel lblLightVehicleUnits = new JLabel(Messages.getString("MechSelectorDialog.Search.Units"));
    private JTextField tStartLightVehicleUnits = new JTextField(4);
    private JTextField tEndLightVehicleUnits = new JTextField(4);
    private JLabel lblProtomechBays = new JLabel(Messages.getString("MechSelectorDialog.Search.ProtomechBays"));
    private JTextField tStartProtomechBays = new JTextField(4);
    private JTextField tEndProtomechBays = new JTextField(4);
    private JLabel lblProtomechDoors = new JLabel(Messages.getString("MechSelectorDialog.Search.Doors"));
    private JTextField tStartProtomechDoors = new JTextField(4);
    private JTextField tEndProtomechDoors = new JTextField(4);
    private JLabel lblProtomechUnits = new JLabel(Messages.getString("MechSelectorDialog.Search.Units"));
    private JTextField tStartProtomechUnits = new JTextField(4);
    private JTextField tEndProtomechUnits = new JTextField(4);
    private JLabel lblBattleArmorBays = new JLabel(Messages.getString("MechSelectorDialog.Search.BattleArmorBays"));
    private JTextField tStartBattleArmorBays = new JTextField(4);
    private JTextField tEndBattleArmorBays = new JTextField(4);
    private JLabel lblBattleArmorDoors = new JLabel(Messages.getString("MechSelectorDialog.Search.Doors"));
    private JTextField tStartBattleArmorDoors = new JTextField(4);
    private JTextField tEndBattleArmorDoors = new JTextField(4);
    private JLabel lblBattleArmorUnits = new JLabel(Messages.getString("MechSelectorDialog.Search.Units"));
    private JTextField tStartBattleArmorUnits = new JTextField(4);
    private JTextField tEndBattleArmorUnits = new JTextField(4);
    private JLabel lblInfantryBays = new JLabel(Messages.getString("MechSelectorDialog.Search.InfantryBays"));
    private JTextField tStartInfantryBays = new JTextField(4);
    private JTextField tEndInfantryBays = new JTextField(4);
    private JLabel lblInfantryDoors = new JLabel(Messages.getString("MechSelectorDialog.Search.Doors"));
    private JTextField tStartInfantryDoors = new JTextField(4);
    private JTextField tEndInfantryDoors = new JTextField(4);
    private JLabel lblInfantryUnits = new JLabel(Messages.getString("MechSelectorDialog.Search.Units"));
    private JTextField tStartInfantryUnits = new JTextField(4);
    private JTextField tEndInfantryUnits = new JTextField(4);
    private JLabel lblSuperHeavyVehicleBays = new JLabel(Messages.getString("MechSelectorDialog.Search.SuperHeavyVehicleBays"));
    private JTextField tStartSuperHeavyVehicleBays = new JTextField(4);
    private JTextField tEndSuperHeavyVehicleBays = new JTextField(4);
    private JLabel lblSuperHeavyVehicleDoors = new JLabel(Messages.getString("MechSelectorDialog.Search.Doors"));
    private JTextField tStartSuperHeavyVehicleDoors = new JTextField(4);
    private JTextField tEndSuperHeavyVehicleDoors = new JTextField(4);
    private JLabel lblSuperHeavyVehicleUnits = new JLabel(Messages.getString("MechSelectorDialog.Search.Units"));
    private JTextField tStartSuperHeavyVehicleUnits = new JTextField(4);
    private JTextField tEndSuperHeavyVehicleUnits = new JTextField(4);
    private JLabel lblDropshuttleBays = new JLabel(Messages.getString("MechSelectorDialog.Search.DropshuttleBays"));
    private JTextField tStartDropshuttleBays = new JTextField(4);
    private JTextField tEndDropshuttleBays = new JTextField(4);
    private JLabel lblDropshuttleDoors = new JLabel(Messages.getString("MechSelectorDialog.Search.Doors"));
    private JTextField tStartDropshuttleDoors = new JTextField(4);
    private JTextField tEndDropshuttleDoors = new JTextField(4);
    private JLabel lblDropshuttleUnits = new JLabel(Messages.getString("MechSelectorDialog.Search.Units"));
    private JTextField tStartDropshuttleUnits = new JTextField(4);
    private JTextField tEndDropshuttleUnits = new JTextField(4);
    private JLabel lblDockingCollars = new JLabel(Messages.getString("MechSelectorDialog.Search.DockingCollars"));
    private JTextField tStartDockingCollars = new JTextField(4);
    private JTextField tEndDockingCollars = new JTextField(4);
    private JLabel lblBattleArmorHandles = new JLabel(Messages.getString("MechSelectorDialog.Search.BattleArmorHandles"));
    private JTextField tStartBattleArmorHandles = new JTextField(4);
    private JTextField tEndBattleArmorHandles = new JTextField(4);
    private JLabel lblCargoBayUnits = new JLabel(Messages.getString("MechSelectorDialog.Search.CargoBayUnits"));
    private JTextField tStartCargoBayUnits = new JTextField(4);
    private JTextField tEndCargoBayUnits = new JTextField(4);
    private JLabel lblNavalRepairFacilities = new JLabel(Messages.getString("MechSelectorDialog.Search.NavalRepairFacilities"));
    private JTextField tStartNavalRepairFacilities = new JTextField(4);
    private JTextField tEndNavalRepairFacilities = new JTextField(4);

    // Quirks
    private JButton btnQuirksClear = new JButton(Messages.getString("MechSelectorDialog.ClearTab"));
    private JLabel lblQuirkInclude = new JLabel("\u2611");
    private JComboBox<String> cQuirkInclue = new JComboBox<>();
    private JLabel lblQuirkExclude = new JLabel("\u2612");
    private JComboBox<String> cQuirkExclude = new JComboBox<>();
    private JLabel lblQuirkType = new JLabel(Messages.getString("MechSelectorDialog.Search.Quirk"));
    private JList<TriStateItem> listQuirkType = new JList<>(new DefaultListModel<TriStateItem>());
    private JScrollPane spQuirkType = new JScrollPane(listQuirkType);
    private JLabel lblWeaponQuirkInclude = new JLabel("\u2611");
    private JComboBox<String> cWeaponQuirkInclue = new JComboBox<>();
    private JLabel lblWeaponQuirkExclude = new JLabel("\u2612");
    private JComboBox<String> cWeaponQuirkExclude = new JComboBox<>();
    private JLabel lblWeaponQuirkType = new JLabel(Messages.getString("MechSelectorDialog.Search.WeaponQuirk"));
    private JList<TriStateItem> listWeaponQuirkType = new JList<>(new DefaultListModel<TriStateItem>());
    private JScrollPane spWeaponQuirkType = new JScrollPane(listWeaponQuirkType);

    // Unit Type
    private JButton btnUnitTypeClear = new JButton(Messages.getString("MechSelectorDialog.ClearTab"));
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
    private JLabel lblFilterOmni = new JLabel(Messages.getString("MechSelectorDialog.Search.Omni"));
    private JButton btnFilterOmni = new JButton("\u2610");
    private JLabel lblFilterMilitary = new JLabel(Messages.getString("MechSelectorDialog.Search.Military"));
    private JButton btnFilterMilitary = new JButton("\u2610");
    private JLabel lblFilterIndustrial = new JLabel(Messages.getString("MechSelectorDialog.Search.Industrial"));
    private JButton btnFilterIndustrial = new JButton("\u2610");
    private JLabel lblFilterMountedInfantry = new JLabel(Messages.getString("MechSelectorDialog.Search.MountedInfantry"));
    private JButton btnFilterMountedInfantry = new JButton("\u2610");
    private JLabel lblFilterWaterOnly = new JLabel(Messages.getString("MechSelectorDialog.Search.WaterOnly"));
    private JButton btnFilterWaterOnly = new JButton("\u2610");
    private JLabel lblFilterSupportVehicle = new JLabel(Messages.getString("MechSelectorDialog.Search.SupportVehicle"));
    private JButton btnFilterSupportVehicle = new JButton("\u2610");
    private JLabel lblFilterAerospaceFighter = new JLabel(Messages.getString("MechSelectorDialog.Search.AerospaceFighter"));
    private JButton btnFilterAerospaceFighter = new JButton("\u2610");
    private JLabel lblFilterDoomedOnGround = new JLabel(Messages.getString("MechSelectorDialog.Search.DoomedOnGround"));
    private JButton btnFilterDoomedOnGround = new JButton("\u2610");
    private JLabel lblFilterDoomedInAtmosphere = new JLabel(Messages.getString("MechSelectorDialog.Search.DoomedInAtmosphere"));
    private JButton btnFilterDoomedInAtmosphere = new JButton("\u2610");
    private JLabel lblFilterDoomedInSpace = new JLabel(Messages.getString("MechSelectorDialog.Search.DoomedInSpace"));
    private JButton btnFilterDoomedInSpace = new JButton("\u2610");
    private JLabel lblFilterDoomedInExtremeTemp = new JLabel(Messages.getString("MechSelectorDialog.Search.DoomedInExtremeTemp"));
    private JButton btnFilterDoomedInExtremeTemp = new JButton("\u2610");
    private JLabel lblFilterDoomedInVacuum = new JLabel(Messages.getString("MechSelectorDialog.Search.DoomedInVacuum"));
    private JButton btnFilterDoomedInVacuum = new JButton("\u2610");

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

    private static class TriStateItem {
        public String state;
        public String text;
        public int code;

        public TriStateItem(String state, String text) {
            this.state = state;
            this.text = text;
        }

        public TriStateItem(String state, int code, String text) {
            this.state = state;
            this.code = code;
            this.text = text;
        }

        @Override
        public String toString() {
            return state + " " + text;
        }
    }

    private void jlistSetup(JList l) {
        l.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        l.setSelectionModel(new NoSelectionModel());
        l.addMouseListener(new MouseAdapter() {
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
    }

    private void loadTriStateItem(List<String> s, JList l, int count) {
        DefaultListModel dlma = new DefaultListModel();

        for (String desc : s) {
            dlma.addElement(new TriStateItem("\u2610", desc));
        }

        l.setModel(dlma);
        l.setVisibleRowCount(count);
        jlistSetup(l);
    }

    private void loadTriStateItem(Map<Integer, String> s, JList l, int count) {
        DefaultListModel dlma = new DefaultListModel();

        for (Map.Entry<Integer, String> desc : s.entrySet()) {
            dlma.addElement(new TriStateItem("\u2610", desc.getKey(), desc.getValue()));
        }

        l.setModel(dlma);
        l.setVisibleRowCount(count);
        jlistSetup(l);
    }

    private void loadYesNo(JComboBox<String> cb) {
        cb.addItem(Messages.getString("MechSelectorDialog.Search.Any"));
        cb.addItem(Messages.getString("MechSelectorDialog.Search.Yes"));
        cb.addItem(Messages.getString("MechSelectorDialog.Search.No"));
    }

    private JPanel createBasePanel() {
        btnBaseClear.addActionListener(this);

        cArmor.addItem(Messages.getString("MechSelectorDialog.Search.Any"));
        cArmor.addItem(Messages.getString("MechSelectorDialog.Search.Armor25"));
        cArmor.addItem(Messages.getString("MechSelectorDialog.Search.Armor50"));
        cArmor.addItem(Messages.getString("MechSelectorDialog.Search.Armor75"));
        cArmor.addItem(Messages.getString("MechSelectorDialog.Search.Armor90"));

        loadYesNo(cOfficial);
        loadYesNo(cCanon);
        loadYesNo(cClanEngine);
        loadYesNo(cInvalid);
        loadYesNo(cFailedToLoadEquipment);
        loadYesNo(cPatchwork);

        loadTriStateItem(EquipmentType.getArmorNames(), listArmorType, 5);
        loadTriStateItem(Mech.getCockpitDescrtiption(), listCockpitType, 7);
        loadTriStateItem(EquipmentType.getStructureNames(), listInternalsType, 7);
        loadTriStateItem(Engine.getEngineTypes(), listEngineType, 5);
        loadTriStateItem(Entity.getGyroTypes(), listGyroType, 7);
        loadTriStateItem(SimpleTechLevel.getDescriptions(), listTechLevel, 5);
        loadTriStateItem(Entity.getTechBaseDescriptions(), listTechBase, 4);

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
        JPanel yearPanel = new JPanel();
        yearPanel.add(lblYear);
        yearPanel.add(tStartYear);
        yearPanel.add(new JLabel("-"));
        yearPanel.add(tEndYear);
        basePanel.add(yearPanel, c);
        c.gridx = 1;
        JPanel p1bPanel = new JPanel();
        p1bPanel.add(lblInvalid);
        p1bPanel.add(cInvalid);
        p1bPanel.add(lblFailedToLoadEquipment);
        p1bPanel.add(cFailedToLoadEquipment);
        basePanel.add(p1bPanel, c);

        c.gridx = 0; c.gridy++;
        JPanel bvPanel = new JPanel();
        bvPanel.add(lblBV);
        bvPanel.add(tStartBV);
        bvPanel.add(new JLabel("-"));
        bvPanel.add(tEndBV);
        basePanel.add(bvPanel, c);
        c.gridx = 1;
        JPanel tonsPanel = new JPanel();
        tonsPanel.add(lblTons);
        tonsPanel.add(tStartTons);
        tonsPanel.add(new JLabel("-"));
        tonsPanel.add(tEndTons);
        basePanel.add(tonsPanel, c);

        c.gridx = 0; c.gridy++;
        JPanel walkPanel = new JPanel();
        walkPanel.add(lblWalk);
        walkPanel.add(tStartWalk);
        walkPanel.add(new JLabel("-"));
        walkPanel.add(tEndWalk);
        basePanel.add(walkPanel, c);
        c.gridx = 1;
        JPanel jumpPanel = new JPanel();
        jumpPanel.add(lblJump);
        jumpPanel.add(tStartJump);
        jumpPanel.add(new JLabel("-"));
        jumpPanel.add(tEndJump);
        basePanel.add(jumpPanel, c);

        c.gridx = 0; c.gridy++;
        JPanel lowerArmsPanel = new JPanel();
        lowerArmsPanel.add(lblLowerArms);
        lowerArmsPanel.add(tStartLowerArms);
        lowerArmsPanel.add(new JLabel("-"));
        lowerArmsPanel.add(tEndLowerArms);
        basePanel.add(lowerArmsPanel, c);
        c.gridx = 1;
        JPanel handsPanel = new JPanel();
        handsPanel.add(lblHands);
        handsPanel.add(tStartHands);
        handsPanel.add(new JLabel("-"));
        handsPanel.add(tEndHands);
        basePanel.add(handsPanel, c);

        c.gridx = 0; c.gridy++;
        JPanel p2Panel = new JPanel();
        p2Panel.add(lblTankTurrets);
        p2Panel.add(tStartTankTurrets);
        p2Panel.add(new JLabel("-"));
        p2Panel.add(tEndTankTurrets);
        basePanel.add(p2Panel, c);
        c.gridx = 1;
        JPanel armorPanel = new JPanel();
        armorPanel.add(lblArmor);
        armorPanel.add(cArmor);
        basePanel.add(armorPanel, c);

        c.gridx = 0; c.gridy++;;
        c.gridwidth = 1;
        JPanel cockpitPanel = new JPanel(new BorderLayout());
        cockpitPanel.add(lblCockpitType, BorderLayout.NORTH);
        cockpitPanel.add(spCockpitType, BorderLayout.CENTER);
        basePanel.add(cockpitPanel, c);
        c.gridx = 1;
        JPanel sysPane = new JPanel();
        JPanel enginePanel = new JPanel(new BorderLayout());
        enginePanel.add(lblEngineType, BorderLayout.NORTH);
        enginePanel.add(spEngineType, BorderLayout.CENTER);
        JPanel clanEnginePanel = new JPanel();
        clanEnginePanel.add(lblClanEngine);
        clanEnginePanel.add(cClanEngine);
        enginePanel.add(clanEnginePanel, BorderLayout.SOUTH);
        sysPane.add(enginePanel);
        JPanel gyroPanel = new JPanel(new BorderLayout());
        gyroPanel.add(lblGyroType, BorderLayout.NORTH);
        gyroPanel.add(spGyroType, BorderLayout.CENTER);
        sysPane.add(gyroPanel);
        basePanel.add(sysPane, c);

        c.gridx = 0; c.gridy++;;
        JPanel armorTypePanel = new JPanel(new BorderLayout());
        armorTypePanel.add(lblArmorType, BorderLayout.NORTH);
        armorTypePanel.add(spArmorType, BorderLayout.CENTER);
        JPanel patchworkPanel = new JPanel();
        patchworkPanel.add(lblPatchwork);
        patchworkPanel.add(cPatchwork);
        armorTypePanel.add(patchworkPanel, BorderLayout.SOUTH);
        basePanel.add(armorTypePanel, c);
        c.gridx = 1;
        JPanel internalsPanel = new JPanel(new BorderLayout());
        internalsPanel.add(lblInternalsType, BorderLayout.NORTH);
        internalsPanel.add(spInternalsType, BorderLayout.CENTER);
        basePanel.add(internalsPanel, c);

        c.gridx = 0; c.gridy++;;
        JPanel techLevelPanel = new JPanel(new BorderLayout());
        techLevelPanel.add(lblTechLevel, BorderLayout.NORTH);
        techLevelPanel.add(spTechLevel, BorderLayout.CENTER);
        basePanel.add(techLevelPanel, c);
        c.gridx = 1;
        JPanel techBasePanel = new JPanel(new BorderLayout());
        techBasePanel.add(lblTechBase, BorderLayout.NORTH);
        techBasePanel.add(spTechBase, BorderLayout.CENTER);
        basePanel.add(techBasePanel, c);

        c.weighty = 1;
        JPanel blankPanel = new JPanel();
        c.gridx = 0; c.gridy++;;
        blankPanel.add(btnBaseClear, c);
        basePanel.add(blankPanel, c);

        return basePanel;
    }

    private JPanel createTransportsPanel() {
        btnTransportsClear.addActionListener(this);

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
        mbPanel.add(new JLabel("-"));
        mbPanel.add(tEndMechBays);
        mbPanel.add(lblMechDoors);
        mbPanel.add(tStartMechDoors);
        mbPanel.add(new JLabel("-"));
        mbPanel.add(tEndMechDoors);
        mbPanel.add(lblMechUnits);
        mbPanel.add(tStartMechUnits);
        mbPanel.add(new JLabel("-"));
        mbPanel.add(tEndMechUnits);
        transportsPanel.add(mbPanel, c);
        c.insets = new Insets(5, 10, 0, 0);
        c.gridx = 0; c.gridy++;
        JPanel abPanel = new JPanel();
        abPanel.add(lblASFBays);
        abPanel.add(tStartASFBays);
        abPanel.add(new JLabel("-"));
        abPanel.add(tEndASFBays);
        abPanel.add(lblASFDoors);
        abPanel.add(tStartASFDoors);
        abPanel.add(new JLabel("-"));
        abPanel.add(tEndASFDoors);
        abPanel.add(lblASFUnits);
        abPanel.add(tStartASFUnits);
        abPanel.add(new JLabel("-"));
        abPanel.add(tEndASFUnits);
        transportsPanel.add(abPanel, c);
        c.gridx = 0; c.gridy++;
        JPanel scbPanel = new JPanel();
        scbPanel.add(lblSmallCraftBays);
        scbPanel.add(tStartSmallCraftBays);
        scbPanel.add(new JLabel("-"));
        scbPanel.add(tEndSmallCraftBays);
        scbPanel.add(lblSmallCraftDoors);
        scbPanel.add(tStartSmallCraftDoors);
        scbPanel.add(new JLabel("-"));
        scbPanel.add(tEndSmallCraftDoors);
        scbPanel.add(lblSmallCraftUnits);
        scbPanel.add(tStartSmallCraftUnits);
        scbPanel.add(new JLabel("-"));
        scbPanel.add(tEndSmallCraftUnits);
        transportsPanel.add(scbPanel, c);
        c.gridx = 0; c.gridy++;
        JPanel dPanel = new JPanel();
        dPanel.add(lblDropshuttleBays);
        dPanel.add(tStartDropshuttleBays);
        dPanel.add(new JLabel("-"));
        dPanel.add(tEndDropshuttleBays);
        dPanel.add(lblDropshuttleDoors);
        dPanel.add(tStartDropshuttleDoors);
        dPanel.add(new JLabel("-"));
        dPanel.add(tEndDropshuttleDoors);
        dPanel.add(lblDropshuttleUnits);
        dPanel.add(tStartDropshuttleUnits);
        dPanel.add(new JLabel("-"));
        dPanel.add(tEndDropshuttleUnits);
        transportsPanel.add(dPanel, c);
        c.gridx = 0; c.gridy++;
        JPanel lvPanel = new JPanel();
        lvPanel.add(lblLightVehicleBays);
        lvPanel.add(tStartLightVehicleBays);
        lvPanel.add(new JLabel("-"));
        lvPanel.add(tEndLightVehicleBays);
        lvPanel.add(lblLightVehicleDoors);
        lvPanel.add(tStartLightVehicleDoors);
        lvPanel.add(new JLabel("-"));
        lvPanel.add(tEndLightVehicleDoors);
        lvPanel.add(lblLightVehicleUnits);
        lvPanel.add(tStartLightVehicleUnits);
        lvPanel.add(new JLabel("-"));
        lvPanel.add(tEndLightVehicleUnits);
        transportsPanel.add(lvPanel, c);
        c.gridx = 0; c.gridy++;
        JPanel hvPanel = new JPanel();
        hvPanel.add(lblHeavyVehicleBays);
        hvPanel.add(tStartHeavyVehicleBays);
        hvPanel.add(new JLabel("-"));
        hvPanel.add(tEndHeavyVehicleBays);
        hvPanel.add(lblHeavyVehicleDoors);
        hvPanel.add(tStartHeavyVehicleDoors);
        hvPanel.add(new JLabel("-"));
        hvPanel.add(tEndHeavyVehicleDoors);
        hvPanel.add(lblHeavyVehicleUnits);
        hvPanel.add(tStartHeavyVehicleUnits);
        hvPanel.add(new JLabel("-"));
        hvPanel.add(tEndHeavyVehicleUnits);
        transportsPanel.add(hvPanel, c);
        c.gridx = 0; c.gridy++;
        JPanel shvPanel = new JPanel();
        shvPanel.add(lblSuperHeavyVehicleBays);
        shvPanel.add(tStartSuperHeavyVehicleBays);
        shvPanel.add(new JLabel("-"));
        shvPanel.add(tEndSuperHeavyVehicleBays);
        shvPanel.add(lblSuperHeavyVehicleDoors);
        shvPanel.add(tStartSuperHeavyVehicleDoors);
        shvPanel.add(new JLabel("-"));
        shvPanel.add(tEndSuperHeavyVehicleDoors);
        shvPanel.add(lblSuperHeavyVehicleUnits);
        shvPanel.add(tStartSuperHeavyVehicleUnits);
        shvPanel.add(new JLabel("-"));
        shvPanel.add(tEndSuperHeavyVehicleUnits);
        transportsPanel.add(shvPanel, c);
        c.gridx = 0; c.gridy++;
        JPanel pmPanel = new JPanel();
        pmPanel.add(lblProtomechBays);
        pmPanel.add(tStartProtomechBays);
        pmPanel.add(new JLabel("-"));
        pmPanel.add(tEndProtomechBays);
        pmPanel.add(lblProtomechDoors);
        pmPanel.add(tStartProtomechDoors);
        pmPanel.add(new JLabel("-"));
        pmPanel.add(tEndProtomechDoors);
        pmPanel.add(lblProtomechUnits);
        pmPanel.add(tStartProtomechUnits);
        pmPanel.add(new JLabel("-"));
        pmPanel.add(tEndProtomechUnits);
        transportsPanel.add(pmPanel, c);
        c.gridx = 0; c.gridy++;
        JPanel baPanel = new JPanel();
        baPanel.add(lblBattleArmorBays);
        baPanel.add(tStartBattleArmorBays);
        baPanel.add(new JLabel("-"));
        baPanel.add(tEndBattleArmorBays);
        baPanel.add(lblBattleArmorDoors);
        baPanel.add(tStartBattleArmorDoors);
        baPanel.add(new JLabel("-"));
        baPanel.add(tEndBattleArmorDoors);
        baPanel.add(lblBattleArmorUnits);
        baPanel.add(tStartBattleArmorUnits);
        baPanel.add(new JLabel("-"));
        baPanel.add(tEndBattleArmorUnits);
        transportsPanel.add(baPanel, c);
        c.gridx = 0; c.gridy++;
        JPanel iPanel = new JPanel();
        iPanel.add(lblInfantryBays);
        iPanel.add(tStartInfantryBays);
        iPanel.add(new JLabel("-"));
        iPanel.add(tEndInfantryBays);
        iPanel.add(lblInfantryDoors);
        iPanel.add(tStartInfantryDoors);
        iPanel.add(new JLabel("-"));
        iPanel.add(tEndInfantryDoors);
        iPanel.add(lblInfantryUnits);
        iPanel.add(tStartInfantryUnits);
        iPanel.add(new JLabel("-"));
        iPanel.add(tEndInfantryUnits);
        transportsPanel.add(iPanel, c);
        c.gridx = 0; c.gridy++;
        JPanel dcPanel = new JPanel();
        dcPanel.add(lblDockingCollars);
        dcPanel.add(tStartDockingCollars);
        dcPanel.add(new JLabel("-"));
        dcPanel.add(tEndDockingCollars);
        transportsPanel.add(dcPanel, c);
        c.gridx = 0; c.gridy++;
        JPanel tsPanel = new JPanel();
        tsPanel.add(lblTroopSpace);
        tsPanel.add(tStartTroopSpace);
        tsPanel.add(new JLabel("-"));
        tsPanel.add(tEndTroopSpace);
        transportsPanel.add(tsPanel, c);
        c.gridx = 0; c.gridy++;
        JPanel cbPanel = new JPanel();
        cbPanel.add(lblCargoBayUnits);
        cbPanel.add(tStartCargoBayUnits);
        cbPanel.add(new JLabel("-"));
        cbPanel.add(tEndCargoBayUnits);
        transportsPanel.add(cbPanel, c);
        c.gridx = 0; c.gridy++;
        JPanel nrfPanel = new JPanel();
        nrfPanel.add(lblNavalRepairFacilities);
        nrfPanel.add(tStartNavalRepairFacilities);
        nrfPanel.add(new JLabel("-"));
        nrfPanel.add(tEndNavalRepairFacilities);
        transportsPanel.add(nrfPanel, c);
        c.gridx = 0; c.gridy++;
        JPanel bahPanel = new JPanel();
        bahPanel.add(lblBattleArmorHandles);
        bahPanel.add(tStartBattleArmorHandles);
        bahPanel.add(new JLabel("-"));
        bahPanel.add(tEndBattleArmorHandles);
        transportsPanel.add(bahPanel, c);

        c.weighty = 1;
        JPanel blankPanel = new JPanel();
        c.gridx = 0; c.gridy++;;
        blankPanel.add(btnTransportsClear, c);
        transportsPanel.add(blankPanel, c);

        return transportsPanel;
    }

    private void loadTriStateItem(AbstractOptions s, JList l, int count) {
        List<String> qs = new ArrayList<>();
        for (final Enumeration<IOptionGroup> optionGroups = s.getGroups(); optionGroups.hasMoreElements(); ) {
            final IOptionGroup group = optionGroups.nextElement();
            for (final Enumeration<IOption> options = group.getOptions(); options.hasMoreElements(); ) {
                final IOption option = options.nextElement();
                if (option != null) {
                    qs.add(option.getDisplayableNameWithValue());
                }
            }
        }
        qs = qs.stream().sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());

        DefaultListModel dlm  = new DefaultListModel();

        for (String q : qs) {
            dlm.addElement(new TriStateItem("\u2610", q));
        }
        l.setModel(dlm);

        l.setVisibleRowCount(count);
        l.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        l.setSelectionModel(new NoSelectionModel());
        l.addMouseListener(new MouseAdapter() {
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
    }

    private void loadAndOr(JComboBox<String> cb, int index) {
        cb.addItem(Messages.getString("MechSelectorDialog.Search.and"));
        cb.addItem(Messages.getString("MechSelectorDialog.Search.or"));
        cb.setSelectedIndex(index);
    }

    private JPanel createQuirkPanel() {
        btnQuirksClear.addActionListener(this);

        loadAndOr(cQuirkInclue, 0);
        loadAndOr(cQuirkExclude, 1);
        loadTriStateItem(new Quirks(), listQuirkType, 25);

        loadAndOr(cWeaponQuirkInclue, 0);
        loadAndOr(cWeaponQuirkExclude, 1);
        cWeaponQuirkExclude.setSelectedIndex(1);

        loadTriStateItem(new WeaponQuirks(), listWeaponQuirkType, 17);

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
        c.gridx = 0; c.gridy++;;
        blankPanel.add(btnQuirksClear, c);
        quirksPanel.add(blankPanel, c);

        return quirksPanel;
    }

    private JPanel createUnitTypePanel() {
        btnUnitTypeClear.addActionListener(this);

        Border emptyBorder = BorderFactory.createEmptyBorder(1, 1, 1, 1);
        btnFilterMech.setBorder(emptyBorder);
        btnFilterMech.addActionListener(this);
        btnFilterBipedMech.setBorder(emptyBorder);
        btnFilterBipedMech.addActionListener(this);
        btnFilterProtoMech.setBorder(emptyBorder);
        btnFilterProtoMech.addActionListener(this);
        btnFilterLAM.setBorder(emptyBorder);
        btnFilterLAM.addActionListener(this);
        btnFilterTripod.setBorder(emptyBorder);
        btnFilterTripod.addActionListener(this);
        btnFilterQuad.setBorder(emptyBorder);
        btnFilterQuad.addActionListener(this);
        btnFilterQuadVee.setBorder(emptyBorder);
        btnFilterQuadVee.addActionListener(this);
        btnFilterAero.setBorder(emptyBorder);
        btnFilterAero.addActionListener(this);
        btnFilterFixedWingSupport.setBorder(emptyBorder);
        btnFilterFixedWingSupport.addActionListener(this);
        btnFilterConvFighter.setBorder(emptyBorder);
        btnFilterConvFighter.addActionListener(this);
        btnFilterSmallCraft.setBorder(emptyBorder);
        btnFilterSmallCraft.addActionListener(this);
        btnFilterDropship.setBorder(emptyBorder);
        btnFilterDropship.addActionListener(this);
        btnFilterJumpship.setBorder(emptyBorder);
        btnFilterJumpship.addActionListener(this);
        btnFilterWarship.setBorder(emptyBorder);
        btnFilterWarship.addActionListener(this);
        btnFilterSpaceStation.setBorder(emptyBorder);
        btnFilterSpaceStation.addActionListener(this);
        btnFilterInfantry.setBorder(emptyBorder);
        btnFilterInfantry.addActionListener(this);
        btnFilterBattleArmor.setBorder(emptyBorder);
        btnFilterBattleArmor.addActionListener(this);
        btnFilterTank.setBorder(emptyBorder);
        btnFilterTank.addActionListener(this);
        btnFilterVTOL.setBorder(emptyBorder);
        btnFilterVTOL.addActionListener(this);
        btnFilterSupportVTOL.setBorder(emptyBorder);;
        btnFilterSupportVTOL.addActionListener(this);
        btnFilterGunEmplacement.setBorder(emptyBorder);
        btnFilterGunEmplacement.addActionListener(this);
        btnFilterSupportTank.setBorder(emptyBorder);
        btnFilterSupportTank.addActionListener(this);
        btnFilterLargeSupportTank.setBorder(emptyBorder);;
        btnFilterLargeSupportTank.addActionListener(this);
        btnFilterSuperHeavyTank.setBorder(emptyBorder);
        btnFilterSuperHeavyTank.addActionListener(this);
        btnFilterOmni.setBorder(emptyBorder);
        btnFilterOmni.addActionListener(this);
        btnFilterMilitary.setBorder(emptyBorder);
        btnFilterMilitary.addActionListener(this);
        btnFilterIndustrial.setBorder(emptyBorder);
        btnFilterIndustrial.addActionListener(this);
        btnFilterMountedInfantry.setBorder(emptyBorder);
        btnFilterMountedInfantry.addActionListener(this);
        btnFilterWaterOnly.setBorder(emptyBorder);
        btnFilterWaterOnly.addActionListener(this);
        btnFilterSupportVehicle.setBorder(emptyBorder);
        btnFilterSupportVehicle.addActionListener(this);
        btnFilterAerospaceFighter.setBorder(emptyBorder);
        btnFilterAerospaceFighter.addActionListener(this);
        btnFilterDoomedOnGround.setBorder(emptyBorder);
        btnFilterDoomedOnGround.addActionListener(this);
        btnFilterDoomedInAtmosphere.setBorder(emptyBorder);
        btnFilterDoomedInAtmosphere.addActionListener(this);
        btnFilterDoomedInSpace.setBorder(emptyBorder);
        btnFilterDoomedInSpace.addActionListener(this);
        btnFilterDoomedInExtremeTemp.setBorder(emptyBorder);
        btnFilterDoomedInExtremeTemp.addActionListener(this);
        btnFilterDoomedInVacuum.setBorder(emptyBorder);
        btnFilterDoomedInVacuum.addActionListener(this);

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

        c.gridx = 0;
        c.gridy++;
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

        c.gridx = 0;
        c.gridy++;
        JPanel filterAeroPanel = new JPanel();
        filterAeroPanel.add(btnFilterAero);
        filterAeroPanel.add(lblFilterAero);
        unitTypePanel.add(filterAeroPanel, c);
        c.gridx = 1;
        JPanel filterAerospaceFighterPanel = new JPanel();
        filterAerospaceFighterPanel.add(btnFilterAerospaceFighter);
        filterAerospaceFighterPanel.add(lblFilterAerospaceFighter);
        unitTypePanel.add(filterAerospaceFighterPanel, c);
        c.gridx = 2;
        JPanel filterConvFighterPanel = new JPanel();
        filterConvFighterPanel.add(btnFilterConvFighter);
        filterConvFighterPanel.add(lblFilterConvFighter);
        unitTypePanel.add(filterConvFighterPanel, c);
        c.gridx = 3;
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

        c.gridx = 0;
        c.gridy++;
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

        c.gridy++;
        c.gridx = 0;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth  = 4;
        JPanel dotSep = new JPanel();
        dotSep.setLayout(new BoxLayout(dotSep, BoxLayout.PAGE_AXIS));
        dotSep.add(new ASAdvancedSearchPanel.DottedSeparator());
        unitTypePanel.add(dotSep, c);

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
        unitTypePanel.add(filter1Panel, c);

        c.gridx = 0; c.gridy++;
        JPanel filter2Panel = new JPanel();
        filter2Panel.add(btnFilterWaterOnly);
        filter2Panel.add(lblFilterWaterOnly);
        filter2Panel.add(btnFilterDoomedInExtremeTemp);
        filter2Panel.add(lblFilterDoomedInExtremeTemp);
        filter2Panel.add(btnFilterDoomedInVacuum);
        filter2Panel.add(lblFilterDoomedInVacuum);
        unitTypePanel.add(filter2Panel, c);


        c.gridx = 0; c.gridy++;
        JPanel filter3Panel = new JPanel();
        filter3Panel.add(btnFilterDoomedOnGround);
        filter3Panel.add(lblFilterDoomedOnGround);
        filter3Panel.add(btnFilterDoomedInAtmosphere);
        filter3Panel.add(lblFilterDoomedInAtmosphere);
        filter3Panel.add(btnFilterDoomedInSpace);
        filter3Panel.add(lblFilterDoomedInSpace);
        unitTypePanel.add(filter3Panel, c);

        c.gridx = 0; c.gridy++;;
        c.weighty = 1;
        JPanel blankPanel = new JPanel();
        blankPanel.add(btnUnitTypeClear);
        unitTypePanel.add(blankPanel, c);


        return unitTypePanel;
    }

    private JPanel createWeaponEqPanel() {
        // Initialize Items
        btnWEAnd.addActionListener(this);
        btnWEAdd.addActionListener(this);
        btnWELeftParen.addActionListener(this);
        btnWERightParen.addActionListener(this);
        btnWEOr.addActionListener(this);
        btnWEClear.addActionListener(this);
        btnWEBack.addActionListener(this);

        btnWEBack.setEnabled(false);
        btnWEAdd.setEnabled(false);

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

        txtWEEqExp.setEditable(false);
        txtWEEqExp.setLineWrap(true);
        txtWEEqExp.setWrapStyleWord(true);

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
        cboPanel.add(lblTechLevelBase, c);
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
        btnPanel.add(btnWEAdd, c);
        btnPanel.add(btnWELeftParen, c);
        btnPanel.add(btnWERightParen, c);
        btnPanel.add(btnWEAnd, c);
        btnPanel.add(btnWEOr, c);
        btnPanel.add(btnWEBack, c);
        btnPanel.add(btnWEClear, c);
        weaponEqPanel.add(btnPanel, c);

        // Filter Expression
        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 0; c.gridy++;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        weaponEqPanel.add(lblWEEqExpTxt, c);
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridwidth = 3;
        weaponEqPanel.add(expWEScroller, c);
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
                btnWEAdd.setEnabled(true);
            } else if (tblWeapons.getSelectedRow() >= 0) {
                tblEquipment.clearSelection();
                tblWeaponType.clearSelection();
            }
        } else if (evt.getSource().equals(tblEquipment.getSelectionModel())) {
            if ((tblEquipment.getSelectedRow() >= 0) && lastTokIsOperation) {
                tblWeapons.clearSelection();
                tblWeaponType.clearSelection();
                btnWEAdd.setEnabled(true);
            } else if (tblEquipment.getSelectedRow() >= 0) {
                tblWeapons.clearSelection();
                tblWeaponType.clearSelection();
            }
        } else if (evt.getSource().equals(tblWeaponType.getSelectionModel())) {
            if ((tblWeaponType.getSelectedRow() >= 0) && lastTokIsOperation) {
                tblWeapons.clearSelection();
                tblEquipment.clearSelection();
                btnWEAdd.setEnabled(true);
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
            btnWEAdd.setEnabled(true);
        }
        btnWELeftParen.setEnabled(true);
    }

    /**
     * Convenience method for disabling the buttons related to weapon/equipment
     * selection for filtering (btnAddEquipment, btnAddWeapon, etc)
     */
    private void disableSelectionButtons() {
        btnWEAdd.setEnabled(false);
        btnWELeftParen.setEnabled(false);
    }

    /**
     * Convenience method for enabling the buttons related to filter operations
     * for filtering (btnAnd, btnOr, etc)
     */
    private void enableOperationButtons() {
        btnWEOr.setEnabled(true);
        btnWEAnd.setEnabled(true);
        btnWERightParen.setEnabled(true);
    }

    /**
     * Convenience method for disabling the buttons related to filter operations
     * for filtering (btnAnd, btnOr, etc)
     */
    private void disableOperationButtons() {
        btnWEOr.setEnabled(false);
        btnWEAnd.setEnabled(false);
        btnWERightParen.setEnabled(false);
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
        } else if (ev.getSource().equals(btnWEAdd)) {
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
                txtWEEqExp.setText(filterExpressionString());
                btnWEBack.setEnabled(true);
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
                txtWEEqExp.setText(filterExpressionString());
                btnWEBack.setEnabled(true);
                enableOperationButtons();
                disableSelectionButtons();
            }
            row = tblWeaponType.getSelectedRow();
            if (row >= 0) {
                int qty = Integer.parseInt((String)tblWeaponType.getValueAt(row, WeaponClassTableModel.COL_QTY));
                filterToks.add(new WeaponClassFT((WeaponClass)tblWeaponType.getModel().getValueAt(tblWeaponType.convertRowIndexToModel(row), WeaponClassTableModel.COL_VAL), qty));
                txtWEEqExp.setText(filterExpressionString());
                btnWEBack.setEnabled(true);
                enableOperationButtons();
                disableSelectionButtons();
            }
        } else if (ev.getSource().equals(btnWELeftParen)) {
            filterToks.add(new ParensFT("("));
            txtWEEqExp.setText(filterExpressionString());
            btnWEBack.setEnabled(true);
            disableOperationButtons();
            enableSelectionButtons();
            btnWELeftParen.setEnabled(false);
            btnWERightParen.setEnabled(false);
        } else if (ev.getSource().equals(btnWERightParen)) {
            filterToks.add(new ParensFT(")"));
            txtWEEqExp.setText(filterExpressionString());
            btnWEBack.setEnabled(true);
            enableOperationButtons();
            disableSelectionButtons();
            btnWELeftParen.setEnabled(false);
            btnWERightParen.setEnabled(false);
        } else if (ev.getSource().equals(btnWEAnd)) {
            filterToks.add(new OperationFT(MechSearchFilter.BoolOp.AND));
            txtWEEqExp.setText(filterExpressionString());
            btnWEBack.setEnabled(true);
            disableOperationButtons();
            enableSelectionButtons();
        } else if (ev.getSource().equals(btnWEOr)) {
            filterToks.add(new OperationFT(MechSearchFilter.BoolOp.OR));
            txtWEEqExp.setText(filterExpressionString());
            btnWEBack.setEnabled(true);
            disableOperationButtons();
            enableSelectionButtons();
        } else if (ev.getSource().equals(btnWEBack)) {
            if (!filterToks.isEmpty()) {
                filterToks.remove(filterToks.size() - 1);
                txtWEEqExp.setText(filterExpressionString());
                if (filterToks.isEmpty()) {
                    btnWEBack.setEnabled(false);
                }

                if ((filterToks.isEmpty()) || (filterToks.lastElement() instanceof OperationFT)) {
                    disableOperationButtons();
                    enableSelectionButtons();
                } else {
                    enableOperationButtons();
                    disableSelectionButtons();
                }
            }
        } else if (ev.getSource().equals(btnWEClear)) {
            clearWeaponsEquipment();
        } else if (ev.getSource().equals(btnBaseClear)) {
            clearBase();
        } else if (ev.getSource().equals(btnUnitTypeClear)) {
            clearUnitType();
        } else if (ev.getSource().equals(btnTransportsClear)) {
            clearTransports();
        } else if (ev.getSource().equals(btnQuirksClear)) {
            clearQuirks();
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
        } else if (ev.getSource().equals(btnFilterOmni)) {
            toggleText(btnFilterOmni);
        } else if (ev.getSource().equals(btnFilterMilitary)) {
            toggleText(btnFilterMilitary);
        } else if (ev.getSource().equals(btnFilterIndustrial)) {
            toggleText(btnFilterIndustrial);
        } else if (ev.getSource().equals(btnFilterMountedInfantry)) {
            toggleText(btnFilterMountedInfantry);
        } else if (ev.getSource().equals(btnFilterWaterOnly)) {
            toggleText(btnFilterWaterOnly);
        } else if (ev.getSource().equals(btnFilterSupportVehicle)) {
            toggleText(btnFilterSupportVehicle);
        } else if (ev.getSource().equals(btnFilterAerospaceFighter)) {
            toggleText(btnFilterAerospaceFighter);
        } else if (ev.getSource().equals(btnFilterDoomedOnGround)) {
            toggleText(btnFilterDoomedOnGround);
        } else if (ev.getSource().equals(btnFilterDoomedInAtmosphere)) {
            toggleText(btnFilterDoomedInAtmosphere);
        } else if (ev.getSource().equals(btnFilterDoomedInSpace)) {
            toggleText(btnFilterDoomedInSpace);
        } else if (ev.getSource().equals(btnFilterDoomedInExtremeTemp)) {
            toggleText(btnFilterDoomedInExtremeTemp);
        } else if (ev.getSource().equals(btnFilterDoomedInVacuum)) {
            toggleText(btnFilterDoomedInVacuum);
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
        ListModel<TriStateItem> m = list.getModel();

        for (int i = 0; i < m.getSize(); i++) {
            TriStateItem ms = m.getElementAt(i);

            if (index == i) {
                if (ms.state.contains("\u2610")) {
                    ms.state = "\u2611";
                } else if (ms.state.contains("\u2611")) {
                    ms.state = "\u2612";
                } else if (ms.state.contains("\u2612")) {
                    ms.state = "\u2610";
                }
            }
        }

        list.setModel(m);
        list.repaint();
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
        txtWEEqExp.setText(mechFilter.getEquipmentExpression());
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

    private void clearTriStateItem(JList l) {
        ListModel<TriStateItem> m = l.getModel();

        for (int i = 0; i < m.getSize(); i++) {
            TriStateItem ms = m.getElementAt(i);
            ms.state = "\u2610";
        }

        l.setModel(m);
        l.repaint();
    }

    private void clearBase() {
        tStartWalk.setText("");
        tEndWalk.setText("");
        tStartJump.setText("");
        tEndJump.setText("");
        cArmor.setSelectedIndex(0);
        cOfficial.setSelectedIndex(0);
        cCanon.setSelectedIndex(0);
        cPatchwork.setSelectedIndex(0);
        cInvalid.setSelectedIndex(0);
        cFailedToLoadEquipment.setSelectedIndex(0);
        cClanEngine.setSelectedIndex(0);
        tStartTankTurrets.setText("");
        tEndTankTurrets.setText("");
        tStartLowerArms.setText("");
        tEndLowerArms.setText("");
        tStartHands.setText("");
        tEndHands.setText("");
        tStartYear.setText("");
        tEndYear.setText("");
        tStartTons.setText("");
        tEndTons.setText("");
        tStartBV.setText("");
        tEndBV.setText("");
        tSource.setText("");

        clearTriStateItem(listArmorType);
        clearTriStateItem(listCockpitType);
        clearTriStateItem(listEngineType);
        clearTriStateItem(listGyroType);
        clearTriStateItem(listInternalsType);
        clearTriStateItem(listTechLevel);
        clearTriStateItem(listTechBase);
    }

    private void clearTransports() {
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
    }

    private void clearUnitType() {
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

    private void clearQuirks() {
        cQuirkInclue.setSelectedIndex(0);
        cQuirkExclude.setSelectedIndex(1);
        clearTriStateItem(listQuirkType);

        cWeaponQuirkInclue.setSelectedIndex(0);
        cWeaponQuirkExclude.setSelectedIndex(1);
        clearTriStateItem(listWeaponQuirkType);
    }

    private void clearWeaponsEquipment() {
        filterToks.clear();
        tblWeapons.clearSelection();
        tblEquipment.clearSelection();
        txtWEEqExp.setText("");
        btnWEBack.setEnabled(false);
        disableOperationButtons();
        enableSelectionButtons();
    }

    /**
     *  Clear the filter.
     */
    public void clearValues() {
        mechFilter = null;

        clearUnitType();
        clearBase();
        clearTransports();
        clearQuirks();
        clearWeaponsEquipment();
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

    private void updateTriStateItemInteger(List<Integer> include, List<Integer> exclude,  JList l) {
        ListModel<TriStateItem> m = l.getModel();

        for (int i = 0; i < m.getSize(); i++) {
            TriStateItem ms = m.getElementAt(i);
            if (ms.state.contains("\u2611")) {
                include.add(i);
            } else if (ms.state.contains("\u2612")) {
                exclude.add(i);
            }
        }
    }

    private void updateTriStateItemString(List<String> include, List<String> exclude, JList l) {
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

    private void updateTriStateItemString(Map<Integer, String> include, Map<Integer, String> exclude, JList l) {
        ListModel<TriStateItem> m = l.getModel();

        for (int i = 0; i < m.getSize(); i++) {
            TriStateItem ms = m.getElementAt(i);
            if (ms.state.contains("\u2611")) {
                include.put(ms.code, ms.text);
            } else if (ms.state.contains("\u2612")) {
                exclude.put(ms.code, ms.text);
            }
        }
    }

    private void updateBase() {
        mechFilter.sStartWalk = tStartWalk.getText();
        mechFilter.sEndWalk = tEndWalk.getText();

        mechFilter.sStartJump = tStartJump.getText();
        mechFilter.sEndJump = tEndJump.getText();

        mechFilter.iArmor = cArmor.getSelectedIndex();

        mechFilter.sStartTankTurrets = tStartTankTurrets.getText();
        mechFilter.sEndTankTurrets = tEndTankTurrets.getText();
        mechFilter.sStartLowerArms = tStartLowerArms.getText();
        mechFilter.sEndLowerArms = tEndLowerArms.getText();
        mechFilter.sStartHands = tStartHands.getText();
        mechFilter.sEndHands = tEndHands.getText();

        mechFilter.iOfficial = cOfficial.getSelectedIndex();
        mechFilter.iCanon = cCanon.getSelectedIndex();
        mechFilter.iInvalid = cInvalid.getSelectedIndex();
        mechFilter.iFailedToLoadEquipment = cFailedToLoadEquipment.getSelectedIndex();
        mechFilter.iClanEngine = cClanEngine.getSelectedIndex();
        mechFilter.iPatchwork = cPatchwork.getSelectedIndex();

        mechFilter.source = tSource.getText();

        mechFilter.sStartYear = tStartYear.getText();
        mechFilter.sEndYear = tEndYear.getText();

        mechFilter.sStartTons = tStartTons.getText();
        mechFilter.sEndTons = tEndTons.getText();

        mechFilter.sStartBV = tStartBV.getText();
        mechFilter.sEndBV = tEndBV.getText();

        updateTriStateItemInteger(mechFilter.armorType, mechFilter.armorTypeExclude, listArmorType);
        updateTriStateItemInteger(mechFilter.cockpitType, mechFilter.cockpitTypeExclude, listCockpitType);
        updateTriStateItemInteger(mechFilter.internalsType, mechFilter.internalsTypeExclude, listInternalsType);
        updateTriStateItemString(mechFilter.engineType, mechFilter.engineTypeExclude, listEngineType);
        updateTriStateItemString(mechFilter.gyroType, mechFilter.gyroTypeExclude, listGyroType);
        updateTriStateItemString(mechFilter.techLevel, mechFilter.techLevelExclude, listTechLevel);
        updateTriStateItemString(mechFilter.techBase, mechFilter.techBaseExclude, listTechBase);
    }

    private void updateTransports() {
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
    }

    private void updateQuirks() {
        mechFilter.quirkInclude = cQuirkInclue.getSelectedIndex();
        mechFilter.quirkExclude = cQuirkExclude.getSelectedIndex();

        updateTriStateItemString(mechFilter.quirkType, mechFilter.quirkTypeExclude, listQuirkType);

        mechFilter.weaponQuirkInclude = cWeaponQuirkInclue.getSelectedIndex();
        mechFilter.weaponQuirkExclude = cWeaponQuirkExclude.getSelectedIndex();

        updateTriStateItemString(mechFilter.weaponQuirkType, mechFilter.weaponQuirkTypeExclude, listWeaponQuirkType);
    }

    private void updateUnitTypes() {
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
        mechFilter.iOmni = getValue(btnFilterOmni);
        mechFilter.iMilitary = getValue(btnFilterMilitary);
        mechFilter.iIndustrial = getValue(btnFilterIndustrial);
        mechFilter.iMountedInfantry = getValue(btnFilterMountedInfantry);
        mechFilter.iWaterOnly = getValue(btnFilterWaterOnly);
        mechFilter.iSupportVehicle = getValue(btnFilterSupportVehicle);
        mechFilter.iAerospaceFighter = getValue(btnFilterAerospaceFighter);
        mechFilter.iDoomedOnGround = getValue(btnFilterDoomedOnGround);
        mechFilter.iDoomedInAtmosphere = getValue(btnFilterDoomedInAtmosphere);
        mechFilter.iDoomedInSpace = getValue(btnFilterDoomedInSpace);
        mechFilter.iDoomedInExtremeTemp = getValue(btnFilterDoomedInExtremeTemp);
        mechFilter.iDoomedInVacuum = getValue(btnFilterDoomedInVacuum);
    }

    /**
     * Update the search fields that aren't automatically updated.
     */
    protected void updateMechSearchFilter() {
        mechFilter.isDisabled = false;

        updateBase();
        updateTransports();
        updateQuirks();
        updateUnitTypes();
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
