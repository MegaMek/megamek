/*
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
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.common;

/**
 * This class is used to store information about an individual sourcebook such as TRO: 3039.
 */
@SuppressWarnings("unused") // Methods used by Jackson during deserialization
public class SourceBook {

    private String title;
    private int id;
    private String sku;
    private String abbrev;
    private String image;
    private String url;
    private String ispublished;
    private String description;
    private String mul_url;

    /**
     * @return The full title of the book, such as "Technical Readout: 3039"
     */
    public String getTitle() {
        return title;
    }

    private void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return The mul id of the book.
     */
    public int getId() {
        return id;
    }

    private void setId(int id) {
        this.id = id;
    }

    /**
     * @return The product code of the book, such as "CAT35121c"
     */
    public String getSku() {
        return sku;
    }

    private void setSku(String sku) {
        this.sku = sku;
    }

    /**
     * @return The abbreviation of the book, such as "TR: 3039"
     */
    public String getAbbrev() {
        return abbrev;
    }

    private void setAbbrev(String abbrev) {
        this.abbrev = abbrev;
    }

    // image URLs, currently bad quality
    private void setImage(String image) {
        this.image = image;
    }

    // shop page URls
    private void setUrl(String url) {
        this.url = url;
    }

    // some books are apparently unpublished or were at some point
    private void setIspublished(String ispublished) {
        this.ispublished = ispublished;
    }

    /**
     * @return A description of the book with HTML tags.
     */
    public String getDescription() {
        return description;
    }

    private void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return A complete Master Unit List URL for this book
     */
    public String getMul_url() {
        return mul_url;
    }

    private void setMul_url(String mul_url) {
        this.mul_url = mul_url;
    }
}
