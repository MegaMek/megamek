/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
 */

package megamek.client.ui.swing;

import java.awt.GridBagLayout;
import java.io.Serial;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;

import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.common.MekSummaryCache;

public class UnitLoadingDialog extends JDialog {
    @Serial
    private static final long serialVersionUID = -3454307876761238915L;
    private final JLabel lCacheCount = new JLabel();
    private final JLabel lFileCount = new JLabel();
    private final JLabel lZipCount = new JLabel();

    // Determines how often to update the loading dialog.
    // Setting this too low causes noticeable loading delays.
    private static final long UPDATE_FREQUENCY = 50;

    boolean loadingDone = false;

    public UnitLoadingDialog(JFrame frame) {
        super(frame, Messages.getString("UnitLoadingDialog.pleaseWait"));

        getContentPane().setLayout(new GridBagLayout());
        JLabel lLoading = new JLabel(Messages.getString("UnitLoadingDialog.LoadingUnits"));
        getContentPane().add(lLoading, GBC.eol());

        JLabel lCacheText = new JLabel(Messages.getString("UnitLoadingDialog.fromCache"));
        getContentPane().add(lCacheText, GBC.std());
        getContentPane().add(lCacheCount, GBC.eol());

        JLabel lFileText = new JLabel(Messages.getString("UnitLoadingDialog.fromFiles"));
        getContentPane().add(lFileText, GBC.std());
        getContentPane().add(lFileCount, GBC.eol());

        JLabel lZipText = new JLabel(Messages.getString("UnitLoadingDialog.fromZips"));
        getContentPane().add(lZipText, GBC.std());
        getContentPane().add(lZipCount, GBC.eol());

        setSize(250, 130);
        // move to the middle of the screen
        setLocationRelativeTo(frame);

        Runnable r = () -> {
            while (!loadingDone && !MekSummaryCache.getInstance().isInitialized()) {
                updateCounts();
                try {
                    wait(UPDATE_FREQUENCY);
                } catch (InterruptedException ignored) {
                    // not supposed to come here
                }
            }
        };

        MekSummaryCache.Listener mekSummaryCacheListener = () -> {
            loadingDone = true;
            setVisible(false);
        };

        MekSummaryCache.getInstance().addListener(mekSummaryCacheListener);
        Thread t = new Thread(r, "Unit Loader Dialog");
        t.start();
    }

    void updateCounts() {
        lCacheCount.setText(String.valueOf(MekSummaryCache.getInstance().getCacheCount()));
        lFileCount.setText(String.valueOf(MekSummaryCache.getInstance().getFileCount()));
        lZipCount.setText(String.valueOf(MekSummaryCache.getInstance().getZipCount()));
    }
}
