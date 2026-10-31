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
package megamek.client.ui.dialogs.buttonDialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import jakarta.annotation.Nullable;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.PriorityTargetChoices;
import megamek.client.bot.princess.PriorityTargetChoices.Candidate;
import megamek.client.bot.princess.PriorityTargetChoices.ForceRole;
import megamek.client.ui.Messages;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.util.UIUtil.TipTextField;
import megamek.common.Player;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Picks a bot's priority targets by name instead of by unit ID, each with a target priority from 1 (shoot first) to 5.
 * Only the bot's enemies are listed, the units it would shoot at, under their owner and force; ticking a force ticks
 * the units in it now. It sits inside the bot settings and inside the in-game priority target window.
 *
 * <p>Enemy convoy units are shown ticked at priority 1 and cannot be unticked, because the bot targets them that way
 * whatever is picked here. Picked units that are not enemies in this game, from older settings, a team change or IDs
 * typed before there was a game, are kept in an "Other targets" group so they can still be seen and unticked.</p>
 */
public class BotTargetPickerPanel extends JPanel {

    private static final MMLogger LOGGER = MMLogger.create(BotTargetPickerPanel.class);

    /** Left indent of a force row and of a unit row, in unscaled pixels, so the list reads as owner, force, unit. */
    private static final int FORCE_INDENT = 16;
    private static final int UNIT_INDENT = 36;

    private static final Integer[] PRIORITY_CHOICES = { 1, 2, 3, 4, 5 };

    private final @Nullable Game game;
    private final List<Candidate> candidates;
    private final Set<Integer> listedUnitIds = new HashSet<>();
    private final Map<Integer, Integer> pickedPriorities = new HashMap<>();
    private final Set<Integer> fixedUnitIds;
    /** Players whose forces are folded away. Players start open. */
    private final Set<Integer> collapsedOwnerIds = new HashSet<>();
    /** Forces whose units are showing, keyed by owner and force. Forces start folded so a big list reads as lances. */
    private final Set<String> expandedForceKeys = new HashSet<>();

    private final TipTextField searchField = new TipTextField(20,
          Messages.getString("BotConfigDialog.pickerSearchHint"));
    private final JPanel rowsPanel = new JPanel();
    /**
     * The search the list was last built for. Clicking a checkbox takes focus from the search box, which then writes
     * its hint in as text; rebuilding for that would throw away the checkbox mid-click, so typing only rebuilds the
     * list when the search really changed.
     */
    private String appliedSearch = "";
    private final JLabel countLabel = new JLabel();

    /**
     * @param game              the game as this client sees it, or {@code null} when there is no game yet, which
     *                          leaves only the "Other targets" group
     * @param bot               the bot being set up, or {@code null} when it does not exist yet; every unit is then
     *                          listed, since it has no units or team of its own
     * @param currentPriorities the bot's priority targets so far, unit ID to target priority
     * @param fixedUnitIds      units that stay ticked; in a running game the bot only takes orders to add a priority
     *                          target, so a unit already on its list stays on it
     * @param listHeight        the height of the scrolling list, in unscaled pixels
     */
    public BotTargetPickerPanel(@Nullable Game game, @Nullable Player bot, Map<Integer, Integer> currentPriorities,
          Set<Integer> fixedUnitIds, int listHeight) {
        super(new BorderLayout(0, UIUtil.scaleForGUI(6)));
        this.game = game;
        this.fixedUnitIds = new HashSet<>(fixedUnitIds);
        // only the units this bot would shoot at: its enemies, which in a three-way battle means both other sides
        candidates = (game == null) ? List.of() : PriorityTargetChoices.targetsFor(game, bot);
        for (Candidate candidate : candidates) {
            listedUnitIds.add(candidate.unitId());
        }

        searchField.setToolTipText(Messages.getString("BotConfigDialog.pickerSearchTip"));
        searchField.getDocument().addDocumentListener(new RefreshOnTyping());

        rowsPanel.setLayout(new BoxLayout(rowsPanel, BoxLayout.PAGE_AXIS));
        JScrollPane scroller = new JScrollPane(rowsPanel);
        scroller.getVerticalScrollBar().setUnitIncrement(UIUtil.scaleForGUI(16));
        // narrow preferred width: the list stretches to the space it is given rather than widening the window
        scroller.setPreferredSize(new Dimension(UIUtil.scaleForGUI(360), UIUtil.scaleForGUI(listHeight)));

        // count and priority key on two lines, so neither is cut off on a narrow page
        JPanel footer = new JPanel(new BorderLayout());
        footer.add(countLabel, BorderLayout.PAGE_START);
        footer.add(new JLabel(Messages.getString("BotConfigDialog.pickerPriorityHint")), BorderLayout.PAGE_END);

        add(searchField, BorderLayout.PAGE_START);
        add(scroller, BorderLayout.CENTER);
        add(footer, BorderLayout.PAGE_END);
        setPickedPriorities(currentPriorities);
        LOGGER.debug("[PriorityTargets] picker opens with {} enemy unit(s), bot in game: {}, {} already picked",
              candidates.size(), bot != null, pickedPriorities.size());
    }

