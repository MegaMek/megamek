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
import java.awt.Choice;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.List;
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
import java.awt.event.WindowListener;
import java.io.File;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Vector;

import megamek.client.Client;
import megamek.client.ui.MechView;
import megamek.client.ui.Messages;
import megamek.client.ui.AWT.widget.BufferedPanel;
import megamek.common.BattleArmor;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.EquipmentType;
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

public class MechSelectorDialog extends Dialog implements ActionListener,
        ItemListener, KeyListener, Runnable, WindowListener {
    /**
     * 
     */
    private static final long serialVersionUID = -4382585886146902257L;

    // how long after a key is typed does a new search begin
    private final static int KEY_TIMEOUT = 1000;

    // these indices should match up with the static values in the
    // MechSummaryComparator
    private String[] m_saSorts = {
            Messages.getString("MechSelectorDialog.0"), Messages.getString("MechSelectorDialog.1"), Messages.getString("MechSelectorDialog.2"), Messages.getString("MechSelectorDialog.3"), Messages.getString("MechSelectorDialog.4"), Messages.getString("MechSelectorDialog.5"), Messages.getString("MechSelectorDialog.6") }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

    private MechSummary[] m_mechsCurrent;
    private Client m_client;
    private ClientGUI m_clientgui;
    private UnitLoadingDialog unitLoadingDialog;

    private StringBuffer m_sbSearch = new StringBuffer();
    private long m_nLastSearch = 0;

    private Label m_labelWeightClass = new Label(Messages
            .getString("MechSelectorDialog.m_labelWeightClass"), Label.RIGHT); //$NON-NLS-1$
    private Choice m_chWeightClass = new Choice();
    private Label m_labelType = new Label(Messages
            .getString("MechSelectorDialog.m_labelType"), Label.RIGHT); //$NON-NLS-1$
    private Choice m_chType = new Choice();
    private Label m_labelUnitType = new Label(Messages
            .getString("MechSelectorDialog.m_labelUnitType"), Label.RIGHT); //$NON-NLS-1$
    private Choice m_chUnitType = new Choice();
    private Label m_labelSort = new Label(Messages
            .getString("MechSelectorDialog.m_labelSort"), Label.RIGHT); //$NON-NLS-1$
    private Choice m_chSort = new Choice();
    private Panel m_pParams = new Panel();
    private Panel m_pListOptions = new Panel();
    private Label m_labelListOptions = new Label(Messages
            .getString("MechSelectorDialog.m_labelListOptions"));
    private Checkbox m_cModel = new Checkbox(Messages
            .getString("MechSelectorDialog.m_cModel"), GUIPreferences
            .getInstance().getMechSelectorIncludeModel());
    private Checkbox m_cName = new Checkbox(Messages
            .getString("MechSelectorDialog.m_cName"), GUIPreferences
            .getInstance().getMechSelectorIncludeName());
    private Checkbox m_cTons = new Checkbox(Messages
            .getString("MechSelectorDialog.m_cTons"), GUIPreferences
            .getInstance().getMechSelectorIncludeTons());
    private Checkbox m_cBV = new Checkbox(Messages
            .getString("MechSelectorDialog.m_cBV"), GUIPreferences
            .getInstance().getMechSelectorIncludeBV());
    private Checkbox m_cYear = new Checkbox(Messages
            .getString("MechSelectorDialog.m_cYear"), GUIPreferences
            .getInstance().getMechSelectorIncludeYear());
    private Checkbox m_cLevel = new Checkbox(Messages
            .getString("MechSelectorDialog.m_cLevel"), GUIPreferences
            .getInstance().getMechSelectorIncludeLevel());
    private Checkbox m_cCost = new Checkbox(Messages
            .getString("MechSelectorDialog.m_cCost"), GUIPreferences
            .getInstance().getMechSelectorIncludeCost());

    private Panel m_pOpenAdvanced = new Panel();
    private Button m_bToggleAdvanced = new Button("< Advanced Search >");
    private Panel m_pSouthParams = new Panel();

    List m_mechList = new List(10);
    private Button m_bPick = new Button(Messages
            .getString("MechSelectorDialog.m_bPick")); //$NON-NLS-1$
    private Button m_bPickClose = new Button(Messages
            .getString("MechSelectorDialog.m_bPickClose")); //$NON-NLS-1$    
    private Button m_bCancel = new Button(Messages.getString("Close")); //$NON-NLS-1$
    private Panel m_pButtons = new Panel();

    private TextArea m_mechView = new TextArea("", 36, 35);
    private Panel m_pLeft = new Panel();

    private Choice m_cWalk = new Choice();
    private TextField m_tWalk = new TextField(2);
    private Choice m_cJump = new Choice();
    private TextField m_tJump = new TextField(2);
    private Choice m_cArmor = new Choice();
    private TextField m_tWeapons1 = new TextField(2);
    private Choice m_cWeapons1 = new Choice();
    private Choice m_cOrAnd = new Choice();
    private TextField m_tWeapons2 = new TextField(2);
    private Choice m_cWeapons2 = new Choice();
    private Checkbox m_chkEquipment = new Checkbox();
    private Choice m_cEquipment = new Choice();
    private TextField m_tStartYear = new TextField(4);
    private TextField m_tEndYear = new TextField(4);
    private Button m_bSearch = new Button(Messages
            .getString("MechSelectorDialog.Search.Search"));
    private Button m_bReset = new Button(Messages
            .getString("MechSelectorDialog.Search.Reset"));
    private Label m_lCount = new Label();

    private int m_count;
    private int m_old_nType;
    private int m_old_nUnitType;

    private Panel m_pUpper = new Panel();
    BufferedPanel m_pPreview = new BufferedPanel();

    private Label m_labelPlayer = new Label(Messages
            .getString("MechSelectorDialog.m_labelPlayer"), Label.RIGHT); //$NON-NLS-1$
    private Choice m_chPlayer = new Choice();

    private boolean includeMaxTech;

    private EntityVerifier entityVerifier = new EntityVerifier(new File(
            "data/mechfiles/UnitVerifierOptions.xml"));

    public MechSelectorDialog(ClientGUI cl, UnitLoadingDialog uld) {
        super(cl.frame, Messages.getString("MechSelectorDialog.title"), true); //$NON-NLS-1$
        m_client = cl.getClient();
        m_clientgui = cl;
        unitLoadingDialog = uld;

        for (int x = 0; x < m_saSorts.length; x++) {
            m_chSort.addItem(m_saSorts[x]);
        }
        updatePlayerChoice();

        m_pParams.setLayout(new GridLayout(4, 2));
        m_pParams.add(m_labelWeightClass);
        m_pParams.add(m_chWeightClass);
        m_pParams.add(m_labelType);
        m_pParams.add(m_chType);
        m_pParams.add(m_labelUnitType);
        m_pParams.add(m_chUnitType);
        m_pParams.add(m_labelSort);
        m_pParams.add(m_chSort);

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
        m_pButtons.add(m_bPickClose);
        m_pButtons.add(m_bCancel);
        m_pButtons.add(m_labelPlayer);
        m_pButtons.add(m_chPlayer);

        m_pUpper.setLayout(new BorderLayout());
        m_pPreview.setPreferredSize(84, 72);
        m_pUpper.add(m_pParams, BorderLayout.WEST);
        m_pUpper.add(m_pPreview, BorderLayout.CENTER);
        m_pUpper.add(m_pSouthParams, BorderLayout.SOUTH);

        m_pLeft.setLayout(new BorderLayout());
        m_pLeft.add(m_pUpper, BorderLayout.NORTH);
        m_mechList.setFont(new Font("Monospaced", Font.PLAIN, 12)); //$NON-NLS-1$
        m_mechList.addKeyListener(this);
        m_pLeft.add(m_mechList, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(m_pLeft, BorderLayout.WEST);
        m_mechView.setFont(new Font("Monospaced", Font.PLAIN, 12)); //$NON-NLS-1$
        add(m_mechView, BorderLayout.CENTER);
        add(m_pButtons, BorderLayout.SOUTH);

        // clearMechPreview();

        m_chWeightClass.addItemListener(this);
        m_chType.addItemListener(this);
        m_chUnitType.addItemListener(this);
        m_chSort.addItemListener(this);
        m_mechList.addItemListener(this);
        m_bPick.addActionListener(this);
        m_bPickClose.addActionListener(this);
        m_bCancel.addActionListener(this);
        m_bSearch.addActionListener(this);
        m_bReset.addActionListener(this);
        m_bToggleAdvanced.addActionListener(this);
        setSize(700, 350);
        setLocation(computeDesiredLocation());
        populateChoices();
        addWindowListener(this);
        updateWidgetEnablements();
    }

    private void buildSouthParams(boolean showAdvanced) {
        if (showAdvanced) {
            m_bToggleAdvanced.setLabel(Messages
                    .getString("MechSelectorDialog.Search.Hide"));
            m_pOpenAdvanced.add(m_bToggleAdvanced);

            m_pSouthParams.setLayout(new GridLayout(11, 1));
            m_pSouthParams.add(m_pListOptions);
            m_pSouthParams.add(m_pOpenAdvanced);

            Panel row1 = new Panel();
            row1.setLayout(new FlowLayout(FlowLayout.LEFT));
            row1.add(new Label(Messages
                    .getString("MechSelectorDialog.Search.Walk")));
            row1.add(m_cWalk);
            row1.add(m_tWalk);
            m_pSouthParams.add(row1);

            Panel row2 = new Panel();
            row2.setLayout(new FlowLayout(FlowLayout.LEFT));
            row2.add(new Label(Messages
                    .getString("MechSelectorDialog.Search.Jump")));
            row2.add(m_cJump);
            row2.add(m_tJump);
            m_pSouthParams.add(row2);

            Panel row3 = new Panel();
            row3.setLayout(new FlowLayout(FlowLayout.LEFT));
            row3.add(new Label(Messages
                    .getString("MechSelectorDialog.Search.Armor")));
            row3.add(m_cArmor);
            m_pSouthParams.add(row3);

            Panel row4 = new Panel();
            row4.setLayout(new FlowLayout(FlowLayout.LEFT));
            row4.add(new Label(Messages
                    .getString("MechSelectorDialog.Search.Weapons")));
            m_pSouthParams.add(row4);

            Panel row5 = new Panel();
            row5.setLayout(new FlowLayout(FlowLayout.LEFT));
            row5.add(new Label(Messages
                    .getString("MechSelectorDialog.Search.WeaponsAtLeast")));
            row5.add(m_tWeapons1);
            row5.add(m_cWeapons1);
            m_pSouthParams.add(row5);

            Panel row6 = new Panel();
            row6.setLayout(new FlowLayout(FlowLayout.LEFT));
            row6.add(m_cOrAnd);
            row6.add(new Label(Messages
                    .getString("MechSelectorDialog.Search.WeaponsAtLeast")));
            row6.add(m_tWeapons2);
            row6.add(m_cWeapons2);
            m_pSouthParams.add(row6);

            Panel row7 = new Panel();
            row7.setLayout(new FlowLayout(FlowLayout.LEFT));
            row7.add(new Label(Messages
                    .getString("MechSelectorDialog.Search.Equipment")));
            row7.add(m_chkEquipment);
            row7.add(m_cEquipment);
            m_pSouthParams.add(row7);

            Panel row8 = new Panel();
            row8.setLayout(new FlowLayout(FlowLayout.LEFT));
            row8.add(new Label(Messages
                    .getString("MechSelectorDialog.Search.Year")));
            row8.add(m_tStartYear);
            row8.add(new Label("-"));
            row8.add(m_tEndYear);
            m_pSouthParams.add(row8);

            Panel row9 = new Panel();
            row9.add(m_bSearch);
            row9.add(m_bReset);
            row9.add(m_lCount);
            m_pSouthParams.add(row9);
        } else {
            m_bToggleAdvanced.setLabel(Messages
                    .getString("MechSelectorDialog.Search.Show"));
            m_pOpenAdvanced.add(m_bToggleAdvanced);

            m_pSouthParams.setLayout(new GridLayout(2, 1));
            m_pSouthParams.add(m_pListOptions);
            m_pSouthParams.add(m_pOpenAdvanced);
        }
    }

    private void toggleAdvanced() {
        m_pUpper.remove(m_pSouthParams);
        m_pSouthParams = new Panel();
        if (GUIPreferences.getInstance().getMechSelectorShowAdvanced()) {
            buildSouthParams(false);
            GUIPreferences.getInstance().setMechSelectorShowAdvanced(false);
        } else {
            buildSouthParams(true);
            GUIPreferences.getInstance().setMechSelectorShowAdvanced(true);
        }
        m_pUpper.add(m_pSouthParams, BorderLayout.SOUTH);
        this.invalidate();
        this.pack();
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
        if (hFailedFiles != null && hFailedFiles.size() > 0) {
            new UnitFailureDialog(m_clientgui.frame, hFailedFiles); // self-showing
                                                                    // dialog
        }
    }

    private void populateChoices() {

        for (int i = 0; i < EntityWeightClass.SIZE; i++) {
            m_chWeightClass.addItem(EntityWeightClass.getClassName(i));
        }
        m_chWeightClass.addItem(Messages.getString("MechSelectorDialog.All")); //$NON-NLS-1$
        m_chWeightClass.select(0);

        includeMaxTech = m_client.game.getOptions().booleanOption(
                "allow_advanced_units");
        int maxTech = (includeMaxTech ? TechConstants.SIZE
                : TechConstants.SIZE_LEVEL_2);
        for (int i = 0; i < maxTech; i++) {
            m_chType.addItem(TechConstants.getLevelDisplayableName(i));
        }
        // m_chType.addItem(Messages.getString("MechSelectorDialog.ISAll"));
        // //$NON-NLS-1$
        // m_chType.addItem(Messages.getString("MechSelectorDialog.ISAndClan"));
        // //$NON-NLS-1$
        // More than 8 items causes the drop down to sprout a vertical
        // scroll bar. I guess we'll sacrifice this next one to stay
        // under the limit. Stupid AWT Choice class!
        // m_chType.addItem("Mixed All");
        // m_chType.addItem(Messages.getString("MechSelectorDialog.All"));
        // //$NON-NLS-1$
        m_chType.select(0);

        for (int i = 0; i < UnitType.SIZE; i++) {
            m_chUnitType.addItem(UnitType.getTypeDisplayableName(i));
        }
        m_chUnitType.addItem(Messages.getString("MechSelectorDialog.All")); //$NON-NLS-1$
        m_chUnitType.select(0);

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
        int nUnitType = m_chUnitType.getSelectedIndex();
        for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e
                .hasMoreElements();) {
            EquipmentType et = e.nextElement();
            if (et instanceof WeaponType
                    && (et.getTechLevel() == nType
                            || ((nType == TechConstants.T_TW_ALL) && ((et
                                    .getTechLevel() == TechConstants.T_INTRO_BOXSET)
                                    || (et.getTechLevel() == TechConstants.T_IS_TW_NON_BOX) || (et
                                    .getTechLevel() == TechConstants.T_CLAN_TW))) || ((nType == TechConstants.T_IS_TW_ALL || nType == TechConstants.T_IS_TW_NON_BOX) && ((et
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
                        && (nType == TechConstants.T_TW_ALL
                                || nType == TechConstants.T_IS_TW_NON_BOX || nType == TechConstants.T_IS_TW_ALL)) {
                    equipment.add(et.getName());
                }
            }
            if (et instanceof MiscType
                    && (et.getTechLevel() == nType
                            || ((nType == TechConstants.T_TW_ALL) && ((et
                                    .getTechLevel() == TechConstants.T_INTRO_BOXSET)
                                    || (et.getTechLevel() == TechConstants.T_IS_TW_NON_BOX) || (et
                                    .getTechLevel() == TechConstants.T_CLAN_TW))) || ((nType == TechConstants.T_IS_TW_ALL || nType == TechConstants.T_IS_TW_NON_BOX) && ((et
                            .getTechLevel() == TechConstants.T_INTRO_BOXSET) || (et
                            .getTechLevel() == TechConstants.T_IS_TW_NON_BOX))))) {
                equipment.add(et.getName());
            }
        }
        for (String weaponName : weapons) {
            m_cWeapons1.add(weaponName);
            m_cWeapons2.add(weaponName);
        }
        for (String equipName : equipment) {
            m_cEquipment.add(equipName);
        }
        m_cWeapons1.invalidate();
        m_cWeapons2.invalidate();
        m_cEquipment.invalidate();
        this.pack();
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
        for (int x = 0; x < mechs.length; x++) {
            if ( /* Weight */
            (nClass == EntityWeightClass.SIZE || mechs[x].getWeightClass() == nClass)
                    && /* Technology Level */
                    ((nType == TechConstants.T_ALL)
                            || (nType == mechs[x].getType())
                            || ((nType == TechConstants.T_TW_ALL) && ((mechs[x]
                                    .getType() == TechConstants.T_INTRO_BOXSET)
                                    || (mechs[x].getType() == TechConstants.T_IS_TW_NON_BOX) || (mechs[x]
                                    .getType() == TechConstants.T_CLAN_TW))) || ((nType == TechConstants.T_IS_TW_ALL) && ((mechs[x]
                            .getType() == TechConstants.T_INTRO_BOXSET) || (mechs[x]
                            .getType() == TechConstants.T_IS_TW_NON_BOX))))
                    && /* Unit Type (Mek, Infantry, etc.) */
                    (nUnitType == UnitType.SIZE || mechs[x].getUnitType()
                            .equals(UnitType.getTypeName(nUnitType)))
                    && /* canon required */(!m_client.game.getOptions()
                            .booleanOption("canon_only") || mechs[x].isCanon())) {
                vMechs.addElement(mechs[x]);
            }
        }
        m_mechsCurrent = new MechSummary[vMechs.size()];
        vMechs.copyInto(m_mechsCurrent);
        m_count = vMechs.size();
        if (!calledByAdvancedSearch
                && (m_old_nType != nType || m_old_nUnitType != nUnitType)) {
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
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            for (int x = 0; x < m_mechsCurrent.length; x++) {
                m_mechList.add(formatMech(m_mechsCurrent[x]));
            }
        } finally {
            this.setCursor(Cursor.getDefaultCursor());
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
        for (int i = 0; i < m_mechsCurrent.length; i++) {
            MechSummary ms = m_mechsCurrent[i];
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
                if (entity.getWalkMP() < walk)
                    return false;
            } else if (m_cWalk.getSelectedIndex() == 1) { // equal to
                if (walk != entity.getWalkMP())
                    return false;
            } else if (m_cWalk.getSelectedIndex() == 2) { // not more than
                if (entity.getWalkMP() > walk)
                    return false;
            }
        }

        int jump = -1;
        try {
            jump = Integer.parseInt(m_tJump.getText());
        } catch (NumberFormatException ne) {
        }
        if (jump > -1) {
            if (m_cJump.getSelectedIndex() == 0) { // at least
                if (entity.getJumpMP() < jump)
                    return false;
            } else if (m_cJump.getSelectedIndex() == 1) { // equal to
                if (jump != entity.getJumpMP())
                    return false;
            } else if (m_cJump.getSelectedIndex() == 2) { // not more than
                if (entity.getJumpMP() > jump)
                    return false;
            }
        }

        int sel = m_cArmor.getSelectedIndex();
        if (sel > 0) {
            int armor = entity.getTotalArmor();
            int maxArmor = entity.getTotalInternal() * 2 + 3;
            if (sel == 1) {
                if (armor < (maxArmor * .25))
                    return false;
            } else if (sel == 2) {
                if (armor < (maxArmor * .5))
                    return false;
            } else if (sel == 3) {
                if (armor < (maxArmor * .75))
                    return false;
            } else if (sel == 4) {
                if (armor < (maxArmor * .9))
                    return false;
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
            if (count >= weapon1)
                foundWeapon1 = true;
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
            if (count >= weapon2)
                foundWeapon2 = true;
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
        if (entity.getYear() < startYear || entity.getYear() > endYear) {
            return false;
        }

        if (weaponLine1Active && !weaponLine2Active && !foundWeapon1)
            return false;
        if (weaponLine2Active && !weaponLine1Active && !foundWeapon2)
            return false;
        if (weaponLine1Active && weaponLine2Active) {
            if (m_cOrAnd.getSelectedIndex() == 0 /* 0 is "or" choice */) {
                if (!foundWeapon1 && !foundWeapon2)
                    return false;
            } else { // "and" choice in effect
                if (!foundWeapon1 || !foundWeapon2)
                    return false;
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
            if (count < 1)
                return false;
        }

        return true;
    }

    private void resetSearch() {
        m_cWalk.select(0);
        m_tWalk.setText("");
        m_cJump.select(0);
        m_tJump.setText("");
        m_cArmor.select(0);
        m_tWeapons1.setText("");
        m_cWeapons1.select(0);
        m_cOrAnd.select(0);
        m_tWeapons2.setText("");
        m_cWeapons2.select(0);
        m_chkEquipment.setState(false);
        m_cEquipment.select(0);

        filterMechs(false);
    }

    private Point computeDesiredLocation() {
        int desiredX = m_clientgui.frame.getLocation().x
                + m_clientgui.frame.getSize().width / 2 - getSize().width / 2;
        if (desiredX < 0)
            desiredX = 0;
        int desiredY = m_clientgui.frame.getLocation().y
                + m_clientgui.frame.getSize().height / 2 - getSize().height / 2;
        if (desiredY < 0)
            desiredY = 0;
        return new Point(desiredX, desiredY);
    }

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
        if (GUIPreferences.getInstance().getMechSelectorIncludeModel())
            val += makeLength(ms.getModel(), 10) + " "; //$NON-NLS-1$ //$NON-NLS-2$
        if (GUIPreferences.getInstance().getMechSelectorIncludeName())
            val += makeLength(ms.getChassis(), 20) + " "; //$NON-NLS-1$ //$NON-NLS-2$
        if (GUIPreferences.getInstance().getMechSelectorIncludeTons())
            val += makeLength("" + ms.getTons(), 7) + " "; //$NON-NLS-1$ //$NON-NLS-2$
        if (GUIPreferences.getInstance().getMechSelectorIncludeBV())
            val += makeLength("" + ms.getBV(), 6) + " "; //$NON-NLS-1$ //$NON-NLS-2$
        if (GUIPreferences.getInstance().getMechSelectorIncludeYear())
            val += ms.getYear() + " ";
        if (GUIPreferences.getInstance().getMechSelectorIncludeLevel())
            val += levelOrValid + " ";
        if (GUIPreferences.getInstance().getMechSelectorIncludeCost())
            val += ms.getCost() + " ";
        return val;
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == m_bCancel) {
            this.setVisible(false);
        } else if (ae.getSource() == m_bPick || ae.getSource() == m_bPickClose) {
            int x = m_mechList.getSelectedIndex();
            if (x == -1) {
                return;
            }
            MechSummary ms = m_mechsCurrent[m_mechList.getSelectedIndex()];
            try {
                Entity e = new MechFileParser(ms.getSourceFile(), ms
                        .getEntryName()).getEntity();
                Client c = null;
                if (m_chPlayer.getSelectedIndex() > 0) {
                    String name = m_chPlayer.getSelectedItem();
                    c = m_clientgui.getBots().get(name);
                }
                if (c == null) {
                    c = m_client;
                }
                autoSetSkills(e);
                e.setOwner(c.getLocalPlayer());
                c.sendAddEntity(e);
            } catch (EntityLoadingException ex) {
                System.out
                        .println("Unable to load mech: " + ms.getSourceFile() + ": " + ms.getEntryName() + ": " + ex.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                ex.printStackTrace();
                return;
            }
            if (ae.getSource() == m_bPickClose) {
                this.setVisible(false);
            }
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
        } else if (ie.getSource() == m_chWeightClass
                || ie.getSource() == m_chType || ie.getSource() == m_chUnitType) {
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
        } else if (ie.getSource() == m_cModel || ie.getSource() == m_cName
                || ie.getSource() == m_cTons || ie.getSource() == m_cBV
                || ie.getSource() == m_cYear || ie.getSource() == m_cLevel
                || ie.getSource() == m_cCost) {
            GUIPreferences.getInstance().setMechSelectorIncludeModel(
                    m_cModel.getState());
            GUIPreferences.getInstance().setMechSelectorIncludeName(
                    m_cName.getState());
            GUIPreferences.getInstance().setMechSelectorIncludeTons(
                    m_cTons.getState());
            GUIPreferences.getInstance().setMechSelectorIncludeBV(
                    m_cBV.getState());
            GUIPreferences.getInstance().setMechSelectorIncludeYear(
                    m_cYear.getState());
            GUIPreferences.getInstance().setMechSelectorIncludeLevel(
                    m_cLevel.getState());
            GUIPreferences.getInstance().setMechSelectorIncludeCost(
                    m_cCost.getState());
            clearMechPreview();
            sortMechs(); // sorting has side-effect of repopulating list
            m_mechList.invalidate(); // force re-layout of window
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

    void previewMech(Entity entity) {
        MechView mechView = new MechView(entity, m_client.game.getOptions().booleanOption("show_bay_detail"));
        m_mechView.setEditable(false);
        String readout = mechView.getMechReadout();
        StringBuffer sb = new StringBuffer(readout);
        m_mechView.setText(readout);
        if (entity instanceof Mech || entity instanceof Tank) {
            TestEntity testEntity = null;
            if (entity instanceof Mech)
                testEntity = new TestMech((Mech) entity,
                        entityVerifier.mechOption, null);
            else
                // entity instanceof Tank
                testEntity = new TestTank((Tank) entity,
                        entityVerifier.tankOption, null);
            if (!testEntity.correctEntity(sb, !m_clientgui.getClient().game
                    .getOptions().booleanOption("is_eq_limits"))) {
                m_mechView.setText(sb.toString());
            }
        }
        m_mechView.setCaretPosition(0);

        // Preview image of the unit...
        m_clientgui.loadPreviewImage(m_pPreview, entity, m_client
                .getLocalPlayer());
        m_pPreview.paint(m_pPreview.getGraphics());
    }

    private static final String SPACES = "                        "; //$NON-NLS-1$

    private String makeLength(String s, int nLength) {
        if (s.length() == nLength) {
            return s;
        } else if (s.length() > nLength) {
            return s.substring(0, nLength - 2) + ".."; //$NON-NLS-1$
        } else {
            return s + SPACES.substring(0, nLength - s.length());
        }
    }

    public void keyReleased(java.awt.event.KeyEvent ke) {
    }

    public void keyPressed(java.awt.event.KeyEvent ke) {
        if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
            ActionEvent event = new ActionEvent(m_bPick,
                    ActionEvent.ACTION_PERFORMED, ""); //$NON-NLS-1$
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
        this.setVisible(false);
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
        m_bPick.setEnabled(enable);
        m_bPickClose.setEnabled(enable);
    }

   
    private void autoSetSkills(Entity e) {
        IClientPreferences cs = PreferenceManager.getClientPreferences();
        if (!cs.useAverageSkills())
            return;
        int piloting = 5;
        int gunnery = 4;
        if (e.isClan()) {
            if (e instanceof Mech || e instanceof BattleArmor) {
                gunnery = 3;
                piloting = 4;
                if (m_client.game.getOptions()
                        .booleanOption("pilot_advantages")) {
                    PilotOptions ops = e.getCrew().getOptions();
                    ops.getOption("clan_pilot_training").setValue(true);
                }
            } else if (e instanceof Tank) {
                gunnery = 5;
                piloting = 6;
            } else if (e instanceof Infantry) {
                if (e.getMovementMode() == IEntityMovementMode.INF_LEG) {
                    gunnery = 5;
                    piloting = 5;
                } else {
                    gunnery = 5;
                    piloting = 6;
                }
            }
        } else if (e instanceof Infantry) {
            // IS crews are 4/5 except infantry
            if (e.getMovementMode() == IEntityMovementMode.INF_LEG
                    || e instanceof BattleArmor) {
                gunnery = 4;
                piloting = 5;
            } else {
                gunnery = 4;
                piloting = 6;
            }
        }
        e.getCrew().setGunnery(gunnery);
        e.getCrew().setPiloting(piloting);
    }
}
