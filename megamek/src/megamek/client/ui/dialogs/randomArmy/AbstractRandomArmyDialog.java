/*
 * Copyright (C) 2006 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2006-2026 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.dialogs.randomArmy;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.CloseAction;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.client.ui.models.UnitTableModel;
import megamek.client.ui.util.UIUtil;
import megamek.common.loaders.MekSummary;
import megamek.common.options.GameOptions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 * This class is the framework for the random army dialog that is most prominently used in MM's lobby. Subclasses of it
 * can be used anywhere. It requires the data/names and data/rat folders for full functionality. Subclasses can generate
 * a button panel that allows interaction with the results (see the present subclasses for examples). Subclasses or
 * callers can also supply GameOptions that influence some of the generators.
 */
public abstract class AbstractRandomArmyDialog extends JDialog {

    // TODO: provide a common results API

    protected static final int TAB_FORCE_GENERATOR = 5;

    private static final String CARD_PREVIEW = "card_preview";
    private static final String CARD_FORCE_TREE = "card_force_tree";

    protected final JFrame parentFrame;

    protected final JTabbedPane tabbedPane = new JTabbedPane();
    private final Map<Integer, RandomArmyTab> generators = new HashMap<>();

    protected final ForceGeneratorViewUi forceGeneratorPanel;
    protected ForceGenerationOptionsPanel formationPanel;

    private final RandomArmyBvTab bvTab;
    private final RandomArmyRatGenTab ratGenTab;

    private final JComponent previewPanel = Box.createVerticalBox();

    private final CardLayout previewPaneCardLayout = new CardLayout();
    private final JPanel previewPane = new JPanel(previewPaneCardLayout);
    private final JSplitPane splitPane;

    private final JButton addAllButton = new JButton(Messages.getString("RandomArmyDialog.AddAll"));
    private final JButton addSelectedButton = new JButton(Messages.getString("RandomArmyDialog.AddSelected"));
    private final JButton rollButton = new JButton(Messages.getString("RandomArmyDialog.Roll"));
    private final JButton clearButton = new JButton(Messages.getString("RandomArmyDialog.Clear"));

    protected UnitTableModel chosenUnitsModel = new UnitTableModel();
    private final JTable chosenUnitsTable = new RandomArmyUnitTable(chosenUnitsModel);
    private final JLabel chosenBvTotalLabel = new JLabel();

    protected UnitTableModel rolledUnitsModel = new UnitTableModel();
    private final JTable rolledUnitsTable = new RandomArmyUnitTable(rolledUnitsModel);
    private final JLabel rolledBvTotalLabel = new JLabel();

    private JComponent buttonPanel;

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    private GameOptions gameOptions = new GameOptions();

    public AbstractRandomArmyDialog(JFrame parent) {
        super(parent, Messages.getString("RandomArmyDialog.title"), ModalityType.APPLICATION_MODAL);
        parentFrame = parent;

        createFormationPanel();
        createPreviewPanel();
        forceGeneratorPanel = new ForceGeneratorViewUi(parentFrame, gameOptions);

        bvTab = new RandomArmyBvTab(parentFrame, gameOptions);
        var simpleTab = new SimpleRandomLancePanel(this);
        var ratTab = new RandomArmyRatTab();
        ratGenTab = new RandomArmyRatGenTab(this, gameOptions);

        addTab(Messages.getString("RandomArmyDialog.BVtab"), new BorderlessScrollPane(bvTab), bvTab);
        addTab(Messages.getString("RandomArmyDialog.SimpleTab"), new BorderlessScrollPane(simpleTab), simpleTab);
        addTab(Messages.getString("RandomArmyDialog.RATtab"), ratTab, ratTab);
        addTab(Messages.getString("RandomArmyDialog.RATGentab"), ratGenTab, ratGenTab);
        addTab(Messages.getString("RandomArmyDialog.Formationtab"),
              new BorderlessScrollPane(formationPanel),
              formationPanel);
        tabbedPane.addTab(Messages.getString("RandomArmyDialog.Forcetab"), forceGeneratorPanel.getLeftPanel());

        tabbedPane.addChangeListener(ev -> {
            if (tabbedPane.getSelectedIndex() == TAB_FORCE_GENERATOR) {
                previewPaneCardLayout.show(previewPane, CARD_FORCE_TREE);
            } else {
                previewPaneCardLayout.show(previewPane, CARD_PREVIEW);
            }
        });

        previewPane.add(previewPanel, CARD_PREVIEW);
        previewPane.add(forceGeneratorPanel.getRightPanel(), CARD_FORCE_TREE);
        previewPane.setMinimumSize(new Dimension(0, 0));

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tabbedPane, previewPane);
        splitPane.setResizeWeight(0.5);

        // construct the main dialog
        add(splitPane, BorderLayout.CENTER);
        setLocationRelativeTo(parentFrame);

        splitPane.setDividerLocation(GUIP.getRndArmySplitPos());
        setSize(GUIP.getRndArmySizeWidth(), GUIP.getRndArmySizeHeight());
        setLocation(GUIP.getRndArmyPosX(), GUIP.getRndArmyPosY());

        updateBvTotals();

