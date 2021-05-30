/*
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.panels;

import megamek.client.ui.lists.ImageList;
import megamek.client.ui.renderers.AbstractIconRenderer;
import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import megamek.common.util.fileUtils.DirectoryItems;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public abstract class AbstractIconChooser extends JPanel implements TreeSelectionListener {
    //region Variable Declarations
    private AbstractIcon originalIcon;

    // display frames
    private JSplitPane splitPane;

    // The scrollpane containing the directory tree
    private JScrollPane scrpTree;

    // directory selection tree
    protected JTree treeCategories;

    // image selection list
    protected ImageList imageList;

    /** When true, icons from all subdirectories of the current selection are shown. */
    protected boolean includeSubDirs = true;
    //endregion Variable Declarations

    //region Constructors
    public AbstractIconChooser(final JTree tree, final @Nullable AbstractIcon icon) {
        initialize(tree);
        setOriginalIcon(icon);
        setSelection(icon);
    }
    //endregion Constructors

    //region Getters
    public JSplitPane getSplitPane() {
        return splitPane;
    }
    //endregion Getters

    //region Initialization
    private void initialize(final JTree tree) {
        // Set up the image list (right panel)
        imageList = new ImageList(new AbstractIconRenderer());
        JScrollPane scrpImages = new JScrollPane(imageList);
        scrpImages.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrpImages.setMinimumSize(new Dimension(500, 240));

        // set up the directory tree (left panel)
        treeCategories = tree;
        if (treeCategories != null) {
            treeCategories.addTreeSelectionListener(this);
        }
        scrpTree = new JScrollPane(treeCategories);
        scrpTree.setBackground(UIManager.getColor("Table.background"));

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, scrpTree, scrpImages);
        splitPane.setResizeWeight(0.5);

        setLayout(new BorderLayout());
        add(searchPanel(), BorderLayout.PAGE_START);
        add(splitPane, BorderLayout.CENTER);
    }

    /** Constructs a functions panel containing the search bar. */
    private JPanel searchPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 2));

        JLabel searchLbl = new JLabel("Search: ");
        JTextField search = new JTextField(20);
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                updateSearch(search.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateSearch(search.getText());
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                updateSearch(search.getText());
            }
        });
        panel.add(searchLbl);
        panel.add(search);

        panel.setBorder(new EmptyBorder(5, 5, 5, 5));
        return panel;
    }

    /**
     * Adds the icons of the given category to the given items List.
     * Assumes that the root of the path (AbstractIcon.ROOT_CATEGORY) is passed as ""!
     */
    protected void addCategoryItems(final String category, final List<AbstractIcon> items) {
        for (Iterator<String> iconNames = getDirectory().getItemNames(category);
             iconNames.hasNext(); ) {
            items.add(createIcon(category, iconNames.next()));
        }
    }
    //endregion Initialization

    //region Getters/Setters
    private AbstractIcon getOriginalIcon() {
        return originalIcon;
    }

    private void setOriginalIcon(final @Nullable AbstractIcon originalIcon) {
        this.originalIcon = originalIcon;
    }

    public ImageList getImageList() {
        return imageList;
    }

    public void setImageList(final ImageList imageList) {
        this.imageList = imageList;
    }
    //endregion Getters/Setters

    protected abstract DirectoryItems getDirectory();

    protected abstract AbstractIcon createIcon(final String category, final String filename);

    /**
     * Reacts to changes in the search field, showing searched items
     * for the search string given by contents when at least
     * 3 characters are present in the search field
     * and reverting to the selected category when the search field is
     * empty.
     */
    private void updateSearch(final String contents) {
        if (contents.isEmpty()) {
            TreePath path = treeCategories.getSelectionPath();
            if (path == null) {
                return;
            }

            // Convert the path to a single String
            // The conversion starts with the node below the root
            // if there's any, so when the root itself is selected,
            // category remains "".
            Object[] nodes = path.getPath();
            StringBuilder category = new StringBuilder();
            for (int i = 1; i < nodes.length; i++) {
                category.append((String) ((DefaultMutableTreeNode) nodes[i]).getUserObject()).append("/");
            }
            imageList.updateImages(getItems(category.toString()));
        } else if (contents.length() > 2) {
            imageList.updateImages(getSearchedItems(contents));
        }
    }

    /**
     * Returns the selected AbstractIcon
     */
    public @Nullable AbstractIcon getSelectedItem() {
        return imageList.getSelectedValue();
    }

    /**
     * Returns the index of the selected image
     */
    public int getSelectedIndex() {
        return imageList.getSelectedIndex();
    }

    /**
     * This is used to refresh the contents of the directory
     */
    public abstract void refreshDirectory();

    /**
     * This method is to ONLY be called by those methods overwriting the abstract refreshDirectory
     * above
     * @param newTree the new directory tree
     */
    protected void refreshDirectory(JTree newTree) {
        if (treeCategories != null) {
            treeCategories.removeTreeSelectionListener(this);
        }
        treeCategories = newTree;
        treeCategories.addTreeSelectionListener(this);
        scrpTree = new JScrollPane(treeCategories);
        splitPane.setLeftComponent(scrpTree);
        setSelection(getOriginalIcon());
    }

    /**
     * Called at start and when a new category is selected in the directory tree.
     * Returns a list of items that should be shown for the category which
     * is given as a Treepath.
     */
    protected abstract List<AbstractIcon> getItems(String category);

    /**
     * Called when at least 3 characters are entered into the search bar.
     *
     * @param searchString the string to search for
     * @return a list of icons that fit the provided search string
     */
    protected List<AbstractIcon> getSearchedItems(String searchString) {
        // For a category that contains the search string, all its items
        // are added to the list. Additionally, all items that contain
        // the search string are added.
        List<AbstractIcon> result = new ArrayList<>();
        String lowerSearched = searchString.toLowerCase();

        for (Iterator<String> catNames = getDirectory().getCategoryNames(); catNames.hasNext(); ) {
            String tcat = catNames.next();
            if (tcat.toLowerCase().contains(lowerSearched)) {
                addCategoryItems(tcat, result);
                continue;
            }
            for (Iterator<String> itemNames = getDirectory().getItemNames(tcat); itemNames.hasNext(); ) {
                String item = itemNames.next();
                if (item.toLowerCase().contains(lowerSearched)) {
                    result.add(createIcon(tcat, item));
                }
            }
        }

        return result;
    }

    /**
     * Selects the given category in the tree, updates the shown images to this
     * category and selects the item given by filename in the image list.
     */
    protected void setSelection(@Nullable AbstractIcon icon) {
        if (treeCategories == null) {
            return;
        }

        // This cumbersome code takes the category name and transforms it into
        // a TreePath so it can be selected in the dialog
        // When the icon directory has changes, the previous selection might not be found
        boolean found = false;
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeCategories.getModel().getRoot();
        DefaultMutableTreeNode currentNode = root;
        if (icon != null) {
            for (String name : icon.getCategory().split(Pattern.quote("/"))) {
                found = false;
                for (Enumeration<?> enm = currentNode.children(); enm.hasMoreElements(); ) {
                    DefaultMutableTreeNode child = (DefaultMutableTreeNode) enm.nextElement();
                    if (name.equals(child.getUserObject())) {
                        currentNode = child;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    break;
                }
            }
        }
        // Select the root if the selection could not be found
        if (found) {
            treeCategories.setSelectionPath(new TreePath(currentNode.getPath()));
            imageList.setSelectedValue(icon, true);
        } else {
            treeCategories.setSelectionPath(new TreePath(root.getPath()));
        }
    }

    @Override
    public void valueChanged(TreeSelectionEvent ev) {
        if (ev.getSource().equals(treeCategories)) {
            TreePath path = ev.getPath();
            if (path == null) {
                return;
            }

            // Convert the path to a single String
            // The conversion starts with the node below the root
            // if there's any, so when the root itself is selected,
            // category remains "".
            Object[] nodes = path.getPath();
            StringBuilder category = new StringBuilder();
            for (int i = 1; i < nodes.length; i++) {
                category.append((String) ((DefaultMutableTreeNode) nodes[i]).getUserObject()).append("/");
            }
            imageList.updateImages(getItems(category.toString()));
        }
    }
}
