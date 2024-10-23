/*
 * synopsys-detect-scripts
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.detect.scripts.scripts;

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

import javax.annotation.Nullable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.log.IntLogger;
import com.blackduck.integration.log.Slf4jIntLogger;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.rest.client.IntHttpClient;
import com.blackduck.integration.rest.proxy.ProxyInfo;
import com.blackduck.integration.rest.request.Request;
import com.blackduck.integration.rest.response.Response;
import com.blackduck.integration.util.ResourceUtil;

public class ScriptBuilder {
    // When moving to a new Detect major version:
    // 1. Add the pair of scripts (.sh and .ps1) for the CURRENT major to src/main/resources/earlierversions
    // 2. Update DETECT_MAJOR_VERSION to the NEW major version.
    private static final int DETECT_MAJOR_VERSION = 10;

    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    public void generateScripts(final File outputDirectory) throws IOException, IntegrationException {
        final String scriptVersion = ResourceUtil.getResourceAsString(this.getClass(), "/version.txt", StandardCharsets.UTF_8);
        final List<File> scriptFiles = new ArrayList<>();
        generateScript(scriptFiles, outputDirectory, "detect-sh.sh", "sh", scriptVersion, DETECT_MAJOR_VERSION);
        generateScript(scriptFiles, outputDirectory, "detect-ps.ps1", "ps1", scriptVersion, DETECT_MAJOR_VERSION);

        File dir = new File("src/main/resources/earlierversions");
        for (File nextFile : dir.listFiles()) {
            FileUtils.copyFileToDirectory(nextFile, outputDirectory);
        }
        scriptFiles.forEach(this::logFileLocation);
    }

    private void logFileLocation(final File file) {
        logger.info(String.format("Generated script at: %s", file.getAbsolutePath()));
    }

    public void generateScript(List<File> scriptFiles, final File outputDirectory, final String templateFileName, final String scriptExtension, final String scriptVersion, int detectMajorVersion) throws IOException, IntegrationException {
        final File shellScriptVersionlessFile = new File(outputDirectory, generateVersionlessScriptFilename(scriptExtension, detectMajorVersion));
        final File shellScriptVersionedFile = new File(outputDirectory, generateVersionedScriptFilename(scriptVersion, scriptExtension, detectMajorVersion));

        String detectVersionPropertyName = generateDetectVersionPropertyName(detectMajorVersion);
        if (!scriptVersion.contains("-SNAPSHOT")) {
            final File createdFile = buildScript(templateFileName, shellScriptVersionlessFile, scriptVersion, detectVersionPropertyName);
            scriptFiles.add(createdFile);
        }

        final File createdFile = buildScript(templateFileName, shellScriptVersionedFile, scriptVersion, detectVersionPropertyName);
        scriptFiles.add(createdFile);
    }

    private String generateDetectVersionPropertyName(final int detectMajorVersion) {
        return String.format("DETECT_LATEST_%s", detectMajorVersion);
    }

    private String generateVersionlessScriptFilename(final String scriptExtension, int detectMajorVersion) {
        return generateScriptFilename(detectMajorVersion, scriptExtension, null);
    }

    private String generateVersionedScriptFilename(String scriptVersion, String scriptExtension, int detectMajorVersion) {
        return generateScriptFilename(detectMajorVersion, scriptExtension, scriptVersion);
    }

    private String generateScriptFilename(int detectMajorVersion, String scriptExtension, @Nullable String scriptVersion) {
        StringBuilder sb = new StringBuilder();
        sb.append("detect");
        sb.append(detectMajorVersion);
        if (scriptVersion != null) {
            sb.append("-");
            sb.append(scriptVersion);
        }
        sb.append(".");
        sb.append(scriptExtension);
        return sb.toString();
    }

    private File buildScript(final String scriptTemplateFileName, final File outputFile, final String scriptVersion, final String detectVersionPropertyName) throws IOException, IntegrationException {
        final String VERSION_TOKEN = "//SCRIPT_VERSION//";
        final String BUILD_DATE_TOKEN = "//BUILD_DATE//";
        final String MAJOR_VERSIONS_TOKEN = "//DETECT_MAJOR_VERSIONS//";
        final String DEFAULT_VERSION_KEY_TOKEN = "//DEFAULT_DETECT_VERSION_KEY//";

        String scriptContents = ResourceUtil.getResourceAsString(this.getClass(), "/templates/" + scriptTemplateFileName, StandardCharsets.UTF_8);
        scriptContents = scriptContents.replaceAll(VERSION_TOKEN, scriptVersion);

        final Date date = Date.from(Instant.now());
        final DateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd");
        final String formattedDate = dateFormat.format(date);
        scriptContents = scriptContents.replaceAll(BUILD_DATE_TOKEN, formattedDate);

        final String artifactoryUrl = "https://repo.blackduck.com/bds-integrations-release/com/blackduck/integration/detect?properties";
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
        final IntHttpClient intHttpClient = new IntHttpClient(logger, new Gson(), 200, true, ProxyInfo.NO_PROXY_INFO);
        final Request request = new Request.Builder(new HttpUrl(artifactoryUrl)).build();

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
