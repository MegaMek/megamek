/*
 * MegaMek - Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing.lobby;

import megamek.MMConstants;
import megamek.client.Client;
import megamek.client.bot.BotClient;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.Princess;
import megamek.client.generator.TeamLoadoutGenerator;
import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.client.ui.baseComponents.AbstractButtonDialog;
import megamek.client.ui.dialogs.BotConfigDialog;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.panels.SkillGenerationOptionsPanel;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.boardview.BoardView;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.containers.MunitionTree;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import org.apache.commons.collections.IteratorUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import static megamek.client.ui.Messages.getString;
import static megamek.client.ui.swing.lobby.LobbyUtility.isValidStartPos;
import static megamek.client.ui.swing.util.UIUtil.*;

/**
 * A dialog that can be used to adjust advanced player settings like initiative,
 * minefields, and maybe other things in the future like force abilities.
 *
 * @author Jay Lawson
 * @author Simon (Juliez)
 */
public class PlayerSettingsDialog extends AbstractButtonDialog {

    public PlayerSettingsDialog(ClientGUI cg, Client cl, BoardView bv) {
        super(cg.frame, "PlayerSettingsDialog", "PlayerSettingsDialog.title");
        client = cl;
        clientgui = cg;
        this.bv = bv;
        currentPlayerStartPos = cl.getLocalPlayer().getStartingPos();
        if (currentPlayerStartPos > 10) {
            currentPlayerStartPos -= 10;
        }

        numFormatter.setMinimum(0);
        numFormatter.setCommitsOnValidEdit(true);

        initialize();
    }

    @Override
    protected void finalizeInitialization() throws Exception {
        adaptToGUIScale();
        super.finalizeInitialization();
    }

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

    private final Client client;
    private final ClientGUI clientgui;
    private final BoardView bv;

    // Initiative Section
    private final JLabel labInit = new TipLabel(Messages.getString("PlayerSettingsDialog.initMod"), SwingConstants.RIGHT);
    private final TipTextField fldInit = new TipTextField(3);

    // Mines Section
    private final JLabel labConventional = new JLabel(getString("PlayerSettingsDialog.labConventional"), SwingConstants.RIGHT);
    private final JLabel labVibrabomb = new JLabel(getString("PlayerSettingsDialog.labVibrabomb"), SwingConstants.RIGHT);
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
    private final TipButton[] butStartPos = new TipButton[11];
    // this might seem like kind of a dumb way to declare it, but JFormattedTextField doesn't have an overload that
    // takes both a number formatter and a default value.
    private final NumberFormatter numFormatter = new NumberFormatter(NumberFormat.getIntegerInstance());
    private final DefaultFormatterFactory formatterFactory = new DefaultFormatterFactory(numFormatter);
    private final JFormattedTextField txtOffset = new JFormattedTextField(formatterFactory, 0);
    private final JFormattedTextField txtWidth = new JFormattedTextField(formatterFactory, 3);
    private JSpinner spinStartingAnyNWx;
    private JSpinner spinStartingAnyNWy;
    private JSpinner spinStartingAnySEx;
    private JSpinner spinStartingAnySEy;

    // Bot Settings Section
    private final JButton butBotSettings = new JButton(Messages.getString("PlayerSettingsDialog.botSettings"));

    // Team Configuration Section
    private Team team;
    private final JLabel labelAutoconfig = new TipLabel(
            Messages.getString("PlayerSettingsDialog.autoConfigFaction"), SwingConstants.LEFT);
    private JComboBox<String> cmbTeamFaction;
    private final JButton butAutoconfigure = new JButton(Messages.getString("PlayerSettingsDialog.autoConfig"));
    private final JButton butRandomize = new JButton(Messages.getString("PlayerSettingsDialog.randomize"));
    private Checkbox chkTrulyRandom = new Checkbox("Truly Random", false);
    private final JButton butSaveADF = new JButton(Messages.getString("PlayerSettingsDialog.saveADF"));
    private final JButton butLoadADF = new JButton(Messages.getString("PlayerSettingsDialog.loadADF"));
    private final JButton butRestoreMT = new JButton(Messages.getString("PlayerSettingsDialog.restore"));
    private TeamLoadoutGenerator tlg;
    private MunitionTree munitionTree = null;
    private MunitionTree originalMT = null;

    private int currentPlayerStartPos;

