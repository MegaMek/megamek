/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import jakarta.annotation.Nullable;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.dialogs.BotCommands.HexTargetPicker;
import megamek.client.ui.util.FlatLafStyleBuilder;
import megamek.client.ui.util.UIUtil;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.enums.NuclearWeaponType;
import megamek.logging.MMLogger;
import megamek.server.nuclear.NuclearBlastProfile;

/**
 * The Game Master's nuclear strike order: where, which warhead, and whose side fired it. It shows the warhead's figures
 * from the same {@link NuclearBlastProfile} the server resolves, so what the dialog promises is what the blast does,
 * and it sends the same {@code /ns} or {@code /nsc} command a Game Master could type.
 */
public class NuclearStrikeDialog extends JDialog {
    private static final MMLogger LOGGER = MMLogger.create(NuclearStrikeDialog.class);

    private static final Color HAZARD_YELLOW = new Color(224, 176, 42);
    private static final Color HAZARD_DARK = new Color(30, 31, 34);
    private static final Color HAZARD_STRIPE = new Color(40, 37, 26);
    private static final Color STRIKE_RED = new Color(179, 38, 30);

    /** Blast damage at which a dry hex becomes rough (IO:AE p.165); mirrors the resolver. */
    private static final int ROUGH_TERRAIN_DAMAGE = 200;

    private static final String BOOK_CARD = "book";
    private static final String CUSTOM_CARD = "custom";

