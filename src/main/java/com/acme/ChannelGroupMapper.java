package com.acme;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;


@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix="channel-group-mappings")
public class ChannelGroupMapper
{
    private Map<String, String> channels = new HashMap();

    public Map<String, String> getChannels()
    {
        return channels;
    }
    public void setChannels(Map<String, String> channels)
    {
        this.channels = channels;
    }
}
