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
import megamek.client.bot.duchess.ai.utility.tw.decision.TWDecision;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.MegaMekController;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.List;

public class AiProfileEditor extends JFrame {
    private final TWUtilityAIRepository sharedData = TWUtilityAIRepository.getInstance();
    private final GUIPreferences guip = GUIPreferences.getInstance();
    private final MegaMekController controller;

    private JButton newDecisionButton;
    private JTree repositoryViewer;
    private JTabbedPane dseEditorPane;
    private JPanel dseConfigPane;
    private JTextField nameDseTextField;
    private JTextField descriptionDseTextField;
    private JScrollPane considerationsScrollPane;
    private JPanel considerationsPane;
    private JTextField notesTextField;
    private JTextField descriptionTextField;
    private JTextField profileNameTextField;
    private JButton newConsiderationButton;
    private JPanel profilePane;
    private JTable decisionScoreEvaluatorTable;
    private JPanel decisionPane;
    private JComboBox<Action> actionComboBox;
    private JSpinner weightSpinner;
    private JScrollPane evaluatorScrollPane;
    private JPanel uAiEditorPanel;

    public AiProfileEditor(MegaMekController controller) {
        this.controller = controller;
        $$$setupUI$$$();
        initialize();
        setTitle("AI Profile Editor");
        setSize(1200, 800);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setContentPane(uAiEditorPanel);
        setVisible(true);
    }

    private void initialize() {
        considerationsScrollPane.setViewportView(new ConsiderationPane());
        evaluatorScrollPane.setViewportView(new DecisionScoreEvaluatorPane());
        newDecisionButton.addActionListener(e -> {
            var action = (Action) actionComboBox.getSelectedItem();
            var weight = (double) weightSpinner.getValue();
            var dse = new TWDecision(action, weight, sharedData.getDecisionScoreEvaluators().get(0));
            var model = decisionScoreEvaluatorTable.getModel();
            //noinspection unchecked
            ((DecisionScoreEvaluatorTableModel<TWDecision>) model).addRow(dse);
        });
    }

    private void persistProfile() {
        var model = (DecisionScoreEvaluatorTableModel<TWDecision>) decisionScoreEvaluatorTable.getModel();
        var updatedList = model.getDecisions();
        System.out.println("== Updated DecisionScoreEvaluator List ==");
        for (int i = 0; i < updatedList.size(); i++) {
            var dse = updatedList.get(i);
            System.out.printf("Row %d -> Decision: %s, Evaluator: %s%n",
                i,
                dse.getAction().getActionName(),
                dse.getDecisionScoreEvaluator().getName());
        }
    }

    public JFrame getFrame() {
        return this;
    }

    private void createUIComponents() {
        weightSpinner = new JSpinner(new SpinnerNumberModel(1d, 0d, 4d, 0.01d));

        var root = new DefaultMutableTreeNode("Utility AI Repository");
        addToMutableTreeNode(root, "Profiles", sharedData.getProfiles());
        addToMutableTreeNode(root, "Decisions", sharedData.getDecisions());
        addToMutableTreeNode(root, "Considerations", sharedData.getConsiderations());
        addToMutableTreeNode(root, "Decision Score Evaluators (DSE)", sharedData.getDecisionScoreEvaluators());
        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        repositoryViewer = new JTree(treeModel);

        actionComboBox = new JComboBox<>(Action.values());

        var model = new DecisionScoreEvaluatorTableModel<>(sharedData.getDecisions());
        decisionScoreEvaluatorTable = new DecisionScoreEvaluatorTable<>(model, Action.values(), sharedData.getDecisionScoreEvaluators());
    }

    private <T extends NamedObject> void addToMutableTreeNode(DefaultMutableTreeNode root, String nodeName, List<T> items) {
        var profilesNode = new DefaultMutableTreeNode(nodeName);
        root.add(profilesNode);
        for (var profile : items) {
            profilesNode.add(new DefaultMutableTreeNode(profile.getName()));
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
        uAiEditorPanel.setLayout(new GridBagLayout());
        final JSplitPane splitPane1 = new JSplitPane();
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(3, 3, 3, 3);
        uAiEditorPanel.add(splitPane1, gbc);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane1.setLeftComponent(panel1);
        newDecisionButton = new JButton();
        newDecisionButton.setText("New Decision");
        panel1.add(newDecisionButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(233, 34), null, 0, false));
        newConsiderationButton = new JButton();
        newConsiderationButton.setText("New Consideration");
        panel1.add(newConsiderationButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, new Dimension(233, 34), null, 0, false));
        panel1.add(repositoryViewer, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(233, 50), null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane1.setRightComponent(panel2);
        dseEditorPane = new JTabbedPane();
        panel2.add(dseEditorPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        profilePane = new JPanel();
        profilePane.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        dseEditorPane.addTab("Profile", profilePane);
        final JScrollPane scrollPane1 = new JScrollPane();
        profilePane.add(scrollPane1, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        scrollPane1.setViewportView(decisionScoreEvaluatorTable);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        profilePane.add(panel3, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        profileNameTextField = new JTextField();
        panel3.add(profileNameTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        descriptionTextField = new JTextField();
        panel3.add(descriptionTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        notesTextField = new JTextField();
        panel3.add(notesTextField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Profile Name");
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Description");
        panel3.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Notes");
        panel3.add(label3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        decisionPane = new JPanel();
        decisionPane.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        dseEditorPane.addTab("Decision", decisionPane);
        evaluatorScrollPane = new JScrollPane();
        decisionPane.add(evaluatorScrollPane, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 5, new Insets(0, 0, 0, 0), -1, -1));
        decisionPane.add(panel4, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Action");
        panel4.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        actionComboBox.setEditable(false);
        panel4.add(actionComboBox, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(300, -1), null, null, 0, false));
        panel4.add(weightSpinner, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(100, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Weight");
        panel4.add(label5, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel4.add(spacer1, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        considerationsPane = new JPanel();
        considerationsPane.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        considerationsPane.setName("");
        dseEditorPane.addTab("Considerations", considerationsPane);
        dseConfigPane = new JPanel();
        dseConfigPane.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        considerationsPane.add(dseConfigPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Name");
        dseConfigPane.add(label6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Description");
        dseConfigPane.add(label7, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        nameDseTextField = new JTextField();
        dseConfigPane.add(nameDseTextField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        descriptionDseTextField = new JTextField();
        dseConfigPane.add(descriptionDseTextField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        considerationsScrollPane = new JScrollPane();
        considerationsPane.add(considerationsScrollPane, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return uAiEditorPanel;
    }

}
