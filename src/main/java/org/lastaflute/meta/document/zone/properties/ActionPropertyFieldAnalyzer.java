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
package org.lastaflute.meta.document.zone.properties;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dbflute.jdbc.Classification;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfReflectionUtil;
import org.dbflute.util.DfReflectionUtil.ReflectionFailureException;
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
public class ActionPropertyFieldAnalyzer {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    protected static final List<String> ROOT_TARGET_SUFFIX_LIST;
    static {
        // basically Part is defined as inner class however we cannot help allowing independent file
        // actually the case exists in real project by jflute (2021/06/26)
        ROOT_TARGET_SUFFIX_LIST = Arrays.asList("Form", "Body", "Bean", "Result", "Part");
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

    // zone
    protected final ActionPropertiesAnalyzer actionPropertiesAnalyzer; // recursive call

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ActionPropertyFieldAnalyzer(OptionalThing<SourceParserReflector> sourceParserReflector,
            Map<String, Type> genericParameterTypesMap, MetaAnnotationArranger metaAnnotationArranger,
            MetaTypeNameAdjuster metaTypeNameAdjuster, FormFieldNameAdjuster formFieldNameAdjuster,
            ActionPropertiesAnalyzer actionPropertiesAnalyzer) {
        this.sourceParserReflector = sourceParserReflector;

        this.genericParameterTypesMap = genericParameterTypesMap;

        this.metaAnnotationArranger = metaAnnotationArranger;
        this.metaTypeNameAdjuster = metaTypeNameAdjuster;
        this.formFieldNameAdjuster = formFieldNameAdjuster;

        this.actionPropertiesAnalyzer = actionPropertiesAnalyzer;
    }

    // ===================================================================================
    //                                                              Analyze Property Field
    //                                                              ======================
    public TypeDocMeta analyzePropertyField(Class<?> propertyOwner, int depth, Field field) {
        final TypeDocMeta meta = new TypeDocMeta();

        final Class<?> resolvedClass = reflectBasicAnalysisToMeta(field, meta);
        reflectNestAnalysisToMeta(depth, field, meta, resolvedClass);

        // e.g. comment item (description, comment)
        sourceParserReflector.ifPresent(sourceParserReflector -> {
            sourceParserReflector.reflect(meta, propertyOwner);
        });

        // necessary to set it after parsing javadoc
        meta.setName(adjustFieldName(propertyOwner, field));
        meta.setPublicName(adjustPublicFieldName(propertyOwner, field));
        return meta;
    }

    // -----------------------------------------------------
    //                                        Basic Analysis
    //                                        --------------
    protected Class<?> reflectBasicAnalysisToMeta(Field field, TypeDocMeta meta) {
        final Class<?> resolvedClass;
        {
            final Type resolvedType;
            {
                final Type genericClass = genericParameterTypesMap.get(field.getGenericType().getTypeName());
                resolvedType = genericClass != null ? genericClass : field.getType();
            }

            // basic item
            meta.setName(field.getName()); // also property name #question but overridden later, needed? by jflute
            meta.setPublicName(adjustPublicFieldName(null, field));
            // #question type property is not related to resolvedType, is it OK? by jflute
            meta.setType(field.getType()); // e.g. String, Integer, SeaPart
            meta.setTypeName(adjustTypeName(resolvedType));
            meta.setSimpleTypeName(adjustSimpleTypeName(resolvedType));

            // annotation item
            meta.setAnnotationTypeList(Arrays.asList(field.getAnnotations()));
            meta.setAnnotationList(metaAnnotationArranger.arrangeAnnotationList(meta.getAnnotationTypeList()));

            // comment item (value expression)
            if (resolvedType instanceof Class) {
                resolvedClass = (Class<?>) resolvedType;
            } else {
                resolvedClass = (Class<?>) DfReflectionUtil.getGenericParameterTypes(resolvedType)[0];
            }
            if (resolvedClass.isEnum()) {
                meta.setValue(buildEnumValuesExp(resolvedClass)); // e.g. {FML = Formalized, PRV = Provisinal, ...}
            }
        }
        return resolvedClass;
    }

    // -----------------------------------------------------
    //                                         Nest Analysis
    //                                         -------------
    protected void reflectNestAnalysisToMeta(int depth, Field field, TypeDocMeta meta, Class<?> resolvedClass) {
        final int nestDepth = depth - 1;
        if (isTargetSuffixResolvedClass(resolvedClass)) { // nested bean of direct type as top or inner class
            // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
            // e.g.
            //  public SeaResult sea; // current field, the outer type should have suffix
            //
            //   or
            //
            //  public class SeaResult { // the declaring class should have suffix
            //      public HangarPart hangar; // current field
            //      public static class HangarPart {
            //          ...
            //      }
            //  }
            // _/_/_/_/_/_/_/_/_/_/
            final List<TypeDocMeta> nestTypeDocMetaList = actionPropertiesAnalyzer.analyzeProperties(resolvedClass, nestDepth);
            meta.setNestTypeDocMetaList(nestTypeDocMetaList);
        } else if (isTargetSuffixFieldGeneric(field)) { // nested bean of generic type as top or inner class
            // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
            // e.g.
            //  public List<SeaResult> seaList; // current field, the outer type should have suffix
            //
            //   or
            //
            //  public class SeaResult { // the declaring class should have suffix
            //      public List<HangarPart> hangarList; // current field
            //      public static class HangarPart {
            //          ...
            //      }
            //  }
            // _/_/_/_/_/_/_/_/_/_/
            Type type = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
            if (type instanceof Class<?>) {
                final Class<?> typeArgumentClass = (Class<?>) type;
                final List<TypeDocMeta> nestTypeDocMetaList = actionPropertiesAnalyzer.analyzeProperties(typeArgumentClass, nestDepth);
                meta.setNestTypeDocMetaList(nestTypeDocMetaList);
                // overriding type names that are already set before
                final String currentTypeName = meta.getTypeName();
                meta.setTypeName(adjustTypeName(currentTypeName) + "<" + adjustTypeName(typeArgumentClass) + ">");
                meta.setSimpleTypeName(adjustSimpleTypeName(currentTypeName) + "<" + adjustSimpleTypeName(typeArgumentClass) + ">");
            } else if (type instanceof ParameterizedType) {
                final Class<?> typeArgumentClass = (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
                final List<TypeDocMeta> nestTypeDocMetaList = actionPropertiesAnalyzer.analyzeProperties(typeArgumentClass, nestDepth);
                meta.setNestTypeDocMetaList(nestTypeDocMetaList);
                // overriding type names that are already set before
                final String currentTypeName = meta.getTypeName();
                meta.setTypeName(adjustTypeName(currentTypeName) + "<" + adjustTypeName(((ParameterizedType) type).getRawType()) + "<"
                        + adjustTypeName(typeArgumentClass) + ">>");
                meta.setSimpleTypeName(
                        adjustSimpleTypeName(currentTypeName) + "<" + adjustSimpleTypeName(((ParameterizedType) type).getRawType()) + "<"
                                + adjustSimpleTypeName(typeArgumentClass) + ">>");
            }
        } else { // e.g. String, Integer, LocalDate, Sea<Mystic>
            // TODO p1us2er0 optimisation, generic handling in analyzePropertyField() (2017/09/26)
            if (field.getGenericType().getTypeName().matches(".*<(.*)>")) { // e.g. Sea<Mystic>
                final String genericTypeName = field.getGenericType().getTypeName().replaceAll(".*<(.*)>", "$1");

                // generic item
                try {
                    meta.setGenericType(DfReflectionUtil.forName(genericTypeName));
                } catch (ReflectionFailureException ignored) { // e.g. BEAN (generic parameter name)
                    meta.setGenericType(Object.class); // unknown
                }

                final Type genericClass = genericParameterTypesMap.get(genericTypeName);
                if (genericClass != null) { // the generic is defined at top definition (e.g. return)
                    final List<TypeDocMeta> nestTypeDocMetaList =
                            actionPropertiesAnalyzer.analyzeProperties((Class<?>) genericClass, nestDepth);
                    meta.setNestTypeDocMetaList(nestTypeDocMetaList);

                    // overriding type names that are already set before
                    final String typeName = meta.getTypeName();
                    meta.setTypeName(adjustTypeName(typeName) + "<" + adjustTypeName(genericClass) + ">");
                    meta.setSimpleTypeName(adjustSimpleTypeName(typeName) + "<" + adjustSimpleTypeName(genericClass) + ">");
                } else {
                    // overriding type names that are already set before
                    final String typeName = meta.getTypeName();
                    meta.setTypeName(adjustTypeName(typeName) + "<" + adjustTypeName(genericTypeName) + ">");
                    meta.setSimpleTypeName(adjustSimpleTypeName(typeName) + "<" + adjustSimpleTypeName(genericTypeName) + ">");
                }
            }
        }
    }

    // ===================================================================================
    //                                                                         ENUM Values
    //                                                                         ===========
    protected String buildEnumValuesExp(Class<?> typeClass) {
        // cannot resolve type by maven compiler, explicitly cast it
        final String valuesExp;
        if (Classification.class.isAssignableFrom(typeClass)) {
            @SuppressWarnings("unchecked")
            final Class<Classification> clsType = ((Class<Classification>) typeClass);
            valuesExp = Arrays.stream(clsType.getEnumConstants()).collect(Collectors.toMap(keyMapper -> {
                return ((Classification) keyMapper).code();
            }, valueMapper -> {
                return ((Classification) valueMapper).alias();
            }, (u, v) -> v, LinkedHashMap::new)).toString(); // e.g. {FML = Formalized, PRV = Provisinal, ...}
        } else {
            final Enum<?>[] constants = (Enum<?>[]) typeClass.getEnumConstants();
            valuesExp = Arrays.stream(constants).collect(Collectors.toList()).toString(); // e.g. [SEA, LAND, PIARI]
        }
        return valuesExp;
    }

    // ===================================================================================
    //                                                                       Target Suffix
    //                                                                       =============
    // target means e.g. Form, Result or their inner class (Part class)
    protected boolean isTargetSuffixResolvedClass(Class<?> resolvedClass) {
        return getTargetTypeSuffixList().stream().anyMatch(suffix -> {
            final String fqcn = resolvedClass.getName(); // may be inner class e.g. SeaForm$MysticPart
            return determineTargetSuffixResolvedClass(fqcn, suffix);
        });
    }

    protected boolean isTargetSuffixFieldGeneric(Field field) {
        // #thinking jflute can it fast-return if non-generic field? e.g. java.langString (2021/07/18)
        return getTargetTypeSuffixList().stream().anyMatch(suffix -> {
            // only called for non-suffix-target field type here  
            // e.g.
            //  fieldTypeName=java.lang.String, genericTypeName=java.lang.String
            //  fieldTypeName=java.lang.Integer, genericTypeName=java.lang.Integer
            //  fieldTypeName=java.util.List, genericTypeName=java.util.List<java.lang.String>
            //  fieldTypeName=java.util.List, genericTypeName=java.util.List<...SeaForm$MysticPart>
            //  fieldTypeName=java.util.List, genericTypeName=java.util.List<...SeaForm$MysticPart>
            //  fieldTypeName=java.util.List, genericTypeName=java.util.List<...MysticPart>
            //  fieldTypeName=java.util.List, genericTypeName=java.util.List<...MysticResult>
            final String fqcn = field.getGenericType().getTypeName();
            return determineTargetSuffixResolvedClass(fqcn, suffix);
        });
    }

    protected List<String> getTargetTypeSuffixList() {
        return ROOT_TARGET_SUFFIX_LIST; // e.g. Form, Result
    }

    protected boolean determineTargetSuffixResolvedClass(String fqcn, String suffix) {
        return fqcn.endsWith(suffix) || fqcn.contains(suffix + "$"); // e.g. SeaForm or SeaForm$MysticPart
    }

    // ===================================================================================
    //                                                                          Field Name
    //                                                                          ==========
    protected String adjustFieldName(Class<?> clazz, Field field) {
        return formFieldNameAdjuster.adjustFieldName(clazz, field);
    }

    protected String adjustPublicFieldName(Class<?> clazz, Field field) {
        return formFieldNameAdjuster.adjustPublicFieldName(clazz, field);
    }

    // ===================================================================================
    //                                                                           Type Name
    //                                                                           =========
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
