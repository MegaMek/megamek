/*  
* MegaMek - Copyright (C) 2013-2020 - The MegaMek Team  
*  
* This program is free software; you can redistribute it and/or modify it under  
* the terms of the GNU General Public License as published by the Free Software  
* Foundation; either version 2 of the License, or (at your option) any later  
* version.  
*  
* This program is distributed in the hope that it will be useful, but WITHOUT  
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS  
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more  
* details.  
*/  
package megamek.client.ui.swing;

import megamek.client.ui.swing.util.UIUtil;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.net.URL;

/**
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since 11/4/13 9:20 PM
 */
public class HelpDialog extends JDialog {

    private static final long serialVersionUID = 1442198850518387690L;
    
    private static final int WIDTH = 600;
    private static final int HEIGHT = 400;

    private URL helpUrl;

    public HelpDialog(String title, URL helpURL) {
        setTitle(title);
        getContentPane().setLayout(new BorderLayout());
        this.helpUrl = helpURL;

        JEditorPane mainView = new JEditorPane();
        mainView.setEditable(false);
        try {
            mainView.setPage(helpUrl);
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            JOptionPane.showMessageDialog(this, ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
        }

        //Listen for the user clicking on hyperlinks.
        mainView.addHyperlinkListener(e -> {
            try {
                if (HyperlinkEvent.EventType.ACTIVATED == e.getEventType()) {
                    mainView.setPage(e.getURL());
                }
            } catch (Exception ex) {
                LogManager.getLogger().error("", ex);
                JOptionPane.showMessageDialog(this, ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
            }
        });

        add(new JScrollPane(mainView));
        setModalExclusionType(ModalExclusionType.TOOLKIT_EXCLUDE);

        adaptToGUIScale();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
    }

    private void adaptToGUIScale() {
        UIUtil.adjustDialog(this, UIUtil.FONT_SCALE1);
    }
}
