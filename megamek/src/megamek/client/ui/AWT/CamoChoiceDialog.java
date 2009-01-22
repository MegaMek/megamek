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

package megamek.client.ui.AWT;

import java.awt.BorderLayout;
import java.awt.Choice;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.ItemSelectable;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Iterator;
import java.util.Vector;

import megamek.client.ui.Messages;
import megamek.client.ui.AWT.util.ImageFileFactory;
import megamek.client.ui.AWT.util.PlayerColors;
import megamek.client.ui.AWT.widget.AdvancedLabel;
import megamek.client.ui.AWT.widget.ImageButton;
import megamek.common.Player;
import megamek.common.util.DirectoryItems;

/**
 * This dialog allows players to select the camo pattern (or color) used by
 * their units during the game. It automatically fills itself with all the color
 * choices in <code>Settings</code> and all the camo patterns in the
 * "data/iamges/camo" directory tree. Created on January 19, 2004
 * 
 * @author James Damour
 * @version 1
 */
public class CamoChoiceDialog extends Dialog implements ActionListener,
        ItemListener, ItemSelectable {

    /**
     * 
     */
    private static final long serialVersionUID = -7942369852180659605L;

    /**
     * The parent <code>Frame</code> of this dialog.
     */
    private Frame frame;

    /**
     * The categorized camo patterns.
     */
    private DirectoryItems camos;

    /**
     * The menu containing the category names.
     */
    private Choice categories;

    /**
     * The list containing the item names.
     */
    private List items;

    /**
     * The "keep old camo" button.
     */
    private ImageButton keep;

    /**
     * The "select new camo" button.
     */
    private ImageButton select;

    /**
     * The previously selected category.
     */
    private String prevCat;

    /**
     * The previously selected item.
     */
    private String prevItem;

    /**
     * The registered camo selection listeners.
     */
    private Vector<ItemListener> listeners = new Vector<ItemListener>();

    /**
     * A helper function to close the dialog.
     */
    /* package */void close() {

        // Make sure the previous selection is the current selection.
        setCategory(prevCat);
        setItemName(prevItem);

        // And hide the dialog.
        setVisible(false);
    }

    /**
     * A helper function to fill the list with items in the selected category.
     * 
     * @param category - the <code>String</code> name of the category whose
     *            items should be displayed.
     */
    /* package */void fillList(String category) {

        // Clear the list of items.
        items.removeAll();

        // If this is the "no camos" category, then
        // fill the item list with the colors.
        if (Player.NO_CAMO.equals(category)) {
            for (int color = 0; color < Player.colorNames.length; color++) {
                items.add(Player.colorNames[color]);
            }
        }

        // Otherwise, fill the list with the camo names.
        else {

            // Translate the "root camo" category name.
            Iterator<String> camoNames;
            if (Player.ROOT_CAMO.equals(category)) {
                camoNames = camos.getItemNames(""); //$NON-NLS-1$
            } else {
                camoNames = camos.getItemNames(category);
            }

            // Get the camo names for this category.
            while (camoNames.hasNext()) {
                items.add(camoNames.next());
            }
        }

        // If this is the previous selection, then
        // select the previous item in the category.
        // Otherwise, select the first item in the list.
        if (prevCat.equals(category)) {
            setItemName(prevItem);
        } else {
            setItemName(items.getItem(0));
        }

    }

    /**
     * A helper function to assign values for the previously selected camo. This
     * function will also set the "keep old camo" button's image. Please note,
     * if the specified selection does not exist, or if there is an error when
     * generating the selection's image, the values won't change.
     * 
     * @param category - the <code>String</code> category name. This value
     *            must be one of the categories from the
     *            <code>DirectoryItems</code>.
     * @param item - the <code>String</code> name of the item. This value must
     *            be one of the items in the named category from
     *            <code>DirectoryItems</code>.
     */
    /* package */void setPrevSelection(String category, String item) {

        // If a "no camo" item is selected, clear the image.
        if (Player.NO_CAMO.equals(category)) {
            keep.setImage(null);

            // Find the correct background color.
            for (int color = 0; color < Player.colorNames.length; color++) {
                if (Player.colorNames[color].equals(item)) {
                    keep.setBackground(PlayerColors.getColor(color));
                    prevCat = category;
                    prevItem = item;
                    break;
                }
            }
        }

        // Otherwise, clear the background color and try to
        // set the camo image for the "keep old camo" button.
        else {
            try {
                // Don't forget to translate the ROOT_CAMO.
                String curCat = category;
                if (Player.ROOT_CAMO.equals(curCat)) {
                    curCat = ""; //$NON-NLS-1$
                }

                // We need to copy the image to make it appear.
                Image image = (Image) camos.getItem(curCat, item);

                // Now, we're ready.
                keep.setBackground(categories.getBackground());
                keep.setImage(image);
                prevCat = category;
                prevItem = item;
            } catch (Exception err) {
                // Print the stack trace and display the message.
                err.printStackTrace();
                AlertDialog dlg = new AlertDialog(
                        frame,
                        Messages
                                .getString("CamoChoiceDialog.error_getting_camo"), err.getMessage()); //$NON-NLS-1$
                dlg.setVisible(true);
                dlg.dispose();
            }
        }
    }

    /**
     * Create a dialog that allows players to choose a camo pattern.
     * 
     * @param parent - the <code>Frame</code> that displays this dialog.
     */
    public CamoChoiceDialog(Frame parent) {

        // Initialize our superclass and record our parent frame.
        super(parent, Messages
                .getString("CamoChoiceDialog.select_camo_pattern"), true); //$NON-NLS-1$
        frame = parent;

        // Declare local variables.
        Iterator<String> names;
        String name;

        // Parse the camo directory.
        try {
            camos = new DirectoryItems(new File("data/images/camo"), "", //$NON-NLS-1$ //$NON-NLS-2$
                    ImageFileFactory.getInstance());
        } catch (Exception e) {
            camos = null;
        }

        // Close the window, when the WM says to.
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                close();
            }
        });

        // Use a border layout.
        this.setLayout(new BorderLayout());

        // Create a pulldown menu for the categories.
        Panel panel = new Panel();
        this.add(panel, BorderLayout.NORTH);
        panel.setLayout(new GridBagLayout());
        GridBagConstraints layout = new GridBagConstraints();
        layout.anchor = GridBagConstraints.CENTER;
        categories = new Choice();
        panel.add(categories, layout);

        // Fill the pulldown. Include the "no camo" category.
        // Make sure the "no camo" and "root camo" are at top.
        // Only add the "root camo" category if it contains items.
        categories.addItem(Player.NO_CAMO);
        if (camos != null) {
            if (camos.getItemNames("").hasNext()) { //$NON-NLS-1$
                categories.addItem(Player.ROOT_CAMO);
            }
            names = camos.getCategoryNames();
            while (names.hasNext()) {
                name = names.next();
                if (!name.equals("")) { //$NON-NLS-1$
                    categories.addItem(name);
                }
            }
        }

        // Refill the item list when a new category is selected.
        // Make sure that the "select new camo" button is updated.
        categories.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                fillList((String) event.getItem());
                CamoChoiceDialog.this.itemStateChanged(new ItemEvent(items,
                        event.getID(), items.getSelectedItem(),
                        ItemEvent.SELECTED));
            }
        });

        // Create a list to hold the items in the category.
        items = new List(15);
        this.add(items, BorderLayout.CENTER);

        // Update the "select new camo" when an item is selected.
        items.addItemListener(this);

        // Create a panel to hold our buttons.
        // Use a grid bag layout.
        panel = new Panel();
        panel.setLayout(new GridBagLayout());
        this.add(panel, BorderLayout.EAST);
        layout = new GridBagConstraints();
        layout.anchor = GridBagConstraints.EAST;
        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 1;
        layout.gridheight = 1;
        layout.fill = GridBagConstraints.NONE;
        layout.ipadx = 4;
        layout.ipady = 4;
        layout.weightx = 0;
        layout.weighty = 0;

        // Add a "spacer" label to push everything else to the bottom.
        layout.weighty = 1;
        panel.add(new Label(), layout);
        layout.weighty = 0;
        layout.gridy++;

        // Add a label for the "keep old camo" button.
        panel.add(new AdvancedLabel(Messages
                .getString("CamoChoiceDialog.keep_old_camo")), layout); //$NON-NLS-1$
        layout.gridy++;

        // Create the "keep old camo" button.
        keep = new ImageButton();
        keep.setLabel(Messages.getString("CamoChoiceDialog.no_camo")); //$NON-NLS-1$
        keep.setPreferredSize(84, 72);
        keep.addActionListener(new ActionListener() {
            // Pressing this button closes without saving.
            public void actionPerformed(ActionEvent event) {
                close();
            }
        });
        keep.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent event) {
                // Pressing enter on this button closes without saving.
                if (KeyEvent.VK_ENTER == event.getKeyCode()) {
                    close();
                }
            }
        });
        panel.add(keep, layout);
        layout.gridy++;

        // Add a label for the "select new camo" button.
        panel.add(new AdvancedLabel(Messages
                .getString("CamoChoiceDialog.select_new_camo")), layout); //$NON-NLS-1$
        layout.gridy++;

        // Create the "select new camo" button.
        select = new ImageButton();
        select.setLabel(Messages.getString("CamoChoiceDialog.no_camo")); //$NON-NLS-1$
        select.setPreferredSize(84, 72);
        select.addActionListener(this);
        panel.add(select, layout);

        // Fire the "select new camo" action when the enter key is pressed
        // on either the list or the "select new camo" button.
        KeyAdapter enterAdapter = new KeyAdapter() {
            public void keyPressed(KeyEvent event) {
                if (KeyEvent.VK_ENTER == event.getKeyCode()) {
                    actionPerformed(new ActionEvent(select, event.getID(),
                            select.getActionCommand()));
                }
            }
        };
        items.addKeyListener(enterAdapter);
        select.addKeyListener(enterAdapter);

        // Set the "previously selected" values to the defaults.
        setPrevSelection(Player.NO_CAMO, Player.colorNames[0]);

        // Fill the item list with the colors.
        fillList(Player.NO_CAMO);

        // Perform the initial layout.
        this.pack();
    }

    /**
     * Handle the "select new camo" button's action. <p/> Implements
     * <code>ActionListener</code>.
     * 
     * @param event - the <code>ActionEvent</code> that was invoked.
     */
    public void actionPerformed(ActionEvent event) {

        // Did the worker change their selection?
        String curCat = categories.getSelectedItem();
        String curItem = items.getSelectedItem();
        if (!curCat.equals(prevCat) || !curItem.equals(prevItem)) {

            // Save the new values.
            setPrevSelection(curCat, curItem);

            // Are there any listeners?
            if (!listeners.isEmpty()) {

                // Get the Image.
                Image image = null;
                Image[] array = (Image[]) this.getSelectedObjects();
                if (null != array)
                    image = array[0];

                // Create an ItemEvent for the change.
                ItemEvent alert = new ItemEvent(this, event.getID(), image,
                        ItemEvent.ITEM_STATE_CHANGED);

                // Alert the listeners.
                for (ItemListener l : listeners) {
                    l.itemStateChanged(alert);
                }

            } // End have-listeners

        } // End selection-changed

        // Now exit.
        close();
    }

    /**
     * Update the "select new camo" button whenever a list item is selected.
     * <p/> Implements <code>ItemListener</code>.
     * 
     * @param event - the <code>ItemEvent</code> for the list selection.
     */
    public void itemStateChanged(ItemEvent event) {

        // Get the category and the index of the selected item.
        String curCat = categories.getSelectedItem();

        // If a "no camo" item is selected, clear the image.
        if (Player.NO_CAMO.equals(curCat)) {
            select.setImage(null);
            select.setBackground(PlayerColors
                    .getColor(items.getSelectedIndex()));
            return;
        }

        // Replace the ROOT_CAMO string with "".
        if (Player.ROOT_CAMO.equals(curCat)) {
            curCat = ""; //$NON-NLS-1$
        }

        // Clear the background and try to set the camo image.
        try {
            select.setBackground(categories.getBackground());
            select.setImage((Image) camos.getItem(curCat, items
                    .getSelectedItem()));
        } catch (Exception err) {
            // Print the stack trace and display the message.
            err.printStackTrace();
            AlertDialog dlg = new AlertDialog(
                    frame,
                    Messages.getString("CamoChoiceDialog.error_getting_camo"), err.getMessage()); //$NON-NLS-1$
            dlg.setVisible(true);
            dlg.dispose();
        }
    }

    /**
     * Get the most recently selected (and confirmed) image. The player must
     * click the "select new camo" button to change this value. <p/> Implements
     * <code>ItemSelectable</code>.
     * 
     * @return If the player has selected from the "no camo" category, or if an
     *         error occurs in getting the selected image, a <code>null</code>
     *         is returned. Otherwise, the array will contain a single
     *         <code>Image</code>.
     */
    public Object[] getSelectedObjects() {

        // Update the prev selection.
        setPrevSelection(categories.getSelectedItem(), items.getSelectedItem());

        // Return a null if the "no camo" category is selected.
        if (Player.NO_CAMO.equals(prevCat))
            return null;

        // Try to get the selected camo's Image.
        // Don't forget to translate the ROOT_CAMO.
        Image image = null;
        try {
            String curCat = prevCat;
            if (Player.ROOT_CAMO.equals(curCat)) {
                curCat = ""; //$NON-NLS-1$
            }
            image = (Image) camos.getItem(curCat, prevItem);
        } catch (Exception err) {
            // Print the stack trace and display the message.
            err.printStackTrace();
            AlertDialog dlg = new AlertDialog(
                    frame,
                    Messages.getString("CamoChoiceDialog.error_getting_camo"), err.getMessage()); //$NON-NLS-1$
            dlg.setVisible(true);
            dlg.dispose();
        }
        if (null == image)
            return null;

        // Return an array containing the image.
        Image[] ret = new Image[1];
        ret[0] = image;
        return ret;
    }

    /**
     * Add an <code>ItemListener</code> that wants to be alerted when a new
     * camo is selected. <p/> Implements <code>ItemSelectable</code>.
     * 
     * @param listener - the <code>ItemListener</code> to be alerted.
     */
    public void addItemListener(ItemListener listener) {

        // Don't add a listener multiple times.
        if (!listeners.contains(listener)) {
            listeners.addElement(listener);
        }
    }

    /**
     * Remove an <code>ItemListener</code> that wants to stop be alerted when
     * a new camo is selected. <p/> Implements <code>ItemSelectable</code>.
     * 
     * @param listener - the <code>ItemListener</code> to be alerted.
     */
    public void removeItemListener(ItemListener listener) {
        listeners.removeElement(listener);
    }

    /**
     * Get the selected category.
     * 
     * @return the <code>String</code> name of the most recently selected
     *         category. This value will not be <code>null</code>.
     */
    public String getCategory() {
        return prevCat;
    }

    /**
     * Get the selected item's name. If the most recently selected category is
     * <code>Player.NO_CAMO</code>, then the item named is a color from
     * <code>Player.colorNames</code>.
     * 
     * @return the <code>String</code> name of the most recently selected
     *         item. This value will not be <code>null</code>.
     */
    public String getItemName() {
        return prevItem;
    }

    /**
     * Set the selected category.
     * 
     * @param category - the <code>String</code> name of the desired category.
     *            This value may be <code>null</code>. If no match is found,
     *            the category will not change.
     */
    public void setCategory(String category) {

        // Get the current selection.
        String cur = categories.getSelectedItem();

        // Do nothing, if the request is for the selected item.
        if (!cur.equals(category)) {

            // Try to find the requested item.
            for (int loop = 0; loop < categories.getItemCount(); loop++) {

                // Did we find it?
                if (categories.getItem(loop).equals(category)) {

                    // Select this position.
                    categories.select(loop);

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
     * @param item - the <code>String</code> name of the desired item. This
     *            value may be <code>null</code>. If no match is found, the
     *            item selection will not change.
     */
    public void setItemName(String item) {

        // Do nothing is we're passed a null.
        if (null != item) {

            // Get the current selection.
            String cur = items.getSelectedItem();

            // Do nothing, if the request is for the selected item.
            if (!item.equals(cur)) {

                // Try to find the requested item.
                String[] contents = items.getItems();
                for (int loop = 0; loop < items.getItemCount(); loop++) {

                    // Did we find it?
                    if (contents[loop].equals(item)) {

                        // Select this position.
                        items.select(loop);

                        // Stop looking for the item.
                        break;

                    } // End found-requested-item

                } // Check the next item

            } // End new-selection

        } // End not-passed-null
    }

    /**
     * Show the dialog. Make sure that all selections have been applied. <p/>
     * Overrides <code>Component#setVisible()</code>.
     */
    public void setVisible(boolean show) {

        if (show) {
            // Make sure the "keep" button is set correctly.
            setPrevSelection(categories.getSelectedItem(), items
                    .getSelectedItem());

            // Make sure the "select" button is set correctly.
            itemStateChanged(new ItemEvent(items, 0, items.getSelectedItem(),
                    ItemEvent.SELECTED));

            // Now show the dialog.
        }
        super.setVisible(show);
    }

}
