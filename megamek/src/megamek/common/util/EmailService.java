/*
* MegaMek -
* Copyright (C) 2021 - The MegaMek Team. All Rights Reserved.
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

package megamek.common.util;

import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.Vector;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import megamek.common.IPlayer;
import megamek.common.Report;


public class EmailService {


    private InternetAddress from;
    private Properties mailProperties;
    private Session mailSession;


    public EmailService(Properties mailProperties) throws Exception {
        this.from = InternetAddress.parse(
            mailProperties.getProperty("megamek.smtp.from", "")
        )[0];
        this.mailProperties = mailProperties;

        Authenticator auth = null;
        var login = mailProperties.getProperty("megamek.smtp.login", "").trim();
        var password = mailProperties.getProperty("megamek.smtp.password", "").trim();
        if (login.length() > 0 && password.length() > 0) {
            auth = new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(login, password);
                    }
                };
        }

        mailSession = Session.getInstance(mailProperties, auth);
    }

    public Message newReportMessage(IPlayer player,
                                    int round,
                                    Vector<Report> reports) throws Exception {
        var message = new MimeMessage(mailSession);
        message.setFrom(from);
        message.setRecipient(
            Message.RecipientType.TO,
            new InternetAddress(player.getEmail(), player.getName())
        );

        Report subjectReport;
        if (round < 1) {
            subjectReport = new Report(990);
        } else {
            subjectReport = new Report(991);
            subjectReport.add(round, false);
        }
        message.setSubject(subjectReport.getText());

        var body = new StringBuilder("<div style=\"white-space: pre\">");
        for (var report: reports) {
            body.append(report.getText());
        }
        body.append("</div>");
        message.setText(body.toString(), "UTF-8", "html");
        return message;
    }

    public void send(final Message message) throws MessagingException {
        Transport.send(message);
    }

}
