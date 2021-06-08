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
package org.lastaflute.meta.document;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.dbflute.jdbc.Classification;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfReflectionUtil;
import org.dbflute.util.DfReflectionUtil.ReflectionFailureException;
import org.dbflute.util.DfStringUtil;
import org.lastaflute.core.json.JsonMappingOption;
import org.lastaflute.core.json.JsonMappingOption.JsonFieldNaming;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.meta.document.action.ExecuteMethodCollector;
import org.lastaflute.meta.document.docmeta.ActionDocMeta;
import org.lastaflute.meta.document.docmeta.TypeDocMeta;
import org.lastaflute.meta.sourceparser.SourceParserReflector;
import org.lastaflute.meta.util.LaDocReflectionUtil;
import org.lastaflute.web.UrlChain;
import org.lastaflute.web.path.ActionPathResolver;
import org.lastaflute.web.ruts.config.ActionExecute;
import org.lastaflute.web.ruts.multipart.MultipartFormFile;

import com.google.gson.FieldNamingPolicy;

// package of this class should be under lastaflute but no fix for compatible
/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.0-sp9 of UTFlute (2015/09/18 Friday)
 */
public class ActionDocumentAnalyzer extends BaseDocumentAnalyzer {

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

    protected static final List<Class<?>> NATIVE_TYPE_LIST =
            Arrays.asList(void.class, boolean.class, byte.class, int.class, long.class, float.class, double.class, Void.class, Byte.class,
                    Boolean.class, Integer.class, Byte.class, Long.class, Float.class, Double.class, String.class, Map.class, byte[].class,
                    Byte[].class, Date.class, LocalDate.class, LocalDateTime.class, LocalTime.class, MultipartFormFile.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The list of source directory. (NotNull) */
    protected final List<String> srcDirList;

    /** depth of analyzed target, to avoid cyclic analyzing. */
    protected final int depth;

    /** The optional reflector of source parser, e.g. java parser. (NotNull, EmptyAllowed) */
    protected final OptionalThing<SourceParserReflector> sourceParserReflector;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public ActionDocumentAnalyzer(List<String> srcDirList, int depth, OptionalThing<SourceParserReflector> sourceParserReflector) {
        this.srcDirList = srcDirList;
        this.depth = depth;
        this.sourceParserReflector = sourceParserReflector;
    }

    // ===================================================================================
    //                                                                            Generate
    //                                                                            ========
    public List<ActionDocMeta> generateActionDocMetaList() { // the list is per execute method
        return createExecuteMethodCollector().collectActionExecuteList().stream().map(execute -> {
            return createActionDocMeta(execute);
        }).collect(Collectors.toList());
    }

    protected ExecuteMethodCollector createExecuteMethodCollector() {
        return new ExecuteMethodCollector(srcDirList, sourceParserReflector, execute -> {
            return exceptsActionExecute(execute);
        });
    }

    protected boolean exceptsActionExecute(ActionExecute actionExecute) { // may be overridden
        if (suppressActionExecute(actionExecute)) { // for compatible
            return true;
        }
        return false;
    }

    @Deprecated
    protected boolean suppressActionExecute(ActionExecute actionExecute) {
        return false;
    }

    // ===================================================================================
    //                                                                      Action DocMeta
    //                                                                      ==============
    protected ActionDocMeta createActionDocMeta(ActionExecute execute) {
        final ActionDocMeta actionDocMeta = new ActionDocMeta();
        final Class<?> actionClass = execute.getActionMapping().getActionDef().getComponentClass();
        final UrlChain urlChain = new UrlChain(actionClass);
        final String urlPattern = execute.getPreparedUrlPattern().getResolvedUrlPattern();
        if (!"index".equals(urlPattern)) {
            urlChain.moreUrl(urlPattern);
        }

        // action item
        actionDocMeta.setUrl(getActionPathResolver().toActionUrl(actionClass, urlChain));

        // class item
        final Method executeMethod = execute.getExecuteMethod();
        final Class<?> methodDeclaringClass = executeMethod.getDeclaringClass(); // basically same as componentClass
        actionDocMeta.setType(methodDeclaringClass);
        actionDocMeta.setTypeName(adjustTypeName(methodDeclaringClass));
        actionDocMeta.setSimpleTypeName(adjustSimpleTypeName(methodDeclaringClass));

        // field item
        actionDocMeta.setFieldTypeDocMetaList(Arrays.stream(methodDeclaringClass.getDeclaredFields()).map(field -> {
            final TypeDocMeta typeDocMeta = new TypeDocMeta();
            typeDocMeta.setName(field.getName());
            typeDocMeta.setPublicName(adjustPublicFieldName(null, field));
            typeDocMeta.setType(field.getType());
            typeDocMeta.setTypeName(adjustTypeName(field.getGenericType()));
            typeDocMeta.setSimpleTypeName(adjustSimpleTypeName((field.getGenericType())));
            typeDocMeta.setAnnotationTypeList(Arrays.asList(field.getAnnotations()));
            typeDocMeta.setAnnotationList(arrangeAnnotationList(typeDocMeta.getAnnotationTypeList()));

            sourceParserReflector.ifPresent(sourceParserReflector -> {
                sourceParserReflector.reflect(typeDocMeta, field.getType());
            });
            return typeDocMeta;
        }).collect(Collectors.toList()));

        // method item
        actionDocMeta.setMethodName(executeMethod.getName());

        // annotation item
        final List<Annotation> annotationList = DfCollectionUtil.newArrayList();
        annotationList.addAll(Arrays.asList(methodDeclaringClass.getAnnotations()));
        annotationList.addAll(Arrays.asList(executeMethod.getAnnotations()));
        actionDocMeta.setAnnotationTypeList(annotationList); // contains both action and execute method
        actionDocMeta.setAnnotationList(arrangeAnnotationList(annotationList));

        // in/out item (parameter, form, return)
        final List<TypeDocMeta> parameterTypeDocMetaList = DfCollectionUtil.newArrayList();
        Arrays.stream(executeMethod.getParameters()).filter(parameter -> {
            return !(execute.getFormMeta().isPresent() && execute.getFormMeta().get().getSymbolFormType().equals(parameter.getType()));
        }).forEach(parameter -> { // except form parameter here
            actionDocMeta.setUrl(buildNewActionUrl(actionDocMeta, parameter));
            parameterTypeDocMetaList.add(analyzeMethodParameter(parameter));
        });
        actionDocMeta.setParameterTypeDocMetaList(parameterTypeDocMetaList);
        analyzeFormClass(execute).ifPresent(formTypeDocMeta -> {
            actionDocMeta.setFormTypeDocMeta(formTypeDocMeta);
        });
        actionDocMeta.setReturnTypeDocMeta(analyzeReturnClass(executeMethod));

        // extension item (url, return, comment...)
        sourceParserReflector.ifPresent(sourceParserReflector -> {
            sourceParserReflector.reflect(actionDocMeta, executeMethod);
        });

        return actionDocMeta;
    }

    protected String buildNewActionUrl(ActionDocMeta actionDocMeta, Parameter parameter) {
        final StringBuilder builder = new StringBuilder();
        builder.append("{").append(parameter.getName()).append("}");
        return actionDocMeta.getUrl().replaceFirst("\\{\\}", builder.toString());
    }

    // ===================================================================================
    //                                                                             Analyze
    //                                                                             =======
    // -----------------------------------------------------
    //                                     Analyze Parameter
    //                                     -----------------
    protected TypeDocMeta analyzeMethodParameter(Parameter parameter) {
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
        parameterDocMeta.setAnnotationList(arrangeAnnotationList(parameterDocMeta.getAnnotationTypeList()));
        parameterDocMeta.setNestTypeDocMetaList(Collections.emptyList());
        sourceParserReflector.ifPresent(sourceParserReflector -> {
            sourceParserReflector.reflect(parameterDocMeta, parameter.getType());
        });
        return parameterDocMeta;
    }

    // -----------------------------------------------------
    //                                          Analyze Form
    //                                          ------------
    protected OptionalThing<TypeDocMeta> analyzeFormClass(ActionExecute execute) {
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
            // #question can be emptyMap()? it seems like read-only in analyzeProperties() by jflute (2019/07/01)
            final Map<String, Type> genericParameterTypesMap = DfCollectionUtil.newLinkedHashMap();
            final List<TypeDocMeta> propertyDocMetaList = analyzeProperties(formType, genericParameterTypesMap, depth);
            formDocMeta.setNestTypeDocMetaList(propertyDocMetaList);
            sourceParserReflector.ifPresent(sourceParserReflector -> {
                sourceParserReflector.reflect(formDocMeta, formType);
            });
            return formDocMeta;
        });
    }

    // -----------------------------------------------------
    //                                        Analyze Return
    //                                        --------------
    protected TypeDocMeta analyzeReturnClass(Method method) {
        final TypeDocMeta returnDocMeta = new TypeDocMeta();
        returnDocMeta.setType(method.getReturnType());
        returnDocMeta.setTypeName(adjustTypeName(method.getGenericReturnType()));
        returnDocMeta.setSimpleTypeName(adjustSimpleTypeName(method.getGenericReturnType()));
        returnDocMeta.setGenericType(DfReflectionUtil.getGenericFirstClass(method.getGenericReturnType()));
        returnDocMeta.setAnnotationTypeList(Arrays.asList(method.getAnnotatedReturnType().getAnnotations()));
        returnDocMeta.setAnnotationList(arrangeAnnotationList(returnDocMeta.getAnnotationTypeList()));
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
            final List<Class<? extends Object>> nativeClassList = getNativeClassList();
            if (returnClass != null && !nativeClassList.contains(returnClass)) {
                final List<TypeDocMeta> propertyDocMetaList = analyzeProperties(returnClass, genericParameterTypesMap, depth);
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

    public List<Class<?>> getNativeClassList() {
        return NATIVE_TYPE_LIST;
    }

    // -----------------------------------------------------
    //                                    Analyze Properties
    //                                    ------------------
    // #hope separate analyzeProperties() from this generator (because depth is shadowed) by jflute (2019/07/01)
    // (also analyzePropertyField())
    //
    // for e.g. form type, return type, nested property type
    protected List<TypeDocMeta> analyzeProperties(Class<?> propertyOwner, Map<String, Type> genericParameterTypesMap, int depth) {
        if (depth < 0) {
            return DfCollectionUtil.newArrayList();
        }
        final Set<Field> fieldSet = extractWholeFieldSet(propertyOwner);
        return fieldSet.stream().filter(field -> { // also contains private fields and super's fields
            return !exceptsField(field);
        }).map(field -> { // #question can private fields be treated as property? by jflute
            return analyzePropertyField(propertyOwner, genericParameterTypesMap, depth, field);
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

    // -----------------------------------------------------
    //                                Analyze Property Field
    //                                ----------------------
    protected TypeDocMeta analyzePropertyField(Class<?> propertyOwner, Map<String, Type> genericParameterTypesMap, int depth, Field field) {
        final TypeDocMeta meta = new TypeDocMeta();

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
            meta.setAnnotationList(arrangeAnnotationList(meta.getAnnotationTypeList()));

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
            meta.setNestTypeDocMetaList(analyzeProperties(resolvedClass, genericParameterTypesMap, depth - 1));
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
                meta.setNestTypeDocMetaList(analyzeProperties(typeArgumentClass, genericParameterTypesMap, depth - 1));
                // overriding type names that are already set before
                final String currentTypeName = meta.getTypeName();
                meta.setTypeName(adjustTypeName(currentTypeName) + "<" + adjustTypeName(typeArgumentClass) + ">");
                meta.setSimpleTypeName(adjustSimpleTypeName(currentTypeName) + "<" + adjustSimpleTypeName(typeArgumentClass) + ">");
            } else if (type instanceof ParameterizedType) {
                final Class<?> typeArgumentClass = (Class<?>) ((ParameterizedType) type).getActualTypeArguments()[0];
                meta.setNestTypeDocMetaList(analyzeProperties(typeArgumentClass, genericParameterTypesMap, depth - 1));
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
                    meta.setNestTypeDocMetaList(analyzeProperties((Class<?>) genericClass, genericParameterTypesMap, depth - 1));

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

        // e.g. comment item (description, comment)
        sourceParserReflector.ifPresent(sourceParserReflector -> {
            sourceParserReflector.reflect(meta, propertyOwner);
        });

        // necessary to set it after parsing javadoc
        meta.setName(adjustFieldName(propertyOwner, field));
        meta.setPublicName(adjustPublicFieldName(propertyOwner, field));
        return meta;
    }

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

    // -----------------------------------------------------
    //                                         Target Suffix
    //                                         -------------
    // target means e.g. Form, Result or their inner class (Part class)
    protected boolean isTargetSuffixResolvedClass(Class<?> resolvedClass) {
        return getTargetTypeSuffixList().stream().anyMatch(suffix -> {
            final String fqcn = resolvedClass.getName(); // may be inner class e.g. SeaForm$MysticPart
            return determineTargetSuffixResolvedClass(fqcn, suffix);
        });
    }

    protected boolean isTargetSuffixFieldGeneric(Field field) {
        return getTargetTypeSuffixList().stream().anyMatch(suffix -> {
            final String fqcn = field.getGenericType().getTypeName(); // may be inner class e.g. SeaForm$MysticPart
            return determineTargetSuffixResolvedClass(fqcn, suffix);
        });
    }

    protected List<String> getTargetTypeSuffixList() {
        return TARGET_SUFFIX_LIST; // e.g. Form, Result
    }

    protected boolean determineTargetSuffixResolvedClass(String fqcn, String suffix) {
        return fqcn.endsWith(suffix) || fqcn.contains(suffix + "$"); // e.g. SeaForm or SeaForm$MysticPart
    }

    // -----------------------------------------------------
    //                                            Field Name
    //                                            ----------
    protected String adjustFieldName(Class<?> clazz, Field field) {
        return field.getName();
    }

    protected String adjustPublicFieldName(Class<?> clazz, Field field) {
        // done (by jflute 2019/01/17) p1us2er0 judge accurately in adjustFieldName() (2017/04/20)
        if (clazz == null || isActionFormComponentType(clazz)) {
            return field.getName();
        }
        // basically JsonBody or JsonResult here
        // (Thymeleaf beans cannot be analyzed as framework so not here)
        return getApplicationJsonMappingOption().flatMap(jsonMappingOption -> {
            return jsonMappingOption.getFieldNaming().map(naming -> {
                if (naming == JsonFieldNaming.IDENTITY) {
                    return FieldNamingPolicy.IDENTITY.translateName(field);
                } else if (naming == JsonFieldNaming.CAMEL_TO_LOWER_SNAKE) {
                    return FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES.translateName(field);
                } else {
                    return field.getName();
                }
            });
        }).orElse(field.getName());
    }

    protected boolean isActionFormComponentType(Class<?> clazz) { // and not JSON body
        // #thinking jflute using Form meta of LastaFlute is better? (2019/01/17)
        return clazz.getSimpleName().endsWith("Form") // just form class
                || clazz.getName().contains("Form$"); // inner class of Form (e.g. Part)
    }

    // ===================================================================================
    //                                                                     Action Property
    //                                                                     ===============
    protected Map<String, String> convertPropertyNameMap(String parentName, TypeDocMeta typeDocMeta) {
        if (typeDocMeta == null) {
            return DfCollectionUtil.newLinkedHashMap();
        }

        final Map<String, String> propertyNameMap = DfCollectionUtil.newLinkedHashMap();

        final String name = calculateName(parentName, typeDocMeta.getName(), typeDocMeta.getTypeName());
        if (DfStringUtil.is_NotNull_and_NotEmpty(name)) {
            propertyNameMap.put(name, "");
        }

        if (typeDocMeta.getNestTypeDocMetaList() != null) {
            typeDocMeta.getNestTypeDocMetaList().forEach(nestDocMeta -> {
                propertyNameMap.putAll(convertPropertyNameMap(name, nestDocMeta));
            });
        }

        return propertyNameMap;
    }

    protected String calculateName(String parentName, String name, String type) {
        if (DfStringUtil.is_Null_or_Empty(name)) {
            return null;
        }

        final StringBuilder builder = new StringBuilder();
        if (DfStringUtil.is_NotNull_and_NotEmpty(parentName)) {
            builder.append(parentName + ".");
        }
        builder.append(name);
        if (name.endsWith("List")) {
            builder.append("[]");
        }

        return builder.toString();
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected DocumentAnalyzerFactory createDocumentGeneratorFactory() {
        return new DocumentAnalyzerFactory();
    }

    protected ActionPathResolver getActionPathResolver() {
        return ContainerUtil.getComponent(ActionPathResolver.class);
    }

    protected OptionalThing<JsonMappingOption> getApplicationJsonMappingOption() {
        return createDocumentGeneratorFactory().getApplicationJsonMappingOption();
    }
}
