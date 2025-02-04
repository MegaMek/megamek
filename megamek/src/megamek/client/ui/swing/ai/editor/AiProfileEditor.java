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

import megamek.ai.utility.Decision;
import megamek.ai.utility.NamedObject;
import megamek.client.bot.caspar.ai.utility.tw.TWUtilityAIRepository;
import megamek.client.bot.caspar.ai.utility.tw.considerations.TWConsideration;
import megamek.client.bot.caspar.ai.utility.tw.decision.TWDecision;
import megamek.client.bot.caspar.ai.utility.tw.decision.TWDecisionScoreEvaluator;
import megamek.client.bot.caspar.ai.utility.tw.profile.TWProfile;
import megamek.client.ui.Messages;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.swing.CommonMenuBar;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.common.Entity;
import megamek.logging.MMLogger;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.awt.Taskbar.Feature;

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
    private JPanel uAiEditorPanel;
    private JScrollPane profileScrollPane;
    private JPanel dsePane;
    private JPanel considerationTabPane;
    private JPanel considerationEditorPanel;
    private JButton saveProfileButton;
    private JButton saveDseButton;
    private JButton saveConsiderationButton;
    private JToolBar profileTools;
    private ConsiderationPane considerationPane;

    private final AtomicReference<TWProfile> currentProfile = new AtomicReference<>();
    private final AtomicReference<TWDecision> currentDecision = new AtomicReference<>();
    private final AtomicReference<TWDecisionScoreEvaluator> currentDecisionScoreEvaluator = new AtomicReference<>();
    private final AtomicReference<TWConsideration> currentConsideration = new AtomicReference<>();

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
        showNotification(this, false);
    }

    private static void showNotification(JFrame frame, boolean dismiss) {
        if (Taskbar.isTaskbarSupported()) {
            Taskbar taskbar = Taskbar.getTaskbar();

            if (taskbar.isSupported(Feature.ICON_BADGE_TEXT)) {
                // Set badge text (macOS)
                if (dismiss) {
                    taskbar.setIconBadge("");
                } else {
                    taskbar.setIconBadge("Turn!");
                }
            }

            if (taskbar.isSupported(Feature.USER_ATTENTION)) {
                // Request user attention (macOS bounce or Windows flash)
                if (dismiss) {
                    taskbar.requestUserAttention(false, true);
                } else {
                    taskbar.requestUserAttention(true, true);
                }
            }
        } else {
            // Fallback for unsupported platforms
            if (dismiss) {
                frame.setTitle("AI Profile Editor");
            } else {
                frame.setTitle("New Message! - Notification Example");
            }
        }
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

        // Profile toolbar

        var copyDecisionBtn = new JButton(Messages.getString("aiEditor.copy.decision"));
        copyDecisionBtn.addActionListener(e -> {
            var model = profileDecisionTable.getModel();
            var selectedRow = profileDecisionTable.getSelectedRow();
            if (selectedRow >= 0) {
                // noinspection unchecked
                var decision = ((DecisionTableModel<TWDecision>) model).getDecisions().get(selectedRow);
                if (decision != null) {
                    var newDecision = decision.copy();
                    // noinspection unchecked
                    ((DecisionTableModel<TWDecision>) model).addRow(newDecision);
                } else {
                    logger.error(Messages.getString("aiEditor.copy.decision.error"),
                        Messages.getString("aiEditor.copy.decision.error.title"));
                }
            }
        });
        var editDecisionBtn = new JButton(Messages.getString("aiEditor.edit.decision"));
        editDecisionBtn.addActionListener(e -> {
            var model = profileDecisionTable.getModel();
            var selectedRow = profileDecisionTable.getSelectedRow();
            if (selectedRow >= 0) {
                // noinspection unchecked
                var decision = ((DecisionTableModel<TWDecision>) model).getDecisions().get(selectedRow);
                if (decision != null) {
                    editDecisionScoreEvaluator(decision);
                } else {
                    logger.error(Messages.getString("aiEditor.edit.decisionScoreEvaluator.error"),
                        Messages.getString("aiEditor.edit.decisionScoreEvaluator.error.title"));
                }
            }

        });
        var deleteDecisionBtn = new JButton(Messages.getString("aiEditor.delete.decision"));
        deleteDecisionBtn.addActionListener(e -> {
            var model = profileDecisionTable.getModel();
            var selectedRow = profileDecisionTable.getSelectedRow();
            if (selectedRow >= 0) {
                var decision = ((DecisionTableModel<TWDecision>) model).getDecisions().get(selectedRow);
                if (decision.equals(currentDecision.get())) {
                    currentDecision.set(null);
                }
                // noinspection unchecked
                ((DecisionTableModel<TWDecision>) model).deleteRow(selectedRow);
            }
        });

        profileTools.add(copyDecisionBtn);
        profileTools.add(editDecisionBtn);
        profileTools.add(deleteDecisionBtn);

        // Setup window frame behaviors
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
                        JPopupMenu contextMenu = createContextMenu(node);
                        contextMenu.show(repositoryViewer, e.getX(), e.getY());
                    }
                }
            }
        });

        repositoryViewer.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    TreePath path = repositoryViewer.getSelectionPath();
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        if (node.isLeaf()) {
                            handleOpenNodeAction(node);
                        }
                    }
                }
            }
        });

        getFrame().setJMenuBar(menuBar);
        menuBar.addActionListener(this);
        saveProfileButton.addActionListener(e -> {
            try {
                persistProfile();
            } catch (IllegalArgumentException ex) {
                logger.errorDialog("Error saving profile",
                    "One or more fields are empty or invalid in the Profile tab. Please correct the errors and try again.");
            }
        });
        saveDseButton.addActionListener(e -> {
            try {
                persistDecisionScoreEvaluator();
            } catch (IllegalArgumentException ex) {
                logger.errorDialog("Error saving decision score evaluator",
                    "One or more fields are empty or invalid in the Decision Score Evaluator tab. Please correct the errors and try again.");
            }
        });
        saveConsiderationButton.addActionListener(e -> {
            try {
                persistConsideration();
            } catch (IllegalArgumentException ex) {
                logger.errorDialog("Error saving consideration",
                    "One or more fields are empty or invalid in the Consideration tab. Please correct the errors and try again.");
            }
        });

        saveProfileButton.setVisible(true);
        saveDseButton.setVisible(false);
        saveConsiderationButton.setVisible(false);
        mainEditorTabbedPane.addChangeListener(e -> {
            if (mainEditorTabbedPane.getSelectedComponent() == profileTabPane) {
                saveProfileButton.setVisible(true);
                saveDseButton.setVisible(false);
                saveConsiderationButton.setVisible(false);
            } else if (mainEditorTabbedPane.getSelectedComponent() == dseTabPane) {
                saveProfileButton.setVisible(false);
                saveDseButton.setVisible(true);
                saveConsiderationButton.setVisible(false);
            } else if (mainEditorTabbedPane.getSelectedComponent() == considerationTabPane) {
                saveProfileButton.setVisible(false);
                saveDseButton.setVisible(false);
                saveConsiderationButton.setVisible(true);
            }
        });

    }

    private JPopupMenu createContextMenu(DefaultMutableTreeNode node) {
        // Create a popup menu
        showNotification(this, true);
        JPopupMenu contextMenu = new JPopupMenu();
        var obj = node.getUserObject();
        if (obj instanceof String) {
            if (obj.equals(Messages.getString("aiEditor.Profiles"))) {
                JMenuItem menuItemAction = new JMenuItem(Messages.getString("aiEditor.new.profile"));
                menuItemAction.addActionListener(evt -> {
                    createNewProfile();
                });
                contextMenu.add(menuItemAction);
            } else if (obj.equals(Messages.getString("aiEditor.DecisionScoreEvaluators"))) {
                JMenuItem menuItemAction = new JMenuItem(Messages.getString("aiEditor.new.dse"));
                menuItemAction.addActionListener(evt -> {
                    createNewDecisionScoreEvaluator();
                });
                contextMenu.add(menuItemAction);
            } else if (obj.equals(Messages.getString("aiEditor.Considerations"))) {
                JMenuItem menuItemAction = new JMenuItem(Messages.getString("aiEditor.new.consideration"));
                menuItemAction.addActionListener(evt -> {
                    addNewConsideration();
                });
                contextMenu.add(menuItemAction);
            }
        } else {
            JMenuItem menuItemAction;
            menuItemAction = new JMenuItem("Edit " + ((obj instanceof NamedObject) ? ((NamedObject) obj).getName() : "item"));
            menuItemAction.addActionListener(evt -> {
                handleOpenNodeAction(node);
            });
            contextMenu.add(menuItemAction);

            if (obj instanceof TWProfile) {
                var action = getCopyProfileMenuItem((TWProfile) obj);
                contextMenu.add(action);
            } else if (obj instanceof TWDecisionScoreEvaluator dse) {
                var action1 = new JMenuItem(Messages.getString("aiEditor.new.dse"));
                action1.addActionListener(evt -> {
                    createNewDecisionScoreEvaluator();
                });
                contextMenu.add(action1);
                var action2 = new JMenuItem(Messages.getString("aiEditor.add.to.profile"));
                action2.addActionListener(evt -> {
                    var model = profileDecisionTable.getModel();
                    //noinspection unchecked
                    ((DecisionTableModel<TWDecision>) model).addRow(createNewDecision(dse));
                });
                contextMenu.add(action2);
            } else if (obj instanceof TWConsideration twConsideration) {
                var action1 = new JMenuItem(Messages.getString("aiEditor.new.consideration"));
                action1.addActionListener(evt -> {
                    addNewConsideration();
                });
                contextMenu.add(action1);
                // if the tab is a DSE, you may add the consideration to the DSE
                if (mainEditorTabbedPane.getSelectedComponent() == dseTabPane) {
                    var action2 = new JMenuItem(Messages.getString("aiEditor.add.to.dse"));
                    action2.addActionListener(evt -> {
                        System.out.println(">>>>>>>>>>>>>>>>>>HERE!!!!!!!!!!!!!!");
                        addConsiderationToDse(twConsideration);
                    });
                    contextMenu.add(action2);
                }
            }

            JMenuItem menuItemOther = new JMenuItem(Messages.getString("aiEditor.contextualMenu.delete"));
            menuItemOther.addActionListener(evt -> {
                // Another action
                int deletePrompt = JOptionPane.showConfirmDialog(null,
                    Messages.getString("aiEditor.deleteNodePrompt"),
                    Messages.getString("aiEditor.deleteNode.title"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
                if (deletePrompt == JOptionPane.YES_OPTION) {
                    handleDeleteNodeAction(node);
                }
            });
            contextMenu.add(menuItemOther);
        }
        return contextMenu;
    }

    private void addConsiderationToDse(TWConsideration twConsideration) {
        System.out.println("HERE!!!!!!!!!!!!!!");
        if (currentDecision.get() == null) {
            var dse = ((DecisionScoreEvaluatorPane) dsePane).getDecisionScoreEvaluator();
            dse.addConsideration(twConsideration);
            ((DecisionScoreEvaluatorPane) dsePane).setDecisionScoreEvaluator(dse);
        } else {
            currentDecisionScoreEvaluator.get().addConsideration(twConsideration);
            ((DecisionScoreEvaluatorPane) dsePane).setDecisionScoreEvaluator(currentDecisionScoreEvaluator);
        }
        hasDseChanges = true;
        dsePane.updateUI();
    }

    private JMenuItem getCopyProfileMenuItem(TWProfile obj) {
        var action = new JMenuItem(Messages.getString("aiEditor.copy.profile"));
        action.addActionListener(evt -> {
            profileId = -1;
            profileNameTextField.setText(obj.getName() + " " + Messages.getString("aiEditor.item.copy"));
            descriptionTextField.setText(obj.getDescription());
            profileDecisionTable.setModel(new DecisionTableModel<>(obj.getDecisions()));
            mainEditorTabbedPane.setSelectedComponent(profileTabPane);
            hasProfileChanges = true;
        });
        return action;
    }


    private void handleOpenNodeAction(DefaultMutableTreeNode node) {
        var obj = node.getUserObject();
        if (obj instanceof TWProfile twProfile) {
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
            if (twDecision.equals(currentDecision.get())) {
                currentDecision.set(null);
            }
            sharedData.removeDecision(twDecision);
            hasDecisionChanges = true;
        } else if (obj instanceof TWProfile twProfile) {
            if (twProfile.equals(currentProfile.get())) {
                currentProfile.set(null);
            }
            sharedData.removeProfile(twProfile);
            hasProfileChanges = true;
        } else if (obj instanceof TWDecisionScoreEvaluator twDse) {
            if (twDse.equals(currentDecisionScoreEvaluator.get())) {
                currentDecisionScoreEvaluator.set(null);
            }
            sharedData.removeDecisionScoreEvaluator(twDse);
            hasDseChanges = true;
        } else if (obj instanceof TWConsideration twConsideration) {
            if (twConsideration.equals(currentConsideration.get())) {
                currentConsideration.set(null);
            }
            sharedData.removeConsideration(twConsideration);
            hasConsiderationChanges = true;
        }

        ((DefaultTreeModel) repositoryViewer.getModel()).removeNodeFromParent(node);
    }

    private void openConsideration(TWConsideration twConsideration) {
        currentConsideration.set(twConsideration);
        considerationPane.setConsideration(twConsideration);
        mainEditorTabbedPane.setSelectedComponent(considerationTabPane);
        hasConsiderationChanges = true;
    }

    private void openProfile(TWProfile twProfile) {
        currentProfile.set(twProfile);
        profileId = twProfile.getId();
        profileNameTextField.setText(twProfile.getName());
        descriptionTextField.setText(twProfile.getDescription());
        profileDecisionTable.setModel(new DecisionTableModel<>(twProfile.getDecisions()));
        mainEditorTabbedPane.setSelectedComponent(profileTabPane);
        hasProfileChanges = true;
    }

    private void openDecisionScoreEvaluator(TWDecisionScoreEvaluator twDse) {
        currentDecision.set(null);
        currentDecisionScoreEvaluator.set(twDse);
        ((DecisionScoreEvaluatorPane) dsePane).setDecisionScoreEvaluator(twDse);
        mainEditorTabbedPane.setSelectedComponent(dseTabPane);
        hasDseChanges = true;
    }

    private void editDecisionScoreEvaluator(TWDecision twDecision) {
        currentDecision.set(twDecision);
        currentDecisionScoreEvaluator.set((TWDecisionScoreEvaluator) twDecision.getDecisionScoreEvaluator());
        ((DecisionScoreEvaluatorPane) dsePane).setDecisionScoreEvaluator(currentDecisionScoreEvaluator);
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
            logger.errorDialog(Messages.getString("aiEditor.save.error.title"),
                Messages.getString("aiEditor.save.error.message") + ": " + ex.getMessage());
        }
        return false;
    }

    private void persistConsideration() {
        var consideration = considerationPane.getConsideration();
        sharedData.addConsideration(consideration);
        hasConsiderationChanges = false;
        hasChangesToSave = true;
        sharedData.persistDataToUserData();
        loadDataRepoViewer();
    }

    private void persistDecisionScoreEvaluator() {
        if (currentDecision.get() != null && currentDecisionScoreEvaluator.get() != null) {
            // This updates the current decision with the edits that were made on it
            var dse = ((DecisionScoreEvaluatorPane) dsePane).updateInPlaceTheDSE();
            currentDecision.get().setDecisionScoreEvaluator(dse);
            var profile = currentProfile.get();
            sharedData.addDecisionScoreEvaluator(dse);
            if (!profile.getDecisions().contains(currentDecision.get())) {
                profile.getDecisions().add(currentDecision.get());
            }
            sharedData.addProfile(profile);
        } else {
            var dse = ((DecisionScoreEvaluatorPane) dsePane).getDecisionScoreEvaluator();
            sharedData.addDecisionScoreEvaluator(dse);
        }
        hasDseChanges = false;
        hasChangesToSave = true;
        sharedData.persistDataToUserData();
        loadDataRepoViewer();
    }

    private void persistProfile() {
        //noinspection unchecked
        var model = (DecisionTableModel<TWDecision>) profileDecisionTable.getModel();
        var decisions = model.getDecisions().stream().map(e -> (Decision<Entity, Entity>) e).toList();

        if (profileId <= 0) {
            var ids = sharedData.getProfiles().stream().map(TWProfile::getId).collect(Collectors.toSet());
            while (ids.contains(profileId) || profileId <= 0) {
                profileId = new Random().nextInt(1, Integer.MAX_VALUE);
            }
        }

        sharedData.addProfile(new TWProfile(profileId, profileNameTextField.getText(), descriptionTextField.getText(), decisions));
        hasProfileChanges = false;
        hasChangesToSave = true;
        sharedData.persistDataToUserData();
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
                // not implemented???
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
                loadDataRepoViewer();
                break;
            case AI_EDITOR_UNDO:
                break;
            case AI_EDITOR_REDO:
                break;
            case AI_EDITOR_NEW_CONSIDERATION:
                addNewConsideration();
                break;
            case AI_EDITOR_NEW_DECISION_SCORE_EVALUATOR:
                createNewDecisionScoreEvaluator();
                break;
            case AI_EDITOR_EXPORT: {
                var fileChooser = new JFileChooser();
                fileChooser.setDialogTitle(Messages.getString("aiEditor.export.title"));
                fileChooser.setFileFilter(new FileNameExtensionFilter(Messages.getString("aiEditor.save.filenameExtension"), "uai"));
                fileChooser.setAcceptAllFileFilterUsed(false);
                int userSelection = fileChooser.showSaveDialog(this);
                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    File fileToSave = fileChooser.getSelectedFile();
                    if (!fileToSave.getName().toLowerCase().endsWith(".uai")) {
                        fileToSave = new File(fileToSave + ".uai");
                    }
                    try {
                        sharedData.exportAiData(fileToSave);
                    } catch (Exception ex) {
                        logger.errorDialog(Messages.getString("aiEditor.export.error.title"),
                            Messages.getString("aiEditor.export.error.message"));
                    }
                }
                break;
            }
            case AI_EDITOR_IMPORT: {
                var fileChooser = new JFileChooser();
                fileChooser.setDialogTitle(Messages.getString("aiEditor.import.title"));
                fileChooser.setFileFilter(new FileNameExtensionFilter(Messages.getString("aiEditor.save.filenameExtension"), "uai"));
                fileChooser.setAcceptAllFileFilterUsed(false);
                int userSelection = fileChooser.showOpenDialog(this);
                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    File fileToLoad = fileChooser.getSelectedFile();
                    try {
                        sharedData.importAiData(fileToLoad);
                    } catch (Exception ex) {
                        logger.errorDialog(Messages.getString("aiEditor.import.error.title"),
                            Messages.getString("aiEditor.import.error.message"));
                    }
                }
            }
        }
    }

    private void createNewProfile() {
        profileId = -1;
        profileNameTextField.setText(Messages.getString("aiEditor.new.profile"));
        descriptionTextField.setText(Messages.getString("aiEditor.new.profile.description", new Date()));
        profileDecisionTable.setModel(new DecisionTableModel<>());
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

    private TWDecision createNewDecision(TWDecision decision) {
        return decision.copy();
    }

    private TWDecision createNewDecision(TWDecisionScoreEvaluator dse) {
        return new TWDecision("New Decision", "Created at " + new Date(), 1.0, dse.copy());
    }

    private void createUIComponents() {
        loadDataRepoViewer();
        initializeProfileUI();
        dsePane = new DecisionScoreEvaluatorPane();
    }

    private void initializeProfileUI() {
        var model = new DecisionTableModel<>(sharedData.getDecisions());
        profileDecisionTable = new DecisionScoreEvaluatorTable<>(model);
    }

    private void loadDataRepoViewer() {
        sharedData.reloadRepository();
        var root = new DefaultMutableTreeNode(Messages.getString("aiEditor.tree.title"));
        addToMutableTreeNode(root, Messages.getString("aiEditor.Profiles"), sharedData.getProfiles());
        addToMutableTreeNode(root, Messages.getString("aiEditor.DecisionScoreEvaluators"), sharedData.getDecisionScoreEvaluators());
        addToMutableTreeNode(root, Messages.getString("aiEditor.Considerations"), sharedData.getConsiderations());
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
            childNode.setUserObject(item);

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
        uAiEditorPanel.setLayout(new GridBagLayout());
        final JSplitPane splitPane1 = new JSplitPane();
        GridBagConstraints gbc;
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        uAiEditorPanel.add(splitPane1, gbc);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridBagLayout());
        splitPane1.setLeftComponent(panel1);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel1.add(repositoryViewer, gbc);
        saveProfileButton = new JButton();
        saveProfileButton.setText("Save");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel1.add(saveProfileButton, gbc);
        saveDseButton = new JButton();
        saveDseButton.setText("Save");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel1.add(saveDseButton, gbc);
        saveConsiderationButton = new JButton();
        saveConsiderationButton.setText("Save");
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel1.add(saveConsiderationButton, gbc);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridBagLayout());
        splitPane1.setRightComponent(panel2);
        mainEditorTabbedPane = new JTabbedPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel2.add(mainEditorTabbedPane, gbc);
        profileTabPane = new JPanel();
        profileTabPane.setLayout(new GridBagLayout());
        mainEditorTabbedPane.addTab(this.$$$getMessageFromBundle$$$("megamek/common/options/messages", "aiEditor.profile"), profileTabPane);
        profileScrollPane = new JScrollPane();
        profileScrollPane.setDoubleBuffered(false);
        profileScrollPane.setWheelScrollingEnabled(true);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        profileTabPane.add(profileScrollPane, gbc);
        profileDecisionTable.setColumnSelectionAllowed(false);
        profileDecisionTable.setDragEnabled(true);
        profileDecisionTable.setFillsViewportHeight(true);
        profileDecisionTable.setInheritsPopupMenu(true);
        profileDecisionTable.setMinimumSize(new Dimension(150, 32));
        profileDecisionTable.setPreferredScrollableViewportSize(new Dimension(150, 32));
        profileScrollPane.setViewportView(profileDecisionTable);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        profileTabPane.add(panel3, gbc);
        profileNameTextField = new JTextField();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel3.add(profileNameTextField, gbc);
        descriptionTextField = new JTextField();
        gbc = new GridBagConstraints();
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel3.add(descriptionTextField, gbc);
        final JLabel label1 = new JLabel();
        this.$$$loadLabelText$$$(label1, this.$$$getMessageFromBundle$$$("megamek/common/options/messages", "aiEditor.profile.name"));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        panel3.add(label1, gbc);
        final JLabel label2 = new JLabel();
        this.$$$loadLabelText$$$(label2, this.$$$getMessageFromBundle$$$("megamek/common/options/messages", "AiEditor.description"));
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        panel3.add(label2, gbc);
        profileTools = new JToolBar();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        profileTabPane.add(profileTools, gbc);
        dseTabPane = new JPanel();
        dseTabPane.setLayout(new GridBagLayout());
        dseTabPane.setName("");
        mainEditorTabbedPane.addTab(this.$$$getMessageFromBundle$$$("megamek/common/options/messages", "aiEditor.tab.dse"), dseTabPane);
        final JScrollPane scrollPane1 = new JScrollPane();
        scrollPane1.setHorizontalScrollBarPolicy(31);
        scrollPane1.setWheelScrollingEnabled(true);
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        dseTabPane.add(scrollPane1, gbc);
        scrollPane1.setViewportView(dsePane);
        considerationTabPane = new JPanel();
        considerationTabPane.setLayout(new GridBagLayout());
        mainEditorTabbedPane.addTab(this.$$$getMessageFromBundle$$$("megamek/common/options/messages", "aiEditor.tab.consideration"), considerationTabPane);
        final JScrollPane scrollPane2 = new JScrollPane();
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        considerationTabPane.add(scrollPane2, gbc);
        considerationEditorPanel = new JPanel();
        considerationEditorPanel.setLayout(new GridBagLayout());
        scrollPane2.setViewportView(considerationEditorPanel);
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


    public JComponent $$$getRootComponent$$$() {
        return uAiEditorPanel;
    }

}