    /**
     * @return the units picked, unit ID to target priority; enemy convoy units are left out, as the bot adds them
     *       itself
     */
    public Map<Integer, Integer> getPickedPriorities() {
        return new HashMap<>(pickedPriorities);
    }

    /**
     * Replaces what is picked, for example when a preset or another bot's settings are loaded.
     *
     * @param priorities unit ID to target priority
     */
    public void setPickedPriorities(Map<Integer, Integer> priorities) {
        pickedPriorities.clear();
        pickedPriorities.putAll(priorities);
        for (Candidate candidate : candidates) {
            if (candidate.isAlwaysTarget()) {
                pickedPriorities.remove(candidate.unitId());
            }
        }
        rebuildRows();
    }

    /**
     * Adds units at the default target priority, leaving any already picked as they are. Used by the unit ID box.
     *
     * @param unitIds the units to add
     */
    public void addAtDefaultPriority(Collection<Integer> unitIds) {
        for (int unitId : unitIds) {
            pickedPriorities.putIfAbsent(unitId, BehaviorSettings.DEFAULT_TARGET_PRIORITY);
        }
        LOGGER.debug("[PriorityTargets] added by ID at the default priority: {}", unitIds);
        rebuildRows();
    }

    /** Puts the cursor in the search box. */
    public void focusSearch() {
        searchField.requestFocus();
    }

    /** Lays the list out again for the current search, ticks and folded groups. */
    private void rebuildRows() {
        rowsPanel.removeAll();
        List<Candidate> shown = shownCandidates();
        // while searching, every group with a match is open, so a match is never hidden in a folded force
        boolean isSearching = !searchField.getEnteredText().isBlank();
        int ownerId = Integer.MIN_VALUE;
        int index = 0;
        while (index < shown.size()) {
            Candidate first = shown.get(index);
            List<Candidate> forceUnits = sameForceRun(shown, index);
            index += forceUnits.size();
            if (first.ownerId() != ownerId) {
                ownerId = first.ownerId();
                rowsPanel.add(ownerRow(first, isSearching));
            }
            if (!isSearching && collapsedOwnerIds.contains(first.ownerId())) {
                continue;
            }
            rowsPanel.add(forceRow(forceUnits, isSearching));
            boolean isForceOpen = isSearching || expandedForceKeys.contains(forceKey(first));
            if (isForceOpen) {
                for (Candidate unit : forceUnits) {
                    rowsPanel.add(unitRow(unit));
                }
            }
        }
        List<Integer> otherTargets = otherTargetIds();
        if (!otherTargets.isEmpty()) {
            JLabel heading = new JLabel(Messages.getString("BotConfigDialog.pickerOtherTargets"));
            heading.setFont(heading.getFont().deriveFont(Font.BOLD));
            heading.setToolTipText(Messages.getString("BotConfigDialog.pickerOtherTargetsTip"));
            rowsPanel.add(row(heading, null, 0));
            for (int unitId : otherTargets) {
                rowsPanel.add(otherTargetRow(unitId));
            }
        }
        if (shown.isEmpty() && otherTargets.isEmpty()) {
            String key = (game == null) ? "BotConfigDialog.pickerNoGame" : "BotConfigDialog.pickerNoMatch";
            rowsPanel.add(row(new JLabel(Messages.getString(key)), null, 0));
        }
        rowsPanel.add(Box.createVerticalGlue());
        updateCount();
        rowsPanel.revalidate();
        rowsPanel.repaint();
    }