    private final ClientGUI clientGUI;
    private final JTextField hexField = new JTextField(6);
    private final JComboBox<WarheadChoice> warheadChoice = new JComboBox<>();
    private final JComboBox<PlayerChoice> playerChoice = new JComboBox<>();
    private final JPanel warheadCards = new JPanel(new CardLayout());
    private final JPanel statsPanel = new JPanel(new GridBagLayout());
    private final JLabel impactValue = new JLabel();
    private final JLabel dropValue = new JLabel();
    private final JLabel blastRadiusValue = new JLabel();
    private final JLabel secondaryValue = new JLabel();
    private final JLabel craterValue = new JLabel();
    private final JLabel roughValue = new JLabel();
    private final JLabel reachLabel = new JLabel();
    private final JLabel customRadiusLabel = new JLabel();
    private final JLabel errorLabel = new JLabel(" ");
    private final JSpinner impactSpinner = new JSpinner(new SpinnerNumberModel(1_000, 1, 1_000_000, 100));
    private final JSpinner dropSpinner = new JSpinner(new SpinnerNumberModel(23, 1, 1_000_000, 1));
    private final JSpinner secondarySpinner = new JSpinner(new SpinnerNumberModel(85, 1, 1_000, 1));
    private final JSpinner craterSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 9, 1));

    /**
     * @param clientGUI  the client GUI the dialog belongs to
     * @param groundZero the hex that was right-clicked, or {@code null} when opened from the Commands button
     */
    public NuclearStrikeDialog(ClientGUI clientGUI, @Nullable Coords groundZero) {
        super(clientGUI.getFrame(), Messages.getString("NuclearStrike.dialog.title"), false);
        this.clientGUI = clientGUI;
        if (groundZero != null) {
            hexField.setText(groundZero.getBoardNum());
        }
        buildLayout();
        refreshFigures();
        pack();
        setLocationRelativeTo(clientGUI.getFrame());
    }

    private void buildLayout() {
        JPanel content = new JPanel(new BorderLayout());
        content.add(new HazardBanner(), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        int gutter = UIUtil.scaleForGUI(12);
        body.setBorder(new EmptyBorder(gutter, gutter, UIUtil.scaleForGUI(4), gutter));
        body.add(groundZeroSection());
        body.add(verticalGap());
        body.add(warheadSection());
        body.add(verticalGap());
        body.add(firedBySection());
        content.add(body, BorderLayout.CENTER);
        content.add(footer(), BorderLayout.SOUTH);
        setContentPane(content);
    }

    private JComponent verticalGap() {
        JPanel gap = new JPanel();
        gap.setPreferredSize(UIUtil.scaleForGUI(1, 8));
        gap.setMaximumSize(UIUtil.scaleForGUI(4000, 8));
        return gap;
    }

    private JPanel section(String titleKey) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createTitledBorder(Messages.getString(titleKey)),
              new EmptyBorder(UIUtil.scaleForGUI(2), UIUtil.scaleForGUI(6), UIUtil.scaleForGUI(6),
                    UIUtil.scaleForGUI(6))));
        return panel;
    }

    private GridBagConstraints cell(int column, int row) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(UIUtil.scaleForGUI(2), 0, UIUtil.scaleForGUI(2), UIUtil.scaleForGUI(8));
        return constraints;
    }

    private GridBagConstraints wideCell(int row) {
        GridBagConstraints constraints = cell(0, row);
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        return constraints;
    }

    private JPanel groundZeroSection() {
        JPanel panel = section("NuclearStrike.dialog.groundZero");
        panel.add(new JLabel(Messages.getString("NuclearStrike.dialog.hex")), cell(0, 0));
        new FlatLafStyleBuilder().font(UIManager.getFont("monospaced.font")).apply(hexField);
        hexField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                refreshFigures();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                refreshFigures();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                refreshFigures();
            }
        });
        panel.add(hexField, cell(1, 0));
        JButton pickButton = new JButton(Messages.getString("NuclearStrike.dialog.pick"));
        pickButton.addActionListener(event -> pickOnMap());
        GridBagConstraints pickCell = cell(2, 0);
        pickCell.weightx = 1;
        panel.add(pickButton, pickCell);
        return panel;
    }

    private JPanel warheadSection() {
        JPanel panel = section("NuclearStrike.dialog.warhead");
        panel.add(new JLabel(Messages.getString("NuclearStrike.dialog.weapon")), cell(0, 0));
        for (NuclearWeaponType weaponType : NuclearWeaponType.values()) {
            warheadChoice.addItem(new WarheadChoice(weaponType));
        }
        warheadChoice.addItem(new WarheadChoice(null));
        warheadChoice.setSelectedIndex(NuclearWeaponType.DAVY_CROCKETT_M.ordinal());
        warheadChoice.addActionListener(event -> refreshFigures());
        GridBagConstraints choiceCell = cell(1, 0);
        choiceCell.fill = GridBagConstraints.HORIZONTAL;
        choiceCell.weightx = 1;
        panel.add(warheadChoice, choiceCell);

        addStat(0, 0, "NuclearStrike.dialog.impact", impactValue);
        addStat(1, 0, "NuclearStrike.dialog.dropPerHex", dropValue);
        addStat(0, 1, "NuclearStrike.dialog.blastRadius", blastRadiusValue);
        addStat(1, 1, "NuclearStrike.dialog.secondary", secondaryValue);
        addStat(0, 2, "NuclearStrike.dialog.crater", craterValue);
        addStat(1, 2, "NuclearStrike.dialog.rough", roughValue);
        JPanel bookCard = new JPanel();
        bookCard.setLayout(new BoxLayout(bookCard, BoxLayout.Y_AXIS));
        statsPanel.setAlignmentX(LEFT_ALIGNMENT);
        bookCard.add(statsPanel);
        reachLabel.setAlignmentX(LEFT_ALIGNMENT);
        reachLabel.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, UIUtil.scaleForGUI(3), 0, 0, HAZARD_YELLOW),
              new EmptyBorder(UIUtil.scaleForGUI(4), UIUtil.scaleForGUI(6), UIUtil.scaleForGUI(4), 0)));
        bookCard.add(reachLabel);
        JLabel sourceLabel = new JLabel(Messages.getString("NuclearStrike.dialog.source"));
        new FlatLafStyleBuilder().size(0.85).apply(sourceLabel);
        sourceLabel.setAlignmentX(LEFT_ALIGNMENT);
        bookCard.add(sourceLabel);
        warheadCards.add(bookCard, BOOK_CARD);
        warheadCards.add(customCard(), CUSTOM_CARD);
        panel.add(warheadCards, wideCell(1));
        return panel;
    }

    private JPanel customCard() {
        JPanel card = new JPanel(new GridBagLayout());
        addSpinnerRow(card, 0, "NuclearStrike.dialog.impact", impactSpinner);
        addSpinnerRow(card, 1, "NuclearStrike.dialog.dropPerHex", dropSpinner);
        addSpinnerRow(card, 2, "NuclearStrike.dialog.secondaryRadius", secondarySpinner);
        addSpinnerRow(card, 3, "NuclearStrike.dialog.craterDepth", craterSpinner);
        new FlatLafStyleBuilder().size(0.85).apply(customRadiusLabel);
        card.add(customRadiusLabel, wideCell(4));
        return card;
    }

    private void addSpinnerRow(JPanel card, int row, String labelKey, JSpinner spinner) {
        card.add(new JLabel(Messages.getString(labelKey)), cell(0, row));
        spinner.addChangeListener(event -> refreshFigures());
        spinner.setPreferredSize(UIUtil.scaleForGUI(110, 24));
        GridBagConstraints spinnerCell = cell(1, row);
        spinnerCell.weightx = 1;
        card.add(spinner, spinnerCell);
    }

    private JPanel firedBySection() {
        JPanel panel = section("NuclearStrike.dialog.firedBy");
        panel.add(new JLabel(Messages.getString("NuclearStrike.dialog.player")), cell(0, 0));
        playerChoice.addItem(new PlayerChoice(Player.PLAYER_NONE,
              Messages.getString("NuclearStrike.dialog.noPlayer")));
        for (Player player : clientGUI.getClient().getGame().getPlayersList()) {
            playerChoice.addItem(new PlayerChoice(player.getId(),
                  Messages.getString("NuclearStrike.dialog.playerEntry", player.getId(), player.getName())));
        }
        GridBagConstraints choiceCell = cell(1, 0);
        choiceCell.fill = GridBagConstraints.HORIZONTAL;
        choiceCell.weightx = 1;
        panel.add(playerChoice, choiceCell);
        JLabel helpLabel = new JLabel("<html><div style='width:" + UIUtil.scaleForGUI(380) + "px'>"
              + Messages.getString("NuclearStrike.dialog.firedByHelp") + "</div></html>");
        new FlatLafStyleBuilder().size(0.85).apply(helpLabel);
        panel.add(helpLabel, wideCell(1));
        return panel;
    }

    private JPanel footer() {
        JPanel footer = new JPanel(new BorderLayout(UIUtil.scaleForGUI(8), 0));
        int gutter = UIUtil.scaleForGUI(12);
        footer.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Separator.foreground")),
              new EmptyBorder(UIUtil.scaleForGUI(8), gutter, UIUtil.scaleForGUI(10), gutter)));
        JPanel notes = new JPanel();
        notes.setLayout(new BoxLayout(notes, BoxLayout.Y_AXIS));
        JLabel timingLabel = new JLabel(Messages.getString("NuclearStrike.dialog.timing"));
        new FlatLafStyleBuilder().size(0.85).apply(timingLabel);
        notes.add(timingLabel);
        errorLabel.setForeground(UIManager.getColor("Component.error.focusedBorderColor"));
        notes.add(errorLabel);
        footer.add(notes, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, UIUtil.scaleForGUI(8), 0));
        JButton cancelButton = new JButton(Messages.getString("NuclearStrike.dialog.cancel"));
        cancelButton.addActionListener(event -> dispose());
        JButton authorizeButton = new JButton(Messages.getString("NuclearStrike.dialog.authorize"));
        authorizeButton.setBackground(STRIKE_RED);
        authorizeButton.setForeground(Color.WHITE);
        new FlatLafStyleBuilder().bold().apply(authorizeButton);
        authorizeButton.addActionListener(event -> authorize());
        buttons.add(cancelButton);
        buttons.add(authorizeButton);
        footer.add(buttons, BorderLayout.EAST);
        getRootPane().setDefaultButton(cancelButton);
        return footer;
    }

    /** @return the hex in the Ground Zero field, or {@code null} when it does not name a hex on the map */
    private @Nullable Coords groundZero() {
        String text = hexField.getText().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            Coords coords = Coords.parseHexNumber(text);
            return board().contains(coords) ? coords : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private Board board() {
        return clientGUI.getClient().getGame().getBoard(0);
    }

    private WarheadChoice selectedWarhead() {
        return (WarheadChoice) warheadChoice.getSelectedItem();
    }

    private NuclearBlastProfile selectedProfile() {
        NuclearWeaponType weaponType = selectedWarhead().weaponType();
        if (weaponType != null) {
            return NuclearBlastProfile.groundBurst(weaponType);
        }
        return NuclearBlastProfile.custom((int) impactSpinner.getValue(), (int) dropSpinner.getValue(),
              (int) secondarySpinner.getValue(), (int) craterSpinner.getValue());
    }

    /** Shows the selected warhead's figures and how much of this map its blast reaches. */
    private void refreshFigures() {
        boolean isCustom = selectedWarhead().weaponType() == null;
        ((CardLayout) warheadCards.getLayout()).show(warheadCards, isCustom ? CUSTOM_CARD : BOOK_CARD);
        NuclearBlastProfile profile = selectedProfile();
        if (isCustom) {
            customRadiusLabel.setText(Messages.getString("NuclearStrike.dialog.customRadius",
                  profile.impactDamage(), profile.damageReduction(), profile.blastRadius()));
        } else {
            fillStats(profile);
        }
        reachLabel.setText(reachText(profile));
        errorLabel.setText(" ");
        pack();
    }

    /** Puts the warhead's figures into the labels built once in {@link #warheadSection()}. */
    private void fillStats(NuclearBlastProfile profile) {
        String crater = (profile.craterDepth() == 0)
              ? Messages.getString("NuclearStrike.dialog.none")
              : Messages.getString("NuclearStrike.dialog.craterSize", profile.craterDepth(), profile.craterRadius());
        int roughRing = lastRoughRing(profile);
        String rough = (roughRing < 0)
              ? Messages.getString("NuclearStrike.dialog.none")
              : Messages.getString("NuclearStrike.dialog.hexes", roughRing);
        impactValue.setText(String.format("%,d", profile.impactDamage()));
        dropValue.setText(String.format("%,d", profile.damageReduction()));
        blastRadiusValue.setText(Messages.getString("NuclearStrike.dialog.hexes", profile.blastRadius()));
        secondaryValue.setText(Messages.getString("NuclearStrike.dialog.hexes", profile.secondaryRadius()));
        craterValue.setText(crater);
        roughValue.setText(rough);
    }

    private void addStat(int column, int row, String labelKey, JLabel valueLabel) {
        JLabel nameLabel = new JLabel(Messages.getString(labelKey));
        nameLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        GridBagConstraints nameCell = cell(column * 2, row);
        statsPanel.add(nameLabel, nameCell);
        GridBagConstraints valueCell = cell((column * 2) + 1, row);
        valueCell.anchor = GridBagConstraints.EAST;
        valueCell.insets.right = UIUtil.scaleForGUI(18);
        statsPanel.add(valueLabel, valueCell);
    }

    /**
     * @return the farthest ring the blast turns rough, or -1 for none; the same test the resolver applies to each ring
     */
    private static int lastRoughRing(NuclearBlastProfile profile) {
        int ring = -1;
        while (profile.damageAt(ring + 1) >= ROUGH_TERRAIN_DAMAGE) {
            ring++;
        }
        return ring;
    }

    private String reachText(NuclearBlastProfile profile) {
        Coords groundZero = groundZero();
        Board board = board();
        if (groundZero == null) {
            return Messages.getString("NuclearStrike.dialog.reachNoHex");
        }
        int farthest = 0;
        Coords[] corners = { new Coords(0, 0), new Coords(board.getWidth() - 1, 0),
                             new Coords(0, board.getHeight() - 1),
                             new Coords(board.getWidth() - 1, board.getHeight() - 1) };
        for (Coords corner : corners) {
            farthest = Math.max(farthest, groundZero.distance(corner));
        }
        if (profile.blastRadius() > farthest) {
            return Messages.getString("NuclearStrike.dialog.reachWhole", board.getWidth(), board.getHeight(),
                  groundZero.getBoardNum());
        }
        return Messages.getString("NuclearStrike.dialog.reachPart", profile.blastRadius(), farthest,
              board.getWidth(), board.getHeight());
    }

    /** Hides the dialog while the Game Master clicks Ground Zero on the board, then brings it back. */
    private void pickOnMap() {
        BoardView boardView = clientGUI.getCurrentBoardView()
              .filter(BoardView.class::isInstance)
              .map(BoardView.class::cast)
              .orElse(null);
        if (boardView == null) {
            LOGGER.info("[Nukes] Pick on map unavailable: no board view is showing");
            return;
        }
        setVisible(false);
        HexTargetPicker picker = new HexTargetPicker(clientGUI, boardView,
              Messages.getString("NuclearStrike.dialog.pickPrompt"), true, 1, hexNumbers -> {
            hexField.setText(hexNumbers);
            setVisible(true);
        });
        picker.setOnCancelled(() -> setVisible(true));
        picker.start();
    }

    /** Sends the strike as the same command a Game Master could type, then closes. */
    private void authorize() {
        Coords groundZero = groundZero();
        if (groundZero == null) {
            errorLabel.setText(Messages.getString("NuclearStrike.dialog.badHex"));
            return;
        }
        int column = groundZero.getX() + 1;
        int row = groundZero.getY() + 1;
        NuclearWeaponType weaponType = selectedWarhead().weaponType();
        StringBuilder command = new StringBuilder();
        if (weaponType != null) {
            command.append("/ns x=").append(column).append(" y=").append(row).append(" type=").append(weaponType.name());
        } else {
            command.append("/nsc x=").append(column).append(" y=").append(row)
                  .append(" dmg=").append(impactSpinner.getValue())
                  .append(" deg=").append(dropSpinner.getValue())
                  .append(" radius=").append(secondarySpinner.getValue())
                  .append(" depth=").append(craterSpinner.getValue());
        }
        PlayerChoice firingPlayer = (PlayerChoice) playerChoice.getSelectedItem();
        if ((firingPlayer != null) && (firingPlayer.playerId() != Player.PLAYER_NONE)) {
            command.append(" playerID=").append(firingPlayer.playerId());
        }
        LOGGER.info("[Nukes] Strike dialog sent: {}", command);
        clientGUI.getClient().sendChat(command.toString());
        dispose();
    }

    /** A warhead in the list: a standard weapon, or {@code null} for a custom device. */
    private record WarheadChoice(@Nullable NuclearWeaponType weaponType) {
        @Override
        public String toString() {
            return (weaponType == null)
                  ? Messages.getString("NuclearStrike.dialog.custom")
                  : Messages.getString("NuclearStrike.dialog.weaponEntry", weaponType.getDisplayName(),
                  weaponType.getDelivery());
        }
    }

    /** A player in the "Fired by" list, shown as "# - Name". */
    private record PlayerChoice(int playerId, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    /** The striped hazard band with the radiation trefoil, the title and the release authority. */
    private static final class HazardBanner extends JPanel {
        private static final int STRIPE_WIDTH = 10;

        HazardBanner() {
            super(new BorderLayout());
            int padding = UIUtil.scaleForGUI(12);
            int trefoilSpace = UIUtil.scaleForGUI(64);
            setBorder(BorderFactory.createCompoundBorder(
                  BorderFactory.createMatteBorder(0, 0, UIUtil.scaleForGUI(3), 0, HAZARD_YELLOW),
                  new EmptyBorder(padding, trefoilSpace, padding, padding)));
            JPanel titles = new JPanel();
            titles.setOpaque(false);
            titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
            JLabel title = new JLabel(Messages.getString("NuclearStrike.dialog.banner"));
            title.setForeground(Color.WHITE);
            new FlatLafStyleBuilder().size(1.4).bold().apply(title);
            JLabel authority = new JLabel(Messages.getString("NuclearStrike.dialog.authority"));
            authority.setForeground(HAZARD_YELLOW);
            new FlatLafStyleBuilder().size(0.9).apply(authority);
            titles.add(title);
            titles.add(authority);
            add(titles, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setColor(HAZARD_DARK);
            graphics2D.fillRect(0, 0, getWidth(), getHeight());
            int stripe = UIUtil.scaleForGUI(STRIPE_WIDTH);
            graphics2D.setColor(HAZARD_STRIPE);
            graphics2D.setStroke(new BasicStroke(stripe));
            for (int x = -getHeight(); x < getWidth(); x += stripe * 2) {
                graphics2D.drawLine(x, getHeight(), x + getHeight(), 0);
            }
            paintTrefoil(graphics2D);
            graphics2D.dispose();
        }

        /** Draws the radiation symbol: a yellow disc with three dark blades around a dark hub. */
        private void paintTrefoil(Graphics2D graphics2D) {
            int diameter = UIUtil.scaleForGUI(44);
            double centerX = UIUtil.scaleForGUI(12) + (diameter / 2.0);
            double centerY = getHeight() / 2.0;
            double radius = diameter / 2.0;
            graphics2D.setColor(HAZARD_YELLOW);
            graphics2D.fill(new Ellipse2D.Double(centerX - radius, centerY - radius, diameter, diameter));
            graphics2D.setColor(HAZARD_DARK);
            double bladeRadius = radius * 0.86;
            for (int blade = 0; blade < 3; blade++) {
                double startAngle = 60 + (blade * 120);
                graphics2D.fill(new Arc2D.Double(centerX - bladeRadius, centerY - bladeRadius, bladeRadius * 2,
                      bladeRadius * 2, startAngle, 60, Arc2D.PIE));
            }
            double hubGap = radius * 0.28;
            graphics2D.setColor(HAZARD_YELLOW);
            graphics2D.fill(new Ellipse2D.Double(centerX - hubGap, centerY - hubGap, hubGap * 2, hubGap * 2));
            double hub = radius * 0.17;
            graphics2D.setColor(HAZARD_DARK);
            graphics2D.fill(new Ellipse2D.Double(centerX - hub, centerY - hub, hub * 2, hub * 2));
        }
    }
}
