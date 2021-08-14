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
package org.lastaflute.meta.swagger.spec.zone.parameter;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.Constraint;
import javax.validation.constraints.Email;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfStringUtil;
import org.dbflute.util.DfTypeUtil;
import org.hibernate.validator.constraints.Length;
import org.lastaflute.meta.document.docmeta.TypeDocMeta;
import org.lastaflute.meta.swagger.spec.parts.datatype.SwaggerSpecDataType;
import org.lastaflute.meta.swagger.spec.parts.datatype.SwaggerSpecDataTypeHandler;
import org.lastaflute.meta.swagger.spec.parts.defaultvalue.SwaggerSpecDefaultValueHandler;
import org.lastaflute.meta.swagger.spec.parts.definition.SwaggerSpecDefinitionHandler;
import org.lastaflute.meta.swagger.spec.parts.encoding.SwaggerSpecEncodingHandler;
import org.lastaflute.meta.swagger.spec.parts.enumtype.SwaggerSpecEnumHandler;
import org.lastaflute.meta.swagger.spec.parts.property.SwaggerSpecPropertyHandler;
import org.lastaflute.web.api.JsonParameter;
import org.lastaflute.web.response.ActionResponse;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from SwaggerGenerator (2021/08/07 Saturday)
 */
public class SwaggerSpecParameterSetupper {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // -----------------------------------------------------
    //                                    Resource for Setup
    //                                    ------------------
    protected final List<Class<?>> nativeDataTypeList; // standard native types of e.g. parameter, not null, not empty

    // -----------------------------------------------------
    //                                         Parts Handler
    //                                         -------------
    protected final SwaggerSpecEnumHandler enumHandler;
    protected final SwaggerSpecDataTypeHandler dataTypeHandler;
    protected final SwaggerSpecDefaultValueHandler defaultValueHandler;
    protected final SwaggerSpecPropertyHandler propertyHandler;
    protected final SwaggerSpecDefinitionHandler definitionHandler;
    protected final SwaggerSpecEncodingHandler encodingHandler;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public SwaggerSpecParameterSetupper(List<Class<?>> nativeDataTypeList, SwaggerSpecEnumHandler enumHandler,
            SwaggerSpecDataTypeHandler dataTypeHandler, SwaggerSpecDefaultValueHandler defaultValueHandler,
            SwaggerSpecPropertyHandler propertyHandler, SwaggerSpecDefinitionHandler definitionHandler,
            SwaggerSpecEncodingHandler encodingHandler) {
        this.nativeDataTypeList = nativeDataTypeList;
        this.enumHandler = enumHandler;
        this.dataTypeHandler = dataTypeHandler;
        this.defaultValueHandler = defaultValueHandler;
        this.propertyHandler = propertyHandler;
        this.definitionHandler = definitionHandler;
        this.encodingHandler = encodingHandler;
    }

