package com.acme;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.irc.IrcConstants;
import org.apache.camel.component.irc.IrcMessage;
import org.apache.camel.component.telegram.model.IncomingMessage;
import org.apache.camel.component.telegram.model.OutgoingTextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;


@Component
public class TeleIrcBridgeRoutes extends RouteBuilder
{
    private final Logger LOG = LoggerFactory.getLogger(TeleIrcBridgeRoutes.class);

    @Value("${irc.uri}")
    private String ircUri;

    @Value("${telegram.uri}")
    private String telegramUri;

    @Autowired
    private ChannelGroupMapper channelGroupMappings;

    @Override
    public void configure() throws Exception
    {

        from(ircUri)
                .process(exchange -> {
                    exchange.getOut().setBody(exchange.getIn().getBody());
                    exchange.getOut().setHeaders(exchange.getIn().getHeaders());

                    IrcMessage ircMessage = exchange.getIn().getBody(
                            IrcMessage.class);
                    LOG.debug("IRC msg type {}",
                            ircMessage.getMessageType());
                    if (ircMessage.getMessageType().equals("PRIVMSG")) {
                        LOG.debug("IRC [{}] {}: {}",
                                ircMessage.getTarget(),
                                ircMessage.getUser().getNick(),
                                ircMessage.getMessage());

                        String channel = ircMessage.getTarget()
                                .replaceAll("^#", "");
                        String matchingGroup = matchChannelToGroup(channel);

                        if (matchingGroup.equals("")) {
                            LOG.warn("Couldn't find group match for channel: {}",
                                    ircMessage.getTarget());
                            exchange.getOut().setBody(null);
                        } else {
                            String combinedMsg = String.format("<%s> %s",
                                    ircMessage.getUser().getNick(),
                                    ircMessage.getMessage());

                            OutgoingTextMessage outMsg = new OutgoingTextMessage();
                            outMsg.setChatId(matchingGroup);
                            outMsg.setText(combinedMsg);
                            LOG.info("IRC[{}] -> Telegram[{}]: {}",
                                    ircMessage.getTarget(),
                                    matchingGroup,
                                    combinedMsg);
                            exchange.getOut().setBody(outMsg);
                        }
                    } else {
                        LOG.info("IRC other msg: {}", ircMessage.getMessage());
                        exchange.getOut().setBody(null);
                    }
                })
                .to(telegramUri)
                .log("IRC -> Telegram delivered");

        from(telegramUri)
                .log("Incoming Telegram message")
                .process(exchange -> {
                    exchange.getOut().setBody(exchange.getIn().getBody());
                    exchange.getOut().setHeaders(exchange.getIn().getHeaders());

                    IncomingMessage telegramMsg = exchange.getIn().getBody(
                            IncomingMessage.class);
                    String telegramGroup = telegramMsg.getChat().getId()
                            .replaceAll("^#", "");

                    String groupName = telegramMsg.getChat().getTitle();
                    String groupId  = telegramMsg.getChat().getId();
                    String matchingChannel = matchGroupToChannel(
                            groupName, groupId);

                    if (matchingChannel.equals("")) {
                        LOG.warn("Couldn't find channel match for group: {}",
                                telegramGroup);
                        exchange.getOut().setBody(null);
                    } else if (telegramMsg.getText() == null ||
                               telegramMsg.getText().length() < 1) {
                        LOG.warn("Text was null, not going to relay message");
                        exchange.getOut().setBody(null);
                    } else {
                        String userName = "";

                        if (telegramMsg.getFrom().getUsername() != null) {
                                userName  = telegramMsg.getFrom().getUsername();
                        } else {
                                String firstName = telegramMsg.getFrom()
                                        .getFirstName();
                                String lastName = telegramMsg.getFrom()
                                        .getLastName();

                                if (firstName == null)
                                    firstName = "";
                                if (lastName == null) {
                                    userName = String.format("%s",
                                            firstName);
                                } else {
                                        userName = String.format("%s %s",
                                                firstName, lastName);
                                    }
                        }
                        String combinedMsg = String.format("%s: %s",
                                userName,
                                telegramMsg.getText());
                        exchange.getOut().setHeader(
                                IrcConstants.IRC_TARGET,
                                matchingChannel);
                        LOG.info("Telegram[{}] -> IRC[{}]: {}",
                                telegramMsg.getChat().getTitle(),
                                matchingChannel,
                                combinedMsg);
                        exchange.getOut().setBody(combinedMsg);
                    }
                })
                .to(ircUri)
                .log("Telegram -> IRC delivered");
    } // configure

    public String matchChannelToGroup(String channel)
    {
        String matchingGroup = "";

        for (Map.Entry<String, String> e : channelGroupMappings
             .getChannels().entrySet()) {
            if (e.getKey().equals(channel)) {
                LOG.info("IRC channel {} equals Telegram group {}({})",
                        channel,
                        e.getKey(),
                        e.getValue());
                matchingGroup = e.getValue();
                return matchingGroup;
            }
        }
        return matchingGroup;
    }

    public String matchGroupToChannel(String groupName, String groupId)
    {
        String matchingChannel = "";

        for (Map.Entry<String, String> e : channelGroupMappings
                .getChannels().entrySet()) {
            if (e.getValue().equals(groupId)) {
                LOG.info(
                    "Telegram group {}({}) equals IRC channel {}",
                        groupName,
                        groupId,
                        e.getKey());
                matchingChannel = String.format("#%s", e.getKey());
                return matchingChannel;
            }
        }
        return matchingChannel;
    }

}
