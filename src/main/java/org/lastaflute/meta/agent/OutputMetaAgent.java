/*
 * Copyright 2015-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.lastaflute.meta.agent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author jflute
 * @since 0.5.1 (2021/05/29 Saturday at ronppongi japanese)
 */
public class OutputMetaAgent { // precondition: current directory is project root

    // ===================================================================================
    //                                                                           Save Meta
    //                                                                           =========
    public void saveLastaDocMeta(String json) {
        doSaveOutputMeta(json, getLastaDocJsonPath());
    }

    public void saveSwaggerMeta(String json) {
        doSaveOutputMeta(json, getSwaggerJsonPath());
    }

    protected void doSaveOutputMeta(String json, Path path) {
        if (json == null) {
            throw new IllegalArgumentException("The argument 'json' should not be null.");
        }
        final Path parentPath = path.getParent();
        if (!Files.exists(parentPath)) {
            try {
                Files.createDirectories(parentPath);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create directory: " + parentPath, e);
            }
        }

        try (BufferedWriter bw = Files.newBufferedWriter(path, Charset.forName("UTF-8"))) {
            bw.write(json);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write the json to the file: " + path, e);
        }
    }

    // ===================================================================================
    //                                                                    Path Information
    //                                                                    ================
    public Path getLastaDocJsonPath() { // relative from project root
        return Paths.get(getOutputMetaDir(), "analyzed-lastadoc.json");
    }

    public Path getSwaggerJsonPath() { // relative from project root
        return Paths.get(getOutputMetaDir(), "swagger.json");
    }

    public String getOutputMetaDir() { // precondition: current directory is project root
        if (new File("./pom.xml").exists()) {
            return "./target/lastadoc/";
        }
        return "./build/lastadoc/"; // for e.g. Gradle
    }
}
