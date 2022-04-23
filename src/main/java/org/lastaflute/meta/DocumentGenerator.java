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
package org.lastaflute.meta;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.lastaflute.core.json.control.JsonControlMeta;
import org.lastaflute.core.json.engine.RealJsonEngine;
import org.lastaflute.meta.document.ActionDocumentAnalyzer;
import org.lastaflute.meta.document.DocumentAnalyzerFactory;
import org.lastaflute.meta.document.JobDocumentAnalyzer;
import org.lastaflute.meta.document.docmeta.ActionDocMeta;
import org.lastaflute.meta.document.outputmeta.OutputMetaSerializer;
import org.lastaflute.meta.infra.json.MetauseJsonEngineProvider;
import org.lastaflute.meta.sourceparser.SourceParserReflector;
import org.lastaflute.meta.sourceparser.SourceParserReflectorFactory;

// package of this class should be under lastaflute but no fix for compatible
/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.0-sp9 (2015/09/18 Friday)
 */
public class DocumentGenerator {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    /** The default source directory to search actions and jobs. */
    protected static final String DEFAULT_SRC_DIR = "src/main/java/"; // trailing slash needed?

    /** The default depth to search nest world. */
    protected static final int DEFAULT_DEPTH = 4;

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // -----------------------------------------------------
    //                                                 Basic
    //                                                 -----
    /** The list of source directories to search actions and jobs. (NotNull) */
    protected final List<String> srcDirList;

    /** The depth to search nest world. (NotMinus) */
    protected int depth;

    /** The optional reflector of source parser, e.g. java parser. (NotNull, EmptyAllowed) */
    protected final OptionalThing<SourceParserReflector> sourceParserReflector;

    // -----------------------------------------------------
    //                                                Option
    //                                                ------
    /** Does it suppress job document generation? */
    protected boolean jobDocSuppressed; // for e.g. heavy scheduling (using e.g. DB) like Fess

    // -----------------------------------------------------
    //                                                 Parts
    //                                                 -----
    protected final DocumentAnalyzerFactory documentAnalyzerFactory = newDocumentGeneratorFactory();

    protected DocumentAnalyzerFactory newDocumentGeneratorFactory() {
        return new DocumentAnalyzerFactory();
    }

    protected final MetauseJsonEngineProvider metauseJsonEngineProvider = newMetauseJsonEngineProvider();

    protected MetauseJsonEngineProvider newMetauseJsonEngineProvider() {
        return new MetauseJsonEngineProvider();
    }

    protected final OutputMetaSerializer outputMetaSerializer = newOutputMetaSerializer();

    protected OutputMetaSerializer newOutputMetaSerializer() {
        return new OutputMetaSerializer();
    }

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public DocumentGenerator() { // basically LastaFlute libraries use this
        this.srcDirList = prepareDefaultSrcDirList();
        this.depth = DEFAULT_DEPTH;
        this.sourceParserReflector = prepareSourceParserReflector(this.srcDirList);
    }

    protected List<String> prepareDefaultSrcDirList() {
        // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
        // e.g.
        //  this project's src/main/java
        //  the common's src/main/java
        //
        // basically LastaFlute projects have the common project if multi project
        // _/_/_/_/_/_/_/_/_/_/
        final List<String> srcDirList = DfCollectionUtil.newArrayList();
        srcDirList.add(DEFAULT_SRC_DIR);
        final String projectDirName = new File(".").getAbsoluteFile().getParentFile().getName();
        final String commonDir = "../" + projectDirName.replaceAll("-.*", "-common") + "/" + DEFAULT_SRC_DIR;
        if (new File(commonDir).exists()) {
            srcDirList.add(commonDir);
        }
        return srcDirList;
    }

    // for when default src directory is unneeded, e.g. not src/main/java
    public DocumentGenerator(List<String> srcDirList) {
        this.srcDirList = srcDirList;
        this.depth = DEFAULT_DEPTH;
        this.sourceParserReflector = prepareSourceParserReflector(srcDirList);
    }

    protected OptionalThing<SourceParserReflector> prepareSourceParserReflector(List<String> srcDirList) {
        return createSourceParserReflectorFactory().reflector(srcDirList);
    }

    protected SourceParserReflectorFactory createSourceParserReflectorFactory() {
        return new SourceParserReflectorFactory();
    }

    // ===================================================================================
    //                                                                              Option
    //                                                                              ======
    public void addSrcDir(String srcDir) { // important for e.g. application library project
        srcDirList.add(srcDir);
    }

    public void setDepth(int depth) { // to change default depth
        this.depth = depth;
    }

    public DocumentGenerator suppressJobDoc() {
        jobDocSuppressed = true;
        return this;
    }

    // ===================================================================================
    //                                                                         Action Meta
    //                                                                         ===========
    // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
    // [call hierarchy] (2022/04/23) *should be fixed when big refactoring
    //
    // DocumentGenerator@saveLastaDocMeta()    // basically [App]LastaDocTest calls
    //  |-ActionDocumentAnalyzer               // for action information
    //  |-JobDocumentAnalyzer                  // for job information (if lasta-job exists)
    //  |-MetauseJsonEngineProvider            // for json parser
    //  |-OutputMetaSerializer                 // makes swagger.json
    // _/_/_/_/_/_/_/_/_/_/
    public void saveLastaDocMeta() {
        final Map<String, Object> lastaMetaDetailMap = generateLastaDetailMap();
        final String json = createJsonEngine().toJson(lastaMetaDetailMap);
        outputMetaSerializer.saveLastaDocMeta(json);
    }

    protected Map<String, Object> generateLastaDetailMap() {
        final List<ActionDocMeta> actionDocMetaList = createActionDocumentAnalyzer().analyzeAction();
        final Map<String, Object> lastaMetaDetailMap = DfCollectionUtil.newLinkedHashMap();
        lastaMetaDetailMap.put("actionDocMetaList", actionDocMetaList);
        createJobDocumentAnalyzer().ifPresent(jobDocumentGenerator -> {
            lastaMetaDetailMap.put("jobDocMetaList", jobDocumentGenerator.analyzeJobDocMetaList());
        });
        return lastaMetaDetailMap;
    }

    // ===================================================================================
    //                                                                   Document Analyzer
    //                                                                   =================
    public ActionDocumentAnalyzer createActionDocumentAnalyzer() { // also called by e.g. swagger
        return documentAnalyzerFactory.createActionDocumentAnalyzer(srcDirList, depth, sourceParserReflector);
    }

    protected OptionalThing<JobDocumentAnalyzer> createJobDocumentAnalyzer() {
        if (jobDocSuppressed) {
            return OptionalThing.empty();
        }
        return documentAnalyzerFactory.createJobDocumentAnalyzer(srcDirList, depth, sourceParserReflector);
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected RealJsonEngine createJsonEngine() {
        return metauseJsonEngineProvider.createJsonEngine();
    }

    protected JsonControlMeta getAppJsonControlMeta() {
        return metauseJsonEngineProvider.getAppJsonControlMeta();
    }
}
