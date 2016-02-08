/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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

package megamek.common;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import megamek.client.ui.swing.util.PlayerColors;

/**
 * This class defines a single server report. It holds information such as the
 * report ID, who the report is about, who should see the report, and some
 * formatting information.
 * <p>
 * Typically, the report will be created by the relevant section in the
 * <code>Server</code>, and added to the phase report vector. The actual text
 * of the report must also be added to the <i>report-messages.properties</i>
 * file.
 * <p>
 * Example:
 * <p>
 * <code>Report r = new Report(3455);\n
 * r.subject = entity.getId();\n
 * r.indent();\n
 * r.addDesc(entity);\n
 * r.add(6);\n
 * r.choose(true);\n
 * vPhaseReport.addElement(r);</code>
 * <p>
 * Then the following line would be added to <i>report-messages.properties</i>:
 * <p>
 * 3455::&lt;data&gt; (&lt;data&gt;) does &lt;data&gt; damage to the
 * &lt;msg:3456,3457&gt;.\n
 * 3456::tank\n
 * 3457::building
 * <p>
 * When the client parses the report, it will fill in the &lt;data&gt; tags with
 * the values that were given to the <code>add</code> methods called on the
 * report object.
 * <p>
 * The example above might produce a report such as this when the
 * <code>getText</code> method was called:
 * <p> " Crusader (Bob) does 6 damage to the tank."
 *
 * @author Ryan McConnell (oscarmm)
 * @version $Revision$
 * @since 0.30
 */
public class Report implements Serializable {
    /*
     * Note: some fields are marked transient because they are only used by the
     * server (or only the client). This shaves a few bytes off the packet size,
     * helping the dial-up people :)
     */

    private static final long serialVersionUID = -5586008091586682078L;
        
    private static final int MESSAGE_NONE = -1;
    
    /** Report Type: visible to all players. */
    public static final int PUBLIC = 0;
    
    /**
     * Report Type: visible to all players, but all data marked for obscuration
     * remains hidden. Note: Not used at this time, since all reports are
     * considered <code>obscured</code> unless explicitly marked
     * <code>public</code>.
     */
    public static final int OBSCURED = 1;
    
    /**
     * Report is only visible to those players who can see the subject. Note:
     * Not used at this time, since all reports are considered
     * <code>obscured</code> unless explicitly marked <code>public</code>.
     */
    public static final int HIDDEN = 2;
    
    /** Testing only - remove me later. */
    public static final int TESTING = 3;
    
    /**
     * Messages which should be sent only to the player indicated by "player"
     */
    public static final int PLAYER = 4;

    /**
     * The string that appears in the report to obscure certain information.
     */
    public static final String OBSCURED_STRING = "????";

    /** Number of spaces to use per indentation level. */
    private static final int DEFAULT_INDENTATION = 4;
    
    /** Required - associates this object with its text. */
    public int messageId = Report.MESSAGE_NONE;
    
    /** The number of spaces this report should be indented. */
    private int indentation = 0;

    /**
     * The number of newlines to add at the end of this report. Defaults to one.
     */
    public int newlines = 1;

    /** The data values to fill in the report with. */
    private Vector<String> tagData = new Vector<String>();

    /** How to translate the tagData or not at all. */
    private String tagTranslate = null;

    /**
     * How this report is handled when double-blind play is in effect. See
     * constants below for more details.
     */
    public transient int type = Report.HIDDEN;

    /**
     * The entity this report concerns, if applicable. If this is left blank,
     * then the report will be considered <code>public</code>.
     */
    public transient int subject = Entity.NONE;
    
    /**
     * The player this report concerns, if applicable. This should be filled in
     * if this report is not public and still does not belong to a specific
     * visible entity
     */
    public transient int player = IPlayer.PLAYER_NONE;

    /**
     * This hash table will store the tagData Vector indexes that are supposed
     * to be obscured before sending to clients. This only applies when the
     * report type is "obscured".
     */
    private Hashtable<Integer, Boolean> obscuredIndexes = 
            new Hashtable<Integer, Boolean>();

    /**
     * Vector to store the player names of those who received an obscured
     * version of this report. Used to reconstruct individual client's reports
     * from the master copy stored by the server.
     */
    private Vector<String> obscuredRecipients = new Vector<String>();

