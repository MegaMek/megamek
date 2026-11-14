/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.preferences;

import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.lang.ref.WeakReference;
import javax.swing.JFrame;

import megamek.codeUtilities.StringUtility;
import megamek.logging.MMLogger;

/**
 * JWindowPreference monitors the size (width and height), location, and maximization state of a Window. It sets the
 * saved values when a dialog is loaded and changes them as they change.
 * <p>
 * Call preferences.manage(new JWindowPreference(Window)) to use this preference, on a Window that has called setName
 */
public class JWindowPreference extends PreferenceElement implements ComponentListener, WindowStateListener {
    private final static MMLogger logger = MMLogger.create(JWindowPreference.class);

    private static final int TITLE_BAR_HEIGHT = 30;
    private static final int MIN_VISIBLE_WIDTH = 100;
    private static final int MIN_VISIBLE_HEIGHT = 20;

    // region Variable Declarations
    private final WeakReference<Window> weakReference;
    private int width;
    private int height;
    private int screenX;
    private int screenY;
    private boolean maximized;
    // endregion Variable Declarations

    // region Constructors
    public JWindowPreference(final Window window) throws Exception {
        super(window.getName());

        setWidth(window.getWidth());
        setHeight(window.getHeight());
        if (window.isVisible()) {
            setScreenX(window.getLocationOnScreen().x);
            setScreenY(window.getLocationOnScreen().y);
        }
        setMaximized((window instanceof JFrame)
              && ((((JFrame) window).getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH));

        weakReference = new WeakReference<>(window);
        window.addComponentListener(this);
        window.addWindowStateListener(this);
    }
    // endregion Constructors

    // region Getters/Setters
    public WeakReference<Window> getWeakReference() {
        return weakReference;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(final int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(final int height) {
        this.height = height;
    }

    public int getScreenX() {
        return screenX;
    }

    public void setScreenX(final int screenX) {
        this.screenX = screenX;
    }

    public int getScreenY() {
        return screenY;
    }

    public void setScreenY(final int screenY) {
        this.screenY = screenY;
    }

    public boolean isMaximized() {
        return maximized;
    }

    public void setMaximized(final boolean maximized) {
        this.maximized = maximized;
    }
    // endregion Getters/Setters

    // region PreferenceElement
    @Override
    protected String getValue() {
        return String.format("%d|%d|%d|%d|%s", getWidth(), getHeight(), getScreenX(), getScreenY(), isMaximized());
    }

    @Override
    protected void initialize(final String value) throws Exception {
        if (StringUtility.isNullOrBlank(value)) {
            logger.error("Cannot create a JWindowPreference because of a null or blank input value");
            throw new Exception();
        }

        final Window element = getWeakReference().get();
        if (element != null) {
            final String[] parts = value.split("\\|", -1);

            setWidth(Integer.parseInt(parts[0]));
            setHeight(Integer.parseInt(parts[1]));
            setScreenX(Integer.parseInt(parts[2]));
            setScreenY(Integer.parseInt(parts[3]));
            setMaximized(Boolean.parseBoolean(parts[4]));

            element.setSize(getWidth(), getHeight());
            if (isLocationOnScreen(getScreenX(), getScreenY(), getWidth())) {
                element.setLocation(getScreenX(), getScreenY());
            } else {
                // The saved location is on a monitor that is no longer connected (or the resolution changed). Keep
                // whatever location the window already has, so it cannot open somewhere the user cannot reach.
                logger.warn("Saved location {},{} for window {} is not on any screen; ignoring it",
                      getScreenX(), getScreenY(), element.getName());
                setScreenX(element.getLocation().x);
                setScreenY(element.getLocation().y);
            }
            if (isMaximized()) {
                if (element instanceof JFrame) {
                    ((JFrame) element).setExtendedState(((JFrame) element).getExtendedState() | Frame.MAXIMIZED_BOTH);
                }
            }
        }
    }

    @Override
    protected void dispose() {
        final Window element = getWeakReference().get();
        if (element != null) {
            element.removeComponentListener(this);
            element.removeWindowStateListener(this);
            getWeakReference().clear();
        }
    }

    /**
     * Checks whether a window placed at the given location would have enough of its title bar on a currently connected
     * screen for the user to be able to see and drag it.
     *
     * @param x           the saved X coordinate of the window
     * @param y           the saved Y coordinate of the window
     * @param windowWidth the width of the window
     *
     * @return {@code true} if the location is usable, or if screens cannot be queried (headless)
     */
    private static boolean isLocationOnScreen(final int x, final int y, final int windowWidth) {
        if (GraphicsEnvironment.isHeadless()) {
            return true;
        }

        final Rectangle titleBar = new Rectangle(x, y, windowWidth, TITLE_BAR_HEIGHT);
        final int requiredWidth = Math.clamp(windowWidth, 1, MIN_VISIBLE_WIDTH);
        for (final GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            final Rectangle overlap = device.getDefaultConfiguration().getBounds().intersection(titleBar);
            // intersection() yields a negative width/height when there is no overlap, so these checks also fail then
            if ((overlap.width >= requiredWidth) && (overlap.height >= MIN_VISIBLE_HEIGHT)) {
                return true;
            }
        }
        return false;
    }
    // endregion PreferenceElement

    // region ComponentListener
    @Override
    public void componentResized(final ComponentEvent evt) {
        setWidth(evt.getComponent().getWidth());
        setHeight(evt.getComponent().getHeight());
    }

    @Override
    public void componentMoved(final ComponentEvent evt) {
        if (evt.getComponent().isVisible()) {
            setScreenX(evt.getComponent().getLocationOnScreen().x);
            setScreenY(evt.getComponent().getLocationOnScreen().y);
        }
    }

    @Override
    public void componentShown(final ComponentEvent evt) {

    }

    @Override
    public void componentHidden(final ComponentEvent evt) {

    }
    // endregion ComponentListener

    // region WindowStateListener
    @Override
    public void windowStateChanged(final WindowEvent evt) {
        setMaximized((evt.getNewState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH);
    }
    // endregion WindowStateListener
}
