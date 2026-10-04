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
package megamek.client.ui.dialogs.BotCommands;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import megamek.client.ui.Messages;
import megamek.client.ui.util.UIUtil;
import megamek.common.OffBoardDirection;
import megamek.common.annotations.Nullable;
import megamek.common.orders.LanceRole;
import megamek.common.orders.LanceRoles;

/**
 * Sets a lance's role: none, a convoy with its exit edge, or an escort with its settings. The Move Order editor's Role
 * tab and the lobby's Role dialog both use it, so a role reads the same wherever it is set (HammerGS, 2026-10-02).
 */
public class LanceRolePanel extends JPanel {

    private static final int GAP = 8;
    private static final int PICTURE_SPACING = 6;
    // the summary and the hints wrap at these widths; wider ran the summary past the dialog's edge at a large GUI
    // scale (HammerGS, 2026-10-02)
    private static final int TEXT_WIDTH = 520;
    private static final int HINT_WIDTH = 300;
    private static final String NONE_CARD = "none";

    /** What a convoy does once its route is done. */
    private enum ConvoyEnd {
        LEAVE,
        WAIT
    }

    /** The role choices at the top: none, or one of the kinds. */
    private enum Choice {
        NONE,
        CONVOY,
        ESCORT
    }

    private final String lanceName;
    private final List<Runnable> changeListeners = new ArrayList<>();
    private final Map<Choice, JToggleButton> choiceButtons = new EnumMap<>(Choice.class);
    private final CardLayout cards = new CardLayout();
    private final JPanel cardPanel = new JPanel(cards);
    private final Map<OffBoardDirection, JToggleButton> edgeButtons = new EnumMap<>(OffBoardDirection.class);
    private final Map<ConvoyEnd, JToggleButton> endButtons = new EnumMap<>(ConvoyEnd.class);
    private final JLabel convoyHelp = new JLabel();
    private final JComboBox<LanceRoles.ConvoyChoice> convoyCombo = new JComboBox<>();
    private final Map<LanceRole.Position, JToggleButton> positionButtons = new EnumMap<>(LanceRole.Position.class);
    private final Map<LanceRole.Distance, JToggleButton> distanceButtons = new EnumMap<>(LanceRole.Distance.class);
    private final Map<LanceRole.Movement, JToggleButton> movementButtons = new EnumMap<>(LanceRole.Movement.class);
    private final JComboBox<LanceRole.Contact> contactCombo = new JComboBox<>(LanceRole.Contact.values());
    private final Map<LanceRole.LeaveToFight, JToggleButton> leaveButtons = new EnumMap<>(
          LanceRole.LeaveToFight.class);
    private final Map<LanceRole.WhenConvoyGone, JToggleButton> goneButtons = new EnumMap<>(
          LanceRole.WhenConvoyGone.class);
    private final JLabel escortSummary = new JLabel();
    private final JLabel distanceHint = new JLabel();
    private final JLabel contactHint = new JLabel();
    private final JLabel leaveHint = new JLabel();
    private final JLabel goneHint = new JLabel();
    private boolean isLoading;

    /**
     * @param lanceName       the lance's name, for the summary
     * @param convoys         the convoy lances on the side the escort can guard
     * @param defaultExitEdge the exit edge a new convoy starts with
     */
    public LanceRolePanel(String lanceName, List<LanceRoles.ConvoyChoice> convoys,
          OffBoardDirection defaultExitEdge) {
        super(new BorderLayout(UIUtil.scaleForGUI(GAP), UIUtil.scaleForGUI(GAP)));
        this.lanceName = lanceName;
        int gap = UIUtil.scaleForGUI(GAP);
        setBorder(new EmptyBorder(gap, gap, gap, gap));

        JPanel choiceRow = new JPanel(new FlowLayout(FlowLayout.LEADING, gap, 0));
        choiceRow.add(new JLabel(Messages.getString("BotCommandPanel.Role.title")));
        ButtonGroup choiceGroup = new ButtonGroup();
        for (Choice choice : Choice.values()) {
            JToggleButton button = new JToggleButton(Messages.getString("BotCommandPanel.Role." + choice.name()));
            boldWhenPicked(button);
            button.addActionListener(event -> showChoice(choice));
            choiceGroup.add(button);
            choiceButtons.put(choice, button);
            choiceRow.add(button);
        }
        add(choiceRow, BorderLayout.PAGE_START);

        JLabel noneHelp = new JLabel(Messages.getString("BotCommandPanel.Role.none.help"));
        noneHelp.setVerticalAlignment(SwingConstants.TOP);
        cardPanel.add(noneHelp, NONE_CARD);
        cardPanel.add(createConvoyCard(), Choice.CONVOY.name());
        cardPanel.add(createEscortCard(convoys), Choice.ESCORT.name());
        add(cardPanel, BorderLayout.CENTER);

        loadDefaults(defaultExitEdge);
        showChoice(Choice.NONE);
    }

