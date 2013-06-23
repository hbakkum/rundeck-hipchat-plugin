rundeck-hipchat-plugin
======================

Sends rundeck notification messages to a HipChat room

Installation Instructions
-------------------------

1. Either download the latest release from Maven Central 
([link](http://search.maven.org/#search%7Cga%7C1%7Crundeck-hipchat-plugin)) or build a snapshot from source. 
2. Copy the plugin jar (rundeck-hipchat-plugin-\<version\>.jar) into your $RDECK_BASE/libext - no restart of rundeck required. 

See the [rundeck documentation](http://rundeck.org/docs/manual/plugins.html#installing-plugins) for more 
information on installing rundeck plugins.

Limitations
-----------

Currently, rundeck (1.5.3) notification plugins only support "Instance" scoped configuration properties which means
HipChat API token and room configuration must be entered in the gui each time you configure a notification (as opposed 
to specifying these once in a project property file). Apparently there are plans to add more configuration scopes in 
the future, at which point, I'll update this plugin to take advantage of these.
