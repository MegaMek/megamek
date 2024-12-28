/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */

package megamek.client.ui.swing.ai.editor;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import megamek.ai.utility.Action;
import megamek.ai.utility.NamedObject;
import megamek.client.bot.duchess.ai.utility.tw.TWUtilityAIRepository;
import megamek.client.bot.duchess.ai.utility.tw.considerations.TWConsideration;
import megamek.client.bot.duchess.ai.utility.tw.decision.TWDecision;
import megamek.client.bot.duchess.ai.utility.tw.decision.TWDecisionScoreEvaluator;
import megamek.client.bot.duchess.ai.utility.tw.profile.TWProfile;
import megamek.client.ui.Messages;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.MegaMekController;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Method;
import java.util.List;
import java.util.ResourceBundle;

public class AiProfileEditor extends JFrame {
    private final TWUtilityAIRepository sharedData = TWUtilityAIRepository.getInstance();
    private final GUIPreferences guip = GUIPreferences.getInstance();
    private final MegaMekController controller;

    private JButton newDecisionButton;
    private JTree repositoryViewer;
    private JTabbedPane mainEditorTabbedPane;
    private JPanel dseTabPane;
    private JTextField descriptionTextField;
    private JTextField profileNameTextField;
    private JButton newConsiderationButton;
    private JPanel profileTabPane;
    private JTable profileDecisionTable;
    private JPanel decisionTabPane;
    private JComboBox<Action> actionComboBox;
    private JSpinner weightSpinner;
    private JPanel uAiEditorPanel;
    private JScrollPane profileScrollPane;
    private JPanel decisionTabDsePanel;
    private JPanel dsePane;
    private JPanel considerationTabPane;
    private JPanel considerationEditorPanel;
    private ConsiderationPane considerationPane;


    private boolean hasChanges = true;
    private boolean ignoreHotKeys = false;

    public AiProfileEditor(MegaMekController controller) {
        this.controller = controller;
        $$$setupUI$$$();
        initialize();
        setTitle("AI Profile Editor");
        setSize(1200, 1000);
        setContentPane(uAiEditorPanel);
        setVisible(true);
    }

    private void initialize() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.fill = GridBagConstraints.EAST;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.NORTHEAST;
        gbc.insets = new Insets(0, 0, 0, 0);
        considerationPane = new ConsiderationPane();
        considerationPane.setMinimumSize(new Dimension(considerationEditorPanel.getWidth(), considerationEditorPanel.getHeight()));
        considerationEditorPanel.add(considerationPane, gbc);

        newDecisionButton.addActionListener(e -> {
            var action = (Action) actionComboBox.getSelectedItem();
            var weight = (double) weightSpinner.getValue();
            var dse = new TWDecision(action, weight);
            var model = profileDecisionTable.getModel();
            //noinspection unchecked
            ((DecisionTableModel<TWDecision>) model).addRow(dse);
        });

        newConsiderationButton.addActionListener(e -> {
            var action = (Action) actionComboBox.getSelectedItem();
            var weight = (double) weightSpinner.getValue();
            var dse = new TWDecision(action, weight);
            var model = profileDecisionTable.getModel();
            //noinspection unchecked
            ((DecisionTableModel<TWDecision>) model).addRow(dse);
        });

        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // When the board has changes, ask the user
                if (!hasChanges || (showSavePrompt() != DialogResult.CANCELLED)) {
                    if (controller != null) {
                        controller.removeAllActions();
                        controller.aiEditor = null;
                    }
                    getFrame().dispose();
                }

            }
        });


        // Add mouse listener for double-click events
        repositoryViewer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = repositoryViewer.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        if (node.isLeaf()) {
                            handleNodeAction(node);
                        }
                    }
                }
            }
        });

        repositoryViewer.addTreeSelectionListener(e -> {
            TreePath path = e.getPath();
            if (path != null) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                if (node.isLeaf()) {
                    handleNodeAction(node);
                }
            }
        });
    }


    private void handleNodeAction(DefaultMutableTreeNode node) {
        var obj = node.getUserObject();
        if (obj instanceof TWDecision twDecision) {
            openDecision(twDecision);
        } else if (obj instanceof TWProfile twProfile) {
            openProfile(twProfile);
        } else if (obj instanceof TWDecisionScoreEvaluator twDse) {
            openDecisionScoreEvaluator(twDse);
        } else if (obj instanceof TWConsideration twConsideration) {
            openConsideration(twConsideration);
        }
    }