    private JPanel createConvoyCard() {
        JPanel card = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = formConstraints();
        card.add(new JLabel(Messages.getString("BotCommandPanel.Role.convoy.then")), constraints);
        constraints.gridx = 1;
        card.add(segmentedRow(endButtons, List.of(ConvoyEnd.values()), "BotCommandPanel.Role.convoy.then."),
              constraints);
        constraints.gridx = 0;
        constraints.gridy++;
        card.add(new JLabel(Messages.getString("BotCommandPanel.Role.convoy.exitEdge")), constraints);
        constraints.gridx = 1;
        JPanel edges = segmentedRow(edgeButtons, List.of(OffBoardDirection.NORTH, OffBoardDirection.EAST,
              OffBoardDirection.SOUTH, OffBoardDirection.WEST), "BotCommandPanel.Role.edge.");
        card.add(edges, constraints);
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        card.add(convoyHelp, constraints);
        return card;
    }

    private JPanel createEscortCard(List<LanceRoles.ConvoyChoice> convoys) {
        int gap = UIUtil.scaleForGUI(GAP);
        JPanel card = new JPanel(new BorderLayout(UIUtil.scaleForGUI(GAP * 3), 0));

        JPanel picture = new JPanel(new BorderLayout(0, gap));
        picture.add(new JLabel(Messages.getString("BotCommandPanel.Role.escort.position")), BorderLayout.PAGE_START);
        JPanel compass = new JPanel(new GridBagLayout());
        compass.setBorder(BorderFactory.createEtchedBorder());
        GridBagConstraints cell = new GridBagConstraints();
        int spacing = UIUtil.scaleForGUI(PICTURE_SPACING);
        cell.insets = new Insets(spacing, spacing, spacing, spacing);
        addPosition(compass, cell, LanceRole.Position.LEAD, 1, 0);
        addPosition(compass, cell, LanceRole.Position.LEFT, 0, 1);
        addPosition(compass, cell, LanceRole.Position.RIGHT, 2, 1);
        addPosition(compass, cell, LanceRole.Position.REAR, 1, 2);
        cell.gridx = 1;
        cell.gridy = 1;
        compass.add(new JLabel(Messages.getString("BotCommandPanel.Role.escort.convoyIcon"), SwingConstants.CENTER),
              cell);
        picture.add(compass, BorderLayout.CENTER);
        JButton surroundButton = new JButton(Messages.getString("BotCommandPanel.Role.escort.surround"));
        surroundButton.addActionListener(event -> {
            for (JToggleButton button : positionButtons.values()) {
                button.setSelected(true);
            }
            fireChange();
        });
        picture.add(surroundButton, BorderLayout.PAGE_END);
        card.add(picture, BorderLayout.LINE_START);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = formConstraints();
        for (LanceRoles.ConvoyChoice convoy : convoys) {
            convoyCombo.addItem(convoy);
        }
        convoyCombo.setEnabled(!convoys.isEmpty());
        convoyCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                  boolean isSelected, boolean hasFocus) {
                String text;
                if (value instanceof LanceRoles.ConvoyChoice convoy) {
                    text = Messages.getString(convoy.isConvoy() ? "BotCommandPanel.Role.escort.convoyChoice"
                          : "BotCommandPanel.Role.escort.lanceChoice", convoy.name(), convoy.unitCount());
                } else {
                    text = Messages.getString("BotCommandPanel.Role.escort.noConvoy");
                }
                return super.getListCellRendererComponent(list, text, index, isSelected, hasFocus);
            }
        });
        convoyCombo.addActionListener(event -> fireChange());
        addFormRow(form, constraints, "BotCommandPanel.Role.escort.escorting", convoyCombo, null);
        addFormRow(form, constraints, "BotCommandPanel.Role.escort.distance",
              segmentedRow(distanceButtons, List.of(LanceRole.Distance.values()), "BotCommandPanel.Role.distance."),
              distanceHint);
        // escorts keep in step with their convoy; Bounding is hidden until the bot acts on it (HammerGS, 2026-10-03)
        segmentedRow(movementButtons, List.of(LanceRole.Movement.IN_STEP), "BotCommandPanel.Role.movement.");
        contactCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                  boolean isSelected, boolean hasFocus) {
                String text = (value instanceof LanceRole.Contact contact)
                      ? Messages.getString("BotCommandPanel.Role.contact." + contact.name()) : "";
                return super.getListCellRendererComponent(list, text, index, isSelected, hasFocus);
            }
        });
        contactCombo.addActionListener(event -> fireChange());
        addFormRow(form, constraints, "BotCommandPanel.Role.escort.contact", contactCombo, contactHint);
        addFormRow(form, constraints, "BotCommandPanel.Role.escort.leave",
              segmentedRow(leaveButtons, List.of(LanceRole.LeaveToFight.values()), "BotCommandPanel.Role.leave."),
              leaveHint);
        addFormRow(form, constraints, "BotCommandPanel.Role.escort.gone",
              segmentedRow(goneButtons, List.of(LanceRole.WhenConvoyGone.values()), "BotCommandPanel.Role.gone."),
              goneHint);
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridwidth = 3;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        form.add(escortSummary, constraints);
        card.add(form, BorderLayout.CENTER);
        return card;
    }

    private void addPosition(JPanel compass, GridBagConstraints cell, LanceRole.Position position, int column,
          int row) {
        JToggleButton button = new JToggleButton(Messages.getString("BotCommandPanel.Role.position."
              + position.name()));
        boldWhenPicked(button);
        button.addActionListener(event -> fireChange());
        positionButtons.put(position, button);
        cell.gridx = column;
        cell.gridy = row;
        compass.add(button, cell);
    }

    /**
     * Sets a toggle button's text in bold while it is picked: the theme shades a picked button only a little lighter,
     * and which edge or place was on was hard to tell (HammerGS, 2026-10-03).
     */
    private static void boldWhenPicked(JToggleButton button) {
        Font plain = button.getFont();
        button.addItemListener(event -> button.setFont(plain.deriveFont(button.isSelected() ? Font.BOLD
              : Font.PLAIN)));
    }

    private static GridBagConstraints formConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        int gap = UIUtil.scaleForGUI(GAP);
        constraints.insets = new Insets(0, 0, gap, gap);
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.gridx = 0;
        constraints.gridy = 0;
        return constraints;
    }

    private void addFormRow(JPanel form, GridBagConstraints constraints, String labelKey, Component control,
          @Nullable JLabel hint) {
        constraints.gridx = 0;
        constraints.gridwidth = 1;
        constraints.weightx = 0;
        form.add(new JLabel(Messages.getString(labelKey)), constraints);
        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.NONE;
        form.add(control, constraints);
        if (hint != null) {
            constraints.gridx = 2;
            form.add(hint, constraints);
        }
        constraints.gridy++;
    }

    /**
     * @return a row of toggle buttons of which one is chosen at a time, one per value
     */
    private <T extends Enum<T>> JPanel segmentedRow(Map<T, JToggleButton> buttons, List<T> values, String keyPrefix) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        ButtonGroup group = new ButtonGroup();
        for (T value : values) {
            JToggleButton button = new JToggleButton(Messages.getString(keyPrefix + value.name()));
            boldWhenPicked(button);
            button.addActionListener(event -> fireChange());
            group.add(button);
            buttons.put(value, button);
            row.add(button);
        }
        return row;
    }

    private void loadDefaults(OffBoardDirection defaultExitEdge) {
        isLoading = true;
        edgeButtons.get(edgeButtons.containsKey(defaultExitEdge) ? defaultExitEdge : OffBoardDirection.NORTH)
              .setSelected(true);
        endButtons.get(ConvoyEnd.LEAVE).setSelected(true);
        LanceRole escort = LanceRole.defaultEscort(-1);
        loadEscort(escort);
        isLoading = false;
        refreshText();
    }

    private void loadEscort(LanceRole escort) {
        for (Map.Entry<LanceRole.Position, JToggleButton> entry : positionButtons.entrySet()) {
            entry.getValue().setSelected(escort.getPositions().contains(entry.getKey()));
        }
        distanceButtons.get(escort.getDistance()).setSelected(true);
        movementButtons.getOrDefault(escort.getMovement(), movementButtons.get(LanceRole.Movement.IN_STEP))
              .setSelected(true);
        contactCombo.setSelectedItem(escort.getContact());
        leaveButtons.get(escort.getLeaveToFight()).setSelected(true);
        goneButtons.get(escort.getWhenConvoyGone()).setSelected(true);
        for (int index = 0; index < convoyCombo.getItemCount(); index++) {
            if (convoyCombo.getItemAt(index).forceId() == escort.getConvoyForceId()) {
                convoyCombo.setSelectedIndex(index);
            }
        }
    }

    /**
     * Shows a role, or none.
     *
     * @param role the role, or {@code null} for none
     */
    public void setRole(@Nullable LanceRole role) {
        isLoading = true;
        if (role == null) {
            choiceButtons.get(Choice.NONE).setSelected(true);
            showCard(Choice.NONE);
        } else if (role.isConvoy()) {
            edgeButtons.get(role.getExitEdge()).setSelected(true);
            endButtons.get(role.isWaitingAtRouteEnd() ? ConvoyEnd.WAIT : ConvoyEnd.LEAVE).setSelected(true);
            choiceButtons.get(Choice.CONVOY).setSelected(true);
            showCard(Choice.CONVOY);
        } else {
            loadEscort(role);
            choiceButtons.get(Choice.ESCORT).setSelected(true);
            showCard(Choice.ESCORT);
        }
        isLoading = false;
        refreshText();
    }

    /**
     * @return the role as set, or {@code null} for none; also {@code null} for an escort not yet complete - with no
     *       convoy to guard or no position (see {@link #isComplete()})
     */
    public @Nullable LanceRole getRole() {
        if (choiceButtons.get(Choice.CONVOY).isSelected()) {
            return LanceRole.convoy(selected(edgeButtons, OffBoardDirection.NORTH),
                  selected(endButtons, ConvoyEnd.LEAVE) == ConvoyEnd.WAIT);
        }
        if (!choiceButtons.get(Choice.ESCORT).isSelected() || !isComplete()) {
            return null;
        }
        LanceRoles.ConvoyChoice convoy = (LanceRoles.ConvoyChoice) convoyCombo.getSelectedItem();
        return LanceRole.escort(convoy.forceId(), chosenPositions(), selected(distanceButtons,
                    LanceRole.Distance.MEDIUM), selected(movementButtons, LanceRole.Movement.IN_STEP),
              (LanceRole.Contact) contactCombo.getSelectedItem(),
              selected(leaveButtons, LanceRole.LeaveToFight.BRIEFLY),
              selected(goneButtons, LanceRole.WhenConvoyGone.FOLLOW));
    }

    /**
     * @return {@code true} when the choice can be sent: none, a convoy, or an escort with a convoy and a position
     */
    public boolean isComplete() {
        if (!choiceButtons.get(Choice.ESCORT).isSelected()) {
            return true;
        }
        return (convoyCombo.getSelectedItem() != null) && !chosenPositions().isEmpty();
    }

    /**
     * @return the lance the escort is given when it is not a convoy yet, and so becomes one when the role is sent;
     *       empty otherwise
     */
    public Optional<LanceRoles.ConvoyChoice> lanceToMakeConvoy() {
        if (!isEscortChosen() || !(convoyCombo.getSelectedItem() instanceof LanceRoles.ConvoyChoice choice)
              || choice.isConvoy()) {
            return Optional.empty();
        }
        return Optional.of(choice);
    }

    /**
     * @return {@code true} when Escort is chosen: the lance then has no route of its own
     */
    public boolean isEscortChosen() {
        return choiceButtons.get(Choice.ESCORT).isSelected();
    }

    /**
     * @param listener called whenever the player changes the role or one of its settings
     */
    public void addChangeListener(Runnable listener) {
        changeListeners.add(listener);
    }

    private Set<LanceRole.Position> chosenPositions() {
        Set<LanceRole.Position> positions = EnumSet.noneOf(LanceRole.Position.class);
        for (Map.Entry<LanceRole.Position, JToggleButton> entry : positionButtons.entrySet()) {
            if (entry.getValue().isSelected()) {
                positions.add(entry.getKey());
            }
        }
        return positions;
    }

    private static <T extends Enum<T>> T selected(Map<T, JToggleButton> buttons, T fallback) {
        for (Map.Entry<T, JToggleButton> entry : buttons.entrySet()) {
            if (entry.getValue().isSelected()) {
                return entry.getKey();
            }
        }
        return fallback;
    }

    private void showChoice(Choice choice) {
        showCard(choice);
        fireChange();
    }

    private void showCard(Choice choice) {
        cards.show(cardPanel, (choice == Choice.NONE) ? NONE_CARD : choice.name());
    }

    private void fireChange() {
        if (isLoading) {
            return;
        }
        refreshText();
        for (Runnable listener : changeListeners) {
            listener.run();
        }
    }

    private void refreshText() {
        String edge = Messages.getString("BotCommandPanel.Role.edge." + selected(edgeButtons,
              OffBoardDirection.NORTH).name());
        boolean isWaiting = selected(endButtons, ConvoyEnd.LEAVE) == ConvoyEnd.WAIT;
        // the exit edge matters only to a convoy that leaves
        for (JToggleButton edgeButton : edgeButtons.values()) {
            edgeButton.setEnabled(!isWaiting);
        }
        convoyHelp.setText(html(Messages.getString(isWaiting ? "BotCommandPanel.Role.convoy.helpWait"
              : "BotCommandPanel.Role.convoy.help", edge), TEXT_WIDTH));
        escortSummary.setText(html(escortSummaryText(), TEXT_WIDTH));
        // each hint says what the choice picked on its row means, and changes with it
        distanceHint.setText(hint("distance." + selected(distanceButtons, LanceRole.Distance.MEDIUM).name()));
        contactHint.setText(hint("contact." + ((LanceRole.Contact) contactCombo.getSelectedItem()).name()));
        // how far it leaves to fight matters only when it breaks off to fight
        boolean isBreakingToFight = contactCombo.getSelectedItem() == LanceRole.Contact.BREAK_AND_FIGHT;
        for (JToggleButton leaveButton : leaveButtons.values()) {
            leaveButton.setEnabled(isBreakingToFight);
        }
        leaveHint.setText(isBreakingToFight
              ? hint("leave." + selected(leaveButtons, LanceRole.LeaveToFight.BRIEFLY).name())
              : Messages.getString("BotCommandPanel.Role.hint.leave.onlyBreaking"));
        goneHint.setText(hint("gone." + selected(goneButtons, LanceRole.WhenConvoyGone.FOLLOW).name()));
    }

    private static String hint(String keySuffix) {
        return html(Messages.getString("BotCommandPanel.Role.hint." + keySuffix, LanceRole.BRIEF_CHASE_HEXES),
              HINT_WIDTH);
    }

    private String escortSummaryText() {
        Set<LanceRole.Position> positions = chosenPositions();
        if (positions.isEmpty()) {
            return Messages.getString("BotCommandPanel.Role.summary.noPositions");
        }
        LanceRoles.ConvoyChoice convoy = (LanceRoles.ConvoyChoice) convoyCombo.getSelectedItem();
        if (convoy == null) {
            return Messages.getString("BotCommandPanel.Role.escort.noConvoy");
        }
        List<String> positionNames = new ArrayList<>();
        for (LanceRole.Position position : positions) {
            positionNames.add(Messages.getString("BotCommandPanel.Role.position." + position.name()));
        }
        LanceRole.Distance distance = selected(distanceButtons, LanceRole.Distance.MEDIUM);
        return "<b>" + lanceName + ":</b> "
              + Messages.getString("BotCommandPanel.Role.summary.escort", distance.getNearest(),
              distance.getFurthest(), convoy.name(), String.join(", ", positionNames),
              Messages.getString("BotCommandPanel.Role.summary.movement."
                    + selected(movementButtons, LanceRole.Movement.IN_STEP).name())) + ' '
              + Messages.getString("BotCommandPanel.Role.summary.contact."
              + ((LanceRole.Contact) contactCombo.getSelectedItem()).name()) + ' '
              + Messages.getString("BotCommandPanel.Role.summary.leave."
              + selected(leaveButtons, LanceRole.LeaveToFight.BRIEFLY).name(), LanceRole.BRIEF_CHASE_HEXES) + ' '
              + Messages.getString("BotCommandPanel.Role.summary.gone."
              + selected(goneButtons, LanceRole.WhenConvoyGone.FOLLOW).name())
              + (convoy.isConvoy() ? "" : ' ' + Messages.getString("BotCommandPanel.Role.summary.becomesConvoy",
              convoy.name(), Messages.getString("BotCommandPanel.Role.edge." + convoy.newExitEdge().name())));
    }

    private static String html(String text, int width) {
        return "<html><div style='width:" + UIUtil.scaleForGUI(width) + "px'>" + text + "</div></html>";
    }
}