    // ===================================================================================
    //                                                                       Parameter Map
    //                                                                       =============
    public Map<String, Object> toParameterMap(TypeDocMeta typeDocMeta, Map<String, Map<String, Object>> definitionsMap) {
        // #hope jflute should be cached? because of many calls (2021/06/23)
        final Map<Class<?>, SwaggerSpecDataType> typeMap = dataTypeHandler.createSwaggerDataTypeMap();
        final Class<?> keepType = typeDocMeta.getType();
        if (typeDocMeta.getGenericType() != null && (ActionResponse.class.isAssignableFrom(typeDocMeta.getType())
                || OptionalThing.class.isAssignableFrom(typeDocMeta.getType()))) {
            typeDocMeta.setType(typeDocMeta.getGenericType());
        }

        final Map<String, Object> parameterMap = DfCollectionUtil.newLinkedHashMap();
        parameterMap.put("name", typeDocMeta.getPublicName());
        if (DfStringUtil.is_NotNull_and_NotEmpty(typeDocMeta.getDescription())) {
            parameterMap.put("description", typeDocMeta.getDescription());
        }
        if (typeMap.containsKey(typeDocMeta.getType())) {
            final SwaggerSpecDataType swaggerType = typeMap.get(typeDocMeta.getType());
            parameterMap.put("type", swaggerType.type);
            final String format = swaggerType.format;
            if (DfStringUtil.is_NotNull_and_NotEmpty(format)) {
                parameterMap.put("format", format);
            }
        } else if (typeDocMeta.getAnnotationTypeList().stream().anyMatch(annotationType -> {
            return JsonParameter.class.isAssignableFrom(annotationType.getClass());
        })) {
            parameterMap.put("type", "string");
            // #needs_fix p1us2er0 set description and example. (2018/09/30)
        } else if (Iterable.class.isAssignableFrom(typeDocMeta.getType())) {
            setupArrayAttribute(parameterMap, typeDocMeta, definitionsMap, typeMap);
        } else if (typeDocMeta.getType().equals(Object.class) || Map.class.isAssignableFrom(typeDocMeta.getType())) {
            parameterMap.put("type", "object");
        } else if (Enum.class.isAssignableFrom(typeDocMeta.getType())) {
            // e.g. public AppCDef.PublicProductStatus productStatus;
            @SuppressWarnings("unchecked")
            final Class<? extends Enum<?>> enumType = (Class<? extends Enum<?>>) typeDocMeta.getType();
            setupEnumAttribute(parameterMap, enumType, typeDocMeta);
        } else if (!nativeDataTypeList.contains(typeDocMeta.getType())) {
            final String definition = putDefinitionAttribute(typeDocMeta, definitionsMap);
            parameterMap.clear();
            parameterMap.put("name", typeDocMeta.getPublicName());
            parameterMap.put("$ref", definition);
        } else {
            parameterMap.put("type", "object");
        }

        setupValidationAttribute(typeDocMeta, parameterMap);
        setupExampleAttribute(typeDocMeta, parameterMap);

        typeDocMeta.setType(keepType);
        return parameterMap;
    }

    // ===================================================================================
    //                                                                     Array Attribute
    //                                                                     ===============
    protected void setupArrayAttribute(Map<String, Object> schemaMap, TypeDocMeta typeDocMeta,
            Map<String, Map<String, Object>> definitionsMap, Map<Class<?>, SwaggerSpecDataType> dataTypeMap) {
        schemaMap.put("type", "array");
        if (!typeDocMeta.getNestTypeDocMetaList().isEmpty()) {
            final String definition = putDefinitionAttribute(typeDocMeta, definitionsMap);
            schemaMap.put("items", DfCollectionUtil.newLinkedHashMap("$ref", definition));
        } else {
            final Map<String, Object> itemsMap = DfCollectionUtil.newLinkedHashMap();
            final Class<?> genericType = typeDocMeta.getGenericType();
            if (genericType != null) {
                final SwaggerSpecDataType swaggerDataType = dataTypeMap.get(genericType);
                if (swaggerDataType != null) {
                    itemsMap.put("type", swaggerDataType.type);
                    final String format = swaggerDataType.format;
                    if (DfStringUtil.is_NotNull_and_NotEmpty(format)) {
                        itemsMap.put("format", format);
                    }
                } else { // e.g. List<CDef.StageType> or List<UnknownType>
                    if (Enum.class.isAssignableFrom(genericType)) {
                        // e.g. public List<AppCDef.PublicProductStatus> pastProductStatuses;
                        @SuppressWarnings("unchecked")
                        final Class<? extends Enum<?>> enumType = (Class<? extends Enum<?>>) genericType;
                        setupEnumAttribute(itemsMap, enumType, typeDocMeta);
                    }
                }
            }
            if (!itemsMap.containsKey("type")) {
                itemsMap.put("type", "object");
            }
            schemaMap.put("items", itemsMap);
        }
        if (typeDocMeta.getSimpleTypeName().matches(".*List<.*List<.*")) {
            schemaMap.put("items", DfCollectionUtil.newLinkedHashMap("type", "array", "items", schemaMap.get("items")));
        }
    }

