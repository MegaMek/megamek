/*
 *  TinyXMLTest: a tiny application which outputs the structure of XML files
 *  Copyright (C) 1999  Tom Gibara <tom@srac.org>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package megamek.test;

import gd.xml.ParseException;
import gd.xml.XMLParser;
import gd.xml.XMLResponder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;

public class TinyXMLTest implements XMLResponder {

    private String filename;
    private String prefix;

    public TinyXMLTest(String type, String fname) {
        filename = fname;
        prefix = "  ";
        try {
            XMLParser xp = new XMLParser();
            if (type.equals("xml"))
                xp.parseXML(this);
            if (type.equals("dtd"))
                xp.parseDTD(this);
        } catch (ParseException e) {
            System.out.println(e.toString());
        }
    }

    /*
     * XML RESPONDER METHODS FOLLOW. THIS IS A REALLY WEAK SATISIFACTION OF THE
     * INTERFACE. IT JUST DUMPS EVERYTHING TO THE CONSOLE.
     */

    /* DTD METHODS */

    public void recordNotationDeclaration(String name, String pubID,
            String sysID) throws ParseException {
        System.out.print(prefix + "!NOTATION: " + name);
        if (pubID != null)
            System.out.print("  pubID = " + pubID);
        if (sysID != null)
            System.out.print("  sysID = " + sysID);
        System.out.println("");
    }

    public void recordEntityDeclaration(String name, String value,
            String pubID, String sysID, String notation) throws ParseException {
        System.out.print(prefix + "!ENTITY: " + name);
        if (value != null)
            System.out.print("  value = " + value);
        if (pubID != null)
            System.out.print("  pubID = " + pubID);
        if (sysID != null)
            System.out.print("  sysID = " + sysID);
        if (notation != null)
            System.out.print("  notation = " + notation);
        System.out.println("");
    }

    public void recordElementDeclaration(String name, String content)
            throws ParseException {
        System.out.print(prefix + "!ELEMENT: " + name);
        System.out.println("  content = " + content);
    }

    public void recordAttlistDeclaration(String element, String attr,
            boolean notation, String type, String defmod, String def)
            throws ParseException {
        System.out.print(prefix + "!ATTLIST: " + element);
        System.out.print("  attr = " + attr);
        System.out.print("  type = " + ((notation) ? "NOTATIONS " : "") + type);
        System.out.print("  def. modifier = " + defmod);
        System.out.println((def == null) ? "" : "  def = " + notation);
    }

    public void recordDoctypeDeclaration(String name, String pubID, String sysID)
            throws ParseException {
        System.out.print(prefix + "!DOCTYPE: " + name);
        if (pubID != null)
            System.out.print("  pubID = " + pubID);
        if (sysID != null)
            System.out.print("  sysID = " + sysID);
        System.out.println("");
        prefix = "";
    }

    /* DOC METHDODS */

    public void recordDocStart() {
    }

    public void recordDocEnd() {
        System.out.println("");
        System.out.println("Parsing finished without error");
    }

    @SuppressWarnings("unchecked")
    public void recordElementStart(String name, Hashtable attr)
            throws ParseException {
        System.out.println(prefix + "Element: " + name);
        if (attr != null) {
            Enumeration<?> e = attr.keys();
            System.out.print(prefix);
            String conj = "";
            while (e.hasMoreElements()) {
                Object k = e.nextElement();
                System.out.print(conj + k + " = " + attr.get(k));
                conj = ", ";
            }
            System.out.println("");
        }
        prefix = prefix + "  ";
    }

    public void recordElementEnd(String name) throws ParseException {
        prefix = prefix.substring(2);
    }

    public void recordPI(String name, String pValue) {
        System.out.println(prefix + "*" + name + " PI: " + pValue);
    }

    public void recordCharData(String charData) {
        System.out.println(prefix + charData);
    }

    public void recordComment(String comment) {
        System.out.println(prefix + "*Comment: " + comment);
    }

    /* INPUT METHODS */

    public InputStream getDocumentStream() throws ParseException {
        try {
            return new FileInputStream(filename);
        } catch (FileNotFoundException e) {
            throw new ParseException("could not find the specified file");
        }
    }

    public InputStream resolveExternalEntity(String name, String pubID,
            String sysID) throws ParseException {
        if (sysID != null) {
            File f = new File((new File(filename)).getParent(), sysID);
            try {
                return new FileInputStream(f);
            } catch (FileNotFoundException e) {
                throw new ParseException("file not found (" + f + ")");
            }
        }
        return null;
    }

    public InputStream resolveDTDEntity(String name, String pubID, String sysID)
            throws ParseException {
        return resolveExternalEntity(name, pubID, sysID);
    }

}
