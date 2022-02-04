/*
 * Copyright 2015-2022 the original author or authors.
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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author jflute
 * @since 0.5.1 (2021/05/30 Sunday)
 */
public class OutputMetaPhysical { // precondition: current directory is project root

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
