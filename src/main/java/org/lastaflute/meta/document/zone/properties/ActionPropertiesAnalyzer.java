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
package org.lastaflute.meta.document.zone.properties;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.lastaflute.meta.document.docmeta.TypeDocMeta;
import org.lastaflute.meta.document.parts.action.FormFieldNameAdjuster;
import org.lastaflute.meta.document.parts.annotation.MetaAnnotationArranger;
import org.lastaflute.meta.document.parts.type.MetaTypeNameAdjuster;
import org.lastaflute.meta.sourceparser.SourceParserReflector;

// package of this class should be under lastaflute but no fix for compatible
/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from ActionDocumentAnalyzer (2021/06/26 Saturday at ikspiari)
 */
public class ActionPropertiesAnalyzer {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    /** list of suppressed fields, e.g. enhanced fields by JaCoCo. */
    protected static final Set<String> SUPPRESSED_FIELD_SET;
    static {
        SUPPRESSED_FIELD_SET = DfCollectionUtil.newHashSet("$jacocoData");
    }

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The optional reflector of source parser, e.g. java parser. (NotNull, EmptyAllowed) */
    protected final OptionalThing<SourceParserReflector> sourceParserReflector;

    // properties fixed attribute(s)
    protected final Map<String, Type> genericParameterTypesMap; // read-only

    // parts
    protected final MetaAnnotationArranger metaAnnotationArranger;
    protected final MetaTypeNameAdjuster metaTypeNameAdjuster;
    protected final FormFieldNameAdjuster formFieldNameAdjuster;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ActionPropertiesAnalyzer(OptionalThing<SourceParserReflector> sourceParserReflector, Map<String, Type> genericParameterTypesMap,
            MetaAnnotationArranger metaAnnotationArranger, MetaTypeNameAdjuster metaTypeNameAdjuster,
            FormFieldNameAdjuster formFieldNameAdjuster) {
        this.sourceParserReflector = sourceParserReflector;

        this.genericParameterTypesMap = genericParameterTypesMap;

        this.metaAnnotationArranger = metaAnnotationArranger;
        this.metaTypeNameAdjuster = metaTypeNameAdjuster;
        this.formFieldNameAdjuster = formFieldNameAdjuster;
    }

    // ===================================================================================
    //                                                                  Analyze Properties
    //                                                                  ==================
    // for e.g. form type, return type, nested property type
    // this is recursive call point
    public List<TypeDocMeta> analyzeProperties(Class<?> propertyOwner, int depth) {
        if (depth < 0) {
            return DfCollectionUtil.newArrayList();
        }
        final Set<Field> fieldSet = extractWholeFieldSet(propertyOwner);
        return fieldSet.stream().filter(field -> { // also contains private fields and super's fields
            return !exceptsField(field);
        }).map(field -> { // #question can private fields be treated as property? by jflute
            return analyzePropertyField(propertyOwner, depth, field);
        }).collect(Collectors.toList());
    }

    protected Set<Field> extractWholeFieldSet(Class<?> propertyOwner) {
        final Set<Field> fieldSet = DfCollectionUtil.newLinkedHashSet();
        for (Class<?> targetClazz = propertyOwner; targetClazz != Object.class; targetClazz = targetClazz.getSuperclass()) {
            if (targetClazz == null) { // e.g. interface: MultipartFormFile
                break;
            }
            fieldSet.addAll(Arrays.asList(targetClazz.getDeclaredFields()));
        }
        return fieldSet;
    }

    protected boolean exceptsField(Field field) { // e.g. special field and static field
        return SUPPRESSED_FIELD_SET.contains(field.getName()) || Modifier.isStatic(field.getModifiers());
    }

    // ===================================================================================
    //                                                              Analyze Property Field
    //                                                              ======================
    protected TypeDocMeta analyzePropertyField(Class<?> propertyOwner, int depth, Field field) {
        final ActionPropertyFieldAnalyzer fieldAnalyzer = createActionPropertyFieldAnalyzer();
        return fieldAnalyzer.analyzePropertyField(propertyOwner, depth, field);
    }

    protected ActionPropertyFieldAnalyzer createActionPropertyFieldAnalyzer() {
        return new ActionPropertyFieldAnalyzer(sourceParserReflector, genericParameterTypesMap // basic
                , metaAnnotationArranger, metaTypeNameAdjuster, formFieldNameAdjuster // parts
                , this); // for recursive
    }
}
