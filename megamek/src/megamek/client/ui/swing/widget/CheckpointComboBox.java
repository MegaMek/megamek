/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
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
package megamek.client.ui.swing.widget;

import javax.swing.*;

/**
 * Override of the basic {@link JComboBox} that adds the capacity to checkpoint its current value.  Once the value
 * is checkpointed, the {@link #hasChanged} method will reflect if the current value equals the checkpointed value
 * or not.
 *
 * @author Deric Page (deric dot page at usa dot net)
 * @version %Id%
 * @since 3/21/14 8:34 AM
 */
public class CheckpointComboBox<E> extends JComboBox {

    int checkpointIndex = -1;

    /**
     * Default constructor.
     */
    public CheckpointComboBox() {
    }

    /**
     * Constructor taking in a list of what items will be displayed in the combobox.
     *
     * @param items The items to be displayed in the combobox.
     */
    @SuppressWarnings("unchecked")
    public CheckpointComboBox(Object[] items) {
        super(items);
    }

    /**
     * Checkpoints the combobox's current state by saving the value of it's current index.
     */
    public void checkpoint() {
        checkpointIndex = getSelectedIndex();
    }

    /**
     * @return TRUE if the {@link #checkpoint()} method has been called and the currently selected index does not match
     *         the checkpointed index.
     */
    public boolean hasChanged() {
        return (checkpointIndex != -1) && (checkpointIndex != getSelectedIndex());
    }

    /**
     * @return The currently checkpointed index or -1 if the {@link #checkpoint()} method has not been called.
     */
    public int getCheckpointIndex() {
        return checkpointIndex;
    }

    /**
     * @return The {@link Object} stored at the checkpointed index or NULL if the {@link #checkpoint()} method has not
     *         been called.
     */
    public Object getCheckpointedItem() {
        if (checkpointIndex == -1) {
            return null;
        }
        return getItemAt(checkpointIndex);
    }

    /**
     * Removes the current checkpoint by resetting the checkpointed index to -1.
     */
    public void removeCheckpoint() {
        checkpointIndex = -1;
    }

}
