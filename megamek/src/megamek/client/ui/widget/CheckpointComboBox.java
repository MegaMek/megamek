/*
 * Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.widget;

import java.awt.Dimension;
import java.io.Serial;
import javax.swing.JComboBox;

/**
 * Override of the basic {@link JComboBox} that adds the capacity to checkpoint its current value. Once the value is
 * checkpoint, the {@link #hasChanged} method will reflect if the current value equals the checkpoint value or not.
 *
 * @author Deric Page (deric dot page at usa dot net)
 * @since 3/21/14 8:34 AM
 */
public class CheckpointComboBox<E> extends JComboBox<E> {
    @Serial
    private static final long serialVersionUID = -5047466175280294296L;
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
    public CheckpointComboBox(E[] items) {
        super(items);
    }

    /**
     * Checkpoints the combobox's current state by saving the value of its current index.
     */
    public void checkpoint() {
        checkpointIndex = getSelectedIndex();
    }

    /**
     * @return TRUE if the {@link #checkpoint()} method has been called and the currently selected index does not match
     *       the checkpoint index.
     */
    public boolean hasChanged() {
        return (checkpointIndex != -1) && (checkpointIndex != getSelectedIndex());
    }

    /**
     * @return The current checkpoint index or -1 if the {@link #checkpoint()} method has not been called.
     */
    public int getCheckpointIndex() {
        return checkpointIndex;
    }

    /**
     * @return The {@link Object} stored at the checkpoint index or NULL if the {@link #checkpoint()} method has not
     *       been called.
     */
    public Object getCheckpointItem() {
        if (checkpointIndex == -1) {
            return null;
        }
        return getItemAt(checkpointIndex);
    }

    /**
     * Removes the current checkpoint by resetting the checkpoint index to -1.
     */
    public void removeCheckpoint() {
        checkpointIndex = -1;
    }

    @Override
    public Dimension getMaximumSize() {
        // Make this ComboBox not stretch vertically
        Dimension size = getPreferredSize();
        Dimension maxSize = super.getMaximumSize();
        return new Dimension(maxSize.width, size.height);
    }

}
