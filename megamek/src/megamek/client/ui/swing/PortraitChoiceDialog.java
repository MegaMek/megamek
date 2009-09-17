/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

package megamek.client.ui.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.ImageFileFactory;
import megamek.common.Pilot;
import megamek.common.util.DirectoryItems;

/**
 * This dialog allows players to select a portrait for your pilot.
 *  It automatically fills itself with all of the images in the
 * "data/iamges/portraits" directory tree.
 * <p/>
 * Created on September 17, 2009
 * 
 * @author Jay Lawson
 * @version 1
 */
public class PortraitChoiceDialog extends JDialog implements 
        ListSelectionListener {

    private static final long serialVersionUID = 9220162367683378065L;

    /**
     * The parent <code>Frame</code> of this dialog.
     */
    private JFrame frame;

    /**
     * The categorized portraits.
     */
    private DirectoryItems portraits;

    /**
     * The menu containing the category names.
     */
    JComboBox categories;

    /**
     * The list containing the item names.
     */
    JList items;
    private JScrollPane scrItems;

    /**
     * The "keep old portrait" button.
     */
    private JButton keep;

    /**
     * The "select new portrait" button.
     */
    JButton select;

    /**
     * The button that launched this dialog
     */
    JButton sourceButton;

    /**
     * The previously selected category.
     */
    String prevCat;

    /**
     * The previously selected item.
     */
    String prevItem;

    /**
     * Create a dialog that allows players to choose a portrait
     * 
     * @param parent
     *            - the <code>Frame</code> that displays this dialog.
     */
    public PortraitChoiceDialog(JFrame parent, JButton button) {

        // Initialize our superclass and record our parent frame.
        super(parent, Messages
                .getString("PortraitChoiceDialog.select_portrait"), true); //$NON-NLS-1$
        frame = parent;
        sourceButton = button;

        // Declare local variables.
        Iterator<String> names;
        String name;

        // Parse the portrait directory.
        try {
            portraits = new DirectoryItems(new File("data/images/portraits"), "", //$NON-NLS-1$ //$NON-NLS-2$
                    ImageFileFactory.getInstance());
        } catch (Exception e) {
            portraits = null;
        }

        // Use a border layout.
        getContentPane().setLayout(new BorderLayout());

        // Create a pulldown menu for the categories.
        JPanel panel = new JPanel();
        getContentPane().add(panel, BorderLayout.NORTH);
        panel.setLayout(new GridBagLayout());
        GridBagConstraints layout = new GridBagConstraints();
        layout.anchor = GridBagConstraints.CENTER;
        categories = new JComboBox();
        panel.add(categories, layout);

        categories.addItem(Pilot.ROOT_PORTRAIT);
        if (portraits != null) {
            names = portraits.getCategoryNames();
            while (names.hasNext()) {
                name = names.next();
                if (!"".equals(name)) { //$NON-NLS-1$
                    categories.addItem(name);
                }
            }
        }

        categories.setSelectedIndex(0);
        
        // Refill the item list when a new category is selected.
        // Make sure that the "select new portrait" button is updated.
        categories.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    fillList((String) event.getItem());
                    updateButton();
                }
            }
        });

        // Create a list to hold the items in the category.
        items = new JList(new DefaultListModel());
        scrItems = new JScrollPane(items);
        scrItems
                .setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        getContentPane().add(scrItems);

        // Update the "select new portrait" when an item is selected.
        items.addListSelectionListener(this);
        items.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Create a panel to hold our buttons.
        // Use a grid bag layout.
        panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        getContentPane().add(panel, BorderLayout.EAST);
        layout = new GridBagConstraints();
        layout.anchor = GridBagConstraints.EAST;
        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 1;
        layout.gridheight = 1;
        layout.fill = GridBagConstraints.NONE;
        layout.ipadx = 4;
        layout.ipady = 4;
        layout.weightx = 0.0;
        layout.weighty = 0.0;

        // Add a "spacer" label to push everything else to the bottom.
        layout.weighty = 1.0;
        panel.add(new JLabel(), layout);
        layout.weighty = 0.0;
        layout.gridy++;

        // Add a label for the "keep old portrait" button.
        panel.add(new JLabel(Messages
                .getString("PortraitChoiceDialog.keep_old_portrait")), layout); //$NON-NLS-1$
        layout.gridy++;

        // Create the "keep old portrait" button.
        keep = new JButton();
        keep.setPreferredSize(new Dimension(72, 72));
        InputMap inputMap = getRootPane().getInputMap(
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false),
                "Keep");

        ActionMap actionMap = getRootPane().getActionMap();
        Action keepAction = new AbstractAction() {
            private static final long serialVersionUID = 2096792571263188573L;
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        };
        actionMap.put("Keep", keepAction);
        keep.setAction(keepAction);

        panel.add(keep, layout);
        layout.gridy++;

        // Add a label for the "select new portrait" button.
        panel.add(new JLabel(Messages
                .getString("PortraitChoiceDialog.select_new_portrait")), layout); //$NON-NLS-1$
        layout.gridy++;

        // Create the "select new portrait" button.
        select = new JButton();
        select.setPreferredSize(new Dimension(72, 72));
        panel.add(select, layout);

        // Fire the "select new portrait" action when the enter key is pressed
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false),
                "Accept");

        Action acceptAction = new AbstractAction() {
            private static final long serialVersionUID = 402810917672002505L;
            public void actionPerformed(ActionEvent e) {
                // Did the worker change their selection?
                String curCat = (String) categories.getSelectedItem();
                String curItem = (String) items.getSelectedValue();
                if (!curCat.equals(prevCat) || !curItem.equals(prevItem)) {

                    // Save the new values.
                    setPrevSelection(curCat, curItem);

                    // Update the portrait button
                    sourceButton.setIcon(generateIcon(prevCat, prevItem));
                    
                } // End selection-changed

                // Now exit.
                setVisible(false);
            }
        };

        actionMap.put("Accept", acceptAction);
        select.setAction(acceptAction);

        // Fill the item list with the roo directory.
        fillList(Pilot.ROOT_PORTRAIT);

        // Perform the initial layout.
        pack();
    }
    
    /**
     * A helper function to fill the list with items in the selected category.
     * 
     * @param category
     *            - the <code>String</code> name of the category whose items
     *            should be displayed.
     */
    void fillList(String category) {

        // Clear the list of items.
        ((DefaultListModel) items.getModel()).removeAllElements();

        // Translate the "root portrait" category name.
        Iterator<String> portraitNames;
        if (Pilot.ROOT_PORTRAIT.equals(category)) {
            ((DefaultListModel) items.getModel()).addElement(Pilot.PORTRAIT_NONE);
            portraitNames = portraits.getItemNames(""); //$NON-NLS-1$
        } else {
            portraitNames = portraits.getItemNames(category);
        }
    
        // Get the portrait names for this category.
        while (portraitNames.hasNext()) {
            ((DefaultListModel) items.getModel()).addElement(portraitNames
                    .next());
        }
        items.setSelectedIndex(0);
    }

    /**
     * A helper function to assign values for the previously selected portrait. This
     * function will also set the "keep old portrait" button's image.
     * 
     * @param category
     *            - the <code>String</code> category name. This value must be
     *            one of the categories from the <code>DirectoryItems</code>.
     * @param item
     *            - the <code>String</code> name of the item. This value must be
     *            one of the items in the named category from
     *            <code>DirectoryItems</code>.
     */
    void setPrevSelection(String category, String item) {
        prevCat = category;
        prevItem = item;
        keep.setIcon(generateIcon(prevCat, prevItem));
    }

    Icon generateIcon(String cat, String item) {
        if(null == cat || null == item || Pilot.PORTRAIT_NONE.equals(item)) {
            return null;
        }
        String actualCat = cat;
        // Replace the ROOT_PORTRAIT string with "".
        if (Pilot.ROOT_PORTRAIT.equals(actualCat)) {
            actualCat = ""; //$NON-NLS-1$
        }
      
        //an actual portrait
        try {
            // We need to copy the image to make it appear.
            Image image = (Image) portraits.getItem(actualCat, item);
            image = image.getScaledInstance(-1, 72, Image.SCALE_DEFAULT);
            return new ImageIcon(image);
        } catch (Exception err) {
            // Print the stack trace and display the message.
            err.printStackTrace();
            JOptionPane
                    .showMessageDialog(
                            frame,
                            err.getMessage(),
                            Messages
                                    .getString("PortraitChoiceDialog.error_getting_portrait"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
            return null;
        }
    }

    /**
     * Update the "select new portrait" button whenever a list item is selected.
     * <p/>
     */
    void updateButton() {
        // Get the category and the item.
        String curCat = (String) categories.getSelectedItem();
        String curItem = (String) items.getSelectedValue();
        if (curItem == null) {
            //nothing selected yet
            select.setIcon(null);
            return;
        }
        select.setIcon(generateIcon(curCat, curItem));
    }

    /**
     * Set the selected category.
     * 
     * @param category
     *            - the <code>String</code> name of the desired category. This
     *            value may be <code>null</code>. If no match is found, the
     *            category will not change.
     */
    private void setCategory(String category) {

        // Get the current selection.
        String cur = (String) categories.getSelectedItem();

        // Do nothing, if the request is for the selected item.
        if (!cur.equals(category)) {

            // Try to find the requested item.
            for (int loop = 0; loop < categories.getItemCount(); loop++) {

                // Did we find it?
                if (categories.getItemAt(loop).equals(category)) {

                    // Select this position.
                    categories.setSelectedIndex(loop);

                    // Fill the list.
                    fillList(category);

                    // Stop looking for the category.
                    break;

                } // End found-requested-category

            } // Check the next category

        } // End new-selection

    }

    /**
     * Set the selected item in the currently-selected category.
     * 
     * @param item
     *            - the <code>String</code> name of the desired item. This value
     *            may be <code>null</code>. If no match is found, the item
     *            selection will not change.
     */
    private void setItemName(String item) {

        // Do nothing is we're passed a null.
        if (item != null) {

            // Get the current selection.
            String cur = (String) items.getSelectedValue();

            // Do nothing, if the request is for the selected item.
            if (!item.equals(cur)) {
                items.setSelectedValue(item, true);
            } // End new-selection

        } // End not-passed-null
    }

    /**
     * Show the dialog. Make sure that all selections have been applied.
     * <p/>
     * Overrides <code>Dialog#setVisible(boolean)</code>.
     */
    @Override
    public void setVisible(boolean visible) {

        // Make sure the "keep" button is set correctly.
        setPrevSelection((String) categories.getSelectedItem(), (String) items
                .getSelectedValue());

        // Make sure the "select" button is set correctly.
        updateButton();

        // Now show the dialog.
        super.setVisible(visible);
    }

    public void valueChanged(ListSelectionEvent event) {
        updateButton();
    }
    
    public String getCategory() {
        return prevCat;
    }
    
    public String getItem() {
        return prevItem;
    }
    
    public void setPilot(Pilot pilot) {
        setCategory(pilot.getPortraitCategory());
        setItemName(pilot.getPortraitFileName());
        setPrevSelection(pilot.getPortraitCategory(), pilot.getPortraitFileName());
        if (sourceButton.isVisible()) {
            sourceButton.setIcon(generateIcon(prevCat, prevItem));
        }
    }
}
