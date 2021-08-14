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
package org.lastaflute.meta.document.zone.returntype;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfReflectionUtil;
import org.lastaflute.meta.document.docmeta.TypeDocMeta;
import org.lastaflute.meta.document.parts.action.FormFieldNameAdjuster;
import org.lastaflute.meta.document.parts.annotation.MetaAnnotationArranger;
import org.lastaflute.meta.document.parts.type.MetaTypeNameAdjuster;
import org.lastaflute.meta.document.parts.type.NativeDataTypeProvider;
import org.lastaflute.meta.document.zone.properties.ActionPropertiesAnalyzer;
import org.lastaflute.meta.sourceparser.SourceParserReflector;
import org.lastaflute.meta.util.LaDocReflectionUtil;

// package of this class should be under lastaflute but no fix for compatible
/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from ActionDocumentAnalyzer (2021/06/26 Saturday at ikspiari)
 */
public class ExecuteReturnTypeAnalyzer {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    /** list of suppressed fields, e.g. enhanced fields by JaCoCo. */
    protected static final Set<String> SUPPRESSED_FIELD_SET;
    static {
        SUPPRESSED_FIELD_SET = DfCollectionUtil.newHashSet("$jacocoData");
    }

    protected static final List<String> TARGET_SUFFIX_LIST;
    static {
        TARGET_SUFFIX_LIST = Arrays.asList("Form", "Body", "Bean", "Result");
    }

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** depth of analyzed target, to avoid cyclic analyzing. */
    protected final int depth;

    /** The optional reflector of source parser, e.g. java parser. (NotNull, EmptyAllowed) */
    protected final OptionalThing<SourceParserReflector> sourceParserReflector;

    protected final MetaAnnotationArranger metaAnnotationArranger;
    protected final MetaTypeNameAdjuster metaTypeNameAdjuster;
    protected final NativeDataTypeProvider nativeDataTypeProvider;
    protected final FormFieldNameAdjuster formFieldNameAdjuster;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ExecuteReturnTypeAnalyzer(int depth, OptionalThing<SourceParserReflector> sourceParserReflector,
            MetaAnnotationArranger metaAnnotationArranger, MetaTypeNameAdjuster metaTypeNameAdjuster,
            NativeDataTypeProvider nativeDataTypeProvider, FormFieldNameAdjuster formFieldNameAdjuster) {
        this.depth = depth;
        this.sourceParserReflector = sourceParserReflector;

        this.metaAnnotationArranger = metaAnnotationArranger;
        this.metaTypeNameAdjuster = metaTypeNameAdjuster;
        this.nativeDataTypeProvider = nativeDataTypeProvider;
        this.formFieldNameAdjuster = formFieldNameAdjuster;
    }

    // ===================================================================================
    //                                                                      Analyze Return
    //                                                                      ==============
    public TypeDocMeta analyzeReturnClass(Method method) {
        final TypeDocMeta returnDocMeta = new TypeDocMeta();
        returnDocMeta.setType(method.getReturnType());
        returnDocMeta.setTypeName(adjustTypeName(method.getGenericReturnType()));
        returnDocMeta.setSimpleTypeName(adjustSimpleTypeName(method.getGenericReturnType()));
        returnDocMeta.setGenericType(DfReflectionUtil.getGenericFirstClass(method.getGenericReturnType()));
        returnDocMeta.setAnnotationTypeList(Arrays.asList(method.getAnnotatedReturnType().getAnnotations()));
        returnDocMeta.setAnnotationList(metaAnnotationArranger.arrangeAnnotationList(returnDocMeta.getAnnotationTypeList()));
        derivedManualReturnClass(method, returnDocMeta);

        Class<?> returnClass = returnDocMeta.getGenericType();
        if (returnClass != null) { // e.g. List<String>, Sea<Land>
            // TODO p1us2er0 optimisation, generic handling in analyzeReturnClass() (2015/09/30)
            final Map<String, Type> genericParameterTypesMap = DfCollectionUtil.newLinkedHashMap();
            final Type[] parameterTypes = DfReflectionUtil.getGenericParameterTypes(method.getGenericReturnType());
            final TypeVariable<?>[] typeVariables = returnClass.getTypeParameters();
            IntStream.range(0, parameterTypes.length).forEach(parameterTypesIndex -> {
                final Type[] genericParameterTypes = DfReflectionUtil.getGenericParameterTypes(parameterTypes[parameterTypesIndex]);
                IntStream.range(0, typeVariables.length).forEach(typeVariablesIndex -> {
                    Type type = genericParameterTypes[typeVariablesIndex];
                    genericParameterTypesMap.put(typeVariables[typeVariablesIndex].getTypeName(), type);
                });
            });

            if (Iterable.class.isAssignableFrom(returnClass)) { // e.g. List<String>, List<Sea<Land>>
                returnClass = LaDocReflectionUtil.extractElementType(method.getGenericReturnType(), 1);
            }
            final List<Class<? extends Object>> nativeClassList = nativeDataTypeProvider.provideNativeDataTypeList();
            if (returnClass != null && !nativeClassList.contains(returnClass)) {
                final List<TypeDocMeta> propertyDocMetaList =
                        analyzeProperties(Collections.unmodifiableMap(genericParameterTypesMap), returnClass);
                returnDocMeta.setNestTypeDocMetaList(propertyDocMetaList);
            }

            if (sourceParserReflector.isPresent()) {
                sourceParserReflector.get().reflect(returnDocMeta, returnClass);
            }
        }

        return returnDocMeta;
    }

    protected void derivedManualReturnClass(Method method, TypeDocMeta returnDocMeta) {
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