    /**
     * Rebuilds the list once the current click has been handled. Rebuilding straight away would remove the checkbox or
     * priority box that is still processing the click.
     */
    private void rebuildRowsAfterThisEvent() {
        SwingUtilities.invokeLater(this::rebuildRows);
    }

    private List<Candidate> shownCandidates() {
        return PriorityTargetChoices.matchingSearch(candidates, searchField.getEnteredText());
    }

    /**
     * @return the picked units that are not listed as enemies, in ascending ID order
     */
    private List<Integer> otherTargetIds() {
        List<Integer> others = new ArrayList<>();
        for (int unitId : pickedPriorities.keySet()) {
            if (!listedUnitIds.contains(unitId)) {
                others.add(unitId);
            }
        }
        others.sort(null);
        return others;
    }

    /**
     * @param shown the candidates on show, sorted so a force's units sit next to each other
     * @param start the index of the force's first unit
     *
     * @return the run of candidates from {@code start} that share its owner and force
     */
    private static List<Candidate> sameForceRun(List<Candidate> shown, int start) {
        Candidate first = shown.get(start);
        List<Candidate> run = new ArrayList<>();
        for (int index = start; index < shown.size(); index++) {
            Candidate candidate = shown.get(index);
            if ((candidate.ownerId() != first.ownerId()) || (candidate.forceId() != first.forceId())) {
                break;
            }
            run.add(candidate);
        }
        return run;
    }

    private static String forceKey(Candidate unit) {
        return unit.ownerId() + ":" + unit.forceId();
    }

    private Component ownerRow(Candidate unit, boolean isSearching) {
        JLabel label = new JLabel(Messages.getString("BotConfigDialog.pickerOwner", unit.ownerName(), unit.team()));
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        boolean isOpen = isSearching || !collapsedOwnerIds.contains(unit.ownerId());
        JLabel toggle = foldToggle(isOpen, () -> {
            if (!collapsedOwnerIds.remove(unit.ownerId())) {
                collapsedOwnerIds.add(unit.ownerId());
            }
        });
        return row(withToggle(toggle, label), null, 0);
    }

    /**
     * @return "(ticked of total)" for a folded force, so its picks can be seen without opening it
     */
    private String tickedOfTotal(List<Candidate> forceUnits) {
        int ticked = 0;
        for (Candidate unit : forceUnits) {
            if (unit.isAlwaysTarget() || pickedPriorities.containsKey(unit.unitId())) {
                ticked++;
            }
        }
        return Messages.getString("BotConfigDialog.pickerTickedOfTotal", ticked, forceUnits.size());
    }

