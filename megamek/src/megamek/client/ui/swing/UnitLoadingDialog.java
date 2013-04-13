/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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

/*
 * UnitLoadingDialog.java
 *  Created by Ryan McConnell on June 15, 2003
 */

package megamek.client.ui.swing;

import java.awt.GridBagLayout;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;

import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.common.MechSummaryCache;

public class UnitLoadingDialog extends JDialog {

    /**
     *
     */
    private static final long serialVersionUID = -3454307876761238915L;
    private JLabel lLoading = new JLabel(Messages
            .getString("UnitLoadingDialog.LoadingUnits")); //$NON-NLS-1$
    private JLabel lCacheText = new JLabel(Messages
            .getString("UnitLoadingDialog.fromCache")); //$NON-NLS-1$
    private JLabel lCacheCount = new JLabel();
    private JLabel lFileText = new JLabel(Messages
            .getString("UnitLoadingDialog.fromFiles")); //$NON-NLS-1$
    private JLabel lFileCount = new JLabel();
    private JLabel lZipText = new JLabel(Messages
            .getString("UnitLoadingDialog.fromZips")); //$NON-NLS-1$
    private JLabel lZipCount = new JLabel();

    // Determines how often to update the loading dialog.
    // Setting this too low causes noticeable loading delays.
    private static final long UPDATE_FREQUENCY = 50;

    public UnitLoadingDialog(JFrame frame) {
        super(frame, Messages.getString("UnitLoadingDialog.pleaseWait")); //$NON-NLS-1$

        getContentPane().setLayout(new GridBagLayout());
        getContentPane().add(lLoading, GBC.eol());

        getContentPane().add(lCacheText, GBC.std());
        getContentPane().add(lCacheCount, GBC.eol());

        getContentPane().add(lFileText, GBC.std());
        getContentPane().add(lFileCount, GBC.eol());

        getContentPane().add(lZipText, GBC.std());
        getContentPane().add(lZipCount, GBC.eol());

        setSize(250, 130);
        // move to middle of screen
        setLocationRelativeTo(frame);

        Runnable r = new Runnable() {
            public void run() {
                while (!MechSummaryCache.getInstance().isInitialized()) {
                    updateCounts();
                    try {
                        Thread.sleep(UPDATE_FREQUENCY);
                    } catch (InterruptedException e) {
                        // not supposed to come here
                    }
                }
            }
        };
        Thread t = new Thread(r, "Unit Loader Dialog"); //$NON-NLS-1$
        t.start();
    }

    void updateCounts() {
        lCacheCount.setText(String.valueOf(MechSummaryCache.getInstance()
                .getCacheCount()));
        lFileCount.setText(String.valueOf(MechSummaryCache.getInstance()
                .getFileCount()));
        lZipCount.setText(String.valueOf(MechSummaryCache.getInstance()
                .getZipCount()));
    }
}
