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
package org.lastaflute.meta.document.outputmeta;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author jflute
 * @since 0.5.1 (2021/05/30 Sunday)
 */
public class OutputMetaSerializer { // precondition: current directory is project root

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final OutputMetaPhysical analyzedMetaPhysical = newAnalyzedMetaPhysical();

    protected OutputMetaPhysical newAnalyzedMetaPhysical() {
        return new OutputMetaPhysical();
    }

    // ===================================================================================
    //                                                                           Save Meta
    //                                                                           =========
    public void saveLastaDocMeta(String json) {
        doSaveOutputMeta(json, analyzedMetaPhysical.getLastaDocJsonPath());
    }

    public void saveSwaggerMeta(String json) {
        doSaveOutputMeta(json, analyzedMetaPhysical.getSwaggerJsonPath());
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
}
