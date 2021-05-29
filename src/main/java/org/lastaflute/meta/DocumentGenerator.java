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
package org.lastaflute.meta;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.lastaflute.core.json.JsonMappingOption;
import org.lastaflute.core.json.engine.RealJsonEngine;
import org.lastaflute.meta.agent.outputmeta.OutputMetaAgent;
import org.lastaflute.meta.generator.ActionDocumentGenerator;
import org.lastaflute.meta.generator.DocumentGeneratorFactory;
import org.lastaflute.meta.generator.JobDocumentGenerator;
import org.lastaflute.meta.meta.ActionDocMeta;
import org.lastaflute.meta.reflector.SourceParserReflector;
import org.lastaflute.meta.reflector.SourceParserReflectorFactory;

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
    /** source directory. */
    protected static final String SRC_DIR = "src/main/java/";

    /** depth. */
    protected static final int DEPTH = 4;

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** source directory. (NotNull) */
    protected final List<String> srcDirList;

    /** depth. */
    protected int depth;

    /** The optional reflector of source parser, e.g. java parser. (NotNull, EmptyAllowed) */
    protected final OptionalThing<SourceParserReflector> sourceParserReflector;

    /** Does it suppress job document generation? */
    protected boolean jobDocSuppressed; // for e.g. heavy scheduling (using e.g. DB) like Fess

    protected final OutputMetaAgent outputMetaAgent = newOutputMetaAgent();

    protected OutputMetaAgent newOutputMetaAgent() {
        return new OutputMetaAgent();
    }

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public DocumentGenerator() {
        this.srcDirList = prepareDefaultSrcDirList();
        this.depth = DEPTH;
        this.sourceParserReflector = createSourceParserReflectorFactory().reflector(srcDirList);
    }

    protected List<String> prepareDefaultSrcDirList() {
        final List<String> srcDirList = DfCollectionUtil.newArrayList();
        srcDirList.add(SRC_DIR);
        final String projectDirName = new File(".").getAbsoluteFile().getParentFile().getName();
        final String commonDir = "../" + projectDirName.replaceAll("-.*", "-common") + "/" + SRC_DIR;
        if (new File(commonDir).exists()) {
            srcDirList.add(commonDir);
        }
        return srcDirList;
    }

    public DocumentGenerator(List<String> srcDirList) {
        this.srcDirList = srcDirList;
        this.depth = DEPTH;
        this.sourceParserReflector = createSourceParserReflectorFactory().reflector(srcDirList);
    }

    protected SourceParserReflectorFactory createSourceParserReflectorFactory() {
        return new SourceParserReflectorFactory();
    }

    protected DocumentGeneratorFactory createDocumentGeneratorFactory() {
        return new DocumentGeneratorFactory();
    }

    // ===================================================================================
    //                                                                              Option
    //                                                                              ======
    public void addSrcDir(String srcDir) {
        srcDirList.add(srcDir);
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public DocumentGenerator suppressJobDoc() {
        jobDocSuppressed = true;
        return this;
    }

    // ===================================================================================
    //                                                                         Action Meta
    //                                                                         ===========
    public void saveLastaDocMeta() {
        final Map<String, Object> lastaMetaDetailMap = generateLastaDetailMap();
        final String json = createJsonEngine().toJson(lastaMetaDetailMap);
        outputMetaAgent.saveLastaDocMeta(json);
    }

    protected Map<String, Object> generateLastaDetailMap() {
        final List<ActionDocMeta> actionDocMetaList = createActionDocumentGenerator().generateActionDocMetaList();
        final Map<String, Object> lastaMetaDetailMap = DfCollectionUtil.newLinkedHashMap();
        lastaMetaDetailMap.put("actionDocMetaList", actionDocMetaList);
        createJobDocumentGenerator().ifPresent(jobDocumentGenerator -> {
            lastaMetaDetailMap.put("jobDocMetaList", jobDocumentGenerator.generateJobDocMetaList());
        });
        return lastaMetaDetailMap;
    }

    protected ActionDocumentGenerator createActionDocumentGenerator() {
        return createDocumentGeneratorFactory().createActionDocumentGenerator(srcDirList, depth, sourceParserReflector);
    }

    protected OptionalThing<JobDocumentGenerator> createJobDocumentGenerator() {
        if (jobDocSuppressed) {
            return OptionalThing.empty();
        }
        return createDocumentGeneratorFactory().createJobDocumentGenerator(srcDirList, depth, sourceParserReflector);
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    public RealJsonEngine createJsonEngine() {
        return createDocumentGeneratorFactory().createJsonEngine();
    }

    public OptionalThing<JsonMappingOption> getApplicationJsonMappingOption() {
        return createDocumentGeneratorFactory().getApplicationJsonMappingOption();
    }
}
