package com.yelp.pyleus;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.yelp.pyleus.bolt.PythonBolt;
import com.yelp.pyleus.spout.PythonSpout;

public class PythonComponentsFactory {

    public static final String VIRTUALENV_INTERPRETER = "pyleus_venv/bin/python";
    // A few notes on how this works:
    // (1) The developer of the topology is responsible for putting a newrelic.ini at the root of the topology folder,
    // alongside with the topology definition file (.yml).
    // (2) newrelic-admin only works when the python process it monitors uses the same python installation that
    // installed newrelic-admin, as described here:
    // https://docs.newrelic.com/docs/agents/python-agent/installation-configuration/python-agent-admin-script
    // (3) newrelic-admin installed by Pyleus in the virtualenv actually points to the wrong python installation in a
    // temporary folder. We work around that issue by run it with a specified python installation instead of
    // running it directly as a self-executable script.
    public static final String NEW_RELIC_PREFIX = String.format("NEW_RELIC_CONFIG_FILE=newrelic.ini %s pyleus_venv/bin/newrelic-admin run-program", VIRTUALENV_INTERPRETER);
    public static final String MODULE_OPTION = "-m";

    private String[] buildCommand(final String module, final Map<String, Object> argumentsMap,
        final String loggingConfig, final String serializerConfig) {

        String[] command = new String[3];

        command[0] = "bash";
        command[1] = "-c";

        StringBuilder strBuf = new StringBuilder();
        // Done before launching any spout or bolt in order to cope with Storm permissions bug
        strBuf.append(String.format("chmod 755 %s; %s %s", VIRTUALENV_INTERPRETER, NEW_RELIC_PREFIX, VIRTUALENV_INTERPRETER));
        strBuf.append(String.format(" %s %s", MODULE_OPTION, module));

        if (argumentsMap != null) {
            Gson gson = new GsonBuilder().create();
            String json = gson.toJson(argumentsMap);
            json = json.replace("\"", "\\\"");
            strBuf.append(String.format(" --options \"%s\"", json));
        }

        {
            Map<String, Object> pyleusConfig = new HashMap<String, Object>();
            pyleusConfig.put("logging_config_path", loggingConfig);
            pyleusConfig.put("serializer", serializerConfig);
            Gson gson = new GsonBuilder().create();
            String json = gson.toJson(pyleusConfig);
            json = json.replace("\"", "\\\"");
            strBuf.append(String.format(" --pyleus-config \"%s\"", json));
        }

        command[2] = strBuf.toString();

        return command;
    }

    public PythonBolt createPythonBolt(final String module, final Map<String, Object> argumentsMap,
        final String loggingConfig, final String serializerConfig) {

        return new PythonBolt(buildCommand(module, argumentsMap, loggingConfig, serializerConfig));
    }

    public PythonSpout createPythonSpout(final String module, final Map<String, Object> argumentsMap,
        final String loggingConfig, final String serializerConfig) {

        return new PythonSpout(buildCommand(module, argumentsMap, loggingConfig, serializerConfig));
    }

}
