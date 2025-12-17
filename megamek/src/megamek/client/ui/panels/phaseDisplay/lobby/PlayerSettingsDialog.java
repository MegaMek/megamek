/*
 * Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.panels.phaseDisplay.lobby;

import static megamek.client.ui.Messages.getString;
import static megamek.client.ui.panels.phaseDisplay.lobby.LobbyUtility.isValidStartPos;
import static megamek.client.ui.util.UIUtil.teamColor;
import static megamek.client.ui.util.UIUtil.uiYellow;

import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serial;
import java.math.RoundingMode;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import megamek.MMConstants;
import megamek.client.Client;
import megamek.client.bot.BotClient;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.Princess;
import megamek.client.generator.ReconfigurationParameters;
import megamek.client.generator.TeamLoadOutGenerator;
import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.RATGenerator;
import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.dialogs.buttonDialogs.AbstractButtonDialog;
import megamek.client.ui.dialogs.buttonDialogs.BotConfigDialog;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.panels.abstractPanels.SkillGenerationOptionsPanel;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.util.UIUtil.Content;
import megamek.client.ui.util.UIUtil.FixedYPanel;
import megamek.client.ui.util.UIUtil.OptionPanel;
import megamek.client.ui.util.UIUtil.TipButton;
import megamek.client.ui.util.UIUtil.TipLabel;
import megamek.client.ui.util.UIUtil.TipTextField;
import megamek.common.OffBoardDirection;
import megamek.common.Player;
import megamek.common.Team;
import megamek.common.annotations.Nullable;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.containers.MunitionTree;
import megamek.common.equipment.Briefcase;
import megamek.common.equipment.ICarryable;
import megamek.common.interfaces.IStartingPositions;
import megamek.common.loaders.MapSettings;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.server.ServerBoardHelper;

/**
 * A dialog that can be used to adjust advanced player settings like initiative, minefields, and maybe other things in
 * the future like force abilities.
 *
 * @author Jay Lawson
 * @author Simon (Juliez)
 */
public class PlayerSettingsDialog extends AbstractButtonDialog {

    private static final String CMD_ADD_GROUND_OBJECT = "CMD_ADD_GROUND_OBJECT";
    private static final String CMD_REMOVE_GROUND_OBJECT = "CMD_REMOVE_GROUND_OBJECT_%d";
    private static final String CMD_REMOVE_GROUND_OBJECT_PREFIX = "CMD_REMOVE_GROUND_OBJECT_";

    public PlayerSettingsDialog(ClientGUI cg, Client cl, BoardView bv) {
        super(cg.getFrame(), "PlayerSettingsDialog", "PlayerSettingsDialog.title");
        client = cl;
        clientgui = cg;
        this.bv = bv;
        currentPlayerStartPos = cl.getLocalPlayer().getStartingPos();

        NumberFormat numFormat = NumberFormat.getIntegerInstance();
        numFormat.setGroupingUsed(false);

        NumberFormatter numFormatter = new NumberFormatter(numFormat);
        numFormatter.setMinimum(0);
        numFormatter.setCommitsOnValidEdit(true);

        DefaultFormatterFactory formatterFactory = new DefaultFormatterFactory(numFormatter);

        txtOffset = new JFormattedTextField(formatterFactory, 0);
        txtWidth = new JFormattedTextField(formatterFactory, 3);

        DecimalFormat tonnageFormat = new DecimalFormat();
        tonnageFormat.setGroupingUsed(false);
        tonnageFormat.setRoundingMode(RoundingMode.UNNECESSARY);

        txtGroundObjectTonnage = new JFormattedTextField(tonnageFormat);
        txtGroundObjectTonnage.setText("0");

        txtGroundObjectName = new JTextField();
        txtGroundObjectName.setColumns(20);
        // if it's longer than 20 characters, undo the edit
        txtGroundObjectName.getDocument().addUndoableEditListener(e -> {
            if (txtGroundObjectName.getText().length() > 20 && e.getEdit().canUndo()) {
                e.getEdit().undo();
            }
        });

        initialize();
    }

