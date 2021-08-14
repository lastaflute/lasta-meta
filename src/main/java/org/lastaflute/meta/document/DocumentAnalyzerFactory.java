/*
. * Copyright 2015-2021 the original author or authors.
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
package org.lastaflute.meta.document;

import java.util.List;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfReflectionUtil;
import org.dbflute.util.DfReflectionUtil.ReflectionFailureException;
import org.lastaflute.meta.sourceparser.SourceParserReflector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.6.9 (2075/03/05 Sunday)
 */
public class DocumentAnalyzerFactory {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    private static final Logger logger = LoggerFactory.getLogger(DocumentAnalyzerFactory.class);
    private static final String JOB_MANAGER_CLASS_NAME = "org.lastaflute.job.JobManager";

    // ===================================================================================
    //                                                                            Document
    //                                                                            ========
    public ActionDocumentAnalyzer createActionDocumentAnalyzer(List<String> srcDirList, int depth,
            OptionalThing<SourceParserReflector> sourceParserReflector) {
        return new ActionDocumentAnalyzer(srcDirList, depth, sourceParserReflector);
    }

    public OptionalThing<JobDocumentAnalyzer> createJobDocumentAnalyzer(List<String> srcDirList, int depth,
            OptionalThing<SourceParserReflector> sourceParserReflector) {
        final String className = JOB_MANAGER_CLASS_NAME;
        JobDocumentAnalyzer generator;
        try {
            DfReflectionUtil.forName(className); // confirm whether Lasta Job exists or not
            logger.debug("...Loading lasta job for document: {}", className);
            generator = new JobDocumentAnalyzer(srcDirList, depth, sourceParserReflector);
        } catch (ReflectionFailureException ignored) {
            generator = null;
        }

        return OptionalThing.ofNullable(generator, () -> {
            throw new IllegalStateException("Not found the lasta job: " + className);
        });
    }
}
