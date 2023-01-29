/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.dialogs.nagDialogs;

import megamek.MMConstants;
import megamek.MegaMek;
import megamek.client.ui.baseComponents.AbstractNagDialog;

import javax.swing.*;

public class BotReadmeNagDialog extends AbstractNagDialog {
    //region Constructors
    public BotReadmeNagDialog(final JFrame frame) {
        super(frame, "BotReadmeNagDialog", "BotReadmeNagDialog.title",
                "BotReadmeNagDialog.text", MMConstants.NAG_BOT_README);
    }
    //endregion Constructors

    @Override
    protected boolean checkNag() {
        return !MegaMek.getMMOptions().getNagDialogIgnore(getKey());
    }
}
