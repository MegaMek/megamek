/*
 * MechSelectorDialog.java - Copyright (C) 2002,2004 Josh Yockey
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

package megamek.client.ui.AWT;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.Choice;
import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Point;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import megamek.client.Client;
import megamek.client.ui.Messages;
import megamek.common.BattleArmor;
import megamek.common.EquipmentType;
import megamek.common.EntityMovementMode;
import megamek.common.Infantry;
import megamek.common.LocationFullException;
import megamek.common.TechConstants;

/*
 * Allows a user to sort through a list of MechSummaries and select one
 */
public class CustomBattleArmorDialog extends Dialog implements ActionListener,
        ItemListener, KeyListener, Runnable, TextListener, WindowListener {
    /**
     *
     */
    private static final long serialVersionUID = -5195200950623503842L;
    private Client m_client;
    private ClientGUI m_clientgui;

    private Panel m_pLeft = new Panel();

    private Panel m_pParams = new Panel();
    private Label m_labelBAName = new Label(Messages
            .getString("CustomBattleArmorDialog.m_labelBAName"), Label.RIGHT);
    private TextField m_tfBAName = new TextField();
    private Label m_labelMenPerSquad = new Label(Messages
            .getString("CustomBattleArmorDialog.m_labelMenPerSquad"),
            Label.RIGHT);
    private Choice m_chMenPerSquad = new Choice();
    private Label m_labelTechBase = new Label(Messages
            .getString("CustomBattleArmorDialog.m_labelTechBase"), Label.RIGHT);
    private Choice m_chTechBase = new Choice();
    private Label m_labelChassisType = new Label(Messages
            .getString("CustomBattleArmorDialog.m_labelChassisType"),
            Label.RIGHT);
    private Choice m_chChassisType = new Choice();
    private Label m_labelWeightClass = new Label(Messages
            .getString("CustomBattleArmorDialog.m_labelWeightClass"),
            Label.RIGHT);
    private Choice m_chWeightClass = new Choice();
    private Label m_labelGroundMP = new Label(Messages
            .getString("CustomBattleArmorDialog.m_labelGroundMP"), Label.RIGHT);
    private Choice m_chGroundMP = new Choice();
    private CheckboxGroup m_cbgJumpType = new CheckboxGroup();
    private Checkbox m_cbJumpQuery = new Checkbox(Messages
            .getString("CustomBattleArmorDialog.m_jumpQuery"), m_cbgJumpType,
            true);
    private Checkbox m_cbVTOLQuery = new Checkbox(Messages
            .getString("CustomBattleArmorDialog.m_VTOLQuery"), m_cbgJumpType,
            false);
    private Checkbox m_cbUMUQuery = new Checkbox(Messages
            .getString("CustomBattleArmorDialog.m_UMUQuery"), m_cbgJumpType,
            false);
    private Label m_labelJumpValue = new Label(Messages
            .getString("CustomBattleArmorDialog.m_labelJumpValue"), Label.RIGHT);
    private Choice m_chJumpValue = new Choice();
    private Label m_labelArmorType = new Label(Messages
            .getString("CustomBattleArmorDialog.m_labelArmorType"), Label.RIGHT);
    private Choice m_chArmorType = new Choice();
    private Label m_labelArmorValue = new Label(Messages
            .getString("CustomBattleArmorDialog.m_labelArmorValue"),
            Label.RIGHT);
    private Choice m_chArmorValue = new Choice();
    private Label m_labelLeftManipulator = new Label(Messages
            .getString("CustomBattleArmorDialog.m_labelLeftManipulator"),
            Label.RIGHT);
    private Choice m_chLeftManipulator = new Choice();
    private Label m_labelRightManipulator = new Label(Messages
            .getString("CustomBattleArmorDialog.m_labelRightManipulator"),
            Label.RIGHT);
    private Choice m_chRightManipulator = new Choice();
    private Label m_labelTorsoEquipment = new Label(Messages
            .getString("CustomBattleArmorDialog.m_labelTorsoEquipment"),
            Label.RIGHT);
    private Choice m_chTorsoEquipment = new Choice();
    private Button m_buttonAddTorso = new Button(Messages
            .getString("CustomBattleArmorDialog.m_buttonAdd"));
    private Label m_labelRightArmEquipment = new Label(Messages
            .getString("CustomBattleArmorDialog.m_labelRightArmEquipment"),
            Label.RIGHT);
    private Choice m_chRightArmEquipment = new Choice();
    private Button m_buttonAddRightArm = new Button(Messages
            .getString("CustomBattleArmorDialog.m_buttonAdd"));
    private Label m_labelLeftArmEquipment = new Label(Messages
            .getString("CustomBattleArmorDialog.m_labelLeftArmEquipment"),
            Label.RIGHT);
    private Choice m_chLeftArmEquipment = new Choice();
    private Button m_buttonAddLeftArm = new Button(Messages
            .getString("CustomBattleArmorDialog.m_buttonAdd"));
    private Label m_labelTorsoCurrentEquipment = new Label(Messages
            .getString("CustomBattleArmorDialog.m_labelCurrentTorsoEquipment"),
            Label.RIGHT);
    private Choice m_chTorsoCurrentEquipment = new Choice();
    private Button m_buttonRemoveTorso = new Button(Messages
            .getString("CustomBattleArmorDialog.m_buttonRemove"));
    private Label m_labelRightArmCurrentEquipment = new Label(
            Messages
                    .getString("CustomBattleArmorDialog.m_labelCurrentRightArmEquipment"),
            Label.RIGHT);
    private Choice m_chRightArmCurrentEquipment = new Choice();
    private Button m_buttonRemoveRightArm = new Button(Messages
            .getString("CustomBattleArmorDialog.m_buttonRemove"));
    private Label m_labelLeftArmCurrentEquipment = new Label(
            Messages
                    .getString("CustomBattleArmorDialog.m_labelCurrentLeftArmEquipment"),
            Label.RIGHT);
    private Choice m_chLeftArmCurrentEquipment = new Choice();
    private Button m_buttonRemoveLeftArm = new Button(Messages
            .getString("CustomBattleArmorDialog.m_buttonRemove"));

    private Panel m_pButtons = new Panel();
    private Button m_bPick = new Button(Messages
            .getString("CustomBattleArmorDialog.m_bPick"));
    private Button m_bPickClose = new Button(Messages
            .getString("CustomBattleArmorDialog.m_bPickClose"));
    private Button m_bCancel = new Button(Messages
            .getString("CustomBattleArmorDialog.m_bClose"));
    private Button m_bSave = new Button(Messages
            .getString("CustomBattleArmorDialog.m_bSave"));
    private Button m_buttonReset = new Button(Messages
            .getString("CustomBattleArmorDialog.m_buttonReset"));
    private Label m_labelPlayer = new Label(Messages
            .getString("CustomBattleArmorDialog.m_labelPlayer"), Label.RIGHT);
    private Choice m_chPlayer = new Choice();

    private TextArea m_BAView = new TextArea("", 18, 25,
            TextArea.SCROLLBARS_BOTH);

    private String invalidReason = null;
    private int stateMenPerSquad = 1;
    private int stateTechBase = 0;
    private int stateChassisType = 0;
    private int stateWeightClass = 0;
    private int stateArmorType = 0;
    private int stateArmorValue = 0;
    private int stateJumpType = 0;
    private int stateJumpMP = 0;
    private int stateGroundMP = 1;
    private int stateCurrentWeight = 0;
    private int stateMinWeight = 0;
    private int stateMaxWeight = 400;
    private int stateManipulatorTypeLeft = 0;
    private int stateManipulatorTypeRight = 0;
    private int stateConflictFlags = 0;
    private Vector<BattleArmorEquipment> leftArmEquipment = null;
    private Vector<BattleArmorEquipment> rightArmEquipment = null;
    private Vector<BattleArmorEquipment> torsoEquipment = null;

    static ArrayList<BattleArmorEquipment> equipmentTypes = null;
    static ArrayList<String> equipmentNames = null;

    private static final int TECH_BASE_IS = 0;
    private static final int TECH_BASE_CLAN = 1;
    private static final int TECH_BASE_BOTH = 2;

    private static final int WEIGHT_CLASS_PAL = 0;
    private static final int WEIGHT_CLASS_LIGHT = 1;
    private static final int WEIGHT_CLASS_MEDIUM = 2;
    private static final int WEIGHT_CLASS_HEAVY = 3;
    private static final int WEIGHT_CLASS_ASSAULT = 4;

    private static final int JUMP_TYPE_JUMP = 0;
    private static final int JUMP_TYPE_VTOL = 1;
    private static final int JUMP_TYPE_UMU = 2;

    public static int EQUIPMENT_TYPE_WEAPON = 0;
    public static int EQUIPMENT_TYPE_WEAPON_AP = 1;
    public static int EQUIPMENT_TYPE_PREPROCESS = 2;
    public static int EQUIPMENT_TYPE_AMMO = 3;
    public static int EQUIPMENT_TYPE_OTHER = 4;

    private static final int[][] ARMOR_TYPE_WEIGHT = {
            { 50, 40, 100, 55, 100, 60, 60, 0, 50 },
            { 25, 0, 0, 30, 0, 35, 35, 30, 0 } };

    private static final int[] ARMOR_TYPE_SLOTS = { 0, 5, 4, 3, 4, 4, 5, 5, 5 };

    private static final int[] ARMOR_TYPE_COSTS = { 10000, 12500, 10000, 12000,
            50000, 15000, 20000, 10000, 15000 };

    private static final String[] ARMOR_TYPE_STRINGS = { "Standard",
            "Advanced", "Prototype", "Basic Stealth", "Prototype Stealth",
            "Standard Stealth", "Improved Stealth", "Fire Resistant", "Mimetic" };

    private static final int[] GROUND_MP_WEIGHT = { 25, 30, 40, 80, 160 };

    private static final int[][] JUMP_MP_LIMITS = { { 3, 3, 3, 2, 2 },
            { 7, 6, 5, 0, 0 }, { 5, 5, 4, 3, 2 } };

    private static final int[][] JUMP_MP_WEIGHT = { { 25, 25, 50, 125, 250 },
            { 30, 40, 60, 0, 0 }, { 45, 45, 85, 160, 250 } };

    private static final int[][] JUMP_MP_COST = {
            { 50000, 50000, 75000, 150000, 300000 },
            { 50000, 50000, 100000, 0, 0 },
            { 50000, 50000, 75000, 100000, 150000 } };

    private static final int[] MANIPULATOR_TYPE_WEIGHT = { 0, 0, 0, 15, 15, 35,
            50, 20, 60, 30, 30, 30 };

    private static final int[] MANIPULATOR_TYPE_COSTS = { 0, 2500, 5000, 7500,
            10000, 12500, 15000, 25000, 30000, 500, 2500 };

    public static final int[] ARM_MAX_SLOTS = { 2, 2, 3, 3, 4 };
    public static final int[] TORSO_MAX_SLOTS = { 2, 4, 4, 6, 8 };
    public static final int[] QUAD_MAX_SLOTS = { 0, 5, 7, 9, 11 };

    public static final int LOCATION_ALLOWED_ANY = 0;
    public static final int LOCATION_ALLOWED_TORSO = 1;
    public static final int LOCATION_ALLOWED_ARM = 2;

    private static final int F_CONFLICT_JUMP_GEAR = 0x00000001;

    public CustomBattleArmorDialog(ClientGUI cl) {
        super(cl.frame, Messages.getString("CustomBattleArmorDialog.title"),
                true);
        m_client = cl.getClient();
        m_clientgui = cl;

        updatePlayerChoice();

        GridLayout gl = new GridLayout();
        gl.setColumns(1);
        gl.setRows(0);
        m_pParams.setLayout(gl);

        GridLayout tmpGL = new GridLayout(1, 2);
        Panel tmpP = new Panel();
        tmpP.setLayout(tmpGL);
        tmpP.add(m_labelBAName);
        m_tfBAName.addTextListener(this);
        tmpP.add(m_tfBAName);
        m_pParams.add(tmpP);

        tmpP = new Panel();
        tmpP.setLayout(tmpGL);
        tmpP.add(m_labelMenPerSquad);
        m_chMenPerSquad.addItemListener(this);
        tmpP.add(m_chMenPerSquad);
        m_pParams.add(tmpP);

        tmpP = new Panel();
        tmpP.setLayout(tmpGL);
        tmpP.add(m_labelTechBase);
        m_chTechBase.addItemListener(this);
        tmpP.add(m_chTechBase);
        m_pParams.add(tmpP);

        tmpP = new Panel();
        tmpP.setLayout(tmpGL);
        tmpP.add(m_labelChassisType);
        m_chChassisType.addItemListener(this);
        tmpP.add(m_chChassisType);
        m_pParams.add(tmpP);

        tmpP = new Panel();
        tmpP.setLayout(tmpGL);
        tmpP.add(m_labelWeightClass);
        m_chWeightClass.addItemListener(this);
        tmpP.add(m_chWeightClass);
        m_pParams.add(tmpP);

        tmpP = new Panel();
        tmpP.setLayout(tmpGL);
        tmpP.add(m_labelGroundMP);
        m_chGroundMP.addItemListener(this);
        tmpP.add(m_chGroundMP);
        m_pParams.add(tmpP);

        tmpGL = new GridLayout(1, 3);
        tmpP = new Panel();
        tmpP.setLayout(tmpGL);
        m_cbJumpQuery.addItemListener(this);
        tmpP.add(m_cbJumpQuery);

        m_cbVTOLQuery.addItemListener(this);
        tmpP.add(m_cbVTOLQuery);

        m_cbUMUQuery.addItemListener(this);
        tmpP.add(m_cbUMUQuery);
        m_pParams.add(tmpP);

        tmpGL = new GridLayout(1, 2);
        tmpP = new Panel();
        tmpP.setLayout(tmpGL);
        tmpP.add(m_labelJumpValue);
        m_chJumpValue.addItemListener(this);
        tmpP.add(m_chJumpValue);
        m_pParams.add(tmpP);

        tmpP = new Panel();
        tmpP.setLayout(tmpGL);
        tmpP.add(m_labelArmorType);
        m_chArmorType.addItemListener(this);
        tmpP.add(m_chArmorType);
        m_pParams.add(tmpP);

        tmpP = new Panel();
        tmpP.setLayout(tmpGL);
        tmpP.add(m_labelArmorValue);
        m_chArmorValue.addItemListener(this);
        tmpP.add(m_chArmorValue);
        m_pParams.add(tmpP);

        tmpP = new Panel();
        tmpP.setLayout(tmpGL);
        tmpP.add(m_labelLeftManipulator);
        m_chLeftManipulator.addItemListener(this);
        tmpP.add(m_chLeftManipulator);
        m_pParams.add(tmpP);

        tmpP = new Panel();
        tmpP.setLayout(tmpGL);
        tmpP.add(m_labelRightManipulator);
        m_chRightManipulator.addItemListener(this);
        tmpP.add(m_chRightManipulator);
        m_pParams.add(tmpP);

        tmpGL = new GridLayout(1, 3);
        tmpP = new Panel();
        tmpP.setLayout(tmpGL);
        tmpP.add(m_labelTorsoEquipment);
        tmpP.add(m_chTorsoEquipment);
        m_buttonAddTorso.addActionListener(this);
        tmpP.add(m_buttonAddTorso);
        m_pParams.add(tmpP);

        tmpGL = new GridLayout(1, 3);
        tmpP = new Panel();
        tmpP.setLayout(tmpGL);
        tmpP.add(m_labelRightArmEquipment);
        tmpP.add(m_chRightArmEquipment);
        m_buttonAddRightArm.addActionListener(this);
        tmpP.add(m_buttonAddRightArm);
        m_pParams.add(tmpP);

        tmpGL = new GridLayout(1, 3);
        tmpP = new Panel();
        tmpP.setLayout(tmpGL);
        tmpP.add(m_labelLeftArmEquipment);
        tmpP.add(m_chLeftArmEquipment);
        m_buttonAddLeftArm.addActionListener(this);
        tmpP.add(m_buttonAddLeftArm);
        m_pParams.add(tmpP);

        tmpGL = new GridLayout(1, 3);
        tmpP = new Panel();
        tmpP.setLayout(tmpGL);
        tmpP.add(m_labelTorsoCurrentEquipment);
        tmpP.add(m_chTorsoCurrentEquipment);
        m_buttonRemoveTorso.addActionListener(this);
        tmpP.add(m_buttonRemoveTorso);
        m_pParams.add(tmpP);

        tmpGL = new GridLayout(1, 3);
        tmpP = new Panel();
        tmpP.setLayout(tmpGL);
        tmpP.add(m_labelRightArmCurrentEquipment);
        tmpP.add(m_chRightArmCurrentEquipment);
        m_buttonRemoveRightArm.addActionListener(this);
        tmpP.add(m_buttonRemoveRightArm);
        m_pParams.add(tmpP);

        tmpGL = new GridLayout(1, 3);
        tmpP = new Panel();
        tmpP.setLayout(tmpGL);
        tmpP.add(m_labelLeftArmCurrentEquipment);
        tmpP.add(m_chLeftArmCurrentEquipment);
        m_buttonRemoveLeftArm.addActionListener(this);
        tmpP.add(m_buttonRemoveLeftArm);
        m_pParams.add(tmpP);

        m_pButtons.setLayout(new FlowLayout(FlowLayout.CENTER));
        m_pButtons.add(m_bPick);
        m_pButtons.add(m_bPickClose);
        m_pButtons.add(m_bSave);
        m_pButtons.add(m_buttonReset);
        m_pButtons.add(m_bCancel);
        m_pButtons.add(m_labelPlayer);
        m_pButtons.add(m_chPlayer);

        m_pLeft.setLayout(new BorderLayout());
        m_pLeft.add(m_pParams, BorderLayout.CENTER);
        m_pLeft.add(m_pButtons, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(m_pLeft, BorderLayout.WEST);
        m_BAView.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(m_BAView, BorderLayout.CENTER);

        setSize(800, 450);
        setLocation(computeDesiredLocation());
        new BattleArmorEquipment().initialize();
        populateChoices();
        m_bPick.addActionListener(this);
        m_bPickClose.addActionListener(this);
        m_buttonReset.addActionListener(this);
        m_bCancel.addActionListener(this);
        m_bSave.addActionListener(this);
        addWindowListener(this);
        updateWidgetEnablements();
        previewBA();
    }

    private void resetState() {
        restoreDefaultStates();
        populateChoices();
        previewBA();
    }

    private void restoreDefaultStates() {
        m_tfBAName.setText("");
        invalidReason = null;
        stateMenPerSquad = 1;
        stateTechBase = 0;
        stateChassisType = 0;
        stateWeightClass = 0;
        stateArmorType = 0;
        stateArmorValue = 0;
        stateJumpType = 0;
        stateJumpMP = 0;
        stateGroundMP = 1;
        stateCurrentWeight = 0;
        stateMinWeight = 0;
        stateMaxWeight = 400;
        stateManipulatorTypeLeft = 0;
        stateManipulatorTypeRight = 0;
        stateConflictFlags = 0;
        leftArmEquipment = null;
        rightArmEquipment = null;
        torsoEquipment = null;
    }

    private void updatePlayerChoice() {
        String lastChoice = m_chPlayer.getSelectedItem();
        m_chPlayer.removeAll();
        m_chPlayer.setEnabled(true);
        m_chPlayer.addItem(m_clientgui.getClient().getName());
        for (Iterator<Client> i = m_clientgui.getBots().values().iterator(); i
                .hasNext();) {
            m_chPlayer.addItem(i.next().getName());
        }
        if (m_chPlayer.getItemCount() == 1) {
            m_chPlayer.setEnabled(false);
        } else {
            m_chPlayer.select(lastChoice);
        }
    }

    public void run() {
        // I don't think we actually need to do anything here.
        // In fact, does this need to be a thread?...
    }

    private void populateChoices() {
        // Only two tech base choices: IS and Clan.
        m_chTechBase.add("Inner Sphere");
        m_chTechBase.add("Clan");
        m_chTechBase.select(0);

        // How many guys in a squad of these things?
        m_chMenPerSquad.add("1");
        m_chMenPerSquad.add("2");
        m_chMenPerSquad.add("3");
        m_chMenPerSquad.add("4");
        m_chMenPerSquad.add("5");
        m_chMenPerSquad.add("6");
        m_chMenPerSquad.select(0);

        // Again, only two chassis types: Quad and Biped.
        m_chChassisType.add("Biped");
        m_chChassisType.add("Quad");
        m_chChassisType.select(0);

        // At least initially, we'll have 5 weight classes:
        // PA(L), light, medium, heavy, assault.
        m_chWeightClass.add("PA(L)");
        m_chWeightClass.add("Light");
        m_chWeightClass.add("Medium");
        m_chWeightClass.add("Heavy");
        m_chWeightClass.add("Assault");
        m_chWeightClass.select(0);

        // Ground MP range depends on chassis and weight class.
        // We'll start with biped PA(L), the default chassis/weight.
        m_chGroundMP.add("1");
        m_chGroundMP.add("2");
        m_chGroundMP.add("3");
        m_chGroundMP.select(0);

        // Max Jump/VTOL/UMU MP depends on weight class and chosen movement
        // mode.
        // We'll default to jump MP for PA(L), the default values.
        m_chJumpValue.add("0");
        m_chJumpValue.add("1");
        m_chJumpValue.add("2");
        m_chJumpValue.add("3");
        m_chJumpValue.select(0);

        // Available armor types depends on tech base.
        // We'll start with IS armor types, since we default to IS tech base.
        m_chArmorType.add("Standard");
        m_chArmorType.add("Advanced");
        m_chArmorType.add("Prototype");
        m_chArmorType.add("Basic Stealth");
        m_chArmorType.add("Prototype Stealth");
        m_chArmorType.add("Standard Stealth");
        m_chArmorType.add("Improved Stealth");
        m_chArmorType.add("Fire Resistant");
        m_chArmorType.add("Mimetic");
        m_chArmorType.select(0);

        // Next we populate the manipulator choicees.
        for (int x = 0; x < BattleArmor.MANIPULATOR_TYPE_STRINGS.length; x++) {
            m_chLeftManipulator.add(BattleArmor.MANIPULATOR_TYPE_STRINGS[x]);
            m_chRightManipulator.add(BattleArmor.MANIPULATOR_TYPE_STRINGS[x]);
        }
        m_chLeftManipulator.select(0);
        m_chRightManipulator.select(0);

        // Max armor value depends on weight class.
        // We'll default to that for PA(L), the default weight class.
        m_chArmorValue.add("0");
        m_chArmorValue.add("1");
        m_chArmorValue.add("2");
        m_chArmorValue.select(0);

        // Populate the equipment choices.
        updateEquipmentChoices();
    }

    private void updateEquipmentChoices() {
        String value = m_chTorsoEquipment.getSelectedItem();
        m_chTorsoEquipment.removeAll();
        Object[] tmpE = equipmentTypes.toArray();
        for (int x = 0; x < tmpE.length; x++) {
            BattleArmorEquipment tmpBAE = (BattleArmorEquipment) (tmpE[x]);
            if (((tmpBAE.techBase == TECH_BASE_BOTH) || (tmpBAE.techBase == stateTechBase))
                    && !(hasConflictFlag(tmpBAE.conflictFlag))
                    && ((tmpBAE.allowedLocation == LOCATION_ALLOWED_ANY) || (tmpBAE.allowedLocation == LOCATION_ALLOWED_TORSO))) {
                m_chTorsoEquipment.add(tmpBAE.name);
            }
        }
        m_chTorsoEquipment.select(value);

        value = m_chRightArmEquipment.getSelectedItem();
        m_chRightArmEquipment.removeAll();
        if (stateChassisType != BattleArmor.CHASSIS_TYPE_QUAD) {
            tmpE = equipmentTypes.toArray();
            for (int x = 0; x < tmpE.length; x++) {
                BattleArmorEquipment tmpBAE = (BattleArmorEquipment) (tmpE[x]);
                if (((tmpBAE.techBase == TECH_BASE_BOTH) || (tmpBAE.techBase == stateTechBase))
                        && !(hasConflictFlag(tmpBAE.conflictFlag))
                        && ((tmpBAE.allowedLocation == LOCATION_ALLOWED_ANY) || (tmpBAE.allowedLocation == LOCATION_ALLOWED_ARM))) {
                    m_chRightArmEquipment.add(tmpBAE.name);
                }
            }
        }
        m_chRightArmEquipment.select(value);

        value = m_chLeftArmEquipment.getSelectedItem();
        m_chLeftArmEquipment.removeAll();
        if (stateChassisType != BattleArmor.CHASSIS_TYPE_QUAD) {
            tmpE = equipmentTypes.toArray();
            for (int x = 0; x < tmpE.length; x++) {
                BattleArmorEquipment tmpBAE = (BattleArmorEquipment) (tmpE[x]);
                if (((tmpBAE.techBase == TECH_BASE_BOTH) || (tmpBAE.techBase == stateTechBase))
                        && !(hasConflictFlag(tmpBAE.conflictFlag))
                        && ((tmpBAE.allowedLocation == LOCATION_ALLOWED_ANY) || (tmpBAE.allowedLocation == LOCATION_ALLOWED_ARM))) {
                    m_chLeftArmEquipment.add(tmpBAE.name);
                }
            }
        }
        m_chLeftArmEquipment.select(value);

        m_chLeftArmCurrentEquipment.removeAll();
        if (leftArmEquipment != null) {
            Enumeration<BattleArmorEquipment> tmpEE = leftArmEquipment
                    .elements();
            while (tmpEE.hasMoreElements()) {
                BattleArmorEquipment tmpBAE = (tmpEE.nextElement());
                m_chLeftArmCurrentEquipment.add(tmpBAE.name);
            }
        }
        m_chRightArmCurrentEquipment.removeAll();
        if (rightArmEquipment != null) {
            Enumeration<BattleArmorEquipment> tmpEE = rightArmEquipment
                    .elements();
            while (tmpEE.hasMoreElements()) {
                BattleArmorEquipment tmpBAE = (tmpEE.nextElement());
                m_chRightArmCurrentEquipment.add(tmpBAE.name);
            }
        }
        m_chTorsoCurrentEquipment.removeAll();
        if (torsoEquipment != null) {
            Enumeration<BattleArmorEquipment> tmpEE = torsoEquipment.elements();
            while (tmpEE.hasMoreElements()) {
                BattleArmorEquipment tmpBAE = (tmpEE.nextElement());
                m_chTorsoCurrentEquipment.add(tmpBAE.name);
            }
        }
    }

    private boolean hasConflictFlag(int testFlags) {
        return ((stateConflictFlags & testFlags) > 0);
    }

    private void updateGroundMPChoices() {
        m_chGroundMP.removeAll();
        if (stateChassisType == BattleArmor.CHASSIS_TYPE_BIPED) {
            // Biped BA
            m_chGroundMP.add("1");
            m_chGroundMP.add("2");
            if ((stateWeightClass == WEIGHT_CLASS_PAL)
                    || (stateWeightClass == WEIGHT_CLASS_LIGHT)
                    || (stateWeightClass == WEIGHT_CLASS_MEDIUM)) {
                m_chGroundMP.add("3");
            }
        } else {
            // Quad BA
            m_chGroundMP.add("2");
            m_chGroundMP.add("3");
            m_chGroundMP.add("4");
            if ((stateWeightClass == WEIGHT_CLASS_LIGHT)
                    || (stateWeightClass == WEIGHT_CLASS_MEDIUM)) {
                m_chGroundMP.add("5");
            }
        }
        m_chGroundMP.select(0);
        stateGroundMP = Integer.parseInt(m_chGroundMP.getSelectedItem());
    }

    private void updateJumpMPChoices() {
        int tmp = m_chJumpValue.getSelectedIndex();
        m_chJumpValue.removeAll();
        for (int x = 0; x <= JUMP_MP_LIMITS[stateJumpType][stateWeightClass]; x++) {
            m_chJumpValue.add(Integer.toString(x));
        }
        if (tmp >= m_chJumpValue.getItemCount()) {
            m_chJumpValue.select(m_chJumpValue.getItemCount() - 1);
            stateJumpMP = m_chJumpValue.getSelectedIndex();
        } else {
            m_chJumpValue.select(tmp);
        }
    }

    private void updateArmorValueChoices() {
        int tmp = m_chArmorValue.getSelectedIndex();
        m_chArmorValue.removeAll();
        m_chArmorValue.add("0");
        m_chArmorValue.add("1");
        m_chArmorValue.add("2");
        if (stateWeightClass > 0) {
            m_chArmorValue.add("3");
            m_chArmorValue.add("4");
            m_chArmorValue.add("5");
            m_chArmorValue.add("6");
            if (stateWeightClass > 1) {
                m_chArmorValue.add("7");
                m_chArmorValue.add("8");
                m_chArmorValue.add("9");
                m_chArmorValue.add("10");
                if (stateWeightClass > 2) {
                    m_chArmorValue.add("11");
                    m_chArmorValue.add("12");
                    m_chArmorValue.add("13");
                    m_chArmorValue.add("14");
                    if (stateWeightClass > 3) {
                        m_chArmorValue.add("15");
                        m_chArmorValue.add("16");
                        m_chArmorValue.add("17");
                        m_chArmorValue.add("18");
                    }
                }
            }
        }
        if (tmp >= m_chArmorValue.getItemCount()) {
            m_chArmorValue.select(m_chArmorValue.getItemCount() - 1);
            stateArmorValue = m_chArmorValue.getSelectedIndex();
        } else {
            m_chArmorValue.select(tmp);
        }
    }

    private Point computeDesiredLocation() {
        int desiredX = m_clientgui.frame.getLocation().x
                + m_clientgui.frame.getSize().width / 2 - getSize().width / 2;
        if (desiredX < 0) {
            desiredX = 0;
        }
        int desiredY = m_clientgui.frame.getLocation().y
                + m_clientgui.frame.getSize().height / 2 - getSize().height / 2;
        if (desiredY < 0) {
            desiredY = 0;
        }
        return new Point(desiredX, desiredY);
    }

    @Override
    public void setVisible(boolean show) {
        super.setVisible(show);
        if (show) {
            updatePlayerChoice();
            setLocation(computeDesiredLocation());
            m_BAView.setCaretPosition(0);
        }
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == m_buttonReset) {
            resetState();
            return;
        } else if (ae.getSource() == m_buttonAddTorso) {
            BattleArmorEquipment tmpBAE = equipmentTypes.get(equipmentNames
                    .indexOf(m_chTorsoEquipment.getSelectedItem()));
            if (torsoEquipment == null) {
                torsoEquipment = new Vector<BattleArmorEquipment>();
            }
            torsoEquipment.add(tmpBAE);
            stateConflictFlags |= tmpBAE.conflictFlag;

            // Make sure the BA preview is now correct...
            previewBA();

            // Make sure we update the equpment choices, since they may have now
            // changed...
            updateEquipmentChoices();

            // Nothing else in actionPerformed will matter, so lets move on!
            return;
        } else if (ae.getSource() == m_buttonAddRightArm) {
            BattleArmorEquipment tmpBAE = equipmentTypes.get(equipmentNames
                    .indexOf(m_chRightArmEquipment.getSelectedItem()));
            if (rightArmEquipment == null) {
                rightArmEquipment = new Vector<BattleArmorEquipment>();
            }
            rightArmEquipment.add(tmpBAE);
            stateConflictFlags |= tmpBAE.conflictFlag;

            // Make sure the BA preview is now correct...
            previewBA();

            // Make sure we update the equpment choices, since they may have now
            // changed...
            updateEquipmentChoices();

            // Nothing else in actionPerformed will matter, so lets move on!
            return;
        } else if (ae.getSource() == m_buttonAddLeftArm) {
            BattleArmorEquipment tmpBAE = equipmentTypes.get(equipmentNames
                    .indexOf(m_chLeftArmEquipment.getSelectedItem()));
            if (leftArmEquipment == null) {
                leftArmEquipment = new Vector<BattleArmorEquipment>();
            }
            leftArmEquipment.add(tmpBAE);
            stateConflictFlags |= tmpBAE.conflictFlag;

            // Make sure the BA preview is now correct...
            previewBA();

            // Make sure we update the equpment choices, since they may have now
            // changed...
            updateEquipmentChoices();

            // Nothing else in actionPerformed will matter, so lets move on!
            return;
        } else if (ae.getSource() == m_buttonRemoveTorso) {
            if (torsoEquipment != null) {
                String removeItem = m_chTorsoCurrentEquipment.getSelectedItem();
                Enumeration<BattleArmorEquipment> tmpE = torsoEquipment
                        .elements();
                while (tmpE.hasMoreElements()) {
                    BattleArmorEquipment tmpBAE = (tmpE.nextElement());
                    if (tmpBAE.name.equals(removeItem)) {
                        torsoEquipment.remove(tmpBAE);
                        break;
                    }
                }
                if (torsoEquipment.size() <= 0) {
                    torsoEquipment = null;
                }

                // Make sure the BA preview is now correct...
                previewBA();

                // Make sure we update the equpment choices, since they may have
                // now changed...
                updateEquipmentChoices();
            }

            // Nothing else in actionPerformed will matter, so lets move on!
            return;
        } else if (ae.getSource() == m_buttonRemoveRightArm) {
            if (rightArmEquipment != null) {
                String removeItem = m_chRightArmCurrentEquipment
                        .getSelectedItem();
                Enumeration<BattleArmorEquipment> tmpE = rightArmEquipment
                        .elements();
                while (tmpE.hasMoreElements()) {
                    BattleArmorEquipment tmpBAE = (tmpE.nextElement());
                    if (tmpBAE.name.equals(removeItem)) {
                        rightArmEquipment.remove(tmpBAE);
                        break;
                    }
                }
                if (rightArmEquipment.size() <= 0) {
                    rightArmEquipment = null;
                }

                // Make sure the BA preview is now correct...
                previewBA();

                // Make sure we update the equpment choices, since they may have
                // now changed...
                updateEquipmentChoices();
            }

            // Nothing else in actionPerformed will matter, so lets move on!
            return;
        } else if (ae.getSource() == m_buttonRemoveLeftArm) {
            if (leftArmEquipment != null) {
                String removeItem = m_chLeftArmCurrentEquipment
                        .getSelectedItem();
                Enumeration<BattleArmorEquipment> tmpE = leftArmEquipment
                        .elements();
                while (tmpE.hasMoreElements()) {
                    BattleArmorEquipment tmpBAE = (tmpE.nextElement());
                    if (tmpBAE.name.equals(removeItem)) {
                        leftArmEquipment.remove(tmpBAE);
                        break;
                    }
                }
                if (leftArmEquipment.size() <= 0) {
                    leftArmEquipment = null;
                }

                // Make sure the BA preview is now correct...
                previewBA();

                // Make sure we update the equpment choices, since they may have
                // now changed...
                updateEquipmentChoices();
            }

            // Nothing else in actionPerformed will matter, so lets move on!
            return;
        } else if ((ae.getSource() == m_bPick)
                || (ae.getSource() == m_bPickClose)) {
            // Here, we need to add the current BA as a new entity, if it can
            // legally do so...
            if (!isOK()) {
                new megamek.client.ui.AWT.AlertDialog(m_clientgui.frame,
                        "Can't do that!", "You can't add an invalid unit.")
                        .setVisible(true);
                return;
            }
            try {
                BattleArmor e = getEntity();
                Client c = null;
                if (m_chPlayer.getSelectedIndex() > 0) {
                    String name = m_chPlayer.getSelectedItem();
                    c = m_clientgui.getBots().get(name);
                }
                if (c == null) {
                    c = m_client;
                }
                e.setOwner(c.getLocalPlayer());
                c.sendAddEntity(e);
            } catch (Exception ex) {
                System.err.println("Error while loading custom BattleArmor!");
                ex.printStackTrace();
                return;
            }
        } else if (ae.getSource() == m_bSave) {
            FileDialog fd = new FileDialog(
                    m_clientgui.frame,
                    Messages
                            .getString("CustomBattleArmorDialog.FileSaveDialog"), FileDialog.SAVE); //$NON-NLS-1$
            fd.setDirectory("data" + File.separatorChar + "mechfiles");
            fd.setFile(m_tfBAName.getText() + ".blk");
            fd.setFilenameFilter(new FilenameFilter() {
                public boolean accept(File f, String s) {
                    return s.endsWith(".blk");
                }
            });
            fd.setModal(true);
            fd.setVisible(true);
            String filename = fd.getDirectory() + File.separator + fd.getFile();
            if (filename.indexOf('.') == -1) {
                filename = filename + ".blk";
            }
            File f = new File(filename);
            try {
                BattleArmor ba = getEntity();
                FileWriter wr = new FileWriter(f);
                wr.write(ba.getBLK());
                wr.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        // Specifically NOT an else/if, because this can happen at the same time
        // as one option above.
        if ((ae.getSource() == m_bCancel) || (ae.getSource() == m_bPickClose)) {
            setVisible(false);
        }
    }

    public void textValueChanged(TextEvent te) {
        previewBA();
    }

    public void itemStateChanged(ItemEvent ie) {
        if (ie.getSource() == m_cbJumpQuery) {
            if (m_cbJumpQuery.getState()) {
                stateJumpType = JUMP_TYPE_JUMP;
                updateJumpMPChoices();
                m_labelJumpValue.setText(Messages
                        .getString("CustomBattleArmorDialog.m_labelJumpValue"));
            }
        } else if (ie.getSource() == m_cbVTOLQuery) {
            if (m_cbVTOLQuery.getState()) {
                stateJumpType = JUMP_TYPE_VTOL;
                updateJumpMPChoices();
                m_labelJumpValue.setText(Messages
                        .getString("CustomBattleArmorDialog.m_labelVTOLValue"));
            }
        } else if (ie.getSource() == m_cbUMUQuery) {
            if (m_cbUMUQuery.getState()) {
                stateJumpType = JUMP_TYPE_UMU;
                updateJumpMPChoices();
                m_labelJumpValue.setText(Messages
                        .getString("CustomBattleArmorDialog.m_labelUMUValue"));
            }
        } else if (ie.getSource() == m_chMenPerSquad) {
            if (stateMenPerSquad != Integer.parseInt(m_chMenPerSquad
                    .getSelectedItem())) {
                // Does this actually affect anything else? I'm not sure.
                stateMenPerSquad = Integer.parseInt(m_chMenPerSquad
                        .getSelectedItem());
            }
        } else if (ie.getSource() == m_chTechBase) {
            if (stateTechBase != m_chTechBase.getSelectedIndex()) {
                // If the tech base actually changed, we might have to
                // re-calculate things.
                stateTechBase = m_chTechBase.getSelectedIndex();

                // Because the tech base changed, available equipment may also
                // have changed.
                updateEquipmentChoices();
            }
        } else if (ie.getSource() == m_chChassisType) {
            if (stateChassisType != m_chChassisType.getSelectedIndex()) {
                // The chassis type is actually changing!
                // The state of other settings might change.
                stateChassisType = m_chChassisType.getSelectedIndex();
                if (stateChassisType == BattleArmor.CHASSIS_TYPE_QUAD) {
                    // FIXME
                    // We have to remove everything from the arms correctly!
                    // That way we won't mess anything up!
                    leftArmEquipment = null;
                    rightArmEquipment = null;
                }
                updateGroundMPChoices();
                updateJumpMPChoices();
                updateEquipmentChoices();
            }
        } else if (ie.getSource() == m_chWeightClass) {
            if (stateWeightClass != m_chWeightClass.getSelectedIndex()) {
                stateWeightClass = m_chWeightClass.getSelectedIndex();
                // Needs to update min and max weights!
                switch (stateWeightClass) {
                    case WEIGHT_CLASS_PAL:
                        stateMinWeight = 0;
                        stateMaxWeight = 400;
                        break;
                    case WEIGHT_CLASS_LIGHT:
                        stateMinWeight = 401;
                        stateMaxWeight = 750;
                        break;
                    case WEIGHT_CLASS_MEDIUM:
                        stateMinWeight = 751;
                        stateMaxWeight = 1000;
                        break;
                    case WEIGHT_CLASS_HEAVY:
                        stateMinWeight = 1001;
                        stateMaxWeight = 1500;
                        break;
                    case WEIGHT_CLASS_ASSAULT:
                        stateMinWeight = 1501;
                        stateMaxWeight = 2000;
                        break;
                }
                updateGroundMPChoices();
                updateJumpMPChoices();
                updateArmorValueChoices();
            }
        } else if (ie.getSource() == m_chGroundMP) {
            if (stateGroundMP != Integer.parseInt(m_chGroundMP
                    .getSelectedItem())) {
                stateGroundMP = Integer
                        .parseInt(m_chGroundMP.getSelectedItem());
            }
        } else if (ie.getSource() == m_chJumpValue) {
            if (stateJumpMP != m_chJumpValue.getSelectedIndex()) {
                stateJumpMP = m_chJumpValue.getSelectedIndex();
            }
        } else if (ie.getSource() == m_chLeftManipulator) {
            if (stateManipulatorTypeLeft != m_chLeftManipulator
                    .getSelectedIndex()) {
                stateManipulatorTypeLeft = m_chLeftManipulator
                        .getSelectedIndex();
            }
        } else if (ie.getSource() == m_chRightManipulator) {
            if (stateManipulatorTypeRight != m_chRightManipulator
                    .getSelectedIndex()) {
                stateManipulatorTypeRight = m_chRightManipulator
                        .getSelectedIndex();
            }
        } else if (ie.getSource() == m_chArmorType) {
            if (stateArmorType != m_chArmorType.getSelectedIndex()) {
                stateArmorType = m_chArmorType.getSelectedIndex();
            }
        } else if (ie.getSource() == m_chArmorValue) {
            if (stateArmorValue != m_chArmorValue.getSelectedIndex()) {
                stateArmorValue = m_chArmorValue.getSelectedIndex();
            }
        }
        previewBA();
    }

    void clearBAPreview() {
        m_BAView.setEditable(false);
        m_BAView.setText("");
    }

    void previewBA() {
        String preview = generateBattleArmorPreview();
        m_BAView.setEditable(false);
        m_BAView.setText(preview);
    }

    public void keyPressed(java.awt.event.KeyEvent ke) {
        if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
            ActionEvent event = new ActionEvent(m_bPick,
                    ActionEvent.ACTION_PERFORMED, "");
            actionPerformed(event);
        }
    }

    public void keyReleased(java.awt.event.KeyEvent ke) {
        // Do nothing.
    }

    public void keyTyped(java.awt.event.KeyEvent ke) {
        // Do nothing.
    }

    //
    // WindowListener
    //
    public void windowActivated(java.awt.event.WindowEvent windowEvent) {
        // Do nothing.
    }

    public void windowClosed(java.awt.event.WindowEvent windowEvent) {
        // Do nothing.
    }

    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
        setVisible(false);
    }

    public void windowDeactivated(java.awt.event.WindowEvent windowEvent) {
        // Do nothing.
    }

    public void windowDeiconified(java.awt.event.WindowEvent windowEvent) {
        // Do nothing.
    }

    public void windowIconified(java.awt.event.WindowEvent windowEvent) {
        // Do nothing.
    }

    public void windowOpened(java.awt.event.WindowEvent windowEvent) {
        // Do nothing.
    }

    private void updateWidgetEnablements() {
        m_bPick.setEnabled(true);
        m_bPickClose.setEnabled(true);
    }

    public String generateBattleArmorPreview() {
        StringBuffer retVal = new StringBuffer("");
        if (isOK()) {
            retVal.append(">>>");
            retVal.append(Messages.getString("CustomBattleArmorDialog.valid"));
            retVal.append("<<<");
        } else {
            retVal.append(">>>");
            retVal
                    .append(Messages
                            .getString("CustomBattleArmorDialog.invalid"));
            retVal.append("<<<\n");
            if (invalidReason != null) {
                retVal.append(invalidReason);
            }
        }
        retVal.append("\n\n");

        retVal.append(Messages
                .getString("CustomBattleArmorDialog.m_labelBAName"));
        if (m_tfBAName.getText().trim().length() < 1) {
            retVal.append("<NONE>");
        } else {
            retVal.append(m_tfBAName.getText());
        }
        retVal.append("\n");

        retVal.append(Messages
                .getString("CustomBattleArmorDialog.m_labelMenPerSquad"));
        retVal.append(stateMenPerSquad);
        retVal.append("\n");

        retVal.append(Messages
                .getString("CustomBattleArmorDialog.m_labelTechBase"));
        if (stateTechBase == TECH_BASE_IS) {
            retVal
                    .append(Messages
                            .getString("CustomBattleArmorDialog.tech_base_inner_sphere"));
        } else {
            retVal.append(Messages
                    .getString("CustomBattleArmorDialog.tech_base_clan"));
        }
        retVal.append("\n");

        retVal.append(Messages
                .getString("CustomBattleArmorDialog.m_labelChassisType"));
        if (stateChassisType == BattleArmor.CHASSIS_TYPE_BIPED) {
            retVal.append(Messages
                    .getString("CustomBattleArmorDialog.chassis_type_biped"));
        } else {
            retVal.append(Messages
                    .getString("CustomBattleArmorDialog.chassis_type_quad"));
        }
        retVal.append("\n");

        retVal.append(Messages
                .getString("CustomBattleArmorDialog.m_labelWeightClass"));
        switch (stateWeightClass) {
            case WEIGHT_CLASS_PAL:
                retVal.append(Messages
                        .getString("CustomBattleArmorDialog.weight_class_pal"));
                break;
            case WEIGHT_CLASS_LIGHT:
                retVal
                        .append(Messages
                                .getString("CustomBattleArmorDialog.weight_class_light"));
                break;
            case WEIGHT_CLASS_MEDIUM:
                retVal
                        .append(Messages
                                .getString("CustomBattleArmorDialog.weight_class_medium"));
                break;
            case WEIGHT_CLASS_HEAVY:
                retVal
                        .append(Messages
                                .getString("CustomBattleArmorDialog.weight_class_heavy"));
                break;
            case WEIGHT_CLASS_ASSAULT:
                retVal
                        .append(Messages
                                .getString("CustomBattleArmorDialog.weight_class_assault"));
                break;
        }
        retVal.append(" (");
        retVal.append(Messages.getString("CustomBattleArmorDialog.weight"));
        retVal.append(getChassisWeight());
        retVal.append(")");
        retVal.append("\n");

        retVal.append(Messages.getString("CustomBattleArmorDialog.min_weight"));
        retVal.append(stateMinWeight);
        retVal.append("\n");

        retVal.append(Messages.getString("CustomBattleArmorDialog.max_weight"));
        retVal.append(stateMaxWeight);
        retVal.append("\n");

        retVal.append(Messages
                .getString("CustomBattleArmorDialog.current_weight"));
        retVal.append(stateCurrentWeight);
        retVal.append("\n");

        retVal.append(Messages
                .getString("CustomBattleArmorDialog.m_labelGroundMP"));
        retVal.append(stateGroundMP);
        retVal.append(" (");
        retVal.append(Messages.getString("CustomBattleArmorDialog.weight"));
        retVal.append(getGroundMPWeight());
        retVal.append(")");
        retVal.append("\n");

        if (stateJumpType == JUMP_TYPE_JUMP) {
            retVal.append(Messages
                    .getString("CustomBattleArmorDialog.m_labelJumpValue"));
        } else if (stateJumpType == JUMP_TYPE_VTOL) {
            retVal.append(Messages
                    .getString("CustomBattleArmorDialog.m_labelVTOLValue"));
        } else if (stateJumpType == JUMP_TYPE_UMU) {
            retVal.append(Messages
                    .getString("CustomBattleArmorDialog.m_labelUMUValue"));
        }
        retVal.append(getTotalJumpMP());
        retVal.append(" (");
        retVal.append(Messages.getString("CustomBattleArmorDialog.weight"));
        retVal.append(getJumpMPWeight());
        retVal.append(")");
        retVal.append("\n");

        retVal.append(Messages
                .getString("CustomBattleArmorDialog.m_labelArmorType"));
        retVal.append(ARMOR_TYPE_STRINGS[stateArmorType]);
        retVal.append("\n");

        retVal.append(Messages
                .getString("CustomBattleArmorDialog.m_labelArmorValue"));
        retVal.append(stateArmorValue);
        retVal.append(" (");
        retVal.append(Messages.getString("CustomBattleArmorDialog.weight"));
        retVal.append(getArmorWeight());
        retVal.append(")");
        retVal.append("\n\n");

        retVal.append(Messages.getString("CustomBattleArmorDialog.equipment"));
        retVal.append("\n");
        retVal.append(Messages
                .getString("CustomBattleArmorDialog.m_labelLeftManipulator"));
        retVal
                .append(BattleArmor.MANIPULATOR_TYPE_STRINGS[stateManipulatorTypeLeft]);
        retVal.append(" (");
        retVal.append(Messages.getString("CustomBattleArmorDialog.weight"));
        retVal.append(MANIPULATOR_TYPE_WEIGHT[stateManipulatorTypeLeft]);
        retVal.append(")");
        retVal.append("\n");
        retVal.append(Messages
                .getString("CustomBattleArmorDialog.m_labelRightManipulator"));
        retVal
                .append(BattleArmor.MANIPULATOR_TYPE_STRINGS[stateManipulatorTypeRight]);
        retVal.append(" (");
        retVal.append(Messages.getString("CustomBattleArmorDialog.weight"));
        retVal.append(MANIPULATOR_TYPE_WEIGHT[stateManipulatorTypeRight]);
        retVal.append(")");
        retVal.append("\n\n");
        // Print the rest of the equipment on this thing!
        if (torsoEquipment != null) {
            Enumeration<BattleArmorEquipment> tmpE = torsoEquipment.elements();
            while (tmpE.hasMoreElements()) {
                BattleArmorEquipment tmpBAE = (tmpE.nextElement());
                retVal.append(tmpBAE.getDescription());
                retVal.append("\n");
            }
        }
        if (rightArmEquipment != null) {
            Enumeration<BattleArmorEquipment> tmpE = rightArmEquipment
                    .elements();
            while (tmpE.hasMoreElements()) {
                BattleArmorEquipment tmpBAE = (tmpE.nextElement());
                retVal.append(tmpBAE.getDescription());
                retVal.append("\n");
            }
        }
        if (leftArmEquipment != null) {
            Enumeration<BattleArmorEquipment> tmpE = leftArmEquipment
                    .elements();
            while (tmpE.hasMoreElements()) {
                BattleArmorEquipment tmpBAE = (tmpE.nextElement());
                retVal.append(tmpBAE.getDescription());
                retVal.append("\n");
            }
        }
        retVal.append("\n");

        retVal.append(Messages.getString("CustomBattleArmorDialog.bvEach"));
        retVal.append(calcSuitBV());
        retVal.append("\n");

        retVal.append(Messages.getString("CustomBattleArmorDialog.bvSquad"));
        retVal.append(calcSquadBV());
        retVal.append("\n\n");

        retVal.append(Messages.getString("CustomBattleArmorDialog.costEach"));
        retVal.append(calcSuitCost());
        retVal.append("\n");

        retVal.append(Messages.getString("CustomBattleArmorDialog.costSquad"));
        retVal.append(calcSquadCost());
        retVal.append("\n");

        return retVal.toString();
    }

    public void calcCurrentWeight() {
        stateCurrentWeight = 0;

        // Add in the chassis weight.
        stateCurrentWeight += getChassisWeight();

        // Add in the weight of the unit's armor...
        stateCurrentWeight += getArmorWeight();

        // Add in the unit's ground movement weight...
        stateCurrentWeight += getGroundMPWeight();

        // Add in the unit's jump/VTOL/UMU movement weight...
        stateCurrentWeight += getJumpMPWeight();

        // Add in the weight of all the unit's other equipment.
        stateCurrentWeight += getManipulatorWeight();
        if (leftArmEquipment != null) {
            Enumeration<BattleArmorEquipment> tmpE = leftArmEquipment
                    .elements();
            while (tmpE.hasMoreElements()) {
                BattleArmorEquipment tmpBAE = (tmpE.nextElement());
                stateCurrentWeight += tmpBAE.weight;
            }
        }
        if (rightArmEquipment != null) {
            Enumeration<BattleArmorEquipment> tmpE = rightArmEquipment
                    .elements();
            while (tmpE.hasMoreElements()) {
                BattleArmorEquipment tmpBAE = (tmpE.nextElement());
                stateCurrentWeight += tmpBAE.weight;
            }
        }
        if (torsoEquipment != null) {
            Enumeration<BattleArmorEquipment> tmpE = torsoEquipment.elements();
            while (tmpE.hasMoreElements()) {
                BattleArmorEquipment tmpBAE = (tmpE.nextElement());
                stateCurrentWeight += tmpBAE.weight;
            }
        }

        // FIXME
        // Needs to finish updating stateCurrentWeight!
    }

    public boolean isOK() {
        // We need to check a whole bunch of crap to make sure this is valid.

        // Calculate the weight up front, just to make sure it's been calculated
        // and updated!
        calcCurrentWeight();

        // We'll arbitrarily require the squad to have a name.
        // No blank designations.
        //NOTE: BROKEN
        /*if ((m_tfBAName.getText() == null)
                || (m_tfBAName.getText().trim().length() < 1)) {
            invalidReason = "Squads must be named.";
            return false;
        }*/

        // Quad chassis can't be PA(L)
        if ((stateChassisType == BattleArmor.CHASSIS_TYPE_QUAD)
                && (stateWeightClass == WEIGHT_CLASS_PAL)) {
            invalidReason = "PA(L) suits cannot have a quad chassis.";
            return false;
        }

        // Make sure it's not currently overweight.
        // According to the CBT folks, it's legal for it to be underweight.
        if (stateCurrentWeight > stateMaxWeight) {
            invalidReason = "Suit overweight.";
            return false;
        }

        // Check to make sure the armor is valid for the tech base.
        if (ARMOR_TYPE_WEIGHT[stateTechBase][stateArmorType] == 0) {
            // For now, we allow selection of illegal armor types...
            // And include this slightly cludgy way of detecting illegal
            // armor/tech base pairings.
            invalidReason = ARMOR_TYPE_STRINGS[stateArmorType]
                    + " Armor not legal for chosen tech base.";
            return false;
        }

        // Certain combinations of manipulators make it invalid.
        if (((stateManipulatorTypeLeft == BattleArmor.MANIPULATOR_CARGO_LIFTER) && (stateManipulatorTypeRight != BattleArmor.MANIPULATOR_CARGO_LIFTER))
                || ((stateManipulatorTypeRight == BattleArmor.MANIPULATOR_CARGO_LIFTER) && (stateManipulatorTypeLeft != BattleArmor.MANIPULATOR_CARGO_LIFTER))) {
            invalidReason = "Cargo lifter manipulators must be mounted in pairs.";
            return false;
        }

        // Certain combinations of manipulators make it invalid.
        if (((stateManipulatorTypeLeft == BattleArmor.MANIPULATOR_BATTLE_MAGNET) && (stateManipulatorTypeRight != BattleArmor.MANIPULATOR_BATTLE_MAGNET))
                || ((stateManipulatorTypeRight == BattleArmor.MANIPULATOR_BATTLE_MAGNET) && (stateManipulatorTypeLeft != BattleArmor.MANIPULATOR_BATTLE_MAGNET))) {
            invalidReason = "Magnetic manipulators must be mounted in pairs.";
            return false;
        }

        // Certain combinations of manipulators make it invalid.
        if (((stateManipulatorTypeLeft == BattleArmor.MANIPULATOR_BASIC_MINE_CLEARANCE) && (stateManipulatorTypeRight != BattleArmor.MANIPULATOR_BASIC_MINE_CLEARANCE))
                || ((stateManipulatorTypeRight == BattleArmor.MANIPULATOR_BASIC_MINE_CLEARANCE) && (stateManipulatorTypeLeft != BattleArmor.MANIPULATOR_BASIC_MINE_CLEARANCE))) {
            invalidReason = "Mine clearance manipulators must be mounted in pairs.";
            return false;
        }

        // Check to make sure none of the locations have gone over on slots.
        if (stateChassisType == BattleArmor.CHASSIS_TYPE_QUAD) {
            // We're only going to be using torso stuff here.
            // Quads only have one location!
            if (torsoEquipment != null) {
                int totalSlots = 0;
                Enumeration<BattleArmorEquipment> tmpE = torsoEquipment
                        .elements();
                while (tmpE.hasMoreElements()) {
                    BattleArmorEquipment tmpBAE = (tmpE.nextElement());
                    totalSlots += tmpBAE.slots;
                }
                if (totalSlots > (QUAD_MAX_SLOTS[stateWeightClass] - ARMOR_TYPE_SLOTS[stateArmorType])) {
                    invalidReason = "Unit is using more slots than are available.";
                    return false;
                }
            }
        } else {
            // Here, we have to check all three locations individually.
            int totalFreeSlots = (2 * ARM_MAX_SLOTS[stateWeightClass])
                    + TORSO_MAX_SLOTS[stateWeightClass];
            if (leftArmEquipment != null) {
                int totalSlots = 0;
                Enumeration<BattleArmorEquipment> tmpE = leftArmEquipment
                        .elements();
                while (tmpE.hasMoreElements()) {
                    BattleArmorEquipment tmpBAE = (tmpE.nextElement());
                    totalSlots += tmpBAE.slots;
                }
                if (totalSlots > ARM_MAX_SLOTS[stateWeightClass]) {
                    invalidReason = "Left Arm is using more slots than are available.";
                    return false;
                }
                totalFreeSlots -= totalSlots;
            }
            if (rightArmEquipment != null) {
                int totalSlots = 0;
                Enumeration<BattleArmorEquipment> tmpE = rightArmEquipment
                        .elements();
                while (tmpE.hasMoreElements()) {
                    BattleArmorEquipment tmpBAE = (tmpE.nextElement());
                    totalSlots += tmpBAE.slots;
                }
                if (totalSlots > ARM_MAX_SLOTS[stateWeightClass]) {
                    invalidReason = "Right Arm is using more slots than are available.";
                    return false;
                }
                totalFreeSlots -= totalSlots;
            }
            if (torsoEquipment != null) {
                int totalSlots = 0;
                Enumeration<BattleArmorEquipment> tmpE = torsoEquipment
                        .elements();
                while (tmpE.hasMoreElements()) {
                    BattleArmorEquipment tmpBAE = (tmpE.nextElement());
                    totalSlots += tmpBAE.slots;
                }
                if (totalSlots > TORSO_MAX_SLOTS[stateWeightClass]) {
                    invalidReason = "Torso is using more slots than are available.";
                    return false;
                }
                totalFreeSlots -= totalSlots;
            }
            // Don't forget to include armor...
            if (totalFreeSlots < ARMOR_TYPE_SLOTS[stateArmorType]) {
                invalidReason = "Unit is using more total slots than are available.";
                return false;
            }
        }

        // Check to make sure no locations breach weapon limits.
        // On quads, it can have a total of 4 weapons.
        // On bipeds, it's one anti-'Mech + one anti-personnel or two
        // anti-personnel per arm, plus two anti-'Mech and two anti-personnel
        // in the torso.
        if (stateChassisType == BattleArmor.CHASSIS_TYPE_QUAD) {
            // We're only going to be using torso stuff here.
            // Quads only have one location!
            if (torsoEquipment != null) {
                int totalWeapons = 0;
                Enumeration<BattleArmorEquipment> tmpE = torsoEquipment
                        .elements();
                while (tmpE.hasMoreElements()) {
                    BattleArmorEquipment tmpBAE = (tmpE.nextElement());
                    if ((tmpBAE.internalType == EQUIPMENT_TYPE_WEAPON)
                            || (tmpBAE.internalType == EQUIPMENT_TYPE_WEAPON_AP)) {
                        totalWeapons++;
                    }
                }
                if (totalWeapons > 4) {
                    invalidReason = "Unit has more weapons than it is allowed (limit of 4).";
                    return false;
                }
            }
        } else {
            if (torsoEquipment != null) {
                int totalAPWeapons = 0;
                int totalAMWeapons = 0;
                Enumeration<BattleArmorEquipment> tmpE = torsoEquipment
                        .elements();
                while (tmpE.hasMoreElements()) {
                    BattleArmorEquipment tmpBAE = (tmpE.nextElement());
                    if (tmpBAE.internalType == EQUIPMENT_TYPE_WEAPON) {
                        totalAMWeapons++;
                    } else if (tmpBAE.internalType == EQUIPMENT_TYPE_WEAPON_AP) {
                        totalAPWeapons++;
                    }
                }
                if (totalAMWeapons > 2) {
                    invalidReason = "Unit has more anti-'Mech weapons than it is allowed in its torso (limit of 2).";
                    return false;
                }
                if (totalAPWeapons > 2) {
                    invalidReason = "Unit has more anti-personnel weapons than it is allowed in its torso (limit of 2).";
                    return false;
                }
            }
            if (rightArmEquipment != null) {
                int totalWeapons = 0;
                int totalAMWeapons = 0;
                Enumeration<BattleArmorEquipment> tmpE = rightArmEquipment
                        .elements();
                while (tmpE.hasMoreElements()) {
                    BattleArmorEquipment tmpBAE = (tmpE.nextElement());
                    if (tmpBAE.internalType == EQUIPMENT_TYPE_WEAPON) {
                        totalWeapons++;
                        totalAMWeapons++;
                    } else if (tmpBAE.internalType == EQUIPMENT_TYPE_WEAPON_AP) {
                        totalWeapons++;
                    }
                }
                if (totalAMWeapons > 1) {
                    invalidReason = "Unit has more anti-'Mech weapons than it is allowed in its right arm (limit of 1).";
                    return false;
                } else if (totalWeapons > 2) {
                    invalidReason = "Unit has more weapons than it is allowed in its right arm (limit of 2).";
                    return false;
                }
            }
            if (leftArmEquipment != null) {
                int totalWeapons = 0;
                int totalAMWeapons = 0;
                Enumeration<BattleArmorEquipment> tmpE = leftArmEquipment
                        .elements();
                while (tmpE.hasMoreElements()) {
                    BattleArmorEquipment tmpBAE = (tmpE.nextElement());
                    if (tmpBAE.internalType == EQUIPMENT_TYPE_WEAPON) {
                        totalWeapons++;
                        totalAMWeapons++;
                    } else if (tmpBAE.internalType == EQUIPMENT_TYPE_WEAPON_AP) {
                        totalWeapons++;
                    }
                }
                if (totalAMWeapons > 1) {
                    invalidReason = "Unit has more anti-'Mech weapons than it is allowed in its left arm (limit of 1).";
                    return false;
                } else if (totalWeapons > 2) {
                    invalidReason = "Unit has more weapons than it is allowed in its left arm (limit of 2).";
                    return false;
                }
            }
        }

        // If it hasn't failed on any specific point, then return true.
        return true;
    }

    public int getChassisWeight() {
        return getChassisWeight(stateWeightClass, stateTechBase);
    }

    public static int getChassisWeight(int weightClass, int techBase) {
        if (techBase == TECH_BASE_IS) {
            // Inner Sphere tech base.
            switch (weightClass) {
                case WEIGHT_CLASS_PAL:
                    return 80;
                case WEIGHT_CLASS_LIGHT:
                    return 100;
                case WEIGHT_CLASS_MEDIUM:
                    return 175;
                case WEIGHT_CLASS_HEAVY:
                    return 300;
                case WEIGHT_CLASS_ASSAULT:
                    return 550;
            }
        } else {
            // Clan tech base
            switch (weightClass) {
                case WEIGHT_CLASS_PAL:
                    return 130;
                case WEIGHT_CLASS_LIGHT:
                    return 150;
                case WEIGHT_CLASS_MEDIUM:
                    return 250;
                case WEIGHT_CLASS_HEAVY:
                    return 400;
                case WEIGHT_CLASS_ASSAULT:
                    return 700;
            }
        }
        // This is an error case...
        return 0;
    }

    public int getArmorWeight() {
        return getArmorWeight(stateTechBase, stateArmorType, stateArmorValue);
    }

    public static int getArmorWeight(int techBase, int armorType, int armorValue) {
        return armorValue * ARMOR_TYPE_WEIGHT[techBase][armorType];
    }

    public int getGroundMPWeight() {
        return getGroundMPWeight(stateChassisType, stateWeightClass,
                stateGroundMP);
    }

    public static int getGroundMPWeight(int chassisType, int weightClass,
            int groundMP) {
        return (groundMP - (chassisType == BattleArmor.CHASSIS_TYPE_BIPED ? 1 : 2))
                * GROUND_MP_WEIGHT[weightClass];
    }

    public int getJumpMPWeight() {
        return getJumpMPWeight(stateJumpType, stateWeightClass, stateJumpMP);
    }

    public static int getJumpMPWeight(int jumpStatus, int weightClass,
            int jumpMP) {
        return jumpMP * JUMP_MP_WEIGHT[jumpStatus][weightClass];
    }

    public int getManipulatorWeight() {
        return MANIPULATOR_TYPE_WEIGHT[stateManipulatorTypeLeft]
                + MANIPULATOR_TYPE_WEIGHT[stateManipulatorTypeRight];
    }

    public int calcSuitBV() {
        // Defensive BV Value...
        // Starts at 1.5, for some reason.
        float dBV = 1.5f;

        // Add armor defensive BV value
        // This is armor value times armor BV.
        // Currently, all armor is BV 2 except fire resistant.
        dBV += (stateArmorValue * (stateArmorType == 7 ? 3 : 2));

        // Add 1 if the suit mounts ECM.
        // FIXME
        // if (???HAS ECM???)
        // dBV += 1;

        // Add devensive movement/stealth factor
        // First, find its highest possible movement mod.
        int groundMod = 0;
        if (stateGroundMP >= 5) {
            groundMod = 2;
        } else if (stateGroundMP >= 3) {
            groundMod = 1;
        }
        int jumpMod = 0;
        if (getTotalJumpMP() >= 7) {
            jumpMod = 3;
        } else if (getTotalJumpMP() >= 5) {
            jumpMod = 2;
        } else if (getTotalJumpMP() >= 3) {
            jumpMod = 1;
        }
        if (stateJumpType != JUMP_TYPE_UMU) {
            jumpMod += 1;
        }
        float bestMod = (groundMod > jumpMod ? groundMod : jumpMod);
        // Next, add any potential stealth bonus from armor
        if ((stateArmorType == 8) // Mimetic Armor
                || (stateArmorType == 6)) {
            bestMod += 3;
        } else if ((stateArmorType == 5) // Standard Stealth Armor
                || (stateArmorType == 4) // Prototype Stealth Armor
                || (stateArmorType == 3)) {
            bestMod += 2;
        }
        // If it has a camo system but not mimetic...
        // It gets a bonus for that too.
        // This CAN combine with any stealth armor; just not mimetic.
        // FIXME
        // if ((stateArmorType != 8)
        // && (???HAS CAMO???))
        // bestMod += 2;
        // This max movement mod is then used to find the Defensive BV
        // multiplier.
        bestMod /= 10;
        bestMod += 1;
        dBV *= bestMod;

        float oBV = 0;
        // Now, on to offensive BV value!
        // Add the BV for all direct-fire weapons
        // FIXME

        // Then add all missile weapon BV
        // FIXME

        // Then add anti-'Mech attack BV
        if (canDoAntiMech()) {
            // FIXME
        }

        // Then add anti-personnel weapon BV, maybe
        // FIXME

        // Then add squad support weapon BV, maybe
        // FIXME

        // Add the BV for any other equipment
        // FIXME

        // Modify by the BA's speed factor...
        int speedFactor = Math.max(stateGroundMP, getTotalJumpMP());
        float speedFactorMult = 0;
        switch (speedFactor) {
            case 1:
                speedFactorMult = 0.54f;
                break;
            case 2:
                speedFactorMult = 0.65f;
                break;
            case 3:
                speedFactorMult = 0.77f;
                break;
            case 4:
                speedFactorMult = 0.88f;
                break;
            case 5:
                speedFactorMult = 1f;
                break;
            case 6:
                speedFactorMult = 1.12f;
                break;
            case 7:
                speedFactorMult = 1.24f;
                break;
            case 8:
                speedFactorMult = 1.37f;
                break;
            case 9:
                speedFactorMult = 1.5f;
                break;
            case 10:
                speedFactorMult = 1.63f;
                break;
            case 11:
                speedFactorMult = 1.76f;
                break;
            case 12:
                speedFactorMult = 1.89f;
                break;
        }
        oBV *= speedFactorMult;

        int retVal = Math.round(dBV + oBV);

        return retVal;
    }

    public int calcSquadBV() {
        return calcSuitBV() * stateMenPerSquad;
    }

    public int calcSuitCost() {
        float retVal = 0;

        // Chassis Cost
        switch (stateWeightClass) {
            case WEIGHT_CLASS_PAL:
            case WEIGHT_CLASS_LIGHT:
                // They're both 50,000 C-Bill chassis
                retVal += 50000;
                break;
            case WEIGHT_CLASS_MEDIUM:
                retVal += 100000;
                break;
            case WEIGHT_CLASS_HEAVY:
                retVal += 200000;
                break;
            case WEIGHT_CLASS_ASSAULT:
                retVal += 400000;
                break;
        }

        // Motice Systems Cost
        // First, ground MP: currently all 25,000 per MP
        retVal += (stateGroundMP - (stateChassisType == BattleArmor.CHASSIS_TYPE_BIPED ? 1
                : 2)) * 25000;
        // Second, Jump/VTOL/UMU movement.
        // Varies by weight.
        retVal += stateJumpMP * JUMP_MP_COST[stateJumpType][stateWeightClass];

        // Armor Cost
        retVal += (ARMOR_TYPE_COSTS[stateArmorType] * stateArmorValue);

        // Manipulators
        retVal += MANIPULATOR_TYPE_COSTS[stateManipulatorTypeLeft];
        retVal += MANIPULATOR_TYPE_COSTS[stateManipulatorTypeRight];

        // Technology Modifier
        if (stateTechBase == TECH_BASE_CLAN) {
            retVal *= 1.1;
        }

        // Weapons and Equipment Cost
        // FIXME
        // Not implemented yet...

        // Trooper Training Cost
        if (stateTechBase == TECH_BASE_IS) {
            retVal += 150000;
        } else {
            retVal += 200000;
        }

        return Math.round(retVal);
    }

    public int calcSquadCost() {
        return calcSuitCost() * stateMenPerSquad;
    }

    public BattleArmor getEntity() {
        BattleArmor retVal = new BattleArmor();

        // Set the name.
        retVal.setChassis(m_tfBAName.getText().trim());
        retVal.setModel("");

        retVal.setWeightClass(stateWeightClass);
        retVal.setChassisType(stateChassisType);

        // Set the weight (number of troops), and then initialize the armor.
        retVal.setTroopers(stateMenPerSquad);
        retVal.refreshLocations();
        retVal.autoSetInternal();
        retVal.setArmorType(stateArmorType);
        for (int x = 1; x < retVal.locations(); x++) {
            retVal.initializeArmor(stateArmorValue, x);
        }

        // Set the tech base.
        if (stateTechBase == TECH_BASE_IS) {
            retVal.setTechLevel(TechConstants.T_IS_TW_NON_BOX);
        } else {
            retVal.setTechLevel(TechConstants.T_CLAN_TW);
        }

        // Set the ground movement.
        retVal.setOriginalWalkMP(stateGroundMP);

        // Set the jump movement.
        retVal.setOriginalJumpMP(getTotalJumpMP());

        // Set the movement mode.
        if (stateJumpType == JUMP_TYPE_VTOL) {
            retVal.setMovementMode(EntityMovementMode.VTOL);
        } else if (stateJumpType == JUMP_TYPE_UMU) {
            retVal.setMovementMode(EntityMovementMode.INF_UMU);
            retVal.setOriginalWalkMP(getTotalJumpMP());
            retVal.setOriginalJumpMP(0);
        } else if (getTotalJumpMP() > 0) {
            retVal.setMovementMode(EntityMovementMode.INF_JUMP);
        } else {
            retVal.setMovementMode(EntityMovementMode.INF_LEG);
        }

        // And set its cost.
        retVal.setCost(calcSquadCost());

        try {
            if (stateArmorType == 7) { // Fire-resistant Armor
                retVal.addEquipment(EquipmentType
                        .get("BA-Fire Resistant Armor"),
                        BattleArmor.LOC_SQUAD);
            }

            // If it's capable of anti-'Mech attacks...
            if (canDoAntiMech()) {
                retVal.addEquipment(EquipmentType.get(Infantry.LEG_ATTACK),
                        BattleArmor.LOC_SQUAD);
                retVal.addEquipment(EquipmentType.get(Infantry.SWARM_MEK),
                        BattleArmor.LOC_SQUAD);
                retVal.addEquipment(EquipmentType.get(Infantry.STOP_SWARM),
                        BattleArmor.LOC_SQUAD);

                // Don't forget magnetic claws that give a bonus, like
                // Salamanders!
                if (stateManipulatorTypeLeft == BattleArmor.MANIPULATOR_BATTLE_MAGNET) {
                    // We only check one, because they can only be added in
                    // pairs...
                    retVal.addEquipment(EquipmentType
                            .get("BABattleClawMagnets"),
                            BattleArmor.LOC_SQUAD);
                }
            }

            // Lets add vibro-claws!
            if (((stateManipulatorTypeLeft == BattleArmor.MANIPULATOR_BATTLE_VIBRO) || (stateManipulatorTypeLeft == BattleArmor.MANIPULATOR_HEAVY_BATTLE_VIBRO))
                    && ((stateManipulatorTypeRight == BattleArmor.MANIPULATOR_BATTLE_VIBRO) || (stateManipulatorTypeRight == BattleArmor.MANIPULATOR_HEAVY_BATTLE_VIBRO))) {
                // BA-Vibro Claws (2)
                retVal.addEquipment(EquipmentType.get("BA-Vibro Claws (2)"),
                        BattleArmor.LOC_SQUAD);
            } else if (((stateManipulatorTypeLeft == BattleArmor.MANIPULATOR_BATTLE_VIBRO) || (stateManipulatorTypeLeft == BattleArmor.MANIPULATOR_HEAVY_BATTLE_VIBRO))
                    || ((stateManipulatorTypeRight == BattleArmor.MANIPULATOR_BATTLE_VIBRO) || (stateManipulatorTypeRight == BattleArmor.MANIPULATOR_HEAVY_BATTLE_VIBRO))) {
                // BA-Vibro Claws (1)
                retVal.addEquipment(EquipmentType.get("BA-Vibro Claws (1)"),
                        BattleArmor.LOC_SQUAD);
            }

            /*
             * superceded by newer code
             */
            /*
            if (canMountMech()) {
                // Needs to be able to ride 'Mechs.
                retVal.addEquipment(EquipmentType
                        .get(BattleArmor.BOARDING_CLAW), BattleArmor.LOC_SQUAD);
            }
            */

            // Equipment and stuff needs to be set!
            // Now all other equipment.
            if (leftArmEquipment != null) {
                Enumeration<BattleArmorEquipment> tmpE = leftArmEquipment
                        .elements();
                while (tmpE.hasMoreElements()) {
                    BattleArmorEquipment tmpBAE = (tmpE.nextElement());
                    if ((tmpBAE.internalType == EQUIPMENT_TYPE_WEAPON)
                            || (tmpBAE.internalType == EQUIPMENT_TYPE_AMMO)) {
                        retVal.addEquipment(EquipmentType
                                .get(tmpBAE.weaponTypeName),
                                BattleArmor.LOC_SQUAD);
                    } else if (tmpBAE.internalType == EQUIPMENT_TYPE_OTHER) {
                        // FIXME
                    }
                    // EQUIPMENT_TYPE_PREPROCESS, by definition, should already
                    // have been handled.
                }
            }
            if (rightArmEquipment != null) {
                Enumeration<BattleArmorEquipment> tmpE = rightArmEquipment
                        .elements();
                while (tmpE.hasMoreElements()) {
                    BattleArmorEquipment tmpBAE = (tmpE.nextElement());
                    if ((tmpBAE.internalType == EQUIPMENT_TYPE_WEAPON)
                            || (tmpBAE.internalType == EQUIPMENT_TYPE_AMMO)) {
                        retVal.addEquipment(EquipmentType
                                .get(tmpBAE.weaponTypeName),
                                BattleArmor.LOC_SQUAD);
                    } else if (tmpBAE.internalType == EQUIPMENT_TYPE_OTHER) {
                        // FIXME
                    }
                    // EQUIPMENT_TYPE_PREPROCESS, by definition, should already
                    // have been handled.
                }
            }
            if (torsoEquipment != null) {
                Enumeration<BattleArmorEquipment> tmpE = torsoEquipment
                        .elements();
                while (tmpE.hasMoreElements()) {
                    BattleArmorEquipment tmpBAE = (tmpE.nextElement());
                    if ((tmpBAE.internalType == EQUIPMENT_TYPE_WEAPON)
                            || (tmpBAE.internalType == EQUIPMENT_TYPE_AMMO)) {
                        retVal.addEquipment(EquipmentType
                                .get(tmpBAE.weaponTypeName),
                                BattleArmor.LOC_SQUAD);
                    } else if (tmpBAE.internalType == EQUIPMENT_TYPE_OTHER) {
                        // FIXME
                    }
                    // EQUIPMENT_TYPE_PREPROCESS, by definition, should already
                    // have been handled.
                }
            }
        } catch (LocationFullException e) {
            System.err.println(e);
            e.printStackTrace();
        }

        return retVal;
    }

    public int getTotalJumpMP() {
        int retVal = stateJumpMP;

        // Add any jump MP bonus for equipment...
        // like partial wing or jump booster.
        if (torsoEquipment != null) {
            Enumeration<BattleArmorEquipment> tmpE = torsoEquipment.elements();
            while (tmpE.hasMoreElements()) {
                BattleArmorEquipment tmpBAE = (tmpE.nextElement());
                if (tmpBAE.hasConflictFlag(F_CONFLICT_JUMP_GEAR)) {
                    retVal++;
                }
            }
        }

        return retVal;
    }

    public boolean canDoAntiMech() {
        if (stateChassisType == BattleArmor.CHASSIS_TYPE_QUAD) {
            // Quads can never do anti-'Mech attacks.
            return false;
        }

        if (stateWeightClass >= WEIGHT_CLASS_HEAVY) {
            // Heavy and assault suits can never do anti-'Mech attacks.
            return false;
        }

        if ((stateJumpType == JUMP_TYPE_UMU) && (stateJumpMP > 0)) {
            // UMU-equipped BA cannot normally do anti-'Mech attacks.
            return false;
        }

        if (((stateManipulatorTypeLeft >= BattleArmor.MANIPULATOR_BATTLE) && (stateManipulatorTypeLeft <= BattleArmor.MANIPULATOR_HEAVY_BATTLE_VIBRO))
                || ((stateManipulatorTypeRight >= BattleArmor.MANIPULATOR_BATTLE) && (stateManipulatorTypeRight <= BattleArmor.MANIPULATOR_HEAVY_BATTLE_VIBRO))) {
            // A single battle claw or heavy battle claw allows anti-'Mech
            // attacks.
            return true;
        }

        if (((stateManipulatorTypeLeft >= BattleArmor.MANIPULATOR_BASIC) && (stateManipulatorTypeLeft <= BattleArmor.MANIPULATOR_BASIC_MINE_CLEARANCE))
                && ((stateManipulatorTypeRight >= BattleArmor.MANIPULATOR_BASIC) && (stateManipulatorTypeRight <= BattleArmor.MANIPULATOR_BASIC_MINE_CLEARANCE))) {
            // A pair of basic manipulators allow anti-'Mech attacks.
            return true;
        }

        if ((stateWeightClass <= WEIGHT_CLASS_LIGHT)
                && (stateManipulatorTypeLeft == BattleArmor.MANIPULATOR_ARMORED_GLOVE)
                && (stateManipulatorTypeRight == BattleArmor.MANIPULATOR_ARMORED_GLOVE)) {
            // For light BA and PA(L), two armored gloves allow anti-'Mech
            // attacks.
            return true;
        }

        // if not specifically allowed above, then return false.
        // Only a few cases allow it, so this is easier.
        return false;
    }

    public boolean canMountMech() {
        // Anything capable of leg/swarm attacks can also ride an OmniMech.
        if (canDoAntiMech()) {
            return true;
        }

        // Heavies can sometimes ride 'Mechs even when they can't do anti-'Mech
        // attacks.
        if (stateWeightClass == WEIGHT_CLASS_HEAVY) {
            if (((stateManipulatorTypeLeft >= BattleArmor.MANIPULATOR_BASIC) && (stateManipulatorTypeLeft <= BattleArmor.MANIPULATOR_BASIC_MINE_CLEARANCE))
                    && ((stateManipulatorTypeRight >= BattleArmor.MANIPULATOR_BASIC) && (stateManipulatorTypeRight <= BattleArmor.MANIPULATOR_BASIC_MINE_CLEARANCE))) {
                // A pair of basic manipulators allow anti-'Mech attacks.
                return true;
            }

            if ((stateWeightClass <= WEIGHT_CLASS_LIGHT)
                    && (stateManipulatorTypeLeft == BattleArmor.MANIPULATOR_ARMORED_GLOVE)
                    && (stateManipulatorTypeRight == BattleArmor.MANIPULATOR_ARMORED_GLOVE)) {
                // For light BA and PA(L), two armored gloves allow anti-'Mech
                // attacks.
                return true;
            }
        }

        // If not specifically allowed to...
        // Then it can't ride 'Mechs.
        return false;
    }

    protected class BattleArmorEquipment implements
            Comparable<BattleArmorEquipment> {
        // WeaponType/EquipmentType fields
        String name;
        String weaponTypeName;
        /*
         * int minimumRange = 0; int shortRange = 0; int mediumRange = 0; int
         * longRange = 0; int extremeRange = 0; int damage = 0; int
         * equipmentType = -1; int waterShortRange = 0; int waterMediumRange =
         * 0; int waterLongRange = 0; int waterExtremeRange = 0; int ammoType =
         * 0; int flags = 0; int rackSize = 0;
         */
        // Internal fields
        int weight = 0;
        int cost = 0;
        double bv = 0;
        int internalType = -1;
        int slots = 0;
        int techBase = -1;
        int conflictFlag = 0;
        int allowedLocation = 0;

        BattleArmorEquipment() {
            // Do nothing.
            // The default values are acceptable.
        }

        BattleArmorEquipment(String inN, String inWTN, int inW, int inC,
                double inBV, int inIT, int inS, int inTB, int inAL) {
            this(inN, inWTN, inW, inC, inBV, inIT, inS, inTB, inAL, 0);
        }

        BattleArmorEquipment(String inN, String inWTN, int inW, int inC,
                double inBV, int inIT, int inS, int inTB, int inAL, int inCF) {
            name = inN;
            weaponTypeName = inWTN;
            weight = inW;
            cost = inC;
            bv = inBV;
            internalType = inIT;
            slots = inS;
            techBase = inTB;
            allowedLocation = inAL;
            conflictFlag = inCF;
            CustomBattleArmorDialog.equipmentTypes.add(this);
            CustomBattleArmorDialog.equipmentNames.add(name);
        }

        void initialize() {
            CustomBattleArmorDialog.equipmentTypes = new ArrayList<BattleArmorEquipment>();
            CustomBattleArmorDialog.equipmentNames = new ArrayList<String>();

            // Weapons
            new BattleArmorEquipment("IS Support Machine Gun",
                    "IS BA Machine Gun", 100, 5000, 5, EQUIPMENT_TYPE_WEAPON,
                    1, TECH_BASE_IS, LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("IS Light Recoilless Rifle",
                    "ISLight Recoilless Rifle", 175, 1000, 12,
                    EQUIPMENT_TYPE_WEAPON, 2, TECH_BASE_IS,
                    LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("IS Medium Recoilless Rifle",
                    "ISMedium Recoilless Rifle", 250, 3000, 19,
                    EQUIPMENT_TYPE_WEAPON, 2, TECH_BASE_IS,
                    LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("IS Heavy Recoilless Rifle",
                    "ISHeavy Recoilless Rifle", 325, 5000, 22,
                    EQUIPMENT_TYPE_WEAPON, 3, TECH_BASE_IS,
                    LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("IS Heavy Flamer", "ISFlamer", 150, 7500,
                    6, EQUIPMENT_TYPE_WEAPON, 1, TECH_BASE_IS,
                    LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("CL Support Machine Gun",
                    "CL BA Machine Gun", 100, 5000, 5, EQUIPMENT_TYPE_WEAPON,
                    1, TECH_BASE_CLAN, LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("CL Light Recoilless Rifle",
                    "CLLight Recoilless Rifle", 175, 1000, 12,
                    EQUIPMENT_TYPE_WEAPON, 2, TECH_BASE_CLAN,
                    LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("CL Medium Recoilless Rifle",
                    "CLMedium Recoilless Rifle", 250, 3000, 19,
                    EQUIPMENT_TYPE_WEAPON, 2, TECH_BASE_CLAN,
                    LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("CL Heavy Recoilless Rifle",
                    "CLHeavy Recoilless Rifle", 325, 5000, 22,
                    EQUIPMENT_TYPE_WEAPON, 3, TECH_BASE_CLAN,
                    LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("CL Heavy Flamer", "CLFlamer", 150, 7500,
                    6, EQUIPMENT_TYPE_WEAPON, 1, TECH_BASE_CLAN,
                    LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("CL Semi-Portable Support Pulse Laser",
                    "CLMicroPulseLaser", 160, 12500, 12, EQUIPMENT_TYPE_WEAPON,
                    1, TECH_BASE_CLAN, LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("IS Support Laser", "ISSmall Laser", 200,
                    11250, 9, EQUIPMENT_TYPE_WEAPON, 1, TECH_BASE_IS,
                    LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("CL Support Laser", "CLSmall Laser", 200,
                    11250, 9, EQUIPMENT_TYPE_WEAPON, 1, TECH_BASE_CLAN,
                    LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("CL ER Support Laser", "CLERSmallLaser",
                    250, 11250, 31, EQUIPMENT_TYPE_WEAPON, 2, TECH_BASE_CLAN,
                    LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("Bearhunter Superheavy AC",
                    "CLBearhunter Superheavy AC", 150, 11250, 4,
                    EQUIPMENT_TYPE_WEAPON, 2, TECH_BASE_CLAN,
                    LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("IS David Light Gauss Rifle",
                    "ISDavidLightGaussRifle", 100, 22500, 7,
                    EQUIPMENT_TYPE_WEAPON, 1, TECH_BASE_IS,
                    LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("IS King David Light Gauss Rifle",
                    "ISKingDavidLightGaussRifle", 275, 30000, 7,
                    EQUIPMENT_TYPE_WEAPON, 2, TECH_BASE_IS,
                    LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("IS Firedrake Support Needler",
                    "ISFiredrakeIncendiaryNeedler", 50, 1500, 2,
                    EQUIPMENT_TYPE_WEAPON, 1, TECH_BASE_IS,
                    LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("IS Man-Portable Plasma Rifle",
                    "ISBAPlasma Rifle", 300, 28000, 12, EQUIPMENT_TYPE_WEAPON,
                    2, TECH_BASE_IS, LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("IS Magshot Gauss Rifle", "ISBAMagshotGR",
                    175, 8500, 15, EQUIPMENT_TYPE_WEAPON, 3, TECH_BASE_IS,
                    LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("IS Grand Mauler Gauss Cannonr",
                    "ISGrandMauler", 125, 5500, 6, EQUIPMENT_TYPE_WEAPON, 2,
                    TECH_BASE_IS, LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("IS Tsunami Heavy Gauss Rifle",
                    "BA-ISTsunamiHeavyGaussRifle", 125, 5000, 6,
                    EQUIPMENT_TYPE_WEAPON, 2, TECH_BASE_IS,
                    LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("IS Semi-Portable Autocannon",
                    "ISBAHeavyMG", 150, 7500, 6, EQUIPMENT_TYPE_WEAPON, 1,
                    TECH_BASE_CLAN, LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("IS Semi-Portable Machine Gun",
                    "ISBALightMG", 75, 5000, 5, EQUIPMENT_TYPE_WEAPON, 1,
                    TECH_BASE_CLAN, LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("CL Semi-Portable Autocannon",
                    "CLBAHeavyMG", 150, 7500, 6, EQUIPMENT_TYPE_WEAPON, 1,
                    TECH_BASE_CLAN, LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("CL Semi-Portable Machine Gun",
                    "CLBALightMG", 75, 5000, 5, EQUIPMENT_TYPE_WEAPON, 1,
                    TECH_BASE_CLAN, LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("Ultra Heavy Support Laser", "BACLHeavyMediumLaser", 1000, 0, 0, EQUIPMENT_TYPE_WEAPON, 4, TECH_BASE_CLAN, LOCATION_ALLOWED_ANY); //FIXME: Cost and BV
            new BattleArmorEquipment("Bomb Rack", "BAMicroBomb", 100, 0, 0, EQUIPMENT_TYPE_WEAPON, 2, TECH_BASE_CLAN, LOCATION_ALLOWED_ANY); //FIXME: Cost and BV
            new BattleArmorEquipment("ER Semi-Portable Support Laser", "BACLERMicroLaser", 150, 0, 0, EQUIPMENT_TYPE_WEAPON, 1, TECH_BASE_CLAN, LOCATION_ALLOWED_ANY); //FIXME: Cost and BV
            new BattleArmorEquipment("Medium Pulse Laser", "BAISMediumPulseLaser", 800, 0, 0, EQUIPMENT_TYPE_WEAPON, 3, TECH_BASE_IS, LOCATION_ALLOWED_ANY); //FIXME: Cost and BV
            new BattleArmorEquipment("Grenade Launcher", "BAAutoGL", 75, 0, 0, EQUIPMENT_TYPE_WEAPON, 1, TECH_BASE_IS, LOCATION_ALLOWED_ANY); //FIXME: Cost and BV
            new BattleArmorEquipment("Compact NARC", "BACompactNARC", 150, 0, 0, EQUIPMENT_TYPE_WEAPON, 1, TECH_BASE_IS, LOCATION_ALLOWED_ANY); //FIXME: Cost and BV
            new BattleArmorEquipment("Medium Laser", "BAISMediumLaser", 500, 0, 0, EQUIPMENT_TYPE_WEAPON, 3, TECH_BASE_IS, LOCATION_ALLOWED_ANY); //FIXME: Cost and BV
            new BattleArmorEquipment("Popup Mine", "BAMineLauncher", 200, 0, 0, EQUIPMENT_TYPE_WEAPON, 1, TECH_BASE_IS, LOCATION_ALLOWED_ANY); //FIXME: Cost and BV
            new BattleArmorEquipment("Medium Pulse Laser", "BACLMediumPulseLaser", 800, 0, 0, EQUIPMENT_TYPE_WEAPON, 3, TECH_BASE_CLAN, LOCATION_ALLOWED_ANY); //FIXME: Cost and BV
            new BattleArmorEquipment("Semi-Portable Heavy Laser", "BACLHeavySmallLaser", 500, 0, 0, EQUIPMENT_TYPE_WEAPON, 3, TECH_BASE_IS, LOCATION_ALLOWED_ANY); //FIXME: Cost and BV
            new BattleArmorEquipment("ER Support Laser", "BAISERSmallLaser", 350, 0, 0, EQUIPMENT_TYPE_WEAPON, 2, TECH_BASE_IS, LOCATION_ALLOWED_ANY); //FIXME: Cost and BV
            new BattleArmorEquipment("Support Particle Cannon", "BASupportPPC", 240, 0, 0, EQUIPMENT_TYPE_WEAPON, 2, TECH_BASE_CLAN, LOCATION_ALLOWED_ANY); //FIXME: Cost and BV
            new BattleArmorEquipment("Support Particle Cannon", "BASupportPPC", 250, 0, 0, EQUIPMENT_TYPE_WEAPON, 2, TECH_BASE_IS, LOCATION_ALLOWED_ANY); //FIXME: Cost and BV
            new BattleArmorEquipment("ER Heavy Support Laser", "BACLERMediumLaser", 800, 0, 0, EQUIPMENT_TYPE_WEAPON, 3, TECH_BASE_CLAN, LOCATION_ALLOWED_ANY); //FIXME: Cost and BV
            new BattleArmorEquipment("Support Pulse Laser", "BACLSmallPulseLaser", 400, 0, 0, EQUIPMENT_TYPE_WEAPON, 1, TECH_BASE_CLAN, LOCATION_ALLOWED_ANY); //FIXME: Cost and BV
            new BattleArmorEquipment("Heavy Mortar", "BAISHeavyMortar", 400, 0, 0, EQUIPMENT_TYPE_WEAPON, 2, TECH_BASE_IS, LOCATION_ALLOWED_ANY); //FIXME: Cost and BV
            new BattleArmorEquipment("Light Mortar", "BSISLightMortar", 300, 0, 0, EQUIPMENT_TYPE_WEAPON, 2, TECH_BASE_IS, LOCATION_ALLOWED_ANY); //FIXME: Cost and BV
            new BattleArmorEquipment("Heavy Grenade Launcher", "BAMicroGrenade", 100, 0, 0, EQUIPMENT_TYPE_WEAPON, 1, TECH_BASE_BOTH, LOCATION_ALLOWED_ANY); //FIXME: Cost and B

            new BattleArmorEquipment("Advanced SRM 1 Launcher",
                    "Clan Advanced SRM-1", 60, 15000, 15,
                    EQUIPMENT_TYPE_WEAPON, 2, TECH_BASE_CLAN,
                    LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("Advanced SRM 2 Launcher",
                    "Clan Advanced SRM-2", 90, 30000, 30,
                    EQUIPMENT_TYPE_WEAPON, 2, TECH_BASE_CLAN,
                    LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("Advanced SRM 3 Launcher",
                    "Clan Advanced SRM-3", 120, 45000, 45,
                    EQUIPMENT_TYPE_WEAPON, 3, TECH_BASE_CLAN,
                    LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("Advanced SRM 4 Launcher",
                    "Clan Advanced SRM-4", 150, 60000, 60,
                    EQUIPMENT_TYPE_WEAPON, 3, TECH_BASE_CLAN,
                    LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("Advanced SRM 5 Launcher",
                    "Clan Advanced SRM-5", 180, 75000, 75,
                    EQUIPMENT_TYPE_WEAPON, 4, TECH_BASE_CLAN,
                    LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("Advanced SRM 6 Launcher",
                    "Clan Advanced SRM-6", 210, 90000, 90,
                    EQUIPMENT_TYPE_WEAPON, 4, TECH_BASE_CLAN,
                    LOCATION_ALLOWED_ANY);

            // Ammunition
            new BattleArmorEquipment("Advanced SRM 1 Ammo",
                    "BAAdvancedSRM1 Ammo", 10, 500, 0.02, EQUIPMENT_TYPE_AMMO,
                    1, TECH_BASE_CLAN, LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("Advanced SRM 2 Ammo",
                    "BAAdvancedSRM2 Ammo", 20, 1000, 0.08, EQUIPMENT_TYPE_AMMO,
                    1, TECH_BASE_CLAN, LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("Advanced SRM 3 Ammo",
                    "BAAdvancedSRM3 Ammo", 30, 1500, 0.18, EQUIPMENT_TYPE_AMMO,
                    1, TECH_BASE_CLAN, LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("Advanced SRM 4 Ammo",
                    "BAAdvancedSRM4 Ammo", 40, 2000, 0.32, EQUIPMENT_TYPE_AMMO,
                    1, TECH_BASE_CLAN, LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("Advanced SRM 5 Ammo",
                    "BAAdvancedSRM5 Ammo", 50, 2500, 0.5, EQUIPMENT_TYPE_AMMO,
                    1, TECH_BASE_CLAN, LOCATION_ALLOWED_ANY);
            new BattleArmorEquipment("Advanced SRM 6 Ammo",
                    "BAAdvancedSRM6 Ammo", 60, 3000, 0.72, EQUIPMENT_TYPE_AMMO,
                    1, TECH_BASE_CLAN, LOCATION_ALLOWED_ANY);

            // Equipment
            new BattleArmorEquipment("Jump Booster", null, 125, 75000, 0,
                    EQUIPMENT_TYPE_PREPROCESS, 2, TECH_BASE_BOTH,
                    LOCATION_ALLOWED_TORSO, F_CONFLICT_JUMP_GEAR);
            new BattleArmorEquipment("Partial Wing", null, 200, 50000, 0,
                    EQUIPMENT_TYPE_PREPROCESS, 1, TECH_BASE_IS,
                    LOCATION_ALLOWED_TORSO, F_CONFLICT_JUMP_GEAR);

            /*
             * More stuff to add! // Weapons createBASingleMG()
             * createBASingleFlamer() createBATwinFlamers() createBAInfernoSRM()
             * createBAMicroBomb() createBACLERMicroLaser()
             * createCLTorpedoLRM5() createBAISMediumPulseLaser()
             * createTwinSmallPulseLaser() createTripleSmallLaser()
             * createTripleMG() createFenrirSRM4() createBAAutoGL()
             * createBAISMediumLaser() createBAISERSmallLaser()
             * createBACompactNARC() createSlothSmallLaser()
             * createBAMineLauncher() createBACLMediumPulseLaser()
             * createBASingleSmallPulseLaser() createBASRM4()
             * createBASupportPPC() createPhalanxSRM4()
             * createBACLHeavyMediumLaser() createBACLHeavySmallLaser()
             * createBACLERMediumLaser() createBACLSmallPulseLaser()
             * createBAISLightMortar() createBAISHeavyMortar()
             * createBAMicroGrenade() createISLAWLauncher()
             * createISLAW2Launcher() createISLAW3Launcher()
             * createISLAW4Launcher() createISLAW5Launcher() createISMRM1()
             * createISMRM2() createISMRM3() createISMRM4() createISMRM5()
             * createLRM1() createLRM2() createLRM3() createLRM4() //Ammo
             * createBASRM2Ammo() createBASRM2OSAmmo() createBAInfernoSRMAmmo()
             * createBAMicroBombAmmo() createCLTorpedoLRM5Ammo()
             * createFenrirSRM4Ammo() createBACompactNarcAmmo()
             * createBAMineLauncherAmmo() createBALRM5Ammo()
             * createPhalanxSRM4Ammo() createGrenadierSRM4Ammo()
             * createBAInfernoSRMAmmo() createISLAWLauncherAmmo()
             * createISLAW2LauncherAmmo() createISLAW3LauncherAmmo()
             * createISLAW4LauncherAmmo() createISLAW5LauncherAmmo()
             * createISMRM1Ammo() createISMRM2Ammo() createISMRM3Ammo()
             * createISMRM4Ammo() createISMRM5Ammo() createBAISLRM1Ammo()
             * createBAISLRM2Ammo() createBAISLRM3Ammo() createBAISLRM4Ammo()
             * createBAISLRM5Ammo() createBACLLRM1Ammo() createBACLLRM2Ammo()
             * createBACLLRM3Ammo() createBACLLRM4Ammo() createBACLLRM5Ammo()
             * createBASRM1Ammo() createBASRM2Ammo() createBASRM3Ammo()
             * createBASRM4Ammo() createBASRM5Ammo() createBASRM6Ammo() // Other
             * equipment
             */
        }

        public String getDescription() {
            StringBuffer retVal = new StringBuffer("");
            retVal.append(name);
            retVal.append(" (");
            retVal.append(weight);
            retVal.append(" kg, ");
            retVal.append(slots);
            retVal.append(" slots)");

            return retVal.toString();
        }

        public int compareTo(BattleArmorEquipment o) {
            return name.compareTo(o.name);
        }

        public boolean hasConflictFlag(int testFlags) {
            return ((conflictFlag & testFlags) > 0);
        }
    }
}
