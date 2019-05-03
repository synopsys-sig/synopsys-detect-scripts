package com.synopsys.integration.synopsys.detect.scripts; /**
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
    public static void main(final String[] args) throws IOException, IntegrationException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Please provide the output directory of the scripts.");
        }
        final String outputDirectoryPath = args[0];
        final ScriptBuilder scriptBuilder = new ScriptBuilder();
        final File outputDirectory = new File(outputDirectoryPath);
        outputDirectory.mkdirs();
        scriptBuilder.generateScripts(outputDirectory);
    }

    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    public void generateScripts(final File outputDirectory) throws IOException, IntegrationException {
        final String version = ResourceUtil.getResourceAsString(this.getClass(), "/version.txt", StandardCharsets.UTF_8);
        final List<File> shellScriptFiles = generateScript(outputDirectory, "detect-sh.sh", "sh", version);
        final List<File> powershellScriptFiles = generateScript(outputDirectory, "detect-ps.ps1", "ps1", version);

        shellScriptFiles.forEach(this::logFileLocation);
        powershellScriptFiles.forEach(this::logFileLocation);
    }

    private void logFileLocation(final File file) {
        logger.info(String.format("Generated script at: %s", file.getAbsolutePath()));
    }

    public List<File> generateScript(final File outputDirectory, final String templateFileName, final String scriptExtension, final String scriptVersion) throws IOException, IntegrationException {
        final File shellScriptFile = new File(outputDirectory, String.format("detect.%s", scriptExtension));
        final File shellScriptVersionedFile = new File(outputDirectory, String.format("detect-%s.%s", scriptVersion, scriptExtension));
        final List<File> createdFiles = new ArrayList<>();

        if (!scriptVersion.contains("-SNAPSHOT")) {
            final File createdFile = buildScript(templateFileName, shellScriptFile, scriptVersion);
            createdFiles.add(createdFile);
        }

        final File createdFile = buildScript(templateFileName, shellScriptVersionedFile, scriptVersion);
        createdFiles.add(createdFile);

        return createdFiles;
    }

    private File buildScript(final String scriptTemplateFileName, final File outputFile, final String scriptVersion) throws IOException, IntegrationException {
        final String VERSION_TOKEN = "//SCRIPT_VERSION//";
        final String BUILD_DATE_TOKEN = "//BUILD_DATE//";
        final String MAJOR_VERSIONS_TOKEN = "//DETECT_MAJOR_VERSIONS//";

        String scriptContents = ResourceUtil.getResourceAsString(this.getClass(), "/" + scriptTemplateFileName, StandardCharsets.UTF_8);
        scriptContents = scriptContents.replaceAll(VERSION_TOKEN, scriptVersion);

        final Date date = Date.from(Instant.now());
        final DateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd");
        final String formattedDate = dateFormat.format(date);
        scriptContents = scriptContents.replaceAll(BUILD_DATE_TOKEN, formattedDate);

        // TODO: Remove oldArtifactoryUrl after 5.3.0 release of Detect
        final String oldArtifactoryUrl = "https://repo.blackducksoftware.com/artifactory/api/storage/bds-integrations-release/com/blackducksoftware/integration/hub-detect?properties";
        final String artifactoryUrl = "https://repo.blackducksoftware.com/artifactory/api/storage/bds-integrations-release/com/synopsys/integration/synopsys-detect?properties";
        List<String> detectPropertyTags = fetchDetectPropertyTags(artifactoryUrl);
        if (detectPropertyTags.isEmpty()) {
            detectPropertyTags = fetchDetectPropertyTags(oldArtifactoryUrl);
        }

        final String majorVersionsCommentBlock = formatDetectPropertyTags(detectPropertyTags);
        scriptContents = scriptContents.replace(MAJOR_VERSIONS_TOKEN, majorVersionsCommentBlock);

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
