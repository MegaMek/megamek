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

import megamek.client.ui.baseComponents.AbstractPanel;
import megamek.client.ui.lists.ImageList;
import megamek.client.ui.preferences.JSplitPanePreference;
import megamek.client.ui.preferences.JToggleButtonPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.renderers.AbstractIconRenderer;
import megamek.common.annotations.Nullable;
import megamek.common.icons.AbstractIcon;
import megamek.common.util.fileUtils.AbstractDirectory;

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

public abstract class AbstractIconChooser extends AbstractPanel implements TreeSelectionListener {
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

    /** When selected, icons from all subdirectories of the current selection are shown. */
    private JCheckBox chkIncludeSubdirectories;
    //endregion Variable Declarations

    //region Constructors
    protected AbstractIconChooser(final JFrame frame, final String name, final @Nullable JTree tree,
                                  final @Nullable AbstractIcon icon) {
        this(frame, name, tree, icon, true);
    }

    protected AbstractIconChooser(final JFrame frame, final String name, final @Nullable JTree tree,
                                  final @Nullable AbstractIcon icon, final boolean initialize) {
        super(frame, name);
        setOriginalIcon(icon);
        setTreeCategories(tree);

        if (initialize) {
            initialize();
            setSelection(icon);
        }
    }
    //endregion Constructors

    //region Getters/Setters
    public @Nullable AbstractIcon getOriginalIcon() {
        return originalIcon;
    }

    public void setOriginalIcon(final @Nullable AbstractIcon originalIcon) {
        this.originalIcon = originalIcon;
    }

    public JSplitPane getSplitPane() {
        return splitPane;
    }

    public @Nullable JTree getTreeCategories() {
        return treeCategories;
    }

    public void setTreeCategories(final @Nullable JTree treeCategories) {
        this.treeCategories = treeCategories;
    }

    public ImageList getImageList() {
        return imageList;
    }

    public void setImageList(final ImageList imageList) {
        this.imageList = imageList;
    }

    public JCheckBox getChkIncludeSubdirectories() {
        return chkIncludeSubdirectories;
    }

    public void setChkIncludeSubdirectories(final JCheckBox chkIncludeSubdirectories) {
        this.chkIncludeSubdirectories = chkIncludeSubdirectories;
    }
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected void initialize() {
        // Set up the image list (right panel)
        setImageList(new ImageList(new AbstractIconRenderer()));
        final JScrollPane scrpImages = new JScrollPane(getImageList());
        scrpImages.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrpImages.setMinimumSize(new Dimension(500, 240));

        // set up the directory tree (left panel)
        if (getTreeCategories() != null) {
            getTreeCategories().addTreeSelectionListener(this);
        }
        scrpTree = new JScrollPane(getTreeCategories());
        scrpTree.setBackground(UIManager.getColor("Table.background"));

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, scrpTree, scrpImages);
        getSplitPane().setName("iconSplitPane");
        getSplitPane().setResizeWeight(0.5);

        setLayout(new BorderLayout());
        add(searchPanel(), BorderLayout.PAGE_START);
        add(getSplitPane(), BorderLayout.CENTER);

