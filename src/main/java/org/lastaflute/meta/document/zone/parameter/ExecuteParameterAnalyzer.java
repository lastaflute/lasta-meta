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
package org.lastaflute.meta.document.zone.parameter;

import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfReflectionUtil;
import org.lastaflute.meta.document.docmeta.TypeDocMeta;
import org.lastaflute.meta.document.parts.annotation.MetaAnnotationArranger;
import org.lastaflute.meta.document.parts.type.MetaTypeNameAdjuster;
import org.lastaflute.meta.sourceparser.SourceParserReflector;

// package of this class should be under lastaflute but no fix for compatible
/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from ActionDocumentAnalyzer (2021/06/26 Saturday at roppongi japanese)
 */
public class ExecuteParameterAnalyzer {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The optional reflector of source parser, e.g. java parser. (NotNull, EmptyAllowed) */
    protected final OptionalThing<SourceParserReflector> sourceParserReflector;

    protected final MetaAnnotationArranger metaAnnotationArranger;
    protected final MetaTypeNameAdjuster metaTypeNameAdjuster;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ExecuteParameterAnalyzer(OptionalThing<SourceParserReflector> sourceParserReflector,
            MetaAnnotationArranger metaAnnotationArranger, MetaTypeNameAdjuster metaTypeNameAdjuster) {
        this.sourceParserReflector = sourceParserReflector;

        this.metaAnnotationArranger = metaAnnotationArranger;
        this.metaTypeNameAdjuster = metaTypeNameAdjuster;
    }

    // ===================================================================================
    //                                                                   Analyze Parameter
    //                                                                   =================
    public TypeDocMeta analyzeMethodParameter(Parameter parameter) {
        final TypeDocMeta parameterDocMeta = new TypeDocMeta();
        parameterDocMeta.setName(parameter.getName());
        parameterDocMeta.setPublicName(parameter.getName());
        parameterDocMeta.setType(parameter.getType());
        parameterDocMeta.setTypeName(adjustTypeName(parameter.getParameterizedType()));
        parameterDocMeta.setSimpleTypeName(adjustSimpleTypeName(parameter.getParameterizedType()));
        if (OptionalThing.class.isAssignableFrom(parameter.getType())) {
            parameterDocMeta.setGenericType(DfReflectionUtil.getGenericFirstClass(parameter.getParameterizedType()));
        }
        parameterDocMeta.setAnnotationTypeList(Arrays.asList(parameter.getAnnotatedType().getAnnotations()));
        parameterDocMeta.setAnnotationList(metaAnnotationArranger.arrangeAnnotationList(parameterDocMeta.getAnnotationTypeList()));
        parameterDocMeta.setNestTypeDocMetaList(Collections.emptyList());
        sourceParserReflector.ifPresent(sourceParserReflector -> {
            sourceParserReflector.reflect(parameterDocMeta, parameter.getType());
        });
        return parameterDocMeta;
    }

    // ===================================================================================
    //                                                                     Adjust TypeName
    //                                                                     ===============
    protected String adjustTypeName(Type type) {
        return metaTypeNameAdjuster.adjustTypeName(type);
    }

    protected String adjustTypeName(String typeName) {
        return metaTypeNameAdjuster.adjustTypeName(typeName);
    }

    protected String adjustSimpleTypeName(Type type) {
        return metaTypeNameAdjuster.adjustSimpleTypeName(type);
    }

    protected String adjustSimpleTypeName(String typeName) {
        return metaTypeNameAdjuster.adjustSimpleTypeName(typeName);
    }
}
