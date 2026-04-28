/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui;

import java.awt.GridBagConstraints;
import java.awt.Insets;

/**
 * A GridBagConstraints subclass for setting grid-like layouts where, usually, a left-side label is followed by one or
 * more chooser components. Using this class means using a persistent instance of GridBagConstraints, so that e.g.
 * insets or ipad values need to be set only once. Also, it makes use of call chaining (methods return {@code this}).
 * Note that this class is written with the intention of leaving gridx and gridy entirely at the default (RELATIVE). Not
 * doing that is likely to disrupt the methods; if specific values need to be used, it is likely better to use a basic
 * GridBagConstraints instead of GBC2.
 */
public class GBC2 extends GridBagConstraints {

    private final Insets labelInsets;
    private final Insets otherInsets;

    public GBC2() {
        this(new Insets(0, 0, 0, 0), new Insets(0, 0, 0, 0));
    }

    /**
     * Creates a new GBC2 which uses the given otherInsets for most of its components; the given labelInsets is only
     * used when the forLabel() method is called.
     *
     * @param labelInsets Insets to use when adding a component with forLabel()
     * @param otherInsets Insets to use otherwise
     */
    public GBC2(Insets labelInsets, Insets otherInsets) {
        this.labelInsets = labelInsets;
        this.otherInsets = otherInsets;
    }

    /**
     * Use this for a left-side label. It sets the anchor to east (right-alignment) and fill to NONE, so the JLabel does
     * not need to use any specific alignment setting. This can also be used for empty labels to skip the label column.
     * It can be directly used by calling panel.add(label, gbc.forLabel());
     *
     * @return This GridBagConstraints
     */
    public GBC2 forLabel() {
        gridwidth = 1;
        fill = NONE;
        anchor = EAST;
        insets = labelInsets;
        return this;
    }

    /**
     * Use this for a single-column component other than the left-side label. It sets the anchor to west, fill to
     * HORIZONTAL and the gridwidth to 1. It can be directly used by calling panel.add(component, gbc.oneColumn());
     *
     * @return This GridBagConstraints
     */
    public GBC2 oneColumn() {
        return eol().singleColumn();
    }

    /**
     * Use this for any component that ends a given row. It sets the anchor to west, fill to HORIZONTAL and the
     * gridwidth to REMAINDER. It can be directly used by calling panel.add(component, gbc.eol());
     *
     * @return This GridBagConstraints
     */
    public GBC2 eol() {
        insets = otherInsets;
        gridwidth = REMAINDER;
        fill = HORIZONTAL;
        anchor = WEST;
        return this;
    }

    /**
     * Use this for any component that fills an entire row, like a section title. It sets the anchor to CENTER, fill to
     * HORIZONTAL and the gridwidth to REMAINDER. It can be directly used by calling panel.add(component,
     * gbc.fullLine());
     *
     * @return This GridBagConstraints
     */
    public GBC2 fullLine() {
        insets = otherInsets;
        gridwidth = REMAINDER;
        fill = HORIZONTAL;
        anchor = CENTER;
        return this;
    }

    /**
     * Use this for any component that fills an entire row but still uses the label insets, like a panel with
     * subcomponents. It sets the anchor to CENTER, fill to HORIZONTAL and the gridwidth to REMAINDER. It can be
     * directly used by calling panel.add(component, gbc.fullLineWithLabelInsets());
     *
     * @return This GridBagConstraints
     */
    public GBC2 fullLineWithLabelInsets() {
        insets = labelInsets;
        gridwidth = REMAINDER;
        fill = HORIZONTAL;
        anchor = CENTER;
        return this;
    }

    /**
     * change the insets of this <code>GridBagConstraints</code>
     *
     * @return <code>this</code>
     *
     * @see GridBagConstraints#insets
     */
    public GBC2 insets(int top, int left, int bottom, int right) {
        insets = new Insets(top, left, bottom, right);
        return this;
    }

    private GBC2 singleColumn() {
        gridwidth = 1;
        return this;
    }

    @Override
    public Object clone() {
        return super.clone(); // per Copilot review and "Effective Java" by J. Bloch
    }
}
