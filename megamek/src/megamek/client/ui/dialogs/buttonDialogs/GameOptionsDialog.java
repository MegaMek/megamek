/*
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2002-2026 The MegaMek Team. All Rights Reserved.
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

import static megamek.client.ui.Messages.CLIENT_BUNDLE;
import static megamek.client.ui.util.UIUtil.WrappingButtonPanel;
import static megamek.common.internationalization.I18n.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.function.Predicate;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilder;

import org.apache.commons.text.StringEscapeUtils;

import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.DialogOptionListener;
import megamek.client.ui.dialogs.MMDialogs.MMConfirmDialog;
import megamek.client.ui.panels.DialogOptionComponentYPanel;
import megamek.client.ui.panels.GameOptionsPane;
import megamek.client.ui.panels.phaseDisplay.lobby.VictoryConditionsDialog;
import megamek.client.ui.settings.SettingsBadge;
import megamek.client.ui.settings.SettingsIconLegend;
import megamek.client.ui.util.UIUtil;
import megamek.common.TechConstants;
import megamek.common.annotations.Nullable;
import megamek.common.options.GameOptions;
import megamek.common.options.IBasicOption;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.weapons.bayWeapons.capital.CapitalMissileBayWeapon;
import megamek.logging.MMLogger;
import megamek.utilities.xml.MMXMLUtility;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/** Responsible for displaying the current game options and allowing the user to change them. */
public class GameOptionsDialog extends AbstractButtonDialog implements ActionListener, DialogOptionListener {
    private static final MMLogger LOGGER = MMLogger.create(GameOptionsDialog.class);
    private static final int BUTTON_GAP = 8;
    private static final Set<String> CORE_RULES_DISABLED_OPTIONS = Set.of(
          OptionsConstants.BASE_FLAMER_HEAT,
          OptionsConstants.ADVANCED_COMBAT_TAC_OPS_AMS,
          OptionsConstants.ADVANCED_COMBAT_TAC_OPS_CHARGE_DAMAGE,
          OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_WALK_BACKWARDS,
          OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RETRACTABLE_BLADES,
          OptionsConstants.ADVANCED_COMBAT_UNJAM_UAC,
          OptionsConstants.INIT_FRONT_LOAD_INITIATIVE,
          OptionsConstants.ADVANCED_MINEFIELDS,
          OptionsConstants.ADVANCED_ALTERNATE_MASC,
          OptionsConstants.ADVANCED_ALTERNATE_MASC_ENHANCED);

    private ClientGUI clientGui;
    private JFrame frame;
    private GameOptions options;
    private boolean editable = true;
    private Set<String> excludedOptionNames = Set.of();

    /**
     * A map that maps an option to a collection of DialogOptionComponents that can effect the value of this option.
     */
    private Map<String, List<DialogOptionComponentYPanel>> optionComps = new HashMap<>();

    private final JPanel panOptions = new JPanel(new BorderLayout());
    private GameOptionsPane gameOptionsPane;

    private final WrappingButtonPanel panPassword = new WrappingButtonPanel();
    private final JLabel labPass = new JLabel(getTextAt(CLIENT_BUNDLE, "GameOptionsDialog.Password"));
    private final JTextField texPass = new JTextField(15);
    private final JButton butSave = new JButton(getTextAt(CLIENT_BUNDLE, "GameOptionsDialog.Save"));
    private final JButton butLoad = new JButton(getTextAt(CLIENT_BUNDLE, "GameOptionsDialog.Load"));
    private final JButton butDefaults = new JButton(getTextAt(CLIENT_BUNDLE, "GameOptionsDialog.Defaults"));
    private final JButton butOkay = new JButton(getTextAt(CLIENT_BUNDLE, "Okay"));
    private final JButton butCancel = new JButton(getTextAt(CLIENT_BUNDLE, "Cancel"));
    private final JToggleButton butUnofficial = new JToggleButton();
    private final JToggleButton butLegacy = new JToggleButton();

    /**
     * When the OK button is pressed, the options can be saved to a file; this behavior happens by default but there are
     * some situations where the options should not be saved, such as when loading a scenario.
     */
    private boolean performSave = true;

    /**
     * Creates a new GameOptionsDialog with the given ClientGUI as parent. The ClientGUI supplies the game options. Used
     * in the lobby and game.
     */
    public GameOptionsDialog(ClientGUI cg) {
        super(cg.getFrame(), "GameOptionsDialog", "GameOptionsDialog.title");
        clientGui = cg;
        init(cg.getFrame(), cg.getClient().getGame().getOptions());
    }

    /**
     * Creates a new GameOptionsDialog with the given JFrame as parent. Uses the given game options. Used when starting
     * a scenario.
     */
    public GameOptionsDialog(JFrame frame, GameOptions options, boolean shouldSave) {
        this(frame, options, shouldSave, Set.of());
    }

    /**
     * Creates a scenario-style Game Options dialog while omitting options owned by the calling application.
     *
     * @param frame               parent frame
     * @param options             options edited by the dialog
     * @param shouldSave          whether confirming should save options to disk
     * @param excludedOptionNames option names that must not appear in this presentation
     */
    public GameOptionsDialog(JFrame frame, GameOptions options, boolean shouldSave, Set<String> excludedOptionNames) {
        super(frame, "GameOptionsDialog", "GameOptionsDialog.title");
        performSave = shouldSave;
        this.excludedOptionNames = Set.copyOf(excludedOptionNames);
        init(frame, options);
    }

