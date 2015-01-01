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

package megamek.client.ui.swing;

import java.awt.BorderLayout;
import java.awt.Checkbox;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.List;
import java.awt.Point;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import megamek.client.Client;
import megamek.client.ui.MechView;
import megamek.client.ui.Messages;
import megamek.client.ui.AWT.widget.BufferedPanel;
import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.EquipmentType;
import megamek.common.FighterSquadron;
import megamek.common.IEntityMovementMode;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.MechSummaryComparator;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.Tank;
import megamek.common.TechConstants;
import megamek.common.UnitType;
import megamek.common.WeaponType;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.options.PilotOptions;
import megamek.common.preference.IClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestEntity;
import megamek.common.verifier.TestMech;
import megamek.common.verifier.TestTank;

/*
 * Allows a user to sort through a list of MechSummaries and select one
 */

public class CustomFighterSquadronDialog
    extends JDialog implements ActionListener, ItemListener, KeyListener,
    Runnable, WindowListener
{
    /**
     *
     */
    private static final long serialVersionUID = -4269883152911154774L;

    // how long after a key is typed does a new search begin
    private final static int KEY_TIMEOUT = 1000;

    // these indices should match up with the static values in the MechSummaryComparator
    private String[] m_saSorts = { Messages.getString("MechSelectorDialog.0"), Messages.getString("MechSelectorDialog.1"), Messages.getString("MechSelectorDialog.2"), Messages.getString("MechSelectorDialog.3"), Messages.getString("MechSelectorDialog.4"), Messages.getString("MechSelectorDialog.5") }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

    private MechSummary[] m_mechsCurrent;
    private Client m_client;
    private ClientGUI m_clientgui;
    private UnitLoadingDialog unitLoadingDialog;

    private StringBuffer m_sbSearch = new StringBuffer();
    private long m_nLastSearch = 0;

    private JLabel m_labelWeightClass = new JLabel(Messages.getString("MechSelectorDialog.m_labelWeightClass"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JComboBox m_chWeightClass = new JComboBox();
    private JLabel m_labelType = new JLabel(Messages.getString("MechSelectorDialog.m_labelType"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JComboBox m_chType = new JComboBox();
    private JComboBox m_chUnitType = new JComboBox();
    private JLabel m_labelSort = new JLabel(Messages.getString("MechSelectorDialog.m_labelSort"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JComboBox m_chSort = new JComboBox();
    private JPanel m_pParams = new JPanel();
    private JPanel m_pListOptions = new JPanel();
    private JLabel m_labelListOptions = new JLabel(Messages.getString("MechSelectorDialog.m_labelListOptions"));
    private Checkbox m_cModel = new Checkbox(Messages.getString("MechSelectorDialog.m_cModel"), GUIPreferences.getInstance().getMechSelectorIncludeModel());
    private Checkbox m_cName = new Checkbox(Messages.getString("MechSelectorDialog.m_cName"), GUIPreferences.getInstance().getMechSelectorIncludeName());
    private Checkbox m_cTons = new Checkbox(Messages.getString("MechSelectorDialog.m_cTons"), GUIPreferences.getInstance().getMechSelectorIncludeTons());
    private Checkbox m_cBV = new Checkbox(Messages.getString("MechSelectorDialog.m_cBV"), GUIPreferences.getInstance().getMechSelectorIncludeBV());
    private Checkbox m_cYear = new Checkbox(Messages.getString("MechSelectorDialog.m_cYear"), GUIPreferences.getInstance().getMechSelectorIncludeYear());
    private Checkbox m_cLevel = new Checkbox(Messages.getString("MechSelectorDialog.m_cLevel"), GUIPreferences.getInstance().getMechSelectorIncludeLevel());
    private Checkbox m_cCost = new Checkbox(Messages.getString("MechSelectorDialog.m_cCost"), GUIPreferences.getInstance().getMechSelectorIncludeCost());

    private JButton butRemove = new JButton("<<"); //$NON-NLS-1$
    private JButton butAdd = new JButton(">>"); //$NON-NLS-1$

    private JPanel m_pOpenAdvanced = new JPanel();
    private JButton m_bToggleAdvanced = new JButton("< Advanced Search >");
    private JPanel m_pSouthParams = new JPanel();

    List m_mechList = new List(10);

    private List listFightersSelected = new List();
    private Vector<Aero> squadron = new Vector<Aero>();

    private JButton m_bPick = new JButton(Messages.getString("CustomFighterSquadronDialog.m_bPick")); //$NON-NLS-1$
    private JButton m_bCancel = new JButton(Messages.getString("Close")); //$NON-NLS-1$
    private JPanel m_pButtons = new JPanel();
    private JPanel m_pChooseButtons = new JPanel();

    private TextArea m_mechView = new TextArea("",36,35);
    private TextArea squadronView = new TextArea("",18,35);
    private JPanel m_pLeft = new JPanel();
    private JPanel m_pMiddle = new JPanel();


    private JComboBox m_cWalk = new JComboBox();
    private TextField m_tWalk = new TextField(2);
    private JComboBox m_cJump = new JComboBox();
    private TextField m_tJump = new TextField(2);
    private JComboBox m_cArmor = new JComboBox();
    private TextField m_tWeapons1 = new TextField(2);
    private JComboBox m_cWeapons1 = new JComboBox();
    private JComboBox m_cOrAnd = new JComboBox();
    private TextField m_tWeapons2 = new TextField(2);
    private JComboBox m_cWeapons2 = new JComboBox();
    private Checkbox m_chkEquipment = new Checkbox();
    private JComboBox m_cEquipment = new JComboBox();
    private TextField m_tStartYear = new TextField(4);
    private TextField m_tEndYear = new TextField(4);
    private JButton m_bSearch = new JButton(Messages.getString("MechSelectorDialog.Search.Search"));
    private JButton m_bReset = new JButton(Messages.getString("MechSelectorDialog.Search.Reset"));
    private JLabel m_lCount = new JLabel();

    private int m_count;
    private int m_old_nType;
    private int m_old_nUnitType;

    private JPanel m_pUpper = new JPanel();
    private JPanel m_pLower = new JPanel();
    BufferedPanel m_pPreview = new BufferedPanel();

    private JLabel m_labelPlayer = new JLabel(Messages.getString("MechSelectorDialog.m_labelPlayer"), SwingConstants.RIGHT); //$NON-NLS-1$
    private JComboBox m_chPlayer = new JComboBox();

    private boolean includeMaxTech;

    private EntityVerifier entityVerifier = new EntityVerifier(new File("data/mechfiles/UnitVerifierOptions.xml"));

    public CustomFighterSquadronDialog(ClientGUI cl, UnitLoadingDialog uld)
    {
        super(cl.frame, Messages.getString("CustomFighterSquadronDialog.title"), true); //$NON-NLS-1$
        m_client = cl.getClient();
        m_clientgui = cl;
        unitLoadingDialog = uld;

        for (String sort : m_saSorts) {
            m_chSort.addItem(sort);
        }
        updatePlayerChoice();

        m_pParams.setLayout(new GridLayout(3, 2));
        m_pParams.add(m_labelWeightClass);
        m_pParams.add(m_chWeightClass);
        m_pParams.add(m_labelType);
        m_pParams.add(m_chType);
        m_pParams.add(m_labelSort);
        m_pParams.add(m_chSort);

        m_pChooseButtons.setLayout(new GridLayout(6, 2));
        m_pChooseButtons.add(butAdd);
        m_pChooseButtons.add(butRemove);

        m_pListOptions.add(m_labelListOptions);
        m_cModel.addItemListener(this);
        m_pListOptions.add(m_cModel);
        m_cName.addItemListener(this);
        m_pListOptions.add(m_cName);
        m_cTons.addItemListener(this);
        m_pListOptions.add(m_cTons);
        m_cBV.addItemListener(this);
        m_pListOptions.add(m_cBV);
        m_cYear.addItemListener(this);
        m_pListOptions.add(m_cYear);
        m_cLevel.addItemListener(this);
        m_pListOptions.add(m_cLevel);
        m_cCost.addItemListener(this);
        m_pListOptions.add(m_cCost);

        if (GUIPreferences.getInstance().getMechSelectorShowAdvanced()) {
            buildSouthParams(true);
        } else {
            buildSouthParams(false);
        }

        m_pButtons.setLayout(new FlowLayout(FlowLayout.CENTER));
        m_pButtons.add(m_bPick);
        m_pButtons.add(m_bCancel);
        m_pButtons.add(m_labelPlayer);
        m_pButtons.add(m_chPlayer);

        m_pUpper.setLayout(new BorderLayout());
        m_pPreview.setPreferredSize(84, 72);
        m_pUpper.add(m_pParams, BorderLayout.WEST);
        m_pUpper.add(m_pPreview, BorderLayout.CENTER);
        m_pUpper.add(m_pSouthParams, BorderLayout.SOUTH);

        m_pLower.setLayout(new BorderLayout());
        m_mechList.setFont(new Font("Monospaced", Font.PLAIN, 12)); //$NON-NLS-1$
        m_mechList.addKeyListener(this);
        m_pLower.add(m_mechList, BorderLayout.CENTER);
        m_pLower.add(m_pButtons, BorderLayout.SOUTH);
        m_pLower.add(m_pChooseButtons,BorderLayout.EAST);

        m_pLeft.setLayout(new BorderLayout());
        m_pLeft.add(m_pUpper, BorderLayout.NORTH);
        m_pLeft.add(m_pLower, BorderLayout.CENTER);

        m_pMiddle.setLayout(new BorderLayout());
        m_pMiddle.add(listFightersSelected, BorderLayout.CENTER);
        squadronView.setFont(new Font("Monospaced", Font.PLAIN, 12)); //$NON-NLS-1$
        m_pMiddle.add(squadronView, BorderLayout.SOUTH);

        clearSquadPreview();

        setLayout(new BorderLayout());
        add(m_pLeft, BorderLayout.WEST);
        add(m_pMiddle, BorderLayout.CENTER);
        m_mechView.setFont(new Font("Monospaced", Font.PLAIN, 12)); //$NON-NLS-1$
        add(m_mechView, BorderLayout.EAST);

        clearMechPreview();

        populateChoices();

        listFightersSelected.addItemListener(this);
        listFightersSelected.addKeyListener(this);

        m_chWeightClass.addItemListener(this);
        m_chType.addItemListener(this);
        m_chUnitType.addItemListener(this);
        m_chSort.addItemListener(this);
        m_mechList.addItemListener(this);
        m_bPick.addActionListener(this);
        m_bCancel.addActionListener(this);
        m_bSearch.addActionListener(this);
        m_bReset.addActionListener(this);
        m_bToggleAdvanced.addActionListener(this);
        butAdd.addActionListener(this);
        butRemove.addActionListener(this);
        setSize(1100, 350);
        setLocation(computeDesiredLocation());
        addWindowListener(this);
        updateWidgetEnablements();
    }

    private void buildSouthParams(boolean showAdvanced) {
        if (showAdvanced) {
            m_bToggleAdvanced.setText(Messages
                    .getString("MechSelectorDialog.Search.Hide"));
            m_pOpenAdvanced.add(m_bToggleAdvanced);

            m_pSouthParams.setLayout(new GridLayout(11, 1));
            m_pSouthParams.add(m_pListOptions);
            m_pSouthParams.add(m_pOpenAdvanced);

            JPanel row1 = new JPanel();
            row1.setLayout(new FlowLayout(FlowLayout.LEFT));
            row1.add(new JLabel(Messages
                    .getString("MechSelectorDialog.Search.Walk")));
            row1.add(m_cWalk);
            row1.add(m_tWalk);
            m_pSouthParams.add(row1);

            JPanel row2 = new JPanel();
            row2.setLayout(new FlowLayout(FlowLayout.LEFT));
            row2.add(new JLabel(Messages
                    .getString("MechSelectorDialog.Search.Jump")));
            row2.add(m_cJump);
            row2.add(m_tJump);
            m_pSouthParams.add(row2);

            JPanel row3 = new JPanel();
            row3.setLayout(new FlowLayout(FlowLayout.LEFT));
            row3.add(new JLabel(Messages
                    .getString("MechSelectorDialog.Search.Armor")));
            row3.add(m_cArmor);
            m_pSouthParams.add(row3);

            JPanel row4 = new JPanel();
            row4.setLayout(new FlowLayout(FlowLayout.LEFT));
            row4.add(new JLabel(Messages
                    .getString("MechSelectorDialog.Search.Weapons")));
            m_pSouthParams.add(row4);

            JPanel row5 = new JPanel();
            row5.setLayout(new FlowLayout(FlowLayout.LEFT));
            row5.add(new JLabel(Messages
                    .getString("MechSelectorDialog.Search.WeaponsAtLeast")));
            row5.add(m_tWeapons1);
            row5.add(m_cWeapons1);
            m_pSouthParams.add(row5);

            JPanel row6 = new JPanel();
            row6.setLayout(new FlowLayout(FlowLayout.LEFT));
            row6.add(m_cOrAnd);
            row6.add(new JLabel(Messages
                    .getString("MechSelectorDialog.Search.WeaponsAtLeast")));
            row6.add(m_tWeapons2);
            row6.add(m_cWeapons2);
            m_pSouthParams.add(row6);

            JPanel row7 = new JPanel();
            row7.setLayout(new FlowLayout(FlowLayout.LEFT));
            row7.add(new JLabel(Messages
                    .getString("MechSelectorDialog.Search.Equipment")));
            row7.add(m_chkEquipment);
            row7.add(m_cEquipment);
            m_pSouthParams.add(row7);

            JPanel row8 = new JPanel();
            row8.setLayout(new FlowLayout(FlowLayout.LEFT));
            row8.add(new JLabel(Messages
                    .getString("MechSelectorDialog.Search.Year")));
            row8.add(m_tStartYear);
            row8.add(new JLabel("-"));
            row8.add(m_tEndYear);
            m_pSouthParams.add(row8);

            JPanel row9 = new JPanel();
            row9.add(m_bSearch);
            row9.add(m_bReset);
            row9.add(m_lCount);
            m_pSouthParams.add(row9);
        } else {
            m_bToggleAdvanced.setText(Messages
                    .getString("MechSelectorDialog.Search.Show"));
            m_pOpenAdvanced.add(m_bToggleAdvanced);

            m_pSouthParams.setLayout(new GridLayout(2, 1));
            m_pSouthParams.add(m_pListOptions);
            m_pSouthParams.add(m_pOpenAdvanced);
        }
    }

    private void toggleAdvanced() {
        m_pUpper.remove(m_pSouthParams);
        m_pSouthParams = new JPanel();
        if (GUIPreferences.getInstance().getMechSelectorShowAdvanced()) {
            buildSouthParams(false);
            GUIPreferences.getInstance().setMechSelectorShowAdvanced(false);
        } else {
            buildSouthParams(true);
            GUIPreferences.getInstance().setMechSelectorShowAdvanced(true);
        }
        m_pUpper.add(m_pSouthParams, BorderLayout.SOUTH);
        invalidate();
        pack();
    }

    private void updateTechChoice() {
        boolean maxTechOption = m_client.game.getOptions().booleanOption(
                "allow_advanced_units");
        int maxTech = (maxTechOption ? TechConstants.SIZE
                : TechConstants.SIZE_LEVEL_2);
        if (includeMaxTech == maxTechOption) {
            return;
        }
        includeMaxTech = maxTechOption;
        m_chType.removeAll();
        for (int i = 0; i < maxTech; i++) {
            m_chType.addItem(TechConstants.getLevelDisplayableName(i));
        }
    }

    private void updatePlayerChoice() {
        String lastChoice = (String)m_chPlayer.getSelectedItem();
        m_chPlayer.removeAll();
        m_chPlayer.setEnabled(true);
        m_chPlayer.addItem(m_clientgui.getClient().getName());
        for (Client client : m_clientgui.getBots().values()) {
         m_chPlayer.addItem(client.getName());
      }
        if (m_chPlayer.getItemCount() == 1) {
            m_chPlayer.setEnabled(false);
        } else {
            m_chPlayer.setSelectedItem(lastChoice);
        }
    }

    public void run() {
        // Loading mechs can take a while, so it will have its own thread.
        // This prevents the UI from freezing, and allows the
        // "Please wait..." dialog to behave properly on various Java VMs.
        filterMechs(false);
        m_mechList.invalidate(); // force re-layout of window
        pack();
        setLocation(computeDesiredLocation());

        unitLoadingDialog.setVisible(false);

        final Map<String, String> hFailedFiles = MechSummaryCache.getInstance()
                .getFailedFiles();
        if ((hFailedFiles != null) && (hFailedFiles.size() > 0)) {
            new UnitFailureDialog(m_clientgui.frame, hFailedFiles); // self-showing
                                                                    // dialog
        }
    }

    private void populateChoices() {

        for (int i = 0; i < EntityWeightClass.SIZE; i++) {
            m_chWeightClass.addItem(EntityWeightClass.getClassName(i));
        }
        m_chWeightClass.addItem(Messages.getString("MechSelectorDialog.All")); //$NON-NLS-1$
        m_chWeightClass.setSelectedIndex(0);

        includeMaxTech = m_client.game.getOptions().booleanOption(
                "allow_advanced_units");
        int maxTech = (includeMaxTech ? TechConstants.SIZE
                : TechConstants.SIZE_LEVEL_2);
        for (int i = 0; i < maxTech; i++) {
            m_chType.addItem(TechConstants.getLevelDisplayableName(i));
        }
        m_chType.addItem(Messages.getString("MechSelectorDialog.ISAll"));
        // //$NON-NLS-1$
        m_chType.addItem(Messages.getString("MechSelectorDialog.ISAndClan"));
        // //$NON-NLS-1$
        // More than 8 items causes the drop down to sprout a vertical
        // scroll bar. I guess we'll sacrifice this next one to stay
        // under the limit. Stupid AWT JComboBox class!
        m_chType.addItem("Mixed All");
        m_chType.addItem(Messages.getString("MechSelectorDialog.All"));
        // //$NON-NLS-1$
        m_chType.setSelectedIndex(0);

        for (int i = 0; i < UnitType.SIZE; i++) {
            m_chUnitType.addItem(UnitType.getTypeDisplayableName(i));
        }
        m_chUnitType.addItem(Messages.getString("MechSelectorDialog.All")); //$NON-NLS-1$
        m_chUnitType.setSelectedIndex(0);

        m_cWalk
                .addItem(Messages
                        .getString("MechSelectorDialog.Search.AtLeast"));
        m_cWalk
                .addItem(Messages
                        .getString("MechSelectorDialog.Search.EqualTo"));
        m_cWalk.addItem(Messages
                .getString("MechSelectorDialog.Search.NoMoreThan"));
        m_cJump
                .addItem(Messages
                        .getString("MechSelectorDialog.Search.AtLeast"));
        m_cJump
                .addItem(Messages
                        .getString("MechSelectorDialog.Search.EqualTo"));
        m_cJump.addItem(Messages
                .getString("MechSelectorDialog.Search.NoMoreThan"));
        m_cArmor.addItem(Messages.getString("MechSelectorDialog.Search.Any"));
        m_cArmor.addItem(Messages
                .getString("MechSelectorDialog.Search.Armor25"));
        m_cArmor.addItem(Messages
                .getString("MechSelectorDialog.Search.Armor50"));
        m_cArmor.addItem(Messages
                .getString("MechSelectorDialog.Search.Armor75"));
        m_cArmor.addItem(Messages
                .getString("MechSelectorDialog.Search.Armor90"));
        m_cOrAnd.addItem(Messages.getString("MechSelectorDialog.Search.or"));
        m_cOrAnd.addItem(Messages.getString("MechSelectorDialog.Search.and"));
        populateWeaponsAndEquipmentChoices();
    }

    private void populateWeaponsAndEquipmentChoices() {
        LinkedHashSet<String> weapons = new LinkedHashSet<String>();
        LinkedHashSet<String> equipment = new LinkedHashSet<String>();
        m_cWeapons1.removeAll();
        m_cWeapons2.removeAll();
        m_cEquipment.removeAll();
        m_tWeapons1.setText("");
        m_tWeapons2.setText("");
        m_chkEquipment.setState(false);
        int nType = m_chType.getSelectedIndex();
        if (nType == -1) nType = 0;
        int nUnitType = m_chUnitType.getSelectedIndex();
        if (nUnitType == -1) nUnitType = 0;
        for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e
                .hasMoreElements();) {
            EquipmentType et = e.nextElement();
            if ((et instanceof WeaponType)
                    && ((et.getTechLevel() == nType)
                            || ((nType == TechConstants.T_TW_ALL) && ((et
                                    .getTechLevel() == TechConstants.T_INTRO_BOXSET)
                                    || (et.getTechLevel() == TechConstants.T_IS_TW_NON_BOX) || (et
                                    .getTechLevel() == TechConstants.T_CLAN_TW))) || (((nType == TechConstants.T_IS_TW_ALL) || (nType == TechConstants.T_IS_TW_NON_BOX)) && ((et
                            .getTechLevel() == TechConstants.T_INTRO_BOXSET) || (et
                            .getTechLevel() == TechConstants.T_IS_TW_NON_BOX))))) {
                if (!(nUnitType == UnitType.SIZE)
                        && ((UnitType.getTypeName(nUnitType).equals("Mek") || UnitType
                                .getTypeName(nUnitType).equals("Tank")) && (et
                                .hasFlag(WeaponType.F_INFANTRY)))) {
                    continue;
                }
                weapons.add(et.getName());
                if (et.hasFlag(WeaponType.F_C3M)
                        && ((nType == TechConstants.T_TW_ALL)
                                || (nType == TechConstants.T_IS_TW_NON_BOX) || (nType == TechConstants.T_IS_TW_ALL))) {
                    equipment.add(et.getName());
                }
            }
            if ((et instanceof MiscType)
                    && ((et.getTechLevel() == nType)
                            || ((nType == TechConstants.T_TW_ALL) && ((et
                                    .getTechLevel() == TechConstants.T_INTRO_BOXSET)
                                    || (et.getTechLevel() == TechConstants.T_IS_TW_NON_BOX) || (et
                                    .getTechLevel() == TechConstants.T_CLAN_TW))) || (((nType == TechConstants.T_IS_TW_ALL) || (nType == TechConstants.T_IS_TW_NON_BOX)) && ((et
                            .getTechLevel() == TechConstants.T_INTRO_BOXSET) || (et
                            .getTechLevel() == TechConstants.T_IS_TW_NON_BOX))))) {
                equipment.add(et.getName());
            }
        }
        for (String weaponName : weapons) {
            m_cWeapons1.addItem(weaponName);
            m_cWeapons2.addItem(weaponName);
        }
        for (String equipName : equipment) {
            m_cEquipment.addItem(equipName);
        }
        m_cWeapons1.invalidate();
        m_cWeapons2.invalidate();
        m_cEquipment.invalidate();
        pack();
    }

    private void filterMechs(boolean calledByAdvancedSearch) {
        Vector<MechSummary> vMechs = new Vector<MechSummary>();
        int nClass = m_chWeightClass.getSelectedIndex();
        int nType = m_chType.getSelectedIndex();
        int nUnitType = m_chUnitType.getSelectedIndex();
        MechSummary[] mechs = MechSummaryCache.getInstance().getAllMechs();
        if (mechs == null) {
            System.err.println("No units to filter!"); //$NON-NLS-1$
            return;
        }
        for (MechSummary mech : mechs) {
            if ( /* Weight */
            ((nClass == EntityWeightClass.SIZE) || (mech.getWeightClass() == nClass))
                    && /* Technology Level */
                    ((nType == TechConstants.T_ALL)
                            || (nType == mech.getType())
                            || ((nType == TechConstants.T_TW_ALL) && ((mech
                                    .getType() == TechConstants.T_INTRO_BOXSET)
                                    || (mech.getType() == TechConstants.T_IS_TW_NON_BOX) || (mech
                                    .getType() == TechConstants.T_CLAN_TW))) || ((nType == TechConstants.T_IS_TW_ALL) && ((mech
                            .getType() == TechConstants.T_INTRO_BOXSET) || (mech
                            .getType() == TechConstants.T_IS_TW_NON_BOX))))
                    && /* Unit Type (Mek, Infantry, etc.) */
                    ((nUnitType == UnitType.SIZE) || mech.getUnitType()
                            .equals(UnitType.getTypeName(nUnitType)))
                    && /* canon required */(!m_client.game.getOptions()
                            .booleanOption("canon_only") || mech.isCanon())) {
                vMechs.addElement(mech);
            }
        }
        m_mechsCurrent = new MechSummary[vMechs.size()];
        vMechs.copyInto(m_mechsCurrent);
        m_count = vMechs.size();
        if (!calledByAdvancedSearch
                && ((m_old_nType != nType) || (m_old_nUnitType != nUnitType))) {
            populateWeaponsAndEquipmentChoices();
        }
        m_old_nType = nType;
        m_old_nUnitType = nUnitType;
        sortMechs();
    }

    private void sortMechs() {
        Arrays.sort(m_mechsCurrent, new MechSummaryComparator(m_chSort
                .getSelectedIndex()));
        m_mechList.removeAll();
        try {
            m_mechList.setEnabled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            for (MechSummary element : m_mechsCurrent) {
                m_mechList.add(formatMech(element));
            }
        } finally {
            setCursor(Cursor.getDefaultCursor());
            m_mechList.setEnabled(true);
            // workaround for bug 1263380
            m_mechList.setFont(m_mechList.getFont());
        }
        updateWidgetEnablements();
        m_lCount.setText(m_mechsCurrent.length + "/" + m_count);
        repaint();
    }

    private void searchFor(String search) {
        for (int i = 0; i < m_mechsCurrent.length; i++) {
            if (m_mechsCurrent[i].getName().toLowerCase().startsWith(search)) {
                m_mechList.select(i);
                ItemEvent event = new ItemEvent(m_mechList,
                        ItemEvent.ITEM_STATE_CHANGED, m_mechList,
                        ItemEvent.SELECTED);
                itemStateChanged(event);
                break;
            }
        }
    }

    private void advancedSearch() {
        String s = m_lCount.getText();
        int first = Integer.parseInt(s.substring(0, s.indexOf('/')));
        int second = Integer.parseInt(s.substring(s.indexOf('/') + 1));
        if (first != second) {
            // Search already active, reset list before starting new one.
            filterMechs(true);
        }

        Vector<MechSummary> vMatches = new Vector<MechSummary>();
        for (MechSummary ms : m_mechsCurrent) {
            try {
                Entity entity = new MechFileParser(ms.getSourceFile(), ms
                        .getEntryName()).getEntity();
                if (isMatch(entity)) {
                    vMatches.addElement(ms);
                }
            } catch (EntityLoadingException ex) {
                // do nothing, I guess
            }
        }
        m_mechsCurrent = new MechSummary[vMatches.size()];
        vMatches.copyInto(m_mechsCurrent);
        clearMechPreview();
        sortMechs();
    }

    private boolean isMatch(Entity entity) {
        int walk = -1;
        try {
            walk = Integer.parseInt(m_tWalk.getText());
        } catch (NumberFormatException ne) {
        }
        if (walk > -1) {
            if (m_cWalk.getSelectedIndex() == 0) { // at least
                if (entity.getWalkMP() < walk) {
                    return false;
                }
            } else if (m_cWalk.getSelectedIndex() == 1) { // equal to
                if (walk != entity.getWalkMP()) {
                    return false;
                }
            } else if (m_cWalk.getSelectedIndex() == 2) { // not more than
                if (entity.getWalkMP() > walk) {
                    return false;
                }
            }
        }

        int jump = -1;
        try {
            jump = Integer.parseInt(m_tJump.getText());
        } catch (NumberFormatException ne) {
        }
        if (jump > -1) {
            if (m_cJump.getSelectedIndex() == 0) { // at least
                if (entity.getJumpMP() < jump) {
                    return false;
                }
            } else if (m_cJump.getSelectedIndex() == 1) { // equal to
                if (jump != entity.getJumpMP()) {
                    return false;
                }
            } else if (m_cJump.getSelectedIndex() == 2) { // not more than
                if (entity.getJumpMP() > jump) {
                    return false;
                }
            }
        }

        int sel = m_cArmor.getSelectedIndex();
        if (sel > 0) {
            int armor = entity.getTotalArmor();
            int maxArmor = entity.getTotalInternal() * 2 + 3;
            if (sel == 1) {
                if (armor < (maxArmor * .25)) {
                    return false;
                }
            } else if (sel == 2) {
                if (armor < (maxArmor * .5)) {
                    return false;
                }
            } else if (sel == 3) {
                if (armor < (maxArmor * .75)) {
                    return false;
                }
            } else if (sel == 4) {
                if (armor < (maxArmor * .9)) {
                    return false;
                }
            }
        }

        boolean weaponLine1Active = false;
        boolean weaponLine2Active = false;
        boolean foundWeapon1 = false;
        boolean foundWeapon2 = false;

        int count = 0;
        int weapon1 = -1;
        try {
            weapon1 = Integer.parseInt(m_tWeapons1.getText());
        } catch (NumberFormatException ne) {
        }
        if (weapon1 > -1) {
            weaponLine1Active = true;
            for (int i = 0; i < entity.getWeaponList().size(); i++) {
                WeaponType wt = (WeaponType) (entity.getWeaponList().get(i))
                        .getType();
                if (wt.getName().equals(m_cWeapons1.getSelectedItem())) {
                    count++;
                }
            }
            if (count >= weapon1) {
                foundWeapon1 = true;
            }
        }

        count = 0;
        int weapon2 = -1;
        try {
            weapon2 = Integer.parseInt(m_tWeapons2.getText());
        } catch (NumberFormatException ne) {
        }
        if (weapon2 > -1) {
            weaponLine2Active = true;
            for (int i = 0; i < entity.getWeaponList().size(); i++) {
                WeaponType wt = (WeaponType) (entity.getWeaponList().get(i))
                        .getType();
                if (wt.getName().equals(m_cWeapons2.getSelectedItem())) {
                    count++;
                }
            }
            if (count >= weapon2) {
                foundWeapon2 = true;
            }
        }

        int startYear = Integer.MIN_VALUE;
        int endYear = Integer.MAX_VALUE;
        try {
            startYear = Integer.parseInt(m_tStartYear.getText());
        } catch (NumberFormatException ne) {
        }
        try {
            endYear = Integer.parseInt(m_tEndYear.getText());
        } catch (NumberFormatException ne) {
        }
        if ((entity.getYear() < startYear) || (entity.getYear() > endYear)) {
            return false;
        }

        if (weaponLine1Active && !weaponLine2Active && !foundWeapon1) {
            return false;
        }
        if (weaponLine2Active && !weaponLine1Active && !foundWeapon2) {
            return false;
        }
        if (weaponLine1Active && weaponLine2Active) {
            if (m_cOrAnd.getSelectedIndex() == 0 /* 0 is "or" choice */) {
                if (!foundWeapon1 && !foundWeapon2) {
                    return false;
                }
            } else { // "and" choice in effect
                if (!foundWeapon1 || !foundWeapon2) {
                    return false;
                }
            }
        }

        count = 0;
        if (m_chkEquipment.getState()) {
            for (Mounted m : entity.getEquipment()) {
                EquipmentType mt = m.getType();
                if (mt.getName().equals(m_cEquipment.getSelectedItem())) {
                    count++;
                }
            }
            if (count < 1) {
                return false;
            }
        }

        return true;
    }

    private void resetSearch() {
        m_cWalk.setSelectedIndex(0);
        m_tWalk.setText("");
        m_cJump.setSelectedIndex(0);
        m_tJump.setText("");
        m_cArmor.setSelectedIndex(0);
        m_tWeapons1.setText("");
        m_cWeapons1.setSelectedIndex(0);
        m_cOrAnd.setSelectedIndex(0);
        m_tWeapons2.setText("");
        m_cWeapons2.setSelectedIndex(0);
        m_chkEquipment.setState(false);
        m_cEquipment.setSelectedIndex(0);

        filterMechs(false);
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
        if (show) {
            updatePlayerChoice();
            updateTechChoice();
            setLocation(computeDesiredLocation());
        }
        super.setVisible(show);
    }

    private String formatMech(MechSummary ms) {
        String val = "";
        String levelOrValid;

        if (!ms.getLevel().equals("F")) {
            levelOrValid = TechConstants.T_SIMPLE_LEVEL[ms.getType()];
        } else {
            levelOrValid = "F";
        }
        if (GUIPreferences.getInstance().getMechSelectorIncludeModel()) {
            val += makeLength(ms.getModel(), 10) + " "; //$NON-NLS-1$
        }
        if (GUIPreferences.getInstance().getMechSelectorIncludeName()) {
            val += makeLength(ms.getChassis(), 20) + " "; //$NON-NLS-1$
        }
        if (GUIPreferences.getInstance().getMechSelectorIncludeTons()) {
            val += makeLength("" + ms.getTons(), 3) + " "; //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (GUIPreferences.getInstance().getMechSelectorIncludeBV()) {
            val += makeLength("" + ms.getBV(), 5) + " "; //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (GUIPreferences.getInstance().getMechSelectorIncludeYear()) {
            val += ms.getYear() + " ";
        }
        if (GUIPreferences.getInstance().getMechSelectorIncludeLevel()) {
            val += levelOrValid + " ";
        }
        if (GUIPreferences.getInstance().getMechSelectorIncludeCost()) {
            val += ms.getCost() + " ";
        }
        return val;
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == m_bCancel) {
            //clear squadron
            squadron.removeAllElements();
            //fs.fighters.removeAllElements();
            listFightersSelected.removeAll();
            setVisible(false);
        }
        else if (ae.getSource() == butAdd) {
            int x = m_mechList.getSelectedIndex();
            if (x == -1) {
                return;
            }
            MechSummary ms = m_mechsCurrent[m_mechList.getSelectedIndex()];
            try {
                Entity e = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
                //I need to add them to a list of entities, to eventually be processed
                listFightersSelected.add(e.getDisplayName());
                squadron.add((Aero)e);
                //fs.fighters.add(e);
            } catch (EntityLoadingException ex) {
                System.out.println("Unable to load mech: " + ms.getSourceFile() + ": " + ms.getEntryName() + ": " + ex.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                ex.printStackTrace();
                return;
            }
            //preview the squadron
            clearSquadPreview();
            FighterSquadron fs = new FighterSquadron();
            //fs.compileSquadron();
            previewSquad(fs);
            //if this hits the maximum squadron size then disable add button
            if(squadron.size() == FighterSquadron.MAX_SIZE) {
                butAdd.setEnabled(false);
            }
        }
        else if (ae.getSource() == butRemove) {
            int x = listFightersSelected.getSelectedIndex();
            if (x == -1) {
                return;
            }
            listFightersSelected.remove(x);
            squadron.remove(x);
            //fs.fighters.remove(x);
//          preview the squadron
            clearSquadPreview();
            FighterSquadron fs = new FighterSquadron();
            //fs.compileSquadron();
            previewSquad(fs);
            //make sure that this enables the add button
            butAdd.setEnabled(true);
        }
        else if (ae.getSource() == m_bPick) {
            if(squadron.size() <= 0) {
                return;
            }
            Client c = null;
            if (m_chPlayer.getSelectedIndex() > 0) {
                String name = (String)m_chPlayer.getSelectedItem();
                c = m_clientgui.getBots().get(name);
            }
            if (c == null) {
                c = m_client;
            }
            //compile the fighter squadron
            FighterSquadron fs = new FighterSquadron();
            //create a new fighter squadron entity
            //FighterSquadron chosen = fs;
            //fs.compileSquadron();
            autoSetSkills(fs);
            fs.setOwner(c.getLocalPlayer());
            c.sendAddEntity(fs);
            //clear the current squadron
            squadron.removeAllElements();
            //fs.fighters.removeAllElements();
            listFightersSelected.removeAll();
            setVisible(false);
        } else if (ae.getSource() == m_bSearch) {
            advancedSearch();
        } else if (ae.getSource() == m_bReset) {
            resetSearch();
        } else if (ae.getSource() == m_bToggleAdvanced) {
            toggleAdvanced();
        }
    }

    public void itemStateChanged(ItemEvent ie) {
        if (ie.getSource() == m_chSort) {
            clearMechPreview();
            sortMechs();
        }
        else if ((ie.getSource() == m_chWeightClass)
                 || (ie.getSource() == m_chType)
                 || (ie.getSource() == m_chUnitType)) {
            clearMechPreview();
            filterMechs(false);
        } else if (ie.getSource() == m_mechList) {
            updateWidgetEnablements();
            int selected = m_mechList.getSelectedIndex();
            if (selected == -1) {
                clearMechPreview();
                return;
            }
            MechSummary ms = m_mechsCurrent[selected];
            try {
                Entity entity = new MechFileParser(ms.getSourceFile(), ms
                        .getEntryName()).getEntity();
                previewMech(entity);
            } catch (EntityLoadingException ex) {
                System.out
                        .println("Unable to load mech: " + ms.getSourceFile() + ": " + ms.getEntryName() + ": " + ex.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                ex.printStackTrace();
                clearMechPreview();
                return;
            }
        } else if ((ie.getSource() == m_cModel) ||
                   (ie.getSource() == m_cName) ||
                   (ie.getSource() == m_cTons) ||
                   (ie.getSource() == m_cBV) ||
                   (ie.getSource() == m_cYear) ||
                   (ie.getSource() == m_cLevel) ||
                   (ie.getSource() == m_cCost)) {
            GUIPreferences.getInstance().setMechSelectorIncludeModel(m_cModel.getState());
            GUIPreferences.getInstance().setMechSelectorIncludeName(m_cName.getState());
            GUIPreferences.getInstance().setMechSelectorIncludeTons(m_cTons.getState());
            GUIPreferences.getInstance().setMechSelectorIncludeBV(m_cBV.getState());
            GUIPreferences.getInstance().setMechSelectorIncludeYear(m_cYear.getState());
            GUIPreferences.getInstance().setMechSelectorIncludeLevel(m_cLevel.getState());
            GUIPreferences.getInstance().setMechSelectorIncludeCost(m_cCost.getState());
            clearMechPreview();
            sortMechs(); // sorting has side-effect of repopulating list
            m_mechList.invalidate();  // force re-layout of window
            pack();
            setLocation(computeDesiredLocation());
        }
    }

    void clearMechPreview() {
        m_mechView.setEditable(false);
        m_mechView.setText(""); //$NON-NLS-1$

        // Remove preview image.
        if (MechSummaryCache.getInstance().isInitialized()) {
            m_pPreview.removeBgDrawers();
            m_pPreview.paint(m_pPreview.getGraphics());
        }
    }

    void clearSquadPreview() {
        squadronView.setEditable(false);
        squadronView.setText(""); //$NON-NLS-1$
    }

    void previewMech(Entity entity) {
        MechView mechView = new MechView(entity, m_client.game.getOptions().booleanOption("show_bay_detail"));
        m_mechView.setEditable(false);
        String readout = mechView.getMechReadout();
        StringBuffer sb = new StringBuffer(readout);
        m_mechView.setText(readout);
        if((entity instanceof Mech) || (entity instanceof Tank)) {
            TestEntity testEntity = null;
            if (entity instanceof Mech) {
                testEntity = new TestMech((Mech)entity, entityVerifier.mechOption, null);
            }
            if (entity instanceof Tank) {
                testEntity = new TestTank((Tank)entity, entityVerifier.tankOption, null);
            }
            if (!testEntity.correctEntity(sb, !m_clientgui.getClient().game.getOptions().booleanOption("is_eq_limits"))) {
                m_mechView.setText(sb.toString());
            }
        }
        m_mechView.setCaretPosition(0);

        // Preview image of the unit...
        //m_clientgui.loadPreviewImage(m_pPreview, entity, m_client.getLocalPlayer());
        m_pPreview.paint(m_pPreview.getGraphics());
    }

    void previewSquad(Entity entity) {
        MechView mechView = new MechView(entity, m_client.game.getOptions().booleanOption("show_bay_detail"));
        squadronView.setEditable(false);
        String readout = mechView.getMechReadout();
        squadronView.setText(readout);
        squadronView.setCaretPosition(0);
    }

    private static final String SPACES = "                        "; //$NON-NLS-1$
    private String makeLength(String s, int nLength) {
        if (s.length() == nLength) {
            return s;
        }
        else if (s.length() > nLength) {
            return s.substring(0, nLength - 2) + ".."; //$NON-NLS-1$
        }
        else {
            return s + SPACES.substring(0, nLength - s.length());
        }
    }

    public void keyReleased(java.awt.event.KeyEvent ke) {
    }

    public void keyPressed(java.awt.event.KeyEvent ke) {
    if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
        ActionEvent event = new ActionEvent(m_bPick,ActionEvent.ACTION_PERFORMED,""); //$NON-NLS-1$
        actionPerformed(event);
    }
        long curTime = System.currentTimeMillis();
        if (curTime - m_nLastSearch > KEY_TIMEOUT) {
            m_sbSearch = new StringBuffer();
        }
        m_nLastSearch = curTime;
        m_sbSearch.append(ke.getKeyChar());
        searchFor(m_sbSearch.toString().toLowerCase());
    }

    public void keyTyped(java.awt.event.KeyEvent ke) {
    }

    //
    // WindowListener
    //
    public void windowActivated(java.awt.event.WindowEvent windowEvent) {
    }
    public void windowClosed(java.awt.event.WindowEvent windowEvent) {
    }
    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
        setVisible(false);
    }
    public void windowDeactivated(java.awt.event.WindowEvent windowEvent) {
    }
    public void windowDeiconified(java.awt.event.WindowEvent windowEvent) {
    }
    public void windowIconified(java.awt.event.WindowEvent windowEvent) {
    }
    public void windowOpened(java.awt.event.WindowEvent windowEvent) {
    }

    private void updateWidgetEnablements() {
        final boolean enable = m_mechList.getSelectedIndex() != -1;
        butAdd.setEnabled(enable);
        m_bPick.setEnabled(true);
    }

    private void autoSetSkills(Entity e) {
        IClientPreferences cs = PreferenceManager.getClientPreferences();
        if(!cs.useAverageSkills()) {
            return;
        }
        int piloting=5;
        int gunnery=4;
        if(e.isClan()) {
            if((e instanceof Mech)
                    || (e instanceof BattleArmor)) {
                gunnery = 3;
                piloting = 4;
                if(m_client.game.getOptions().booleanOption("pilot_advantages")) {
                    PilotOptions ops = e.getCrew().getOptions();
                    ops.getOption("clan_pilot_training").setValue(true);
                }
            }
            else if(e instanceof Tank) {
                gunnery = 5;
                piloting = 6;
            }
            else if(e instanceof Infantry) {
                if(e.getMovementMode() == IEntityMovementMode.INF_LEG) {
                    gunnery = 5;
                    piloting = 5;
                }
                else {
                    gunnery = 5;
                    piloting = 6;
                }
            }
        }
        else if(e instanceof Infantry) {
            //IS crews are 4/5 except infantry
            if((e.getMovementMode() == IEntityMovementMode.INF_LEG)
                    || (e instanceof BattleArmor)) {
                gunnery = 4;
                piloting = 5;
            }
            else {
                gunnery = 4;
                piloting = 6;
            }
        }
        e.getCrew().setGunnery(gunnery);
        e.getCrew().setPiloting(piloting);
    }

    /*
     * Now being done in Compute
    private FighterSquadron compileSquadron(Vector<Entity> squadron) {

        //cycle through the entity vector and create a fighter squadron
        FighterSquadron fs = new FighterSquadron();

        String chassis = squadron.elementAt(0).getChassis();
        int si = 99;
        boolean alike = true;
        int armor = 0;
        int heat = 0;
        int safeThrust = 99;
        int n = 0;
        float weight = 0.0f;
        int bv = 0;
        double cost = 0.0;
        int nTC = 0;
        for(Entity e : squadron) {
            if(!chassis.equals(e.getChassis())) {
                alike = false;
            }
            n++;
            //names
            fs.fighters.add(e.getChassis() + " " + e.getModel());
            //armor
            armor += e.getTotalArmor();
            //heat
            heat += e.getHeatCapacity();
            //weight
            weight += e.getWeight();
            bv += e.calculateBattleValue();
            cost += e.getCost();
            //safe thrust
            if(e.getWalkMP() < safeThrust)
                safeThrust = e.getWalkMP();

            Aero a = (Aero)e;
            //si
            if(a.getSI() < si) {
                si = a.getSI();
            }

            //weapons
            Mounted newmount;
            for(Mounted m : e.getEquipment() ) {

                if(m.getType() instanceof WeaponType) {
                    //first load the weapon onto the squadron
                    WeaponType wtype = (WeaponType)m.getType();
                    try{
                        newmount = fs.addEquipment(wtype, m.getLocation());
                    } catch (LocationFullException ex) {
                        System.out.println("Unable to compile weapons"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        ex.printStackTrace();
                        return fs;
                    }
                    //skip to the next if it has no AT class
                    if(wtype.getAtClass() == WeaponType.CLASS_NONE) {
                        continue;
                    }

                    //now find the right bay
                    Mounted bay = fs.getFirstBay(wtype, newmount.getLocation(), newmount.isRearMounted());
                    //if this is null, then I should create a new bay
                    if(bay == null) {
                        EquipmentType newBay = WeaponBay.getBayType(wtype.getAtClass());
                        try{
                            bay = fs.addEquipment(newBay, newmount.getLocation());
                        } catch (LocationFullException ex) {
                            System.out.println("Unable to compile weapons"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                            ex.printStackTrace();
                            return fs;
                        }
                    }
                    //now add the weapon to the bay
                    bay.addWeapon(newmount);
                } else {
                    //just add the equipment normally
                    try{
//                        check if this is a TC
                        if (m.getType() instanceof MiscType && m.getType().hasFlag(MiscType.F_TARGCOMP)) {
                            nTC++;
                        }
                        fs.addEquipment(m.getType(), m.getLocation());
                    } catch (LocationFullException ex) {
                        System.out.println("Unable to add equipment"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        ex.printStackTrace();
                        return fs;
                    }
                }
            }
        }

        armor = (int)Math.round(armor / 10.0);

        fs.setArmor(armor);
        fs.set0Armor(armor);
        fs.setHeatSinks(heat);
        fs.setOriginalWalkMP(safeThrust);
        fs.setN0Fighters(n);
        fs.setNFighters(n);
        fs.autoSetThresh();
        fs.setWeight(weight);
        fs.set0SI(si);

        if(nTC >= n) {
            fs.setHasTC(true);
        }

        //if all the same chassis, name by chassis
        //otherwise name by weight
        if(alike) {
            fs.setChassis(chassis + " Squadron");
        } else {
            int aveWeight = Math.round(weight/n);
            if(aveWeight <= 45) {
                fs.setChassis("Mixed Light Squadron");
            } else if(aveWeight < 75) {
                fs.setChassis("Mixed Medium Squadron");
            } else {
                fs.setChassis("Mixed Heavy Squadron");
            }
        }
        fs.setModel("");

        fs.loadAllWeapons();
        fs.updateAllWeaponBays();


        return fs;
    }
    */
}
