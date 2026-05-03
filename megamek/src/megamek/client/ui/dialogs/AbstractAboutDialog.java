/*
 * Copyright (C) 2003, 2004, 2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (C) 2003-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import com.formdev.flatlaf.FlatClientProperties;
import megamek.client.ui.Messages;
import megamek.client.ui.util.UIUtil;

/**
 * A base "About..." dialog showing a name and version block ("MegaMek Version: 0.50.xx") and the license block as in
 * the license dialog.
 */
public abstract class AbstractAboutDialog {

    private static final String LICENSE_FORMAT = "<html><body width='%d'>%s</body></html>";
    private static final int BASE_WIDTH = 500;
    private final Window parent;

    protected AbstractAboutDialog(Window parent) {
        this.parent = parent;
    }

    private JComponent setupContent() {
        JPanel content = new JPanel(new GridBagLayout());
        var gbc = new GridBagConstraints();
        gbc.ipady = 30;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        content.setBorder(new EmptyBorder(40, 15, 0, 15));
        content.add(versionSection(), gbc);
        content.add(licenseSection(), gbc);
        return content;
    }

    protected final JComponent licenseSection() {
        JEditorPane licenseSection = new JEditorPane();
        licenseSection.setContentType("text/html");
        licenseSection.setEditable(false);
        licenseSection.setText(buildAboutHtml());
        licenseSection.addHyperlinkListener(UIUtil::handleHyperlink);
        licenseSection.setBorder(null);
        return licenseSection;
    }

    private JComponent versionSection() {
        JLabel project = new JLabel(currentProjectName());
        project.putClientProperty(FlatClientProperties.STYLE_CLASS, "h3");
        JLabel version = new JLabel(Messages.getString("about.version", currentVersion()));
        var panel = Box.createVerticalBox();
        panel.add(project);
        panel.add(Box.createVerticalStrut(8));
        panel.add(version);
        return panel;
    }

    /**
     * @return The current project's name. Since this is a name, there's no reason to i18n it.
     */
    protected abstract String currentProjectName();

    /**
     * @return The version as a String without decoration.
     */
    protected abstract String currentVersion();

    /**
     * Displays the dialog (modal).
     */
    public void show() {
        JOptionPane.showMessageDialog(parent,
              setupContent(),
              Messages.getString("about.title", currentProjectName()),
              JOptionPane.PLAIN_MESSAGE,
              null);
    }

    private String buildAboutHtml() {
        return LICENSE_FORMAT.formatted(UIUtil.scaleForGUI(BASE_WIDTH), LicensingDialog.buildLegalHtml());
    }
}
