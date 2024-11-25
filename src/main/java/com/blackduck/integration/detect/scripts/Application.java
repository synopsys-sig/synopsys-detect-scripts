/*
 * detect-scripts
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.detect.scripts;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import com.blackduck.integration.detect.scripts.scripts.ScriptBuilder;
import com.blackduck.integration.exception.IntegrationException;

import freemarker.template.TemplateException;

public class Application {
    public static void main(final String[] args) throws IOException, IntegrationException, TemplateException, URISyntaxException {
        if (args.length != 2) {
            throw new IllegalArgumentException("Please provide two arguments, the operation and the output directory of the scripts.");
        }
        final String operation = args[0];
        final String outputDirectoryPath = args[1];
        final File outputDirectory = new File(outputDirectoryPath);
        outputDirectory.mkdirs();

         if (operation.equals("scripts")) {
            ScriptBuilder scriptBuilder = new ScriptBuilder();
            scriptBuilder.generateScripts(outputDirectory);
        } else {
            throw new IllegalArgumentException("Unknown operation. Must be 'scripts': " + operation);
        }

    }
}
