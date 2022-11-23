/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.helpDialogs;

import megamek.client.ui.Messages;
import megamek.client.ui.baseComponents.AbstractDialog;
import megamek.client.ui.swing.util.UIUtil;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * This class ensures that every Help dialog in MegaMek has an identical look-and-feel.
 */
public abstract class AbstractHelpDialog extends AbstractDialog {
    //region Variable Declarations
    private String helpFilePath;

    //endregion Variable Declarations

    //region Constructors
    protected AbstractHelpDialog(final JFrame frame, final String name, final String helpFilePath) {
        super(frame, name, "AbstractHelpDialog.helpFile");
        setHelpFilePath(helpFilePath);
        initialize();
    }
    //endregion Constructors

    //region Getters/Setters
    public String getHelpFilePath() {
        return helpFilePath;
    }

    public void setHelpFilePath(final String helpFilePath) {
        this.helpFilePath = helpFilePath;
    }
    //endregion Getters/Setters

    @Override
    protected Container createCenterPane() {
        JEditorPane pane = new JEditorPane();
        pane.setName("helpPane");
        pane.setEditable(false);

        final File helpFile = new File(getHelpFilePath());

        // Get the help content file if possible
        try {
            setTitle(getTitle() + helpFile.getName());
            pane.setPage(helpFile.toURI().toURL());
        } catch (Exception e) {
            setTitle(Messages.getString("AbstractHelpDialog.noHelp.title"));
            pane.setText(Messages.getString("AbstractHelpDialog.errorReading") + e.getMessage());
            LogManager.getLogger().error("", e);
        }

        return new JScrollPane(pane);
    }

    @Override
    protected void finalizeInitialization() throws Exception {
        super.finalizeInitialization();
        adaptToGUIScale();
    }

    private void adaptToGUIScale() {
        UIUtil.adjustDialog(this, UIUtil.FONT_SCALE1);
    }
}