    private Component forceRow(List<Candidate> forceUnits, boolean isSearching) {
        Candidate first = forceUnits.getFirst();
        String key = forceKey(first);
        boolean isOpen = isSearching || expandedForceKeys.contains(key);
        JLabel toggle = foldToggle(isOpen, () -> {
            if (!expandedForceKeys.remove(key)) {
                expandedForceKeys.add(key);
            }
        });
        String count = isOpen ? "" : ("  " + tickedOfTotal(forceUnits));
        if (!first.hasForce()) {
            JLabel label = new JLabel(Messages.getString("BotConfigDialog.pickerNoForce") + count);
            label.setEnabled(false);
            return row(withToggle(toggle, label), null, FORCE_INDENT);
        }
        List<Candidate> pickable = new ArrayList<>();
        List<Candidate> picked = new ArrayList<>();
        for (Candidate unit : forceUnits) {
            if (!unit.isAlwaysTarget() && !fixedUnitIds.contains(unit.unitId())) {
                pickable.add(unit);
                if (pickedPriorities.containsKey(unit.unitId())) {
                    picked.add(unit);
                }
            }
        }
        boolean isWholeForcePicked = pickable.isEmpty() || (picked.size() == pickable.size());

        JCheckBox forceBox = new JCheckBox(first.forceName() + roleTag(first.forceRole()) + count);
        forceBox.setSelected(isWholeForcePicked);
        forceBox.setEnabled(!pickable.isEmpty());
        forceBox.setToolTipText(Messages.getString("BotConfigDialog.pickerForceTip"));
        forceBox.addActionListener(event -> {
            for (Candidate unit : pickable) {
                if (forceBox.isSelected()) {
                    pickedPriorities.putIfAbsent(unit.unitId(), BehaviorSettings.DEFAULT_TARGET_PRIORITY);
                } else {
                    pickedPriorities.remove(unit.unitId());
                }
            }
            rebuildRowsAfterThisEvent();
        });

        if (picked.isEmpty()) {
            return row(withToggle(toggle, forceBox), null, FORCE_INDENT);
        }
        // a priority on the force row sets every ticked unit in it at once
        JComboBox<Integer> forcePriority = priorityBox(pickedPriorities.get(picked.getFirst().unitId()));
        forcePriority.addActionListener(event -> {
            for (Candidate unit : picked) {
                pickedPriorities.put(unit.unitId(), (Integer) forcePriority.getSelectedItem());
            }
            rebuildRowsAfterThisEvent();
        });
        return row(withToggle(toggle, forceBox), forcePriority, FORCE_INDENT);
    }

    private Component unitRow(Candidate unit) {
        JCheckBox unitBox = new JCheckBox(Messages.getString("BotConfigDialog.pickerUnit", unit.unitName(),
              unit.unitId()));
        if (unit.isAlwaysTarget()) {
            unitBox.setSelected(true);
            unitBox.setEnabled(false);
            unitBox.setToolTipText(Messages.getString("BotConfigDialog.pickerConvoyTip"));
            JComboBox<Integer> fixedPriority = priorityBox(BehaviorSettings.HIGHEST_TARGET_PRIORITY);
            fixedPriority.setEnabled(false);
            return row(unitBox, fixedPriority, UNIT_INDENT);
        }
        return pickableRow(unitBox, unit.unitId(), UNIT_INDENT);
    }

    private Component otherTargetRow(int unitId) {
        Entity unit = (game == null) ? null : game.getEntity(unitId);
        String text = (unit == null)
              ? Messages.getString("BotConfigDialog.unitListEntryNone", unitId)
              : Messages.getString("BotConfigDialog.pickerUnit", unit.getShortNameRaw(), unitId);
        return pickableRow(new JCheckBox(text), unitId, UNIT_INDENT);
    }

