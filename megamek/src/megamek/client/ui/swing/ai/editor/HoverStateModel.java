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

import java.util.ArrayList;
import java.util.List;

public class HoverStateModel {
    private double hoveringRelativeXPosition = -1; // -1 means no hovering
    private final List<Runnable> listeners = new ArrayList<>();

    public double getHoveringRelativeXPosition() {
        return hoveringRelativeXPosition;
    }

    public void setHoveringRelativeXPosition(double position) {
        if (this.hoveringRelativeXPosition != position) {
            this.hoveringRelativeXPosition = position;
            notifyListeners();
        }
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }
}
