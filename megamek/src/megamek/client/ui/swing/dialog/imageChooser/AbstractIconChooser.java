/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing.dialog.imageChooser;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.GUIPreferences;
import megamek.common.icons.AbstractIcon;

/**
 * Creates a dialog that allows players to select a directory from
 * a directory tree and choose an image from the images in that directory.
 * Subclasses must provide the getItems() method that translates
 * a given category (directory) selected in the tree to a list
 * of items (images) to show in the list.
 * Subclasses can provide getSearchedItems() that translates a given search
 * String to the list of "found" items. If this is provided, showSearch(true)
 * should be called in the constructor to show the search panel.
 */
public abstract class AbstractIconChooser extends JDialog implements TreeSelectionListener {
    //region Variable Declarations
    private static final long serialVersionUID = -7836502700465322620L;
    protected static final GUIPreferences GUIP = GUIPreferences.getInstance();

    // display frames
    private JSplitPane splitPane;

    // The scrollpane containing the directory tree
    private JScrollPane scrpTree;

    // directory selection tree
    protected JTree treeCategories;

    // image selection list
    protected ImageList imageList;

    /** True when the user canceled. */
    private boolean wasCanceled = false;

    /** When true, camos from all subdirectories of the current selection are shown. */
    protected boolean includeSubDirs = true;
    //endregion Variable Declarations

    //region Constructors
    /**
     * Creates a dialog that allows players to choose a directory from
     * a directory tree and an image from the images in that directory.
     *
     * @param parent The Window (dialog or frame) hosting this dialog
     * @param title the dialog title
     * @param renderer A ListCellRenderer<AbstractIcon> to show the images
     * @param tree the JTree with the directories
     */
    public AbstractIconChooser(Window parent, AbstractIcon icon, String title,
                               ListCellRenderer<AbstractIcon> renderer, JTree tree) {
        super(parent, title, ModalityType.APPLICATION_MODAL);

        initialize(renderer, tree);

        if (icon != null) {
            setSelection(icon);
        }
    }
    //endregion Constructors