    // ===================================================================================
    //                                                                      Enum Attribute
    //                                                                      ==============
    protected void setupEnumAttribute(Map<String, Object> attrMap, Class<? extends Enum<?>> enumType, TypeDocMeta typeDocMeta) {
        attrMap.put("type", "string");

        // #for_now jflute typeDocMeta.value (enum expression for LastaDoc) is not used here (2021/08/06)
        // directly extract it from Enum type, ...small dependency so enough?
        final List<Map<String, String>> enumMapList = prepareEnumMapList(enumType);
        attrMap.put("enum", buildEnumCodeList(enumMapList));
        attrMap.put("description", buildEnumDescription(enumType, enumMapList, typeDocMeta));
    }

    protected List<Map<String, String>> prepareEnumMapList(Class<? extends Enum<?>> enumType) {
        return enumHandler.buildEnumMapList(enumType);
    }

    protected List<String> buildEnumCodeList(List<Map<String, String>> enumMapList) {
        return enumMapList.stream().map(em -> em.get("code")).collect(Collectors.toList());
    }

    protected String buildEnumDescription(Class<? extends Enum<?>> enumType, List<Map<String, String>> enumMapList,
            TypeDocMeta typeDocMeta) {
        final StringBuilder sb = new StringBuilder();
        final String description = typeDocMeta.getDescription();
        if (DfStringUtil.is_NotNull_and_NotTrimmedEmpty(description)) {
            sb.append(description).append(":");
        }
        final String contentExp = enumMapList.stream().map(enumMap -> {
            final String code = enumMap.get("code");
            final String name = enumMap.get("name");
            final String alias = enumMap.get("alias");
            final String formatted;
            if (name != null && alias != null && name.equals(alias)) {
                formatted = String.format(" * `%s` - %s.", code, name);
            } else {
                formatted = String.format(" * `%s` - %s, %s.", code, name, alias);
            }
            return formatted;
        }).collect(Collectors.joining());
        sb.append(contentExp);
        final String enumTitle = DfTypeUtil.toClassTitle(enumType); // e.g. AppCDef.PublicProductStatus
        sb.append(" :: fromCls(").append(enumTitle).append(")"); // for server reference
        return sb.toString();
    }

    // ===================================================================================
    //                                                                Validation Attribute
    //                                                                ====================
    protected void setupValidationAttribute(TypeDocMeta typeDocMeta, Map<String, Object> parameterMap) {
        toOrderedAnnotationList(typeDocMeta.getAnnotationTypeList()).forEach(annotation -> {
            // best effort logic here, add logics on demand and simply overwrite if conflict
            // #for_now jflute determination of @Valid existence is so difficult (2021/08/07)
            handleValidationAnnotationIfNeeds(annotation, parameterMap);

            // #for_now jflute one nest only (2021/08/07)
            toOrderedAnnotationList(annotation.annotationType().getAnnotations()).forEach(nestAnno -> {
                handleValidationAnnotationIfNeeds(nestAnno, parameterMap);
            });
        });
    }

    // -----------------------------------------------------
    //                               Ordered Annotation List
    //                               -----------------------
    // to fix attribute order on swagger.json
    protected List<Annotation> toOrderedAnnotationList(List<Annotation> providedList) {
        final List<Annotation> orderedList = new ArrayList<>(providedList);
        Collections.sort(orderedList, Comparator.comparing(anno -> {
            // simple order by class name
            // source code order depends on source parser so not uses here
            // (basically conflict validation should be fixed by application) 
            return anno.getClass().getName();
        }));
        return orderedList;
    }

    protected List<Annotation> toOrderedAnnotationList(Annotation[] providedAry) {
        return toOrderedAnnotationList(Arrays.asList(providedAry));
    }

    // -----------------------------------------------------
    //                                   Annotation Handling
    //                                   -------------------
    protected void handleValidationAnnotationIfNeeds(Annotation annotation, Map<String, Object> parameterMap) {
        if (!isValidationAnnotation(annotation)) { // e.g. @Valid, @ValidateTypeFailure
            return;
        }
        // #for_now jflute pending maxItems, minItems for array (2021/08/07)
        doHandleValidationLength(annotation, parameterMap);
        doHandleValidationSize(annotation, parameterMap);
        doHandleValidationPattern(annotation, parameterMap);
        doHandleValidationEmail(annotation, parameterMap);
        doHandleValidationMinMax(annotation, parameterMap);
    }

