package com.acme.processors;

import com.acme.ChannelGroupMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.irc.IrcMessage;
import org.apache.camel.component.telegram.model.OutgoingTextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


public class IrcMessageProcessor implements Processor
{
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private final ChannelGroupMapper channelGroupMappings;

    public IrcMessageProcessor(ChannelGroupMapper channelGroupMappings)
    {
        this.channelGroupMappings = channelGroupMappings;
    }

    @Override
    public void process(Exchange exchange) throws Exception
    {
        exchange.getOut().setBody(exchange.getIn().getBody());
        exchange.getOut().setHeaders(exchange.getIn().getHeaders());

        IrcMessage ircMessage = exchange.getIn().getBody(
                IrcMessage.class);
        LOG.debug("[IRC] msg type {}",
                ircMessage.getMessageType());
        if (ircMessage.getMessageType().equals("PRIVMSG")) {
            LOG.debug("[IRC {}] {}: {}",
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
                LOG.info("[IRC {}] -> Telegram[{}]: {}",
                        ircMessage.getTarget(),
                        matchingGroup,
                        combinedMsg);
                exchange.getOut().setBody(outMsg);
            }
        } else {
            LOG.info("IRC other msg: {}", ircMessage.getMessage());
            exchange.getOut().setBody(null);
        }
    }

    public String matchChannelToGroup(String channel)
    {
        String matchingGroup = "";

        for (Map.Entry<String, String> e : channelGroupMappings
                .getChannels().entrySet()) {
            if (e.getKey().equals(channel)) {
                LOG.debug("IRC channel {} equals Telegram group {}({})",
                        channel,
                        e.getKey(),
                        e.getValue());
                matchingGroup = e.getValue();
                return matchingGroup;
            }
        }
        return matchingGroup;
    }

}
