package com.acme;

import com.acme.ircutils.MessageSplitter;
import org.junit.Assert;
import org.junit.Test;


public class MessageSplitterTest
{
    private MessageSplitter messageSplitter = new MessageSplitter();
    private String chars001 = "a";
    private String chars215 = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefg";
    private String chars216 = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefgh";
    private String chars217 = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghi";
    private String chars432 = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnop";

    @Test
    public void messageSplitterCharlen001Test()
    {
        String[] splitted = messageSplitter.splitLongMsg(chars001);
        Assert.assertEquals("Wrong size for splitted",
                            1,
                            splitted.length);

        Assert.assertEquals("Wrong size for the first split",
                            1,
                            splitted[0].length());
        Assert.assertEquals("Wrong content for the first element", "a", splitted[0]);
    }

    @Test
    public void messageSplitterCharlen215Test()
    {
        String[] splitted = messageSplitter.splitLongMsg(chars215);
        Assert.assertEquals("Wrong size for splitted",
                            1,
                            splitted.length);

        Assert.assertEquals("Wrong size for the first split",
                            215,
                            splitted[0].length());
        Assert.assertTrue("Wrong content int the first split",
                          splitted[0].endsWith("efg"));
    }

    @Test
    public void messageSplitterCharlen216Test()
    {
        String[] splitted = messageSplitter.splitLongMsg(chars216);
        Assert.assertEquals("Wrong size for splitted",
                            1,
                            splitted.length);

        Assert.assertEquals("Wrong size for the first split",
                            216,
                            splitted[0].length());
        Assert.assertTrue("Wrong content in the first split",
                          splitted[0].endsWith("fgh"));
    }

    @Test
    public void messageSplitterCharlen217Test()
    {
        String[] splitted = messageSplitter.splitLongMsg(chars217);
        Assert.assertEquals("Wrong size for splitted",
                            2,
                            splitted.length);

        Assert.assertEquals("Wrong size for the first split",
                            216,
                            splitted[0].length());
        Assert.assertTrue("Wrong content in the first split",
                          splitted[0].endsWith("fgh"));

        Assert.assertEquals("Wrong size for the second split",
                            1,
                            splitted[1].length());
        Assert.assertTrue("Wrong content in the second split",
                          splitted[1].endsWith("i"));
    }

    @Test
    public void messageSplitterCharlen432Test()
    {
        String[] splitted = messageSplitter.splitLongMsg(chars432);
        Assert.assertEquals("Wrong size for splitted", 2, splitted.length);

        Assert.assertEquals("Wrong size for the first split",
                            216,
                            splitted[0].length());
        Assert.assertTrue("Wrong content in the first split",
                          splitted[0].endsWith("fgh"));

        Assert.assertEquals("Wrong size for the second split",
                            216,
                            splitted[1].length());

        Assert.assertTrue("Wrong content in the second split",
                splitted[1].startsWith("ijk"));
        Assert.assertTrue("Wrong content in the second split",
                          splitted[1].endsWith("nop"));
    }

}