        finalizeInitialization();
    }


    /** Constructs a functions panel containing the search bar. */
    private JPanel searchPanel() {
        final JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 2));
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));
        panel.setName("searchPanel");

        panel.add(new JLabel("Search: "));

        final JTextField search = new JTextField(20);
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
        panel.add(search);

        setChkIncludeSubdirectories(new JCheckBox("Include Subdirectories"));
        getChkIncludeSubdirectories().setToolTipText("Include files contained in subdirectories in the displayed selection. This holds for both general function and the search.");
        getChkIncludeSubdirectories().setName("chkIncludeSubdirectories");
        getChkIncludeSubdirectories().setSelected(true);
        getChkIncludeSubdirectories().addActionListener(evt -> updateSearch(""));
        panel.add(getChkIncludeSubdirectories());

        return panel;
    }

    private void addCategoryIcons(final String category, final List<AbstractIcon> icons) {
        for (final Iterator<String> iconNames = getDirectory().getItemNames(category); iconNames.hasNext(); ) {
            icons.add(createIcon(category, iconNames.next()));
        }
    }

    protected void finalizeInitialization() {
        setPreferences();
    }

    @Override
    protected void setCustomPreferences(final PreferencesNode preferences) {
        super.setCustomPreferences(preferences);
        preferences.manage(new JSplitPanePreference(getSplitPane()));
        preferences.manage(new JToggleButtonPreference(getChkIncludeSubdirectories()));
    }
    //endregion Initialization

    protected abstract @Nullable AbstractDirectory getDirectory();

    protected abstract AbstractIcon createIcon(String category, final String filename);

    /**
     * Reacts to changes in the search field, showing searched items
     * for the search string given by contents when at least
     * 3 characters are present in the search field
     * and reverting to the selected category when the search field is
     * empty.
     */
    private void updateSearch(final String contents) {
        if (contents.length() > 2) {
            getImageList().updateImages(getSearchedItems(contents));
        } else {
            final TreePath path = getTreeCategories().getSelectionPath();
            if (path == null) {
                return;
            }

            // Convert the path to a single String
            // The conversion starts with the node below the root
            // if there's any, so when the root itself is selected,
            // category remains "".
            final Object[] nodes = path.getPath();
            final StringBuilder category = new StringBuilder();
            for (int i = 1; i < nodes.length; i++) {
                category.append((String) ((DefaultMutableTreeNode) nodes[i]).getUserObject()).append("/");
            }
            getImageList().updateImages(getIcons(category.toString()));
        }
    }

    /**
     * Returns the selected AbstractIcon
     */
    public @Nullable AbstractIcon getSelectedItem() {
        return getImageList().getSelectedValue();
    }

    /**
     * Clears the selected AbstractIcon(s)
     */
    public void clearSelectedItems() {
        getImageList().clearSelection();
    }

    /**
     * Returns the index of the selected image
     */
    public int getSelectedIndex() {
        return getImageList().getSelectedIndex();
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
    protected void refreshDirectory(final JTree newTree) {
        if (getTreeCategories() != null) {
            getTreeCategories().removeTreeSelectionListener(this);
        }
        setTreeCategories(newTree);
        getTreeCategories().addTreeSelectionListener(this);
        scrpTree = new JScrollPane(getTreeCategories());
        getSplitPane().setLeftComponent(scrpTree);
        setSelection(getOriginalIcon());
    }

    /**
     * Called at start and when a new category is selected in the directory tree.
     * Assumes that the root of the path (AbstractIcon.ROOT_CATEGORY) is passed as ""!
     * @return a list of items that should be shown for the category which is given as a TreePath.
     */
    protected List<AbstractIcon> getIcons(final String category) {
        if (getDirectory() == null) {
            return new ArrayList<>();
        }
        final List<AbstractIcon> icons = new ArrayList<>();
        if (getChkIncludeSubdirectories().isSelected()) {
            recursivelyDetermineCategoryIcons(getDirectory().getCategory(category), icons);
        } else {
            addCategoryIcons(category, icons);
        }
        return icons;
    }

    private void recursivelyDetermineCategoryIcons(final @Nullable AbstractDirectory category,
                                                   final List<AbstractIcon> icons) {
        if (category == null) {
            return;
        }

        category.getItems().keySet().forEach(f -> icons.add(createIcon(category.getRootPath(), f)));
        category.getCategories().values().forEach(c -> recursivelyDetermineCategoryIcons(c, icons));
    }

    /**
     * Called when at least 3 characters are entered into the search bar.
     *
     * @param searchString the string to search for
     * @return a list of icons that fit the provided search string
     */
    protected List<AbstractIcon> getSearchedItems(String searchString) {
        if (getDirectory() == null) {
            return new ArrayList<>();
        }

        // For a category that contains the search string, all its items
        // are added to the list. Additionally, all items that contain
        // the search string are added.
        final List<AbstractIcon> result = new ArrayList<>();
        final String lowerSearched = searchString.toLowerCase();

        for (final String category : getDirectory().getNonEmptyCategoryPaths()) {
            if (category.toLowerCase().contains(lowerSearched)) {
                addCategoryIcons(category, result);
                continue;
            }

            for (final Iterator<String> itemNames = getDirectory().getItemNames(category); itemNames.hasNext(); ) {
                final String item = itemNames.next();
                if (item.toLowerCase().contains(lowerSearched)) {
                    result.add(createIcon(category, item));
                }
            }
        }

        return result;
    }

    /**
     * Selects the given category in the tree, updates the shown images to this
     * category and selects the item given by filename in the image list.
     */
    protected void setSelection(final @Nullable AbstractIcon icon) {
        if (getTreeCategories() == null) {
            return;
        }

        // This cumbersome code takes the category name and transforms it into a TreePath, so it can
        // be selected in the dialog.
        // When the icon directory has changes, the previous selection might not be found
        boolean found = false;
        final DefaultMutableTreeNode root = (DefaultMutableTreeNode) getTreeCategories().getModel().getRoot();
        DefaultMutableTreeNode currentNode = root;
        if (icon != null) {
            for (final String name : icon.getCategory().split(Pattern.quote("/"))) {
                found = false;
                for (final Enumeration<?> enm = currentNode.children(); enm.hasMoreElements(); ) {
                    final DefaultMutableTreeNode child = (DefaultMutableTreeNode) enm.nextElement();
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
            getTreeCategories().setSelectionPath(new TreePath(currentNode.getPath()));
            getImageList().setSelectedValue(icon, true);
        } else {
            getTreeCategories().setSelectionPath(new TreePath(root.getPath()));
        }
    }

    @Override
    public void valueChanged(final TreeSelectionEvent evt) {
        if (evt.getSource().equals(getTreeCategories())) {
            final TreePath path = evt.getPath();
            if (path == null) {
                return;
            }

            // Convert the path to a single String
            // The conversion starts with the node below the root
            // if there's any, so when the root itself is selected, category remains "".
            final Object[] nodes = path.getPath();
            final StringBuilder category = new StringBuilder();
            for (int i = 1; i < nodes.length; i++) {
                category.append((String) ((DefaultMutableTreeNode) nodes[i]).getUserObject()).append("/");
            }
            getImageList().updateImages(getIcons(category.toString()));
        }
    }
}
