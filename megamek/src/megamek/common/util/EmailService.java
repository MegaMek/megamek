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

import megamek.common.Game;
import megamek.common.Player;
import megamek.common.Report;
import org.apache.logging.log4j.LogManager;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class EmailService {
    private static class RoundReportMessage extends MimeMessage {


        private RoundReportMessage(InternetAddress from,
                                   Player to,
                                   Game game,
                                   Vector<Report> reports,
                                   int sequenceNumber,
                                   Session session) throws Exception {
            super(session);

            // Since MM mutates game state as it progresses, need to
            // fully create the complete message here, so that by the
            // time it is sent things like the current round number
            // hasn't changed from underneath it

            setFrom(from);
            setRecipient(
                RecipientType.TO,
                new InternetAddress(to.getEmail(), to.getName())
            );

            setHeader(
                "Message-ID",
                newMessageId(from, to, game, sequenceNumber)
            );
            if (sequenceNumber > 0) {
                setHeader(
                    "In-Reply-To",
                    newMessageId(from, to, game, sequenceNumber - 1)
                );
            }

            Report subjectReport;
            var round = game.getRoundCount();
            if (round < 1) {
                subjectReport = new Report(990);
            } else {
                subjectReport = new Report(991);
                subjectReport.add(round, false);
            }
            setSubject(subjectReport.getText());

            var body = new StringBuilder("<div style=\"white-space: pre\">");
            for (var report: reports) {
                body.append(report.getText());
            }
            body.append("</div>");
            setText(body.toString(), "UTF-8", "html");
        }

        @Override
        protected void updateMessageID() throws MessagingException {
            // no-op, we have already set it in the ctor
        }

        private static String newMessageId(InternetAddress from,
                                           Player to,
                                           Game game,
                                           int actualSequenceNumber) {
            final var address = from.getAddress();
            return String.format(
                "<megamek.%s.%d.%d.%d@%s>",
                game.getUUIDString(),
                game.getRoundCount(),
                to.getId(),
                actualSequenceNumber,
                address.substring(address.indexOf("@") + 1)
            );
        }

    }


    private InternetAddress from;
    private Map<Player, Integer> messageSequences = new HashMap<>();
    private Properties mailProperties;
    private Session mailSession;

    private BlockingQueue<Message> mailQueue = new LinkedBlockingQueue<>();
    private Thread mailWorker;
    private boolean running = true;


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
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(login, password);
                    }
                };
        }

        mailSession = Session.getInstance(mailProperties, auth);

        mailWorker = new Thread() {
                @Override
                public void run() {
                    workerMain();
                }
            };
        mailWorker.start();
    }

    public Vector<Player> getEmailablePlayers(Game game) {
        Vector<Player> emailable = new Vector<>();
        for (var player: game.getPlayersVector()) {
            if (!StringUtil.isNullOrEmpty(player.getEmail()) && !player.isBot()
                    && !player.isObserver()) {
                emailable.add(player);
            }
        }
        return emailable;
    }

    public Message newReportMessage(Game game,
                                    Vector<Report> reports,
                                    Player player) throws Exception {
        int nextSequence = 0;
        synchronized (messageSequences) {
            var messageSequence = messageSequences.get(player);
            if (messageSequence != null) {
                nextSequence = messageSequence + 1;
            }
            messageSequences.put(player, nextSequence);
        }
        return new RoundReportMessage(
            from, player, game, reports, nextSequence, mailSession
        );
    }

    public void send(final Message message) {
        mailQueue.offer(message);
    }

    public void reset() {
        messageSequences.clear();
    }

    public void shutdown() {
        this.running = false;
        this.mailWorker.interrupt();
    }

    private void workerMain() {
        while (running) {
            try {
                // blocks until a message is received
                var message = mailQueue.take();

                var transport = mailSession.getTransport(message.getFrom()[0]);
                try {
                    transport.connect();
                    while (message != null) {
                        message.saveChanges();
                        transport.sendMessage(message, message.getAllRecipients());

                        // If there are any other messages in the queue,
                        // send them immediately while the connection is
                        // still open. This doesn't block;
                        message = mailQueue.poll();
                    }
                } finally {
                    transport.close();
                }
            } catch (InterruptedException ex) {
                // All good, just shut down
                running = false;
            } catch (Exception ex) {
                LogManager.getLogger().error("Error sending email", ex);
            }
        }
    }

}
