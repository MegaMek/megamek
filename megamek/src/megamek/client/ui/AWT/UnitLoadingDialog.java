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

package megamek.client;

import java.awt.*;

public class UnitLoadingDialog extends Dialog {

    private Label lLoading = new Label("Loading units...");
    private Label lSpacer = new Label();
    private Label lCacheText = new Label("  ...from cache: ");
    private Label lCacheCount = new Label();
    private Label lFileText = new Label("  ...from files: ");
    private Label lFileCount = new Label();
    private Label lZipText = new Label("  ...from zips: ");
    private Label lZipCount = new Label();

    private int cacheCount;
    private int fileCount;
    private int zipCount;

    // Determines how often to update the loading dialog.
    // Setting this too low causes noticeable loading delays.
    private static final int UPDATE_FREQUENCY = 32;

    public UnitLoadingDialog(Frame frame) {
        super(frame,"Please wait...");

        cacheCount = 0;
        fileCount = 0;
        zipCount = 0;        

        setLayout(new GridLayout(4,2));
        add(lLoading);
        add(lSpacer);

        add(lCacheText);
        add(lCacheCount);

        add(lFileText);
        add(lFileCount);

        add(lZipText);
        add(lZipCount);

        setSize(250,130);
		// move to middle of screen
		Dimension screenSize = frame.getToolkit().getScreenSize();
        setLocation(
            screenSize.width / 2 - getSize().width / 2,
            screenSize.height / 2 - getSize().height / 2);
    }
    
    public void incrementCacheCount() {
        cacheCount++;
        if ( (cacheCount % UPDATE_FREQUENCY) == 0)
            lCacheCount.setText(String.valueOf(cacheCount));
    }

    public void incrementFileCount() {
        fileCount++;
        if ( (fileCount % UPDATE_FREQUENCY) == 0)
            lFileCount.setText(String.valueOf(fileCount));
    }

    public void incrementZipCount() {
        zipCount++;
        if ( (zipCount % UPDATE_FREQUENCY) == 0)
            lZipCount.setText(String.valueOf(zipCount));
    }
}