    /**
     * A unit row the player can tick and untick, with a priority box once ticked.
     *
     * @param unitBox the checkbox carrying the unit's name
     * @param unitId  the unit
     * @param indent  the left indent, in unscaled pixels
     *
     * @return the row
     */
    private Component pickableRow(JCheckBox unitBox, int unitId, int indent) {
        unitBox.setSelected(pickedPriorities.containsKey(unitId));
        if (fixedUnitIds.contains(unitId)) {
            unitBox.setEnabled(false);
            unitBox.setToolTipText(Messages.getString("BotConfigDialog.pickerFixedTip"));
        }
        unitBox.addActionListener(event -> {
            if (unitBox.isSelected()) {
                pickedPriorities.put(unitId, BehaviorSettings.DEFAULT_TARGET_PRIORITY);
            } else {
                pickedPriorities.remove(unitId);
            }
            rebuildRowsAfterThisEvent();
        });
        if (!unitBox.isSelected()) {
            return row(unitBox, null, indent);
        }
        JComboBox<Integer> unitPriority = priorityBox(pickedPriorities.get(unitId));
        unitPriority.addActionListener(event -> {
            pickedPriorities.put(unitId, (Integer) unitPriority.getSelectedItem());
            // the force row shows its units' priority, so it has to follow
            rebuildRowsAfterThisEvent();
        });
        return row(unitBox, unitPriority, indent);
    }

    /**
     * A click target showing the look and feel's own tree arrow, open or folded.
     *
     * @param isOpen whether the group is open
     * @param toggle what a click does to the group's state; the list is rebuilt afterwards
     *
     * @return the arrow
     */
    private JLabel foldToggle(boolean isOpen, Runnable toggle) {
        Icon icon = UIManager.getIcon(isOpen ? "Tree.expandedIcon" : "Tree.collapsedIcon");
        JLabel arrow = (icon == null) ? new JLabel(isOpen ? "-" : "+") : new JLabel(icon);
        arrow.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, UIUtil.scaleForGUI(4)));
        arrow.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        arrow.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                toggle.run();
                rebuildRowsAfterThisEvent();
            }
        });
        return arrow;
    }

    private static JPanel withToggle(JLabel toggle, Component content) {
        JPanel result = new JPanel(new BorderLayout());
        result.add(toggle, BorderLayout.LINE_START);
        result.add(content, BorderLayout.CENTER);
        return result;
    }

    private static String roleTag(ForceRole role) {
        return switch (role) {
            case CONVOY -> "  [" + Messages.getString("BotConfigDialog.pickerConvoy") + "]";
            case ESCORT -> "  [" + Messages.getString("BotConfigDialog.pickerEscort") + "]";
            case NONE -> "";
        };
    }

    private static JComboBox<Integer> priorityBox(int priority) {
        JComboBox<Integer> box = new JComboBox<>(PRIORITY_CHOICES);
        box.setSelectedItem(priority);
        box.setToolTipText(Messages.getString("BotConfigDialog.pickerPriorityTip"));
        return box;
    }

    private static JPanel row(Component label, @Nullable Component priority, int indent) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBorder(BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(1), UIUtil.scaleForGUI(indent),
              UIUtil.scaleForGUI(1), UIUtil.scaleForGUI(4)));
        row.add(label, BorderLayout.CENTER);
        if (priority != null) {
            row.add(priority, BorderLayout.LINE_END);
        }
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private void updateCount() {
        int convoyUnits = 0;
        for (Candidate candidate : candidates) {
            if (candidate.isAlwaysTarget()) {
                convoyUnits++;
            }
        }
        countLabel.setText((convoyUnits == 0)
              ? Messages.getString("BotConfigDialog.pickerCount", pickedPriorities.size())
              : Messages.getString("BotConfigDialog.pickerCountConvoy", pickedPriorities.size(), convoyUnits));
    }

    /** Refilters the list when the typed search changes, ignoring the box swapping its hint in and out. */
    private class RefreshOnTyping implements DocumentListener {
        @Override
        public void insertUpdate(DocumentEvent event) {
            refilterIfSearchChanged();
        }

        @Override
        public void removeUpdate(DocumentEvent event) {
            refilterIfSearchChanged();
        }

        @Override
        public void changedUpdate(DocumentEvent event) {
            refilterIfSearchChanged();
        }
    }

    private void refilterIfSearchChanged() {
        String search = searchField.getEnteredText();
        if (!search.equals(appliedSearch)) {
            appliedSearch = search;
            rebuildRows();
        }
    }
}
