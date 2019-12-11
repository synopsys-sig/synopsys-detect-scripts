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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class Semver implements Comparable<Semver>{
    private int major;
    private int minor;
    private int patch;
    private String special;

    public static Optional<Semver> TryParse(String version) {
        //we will check that X and Y are purely numeric and that Z is numeric before it's first dash and contains only one dash.
        List<String> mustBeNumeric = new ArrayList<>();
        String[] pieces = version.split(Pattern.quote("."));
        if (pieces.length != 3) {
            return Optional.empty();
        }
        mustBeNumeric.add(pieces[0]);
        mustBeNumeric.add(pieces[1]);
        String finalPiece = pieces[2];
        String patchPiece;
        String special = "";
        if (finalPiece.contains("-")) {
            if (StringUtils.countMatches(finalPiece, "-") != 1) {
                return Optional.empty();
            } else {
                String[] finalPieces = finalPiece.split(Pattern.quote("-"));
                patchPiece = finalPieces[0];
                special = finalPieces[1];
            }
        } else {
            patchPiece = finalPiece;
        }
        mustBeNumeric.add(patchPiece);
        if (mustBeNumeric.stream().allMatch(StringUtils::isNumeric)) {
            int major = Integer.parseInt(pieces[0]);
            int minor = Integer.parseInt(pieces[1]);
            int patch = Integer.parseInt(patchPiece);
            return Optional.of(new Semver(major, minor, patch, special));
        } else {
            return Optional.empty();
        }
    }

    public Semver(final int major, final int minor, final int patch, final String special) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.special = special;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    public String getSpecial() {
        return special;
    }

    @Override
    public int compareTo(Semver v){
        return Comparator.comparingInt(Semver::getMajor)
                   .thenComparingInt(Semver::getMinor)
                   .thenComparingInt(Semver::getPatch)
                   .thenComparing(Semver::getSpecial)
                   .compare(this, v);
    }
}
