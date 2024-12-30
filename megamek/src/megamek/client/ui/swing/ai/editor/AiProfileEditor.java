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
import megamek.ai.utility.Decision;
import megamek.ai.utility.NamedObject;
import megamek.client.bot.duchess.ai.utility.tw.TWUtilityAIRepository;
import megamek.client.bot.duchess.ai.utility.tw.considerations.TWConsideration;
import megamek.client.bot.duchess.ai.utility.tw.decision.TWDecision;
import megamek.client.bot.duchess.ai.utility.tw.decision.TWDecisionScoreEvaluator;
import megamek.client.bot.duchess.ai.utility.tw.profile.TWProfile;
import megamek.client.ui.Messages;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.swing.CommonMenuBar;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.common.Entity;
import megamek.logging.MMLogger;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static megamek.client.ui.swing.ClientGUI.*;

public class AiProfileEditor extends JFrame implements ActionListener {
    private static final MMLogger logger = MMLogger.create(AiProfileEditor.class);

    private final TWUtilityAIRepository sharedData = TWUtilityAIRepository.getInstance();
    private final GUIPreferences guip = GUIPreferences.getInstance();
    private final MegaMekController controller;

    private JTree repositoryViewer;
    private JTabbedPane mainEditorTabbedPane;
    private JPanel dseTabPane;
    private JTextField descriptionTextField;
    private JTextField profileNameTextField;
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
    private JButton saveProfileButton;
    private JButton saveDseButton;
    private JButton saveConsiderationButton;
    private JButton saveDecisionButton;

    private ConsiderationPane considerationPane;

    private final CommonMenuBar menuBar = CommonMenuBar.getMenuBarForAiEditor();
    private int profileId = -1;

    private boolean hasDecisionChanges = false;
    private boolean hasProfileChanges = false;
    private boolean hasDseChanges = false;
    private boolean hasConsiderationChanges = false;
    private boolean hasChangesToSave = false;
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

    private boolean hasChanges() {
        return hasDecisionChanges || hasProfileChanges || hasDseChanges || hasConsiderationChanges || hasChangesToSave;
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
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // When the board has changes, ask the user
                if (!hasChanges() || (showSavePrompt() != DialogResult.CANCELLED)) {
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
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    TreePath path = repositoryViewer.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        if (node.isLeaf()) {
                            handleOpenNodeAction(node);
                        }
                    }
                } else if (e.getButton() != MouseEvent.BUTTON1) {
                    TreePath path = repositoryViewer.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        repositoryViewer.setSelectionPath(path);
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        if (node.isLeaf()) {
                            JPopupMenu contextMenu = createContextMenu(node);
                            contextMenu.show(repositoryViewer, e.getX(), e.getY());
                        }
                    }
                }
            }
        });