    //region Initialization
    private void initialize(ListCellRenderer<AbstractIcon> renderer, JTree tree) {
        // Set up the image list (right panel)
        imageList = new ImageList(renderer);
        imageList.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    select();
                }
            }
        });
        JScrollPane scrpImages = new JScrollPane(imageList);
        scrpImages.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrpImages.setMinimumSize(new Dimension(500, 240));

        // set up the directory tree (left panel)
        treeCategories = tree;
        treeCategories.addTreeSelectionListener(this);
        scrpTree = new JScrollPane(treeCategories);
        scrpTree.setBackground(UIManager.getColor("Table.background"));

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, scrpTree, scrpImages);
        splitPane.setResizeWeight(0.5);

        setLayout(new BorderLayout());
        JPanel searchPanel = searchPanel();
        add(searchPanel, BorderLayout.PAGE_START);
        add(splitPane, BorderLayout.CENTER);
        add(buttonPanel(), BorderLayout.PAGE_END);

        // Size and position of the dialog
        setMinimumSize(new Dimension(480, 240));
        splitPane.setDividerLocation(GUIP.getInt(GUIPreferences.IMAGE_CHOOSER_SPLIT_POS));
        setSize(GUIP.getImageChoiceSizeWidth(), GUIP.getImageChoiceSizeHeight());
        setLocation(GUIP.getImageChoicePosX(), GUIP.getImageChoicePosY());

        // Make the close "X" cancel the dialog
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cancel();
            }
        });
    }

    /** Constructs the bottom panel with the Okay and Cancel buttons. */
    private JPanel buttonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 2));

        JButton btnCancel = new JButton(Messages.getString("Cancel"));
        btnCancel.addActionListener(evt -> cancel());

        JButton btnOkay = new JButton(Messages.getString("Okay"));
        btnOkay.addActionListener(evt -> select());

        panel.add(btnOkay);
        panel.add(btnCancel);
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));
        return panel;
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
     * Adds the portraits of the given category to the given items List.
     * Assumes that the root of the path {@link AbstractIcon}.ROOT_CATEGORY is passed as ""!
     */
    protected abstract void addCategoryItems(String category, List<AbstractIcon> items);
    //endregion Initialization

    /**
     * Reacts to changes in the search field, showing searched items
     * for the search string given by contents when at least
     * 3 characters are present in the search field
     * and reverting to the selected category when the search field is
     * empty.
     */
    private void updateSearch(String contents) {
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

    /** Returns the selected AbstractIcon. May be null. */
    public AbstractIcon getSelectedItem() {
        return imageList.getSelectedValue();
    }

    /** Returns the index of the selected image. */
    public int getSelectedIndex() {
        return imageList.getSelectedIndex();
    }

    /** Activates the dialog and returns if the user cancelled. */
    public int showDialog() {
        wasCanceled = false;
        setVisible(true);
        // After returning from the modal dialog, save settings the return whether it was cancelled or not...
        saveWindowSettings();
        return wasCanceled ? JOptionPane.CANCEL_OPTION : JOptionPane.OK_OPTION;
    }

    /** Called when the Okay button is pressed or an image is double-clicked. */
    protected void select() {
        wasCanceled = false;
        setVisible(false);
    }

    private void cancel() {
        wasCanceled = true;
        setVisible(false);
    }

    /** Saves the position, size and split of the dialog. */
    private void saveWindowSettings() {
        GUIP.setValue(GUIPreferences.IMAGE_CHOOSER_POS_X, getLocation().x);
        GUIP.setValue(GUIPreferences.IMAGE_CHOOSER_POS_Y, getLocation().y);
        GUIP.setValue(GUIPreferences.IMAGE_CHOOSER_SIZE_WIDTH, getSize().width);
        GUIP.setValue(GUIPreferences.IMAGE_CHOOSER_SIZE_HEIGHT, getSize().height);
        GUIP.setValue(GUIPreferences.IMAGE_CHOOSER_SPLIT_POS, splitPane.getDividerLocation());
    }

    /**
     * Called at start and when a new category is selected in the directory tree.
     * Returns a list of items that should be shown for the category which
     * is given as a Treepath.
     */
    protected abstract List<AbstractIcon> getItems(String category);

    /**
     * Called when at least 3 characters are entered into the search bar.
     * Returns a list of items that should be shown for this particular search string.
     */
    protected List<AbstractIcon> getSearchedItems(String searchString) {
        return new ArrayList<>();
    }

    /**
     * Selects the given category in the tree, updates the shown images to this
     * category and selects the item given by filename in the
     * image list.
     */
    protected void setSelection(AbstractIcon icon) {
        // This cumbersome code takes the category name and transforms it into
        // a TreePath so it can be selected in the dialog
        // When the icon directory has changes, the previous selection might not be found
        boolean found = false;
        String[] names = icon.getCategory().split(Pattern.quote("/"));
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeCategories.getModel().getRoot();
        DefaultMutableTreeNode currentNode = root;
        for (String name : names) {
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
        // Select the root if the selection could not be found
        if (found) {
            treeCategories.setSelectionPath(new TreePath(currentNode.getPath()));
            imageList.setSelectedValue(icon, true);
        } else {
            treeCategories.setSelectionPath(new TreePath(root.getPath()));
        }
    }

    /**
     * This is used to refresh the contents of the directory
     */
    protected abstract void refreshDirectory();

    /**
     * This method is to ONLY be called by those methods overwriting the abstract refreshDirectory
     * above
     * @param newTree the new directory tree
     */
    protected void refreshDirectory(JTree newTree) {
        treeCategories.removeTreeSelectionListener(this);
        treeCategories = newTree;
        treeCategories.addTreeSelectionListener(this);
        scrpTree = new JScrollPane(treeCategories);
        splitPane.setLeftComponent(scrpTree);
        splitPane.setDividerLocation(GUIP.getInt(GUIPreferences.IMAGE_CHOOSER_SPLIT_POS));
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