    /** Initial dialog setup for both constructors. */
    private void init(JFrame frame, GameOptions options) {
        this.options = options;
        this.frame = frame;
        labPass.setLabelFor(texPass);
        panPassword.add(labPass);
        panPassword.add(texPass);
        refreshOptions();
        initialize();
    }

    @Override
    protected Container createCenterPane() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(panOptions, BorderLayout.CENTER);
        mainPanel.add(panPassword, BorderLayout.SOUTH);
        return mainPanel;
    }

    @Override
    protected JPanel createButtonPanel() {
        butOkay.setName("okButton");
        butCancel.setName("cancelButton");
        butDefaults.setName("defaultsButton");
        butSave.setName("saveButton");
        butLoad.setName("loadButton");
        butUnofficial.setName("unofficialToggle");
        butLegacy.setName("legacyToggle");
        butOkay.addActionListener(this::okButtonActionPerformed);
        butCancel.addActionListener(this::cancelActionPerformed);
        butDefaults.addActionListener(this::resetToDefaults);
        butSave.addActionListener(this);
        butLoad.addActionListener(this);
        butUnofficial.addActionListener(this);
        butLegacy.addActionListener(this);

        configureRuleToggle(butUnofficial, GameOptionsPane.unofficialBadge(),
              "GameOptionsDialog.Unofficial", "GameOptionsDialog.Unofficial.tooltip");
        configureRuleToggle(butLegacy, GameOptionsPane.legacyBadge(),
              "GameOptionsDialog.Legacy", "GameOptionsDialog.Legacy.tooltip");

        butOkay.putClientProperty("FlatLaf.style",
              "background: $Button.default.background; foreground: $Button.default.foreground");
        setUniformButtonSize(butOkay, butCancel, butDefaults, butSave, butLoad);
        int gap = UIUtil.scaleForGUI(BUTTON_GAP);

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, gap, 0));
        actionButtons.add(butOkay);
        actionButtons.add(butCancel);
        actionButtons.add(butDefaults);
        actionButtons.add(butSave);
        actionButtons.add(butLoad);

        JButton legendButton = SettingsIconLegend.createLegendButton(
              getTextAt(CLIENT_BUNDLE, "GameOptionsDialog.legend.button"),
              getTextAt(CLIENT_BUNDLE, "GameOptionsDialog.legend.tooltip"),
              GameOptionsPane.legendEntries());
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, gap, 0));
        legendPanel.add(legendButton);
        legendPanel.add(butUnofficial);
        legendPanel.add(butLegacy);

        return responsiveFooter(legendPanel, actionButtons, gap);
    }

    static JPanel responsiveFooter(JPanel ruleControls, JPanel actionButtons, int gap) {
        JPanel footer = new JPanel(new ResponsiveFooterLayout(gap));
        footer.add(ruleControls);
        footer.add(actionButtons);
        return footer;
    }

    private static final class ResponsiveFooterLayout implements LayoutManager {
        private final int gap;

        private ResponsiveFooterLayout(int gap) {
            this.gap = gap;
        }

        @Override
        public void addLayoutComponent(String name, Component component) {
        }

        @Override
        public void removeLayoutComponent(Component component) {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return layoutSize(parent, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return layoutSize(parent, false);
        }

        private Dimension layoutSize(Container parent, boolean preferred) {
            if (parent.getComponentCount() < 2) {
                return new Dimension();
            }
            Dimension controlsSize = componentSize(parent.getComponent(0), preferred);
            Dimension actionsSize = componentSize(parent.getComponent(1), preferred);
            Insets insets = parent.getInsets();
            int requiredWidth = controlsSize.width + actionsSize.width + (gap * 3)
                  + insets.left + insets.right;
            int availableWidth = parent.getWidth();
            boolean wraps = availableWidth > 0 && requiredWidth > availableWidth;
            int width = wraps
                  ? Math.max(controlsSize.width, actionsSize.width) + (gap * 2) + insets.left + insets.right
                  : requiredWidth;
            int height = wraps
                  ? controlsSize.height + actionsSize.height + (gap * 3)
                  : Math.max(controlsSize.height, actionsSize.height) + (gap * 2);
            return new Dimension(width, height + insets.top + insets.bottom);
        }

        private static Dimension componentSize(Component component, boolean preferred) {
            return preferred ? component.getPreferredSize() : component.getMinimumSize();
        }

        @Override
        public void layoutContainer(Container parent) {
            if (parent.getComponentCount() < 2) {
                return;
            }
            Component controls = parent.getComponent(0);
            Component actions = parent.getComponent(1);
            Dimension controlsSize = controls.getPreferredSize();
            Dimension actionsSize = actions.getPreferredSize();
            Insets insets = parent.getInsets();
            int availableWidth = parent.getWidth() - insets.left - insets.right;
            int requiredWidth = controlsSize.width + actionsSize.width + (gap * 3);
            if (requiredWidth <= availableWidth) {
                int controlsX = insets.left + gap;
                int maximumActionsX = parent.getWidth() - insets.right - gap - actionsSize.width;
                int actionsX = Math.min(maximumActionsX,
                      Math.max((parent.getWidth() - actionsSize.width) / 2,
                            controlsX + controlsSize.width + gap));
                int contentHeight = parent.getHeight() - insets.top - insets.bottom;
                controls.setBounds(controlsX, insets.top + (contentHeight - controlsSize.height) / 2,
                      controlsSize.width, controlsSize.height);
                actions.setBounds(actionsX, insets.top + (contentHeight - actionsSize.height) / 2,
                      actionsSize.width, actionsSize.height);
            } else {
                int controlsX = insets.left + gap;
                int actionsX = Math.max(insets.left + gap, (parent.getWidth() - actionsSize.width) / 2);
                int controlsY = insets.top + gap;
                controls.setBounds(controlsX, controlsY, controlsSize.width, controlsSize.height);
                actions.setBounds(actionsX, controlsY + controlsSize.height + gap,
                      actionsSize.width, actionsSize.height);
            }
        }
    }

    private static void configureRuleToggle(JToggleButton button, SettingsBadge badge, String labelKey,
          String tooltipKey) {
        boolean selected = button.isSelected();
      String label = getTextAt(CLIENT_BUNDLE, labelKey);
      button.setText(ruleToggleText(badge, label,
              getTextAt(CLIENT_BUNDLE, "GameOptionsDialog.ruleToggle.on")));
        Dimension onSize = button.getPreferredSize();
      button.setText(ruleToggleText(badge, label,
              getTextAt(CLIENT_BUNDLE, "GameOptionsDialog.ruleToggle.off")));
        Dimension offSize = button.getPreferredSize();
        Dimension size = new Dimension(Math.max(onSize.width, offSize.width), Math.max(onSize.height, offSize.height));
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setToolTipText(getTextAt(CLIENT_BUNDLE, tooltipKey));
        button.setSelected(selected);
        updateRuleToggleText(button, badge, label);
    }

    static String ruleToggleText(SettingsBadge badge, String label, String state) {
        return "<html><nobr>" + badge.toHtml() + "&nbsp;" + StringEscapeUtils.escapeHtml4(label)
              + ": <b>" + StringEscapeUtils.escapeHtml4(state) + "</b></nobr></html>";
    }

    private static void updateRuleToggleText(JToggleButton button, SettingsBadge badge, String label) {
        String stateKey = button.isSelected()
              ? "GameOptionsDialog.ruleToggle.on"
              : "GameOptionsDialog.ruleToggle.off";
        String state = getTextAt(CLIENT_BUNDLE, stateKey);
        button.setText(ruleToggleText(badge, label, state));
        button.getAccessibleContext().setAccessibleName(label + ": " + state);
    }

    private static void setUniformButtonSize(JButton... buttons) {
        int width = Arrays.stream(buttons)
              .mapToInt(button -> button.getPreferredSize().width)
              .max()
              .orElse(0);
        int height = Arrays.stream(buttons)
              .mapToInt(button -> button.getPreferredSize().height)
              .max()
              .orElse(0);
        Dimension size = new Dimension(width, height);
        for (JButton button : buttons) {
            button.setPreferredSize(size);
            button.setMinimumSize(size);
        }
    }

    /** Updates the dialog ui with the given options. */
    public void update(GameOptions options) {
        this.options = options;
        refreshOptions();
    }

    private void send() {
        Vector<IBasicOption> changed = new Vector<>();

        for (List<DialogOptionComponentYPanel> comps : optionComps.values()) {
            // Each option in the list should have the same value, so picking the first is fine
            if (!comps.isEmpty()) {
                DialogOptionComponentYPanel comp = comps.getFirst();
                if (comp.hasChanged()) {
                    changed.addElement(comp.changedOption());
                    comp.setOptionChanged(false);
                }
            }
        }

        if ((clientGui != null) && !changed.isEmpty()) {
            clientGui.getClient().sendGameOptions(texPass.getText(), changed);
        }
    }

    private void doSave() {
        GameOptions.saveOptions(getOptions());
    }

    public Vector<IBasicOption> getOptions() {
        Vector<IBasicOption> output = new Vector<>();

        for (List<DialogOptionComponentYPanel> comps : optionComps.values()) {
            // Each option in the list should have the same value, so picking
            // the first is fine
            if (!comps.isEmpty()) {
                IBasicOption option = comps.getFirst().changedOption();
                output.addElement(option);
            }
        }
        appendUnrepresentedOptions(output, options, optionComps.keySet());
        return output;
    }

    static void appendUnrepresentedOptions(Vector<IBasicOption> output, GameOptions options,
          Set<String> representedOptionNames) {
        for (Enumeration<IOptionGroup> groups = options.getGroups(); groups.hasMoreElements(); ) {
            IOptionGroup group = groups.nextElement();
            for (Enumeration<IOption> groupOptions = group.getOptions(); groupOptions.hasMoreElements(); ) {
                IOption option = groupOptions.nextElement();
                if (!representedOptionNames.contains(option.getName())) {
                    output.addElement(option);
                }
            }
        }
    }

    private void resetToDefaults(final ActionEvent ev) {
        for (List<DialogOptionComponentYPanel> comps : optionComps.values()) {
            for (DialogOptionComponentYPanel comp : comps) {
                if (!comp.isDefaultValue()) {
                    comp.resetToDefault();
                }
            }
        }
        synchronizeRuleToggles();
        toggleOptions();
    }

    public void refreshOptions() {
        String activeFilter = gameOptionsPane == null ? "" : gameOptionsPane.getFilterText();
        panOptions.removeAll();
        optionComps = new HashMap<>();
        List<GameOptionsPane.OptionGroup> groups = new ArrayList<>();
        for (Enumeration<IOptionGroup> i = options.getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();
            if (isVictoryGroupHiddenForLobby(group)) {
                continue;
            }
            List<DialogOptionComponentYPanel> groupComponents = new ArrayList<>();
            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements(); ) {
                IOption option = j.nextElement();
                if (excludedOptionNames.contains(option.getName())) {
                    continue;
                }
                DialogOptionComponentYPanel component = createOptionComponent(option);
                if (component != null) {
                    groupComponents.add(component);
                }
            }
            groups.add(new GameOptionsPane.OptionGroup(group.getName(), group.getDisplayableName(), groupComponents));
        }
        gameOptionsPane = new GameOptionsPane(groups, this::shouldShow, excludedOptionNames);
        panOptions.add(gameOptionsPane, BorderLayout.CENTER);
        synchronizeRuleToggles();
        toggleOptions();
        gameOptionsPane.setFilterText(activeFilter);
        panOptions.revalidate();
        panOptions.repaint();
        validate();
    }

    private void synchronizeRuleToggles() {
        // Without a backing hide option, the caller has chosen an always-visible category with no user toggle.
        butUnofficial.setVisible(hasBackingOption(OptionsConstants.BASE_HIDE_UNOFFICIAL));
        butLegacy.setVisible(hasBackingOption(OptionsConstants.BASE_HIDE_LEGACY));
        butUnofficial.setSelected(!backingOptionSelected(OptionsConstants.BASE_HIDE_UNOFFICIAL));
        butLegacy.setSelected(!backingOptionSelected(OptionsConstants.BASE_HIDE_LEGACY));
        updateRuleToggleText(butUnofficial, GameOptionsPane.unofficialBadge(),
              getTextAt(CLIENT_BUNDLE, "GameOptionsDialog.Unofficial"));
        updateRuleToggleText(butLegacy, GameOptionsPane.legacyBadge(),
              getTextAt(CLIENT_BUNDLE, "GameOptionsDialog.Legacy"));
    }

    private boolean backingOptionSelected(String optionName) {
        List<DialogOptionComponentYPanel> components = optionComps.get(optionName);
        return components != null && !components.isEmpty() && (Boolean) components.getFirst().getValue();
    }

    private boolean hasBackingOption(String optionName) {
        List<DialogOptionComponentYPanel> components = optionComps.get(optionName);
        return components != null && !components.isEmpty();
    }

    /**
     * @param group the option group about to be added as a tab
     *
     * @return {@code true} when the group is the victory options group and this dialog is used in the lobby - the
     *       lobby's Victory Conditions dialog edits those options instead. Scenario setup and mid-game use of this
     *       dialog keep the tab, as no lobby button exists in those contexts.
     */
    private boolean isVictoryGroupHiddenForLobby(IOptionGroup group) {
        boolean isInLobby = (clientGui != null) && clientGui.getClient().getGame().getPhase().isLounge();
        return isInLobby && VictoryConditionsDialog.VICTORY_OPTIONS_GROUP_NAME.equals(group.getName());
    }

    /** Refreshes unofficial/legacy visibility and dependent control state without changing hidden rule values. */
    private void toggleOptions() {
        refreshOptionPresentation(gameOptionsPane, optionComps, editable, selectedRulesSystem());
    }

    static void refreshOptionPresentation(GameOptionsPane pane,
          Map<String, List<DialogOptionComponentYPanel>> optionComponents, boolean dialogEditable,
          String rulesSystem) {
        pane.refreshVisibility();
        applyRulesSystemEditability(optionComponents, dialogEditable, rulesSystem);
    }

    static void deactivateOptions(Map<String, List<DialogOptionComponentYPanel>> optionComponents,
          Predicate<IOption> category) {
        for (List<DialogOptionComponentYPanel> components : optionComponents.values()) {
            // Each option in the list should have the same value, so picking the first is fine
            if (!components.isEmpty()) {
                DialogOptionComponentYPanel component = components.getFirst();
                if (!category.test(component.getOption())) {
                    continue;
                }
                if (component.getOption().getType() == IOption.BOOLEAN) {
                    component.setSelected(false);
                } else if (component.getOption().getName().equals(OptionsConstants.ADVANCED_GHOST_TARGET_MODE)) {
                    component.setValue(OptionsConstants.GHOST_TARGET_MODE_STANDARD);
                }
            }
        }
    }

    /** Returns true when the given Option should never show in the dialog. */
    private boolean isHiddenOption(IOption option) {
        return option.getName().equals(OptionsConstants.BASE_HIDE_UNOFFICIAL)
              || option.getName().equals(OptionsConstants.BASE_HIDE_LEGACY);
    }

    /** Returns true when the given Option should be visible in the dialog. */
    private boolean shouldShow(IOption option) {
        boolean isHiddenUnofficial = !butUnofficial.isSelected() && GameOptionsPane.isUnofficialOption(option);
        boolean isHiddenLegacy = !butLegacy.isSelected() && GameOptionsPane.isLegacyOption(option);
        return !(isHiddenUnofficial || isHiddenLegacy || isHiddenOption(option));
    }

    private void applyRulesSystemEditability() {
        applyRulesSystemEditability(optionComps, editable, selectedRulesSystem());
    }

    static void applyRulesSystemEditability(Map<String, List<DialogOptionComponentYPanel>> optionComponents,
          boolean dialogEditable, String rulesSystem) {
        boolean rulesOptionsEditable = dialogEditable
              && OptionsConstants.RULES_TW.equals(normalizeRulesSystem(rulesSystem));
        for (String optionName : CORE_RULES_DISABLED_OPTIONS) {
            List<DialogOptionComponentYPanel> components = optionComponents.get(optionName);
            if (components != null) {
                components.forEach(component -> component.setEditable(rulesOptionsEditable));
            }
        }
    }

    private String selectedRulesSystem() {
        List<DialogOptionComponentYPanel> components = optionComps.get(OptionsConstants.RULES_SYSTEM);
        if (components != null && !components.isEmpty()) {
            return normalizeRulesSystem(components.getFirst().getValue());
        }
        IOption rulesSystem = options.getOption(OptionsConstants.RULES_SYSTEM);
        return normalizeRulesSystem(rulesSystem == null ? null : rulesSystem.getValue());
    }

    static String normalizeRulesSystem(@Nullable Object value) {
        if (value instanceof String rulesSystem
              && (OptionsConstants.RULES_CORE.equals(rulesSystem) || OptionsConstants.RULES_TW.equals(rulesSystem))) {
            return rulesSystem;
        }
        LOGGER.debug("Unknown rules system '{}'; using '{}'.", value, OptionsConstants.RULES_CORE);
        return OptionsConstants.RULES_CORE;
    }

    /**
     * Creates an editor for an option.
     *
     * @param option option to represent; {@code null} returns no component
     *
     * @return the option editor, or {@code null} when {@code option} is {@code null}
     */
    private @Nullable DialogOptionComponentYPanel createOptionComponent(@Nullable IOption option) {
        if (option == null) {
            return null;
        }
        DialogOptionComponentYPanel optionComp = new DialogOptionComponentYPanel(this, option, true, true);

        if (OptionsConstants.INIT_INF_DEPLOY_EVEN.equals(option.getName())) {
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() ||
                  !(options.getOption(OptionsConstants.INIT_INF_MOVE_EVEN)).booleanValue() ||
                  !editable) {
                optionComp.setEditable(false);
            }
        } else if (OptionsConstants.INIT_INF_MOVE_MULTI.equals(option.getName())) {
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() ||
                  (options.getOption(OptionsConstants.INIT_INF_MOVE_EVEN)).booleanValue() ||
                  (options.getOption(OptionsConstants.INIT_INF_MOVE_LATER)).booleanValue() ||
                  !editable) {
                optionComp.setEditable(false);
            }
        } else if (OptionsConstants.INIT_INF_MOVE_EVEN.equals(option.getName())) {
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() ||
                  (options.getOption(OptionsConstants.INIT_INF_MOVE_MULTI)).booleanValue() ||
                  (options.getOption(OptionsConstants.INIT_INF_MOVE_LATER)).booleanValue() ||
                  !editable) {
                optionComp.setEditable(false);
            }
        } else if (OptionsConstants.INIT_INF_MOVE_LATER.equals(option.getName())) {
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() ||
                  (options.getOption(OptionsConstants.INIT_INF_MOVE_EVEN)).booleanValue() ||
                  (options.getOption(OptionsConstants.INIT_INF_MOVE_MULTI)).booleanValue() ||
                  !editable) {
                optionComp.setEditable(false);
            }
        } else if (OptionsConstants.INIT_PROTOMEKS_MOVE_EVEN.equals(option.getName())) {
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() ||
                  !(options.getOption(OptionsConstants.INIT_PROTOMEKS_MOVE_EVEN)).booleanValue() ||
                  !editable) {
                optionComp.setEditable(false);
            }
        } else if (OptionsConstants.INIT_PROTOMEKS_MOVE_MULTI.equals(option.getName())) {
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() ||
                  (options.getOption(OptionsConstants.INIT_PROTOMEKS_MOVE_EVEN)).booleanValue() ||
                  (options.getOption(OptionsConstants.INIT_PROTOMEKS_MOVE_LATER)).booleanValue() ||
                  !editable) {
                optionComp.setEditable(false);
            }
        } else if (OptionsConstants.INIT_PROTOMEKS_MOVE_EVEN.equals(option.getName())) {
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() ||
                  (options.getOption(OptionsConstants.INIT_PROTOMEKS_MOVE_MULTI)).booleanValue() ||
                  (options.getOption(OptionsConstants.INIT_PROTOMEKS_MOVE_LATER)).booleanValue() ||
                  !editable) {
                optionComp.setEditable(false);
            }
        } else if (OptionsConstants.INIT_PROTOMEKS_MOVE_LATER.equals(option.getName())) {
            if ((options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)).booleanValue() ||
                  (options.getOption(OptionsConstants.INIT_PROTOMEKS_MOVE_EVEN)).booleanValue() ||
                  (options.getOption(OptionsConstants.INIT_PROTOMEKS_MOVE_MULTI)).booleanValue() ||
                  !editable) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_FALLING_EXPANDED)) {
            if (!(options.getOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_HULL_DOWN)).booleanValue()
                  || !editable) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS1)) {
            if ((options.getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_DEAD_ZONES)).booleanValue() || !editable) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS_RANGE)) {
            if (!options.getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RANGE).booleanValue() || !editable) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_DEAD_ZONES)) {
            if ((options.getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS1)).booleanValue() || !editable) {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals(OptionsConstants.ADVANCED_COMBAT_KIND_RAPID_AC)) {
            if ((options.getOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RAPID_AC)).booleanValue()) {
                optionComp.setEditable(editable);
            } else {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals(OptionsConstants.ADVANCED_AERO_RULES_STRATOPS_BEARINGS_ONLY_VELOCITY)) {
            if (option.intValue() < CapitalMissileBayWeapon.CAPITAL_MISSILE_MIN_VELOCITY) {
                //Set to the minimum velocity if under
                option.setValue(CapitalMissileBayWeapon.CAPITAL_MISSILE_MIN_VELOCITY);
            } else if (option.intValue() > CapitalMissileBayWeapon.CAPITAL_MISSILE_MAX_VELOCITY) {
                //Set to the maximum velocity if over
                option.setValue(CapitalMissileBayWeapon.CAPITAL_MISSILE_MAX_VELOCITY);
            }
            optionComp.setEditable(editable);
        } else if (option.getName().equals(OptionsConstants.ADVANCED_ALTERNATE_MASC_ENHANCED)) {
            if ((options.getOption(OptionsConstants.ADVANCED_ALTERNATE_MASC)).booleanValue()) {
                optionComp.setEditable(editable);
            } else {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals("techlevel")) {
            for (String tlName : TechConstants.T_SIMPLE_NAMES) {
                optionComp.addValue(tlName);
            }
            optionComp.setSelected(option.stringValue());
            optionComp.setEditable(editable);
        } else if (option.getName().equals(OptionsConstants.ADVANCED_NEURAL_INTERFACE_MODE)) {
            optionComp.addValue(OptionsConstants.NEURAL_INTERFACE_MODE_OFF);
            optionComp.addValue(OptionsConstants.NEURAL_INTERFACE_MODE_PILOT_ONLY);
            optionComp.addValue(OptionsConstants.NEURAL_INTERFACE_MODE_FULL_TRACKING);
            optionComp.setSelected(option.stringValue());
            optionComp.setEditable(editable);
        } else if (option.getName().equals(OptionsConstants.GAME_MASTER_VOTE_THRESHOLD)) {
            optionComp.addValue(OptionsConstants.GAME_MASTER_VOTE_UNANIMOUS);
            optionComp.addValue(OptionsConstants.GAME_MASTER_VOTE_MAJORITY);
            optionComp.setSelected(option.stringValue());
            optionComp.setEditable(editable);
        } else if (option.getName().equals(OptionsConstants.RULES_SYSTEM)) {
            optionComp.addValue(OptionsConstants.RULES_CORE);
            optionComp.addValue(OptionsConstants.RULES_TW);
            optionComp.setSelected(option.stringValue());
            optionComp.setEditable(editable);
        } else if (option.getName().equals(OptionsConstants.ADVANCED_GHOST_TARGET_MODE)) {
            optionComp.addValue(OptionsConstants.GHOST_TARGET_MODE_LEGACY);
            optionComp.addValue(OptionsConstants.GHOST_TARGET_MODE_STANDARD);
            optionComp.setSelected(option.stringValue());
            // Mode dropdown only editable when TacOps Ghost Targets is enabled
            boolean ghostTargetsEnabled = options.getOption(
                  OptionsConstants.ADVANCED_TAC_OPS_GHOST_TARGET).booleanValue();
            optionComp.setEditable(editable && ghostTargetsEnabled);
        } else if (option.getName().equals(OptionsConstants.ADVANCED_GHOST_TARGET_MAX)) {
            // Ghost target max only applies to Legacy mode
            IOption advancedOption = options.getOption(OptionsConstants.ADVANCED_GHOST_TARGET_MODE);
            boolean isLegacyMode = advancedOption != null && OptionsConstants.GHOST_TARGET_MODE_LEGACY.equals(
                  advancedOption.stringValue());
            boolean ghostTargetsEnabled = options.getOption(
                  OptionsConstants.ADVANCED_TAC_OPS_GHOST_TARGET).booleanValue();
            optionComp.setEditable(editable && isLegacyMode && ghostTargetsEnabled);
        } else if (option.getName().equals(OptionsConstants.ADVANCED_GROUND_MOVEMENT_MEK_LANCE_MOVEMENT)) {
            // Disable if individual init is on
            if (!options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE).booleanValue()) {
                optionComp.setEditable(editable);
            } else {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_LANCE_MOVEMENT)) {
            // Disable if individual init is on
            if (!options.getOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE).booleanValue()) {
                optionComp.setEditable(editable);
            } else {
                optionComp.setEditable(false);
            }
        } else if (option.getName().equals(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)) {
            // Disable if any lance movement is on
            if (!options.getOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_LANCE_MOVEMENT).booleanValue() &&
                  !options.getOption(OptionsConstants.ADVANCED_GROUND_MOVEMENT_MEK_LANCE_MOVEMENT).booleanValue()) {
                optionComp.setEditable(editable);
            } else {
                optionComp.setEditable(false);
            }
        } else {
            optionComp.setEditable(editable);
        }
        List<DialogOptionComponentYPanel> comps = optionComps.computeIfAbsent(option.getName(), k -> new ArrayList<>());
        comps.add(optionComp);
        return optionComp;
    }

    // Gets called when one of the option checkboxes is clicked.
    // Arguments are the GameOption object and the true/false
    // state of the checkbox.
    @Override
    public void optionClicked(DialogOptionComponentYPanel clickedComp, IOption option, boolean state) {

        // Ensure that any other DialogOptionComponents with the same IOption
        // have the same value
        List<DialogOptionComponentYPanel> comps = optionComps.get(option.getName());
        for (DialogOptionComponentYPanel comp : comps) {
            if (!comp.equals(clickedComp)) {
                comp.setValue(clickedComp.getValue());
            }
        }

        if (OptionsConstants.INIT_INF_MOVE_EVEN.equals(option.getName())) {
            comps = optionComps.get(OptionsConstants.INIT_INF_DEPLOY_EVEN);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_MULTI);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_LATER);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
            }
        }
        if (OptionsConstants.INIT_INF_MOVE_MULTI.equals(option.getName())) {
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_EVEN);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_LATER);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
            }
        }
        if (OptionsConstants.INIT_INF_MOVE_LATER.equals(option.getName())) {
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_EVEN);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_MULTI);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
            }
        }
        if (OptionsConstants.INIT_PROTOMEKS_MOVE_EVEN.equals(option.getName())) {
            comps = optionComps.get(OptionsConstants.INIT_PROTOMEKS_MOVE_EVEN);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_PROTOMEKS_MOVE_MULTI);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
            }
            comps = optionComps.get(OptionsConstants.INIT_PROTOMEKS_MOVE_LATER);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
            }
        }
        if (OptionsConstants.INIT_PROTOMEKS_MOVE_MULTI.equals(option.getName())) {
            comps = optionComps.get(OptionsConstants.INIT_PROTOMEKS_MOVE_EVEN);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
            }
            comps = optionComps.get(OptionsConstants.INIT_PROTOMEKS_MOVE_LATER);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
            }
        }
        if (OptionsConstants.INIT_PROTOMEKS_MOVE_LATER.equals(option.getName())) {
            comps = optionComps.get(OptionsConstants.INIT_PROTOMEKS_MOVE_EVEN);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
            }
            comps = optionComps.get(OptionsConstants.INIT_PROTOMEKS_MOVE_MULTI);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
            }
        }
        if (option.getName().equals(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)) {
            comps = optionComps.get(OptionsConstants.INIT_PROTOMEKS_MOVE_EVEN);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(false);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_PROTOMEKS_MOVE_EVEN);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_PROTOMEKS_MOVE_MULTI);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_PROTOMEKS_MOVE_LATER);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_DEPLOY_EVEN);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(false);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_EVEN);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_MULTI);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.INIT_INF_MOVE_LATER);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.ADVANCED_GROUND_MOVEMENT_MEK_LANCE_MOVEMENT);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
            comps = optionComps.get(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_LANCE_MOVEMENT);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
        }
        if (option.getName().equals(OptionsConstants.ADVANCED_GROUND_MOVEMENT_VEHICLE_LANCE_MOVEMENT) ||
              option.getName().equals(OptionsConstants.ADVANCED_GROUND_MOVEMENT_MEK_LANCE_MOVEMENT)) {
            comps = optionComps.get(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
        }
        if ("vacuum".equals(option.getName())) {
            comps = optionComps.get("fire");
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
        }
        if (OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_HULL_DOWN.equals(option.getName())) {
            comps = optionComps.get(OptionsConstants.ADVANCED_GROUND_MOVEMENT_TAC_OPS_FALLING_EXPANDED);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(state);
                comp_i.setSelected(false);
            }
        }
        if (OptionsConstants.ADVANCED_COMBAT_TAC_OPS_DEAD_ZONES.equals(option.getName())) {
            comps = optionComps.get(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS1);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
        }
        if (OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RANGE.equals(option.getName())) {
            comps = optionComps.get(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS_RANGE);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(state);
                comp_i.setSelected(false);
            }
        }
        if (OptionsConstants.ADVANCED_COMBAT_TAC_OPS_LOS1.equals(option.getName())) {
            comps = optionComps.get(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_DEAD_ZONES);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(!state);
                comp_i.setSelected(false);
            }
        }
        if (option.getName().equals(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_RAPID_AC)) {
            comps = optionComps.get(OptionsConstants.ADVANCED_COMBAT_KIND_RAPID_AC);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(state);
                comp_i.setSelected(false);
            }
        }
        if (option.getName().equals(OptionsConstants.ADVANCED_ALTERNATE_MASC)) {
            comps = optionComps.get(OptionsConstants.ADVANCED_ALTERNATE_MASC_ENHANCED);
            for (DialogOptionComponentYPanel comp_i : comps) {
                comp_i.setEditable(state);
                comp_i.setSelected(false);
            }
        }
        if (option.getName().equals(OptionsConstants.ADVANCED_BA_GRAB_BARS)) {
            if (clientGui != null) {
                for (Entity ent : clientGui.getClient().getGame().getEntitiesVector()) {
                    if (ent instanceof Mek) {
                        ((Mek) ent).setBAGrabBars();
                    }
                    if (ent instanceof Tank) {
                        ((Tank) ent).setBAGrabBars();
                    }
                }
            }
        }
    }

    @Override
    public void optionSwitched(DialogOptionComponentYPanel clickedComp, IOption option, int i) {
        if (option.getName().equals(OptionsConstants.ADVANCED_GHOST_TARGET_MODE)) {
            boolean isLegacyMode = OptionsConstants.GHOST_TARGET_MODE_LEGACY.equals(clickedComp.getValue());
            List<DialogOptionComponentYPanel> maxComps = optionComps.get(OptionsConstants.ADVANCED_GHOST_TARGET_MAX);
            if (maxComps != null) {
                for (DialogOptionComponentYPanel maxComp : maxComps) {
                    maxComp.setEditable(editable && isLegacyMode);
                }
            }
        }
        if (option.getName().equals(OptionsConstants.RULES_SYSTEM)) {
            applyRulesSystemEditability();
        }
    }

    @Override
    protected void okAction() {
        if (clientGui != null) {
            send();
        }
        if (performSave) {
            doSave();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == butSave) {
            File gameOptsFile = selectGameOptionsFile(true);
            if (gameOptsFile != null) {
                GameOptions.saveOptions(getOptions(), gameOptsFile.getAbsolutePath());
            }

        } else if (e.getSource() == butLoad) {
            File gameOptsFile = selectGameOptionsFile(false);
            if (gameOptsFile != null) {
                loadOptionsPreservingExcluded(options, gameOptsFile, excludedOptionNames);
                ArrayList<IOption> changed = new ArrayList<>();
                for (List<DialogOptionComponentYPanel> comps : optionComps.values()) {
                    // Each option in the list should have the same value, so picking the first is fine
                    if (!comps.isEmpty()) {
                        DialogOptionComponentYPanel comp = comps.getFirst();
                        if (comp.hasChanged()) {
                            changed.add(comp.getOption());
                        }
                    }
                }
                refreshOptions();
                // We need to ensure that the IOption for the component doesn't
                // match, otherwise send() won't send updates to the server
                for (IOption opt : changed) {
                    List<DialogOptionComponentYPanel> comps = optionComps.get(opt.getName());
                    if (!comps.isEmpty()) {
                        comps.getFirst().setOptionChanged(true);
                    }
                }
            }

        } else if (e.getSource().equals(butUnofficial)) {
            ruleToggleChanged(butUnofficial, OptionsConstants.BASE_HIDE_UNOFFICIAL,
                  GameOptionsPane.unofficialBadge(), "GameOptionsDialog.Unofficial",
                  GameOptionsPane::isUnofficialOption);
        } else if (e.getSource().equals(butLegacy)) {
            ruleToggleChanged(butLegacy, OptionsConstants.BASE_HIDE_LEGACY,
                  GameOptionsPane.legacyBadge(), "GameOptionsDialog.Legacy", GameOptionsPane::isLegacyOption);

        }
    }

    static void loadOptionsPreservingExcluded(GameOptions options, File file, Set<String> excludedOptionNames) {
        Map<String, Object> excludedValues = new HashMap<>();
        for (String optionName : excludedOptionNames) {
            IOption option = options.getOption(optionName);
            if (option != null) {
                excludedValues.put(optionName, option.getValue());
            }
        }
        options.loadOptions(file, false);
        excludedValues.forEach((optionName, value) -> options.getOption(optionName).setValue(value));
    }

    private void ruleToggleChanged(JToggleButton button, String backingOptionName, SettingsBadge badge,
          String labelKey, Predicate<IOption> category) {
        if (!hasBackingOption(backingOptionName)) {
            return;
        }
        if (!button.isSelected() && !MMConfirmDialog.confirm(frame,
            getTextAt(CLIENT_BUNDLE, "GameOptionsDialog.warning.title"),
            getTextAt(CLIENT_BUNDLE, "GameOptionsDialog.HideWarning"))) {
            button.setSelected(true);
            updateRuleToggleText(button, badge, getTextAt(CLIENT_BUNDLE, labelKey));
            return;
        }
        optionComps.get(backingOptionName).getFirst().setSelected(!button.isSelected());
        if (!button.isSelected()) {
            deactivateOptions(optionComps, category);
        }
        updateRuleToggleText(button, badge, getTextAt(CLIENT_BUNDLE, labelKey));
        toggleOptions();
    }

    private File selectGameOptionsFile(boolean saveDialog) {
        JFileChooser fc = new JFileChooser("mmconf");
        fc.setLocation(getLocation().x + 150, getLocation().y + 100);
        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                if (dir.isDirectory()) {
                    return true;
                } else if (dir.getName().endsWith(".xml")) {
                    try {
                        DocumentBuilder builder = MMXMLUtility.newSafeDocumentBuilder();
                        Document doc = builder.parse(dir);
                        NodeList listOfComponents = doc.getElementsByTagName("options");
                        return listOfComponents.getLength() > 0;
                    } catch (Exception e) {
                        return false;
                    }
                } else {
                    return false;
                }
            }

            @Override
            public String getDescription() {
                return "GameOptions";
            }
        });
        int returnVal = saveDialog ? fc.showSaveDialog(this) : fc.showOpenDialog(this);
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (fc.getSelectedFile() == null)) {
            return null;
        }
        File result = fc.getSelectedFile();
        if (!result.getName().endsWith(".xml")) {
            result = new File(result + ".xml");
        }
        return result;
    }

    /**
     * Update the dialog so that it is editable or view-only.
     *
     * @param editable - <code>true</code> if the contents of the dialog are editable, <code>false</code> if they are
     *                 view-only.
     */
    public void setEditable(boolean editable) {

        // Set enabled state of all the option components in the dialog.
        for (List<DialogOptionComponentYPanel> comps : optionComps.values()) {
            for (DialogOptionComponentYPanel comp : comps) {
                comp.setEditable(editable);
            }
        }

        this.editable = editable;
        applyRulesSystemEditability();

        // If the panel is editable, the player can commit or reset.
        texPass.setEnabled(editable);
        butOkay.setEnabled(editable);
        butUnofficial.setEnabled(editable);
        butLegacy.setEnabled(editable);
        // Resetting and loading only change values that a view-only dialog can never commit.
        butDefaults.setVisible(editable);
        butLoad.setVisible(editable);
        revalidate();
        repaint();
    }

    /**
     * Determine whether the dialog is editable or view-only.
     *
     * @return <code>true</code> if the contents of the dialog are editable,
     *       <code>false</code> if they are view-only.
     */
    public boolean isEditable() {
        return editable;
    }
}
