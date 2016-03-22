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
package megamek.common.options;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * An XML adapter that handles converting an Object to an XML string and back.
 * This is mainly used when marshalling/unmarshalling a BasicOption.
 *
 * @author nderwin
 * @see BasicOption
 */
public class ToStringAdapter extends XmlAdapter<String, Object> {

    public ToStringAdapter() {
    }

    @Override
    public String marshal(Object v) throws Exception {
        return v.toString();
    }

    @Override
    public Object unmarshal(String v) throws Exception {
        return v;
    }

}