    /** Keep track of what data we have already substituted for tags. */
    private transient int tagCounter = 0;

    /**
     * Default constructor, note that using this means the
     * <code>messageId</code> field must be explicitly set.
     */
    public Report() {
    }

    /**
     * Create a new report associated with the given report text.
     *
     * @param id the int value of the report from <i>report-messages.properties
     * </i>
     */
    public Report(int id) {
        messageId = id;
    }

    /**
     * Create a new report associated with the given report text and having the
     * given type.
     *
     * @param id the int value of the report from <i>report-messages.properties
     *  </i>
     * @param type the constant specifying the visibility of the report (PUBLIC,
     *            OBSCURED, or HIDDEN)
     */
    public Report(int id, int type) {
        messageId = id;
        this.type = type;
    }

    /**
     * Create a new report which is an exact copy of the given report.
     *
     * @param r the report to be copied
     */
    @SuppressWarnings("unchecked")
    public Report(Report r) {
        messageId = r.messageId;
        indentation = r.indentation;
        newlines = r.newlines;
        tagData = (Vector<String>) r.tagData.clone();
        tagTranslate = r.tagTranslate;
        type = r.type;
        subject = r.subject;
        obscuredIndexes = (Hashtable<Integer, Boolean>) r.obscuredIndexes
                .clone();
        obscuredRecipients = (Vector<String>) r.obscuredRecipients.clone();
        tagCounter = r.tagCounter;
    }

    /**
     * Add the given int to the list of data that will be substituted for the
     * &lt;data&gt; tags in the report. The order in which items are added must
     * match the order of the tags in the report text.
     *
     * @param data the int to be substituted
     */
    public void add(int data) {
        add(data, true);
    }

    /**
     * Add the given int to the list of data that will be substituted for the
     * &lt;data&gt; tags in the report, and mark it as double-blind sensitive
     * information if <code>obscure</code> is true. The order in which items
     * are added must match the order of the tags in the report text.
     *
     * @param data the int to be substituted
     * @param obscure boolean indicating whether the data is double-blind
     *            sensitive
     */
    public void add(int data, boolean obscure) {
        if (obscure) {
            obscuredIndexes.put(new Integer(tagData.size()),
                    new Boolean(true));
        }
        tagData.addElement(String.valueOf(data));
    }

    /**
     * Add the given String to the list of data that will be substituted for the
     * &lt;data&gt; tags in the report. The order in which items are added must
     * match the order of the tags in the report text.
     *
     * @param data the String to be substituted
     */
    public void add(String data) {
        add(data, true);
        tagTranslate = null;
    }

    /**
     * Add the given string to the list of data that will be substituted for the
     * &lt;data&gt; tags in the report. The order in which items are added must
     * match the order of the tags in the report text. The second string
     * argument sets the translation flag to the string value.
     *
     * @param data the String to be substituted
     * @param translate the common Resource Bundle to be used for translation
     */
    public void add(String data, String translate) {
        add(data, true);
        tagTranslate = translate;
    }

    /**
     * Add the given String to the list of data that will be substituted for the
     * &lt;data&gt; tags in the report, and mark it as double-blind sensitive
     * information if <code>obscure</code> is true. The order in which items
     * are added must match the order of the tags in the report text.
     *
     * @param data the String to be substituted
     * @param obscure boolean indicating whether the data is double-blind
     *            sensitive
     */
    public void add(String data, boolean obscure) {
        if (obscure) {
            obscuredIndexes.put(new Integer(tagData.size()),
                    new Boolean(true));
        }
        tagData.addElement(data);
    }

    /**
     * Indicate which of two possible messages should be substituted for the
     * <code>&lt;msg:<i>n</i>,<i>m</i>&gt; tag.  An argument of
     * <code>true</code> would select message <i>n</i> while an
     * argument of <code>false</code> would select <i>m</i>.  In the
     * future, this capability may be expanded to support more than
     * two choices.
     *
     * @param choice boolean indicating which message to substitute
     */
    public void choose(boolean choice) {
        tagData.addElement(String.valueOf(choice));
    }

