package com.acme.ircutils;

import org.springframework.stereotype.Component;


/* Splits too long Telegram messages to be more suitable for IRC */
@Component
public class MessageSplitter
{
    /* Maximum message size is 512 characters with Carriage return and
       line feed included. Since we receive messages without newlines,
       leave space for CR & LF characters. Also, IRCD is ASCII only,
       hence two or more byte characters are consuming more characters.
       That means that lenght might vary. Go with the conservative approach,
       tested with trial and error, maximum is 216 latin1 ä characters.
       Works with Finnish language at least. */
    private final static Integer MAX_IRC_MSG_LEN = 216;

    public String[] splitLongMsg(String msg)
    {
        int splitCount = (int) Math.ceil((double)msg.length() / MAX_IRC_MSG_LEN);
        String[] splits = new String[splitCount];
        int msgSize = msg.length();

        if (splitCount == 1) {
            splits[0] = String.valueOf(msg.subSequence(0, msg.length()));
            return splits;
        }

        /* Message has more than 1 splits, hence
           0 - splitCount - 1 contains strings with max size */
        int startOffset = 0;
        int endOffset = MAX_IRC_MSG_LEN;
        for (int idx = 0; idx < splitCount - 1; idx++) {
            splits[idx] = String.valueOf(msg.subSequence(
                    startOffset,
                    endOffset));
            startOffset = endOffset;
            endOffset = startOffset + MAX_IRC_MSG_LEN;
        }
        // Deal with the leftover part which is shorter than max size
        startOffset = MAX_IRC_MSG_LEN * (splitCount - 1);
        endOffset = msg.length();
        splits[splitCount - 1] = String.valueOf(msg.subSequence(
                startOffset,
                endOffset));

        return splits;
    }
}
