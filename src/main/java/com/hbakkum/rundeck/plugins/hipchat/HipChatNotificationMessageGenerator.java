package com.hbakkum.rundeck.plugins.hipchat;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Hayden Bakkum
 */
public class HipChatNotificationMessageGenerator {

    public String generateMessage(
            final String messageTemplateLocation,
            final String defaultMessageTemplateName,
            final String trigger,
            final Map executionData,
            final Map config) {
        final Configuration freeMarkerCfg = new Configuration();

        final String templateName = determineTemplateName(messageTemplateLocation, defaultMessageTemplateName, freeMarkerCfg);

        final Map<String, Object> model = new HashMap();
        model.put("trigger", trigger);
        model.put("execution", executionData);
        model.put("config", config);

        final StringWriter sw = new StringWriter();
        try {
            final Template template = freeMarkerCfg.getTemplate(templateName);
            template.process(model,sw);

        } catch (IOException ioEx) {
            throw new HipChatNotificationPluginException("Error loading HipChat notification message template: [" + ioEx.getMessage() + "].", ioEx);
        } catch (TemplateException templateEx) {
            throw new HipChatNotificationPluginException("Error merging HipChat notification message template: [" + templateEx.getMessage() + "].", templateEx);
        }

        return sw.toString();
    }

    private String determineTemplateName(final String messageTemplateLocation,
                                         final String defaultMessageTemplateName,
                                         final Configuration freeMarkerCfg) {
        String templateName;

        if (messageTemplateLocation != null && messageTemplateLocation.length() > 0) {
            final File messageTemplateFile = new File(messageTemplateLocation);
            try {
                freeMarkerCfg.setDirectoryForTemplateLoading(messageTemplateFile.getParentFile());
            } catch (IOException ioEx) {
                throw new HipChatNotificationPluginException("Error setting FreeMarker template loading directory: [" + ioEx.getMessage() + "].", ioEx);
            }
            templateName = messageTemplateFile.getName();

        } else {
            freeMarkerCfg.setClassForTemplateLoading(HipChatNotificationPlugin.class, "/templates");
            templateName = defaultMessageTemplateName;
        }

        return templateName;
    }

}