    /**
     * Shortcut method for adding entity name and owner data at the same time.
     * Assumes that the entity name should be obscured, but the owner should
     * not.
     *
     * @param entity the entity you wish to add
     */
    public void addDesc(Entity entity) {
        if (entity != null) {
            add("<font color='0xffffff'><a href=\"#entity:" + entity.getId()
                    + "\">" + entity.getShortName() + "</a></font>", true);
            String colorcode = Integer.toHexString(PlayerColors.getColor(
                    entity.getOwner().getColorIndex()).getRGB() & 0x00f0f0f0);
            add("<B><font color='" + colorcode + "'>"
                    + entity.getOwner().getName() + "</font></B>");
        }
    }

    /**
     * Internal method. Not for typical use.
     * <p>
     * Tests wheter the data value at the given index has been marked as
     * obscured.
     *
     * @param index position of data value (indexes are chronological and start
     *            at zero)
     * @return true if the data value was marked obscured
     */
    public boolean isValueObscured(int index) {
        if (obscuredIndexes.get(new Integer(index)) == null) {
            return false;
        }
        return true;
    }

    /**
     * Internal method. Not for typical use.
     * <p>
     * Remove the data value from the report. This operation is irreversible.
     *
     * @param index position of data value (indexes are chronological and start
     *            at zero
     */
    public void hideData(int index) {
        tagData.setElementAt(null, index);
    }

    /**
     * Indent the report.
     */
    public void indent() {
        indent(1);
    }

    /**
     * Indent the report <i>n</i> times.
     *
     * @param n the number of times to indent the report
     */
    public void indent(int n) {
        indentation += (n * Report.DEFAULT_INDENTATION);
    }

    /**
     * Internal method. Not for typical use.
     * <p>
     * Get the total number of data values associated with this report. Note
     * that this includes the <code>true/false</code> values added for
     * &lt;msg&gt; tags as well.
     *
     * @return the number of data values
     */
    public int dataCount() {
        return tagData.size();
    }

    private String getTag() {
        return getTag(tagCounter);
    }

