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

import megamek.client.generator.RandomUnitGenerator;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.common.loaders.MekSummary;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

class RandomArmyRatTab extends JPanel implements RandomArmyTab, TreeSelectionListener {

    private static final GUIPreferences GUIP = GUIPreferences.getInstance();

    private final JTextField unitCount = new JTextField(3);
    private final JTree ratTree = new JTree();
    private final JLabel ratStatusLabel = new JLabel(Messages.getString("RandomArmyDialog.ratStatusLoading"));

    protected final RandomUnitGenerator rug;

    public RandomArmyRatTab() {
        rug = RandomUnitGenerator.getInstance();
        rug.registerListener(e -> {
            ratStatusLabel.setText(Messages.getString("RandomArmyDialog.ratStatusDoneLoading"));
            updateRATs();
        });
        if (rug.isInitialized()) {
            ratStatusLabel.setText(Messages.getString("RandomArmyDialog.ratStatusDoneLoading"));
        }

        // construct the RAT panel
        unitCount.setText("4");

        ratTree.setRootVisible(false);
        ratTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        ratTree.addTreeSelectionListener(this);

        setLayout(new BorderLayout(5, 5));
        JPanel topLine = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topLine.add(new JLabel(Messages.getString("RandomArmyDialog.Unit")));
        topLine.add(unitCount);
        topLine.add(Box.createHorizontalStrut(10));
        topLine.add(ratStatusLabel);

        add(topLine, BorderLayout.NORTH);
        add(new JScrollPane(ratTree), BorderLayout.CENTER);
    }

    @Override
    public List<MekSummary> generateMekSummaries() {
        try {
            int units = Integer.parseInt(unitCount.getText());
            return RandomUnitGenerator.getInstance().generate(units);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Couldn't parse unit count.", "Error", JOptionPane.ERROR_MESSAGE);
            return Collections.emptyList();
        }
    }

    @Override
    public void setVisible(boolean aFlag) {
        if (aFlag) {
            updateRATs();
        }
        super.setVisible(aFlag);
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) ratTree.getLastSelectedPathComponent();
        if (node != null && node.isLeaf()) {
            String ratName = (String) node.getUserObject();
            rug.setChosenRAT(ratName);
        }
    }

    private void updateRATs() {
        Iterator<String> rats = rug.getRatList();
        if (null == rats) {
            return;
        }

        RandomUnitGenerator.RatTreeNode ratTreeNode = rug.getRatTree();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(ratTreeNode.name);
        createRatTreeNodes(root, ratTreeNode);
        ratTree.setModel(new DefaultTreeModel(root));

        String selectedRATPath = GUIP.getRATSelectedRAT();
        if (!selectedRATPath.isBlank()) {
            String[] nodes = selectedRATPath.replace('[', ' ').replace(']', ' ').split(",");
            TreePath path = findPathByName(nodes);
            ratTree.setSelectionPath(path);
        }
    }

    private void createRatTreeNodes(DefaultMutableTreeNode parentNode, RandomUnitGenerator.RatTreeNode ratTreeNode) {
        for (RandomUnitGenerator.RatTreeNode child : ratTreeNode.children) {
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(child.name);
            if (!child.children.isEmpty()) {
                createRatTreeNodes(newNode, child);
            }
            parentNode.add(newNode);
        }
    }

    private TreePath findPathByName(String... nodeNames) {
        TreeNode root = (TreeNode) ratTree.getModel().getRoot();
        return findNextNode(new TreePath(root), nodeNames, 0);
    }

    private TreePath findNextNode(TreePath parent, String[] nodes, int depth) {
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        String currNode = node.toString();

        // If equal, go down the branch
        if (currNode.equals(nodes[depth].trim())) {
            // If at end, return match
            if (depth == nodes.length - 1) {
                return parent;
            }

            // Traverse children
            if (node.getChildCount() >= 0) {
                for (Enumeration<?> e = node.children(); e.hasMoreElements(); ) {
                    TreeNode n = (TreeNode) e.nextElement();
                    TreePath path = parent.pathByAddingChild(n);
                    TreePath result = findNextNode(path, nodes, depth + 1);
                    // Found a match
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        // No match at this branch
        return null;
    }
}
