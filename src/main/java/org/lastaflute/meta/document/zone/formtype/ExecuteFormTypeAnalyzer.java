/*
 * Copyright 2015-2024 the original author or authors.
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
package org.lastaflute.meta.document.zone.formtype;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.lastaflute.meta.document.docmeta.TypeDocMeta;
import org.lastaflute.meta.document.parts.action.FormFieldNameAdjuster;
import org.lastaflute.meta.document.parts.annotation.MetaAnnotationArranger;
import org.lastaflute.meta.document.parts.type.MetaTypeNameAdjuster;
import org.lastaflute.meta.document.zone.properties.ActionPropertiesAnalyzer;
import org.lastaflute.meta.sourceparser.SourceParserReflector;
import org.lastaflute.web.ruts.config.ActionExecute;

// package of this class should be under lastaflute but no fix for compatible
/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from ActionDocumentAnalyzer (2021/06/26 Saturday at roppongi japanese)
 */
public class ExecuteFormTypeAnalyzer {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** depth of analyzed target, to avoid cyclic analyzing. */
    protected final int depth;

    /** The optional reflector of source parser, e.g. java parser. (NotNull, EmptyAllowed) */
    protected final OptionalThing<SourceParserReflector> sourceParserReflector;

    protected final MetaAnnotationArranger metaAnnotationArranger;
    protected final MetaTypeNameAdjuster metaTypeNameAdjuster;
    protected final FormFieldNameAdjuster formFieldNameAdjuster;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ExecuteFormTypeAnalyzer(int depth, OptionalThing<SourceParserReflector> sourceParserReflector,
            MetaAnnotationArranger metaAnnotationArranger, MetaTypeNameAdjuster metaTypeNameAdjuster,
            FormFieldNameAdjuster formFieldNameAdjuster) {
        this.depth = depth;
        this.sourceParserReflector = sourceParserReflector;

        this.metaAnnotationArranger = metaAnnotationArranger;
        this.metaTypeNameAdjuster = metaTypeNameAdjuster;
        this.formFieldNameAdjuster = formFieldNameAdjuster;
    }

    // ===================================================================================
    //                                                                        Analyze Form
    //                                                                        ============
    public OptionalThing<TypeDocMeta> analyzeFormClass(ActionExecute execute) {
        return execute.getFormMeta().map(lastafluteFormMeta -> {
            final TypeDocMeta formDocMeta = new TypeDocMeta();
            lastafluteFormMeta.getListFormParameterParameterizedType().ifPresent(type -> {
                formDocMeta.setType(lastafluteFormMeta.getSymbolFormType());
                formDocMeta.setTypeName(adjustTypeName(type));
                formDocMeta.setSimpleTypeName(adjustSimpleTypeName(type));
            }).orElse(() -> {
                formDocMeta.setType(lastafluteFormMeta.getSymbolFormType());
                formDocMeta.setTypeName(adjustTypeName(lastafluteFormMeta.getSymbolFormType()));
                formDocMeta.setSimpleTypeName(adjustSimpleTypeName(lastafluteFormMeta.getSymbolFormType()));
            });
            final Class<?> formType = lastafluteFormMeta.getListFormParameterGenericType().orElse(lastafluteFormMeta.getSymbolFormType());
            // #thinking jflute form does not have nest as generic? (2021/06/26)
            final Map<String, Type> genericParameterTypesMap = DfCollectionUtil.emptyMap();
            final List<TypeDocMeta> propertyDocMetaList = analyzeProperties(genericParameterTypesMap, formType);
            formDocMeta.setNestTypeDocMetaList(propertyDocMetaList);
            sourceParserReflector.ifPresent(sourceParserReflector -> {
                sourceParserReflector.reflect(formDocMeta, formType);
            });
            return formDocMeta;
        });
    }

    // -----------------------------------------------------
    //                                    Analyze Properties
    //                                    ------------------
    protected List<TypeDocMeta> analyzeProperties(Map<String, Type> genericParameterTypesMap, Class<?> propertyOwner) {
        final ActionPropertiesAnalyzer propertiesAnalyzer = createActionPropertiesAnalyzer(genericParameterTypesMap);
        return propertiesAnalyzer.analyzeProperties(propertyOwner, depth);
    }

    protected ActionPropertiesAnalyzer createActionPropertiesAnalyzer(Map<String, Type> genericParameterTypesMap) {
        return new ActionPropertiesAnalyzer(sourceParserReflector, genericParameterTypesMap // basic
                , metaAnnotationArranger, metaTypeNameAdjuster, formFieldNameAdjuster); // parts
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
