package megamek.client.ui.swing.dialog.imageChooser;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import megamek.client.ui.swing.tileset.MMStaticDirectoryManager;
import megamek.common.Crew;

public class PortraitChooserTree extends JTree {
    
    private static final long serialVersionUID = 1274949831997174959L;
    
    public PortraitChooserTree() {
        super(); 

        // set up the directory tree (left panel) 
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(Crew.ROOT_PORTRAIT);
        if (MMStaticDirectoryManager.getPortraits() != null) {
            Iterator<String> catNames = MMStaticDirectoryManager.getPortraits().getCategoryNames();
            while (catNames.hasNext()) {
                String catName = catNames.next();
                if ((catName != null) && !catName.equals("")) {
                    String[] names = catName.split("/");
                    addCategoryToTree(root, names);
                }
            }
        }
        setModel(new DefaultTreeModel(root));
    }
    
    /**
     * This recursive method is a hack: DirectoryItems flattens the directory
     * structure, but it provides useful functionality, so this method will
     * reconstruct the directory structure for the JTree.
     *
     * @param node
     * @param names
     */
    private void addCategoryToTree(DefaultMutableTreeNode node, String[] names) {

        // Shouldn't happen
        if (names.length == 0) {
            return;
        }

        boolean matched = false;
        for (Enumeration<?> e = node.children(); e.hasMoreElements();) {
            DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) e.nextElement();
            String nodeName = (String) childNode.getUserObject();
            if (nodeName.equals(names[0])) {
                if (names.length > 1) {
                    addCategoryToTree(childNode,
                            Arrays.copyOfRange(names, 1, names.length));
                    matched = true;
                } else {
                    // I guess we're done? This shouldn't happen, as there
                    // shouldn't be duplicates
                }
            }
        }

        // If we didn't match, lets create nodes for each name
        if (!matched) {
            DefaultMutableTreeNode root = node;
            for (int i = 0; i < names.length; i++) {
                DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(
                        names[i]);
                root.add(newNode);
                root = newNode;
            }
        }
    }
}