    @Override
    protected Container createCenterPane() {
        setupValues();

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(headerSection());
        mainPanel.add(teamConfigSection());
        if (client instanceof BotClient) {
            mainPanel.add(botSection());
        }
        mainPanel.add(startSection());
        mainPanel.add(initiativeSection());
        if (client.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_MINEFIELDS)) {
            mainPanel.add(mineSection());
        }
        mainPanel.add(skillsSection());
        if (!(client instanceof BotClient)) {
            mainPanel.add(emailSection());
        }
        mainPanel.add(Box.createVerticalGlue());

        var scrMain = new JScrollPane(mainPanel);
        scrMain.getVerticalScrollBar().setUnitIncrement(16);
        scrMain.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrMain.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
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

    private JPanel teamConfigSection() {
        JPanel result = new OptionPanel("PlayerSettingsDialog.header.teamConfig");
        Content panContent = new Content(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        result.add(panContent);

        panContent.add(labelAutoconfig, gbc);
        panContent.add(cmbTeamFaction, gbc);
        cmbTeamFaction.addActionListener(listener);
        panContent.add(butAutoconfigure, gbc);
        butAutoconfigure.addActionListener(listener);
        panContent.add(butRandomize, gbc);
        butRandomize.addActionListener(listener);
        panContent.add(chkTrulyRandom, gbc);
        panContent.add(butSaveADF, gbc);
        butSaveADF.addActionListener(listener);
        panContent.add(butLoadADF, gbc);
        butLoadADF.addActionListener(listener);
        panContent.add(butRestoreMT, gbc);
        butRestoreMT.addActionListener(listener);
        butRestoreMT.setEnabled(false);
        return result;
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

        JLabel lblOffset = new JLabel(Messages.getString("CustomMechDialog.labDeploymentOffset"));
        lblOffset.setToolTipText(Messages.getString("CustomMechDialog.labDeploymentOffsetTip"));
        JLabel lblWidth = new JLabel(Messages.getString("CustomMechDialog.labDeploymentWidth"));
        lblWidth.setToolTipText(Messages.getString("CustomMechDialog.labDeploymentWidthTip"));

        txtOffset.setColumns(4);
        txtWidth.setColumns(4);

        result.add(lblOffset, GBC.std());
        result.add(txtOffset, GBC.eol());
        result.add(lblWidth, GBC.std());
        result.add(txtWidth, GBC.eol());

        result.add(new JLabel(Messages.getString("CustomMechDialog.labDeploymentAnyNW")), GBC.std());
        result.add(spinStartingAnyNWx, GBC.std());
        result.add(spinStartingAnyNWy, GBC.eol());
        result.add(new JLabel(Messages.getString("CustomMechDialog.labDeploymentAnySE")), GBC.std());
        result.add(spinStartingAnySEx, GBC.std());
        result.add(spinStartingAnySEy, GBC.eol());

        JButton btnUseRuler = new JButton(Messages.getString("CustomMechDialog.BtnDeploymentUseRuler"));
        btnUseRuler.setToolTipText(Messages.getString("CustomMechDialog.BtnDeploymentUseRulerTip"));
        btnUseRuler.addActionListener(e -> useRuler());
        result.add(btnUseRuler, GBC.std());
        JButton btnApply = new JButton(Messages.getString("CustomMechDialog.BtnDeploymentApply"));
        btnApply.setToolTipText(Messages.getString("CustomMechDialog.BtnDeploymentApplyTip"));
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
        Player player = client.getLocalPlayer();

        player.setConstantInitBonus(getInit());
        player.setNbrMFConventional(getCnvMines());
        player.setNbrMFVibra(getVibMines());
        player.setNbrMFActive(getActMines());
        player.setNbrMFInferno(getInfMines());
        getSkillGenerationOptionsPanel().updateClient();
        player.setEmail(getEmail());

        final GameOptions gOpts = clientgui.getClient().getGame().getOptions();

        // If the gameoption set_arty_player_homeedge is set, adjust the player's offboard
        // arty units to be behind the newly selected home edge.
        OffBoardDirection direction = OffBoardDirection.translateStartPosition(getStartPos());
        if (direction != OffBoardDirection.NONE &&
                gOpts.booleanOption(OptionsConstants.BASE_SET_ARTY_PLAYER_HOMEEDGE)) {
            for (Entity entity: client.getGame().getPlayerEntities(client.getLocalPlayer(), false)) {
                if (entity.getOffBoardDirection() != OffBoardDirection.NONE) {
                    entity.setOffBoard(entity.getOffBoardDistance(), direction);
                }
            }
        }

        // Team Configuration
        String faction = (String) cmbTeamFaction.getSelectedItem();
        team.setFaction(faction);
        if (null != munitionTree) {
            // TODO: create and set up default adf file path for bots
            tlg.reconfigureTeam(team, faction, munitionTree);

        }

        // The deployment position
        player.setStartingPos(getStartPos());
        player.setStartOffset(getStartOffset());
        player.setStartWidth(getStartWidth());
        player.setStartingAnyNWx(getStartingAnyNWx());
        player.setStartingAnyNWy(getStartingAnyNWy());
        player.setStartingAnySEx(getStartingAnySEx());
        player.setStartingAnySEy(getStartingAnySEy());
        client.sendPlayerInfo();
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
        Player player = client.getLocalPlayer();

        // For new auto-loadout-configuration
        team = client.getGame().getTeamForPlayer(player);
        String faction = team.getFaction();
        tlg = new TeamLoadoutGenerator(clientgui);
        originalMT = new MunitionTree();
        originalMT.loadEntityList(
                (ArrayList<Entity>) IteratorUtils.toList(client.getGame().getTeamEntities(team)
                ));

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

        SpinnerNumberModel mStartingAnyNWx = new SpinnerNumberModel(0, 0,bw, 1);
        spinStartingAnyNWx = new JSpinner(mStartingAnyNWx);
        SpinnerNumberModel mStartingAnyNWy = new SpinnerNumberModel(0, 0, bh, 1);
        spinStartingAnyNWy = new JSpinner(mStartingAnyNWy);
        SpinnerNumberModel mStartingAnySEx = new SpinnerNumberModel(0, 0, bw, 1);
        spinStartingAnySEx = new JSpinner(mStartingAnySEx);
        SpinnerNumberModel mStartingAnySEy = new SpinnerNumberModel(0, -0, bh, 1);
        spinStartingAnySEy = new JSpinner(mStartingAnySEy);

        cmbTeamFaction = new JComboBox<String>(ITechnology.MM_FACTION_CODES);
        cmbTeamFaction.setSelectedItem(faction);

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
        for (int i = 0; i < 11; i++) {
            butStartPos[i] = new TipButton("");
            butStartPos[i].addActionListener(listener);
        }
        panStartButtons.setLayout(new GridLayout(4, 3));
        panStartButtons.add(butStartPos[1]);
        panStartButtons.add(butStartPos[2]);
        panStartButtons.add(butStartPos[3]);
        panStartButtons.add(butStartPos[8]);
        panStartButtons.add(butStartPos[10]);
        panStartButtons.add(butStartPos[4]);
        panStartButtons.add(butStartPos[7]);
        panStartButtons.add(butStartPos[6]);
        panStartButtons.add(butStartPos[5]);
        panStartButtons.add(butStartPos[0]);
        panStartButtons.add(butStartPos[9]);
        updateStartGrid();
    }

    /** Assigns texts and tooltips to the starting positions grid. */
    private void updateStartGrid() {
        StringBuilder[] butText = new StringBuilder[11];
        StringBuilder[] butTT = new StringBuilder[11];
        boolean[] hasPlayer = new boolean[11];

        for (int i = 0; i < 11; i++) {
            butText[i] = new StringBuilder();
            butTT[i] = new StringBuilder();
        }

        for (int i = 0; i < 11; i++) {
            butText[i].append("<HTML><P ALIGN=CENTER>");
            if (!isValidStartPos(client.getGame(), client.getLocalPlayer(), i)) {
                butText[i].append(guiScaledFontHTML(uiYellow()));
                butTT[i].append(Messages.getString("PlayerSettingsDialog.invalidStartPosTT"));
            } else {
                butText[i].append(guiScaledFontHTML());
            }
            butText[i].append(IStartingPositions.START_LOCATION_NAMES[i]).append("</FONT><BR>");
        }

        for (Player player : client.getGame().getPlayersVector()) {
            int pos = player.getStartingPos();
            if (!player.equals(client.getLocalPlayer()) && (pos >= 0) && (pos <= 19)) {
                int index = pos > 10 ? pos - 10 : pos;
                butText[index].append(guiScaledFontHTML(teamColor(player, client.getLocalPlayer())));
                butText[index].append("\u25A0</FONT>");
                if (!hasPlayer[index]) {
                    if (butTT[index].length() > 0) {
                        butTT[index].append("<BR><BR>");
                    }
                    butTT[index].append(Messages.getString("PlayerSettingsDialog.deployingHere"));
                    hasPlayer[index] = true;
                }
                butTT[index].append("<BR>").append(player.getName());
            }
        }

        butText[currentPlayerStartPos].append(guiScaledFontHTML(GUIPreferences.getInstance().getMyUnitColor()));
        butText[currentPlayerStartPos].append("\u2B24</FONT>");

        for (int i = 0; i < 11; i++) {
            butStartPos[i].setText(butText[i].toString());
            if (butTT[i].length() > 0) {
                butStartPos[i].setToolTipText(butTT[i].toString());
            }
        }
    }

    ActionListener listener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Deployment buttons
            for (int i = 0; i < 11; i++) {
                if (butStartPos[i].equals(e.getSource())) {
                    currentPlayerStartPos = i;
                    updateStartGrid();
                }
            }

            // Team configuration
            if (butAutoconfigure.equals(e.getSource())) {
                // disable button until Faction changes; result won't change.
                butRestoreMT.setEnabled(true);
                butAutoconfigure.setEnabled(false);
                butRandomize.setEnabled(true);
                munitionTree = tlg.generateMunitionTree(tlg.generateParameters(team), team);
            }

            if (butRandomize.equals(e.getSource())) {
                // Randomize team loadout
                butRestoreMT.setEnabled(true);
                butAutoconfigure.setEnabled(true);
                butRandomize.setEnabled(false);
                tlg.setTrueRandom(chkTrulyRandom.getState());
                munitionTree = TeamLoadoutGenerator.generateRandomizedMT();
            }

            if (cmbTeamFaction.equals(e.getSource())) {
                // Reset autoconfigure button if user changes faction
                butAutoconfigure.setEnabled(true);
            }

            if (butSaveADF.equals(e.getSource())) {
                // Save current MunitionTree off as an ADF file
                if (null != munitionTree) {
                    saveLoadout(munitionTree);
                } else if (null != originalMT) {
                    saveLoadout(originalMT);
                }
            }

            if (butLoadADF.equals(e.getSource())) {
                // Load a MunitionTree into munitionTree variable.
                MunitionTree mt = loadLoadout();
                if (null != mt) {
                    munitionTree = mt;
                    butRestoreMT.setEnabled(true);
                }
            }

            if (butRestoreMT.equals(e.getSource())) {
                if (null != originalMT) {
                    munitionTree = originalMT;
                }

            }

            // Bot settings button
            if (butBotSettings.equals(e.getSource()) && client instanceof Princess) {
                BehaviorSettings behavior = ((Princess) client).getBehaviorSettings();
                var bcd = new BotConfigDialog(clientgui.frame, client.getLocalPlayer().getName(), behavior, clientgui);
                bcd.setVisible(true);
                if (bcd.getResult() == DialogResult.CONFIRMED) {
                    ((Princess) client).setBehaviorSettings(bcd.getBehaviorSettings());
                }
            }
        }
    };

    private MunitionTree loadLoadout() {
        MunitionTree mt = null;
        JFileChooser fc = new JFileChooser(MMConstants.USER_LOADOUTS_DIR);
        fc.setLocation(this.getLocation().x + 150, this.getLocation().y + 100);
        fc.setDialogTitle(Messages.getString("ClientGui.LoadoutLoadDialog.title"));

        int returnVal = fc.showOpenDialog(this);
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (fc.getSelectedFile() == null)) {
            // No file selected?  No loadout!
            return null;
        }

        if (fc.getSelectedFile() != null) {
            String file = fc.getSelectedFile().getAbsolutePath();
            if (null == munitionTree) {
                munitionTree = new MunitionTree(file);
            } else {
                munitionTree.readFromADFFilename(file);
            }
        }
        return mt;
    }

    private void saveLoadout(MunitionTree source) {
        //ignoreHotKeys = true;
        JFileChooser fc = new JFileChooser(MMConstants.USER_LOADOUTS_DIR);
        fc.setLocation(this.getLocation().x + 150, this.getLocation().y + 100);
        fc.setDialogTitle(Messages.getString("ClientGui.LoadoutSaveDialog.title"));

        int returnVal = fc.showSaveDialog(this);
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (fc.getSelectedFile() == null)) {
            // No file selected?  No loadout!
            return;
        }
        if (fc.getSelectedFile() != null) {
            String file = fc.getSelectedFile().getAbsolutePath();
            source.writeToADFFilename(file);
        }
    }

    /**
     * Parse the given field and return the integer it contains or 0, if
     * the field cannot be parsed.
     */
    private int parseField(JTextField field) {
        try {
            return Integer.parseInt(field.getText());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
    private void adaptToGUIScale() {
        UIUtil.adjustDialog(this,  UIUtil.FONT_SCALE1);
    }
}