//        repositoryViewer.addTreeSelectionListener(e -> {
//            TreePath path = e.getPath();
//            if (path != null) {
//                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
//                if (node.isLeaf()) {
//                    handleOpenNodeAction(node);
//                }
//            }
//        });
        getFrame().setJMenuBar(menuBar);
        menuBar.addActionListener(this);
        saveProfileButton.addActionListener(e -> {
            try {
                persistProfile();
            } catch (IllegalArgumentException ex) {
                logger.formattedErrorDialog("Error saving profile",
                    "One or more fields are empty or invalid in the Profile tab. Please correct the errors and try again.");
            }
        });
        saveDseButton.addActionListener(e -> {
            try {
                persistDecisionScoreEvaluator();
            } catch (IllegalArgumentException ex) {
                logger.formattedErrorDialog("Error saving decision score evaluator",
                    "One or more fields are empty or invalid in the Decision Score Evaluator tab. Please correct the errors and try again.");
            }
        });
        saveConsiderationButton.addActionListener(e -> {
            try {
                persistConsideration();
            } catch (IllegalArgumentException ex) {
                logger.formattedErrorDialog("Error saving consideration",
                    "One or more fields are empty or invalid in the Consideration tab. Please correct the errors and try again.");
            }
        });
        saveDecisionButton.addActionListener(e -> {
            try {
                persistDecision();
            } catch (IllegalArgumentException ex) {
                logger.formattedErrorDialog("Error saving decision",
                    "One or more fields are empty or invalid in the Decision tab. Please correct the errors and try again.");
            }
        });
        saveProfileButton.setVisible(true);
        saveDseButton.setVisible(false);
        saveConsiderationButton.setVisible(false);
        saveDecisionButton.setVisible(false);
        mainEditorTabbedPane.addChangeListener(e -> {
            if (mainEditorTabbedPane.getSelectedComponent() == profileTabPane) {
                saveProfileButton.setVisible(true);
                saveDseButton.setVisible(false);
                saveConsiderationButton.setVisible(false);
                saveDecisionButton.setVisible(false);
            } else if (mainEditorTabbedPane.getSelectedComponent() == dseTabPane) {
                saveProfileButton.setVisible(false);
                saveDseButton.setVisible(true);
                saveConsiderationButton.setVisible(false);
                saveDecisionButton.setVisible(false);
            } else if (mainEditorTabbedPane.getSelectedComponent() == considerationTabPane) {
                saveProfileButton.setVisible(false);
                saveDseButton.setVisible(false);
                saveConsiderationButton.setVisible(true);
                saveDecisionButton.setVisible(false);
            } else if (mainEditorTabbedPane.getSelectedComponent() == decisionTabPane) {
                saveProfileButton.setVisible(false);
                saveDseButton.setVisible(false);
                saveConsiderationButton.setVisible(false);
                saveDecisionButton.setVisible(true);
            }
        });

    }

    private JPopupMenu createContextMenu(DefaultMutableTreeNode node) {
        // Create a popup menu
        JPopupMenu contextMenu = new JPopupMenu();
        var obj = node.getUserObject();
        // Example menu item #1
        JMenuItem menuItemAction = new JMenuItem("Open");
        menuItemAction.addActionListener(evt -> {
            handleOpenNodeAction(node);
        });
        contextMenu.add(menuItemAction);

        if (obj instanceof TWDecision twDecision) {
            var action = new JMenuItem("Add to current Profile");
            action.addActionListener(evt -> {
                var model = profileDecisionTable.getModel();
                //noinspection unchecked
                ((DecisionTableModel<TWDecision>) model).addRow(twDecision);
            });
            contextMenu.add(action);
        } else if (obj instanceof TWProfile) {
            var action = getCopyProfileMenuItem((TWProfile) obj);
            contextMenu.add(action);
        } else if (obj instanceof TWDecisionScoreEvaluator twDse) {
            var action = new JMenuItem("New Decision Score Evaluator");
            action.addActionListener(evt -> {
                createNewDecisionScoreEvaluator();
            });
            contextMenu.add(action);
        } else if (obj instanceof TWConsideration twConsideration) {
            var action = new JMenuItem("New Consideration");
            action.addActionListener(evt -> {
                addNewConsideration();
            });
            contextMenu.add(action);
            // if the tab is a DSE, add the consideration to the DSE
            if (mainEditorTabbedPane.getSelectedComponent() == dseTabPane) {
                var action1 = new JMenuItem("Add to current Decision Score Evaluator");
                action1.addActionListener(evt -> {
                    var dse = ((DecisionScoreEvaluatorPane) dsePane).getDecisionScoreEvaluator();
                    dse.addConsideration(twConsideration);
                    ((DecisionScoreEvaluatorPane) dsePane).setDecisionScoreEvaluator(dse);
                });
                contextMenu.add(action1);
            } else if (mainEditorTabbedPane.getSelectedComponent() == decisionTabPane) {
                var action1 = new JMenuItem("Add to current Decision");
                action1.addActionListener(evt -> {
                    var dse = ((DecisionScoreEvaluatorPane)decisionTabDsePanel).getDecisionScoreEvaluator();
                    dse.addConsideration(twConsideration);
                    ((DecisionScoreEvaluatorPane) decisionTabDsePanel).setDecisionScoreEvaluator(dse);
                });
                contextMenu.add(action1);
            }
        }

        // Example menu item #2
        JMenuItem menuItemOther = new JMenuItem("Delete");
        menuItemOther.addActionListener(evt -> {
            // Another action
            int deletePrompt = JOptionPane.showConfirmDialog(null,
                Messages.getString("aiEditor.deleteNodePrompt"),
                Messages.getString("BoardEditor.deleteNodeTitle"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            if (deletePrompt == JOptionPane.YES_OPTION) {
                handleDeleteNodeAction(node);
            }
        });
        contextMenu.add(menuItemOther);

        return contextMenu;
    }

    private JMenuItem getCopyProfileMenuItem(TWProfile obj) {
        var action = new JMenuItem("Copy Profile");
        action.addActionListener(evt -> {
            profileId = -1;
            profileNameTextField.setText(obj.getName() + " (Copy)");
            descriptionTextField.setText(obj.getDescription());
            profileDecisionTable.setModel(new DecisionTableModel<>(obj.getDecisions()));
            mainEditorTabbedPane.setSelectedComponent(profileTabPane);
            hasProfileChanges = true;
        });
        return action;
    }


    private void handleOpenNodeAction(DefaultMutableTreeNode node) {
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


    private void handleDeleteNodeAction(DefaultMutableTreeNode node) {
        var obj = node.getUserObject();
        if (obj instanceof TWDecision twDecision) {
            sharedData.removeDecision(twDecision);
            hasDecisionChanges = true;
        } else if (obj instanceof TWProfile twProfile) {
            sharedData.removeProfile(twProfile);
            hasProfileChanges = true;
        } else if (obj instanceof TWDecisionScoreEvaluator twDse) {
            sharedData.removeDecisionScoreEvaluator(twDse);
            hasDseChanges = true;
        } else if (obj instanceof TWConsideration twConsideration) {
            sharedData.removeConsideration(twConsideration);
            hasConsiderationChanges = true;
        }
        ((DefaultTreeModel) repositoryViewer.getModel()).removeNodeFromParent(node);
    }

    private void openConsideration(TWConsideration twConsideration) {
        considerationPane.setConsideration(twConsideration);
        mainEditorTabbedPane.setSelectedComponent(considerationTabPane);
        hasConsiderationChanges = true;
    }

    private void openDecision(TWDecision twDecision) {
        ((DecisionScoreEvaluatorPane) decisionTabDsePanel).setDecisionScoreEvaluator(twDecision.getDecisionScoreEvaluator());
        mainEditorTabbedPane.setSelectedComponent(decisionTabPane);
        hasDecisionChanges = true;
    }

    private void openProfile(TWProfile twProfile) {
        profileId = twProfile.getId();
        profileNameTextField.setText(twProfile.getName());
        descriptionTextField.setText(twProfile.getDescription());
        profileDecisionTable.setModel(new DecisionTableModel<>(twProfile.getDecisions()));
        mainEditorTabbedPane.setSelectedComponent(profileTabPane);
        hasProfileChanges = true;
    }

    private void openDecisionScoreEvaluator(TWDecisionScoreEvaluator twDse) {
        ((DecisionScoreEvaluatorPane) dsePane).setDecisionScoreEvaluator(twDse);
        mainEditorTabbedPane.setSelectedComponent(dseTabPane);
        hasDseChanges = true;
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
        if (((savePrompt == JOptionPane.YES_OPTION) && !hasChanges())
            || (savePrompt == JOptionPane.CANCEL_OPTION)
            || (savePrompt == JOptionPane.CLOSED_OPTION)) {
            return DialogResult.CANCELLED;
        } else {
            if (saveEverything()) {
                return DialogResult.CONFIRMED;
            } else {
                return DialogResult.CANCELLED;
            }
        }
    }

    private boolean saveEverything() {
        try {
            if (hasProfileChanges) {
                persistProfile();
            }
            if (hasDecisionChanges) {
                persistDecision();
            }
            if (hasDseChanges) {
                persistDecisionScoreEvaluator();
            }
            if (hasConsiderationChanges) {
                persistConsideration();
            }
            if (hasChangesToSave) {
                sharedData.persistDataToUserData();
                hasChangesToSave = false;
            }
            return true;
        } catch (IllegalArgumentException ex) {
            logger.formattedErrorDialog("Error saving data",
                "One or more fields are empty or invalid. Please correct the errors and try again.");
        }
        return false;
    }

    private void persistConsideration() {
        var consideration = considerationPane.getConsideration();
        sharedData.addConsideration(consideration);
        hasConsiderationChanges = false;
        hasChangesToSave = true;
        loadDataRepoViewer();
    }

    private void persistDecision() {
        var dse = ((DecisionScoreEvaluatorPane) decisionTabDsePanel).getDecisionScoreEvaluator();
        var decision = new TWDecision((Action) actionComboBox.getSelectedItem(), (double) weightSpinner.getValue(), dse);
        sharedData.addDecision(decision);
        hasDecisionChanges = false;
        hasChangesToSave = true;
        loadDataRepoViewer();
    }

    private void persistDecisionScoreEvaluator() {
        var dse = ((DecisionScoreEvaluatorPane) dsePane).getDecisionScoreEvaluator();
        sharedData.addDecisionScoreEvaluator(dse);
        hasDseChanges = false;
        hasChangesToSave = true;
        loadDataRepoViewer();
    }

    private void persistProfile() {
        //noinspection unchecked
        var model = (DecisionTableModel<TWDecision>) profileDecisionTable.getModel();
        if (profileId <= 0) {
            var ids = sharedData.getProfiles().stream().map(TWProfile::getId).collect(Collectors.toSet());
            while (ids.contains(profileId) || profileId <= 0) {
                profileId = new Random().nextInt(1, Integer.MAX_VALUE);
            }
        }
        var decisions = model.getDecisions().stream().map(e -> (Decision<Entity, Entity>) e).toList();
        sharedData.addProfile(new TWProfile(profileId, profileNameTextField.getText(), descriptionTextField.getText(), decisions));
        hasProfileChanges = false;
        hasChangesToSave = true;
        loadDataRepoViewer();
    }

    public JFrame getFrame() {
        return this;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        switch (e.getActionCommand()) {
            case AI_EDITOR_NEW:
                createNewProfile();
                break;
            case AI_EDITOR_OPEN:
                break;
            case AI_EDITOR_RECENT_PROFILE:
                break;
            case AI_EDITOR_SAVE:
                saveEverything();
                break;
            case AI_EDITOR_SAVE_AS:
                // not implemented
                break;
            case AI_EDITOR_RELOAD_FROM_DISK:

                break;
            case AI_EDITOR_UNDO:
                break;
            case AI_EDITOR_REDO:
                break;
            case AI_EDITOR_NEW_DECISION:
                createNewDecision();
                break;
            case AI_EDITOR_NEW_CONSIDERATION:
                addNewConsideration();
                break;
            case AI_EDITOR_NEW_DECISION_SCORE_EVALUATOR:
                createNewDecisionScoreEvaluator();
                break;
            case AI_EDITOR_EXPORT:
                break;
            case AI_EDITOR_IMPORT:
                break;
        }
    }

    private void createNewProfile() {
        profileId = -1;
        profileNameTextField.setText("New Profile");
        descriptionTextField.setText("");
        initializeProfileUI();
        mainEditorTabbedPane.setSelectedComponent(profileTabPane);
        profileTabPane.updateUI();
    }

    private void createNewDecisionScoreEvaluator() {
        ((DecisionScoreEvaluatorPane) dsePane).reset();
        mainEditorTabbedPane.setSelectedComponent(dseTabPane);
        dsePane.updateUI();
    }

    private void addNewConsideration() {
        ((DecisionScoreEvaluatorPane) dsePane).addEmptyConsideration();
        dsePane.updateUI();
    }

    private void createNewDecision() {
        var action = (Action) actionComboBox.getSelectedItem();
        var weight = (double) weightSpinner.getValue();
        var dse = new TWDecision(action, weight);
        var model = profileDecisionTable.getModel();
        //noinspection unchecked
        ((DecisionTableModel<TWDecision>) model).addRow(dse);
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

        loadDataRepoViewer();
        actionComboBox = new JComboBox<>(Action.values());
        initializeProfileUI();
        decisionTabDsePanel = new DecisionScoreEvaluatorPane();
        dsePane = new DecisionScoreEvaluatorPane();
    }

    private void initializeProfileUI() {
        var model = new DecisionTableModel<>(sharedData.getDecisions());
        profileDecisionTable = new DecisionScoreEvaluatorTable<>(model, Action.values(), sharedData.getDecisionScoreEvaluators());
    }

    private void loadDataRepoViewer() {
        var root = new DefaultMutableTreeNode(Messages.getString("aiEditor.tree.title"));
        addToMutableTreeNode(root, TreeViewHelper.PROFILES.getName(), sharedData.getProfiles());
        addToMutableTreeNode(root, TreeViewHelper.DECISIONS.getName(), sharedData.getDecisions());
        addToMutableTreeNode(root, TreeViewHelper.DSE.getName(), sharedData.getDecisionScoreEvaluators());
        addToMutableTreeNode(root, TreeViewHelper.CONSIDERATIONS.getName(), sharedData.getConsiderations());
        DefaultTreeModel treeModel = new DefaultTreeModel(root);

        if (repositoryViewer == null) {
            repositoryViewer = new JTree(treeModel);
        } else {
            repositoryViewer.setModel(treeModel);
            repositoryViewer.updateUI();
        }
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
        panel1.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane1.setLeftComponent(panel1);
        panel1.add(repositoryViewer, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(233, 50), null, 0, false));
        saveProfileButton = new JButton();
        saveProfileButton.setText("Save");
        panel1.add(saveProfileButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saveDseButton = new JButton();
        saveDseButton.setText("Save");
        panel1.add(saveDseButton, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saveConsiderationButton = new JButton();
        saveConsiderationButton.setText("Save");
        panel1.add(saveConsiderationButton, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saveDecisionButton = new JButton();
        saveDecisionButton.setText("Save");
        panel1.add(saveDecisionButton, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        splitPane1.setRightComponent(panel2);
        mainEditorTabbedPane = new JTabbedPane();
        panel2.add(mainEditorTabbedPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        profileTabPane = new JPanel();
        profileTabPane.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainEditorTabbedPane.addTab(this.$$$getMessageFromBundle$$$("megamek/common/options/messages", "aiEditor.profile"), profileTabPane);
        profileScrollPane = new JScrollPane();
        profileScrollPane.setDoubleBuffered(false);
        profileScrollPane.setWheelScrollingEnabled(true);
        profileTabPane.add(profileScrollPane, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        profileDecisionTable.setColumnSelectionAllowed(false);
        profileDecisionTable.setDragEnabled(true);
        profileDecisionTable.setFillsViewportHeight(true);
        profileDecisionTable.setInheritsPopupMenu(true);
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
    public JComponent $$$getRootComponent$$$() {
        return uAiEditorPanel;
    }

}
