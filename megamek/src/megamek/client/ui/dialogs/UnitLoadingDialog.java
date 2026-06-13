/*
 * Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2003-2025 The MegaMek Team. All Rights Reserved.
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

/*
 * UnitLoadingDialog.java
 *  Created by Ryan McConnell on June 15, 2003
 */

package megamek.client.ui.dialogs;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.Serial;
import java.util.Objects;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.common.loaders.MekSummaryCache;

public class UnitLoadingDialog extends JDialog {
    @Serial
    private static final long serialVersionUID = -3454307876761238915L;
    private final JLabel lCacheCount = new JLabel();
    private final JLabel lFileCount = new JLabel();
    private final JLabel lZipCount = new JLabel();
    private final JProgressBar progressBar = new JProgressBar();
    private final MekSummaryCache mekSummaryCache;
    private MekSummaryCache.Listener mekSummaryCacheListener;

    // Determines how often to update the loading dialog.
    // Setting this too low causes noticeable loading delays.
    private static final long UPDATE_FREQUENCY = 50;

    private volatile boolean loadingDone = false;

    public UnitLoadingDialog(JFrame frame) {
        this(frame, MekSummaryCache.getInstance());
    }

    public UnitLoadingDialog(JFrame frame, MekSummaryCache mekSummaryCache) {
        this(frame, mekSummaryCache, Messages.getString("UnitLoadingDialog.LoadingUnits"), false);
    }

    public UnitLoadingDialog(JFrame frame, MekSummaryCache mekSummaryCache, String loadingMessage,
          boolean waitForUpcomingLoad) {
        super(frame, Messages.getString("UnitLoadingDialog.pleaseWait"));
        this.mekSummaryCache = Objects.requireNonNull(mekSummaryCache);

        getContentPane().setLayout(new GridBagLayout());
        JLabel lLoading = new JLabel(loadingMessage);
        getContentPane().add(lLoading, GBC.eol());

        progressBar.setIndeterminate(true);
        getContentPane().add(progressBar, GBC.eop().fill(GridBagConstraints.HORIZONTAL));

        JLabel lCacheText = new JLabel(Messages.getString("UnitLoadingDialog.fromCache"));
        getContentPane().add(lCacheText, GBC.std());
        getContentPane().add(lCacheCount, GBC.eol());

        JLabel lFileText = new JLabel(Messages.getString("UnitLoadingDialog.fromFiles"));
        getContentPane().add(lFileText, GBC.std());
        getContentPane().add(lFileCount, GBC.eol());

        JLabel lZipText = new JLabel(Messages.getString("UnitLoadingDialog.fromZips"));
        getContentPane().add(lZipText, GBC.std());
        getContentPane().add(lZipCount, GBC.eol());

        pack();
        setResizable(false);
        // move to middle of screen
        setLocationRelativeTo(frame);

        if (!waitForUpcomingLoad && mekSummaryCache.isInitialized()) {
            loadingDone = true;
            updateCounts();
            return;
        }

        startMonitoring();
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible && loadingDone) {
            return;
        }
        super.setVisible(visible);
    }

    @Override
    public void dispose() {
        loadingDone = true;
        unregisterListener();
        super.dispose();
    }

    private void startMonitoring() {
        updateCounts();

        mekSummaryCacheListener = () -> {
            loadingDone = true;
            unregisterListener();
            SwingUtilities.invokeLater(() -> setVisible(false));
        };
        mekSummaryCache.addListener(mekSummaryCacheListener);

        Runnable r = () -> {
            while (!loadingDone) {
                SwingUtilities.invokeLater(this::updateCounts);
                try {
                    Thread.sleep(UPDATE_FREQUENCY);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        };
        Thread t = new Thread(r, "Unit Loader Dialog");
        t.setDaemon(true);
        t.start();
    }

    private void unregisterListener() {
        if (mekSummaryCacheListener != null) {
            mekSummaryCache.removeListener(mekSummaryCacheListener);
            mekSummaryCacheListener = null;
        }
    }

    private void updateCounts() {
        lCacheCount.setText(String.valueOf(mekSummaryCache.getCacheCount()));
        lFileCount.setText(String.valueOf(mekSummaryCache.getFileCount()));
        lZipCount.setText(String.valueOf(mekSummaryCache.getZipCount()));
    }
}