    protected boolean isValidationAnnotation(Annotation annotation) {
        return annotation.annotationType().getAnnotation(Constraint.class) != null;
    }

    protected void doHandleValidationLength(Annotation annotation, Map<String, Object> parameterMap) {
        if (annotation instanceof Length) { // hibernate
            final Length length = (Length) annotation;
            parameterMap.put("minLength", length.min());
            parameterMap.put("maxLength", length.max());
        }
    }

    protected void doHandleValidationSize(Annotation annotation, Map<String, Object> parameterMap) {
        if (annotation instanceof Size) { // javax
            final Size size = (Size) annotation;
            parameterMap.put("minLength", size.min());
            parameterMap.put("maxLength", size.max());
        }
    }

    protected void doHandleValidationPattern(Annotation annotation, Map<String, Object> parameterMap) {
        if (annotation instanceof Pattern) { // javax
            final Pattern pattern = (Pattern) annotation;
            parameterMap.put("pattern", pattern.regexp());
        }
    }

    protected void doHandleValidationEmail(Annotation annotation, Map<String, Object> parameterMap) {
        if (annotation instanceof Email) { // javax
            final Email email = (Email) annotation;
            parameterMap.put("pattern", email.regexp());
        }
    }

    protected void doHandleValidationMinMax(Annotation annotation, Map<String, Object> parameterMap) {
        if (annotation instanceof Min) { // javax
            final Min min = (Min) annotation;
            parameterMap.put("minimum", min.value());
        }
        if (annotation instanceof Max) { // javax
            final Max max = (Max) annotation;
            parameterMap.put("maximum", max.value());
        }
    }

    // ===================================================================================
    //                                                                   Example Attribute
    //                                                                   =================
    protected void setupExampleAttribute(TypeDocMeta typeDocMeta, final Map<String, Object> parameterMap) {
        defaultValueHandler.deriveDefaultValue(typeDocMeta).ifPresent(defaultValue -> {
            parameterMap.put("example", defaultValue);
        });
    }

    // ===================================================================================
    //                                                                Definition Attribute
    //                                                                ====================
    protected String putDefinitionAttribute(TypeDocMeta typeDocMeta, Map<String, Map<String, Object>> definitionsMap) {
        //     "org.docksidestage.app.web.mypage.MypageResult": {
        //       "type": "object",
        //       "required": [
        //         ...
        //       ],
        //       "properties": {
        //         ...
        //       ],
        //
        //     ...
        //
        //     "org.docksidestage.app.web.base.paging.SearchPagingResult\u003corg.docksidestage.app.web.products.ProductsRowResult\u003e": {
        //       "type": "object",
        //       ...
        String derivedDefinitionName = definitionHandler.deriveDefinitionName(typeDocMeta);
        if (!definitionsMap.containsKey(derivedDefinitionName)) {
            final Map<String, Object> schema = DfCollectionUtil.newLinkedHashMap();
            schema.put("type", "object");
            final List<String> requiredPropertyNameList = propertyHandler.deriveRequiredPropertyNameList(typeDocMeta);
            if (!requiredPropertyNameList.isEmpty()) {
                schema.put("required", requiredPropertyNameList);
            }
            schema.put("properties", typeDocMeta.getNestTypeDocMetaList().stream().map(nestTypeDocMeta -> {
                return toParameterMap(nestTypeDocMeta, definitionsMap);
            }).collect(Collectors.toMap(key -> key.get("name"), value -> {
                // #needs_fix p1us2er0 remove name. refactor required. (2017/10/12)
                final LinkedHashMap<String, Object> property = DfCollectionUtil.newLinkedHashMap(value);
                property.remove("name");
                return property;
            }, (u, v) -> v, LinkedHashMap::new)));

            definitionsMap.put(derivedDefinitionName, schema);
        }
        return "#/definitions/" + encodingHandler.encode(derivedDefinitionName);
    }
}
