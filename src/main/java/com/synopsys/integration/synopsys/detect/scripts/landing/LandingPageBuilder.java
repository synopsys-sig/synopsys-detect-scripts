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
package com.synopsys.integration.synopsys.detect.scripts.landing;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.rest.client.IntHttpClient;
import com.synopsys.integration.rest.proxy.ProxyInfo;
import com.synopsys.integration.rest.request.Request;
import com.synopsys.integration.rest.request.Response;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

//Generates the pages /doc/index.html and /docs/index.html from the freemarker templates in resources.
public class LandingPageBuilder {
    private final IntLogger logger = new Slf4jIntLogger(LoggerFactory.getLogger(this.getClass()));

    public void buildLandingPages(File output, Configuration configuration) throws IOException, TemplateException, IntegrationException {
        File doc = new File(output, "doc");
        File docs = new File(output, "docs");

        doc.mkdirs();
        docs.mkdirs();

        File docIndex = new File(doc, "index.html");
        File docsIndex = new File(docs, "index.html");

        Template docTemplate = configuration.getTemplate("doc.ftl");
        Template docsTemplate = configuration.getTemplate("docs.ftl");

        List<String> paths = fetchDocumentListing();
        List<DetectVersionEntry> entries = parseVersionsFromDocumentListing(paths);
        DetectVersionSet versions = sortVersion(entries);
        DetectVersionEntry version = versions.getReleased().get(0);

        try (FileWriter writer = new FileWriter(docIndex)) {
            docTemplate.process(version, writer);
        }

        try (FileWriter writer = new FileWriter(docsIndex)) {
            docsTemplate.process(versions, writer);
        }
    }

    public List<String> parseDocumentListing(String jsonText) {
        final List<String> propertyTags = new ArrayList<>();
        Gson gson = new Gson();
        JsonElement json = gson.fromJson(jsonText, JsonElement.class);
        for (JsonElement entry : json.getAsJsonArray()){
            String path = entry.getAsJsonObject().get("path").getAsString();
            propertyTags.add(path);
        }
        return propertyTags;
    }

    private List<String> fetchDocumentListing() throws IntegrationException, IOException {
        final IntHttpClient intHttpClient = new IntHttpClient(logger, 200, true, ProxyInfo.NO_PROXY_INFO);
        final Request request = new Request.Builder().uri("https://api.github.com/repos/blackducksoftware/synopsys-detect/contents?ref=gh-pages").build();


        try (final Response response = intHttpClient.execute(request)) {
            final String responseContent = response.getContentString(StandardCharsets.UTF_8);
            return parseDocumentListing(responseContent);
        }
    }

    public DetectVersionSet sortVersion(List<DetectVersionEntry> entries) {
        List<DetectVersionEntry> released = new ArrayList<>();
        List<DetectVersionEntry> snapshot = new ArrayList<>();
        for (DetectVersionEntry entry : entries) {
            if (StringUtils.isEmpty(entry.getSemver().getSpecial())) {
                released.add(entry);
            } else {
                snapshot.add(entry);
            }
        }
        Collections.sort(released);
        Collections.sort(snapshot);
        Collections.reverse(released);
        Collections.reverse(snapshot);
        return  new DetectVersionSet(released, snapshot);
    }

    public List<DetectVersionEntry> parseVersionsFromDocumentListing(List<String> paths) {
        return paths.stream()
                   .map(this::parseVersionFromPath)
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .collect(Collectors.toList());
    }

    public Optional<DetectVersionEntry> parseVersionFromPath(String path){
        String base = "https://blackducksoftware.github.io/synopsys-detect/";
        if (path.startsWith("synopsys-detect-") && path.endsWith("-help.html")) {
            String version = path.replaceAll("synopsys-detect-", "").replaceAll("-help.html", "");
            String url = base + path;
            return Optional.of(new DetectVersionEntry(version, url, Semver.TryParse(version).get()));
        } else {
            Optional<Semver> semver = Semver.TryParse(path);
            return semver.map(version -> new DetectVersionEntry(path, base + path, version));
        }
    }
}