    private String getTag(int index) {
        try {
            String value = tagData.elementAt(index);
            if (value == null) {
                return Report.OBSCURED_STRING;
            } else if (tagTranslate != null) {
                // Each common Resource Bundle is found below
                if (tagTranslate.equals("Messages")) {
                    return Messages.getString(value);
                // Others ifs will be here.
                }
            }
            return value;
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Error: Report#getText --> Array Index out of "
                    + "Bounds Exception (index: " + index
                    + ") for a report with ID " + messageId
                    + ".  Maybe Report#add wasn't called enough "
                    + "times for the amount of tags in the message?");
            return "[Reporting Error: see megameklog.txt for details]";
        }
    }

    /**
     * Get the report in its final form, with all the necessary substitutions
     * made.
     *
     * @return a String with the final report
     */
    public String getText() {
        // The raw text of the message, with tags.
        String raw = ReportMessages.getString(String.valueOf(messageId));

        // This will be the finished product, with data substituted for tags.
        StringBuffer text = new StringBuffer();

        if (raw == null) {
            // Should we handle this better? Check alternate language files?
            System.out.println("Error: No message found for ID "
                    + messageId);
            text.append("[Reporting Error for message ID ").append(
                    messageId).append("]");
        } else {
            int i = 0;
            int mark = 0;
            while (i < raw.length()) {
                if (raw.charAt(i) == '<') {
                    // find end of tag
                    int endTagIdx = raw.indexOf('>', i);
                    if ((raw.indexOf('<', i + 1) != -1)
                            && (raw.indexOf('<', i + 1) < endTagIdx)) {
                        // hmm...this must be a literal '<' character
                        i++;
                        continue;
                    }
                    // copy the preceeding characters into the buffer
                    text.append(raw.substring(mark, i));
                    if (raw.substring(i + 1, endTagIdx).equals("data")) {
                        text.append(getTag());
                        // System.out.println("Report-->getText(): " +
                        // this.tagData.elementAt(this.tagCounter));
                        tagCounter++;
                    } else if (raw.substring(i + 1, endTagIdx).equals("list")) {
                        for (int j = tagCounter; j < tagData.size(); j++) {
                            text.append(getTag(j)).append(", ");
                        }
                        text.setLength(text.length() - 2); // trim last comma
                    } else if (raw.substring(i + 1, endTagIdx).startsWith(
                            "msg:")) {
                        boolean selector = Boolean.valueOf(getTag())
                                .booleanValue();
                        if (selector) {
                            text.append(ReportMessages.getString(raw.substring(
                                    i + 5, raw.indexOf(',', i))));
                        } else {
                            text.append(ReportMessages.getString(raw.substring(
                                    raw.indexOf(',', i) + 1, endTagIdx)));
                        }
                        tagCounter++;
                    } else if (raw.substring(i + 1, endTagIdx)
                            .equals("newline")) {
                        text.append("\n");
                    } else {
                        // not a special tag, so treat as literal text
                        text.append(raw.substring(i, endTagIdx + 1));
                    }
                    mark = endTagIdx + 1;
                    i = endTagIdx;
                }
                i++;
            }
            text.append(raw.substring(mark));
            handleIndentation(text);
            text.append(getNewlines());
        }
        tagCounter = 0;
        // debugReport
        if (type == Report.TESTING) {
            Report.mark(text);
        }
        return text.toString();
    }

    private void handleIndentation(StringBuffer sb) {
        if ((indentation == 0) || (sb.length() == 0)) {
            return;
        }
        int i = 0;
        while (sb.substring(i, i+4).equals("\n")) {
            i+=4;
            if (i == sb.length()) {
                continue;
            }
        }
        sb.insert(i, getSpaces());
    }

    private String getSpaces() {
        StringBuffer spaces = new StringBuffer();
        for (int i = 0; i < indentation; i++) {
            spaces.append("&nbsp;");
        }
        return spaces.toString();
    }

    private String getNewlines() {
        StringBuffer sbNewlines = new StringBuffer();
        for (int i = 0; i < newlines; i++) {
            sbNewlines.append("\n");
        }
        return sbNewlines.toString();
    }

    /**
     * Adds a newline to the last report in the given Vector.
     *
     * @param v a Vector of Report objects
     */
    public static void addNewline(Vector<Report> v) {
        try {
            v.elementAt(v.size() - 1).newlines++;
        }
        catch (ArrayIndexOutOfBoundsException ex) {
            System.err.println("Report.addNewline failed, array index out " +
            		"of bounds");
        }
    }

    /**
     * Internal method. Not for typical use.
     * <p>
     * Adds the given player name to the report's list of players who received
     * an obscured version of this report from the server at some time in the
     * past.
     *
     * @param playerName the String containing the player's name
     */
    public void addObscuredRecipient(String playerName) {
        obscuredRecipients.addElement(playerName);
    }

    /**
     * Internal method. Not for typical use.
     * <p>
     * Tests whether the given player name is on the report's list of players
     * who received an obscured version of this report from the server at some
     * time in the past.
     *
     * @param playerName the String containing the player's name
     * @return true if the player was sent an obscured version of this report
     */
    public boolean isObscuredRecipient(String playerName) {
        for (int i = 0; i < obscuredRecipients.size(); i++) {
            String s = obscuredRecipients.elementAt(i);
            if (s.equals(playerName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Useful for debugging purposes.
     *
     * @return a String of the form "Report(messageId=n)"
     */
    @Override
    public String toString() {
        String val = new String();
        val = "Report(messageId=";
        val += messageId;
        val += ")";
        return val;
    }

    /**
     * DEBUG method - do not use
     */
    // debugReport method
    public void markForTesting() {
        type = Report.TESTING;
    }

    // debugReport method
    private static StringBuffer mark(StringBuffer sb) {
        sb.insert(0, "<hidden>");
        int i = sb.length() - 1;
        while (sb.charAt(i) == '\n') {
            i--;
            if (i == 0) {
                continue;
            }
        }
        sb.insert(i + 1, "</hidden>");
        return sb;
    }

    public static void indentAll(Vector<Report> vDesc, int amount) {
        // Just avoid an error condition.
        if (vDesc == null) {
            return;
        }

        Enumeration<Report> x = vDesc.elements();
        while (x.hasMoreElements()) {
            Report r = x.nextElement();
            r.indent(amount);
        }
    }
}