//    decisionScoreEvaluatorTable = new DecisionScoreEvaluatorTable<>(model, Action.values(), sharedData.getDecisionScoreEvaluators());
//    decisionTabDsePanel = new DecisionScoreEvaluatorPane();
//    dsePane = new DecisionScoreEvaluatorPane();

    private void openConsideration(TWConsideration twConsideration) {
        considerationPane.setConsideration(twConsideration);
        mainEditorTabbedPane.setSelectedComponent(considerationTabPane);
    }

    private void openDecision(TWDecision twDecision) {
        ((DecisionScoreEvaluatorPane) decisionTabDsePanel).setDecisionScoreEvaluator(twDecision.getDecisionScoreEvaluator());
        mainEditorTabbedPane.setSelectedComponent(decisionTabPane);
    }

    private void openProfile(TWProfile twProfile) {
        // profileTab.setProfile(twProfile);
        profileNameTextField.setText(twProfile.getName());
        descriptionTextField.setText(twProfile.getDescription());
        profileDecisionTable.setModel(new DecisionTableModel<>(twProfile.getDecisions()));
        mainEditorTabbedPane.setSelectedComponent(profileTabPane);
    }

    private void openDecisionScoreEvaluator(TWDecisionScoreEvaluator twDse) {
        ((DecisionScoreEvaluatorPane) dsePane).setDecisionScoreEvaluator(twDse);
        mainEditorTabbedPane.setSelectedComponent(dseTabPane);
    }

    private DialogResult showSavePrompt() {
        ignoreHotKeys = true;
        int savePrompt = JOptionPane.showConfirmDialog(null,
            Messages.getString("BoardEditor.exitprompt"),
            Messages.getString("BoardEditor.exittitle"),
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE);
        ignoreHotKeys = false;
        // When the user cancels or did not actually save the board, don't load anything
        if (((savePrompt == JOptionPane.YES_OPTION) && !hasChanges)
            || (savePrompt == JOptionPane.CANCEL_OPTION)
            || (savePrompt == JOptionPane.CLOSED_OPTION)) {
            return DialogResult.CANCELLED;
        } else {
            persistProfile();
            return DialogResult.CONFIRMED;
        }
    }

    private void persistProfile() {
        //noinspection unchecked
        var model = (DecisionTableModel<TWDecision>) profileDecisionTable.getModel();
        var updatedList = model.getDecisions();
        System.out.println("== Updated DecisionScoreEvaluator List ==");
        for (int i = 0; i < updatedList.size(); i++) {
            var dse = updatedList.get(i);
            System.out.printf("Row %d -> Decision: %s, Evaluator: %s%n",
                i,
                dse.getAction().getActionName(),
                dse.getDecisionScoreEvaluator().getName());
            sharedData.addDecision(dse);
        }
        sharedData.persistDataToUserData();
    }

    public JFrame getFrame() {
        return this;
    }

    private enum TreeViewHelper {
        PROFILES("Profiles"),
        DECISIONS("Decisions"),
        DSE("Decision Score Evaluators (DSE)"),
        CONSIDERATIONS("Considerations");

        private final String name;

        TreeViewHelper(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private void createUIComponents() {
        weightSpinner = new JSpinner(new SpinnerNumberModel(1d, 0d, 4d, 0.01d));

        var root = new DefaultMutableTreeNode(Messages.getString("aiEditor.tree.title"));
        addToMutableTreeNode(root, TreeViewHelper.PROFILES.getName(), sharedData.getProfiles());
        addToMutableTreeNode(root, TreeViewHelper.DECISIONS.getName(), sharedData.getDecisions());
        addToMutableTreeNode(root, TreeViewHelper.DSE.getName(), sharedData.getDecisionScoreEvaluators());
        addToMutableTreeNode(root, TreeViewHelper.CONSIDERATIONS.getName(), sharedData.getConsiderations());
        DefaultTreeModel treeModel = new DefaultTreeModel(root);

        repositoryViewer = new JTree(treeModel);
        actionComboBox = new JComboBox<>(Action.values());
        var model = new DecisionTableModel<>(sharedData.getDecisions());
        profileDecisionTable = new DecisionScoreEvaluatorTable<>(model, Action.values(), sharedData.getDecisionScoreEvaluators());
        decisionTabDsePanel = new DecisionScoreEvaluatorPane();
        dsePane = new DecisionScoreEvaluatorPane();

    }

    private <T extends NamedObject> void addToMutableTreeNode(DefaultMutableTreeNode root, String nodeName, List<T> items) {
        var categoryNode = new DefaultMutableTreeNode(nodeName);
        root.add(categoryNode);
        for (var item : items) {
            var childNode = new DefaultMutableTreeNode(item);
            categoryNode.add(childNode);
        }
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        uAiEditorPanel = new JPanel();
        uAiEditorPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JSplitPane splitPane1 = new JSplitPane();
        uAiEditorPanel.add(splitPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane1.setLeftComponent(panel1);
        newDecisionButton = new JButton();
        this.$$$loadButtonText$$$(newDecisionButton, this.$$$getMessageFromBundle$$$("megamek/common/options/messages", "aiEditor.newDecision"));
        panel1.add(newDecisionButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(233, 34), null, 0, false));
        newConsiderationButton = new JButton();
        this.$$$loadButtonText$$$(newConsiderationButton, this.$$$getMessageFromBundle$$$("megamek/common/options/messages", "aiEditor.newConsideration"));
        panel1.add(newConsiderationButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, new Dimension(233, 34), null, 0, false));
        panel1.add(repositoryViewer, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(233, 50), null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane1.setRightComponent(panel2);
        mainEditorTabbedPane = new JTabbedPane();
        panel2.add(mainEditorTabbedPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        profileTabPane = new JPanel();
        profileTabPane.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainEditorTabbedPane.addTab(this.$$$getMessageFromBundle$$$("megamek/common/options/messages", "aiEditor.profile"), profileTabPane);
        profileScrollPane = new JScrollPane();
        profileScrollPane.setWheelScrollingEnabled(true);
        profileTabPane.add(profileScrollPane, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        profileDecisionTable.setColumnSelectionAllowed(false);
        profileDecisionTable.setFillsViewportHeight(true);
        profileDecisionTable.setMinimumSize(new Dimension(150, 32));
        profileDecisionTable.setPreferredScrollableViewportSize(new Dimension(150, 32));
        profileScrollPane.setViewportView(profileDecisionTable);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        profileTabPane.add(panel3, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        profileNameTextField = new JTextField();
        panel3.add(profileNameTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        descriptionTextField = new JTextField();
        panel3.add(descriptionTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("megamek/common/options/messages", "aiEditor.profile.name"));
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, this.$$$getMessageFromBundle$$$("megamek/common/options/messages", "AiEditor.description"));
        panel3.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        decisionTabPane = new JPanel();
        decisionTabPane.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainEditorTabbedPane.addTab(this.$$$getMessageFromBundle$$$("megamek/common/options/messages", "aiEditor.tab.decision"), decisionTabPane);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 5, new Insets(0, 0, 0, 0), -1, -1));
        decisionTabPane.add(panel4, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        this.$$$loadLabelText$$$(label3, this.$$$getMessageFromBundle$$$("megamek/common/options/messages", "aiEditor.action"));
        panel4.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        actionComboBox.setEditable(false);
        panel4.add(actionComboBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(300, -1), null, null, 0, false));
        panel4.add(weightSpinner, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(100, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final JLabel label4 = new JLabel();
        this.$$$loadLabelText$$$(label4, this.$$$getMessageFromBundle$$$("megamek/common/options/messages", "aiEditor.weight"));
        panel4.add(label4, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel4.add(spacer1, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel4.add(scrollPane1, new GridConstraints(1, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        scrollPane1.setViewportView(decisionTabDsePanel);
        dseTabPane = new JPanel();
        dseTabPane.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        dseTabPane.setName("");
        mainEditorTabbedPane.addTab(this.$$$getMessageFromBundle$$$("megamek/common/options/messages", "aiEditor.tab.dse"), dseTabPane);
        final JScrollPane scrollPane2 = new JScrollPane();
        scrollPane2.setHorizontalScrollBarPolicy(31);
        scrollPane2.setWheelScrollingEnabled(true);
        dseTabPane.add(scrollPane2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        scrollPane2.setViewportView(dsePane);
        considerationTabPane = new JPanel();
        considerationTabPane.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainEditorTabbedPane.addTab(this.$$$getMessageFromBundle$$$("megamek/common/options/messages", "aiEditor.tab.consideration"), considerationTabPane);
        final JScrollPane scrollPane3 = new JScrollPane();
        considerationTabPane.add(scrollPane3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        considerationEditorPanel = new JPanel();
        considerationEditorPanel.setLayout(new GridBagLayout());
        scrollPane3.setViewportView(considerationEditorPanel);
        label3.setLabelFor(actionComboBox);
        label4.setLabelFor(weightSpinner);
    }

    private static Method $$$cachedGetBundleMethod$$$ = null;

    private String $$$getMessageFromBundle$$$(String path, String key) {
        ResourceBundle bundle;
        try {
            Class<?> thisClass = this.getClass();
            if ($$$cachedGetBundleMethod$$$ == null) {
                Class<?> dynamicBundleClass = thisClass.getClassLoader().loadClass("com.intellij.DynamicBundle");
                $$$cachedGetBundleMethod$$$ = dynamicBundleClass.getMethod("getBundle", String.class, Class.class);
            }
            bundle = (ResourceBundle) $$$cachedGetBundleMethod$$$.invoke(null, path, thisClass);
        } catch (Exception e) {
            bundle = ResourceBundle.getBundle(path);
        }
        return bundle.getString(key);
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadLabelText$$$(JLabel component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) break;
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setDisplayedMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    private void $$$loadButtonText$$$(AbstractButton component, String text) {
        StringBuffer result = new StringBuffer();
        boolean haveMnemonic = false;
        char mnemonic = '\0';
        int mnemonicIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&') {
                i++;
                if (i == text.length()) break;
                if (!haveMnemonic && text.charAt(i) != '&') {
                    haveMnemonic = true;
                    mnemonic = text.charAt(i);
                    mnemonicIndex = result.length();
                }
            }
            result.append(text.charAt(i));
        }
        component.setText(result.toString());
        if (haveMnemonic) {
            component.setMnemonic(mnemonic);
            component.setDisplayedMnemonicIndex(mnemonicIndex);
        }
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return uAiEditorPanel;
    }

}
