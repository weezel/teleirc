package com.acme;

import com.acme.processors.IrcMessageProcessor;
import com.acme.processors.TelegramMessageProcessor;
import com.acme.telegramutils.TelegramUtils;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class TeleIrcBridgeRoutes extends RouteBuilder
{
    @Value("${irc.uri}")
    private String ircUri;

    @Value("${telegram.uri}")
    private String telegramUri;

    @Autowired
    private ChannelGroupMapper channelGroupMappings;

    @Autowired
    private TelegramUtils telegramUtils;

    @Override
    public void configure() throws Exception
    {
        from(ircUri)
            .routeId("from-irc")
            .process(new IrcMessageProcessor(channelGroupMappings))
            .to(telegramUri)
            .log("[IRC] -> Telegram delivered");

        from(telegramUri)
            .routeId("from-telegram")
            .log("[Telegram] Incoming message")
            .process(new TelegramMessageProcessor(channelGroupMappings,
                                                  telegramUtils))
            .to(ircUri)
            .log("[Telegram] -> IRC delivered");
    }

}