    private final DefaultListCellRenderer factionCbRenderer = new DefaultListCellRenderer() {
        @Serial
        private static final long serialVersionUID = -333065979253244440L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
              boolean cellHasFocus) {
            if (value == null) {
                setText("General");
            } else {
                setText(((FactionRecord) value).getName(year));
            }
            return this;
        }
    };

    private final Comparator<FactionRecord> factionSorter = new Comparator<>() {
        @Override
        public int compare(FactionRecord o1, FactionRecord o2) {
            return o1.getName(year).compareTo(o2.getName(year));
        }
    };

    @Override
    protected void okAction() {
        apply();
    }

    /** Returns the chosen initiative modifier. */
    public int getInit() {
        return parseField(fldInit);
    }

    /** Returns the chosen conventional mines. */
    public int getCnvMines() {
        return parseField(fldConventional);
    }

    /** Returns the chosen inferno mines. */
    public int getInfMines() {
        return parseField(fldInferno);
    }

    /** Returns the chosen active mines. */
    public int getActMines() {
        return parseField(fldActive);
    }

    /** Returns the chosen vibrabombs. */
    public int getVibMines() {
        return parseField(fldVibrabomb);
    }

    /** Returns the start location offset */
    public int getStartOffset() {
        return parseField(txtOffset);
    }

    /** Returns the player start location width */
    public int getStartWidth() {
        return parseField(txtWidth);
    }

    /** Returns the chosen deployment position. */
    public int getStartPos() {
        return currentPlayerStartPos;
    }

    public int getStartingAnyNWx() {
        return Math.min((Integer) spinStartingAnyNWx.getValue(), (Integer) spinStartingAnySEx.getValue()) - 1;
    }

    public int getStartingAnyNWy() {
        return Math.min((Integer) spinStartingAnyNWy.getValue(), (Integer) spinStartingAnySEy.getValue()) - 1;
    }

    public int getStartingAnySEx() {
        return Math.max((Integer) spinStartingAnyNWx.getValue(), (Integer) spinStartingAnySEx.getValue()) - 1;
    }

    public int getStartingAnySEy() {
        return Math.max((Integer) spinStartingAnyNWy.getValue(), (Integer) spinStartingAnySEy.getValue()) - 1;
    }

    /**
     * @return the current {@link SkillGenerationOptionsPanel}
     */
    public SkillGenerationOptionsPanel getSkillGenerationOptionsPanel() {
        return skillGenerationOptionsPanel;
    }

    /** Returns the player's email address. */
    public String getEmail() {
        return fldEmail.getText().trim();
    }

    // PRIVATE

    private final transient Client client;
    private final transient ClientGUI clientgui;
    private final transient BoardView bv;
    private Player player;

    // Initiative Section
    private final JLabel labInit = new TipLabel(Messages.getString("PlayerSettingsDialog.initMod"),
          SwingConstants.RIGHT);
    private final TipTextField fldInit = new TipTextField(3);

    // Mines Section
    private final JLabel labConventional = new JLabel(getString("PlayerSettingsDialog.labConventional"),
          SwingConstants.RIGHT);
    private final JLabel labVibrabomb = new JLabel(getString("PlayerSettingsDialog.labVibrabomb"),
          SwingConstants.RIGHT);
    private final JLabel labActive = new JLabel(getString("PlayerSettingsDialog.labActive"), SwingConstants.RIGHT);
    private final JLabel labInferno = new JLabel(getString("PlayerSettingsDialog.labInferno"), SwingConstants.RIGHT);
    private final JTextField fldConventional = new JTextField(3);
    private final JTextField fldVibrabomb = new JTextField(3);
    private final JTextField fldActive = new JTextField(3);
    private final JTextField fldInferno = new JTextField(3);

    // Skills Section
    private SkillGenerationOptionsPanel skillGenerationOptionsPanel;

    // Email section
    private final JLabel labEmail = new JLabel(getString("PlayerSettingsDialog.labEmail"), SwingConstants.RIGHT);
    private final JTextField fldEmail = new JTextField(20);

    // Deployment Section
    private final JPanel panStartButtons = new JPanel();
    private final Map<Integer, TipButton> butStartPos = new HashMap<>();

    private final JFormattedTextField txtOffset;
    private final JFormattedTextField txtWidth;
    private JSpinner spinStartingAnyNWx;
    private JSpinner spinStartingAnyNWy;
    private JSpinner spinStartingAnySEx;
    private JSpinner spinStartingAnySEy;

    // ground object config section
    private Content groundSectionContent = new Content(new GridLayout(2, 3));
    private final JTextField txtGroundObjectName;
    private final JFormattedTextField txtGroundObjectTonnage;
    private final JCheckBox chkGroundObjectInvulnerable = new JCheckBox();
    private final List<List<Component>> groundSectionComponents = new ArrayList<>();

    // Bot Settings Section
    private final JButton butBotSettings = new JButton(Messages.getString("PlayerSettingsDialog.botSettings"));

    // Team Configuration Section
    private Team team;
    private transient ReconfigurationParameters rp;
    private int year;
    private final JLabel labelAutoconfig = new TipLabel(Messages.getString("PlayerSettingsDialog.autoConfigFaction"),
          SwingConstants.LEFT);
    private final JComboBox<FactionRecord> cmbFaction = new JComboBox<>();
    private final JButton butAutoconfigure = new JButton(Messages.getString("PlayerSettingsDialog.autoConfig"));
    private final JButton butRandomize = new JButton(Messages.getString("PlayerSettingsDialog.randomize"));
    private final JCheckBox chkTrulyRandom = new JCheckBox("Truly Random", false);
    private final JCheckBox chkBanNukes = new JCheckBox("No Nukes", true);
    private final JButton butSaveADF = new JButton(Messages.getString("PlayerSettingsDialog.saveADF"));
    private final JButton butLoadADF = new JButton(Messages.getString("PlayerSettingsDialog.loadADF"));
    private final JButton butRestoreMT = new JButton(Messages.getString("PlayerSettingsDialog.restore"));
    private transient TeamLoadOutGenerator tlg;
    private transient MunitionTree munitionTree = null;
    private transient MunitionTree originalMT = null;

    private int currentPlayerStartPos;

    @Override
    protected Container createCenterPane() {
        setupValues();

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(headerSection());
        mainPanel.add(autoConfigSection());
        if (client instanceof BotClient) {
            mainPanel.add(botSection());
        }
        mainPanel.add(startSection());
        mainPanel.add(initiativeSection());
        if (client.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_MINEFIELDS)) {
            mainPanel.add(mineSection());
        }
        mainPanel.add(groundObjectConfigSection());
        mainPanel.add(skillsSection());
        if (!(client instanceof BotClient)) {
            mainPanel.add(emailSection());
        }
        mainPanel.add(Box.createVerticalGlue());

        var scrMain = new JScrollPane(mainPanel);
        scrMain.getVerticalScrollBar().setUnitIncrement(16);
        scrMain.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrMain.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrMain.setBorder(null);
        return scrMain;
    }

    private JPanel headerSection() {
        JPanel result = new FixedYPanel();
        result.setAlignmentX(Component.LEFT_ALIGNMENT);
        Icon playerIcon = client.getLocalPlayer().getCamouflage().getImageIcon(UIUtil.scaleForGUI(40));
        JLabel playerLabel = new JLabel(client.getLocalPlayer().getName(), playerIcon, SwingConstants.CENTER);
        playerLabel.setIconTextGap(UIUtil.scaleForGUI(12));
        playerLabel.setBorder(new EmptyBorder(15, 0, 10, 0));
        result.add(playerLabel);
        return result;
    }

    private JPanel autoConfigSection() {
        JPanel result = new OptionPanel("PlayerSettingsDialog.header.autoConfig");
        result.setToolTipText(Messages.getString("CustomMekDialog.acfPanelDesc"));
        Content panContent = new Content(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        result.add(panContent);

        // Set up autoconfiguration controls for player
        panContent.add(labelAutoconfig, gbc);
        panContent.add(cmbFaction, gbc);
        cmbFaction.setToolTipText(Messages.getString("CustomMekDialog.acfFactionChooser"));
        cmbFaction.setRenderer(factionCbRenderer);
        updateFactionChoice(getFactionFromCode(team.getFaction(), year));
        panContent.add(butAutoconfigure, gbc);
        butAutoconfigure.addActionListener(listener);
        butAutoconfigure.setToolTipText(Messages.getString("CustomMekDialog.acfExecuteConfig"));
        panContent.add(butRandomize, gbc);
        butRandomize.addActionListener(listener);
        butRandomize.setToolTipText(Messages.getString("CustomMekDialog.acfRandomizer"));
        panContent.add(chkTrulyRandom, gbc);
        chkTrulyRandom.setToolTipText(Messages.getString("CustomMekDialog.acfTrulyRandom"));
        panContent.add(chkBanNukes, gbc);
        chkBanNukes.setToolTipText(Messages.getString("CustomMekDialog.acfBanNukes"));
        panContent.add(butSaveADF, gbc);
        butSaveADF.setToolTipText(Messages.getString("CustomMekDialog.acfSaveADF"));
        butSaveADF.addActionListener(listener);
        panContent.add(butLoadADF, gbc);
        butLoadADF.setToolTipText(Messages.getString("CustomMekDialog.acfLoadADF"));
        butLoadADF.addActionListener(listener);
        panContent.add(butRestoreMT, gbc);
        butRestoreMT.setToolTipText(Messages.getString("CustomMekDialog.acfRestoreMunitionTree"));
        butRestoreMT.addActionListener(listener);
        butRestoreMT.setEnabled(false);
        return result;
    }

    private JPanel groundObjectConfigSection() {
        JPanel result = new OptionPanel("PlayerSettingsDialog.header.GroundObjects");
        result.setToolTipText("Define carryable objects that can be placed prior to unit deployment");
        groundSectionContent = new Content(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel lblName = new JLabel("Name");
        groundSectionContent.add(lblName, gbc);

        gbc.gridx = 1;
        JLabel lblTonnage = new JLabel("Tonnage");
        groundSectionContent.add(lblTonnage, gbc);

        gbc.gridx = 2;
        JLabel lblInvulnerable = new JLabel("Invulnerable");
        groundSectionContent.add(lblInvulnerable);

        gbc.gridy = 1;
        gbc.gridx = 0;
        groundSectionContent.add(txtGroundObjectName, gbc);

        gbc.gridx = 1;
        groundSectionContent.add(txtGroundObjectTonnage, gbc);

        gbc.gridx = 2;
        groundSectionContent.add(chkGroundObjectInvulnerable, gbc);

        gbc.gridx = 3;
        JButton btnAdd = new JButton("Add");
        btnAdd.setActionCommand(CMD_ADD_GROUND_OBJECT);
        btnAdd.addActionListener(listener);
        groundSectionContent.add(btnAdd, gbc);

        for (ICarryable groundObject : player.getGroundObjectsToPlace()) {
            addGroundObjectToUI(groundObject);
        }

        result.add(groundSectionContent);
        return result;
    }

    /**
     * Worker function that adds the given ground object to the UI
     */
    private void addGroundObjectToUI(ICarryable groundObject) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = groundSectionComponents.size() + 2; // there's always two extra rows - header + text fields
        gbc.gridx = 0;

        JLabel nameLabel = new JLabel(groundObject.generalName());
        groundSectionContent.add(nameLabel, gbc);
        List<Component> row = new ArrayList<>();
        row.add(nameLabel);

        gbc.gridx = 1;
        JLabel tonnageLabel = new JLabel(Double.toString(groundObject.getTonnage()));
        groundSectionContent.add(tonnageLabel, gbc);
        row.add(tonnageLabel);

        gbc.gridx = 2;
        JLabel flagLabel = new JLabel(groundObject.isInvulnerable() ? "Yes" : "No");
        groundSectionContent.add(flagLabel, gbc);
        row.add(flagLabel);

        gbc.gridx = 3;
        JButton btnRemove = new JButton("Remove");
        btnRemove.setActionCommand(String.format(CMD_REMOVE_GROUND_OBJECT,
              player.getGroundObjectsToPlace().size() - 1));
        btnRemove.addActionListener(listener);
        groundSectionContent.add(btnRemove, gbc);
        row.add(btnRemove);
        groundSectionComponents.add(row);
        validate();
    }

    /**
     * Worker function that uses the current state of the ground object inputs to add a new ground object to the backing
     * player and the UI
     */
    private void addGroundObject() {
        Briefcase briefcase = new Briefcase();
        briefcase.setName(txtGroundObjectName.getText());

        double tonnage = 0.0;

        try {
            tonnage = Double.parseDouble(txtGroundObjectTonnage.getText());

            // don't allow negative tonnage as we do not have antigravity technology
            if (tonnage < 0) {
                tonnage = 0.0;
            }
        } catch (Exception ignored) {

        }

        briefcase.setTonnage(tonnage);
        briefcase.setInvulnerable(chkGroundObjectInvulnerable.isSelected());
        player.getGroundObjectsToPlace().add(briefcase);

        addGroundObjectToUI(briefcase);
    }

    /**
     * Worker function that removes the chosen ground object from the backing player and the UI
     */
    private void removeGroundObject(String command) {
        int index = Integer.parseInt(command.substring(CMD_REMOVE_GROUND_OBJECT_PREFIX.length()));
        player.getGroundObjectsToPlace().remove(index);
        for (Component component : groundSectionComponents.get(index)) {
            groundSectionContent.remove(component);
        }
        groundSectionComponents.remove(index);

        // kind of a hack, but I'm being lazy - re-index all the
        // CMD_REMOVE_GROUND_OBJECT commands beyond
        // the one that just removed, so they're not pointing to higher indexes than
        // they need to
        for (int componentIndex = index; componentIndex < groundSectionComponents.size(); componentIndex++) {
            ((JButton) groundSectionComponents.get(index).get(2)).setActionCommand(String.format(
                  CMD_REMOVE_GROUND_OBJECT,
                  componentIndex));
        }

        validate();
    }

    private JPanel botSection() {
        JPanel result = new OptionPanel("PlayerSettingsDialog.header.botPlayer");
        Content panContent = new Content(new FlowLayout());
        result.add(panContent);
        panContent.add(butBotSettings);
        butBotSettings.addActionListener(listener);
        return result;
    }

    private JPanel startSection() {
        JPanel result = new OptionPanel("PlayerSettingsDialog.header.startPos");
        Content panContent = new Content(new GridBagLayout());
        result.add(panContent);
        setupStartGrid();
        panContent.add(panStartButtons, GBC.eol());
        panContent.add(deploymentParametersPanel(), GBC.eol());
        return result;
    }

    private JPanel deploymentParametersPanel() {
        GridBagLayout gbl = new GridBagLayout();
        JPanel result = new JPanel(gbl);

        JLabel lblOffset = new JLabel(Messages.getString("CustomMekDialog.labDeploymentOffset"));
        lblOffset.setToolTipText(Messages.getString("CustomMekDialog.labDeploymentOffsetTip"));
        JLabel lblWidth = new JLabel(Messages.getString("CustomMekDialog.labDeploymentWidth"));
        lblWidth.setToolTipText(Messages.getString("CustomMekDialog.labDeploymentWidthTip"));

        txtOffset.setColumns(4);
        txtWidth.setColumns(4);

        result.add(lblOffset, GBC.std());
        result.add(txtOffset, GBC.eol());
        result.add(lblWidth, GBC.std());
        result.add(txtWidth, GBC.eol());

        result.add(new JLabel(Messages.getString("CustomMekDialog.labDeploymentAnyNW")), GBC.std());
        result.add(spinStartingAnyNWx, GBC.std());
        result.add(spinStartingAnyNWy, GBC.eol());
        result.add(new JLabel(Messages.getString("CustomMekDialog.labDeploymentAnySE")), GBC.std());
        result.add(spinStartingAnySEx, GBC.std());
        result.add(spinStartingAnySEy, GBC.eol());

        JButton btnUseRuler = new JButton(Messages.getString("CustomMekDialog.BtnDeploymentUseRuler"));
        btnUseRuler.setToolTipText(Messages.getString("CustomMekDialog.BtnDeploymentUseRulerTip"));
        btnUseRuler.addActionListener(e -> useRuler());
        result.add(btnUseRuler, GBC.std());
        JButton btnApply = new JButton(Messages.getString("CustomMekDialog.BtnDeploymentApply"));
        btnApply.setToolTipText(Messages.getString("CustomMekDialog.BtnDeploymentApplyTip"));
        btnApply.addActionListener(e -> apply());
        result.add(btnApply, GBC.eol());

        return result;
    }

    private void useRuler() {
        if (bv.getRulerStart() != null && bv.getRulerEnd() != null) {
            int x = Math.min(bv.getRulerStart().getX(), bv.getRulerEnd().getX());
            spinStartingAnyNWx.setValue(x + 1);
            int y = Math.min(bv.getRulerStart().getY(), bv.getRulerEnd().getY());
            spinStartingAnyNWy.setValue(y + 1);
            x = Math.max(bv.getRulerStart().getX(), bv.getRulerEnd().getX());
            spinStartingAnySEx.setValue(x + 1);
            y = Math.max(bv.getRulerStart().getY(), bv.getRulerEnd().getY());
            spinStartingAnySEy.setValue(y + 1);
        }
    }

    private void apply() {

        player.setConstantInitBonus(getInit());
        player.setNbrMFConventional(getCnvMines());
        player.setNbrMFVibra(getVibMines());
        player.setNbrMFActive(getActMines());
        player.setNbrMFInferno(getInfMines());
        getSkillGenerationOptionsPanel().updateClient();
        player.setEmail(getEmail());

        final GameOptions gOpts = clientgui.getClient().getGame().getOptions();

        // If the game option set_arty_player_home edge is set, adjust the player's offboard arty units to be behind
        // the newly selected home edge.
        OffBoardDirection direction = OffBoardDirection.translateStartPosition(getStartPos());
        if (direction != OffBoardDirection.NONE &&
              gOpts.booleanOption(OptionsConstants.BASE_SET_ARTY_PLAYER_HOME_EDGE)) {
            for (Entity entity : client.getGame().getPlayerEntities(client.getLocalPlayer(), false)) {
                if (entity.getOffBoardDirection() != OffBoardDirection.NONE) {
                    entity.setOffBoard(entity.getOffBoardDistance(), direction);
                }
            }
        }

        // Unit Munition Configuration
        String faction = getFactionCode();
        team.setFaction(faction);
        if (clientgui.chatlounge != null) {
            ArrayList<Entity> updateEntities = clientgui.getClient().getGame().getPlayerEntities(player, false);

            if (null != munitionTree && null != rp) {
                rp.friendlyFaction = faction;
                rp.binFillPercent = (rp.isPirate) ? TeamLoadOutGenerator.UNSET_FILL_RATIO : 1.0f;
                // Clear any bomb assignments
                LobbyMekPopupActions.resetBombChoices(clientgui, client.getGame(), updateEntities);
                tlg.reconfigureEntities(updateEntities, faction, munitionTree, rp, null);
                // Use sendUpdate because we want the Game to allow us to change on Bot's
                // behalf.
                clientgui.chatlounge.sendProxyUpdates(updateEntities, client.getLocalPlayer());
            }
        }

        // The deployment position
        player.setStartingPos(getStartPos());
        player.setStartOffset(getStartOffset());
        player.setStartWidth(getStartWidth());
        player.setStartingAnyNWx(getStartingAnyNWx());
        player.setStartingAnyNWy(getStartingAnyNWy());
        player.setStartingAnySEx(getStartingAnySEx());
        player.setStartingAnySEy(getStartingAnySEy());
        client.sendPlayerInfo(player);
    }

    private JPanel initiativeSection() {
        JPanel result = new OptionPanel("PlayerSettingsDialog.header.initMod");
        Content panContent = new Content(new GridLayout(1, 2, 10, 5));
        result.add(panContent);
        panContent.add(labInit);
        panContent.add(fldInit);
        labInit.setToolTipText(Messages.getString("PlayerSettingsDialog.initModTT"));
        fldInit.setToolTipText(Messages.getString("PlayerSettingsDialog.initModTT"));
        return result;
    }

    private JPanel mineSection() {
        JPanel result = new OptionPanel("PlayerSettingsDialog.header.minefields");
        Content panContent = new Content(new GridLayout(4, 2, 10, 5));
        result.add(panContent);
        panContent.add(labConventional);
        panContent.add(fldConventional);
        panContent.add(labVibrabomb);
        panContent.add(fldVibrabomb);
        panContent.add(labActive);
        panContent.add(fldActive);
        panContent.add(labInferno);
        panContent.add(fldInferno);
        return result;
    }

    private JPanel skillsSection() {
        final JPanel skillsPanel = new OptionPanel("PlayerSettingsDialog.header.skills");
        skillsPanel.setName("skillsPanel");

        skillGenerationOptionsPanel = new SkillGenerationOptionsPanel(clientgui.getFrame(), clientgui, client);
        skillGenerationOptionsPanel.setBorder(BorderFactory.createEmptyBorder(8, 25, 5, 25));
        skillGenerationOptionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        skillsPanel.add(skillGenerationOptionsPanel);

        return skillsPanel;
    }

    private JPanel emailSection() {
        JPanel result = new OptionPanel("PlayerSettingsDialog.header.email");
        Content panContent = new Content(new GridLayout(1, 2, 10, 5));
        result.add(panContent);
        panContent.add(labEmail);
        panContent.add(fldEmail);
        return result;
    }

    private void setupValues() {
        player = client.getLocalPlayer();
        year = clientgui.getClient().getGame().getOptions().intOption(OptionsConstants.ALLOWED_YEAR);

        // For new auto-loadout-configuration
        team = client.getGame().getTeamForPlayer(player);
        tlg = new TeamLoadOutGenerator(clientgui.getClient().getGame());
        originalMT = new MunitionTree();
        originalMT.loadEntityList(client.getGame().getPlayerEntities(player, false));

        fldInit.setText(Integer.toString(player.getConstantInitBonus()));
        fldConventional.setText(Integer.toString(player.getNbrMFConventional()));
        fldVibrabomb.setText(Integer.toString(player.getNbrMFVibra()));
        fldActive.setText(Integer.toString(player.getNbrMFActive()));
        fldInferno.setText(Integer.toString(player.getNbrMFInferno()));
        fldEmail.setText(player.getEmail());
        txtWidth.setText(Integer.toString(player.getStartWidth()));
        txtOffset.setText(Integer.toString(player.getStartOffset()));

        MapSettings ms = clientgui.getClient().getMapSettings();
        int bh = ms.getBoardHeight() * ms.getMapHeight();
        int bw = ms.getBoardWidth() * ms.getMapWidth();

        SpinnerNumberModel mStartingAnyNWx = new SpinnerNumberModel(0, 0, bw, 1);
        spinStartingAnyNWx = new JSpinner(mStartingAnyNWx);
        SpinnerNumberModel mStartingAnyNWy = new SpinnerNumberModel(0, 0, bh, 1);
        spinStartingAnyNWy = new JSpinner(mStartingAnyNWy);
        SpinnerNumberModel mStartingAnySEx = new SpinnerNumberModel(0, 0, bw, 1);
        spinStartingAnySEx = new JSpinner(mStartingAnySEx);
        SpinnerNumberModel mStartingAnySEy = new SpinnerNumberModel(0, -0, bh, 1);
        spinStartingAnySEy = new JSpinner(mStartingAnySEy);

        int x = Math.min(player.getStartingAnyNWx() + 1, bw);
        spinStartingAnyNWx.setValue(x);
        int y = Math.min(player.getStartingAnyNWy() + 1, bh);
        spinStartingAnyNWy.setValue(y);
        x = Math.min(player.getStartingAnySEx() + 1, bw);
        spinStartingAnySEx.setValue(x);
        y = Math.min(player.getStartingAnySEy() + 1, bh);
        spinStartingAnySEy.setValue(y);
    }

    private void setupStartGrid() {
        panStartButtons.setAlignmentX(Component.LEFT_ALIGNMENT);
        // put these fixed zones in first
        for (int i = 0; i < Board.NUM_ZONES; i++) {
            butStartPos.put(i, new TipButton(""));
            butStartPos.get(i).addActionListener(listener);
            butStartPos.get(i).setActionCommand(((Integer) i).toString());
        }

        var currentBoard = clientgui.getClient().getGame().getPhase().isLounge() ?
              ServerBoardHelper.getPossibleGameBoard(clientgui.getClient().getMapSettings(), true) :
              clientgui.getClient().getGame().getBoard();
        var deploymentZones = currentBoard.getCustomDeploymentZones();
        int extraRowCount = (int) Math.ceil(deploymentZones.size() / 3.0);

        panStartButtons.setLayout(new GridLayout(4 + extraRowCount, 3));
        panStartButtons.add(butStartPos.get(1));
        panStartButtons.add(butStartPos.get(2));
        panStartButtons.add(butStartPos.get(3));
        panStartButtons.add(butStartPos.get(8));
        panStartButtons.add(butStartPos.get(10));
        panStartButtons.add(butStartPos.get(4));
        panStartButtons.add(butStartPos.get(7));
        panStartButtons.add(butStartPos.get(6));
        panStartButtons.add(butStartPos.get(5));
        panStartButtons.add(butStartPos.get(0));
        panStartButtons.add(butStartPos.get(9));
        panStartButtons.add(new JLabel("")); // extra spacer as custom deployment zones should start
        // on next line to avoid confusion

        // now we build any custom deployment zones that the board has and add them as
        // buttons
        for (int zoneID : deploymentZones) {
            TipButton buttonCustomZone = new TipButton("Zone " + zoneID);

            StringBuilder zoneBuilder = new StringBuilder();
            zoneBuilder.append("Zone ");
            zoneBuilder.append(zoneID);
            zoneBuilder.append(": ");
            for (Coords coords : currentBoard.getCustomDeploymentZone(zoneID)) {
                zoneBuilder.append(coords.toFriendlyString());
                zoneBuilder.append(", ");
            }

            zoneBuilder.delete(zoneBuilder.length() - 2, zoneBuilder.length() - 2); // chop off last two characters

            buttonCustomZone.setToolTipText(zoneBuilder.toString());

            // the custom zones should not overlap with the fixed zones this includes the deep zones
            int internalZoneID = Board.encodeCustomDeploymentZoneID(zoneID);
            buttonCustomZone.setActionCommand(Integer.valueOf(internalZoneID).toString());
            buttonCustomZone.addActionListener(listener);
            butStartPos.put(internalZoneID, buttonCustomZone);
            panStartButtons.add(buttonCustomZone);
        }

        updateStartGrid();
    }

    /** Assigns texts and tooltips to the starting positions grid. */
    private void updateStartGrid() {
        Map<Integer, StringBuilder> butText = new HashMap<>();
        Map<Integer, StringBuilder> butTT = new HashMap<>();
        Map<Integer, Boolean> hasPlayer = new HashMap<>();

        for (int i : butStartPos.keySet()) {
            butText.put(i, new StringBuilder());
            butTT.put(i, new StringBuilder());
        }

        for (int i : butStartPos.keySet()) {
            butText.get(i).append("<HTML><P ALIGN=CENTER>");
            if (!isValidStartPos(client.getGame(), client.getLocalPlayer(), i)) {
                butText.get(i).append(UIUtil.fontHTML(uiYellow()));
                butTT.get(i).append(Messages.getString("PlayerSettingsDialog.invalidStartPosTT"));
            }

            if (i <= Board.NUM_ZONES) {
                butText.get(i).append(IStartingPositions.START_LOCATION_NAMES[i]);
            } else {
                butText.get(i).append("Zone ").append(Board.decodeCustomDeploymentZoneID(i));
            }
            butText.get(i).append("</FONT><BR>");
        }

        for (Player listedPlayer : client.getGame().getPlayersList()) {
            int pos = listedPlayer.getStartingPos();
            if (!listedPlayer.equals(client.getLocalPlayer()) && (pos != Board.START_ANY)) {
                butText.get(pos).append(UIUtil.fontHTML(teamColor(listedPlayer, client.getLocalPlayer())));
                butText.get(pos).append("\u25A0</FONT>");
                if (!hasPlayer.containsKey(pos)) {
                    if (!butTT.get(pos).isEmpty()) {
                        butTT.get(pos).append("<BR><BR>");
                    }
                    butTT.get(pos).append(Messages.getString("PlayerSettingsDialog.deployingHere"));
                    hasPlayer.put(pos, true);
                }
                butTT.get(pos).append("<BR>").append(listedPlayer.getName());
            }
        }

        butText.get(currentPlayerStartPos).append(UIUtil.fontHTML(GUIPreferences.getInstance().getMyUnitColor()));
        butText.get(currentPlayerStartPos).append("\u2B24</FONT>");

        for (int i : butStartPos.keySet()) {
            butStartPos.get(i).setText(butText.get(i).toString());
            if (!butTT.get(i).isEmpty()) {
                butStartPos.get(i).setToolTipText(butTT.get(i).toString());
            }
        }
    }

    transient ActionListener listener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Deployment buttons
            for (int i : butStartPos.keySet()) {
                if (butStartPos.get(i).equals(e.getSource())) {
                    currentPlayerStartPos = Integer.parseInt(butStartPos.get(i).getActionCommand());
                    updateStartGrid();
                    return; // an action event is unlikely to have come from two separate sources
                }
            }

            // Team configuration
            if (butAutoconfigure.equals(e.getSource())) {
                // disable button until Faction changes; result won't change.
                butRestoreMT.setEnabled(true);
                butAutoconfigure.setEnabled(false);
                butRandomize.setEnabled(true);
                // Set nuke ban state before generating the tree
                rp = tlg.generateParameters(team);
                rp.isPirate = getFactionCode().equalsIgnoreCase("PIR");
                rp.nukesBannedForMe = chkBanNukes.isSelected();
                ArrayList<Entity> entities = clientgui.getClient().getGame().getPlayerEntities(player, false);
                munitionTree = TeamLoadOutGenerator.generateMunitionTree(rp, entities, "");
            } else if (butRandomize.equals(e.getSource())) {
                // Randomize team loadout
                butRestoreMT.setEnabled(true);
                butAutoconfigure.setEnabled(true);
                butRandomize.setEnabled(false);
                tlg.setTrueRandom(chkTrulyRandom.isSelected());
                munitionTree = TeamLoadOutGenerator.generateRandomizedMT();
            } else if (cmbFaction.equals(e.getSource())) {
                // Reset autoconfigure button if user changes faction
                butAutoconfigure.setEnabled(true);
            } else if (butSaveADF.equals(e.getSource())) {
                // Save current MunitionTree off as an ADF file
                if (null != munitionTree) {
                    saveLoadout(munitionTree);
                } else if (null != originalMT) {
                    saveLoadout(originalMT);
                }
            } else if (butLoadADF.equals(e.getSource())) {
                // Load a MunitionTree into munitionTree variable.
                MunitionTree mt = loadLoadout();
                if (null != mt) {
                    munitionTree = mt;
                    butRestoreMT.setEnabled(true);
                }
            } else if (butRestoreMT.equals(e.getSource())) {
                if (null != originalMT) {
                    munitionTree = originalMT;
                }
                // Bot settings button
            } else if (butBotSettings.equals(e.getSource()) && client instanceof Princess) {
                BehaviorSettings behavior = ((Princess) client).getBehaviorSettings();
                var bcd = new BotConfigDialog(clientgui.getFrame(),
                      client.getLocalPlayer().getName(),
                      behavior,
                      clientgui);
                bcd.setVisible(true);
                if (bcd.getResult() == DialogResult.CONFIRMED) {
                    ((Princess) client).setBehaviorSettings(bcd.getBehaviorSettings());
                }
            } else if (e.getActionCommand().equals(CMD_ADD_GROUND_OBJECT)) {
                addGroundObject();
            } else if (e.getActionCommand().contains(CMD_REMOVE_GROUND_OBJECT_PREFIX)) {
                removeGroundObject(e.getActionCommand());
            }
        }
    };

    /**
     * Let user select an ADF file (Autoconfiguration Definition File) from which to load munition loadout imperatives,
     * which can then be applied to selected units.
     *
     * @return {@link MunitionTree} of available options, null otherwise.
     */
    private @Nullable MunitionTree loadLoadout() {
        JFileChooser fc = new JFileChooser(Paths.get(MMConstants.USER_LOADOUTS_DIR).toAbsolutePath().toString());
        FileNameExtensionFilter adfFilter = new FileNameExtensionFilter("adf files (*.adf)", "adf");
        fc.addChoosableFileFilter(adfFilter);
        fc.setFileFilter(adfFilter);
        fc.setLocation(this.getLocation().x + 150, this.getLocation().y + 100);
        fc.setDialogTitle(Messages.getString("ClientGui.LoadoutLoadDialog.title"));

        int returnVal = fc.showOpenDialog(this);
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (fc.getSelectedFile() == null)) {
            // No file selected? No loadout!
            return null;
        }

        String file = fc.getSelectedFile().getAbsolutePath();
        return new MunitionTree(file);
    }

    private void saveLoadout(MunitionTree source) {
        JFileChooser fc = new JFileChooser(Paths.get(MMConstants.USER_LOADOUTS_DIR).toAbsolutePath().toString());
        FileNameExtensionFilter adfFilter = new FileNameExtensionFilter("adf files (*.adf)", "adf");
        fc.addChoosableFileFilter(adfFilter);
        fc.setFileFilter(adfFilter);
        fc.setLocation(this.getLocation().x + 150, this.getLocation().y + 100);
        fc.setDialogTitle(Messages.getString("ClientGui.LoadoutSaveDialog.title"));

        int returnVal = fc.showSaveDialog(this);
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (fc.getSelectedFile() == null)) {
            // No file selected? No loadout!
            return;
        }
        String file = fc.getSelectedFile().getAbsolutePath();
        if (!file.toLowerCase().endsWith(".adf")) {
            file = file + ".adf";
        }
        source.writeToADFFilename(file);
    }

    /**
     * Parse the given field and return the integer it contains or 0, if the field cannot be parsed.
     */
    private int parseField(JTextField field) {
        try {
            return Integer.parseInt(field.getText());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    public FactionRecord getFaction() {
        return (FactionRecord) cmbFaction.getSelectedItem();
    }

    public String getFactionCode() {
        return getFaction().getKey();
    }

    public FactionRecord getFactionFromCode(String code, int year) {
        for (FactionRecord fRec : RATGenerator.getInstance().getFactionList()) {
            if ((!fRec.isMinor()) &&
                  !fRec.getKey().contains(".") &&
                  fRec.isActiveInYear(year) &&
                  fRec.getKey().equals(code)) {
                return fRec;
            }
        }
        return RATGenerator.getInstance().getFaction("IS");
    }

    public void updateFactionChoice(FactionRecord preset) {
        FactionRecord old = (preset == null) ? (FactionRecord) cmbFaction.getSelectedItem() : preset;
        cmbFaction.removeActionListener(listener);
        cmbFaction.removeAllItems();
        List<FactionRecord> recs = new ArrayList<>();
        for (FactionRecord fRec : RATGenerator.getInstance().getFactionList()) {
            if ((!fRec.isMinor()) && !fRec.getKey().contains(".") && fRec.isActiveInYear(year)) {
                recs.add(fRec);
            }
        }
        recs.sort(factionSorter);
        for (FactionRecord fRec : recs) {
            cmbFaction.addItem(fRec);
        }
        cmbFaction.setSelectedItem(old);
        if (cmbFaction.getSelectedItem() == null) {
            cmbFaction.setSelectedItem(RATGenerator.getInstance().getFaction("IS"));
        }
        cmbFaction.addActionListener(listener);
    }

}
