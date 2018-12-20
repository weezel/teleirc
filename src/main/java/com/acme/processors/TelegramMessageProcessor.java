package com.acme.processors;

import com.acme.ChannelGroupMapper;
import com.acme.telegramutils.TelegramUtils;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.irc.IrcConstants;
import org.apache.camel.component.telegram.model.IncomingMessage;
import org.apache.camel.component.telegram.model.IncomingPhotoSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;


public class TelegramMessageProcessor implements Processor
{
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private final ChannelGroupMapper channelGroupMappings;
    private final TelegramUtils telegramUtils;

    public TelegramMessageProcessor(ChannelGroupMapper channelGroupMappings,
                                    TelegramUtils telegramUtils)
    {
        this.channelGroupMappings = channelGroupMappings;
        this.telegramUtils = telegramUtils;
    }

    @Override
    public void process(Exchange exchange) throws Exception
    {
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
        String publishUrl = "";
        List<IncomingPhotoSize> image = telegramMsg.getPhoto();

        if (matchingChannel.equals("")) {
            LOG.warn("Couldn't find channel match for group: {}",
                    telegramGroup);
            exchange.getOut().setBody(null);
        } else if (telegramMsg.getText() == null &&
                telegramMsg.getPhoto() == null) {
            LOG.warn("[Telegram] Text was null, abort");
            exchange.getOut().setBody(null);
        } else {
            String userName = "";

            if (telegramMsg.getPhoto() != null) {
                LOG.info("[Telegram] Message contains a photo");
                publishUrl = telegramUtils.downloadPhotos(
                        telegramMsg.getPhoto());
                LOG.info("[Telegram] Photo downloaded");
            }

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

            String msg = telegramMsg.getText() != null ?
                    telegramMsg.getText() : "";
            String combinedMsg = "";

            // FIXME It seems that get|set caption is not implemented for
            // inocming messages, it exists for outgoing messages though
            if (msg.length() > 1) {
                combinedMsg = String.format("<%s> %s %s",
                        userName,
                        msg,
                        publishUrl);
            } else {
                combinedMsg = String.format("<%s> %s",
                        userName,
                        publishUrl);
            }

            exchange.getOut().setHeader(
                    IrcConstants.IRC_TARGET,
                    matchingChannel);
            LOG.info("[Telegram {}] -> IRC[{}]: {}",
                    telegramMsg.getChat().getTitle(),
                    matchingChannel,
                    combinedMsg);
            exchange.getOut().setBody(combinedMsg);
        }
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