        String closeAction = "closeAction";
        final KeyStroke escape = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escape, closeAction);
        getRootPane().getInputMap(JComponent.WHEN_FOCUSED).put(escape, closeAction);
        getRootPane().getActionMap().put(closeAction, new CloseAction(this));

        addWindowListener(windowListener);
    }

    static class BorderlessScrollPane extends JScrollPane {
        public BorderlessScrollPane(Component view) {
            super(view);
            setBorder(null);
            getVerticalScrollBar().setUnitIncrement(16);
        }
    }

    private void addTab(String name, JComponent tabComponent, RandomArmyTab generatorTab) {
        tabbedPane.addTab(name, tabComponent);
        generators.put(tabbedPane.indexOfComponent(tabComponent), generatorTab);
    }

    public void setGameOptions(GameOptions newOptions) {
        gameOptions = newOptions;
        bvTab.setGameOptions(newOptions);
        ratGenTab.setGameOptions(newOptions);
        updateYear();
    }

    /**
     * Override to add buttons or other components to the bottom of the dialog. The returned component is added when
     * setVisible(true) is called for the first time; it is added as BorderLayout.SOUTH and is therefore stretched to
     * the width of the dialog.
     *
     * @return A button panel for the dialog
     */
    protected abstract JComponent createButtonsPanel();

    private void createFormationPanel() {
        formationPanel = new ForceGenerationOptionsPanel(ForceGenerationOptionsPanel.Use.FORMATION_BUILDER);
        formationPanel.setYear(gameOptions.intOption("year"));
    }

    private void createPreviewPanel() {
        rolledUnitsTable.setName("Rolled Units");
        JScrollPane rolledUnitsScrollPane = new JScrollPane(rolledUnitsTable);
        rolledUnitsScrollPane.setBorder(BorderFactory.createTitledBorder(
              Messages.getString("RandomArmyDialog.Army")));

        chosenUnitsTable.setName("Chosen Units");
        JScrollPane chosenUnitsScrollPane = new JScrollPane(chosenUnitsTable);
        chosenUnitsScrollPane.setBorder(BorderFactory.createTitledBorder(
              Messages.getString("RandomArmyDialog.SelectedUnits")));

        rolledBvTotalLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        chosenBvTotalLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel addButtonsPanel = new JPanel();
        addButtonsPanel.add(rollButton);
        addButtonsPanel.add(addAllButton);
        addButtonsPanel.add(addSelectedButton);
        JPanel clearButtonPanel = new JPanel(); // required for harmonious button sizing
        clearButtonPanel.add(clearButton);

        JPanel midButtonsPanel = new UIUtil.FixedYPanel(new BorderLayout());
        midButtonsPanel.add(addButtonsPanel, BorderLayout.WEST);
        midButtonsPanel.add(clearButtonPanel, BorderLayout.EAST);

        previewPanel.add(rolledUnitsScrollPane);
        previewPanel.add(rolledBvTotalLabel);
        previewPanel.add(midButtonsPanel);
        previewPanel.add(chosenUnitsScrollPane);
        previewPanel.add(chosenBvTotalLabel);
        previewPanel.setMinimumSize(new Dimension(0, 0));

        rollButton.addActionListener(e -> roll());
        addAllButton.addActionListener(e -> addAll());
        addSelectedButton.addActionListener(e -> addSelected());
        clearButton.addActionListener(e -> clearData());
    }

    private void addAll() {
        rolledUnitsModel.getAllUnits().forEach(chosenUnitsModel::addUnit);
        updateBvTotals();
    }

    private void addSelected() {
        Arrays.stream(rolledUnitsTable.getSelectedRows())
              .map(rolledUnitsTable::convertRowIndexToModel)
              .mapToObj(rolledUnitsModel::getUnitAt)
              .forEach(chosenUnitsModel::addUnit);
        updateBvTotals();
    }

    private void roll() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            if (generators.containsKey(tabbedPane.getSelectedIndex())) {
                RandomArmyTab randomArmyTab = generators.get(tabbedPane.getSelectedIndex());
                rolledUnitsModel.setData(randomArmyTab.generateMekSummaries());
                updateBvTotals();
            }
        } finally {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    WindowListener windowListener = new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent evt) {
            saveWindowSettings();
        }

        private void saveWindowSettings() {
            GUIP.setRndArmySizeHeight(getSize().height);
            GUIP.setRndArmySizeWidth(getSize().width);
            GUIP.setRndArmyPosX(getLocation().x);
            GUIP.setRndArmyPosY(getLocation().y);
            GUIP.setRndArmySplitPos(splitPane.getDividerLocation());
        }
    };

    protected void updateYear() {
        int gameYear = gameOptions.intOption("year");
        formationPanel.setYear(gameYear);
        forceGeneratorPanel.setYear(gameYear);
    }

    @Override
    public void setVisible(boolean show) {
        if (show) {
            if (buttonPanel == null) {
                buttonPanel = createButtonsPanel();
                add(buttonPanel, BorderLayout.SOUTH);
            }
        }
        super.setVisible(show);
    }

    /**
     * Clears all rolled results from all of the tabs.
     */
    protected void clearData() {
        chosenUnitsModel.clearData();
        rolledUnitsModel.clearData();
        updateBvTotals();
    }

    private void updateBvTotals() {
        String rolledTotal = Messages.getString("RandomArmyDialog.BVTotal", calculateTotal(rolledUnitsTable));
        rolledBvTotalLabel.setText(rolledTotal);
        String chosenTotal = Messages.getString("RandomArmyDialog.BVTotal", calculateTotal(chosenUnitsTable));
        chosenBvTotalLabel.setText(chosenTotal);
    }

    protected void addToChosenUnits(MekSummary unit) {
        chosenUnitsModel.addUnit(unit);
        updateBvTotals();
    }

    private int calculateTotal(JTable t) {
        int total = 0;
        for (int i = 0; i < t.getRowCount(); i++) {
            try {
                total += Integer.parseInt(t.getValueAt(i, 1) + "");
            } catch (Exception ignored) {
            }
        }
        return total;
    }
}
