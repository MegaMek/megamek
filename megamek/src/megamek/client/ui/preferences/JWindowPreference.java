/*
 * Copyright (c) 2019-2021 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.preferences;

import megamek.codeUtilities.StringUtility;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.lang.ref.WeakReference;

/**
 * JWindowPreference monitors the size (width and height), location, and maximization state of a Window.
 * It sets the saved values when a dialog is loaded and changes them as they change.
 *
 * Call preferences.manage(new JWindowPreference(Window)) to use this preference, on a Window that
 * has called setName
 */
public class JWindowPreference extends PreferenceElement implements ComponentListener, WindowStateListener {
    //region Variable Declarations
    private final WeakReference<Window> weakReference;
    private int width;
    private int height;
    private int screenX;
    private int screenY;
    private boolean maximized;
    //endregion Variable Declarations

    //region Constructors
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
    //endregion Constructors

    //region Getters/Setters
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
    //endregion Getters/Setters

    //region PreferenceElement
    @Override
    protected String getValue() {
        return String.format("%d|%d|%d|%d|%s", getWidth(), getHeight(), getScreenX(), getScreenY(), isMaximized());
    }

    @Override
    protected void initialize(final String value) throws Exception {
        if (StringUtility.isNullOrBlank(value)) {
            LogManager.getLogger().error("Cannot create a JWindowPreference because of a null or blank input value");
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
            element.setLocation(getScreenX(), getScreenY());
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
    //endregion PreferenceElement

    //region ComponentListener
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
    //endregion ComponentListener

    //region WindowStateListener
    @Override
    public void windowStateChanged(final WindowEvent evt) {
        setMaximized((evt.getNewState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH);
    }
    //endregion WindowStateListener
}
