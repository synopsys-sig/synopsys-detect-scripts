/**
 * synopsys-detect-scripts
 *
 * Copyright (c) 2020 Synopsys, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.synopsys.detect.scripts;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.synopsys.detect.scripts.scripts.ScriptBuilder;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

public class Application {
    public static void main(final String[] args) throws IOException, IntegrationException, TemplateException {
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
        } else if (operation.equals("cert")) {
            final File certSource = new File("src/main/resources/jar_verification.crt");
            final File certDestination = new File(outputDirectory, "jar_verification.crt");
            FileUtils.copyFile(certSource, certDestination);
        } else {
            throw new IllegalArgumentException("Unknown operation. Must be 'scripts' or 'certs': " + operation);
        }

    }
}
