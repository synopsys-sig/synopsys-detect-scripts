/*
 * synopsys-detect-scripts
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.synopsys.detect.scripts.scripts; /**
 * synopsys-detect-scripts
 *
 * Copyright (c) 2019 Synopsys, Inc.
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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.rest.client.IntHttpClient;
import com.synopsys.integration.rest.proxy.ProxyInfo;
import com.synopsys.integration.rest.request.Request;
import com.synopsys.integration.rest.request.Response;
import com.synopsys.integration.util.ResourceUtil;

/**
 * synopsys-detect-scripts
 *
 * Copyright (c) 2019 Synopsys, Inc.
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
public class ScriptBuilder {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    public void generateScripts(final File outputDirectory) throws IOException, IntegrationException {
        final String scriptVersion = ResourceUtil.getResourceAsString(this.getClass(), "/version.txt", StandardCharsets.UTF_8);
        final List<File> scriptFiles = new ArrayList<>();
        generateScript(scriptFiles, outputDirectory, "detect-sh.sh", "sh", scriptVersion, 6);
        generateScript(scriptFiles, outputDirectory, "detect-sh.sh", "sh", scriptVersion, 7);
        generateScript(scriptFiles, outputDirectory, "detect-ps.ps1", "ps1", scriptVersion, 6);
        generateScript(scriptFiles, outputDirectory, "detect-ps.ps1", "ps1", scriptVersion, 7);

        scriptFiles.forEach(this::logFileLocation);
    }

    private void logFileLocation(final File file) {
        logger.info(String.format("Generated script at: %s", file.getAbsolutePath()));
    }

    public void generateScript(List<File> scriptFiles, final File outputDirectory, final String templateFileName, final String scriptExtension, final String scriptVersion, int detectMajorVersion) throws IOException, IntegrationException {
        final File shellScriptFile = new File(outputDirectory, generateScriptFilename(scriptExtension, detectMajorVersion));
        final File shellScriptVersionedFile = new File(outputDirectory, generatedVersionedScriptFilename(scriptVersion, scriptExtension, detectMajorVersion));

        String detectVersionPropertyName = generateDetectVersionPropertyName(detectMajorVersion);
        if (!scriptVersion.contains("-SNAPSHOT")) {
            final File createdFile = buildScript(templateFileName, shellScriptFile, scriptVersion, detectVersionPropertyName);
            scriptFiles.add(createdFile);
        }

        final File createdFile = buildScript(templateFileName, shellScriptVersionedFile, scriptVersion, detectVersionPropertyName);
        scriptFiles.add(createdFile);
    }

    private String generateDetectVersionPropertyName(final int detectMajorVersion) {
        return String.format("DETECT_LATEST_%s", detectMajorVersion);
    }

    private String generateScriptFilename(final String scriptExtension, int detectMajorVersion) {
        if (detectMajorVersion == 6) {
            // The original scripts must stay on Detect 6 forever
            return String.format("detect.%s", scriptExtension);
        } else {
            // Newer script versions have name that match their Detect major version number
            return String.format("detect%d.%s", detectMajorVersion, scriptExtension);
        }
    }

    private String generatedVersionedScriptFilename(String scriptVersion, String scriptExtension, int detectMajorVersion) {
        if (detectMajorVersion == 6) {
            // The original scripts must stay on Detect 6 forever
            return String.format("detect-%s.%s", scriptVersion, scriptExtension);
        } else {
            // Newer script versions have name that match their Detect major version number
            return String.format("detect%d-%s.%s", detectMajorVersion, scriptVersion, scriptExtension);
        }
    }

    private File buildScript(final String scriptTemplateFileName, final File outputFile, final String scriptVersion, final String detectVersionPropertyName) throws IOException, IntegrationException {
        final String VERSION_TOKEN = "//SCRIPT_VERSION//";
        final String BUILD_DATE_TOKEN = "//BUILD_DATE//";
        final String MAJOR_VERSIONS_TOKEN = "//DETECT_MAJOR_VERSIONS//";
        final String DEFAULT_VERSION_KEY_TOKEN = "//DEFAULT_DETECT_VERSION_KEY//";

        String scriptContents = ResourceUtil.getResourceAsString(this.getClass(), "/" + scriptTemplateFileName, StandardCharsets.UTF_8);
        scriptContents = scriptContents.replaceAll(VERSION_TOKEN, scriptVersion);

        final Date date = Date.from(Instant.now());
        final DateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd");
        final String formattedDate = dateFormat.format(date);
        scriptContents = scriptContents.replaceAll(BUILD_DATE_TOKEN, formattedDate);

        final String artifactoryUrl = "https://sig-repo.synopsys.com/bds-integrations-release/com/synopsys/integration/synopsys-detect?properties";
        final List<String> detectPropertyTags = fetchDetectPropertyTags(artifactoryUrl);

        final String majorVersionsCommentBlock = formatDetectPropertyTags(detectPropertyTags);
        scriptContents = scriptContents.replace(MAJOR_VERSIONS_TOKEN, majorVersionsCommentBlock);

        // Lock this script's default artifactory property name to the given Detect major version
        scriptContents = scriptContents.replace(DEFAULT_VERSION_KEY_TOKEN, detectVersionPropertyName);

        outputFile.delete();
        FileUtils.write(outputFile, scriptContents, StandardCharsets.UTF_8);
        outputFile.setExecutable(true);

        return outputFile;
    }

    private String formatDetectPropertyTags(final List<String> detectPropertyTags) {
        final int MAX_COMMENT_CHARACTERS = 55;
        final StringBuilder result = new StringBuilder();

        StringBuilder line = new StringBuilder("#");
        for (int i = 0; i < detectPropertyTags.size(); i++) {
            final String tag = detectPropertyTags.get(i);
            String newText = " " + tag;

            final boolean nextTagExists = i + 1 < detectPropertyTags.size();
            if (nextTagExists) {
                newText += ",";
            }

            if (line.length() + newText.length() < MAX_COMMENT_CHARACTERS) {
                line.append(newText);
            } else {
                result.append(line);
                result.append(System.lineSeparator());
                line = new StringBuilder("#" + newText);
            }
        }

        if (StringUtils.isNotBlank(line.toString())) {
            result.append(line);
        }

        return result.toString();
    }

    private List<String> fetchDetectPropertyTags(final String artifactoryUrl) throws IOException, IntegrationException {
        final IntHttpClient intHttpClient = new IntHttpClient(logger, 200, true, ProxyInfo.NO_PROXY_INFO);
        final Request request = new Request.Builder().uri(artifactoryUrl).build();

        final List<String> propertyTags = new ArrayList<>();
        try (final Response response = intHttpClient.execute(request)) {
            final String responseContent = response.getContentString(StandardCharsets.UTF_8);
            final Pattern pattern = Pattern.compile("\"DETECT_LATEST.*?\"");
            final Matcher matcher = pattern.matcher(responseContent);

            while (matcher.find()) {
                final String tag = matcher.group();
                propertyTags.add(tag.replace("\"", ""));
            }
        }

        return propertyTags;
    }
}
