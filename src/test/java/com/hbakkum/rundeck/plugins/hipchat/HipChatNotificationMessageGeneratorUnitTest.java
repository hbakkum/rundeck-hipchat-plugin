package com.hbakkum.rundeck.plugins.hipchat;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * @author Hayden Bakkum
 */
public class HipChatNotificationMessageGeneratorUnitTest {

    private static final Map<String, Object> EXECUTION_DATA = new HashMap<String, Object>();
    static {
        final Map<String, String> job = new HashMap<String, String>();
        job.put("href", "http://rundeck/jobs/my_job");
        job.put("group", "job_group");
        job.put("name", "job_name");
        job.put("username", "hbakkum");
        job.put("execid", "1");

        final Map<String, Object> context = new HashMap<String, Object>();
        context.put("job", job);

        EXECUTION_DATA.put("job", job);
        EXECUTION_DATA.put("context", context);
        EXECUTION_DATA.put("href", "http://rundeck/jobs/my_job/output");
    }

    private HipChatNotificationMessageGenerator messageGenerator;

    @BeforeMethod
    public void setUp() {
        this.messageGenerator = new HipChatNotificationMessageGenerator();
    }

    @Test
    public void testDefaultTemplateGetsUsedWhenNoTemplateLocationIsSpecified() {
        final String expectedMessage = "Execution of job\n" +
                "<a href=\"http://rundeck/jobs/my_job\">\n" +
                "job_group/job_name</a>\n" +
                "    <b>started</b>\n" +
                "<ul>\n" +
                "    <li>User: hbakkum</li>\n" +
                "    <li>ExecId: 1</li>\n" +
                "</ul>\n" +
                "<a href=\"http://rundeck/jobs/my_job/output\">View Output</a>";

        final String actualMessage = messageGenerator.generateMessage(null, "hipchat-message.ftl", "start", EXECUTION_DATA, null);

        assertEquals(actualMessage, expectedMessage);
    }

    @Test
    public void testDefaultTemplateIsOverriddenWhenTemplateLocationIsSpecified() {
        final String expectedMessage = "job_name started";
        final String templateLocation = this.getClass().getResource("/templates/hipchat-message-override-test.ftl").getFile();

        final String actualMessage = messageGenerator.generateMessage(templateLocation, "hipchat-message.ftl", "start", EXECUTION_DATA, null);

        assertEquals(actualMessage, expectedMessage);
    }


}
