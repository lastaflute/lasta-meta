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
package org.lastaflute.meta.agent.outputmeta;

import java.nio.file.Path;

import org.lastaflute.meta.generator.outputmeta.OutputMetaPhysical;

/**
 * @author jflute
 * @since 0.5.1 (2021/05/29 Saturday at ronppongi japanese)
 */
public class OutputMetaAgent { // precondition: current directory is project root

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final OutputMetaPhysical analyzedMetaPhysical = newAnalyzedMetaPhysical();

    protected OutputMetaPhysical newAnalyzedMetaPhysical() {
        return new OutputMetaPhysical();
    }

    // ===================================================================================
    //                                                                    Path Information
    //                                                                    ================
    public Path getLastaDocJsonPath() { // relative from project root
        return analyzedMetaPhysical.getLastaDocJsonPath();
    }

    public Path getSwaggerJsonPath() { // relative from project root
        return analyzedMetaPhysical.getSwaggerJsonPath();
    }

    public String getOutputMetaDir() { // precondition: current directory is project root
        return analyzedMetaPhysical.getOutputMetaDir();
    }
}
