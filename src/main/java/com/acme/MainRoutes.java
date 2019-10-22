package com.acme;

import com.acme.processors.IrcMessageProcessor;
import com.acme.processors.TelegramMessageProcessor;
import com.acme.telegramutils.TelegramUtils;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.irc.IrcMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class MainRoutes extends RouteBuilder
{
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    private static final String FROM_IRC_ROUTE_ID = "from-irc";
    private static final String FROM_TELEGRAM_ROUTE_ID = "from-telegram";

    private final String ircUri;
    private final String telegramUri;
    private final TelegramUtils telegramUtils;
    private final ChannelGroupMapper channelGroupMappings;

    @Autowired
    public MainRoutes(TelegramUtils telegramUtils,
                      ChannelGroupMapper channelGroupMappings,
                      @Value("${irc.uri}") String ircUri,
                      @Value("${telegram.uri}") String telegramUri)
    {
        this.telegramUtils = telegramUtils;
        this.channelGroupMappings = channelGroupMappings;
        this.ircUri = ircUri;
        this.telegramUri = telegramUri;
    }

    @Override
    public void configure() throws Exception
    {
        onException(RuntimeCamelException.class)
            .process(exchange -> {
                exchange.getOut().setBody(exchange.getIn().getBody());
                exchange.getOut().setHeaders(exchange.getIn().getHeaders());

                LOG.info("Stopping route {}", FROM_IRC_ROUTE_ID);
                exchange.getContext().stopRoute(FROM_IRC_ROUTE_ID);
                LOG.info("Starting route {}", FROM_IRC_ROUTE_ID);
                exchange.getContext().startRoute(FROM_IRC_ROUTE_ID);
            })
            .delay(1000 * 60 * 2);

        from(ircUri)
            .routeId(FROM_IRC_ROUTE_ID)
            .choice()
                .when(header("irc.messageType").isEqualToIgnoreCase("PRIVMSG"))
                    .process(new IrcMessageProcessor(channelGroupMappings))
                    .to(telegramUri)
                    .log("[IRC] -> Telegram delivered")
                .otherwise()
                .process(exchange -> {
                    exchange.getOut().setBody(exchange.getIn().getBody());
                    exchange.getOut().setHeaders(exchange.getIn().getHeaders());

                    try {
                        IrcMessage ircMsg = exchange.getOut().getBody(IrcMessage.class);
                        LOG.info("{} {}", ircMsg.getMessageType(), ircMsg.getMessage());
                    } catch (Exception e) {
                        LOG.warn("Couldn't parse IRC message: {}", e.getMessage());
                    }
                })
            .end();

        from(telegramUri)
            .routeId(FROM_TELEGRAM_ROUTE_ID)
            .log("[Telegram] Incoming message")
            .process(new TelegramMessageProcessor(channelGroupMappings,
                                                  telegramUtils))
            .to(ircUri)
            .log("[Telegram] -> IRC delivered");
    }

}
