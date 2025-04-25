/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 *  This file is part of MegaMek.
 *
 *  MekHQ is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MekHQ is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package megamek.client.ui.dialogs;

import megamek.client.ui.dialogs.helpDialogs.AbstractHelpDialog;
import megamek.common.internationalization.I18n;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class AutoResolveSimulationLogDialog extends AbstractHelpDialog {

    public AutoResolveSimulationLogDialog(final JFrame frame, File logFile) {
        super(frame, I18n.getText("AutoResolveSimulationLogDialog.title"),
            logFile.getAbsolutePath());

        setMinimumSize(new Dimension(800, 400));
        setModalExclusionType(ModalExclusionType.TOOLKIT_EXCLUDE);

    }

}
