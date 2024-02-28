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
package org.lastaflute.meta.swagger.spec.parts.defaultvalue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.dbflute.helper.dfmap.DfMapStyle;
import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfStringUtil;
import org.dbflute.util.Srl;
import org.lastaflute.meta.document.docmeta.TypeDocMeta;
import org.lastaflute.meta.exception.SwaggerDefaultValueParseFailureException;
import org.lastaflute.meta.exception.SwaggerDefaultValueTypeConversionFailureException;
import org.lastaflute.meta.swagger.spec.parts.datatype.SwaggerSpecDataType;
import org.lastaflute.meta.swagger.spec.parts.datatype.SwaggerSpecDataTypeHandler;
import org.lastaflute.meta.swagger.spec.parts.enumtype.SwaggerSpecEnumHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from SwaggerGenerator (2021/06/23 Wednesday at roppongi japanese)
 */
public class SwaggerSpecDefaultValueHandler {

    private static final Logger logger = LoggerFactory.getLogger(SwaggerSpecDefaultValueHandler.class);

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final SwaggerSpecDataTypeHandler dataTypeHandler;
    protected final SwaggerSpecEnumHandler enumHandler;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public SwaggerSpecDefaultValueHandler(SwaggerSpecDataTypeHandler dataTypeHandler, SwaggerSpecEnumHandler enumHandler) {
        this.dataTypeHandler = dataTypeHandler;
        this.enumHandler = enumHandler;
    }

    // ===================================================================================
    //                                                                       Default Value
    //                                                                       =============
    /**
     * @param typeDocMeta The meta of property on bean class. (NotNull)
     * @return The optional default value derived from comment. (NotNull, EmptyAllowed: e.g. not found)
     */
    public OptionalThing<Object> deriveDefaultValue(TypeDocMeta typeDocMeta) {
        try {
            final Map<Class<?>, SwaggerSpecDataType> swaggerDataTypeMap = dataTypeHandler.createSwaggerDataTypeMap();
            if (swaggerDataTypeMap.containsKey(typeDocMeta.getType())) { // scalar e.g. String, Integer, LocalDate
                // e.g.
                //  /** Sea Name e.g. SeaOfDreams */ => SeaOfDreams
                //  /** Sea Name e.g. \"Sea of Dreams\"*/ => Sea of Dreams
                return doDeriveScalarDefalutValue(typeDocMeta, swaggerDataTypeMap);
            } else if (isNonNestIterable(typeDocMeta)) { // e.g. List<String>, ImmutableList<Integer> (not List<SeaResult>)
                // e.g.
                //  /** Sea List e.g. [dockside, hangar] */ => ["dockside", "hangar"]
                //  /** Sea List e.g. ["dockside", "hangar"] */ => ["dockside", "hangar"]
                return doDeriveListDefalutValue(typeDocMeta, swaggerDataTypeMap);
            } else if (isNonNestMap(typeDocMeta)) { // e.g. Map<String, String>
                // e.g.
                //  /** Sea Map e.g. {dockside:over, hangar:mystic] */ => {"dockside" = "over", "hangar" = "mystic"}
                return doDeriveMapDefalutValue(typeDocMeta, swaggerDataTypeMap);
            } else if (Enum.class.isAssignableFrom(typeDocMeta.getType())) { // e.g. CDef
                // e.g.
                //  /** Sea Status e.g. FML */ => FML
                return doDeriveEnumDefaultValue(typeDocMeta, swaggerDataTypeMap);
            }
            return OptionalThing.empty();
        } catch (RuntimeException e) { // unexpected cases
            if (e instanceof SwaggerDefaultValueTypeConversionFailureException) {
                // the exception has enough information
                // (avoid developer confusion by similar message)
                throw e;
            }
            final String msg = buildParseFailureMessage(typeDocMeta);
            throw new SwaggerDefaultValueParseFailureException(msg, e);
        }
    }

    protected String buildParseFailureMessage(TypeDocMeta typeDocMeta) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Failed to parse the \"e.g. default value\" in javadoc's comment.");
        br.addItem("Advice");
        br.addElement("Make sure your \"e.g. default value\" in javadoc's comment.");
        br.addElement("Mismatched type? or Broken expression?");
        br.addElement("For example:");
        br.addElement("  (x): (mismatched type)");
        br.addElement("    /** Sea Count e.g. over */ *Bad");
        br.addElement("    public Integer seaCount;");
        br.addElement("  (o): (correct type)");
        br.addElement("    /** Sea Count e.g. 1 */ OK");
        br.addElement("    public Integer seaCount;");
        br.addElement("");
        br.addElement("  (x): (broken expression)");
        br.addElement("    /** Sea Date e.g. 2022@04-18 */ *Bad");
        br.addElement("    public LocalDate seaDate;");
        br.addElement("  (o): (correct expression)");
        br.addElement("    /** Sea Date e.g. 2022-04-18 */ OK");
        br.addElement("    public LocalDate seaDate;");
        br.addElement("");
        br.addElement("  (x): (broken expression)");
        br.addElement("    /** Sea Map e.g. {dockside=over,hangar=mystic} */ *Bad");
        br.addElement("    public Map<String, String> seaMap;");
        br.addElement("  (o): (correct map)");
        br.addElement("    /** Sea Map e.g. {dockside:over,hangar:mystic} */ OK: JSON style");
        br.addElement("    public Map<String, String> seaMap;");
        br.addItem("typeDocMeta");
        br.addElement(typeDocMeta);
        br.addItem("Property Name");
        br.addElement(typeDocMeta.getName());
        br.addItem("Property Type");
        br.addElement(typeDocMeta.getType());
        final String simpleTypeName = typeDocMeta.getSimpleTypeName();
        if (simpleTypeName != null) {
            br.addElement("(type name: " + simpleTypeName + ")");
        }
        br.addItem("Javadoc");
        br.addElement(typeDocMeta.getComment());
        return br.buildExceptionMessage();
    }

    // -----------------------------------------------------
    //                                                Scalar
    //                                                ------
    protected OptionalThing<Object> doDeriveScalarDefalutValue(TypeDocMeta typeDocMeta,
            Map<Class<?>, SwaggerSpecDataType> swaggerDataTypeMap) {
        final SwaggerSpecDataType swaggerType = swaggerDataTypeMap.get(typeDocMeta.getType());
        final Object extracted = extractDefaultValueFromComment(typeDocMeta.getComment());
        final Object defaultValue = swaggerType.defaultValueFunction.apply(typeDocMeta, extracted);
        return OptionalThing.ofNullable(defaultValue, () -> {
            throw new IllegalStateException("Not found the default value: " + typeDocMeta);
        });
    }

    // ===================================================================================
    //                                                                            Iterable
    //                                                                            ========
    protected boolean isNonNestIterable(TypeDocMeta typeDocMeta) {
        return Iterable.class.isAssignableFrom(typeDocMeta.getType()) && typeDocMeta.getNestTypeDocMetaList().isEmpty();
    }

    protected OptionalThing<Object> doDeriveListDefalutValue(TypeDocMeta typeDocMeta,
            Map<Class<?>, SwaggerSpecDataType> swaggerDataTypeMap) {
        final Object defaultValue = extractDefaultValueFromComment(typeDocMeta.getComment());
        if (!(defaultValue instanceof List)) {
            return OptionalThing.empty();
        }
        @SuppressWarnings("unchecked")
        final List<Object> defaultValueList = (List<Object>) defaultValue;
        Class<?> genericType = typeDocMeta.getGenericType();
        if (genericType == null) {
            genericType = String.class;
        }
        final SwaggerSpecDataType swaggerType = swaggerDataTypeMap.get(genericType);
        if (swaggerType != null) {
            return OptionalThing.of(defaultValueList.stream().map(value -> {
                return swaggerType.defaultValueFunction.apply(typeDocMeta, value);
            }).collect(Collectors.toList()));
        }
        return OptionalThing.empty();
    }

    // ===================================================================================
    //                                                                                Map
    //                                                                               =====
    protected boolean isNonNestMap(TypeDocMeta typeDocMeta) {
        // #for_now jflute actually nest type document is not set when map, however no problem (2022/04/19)
        return Map.class.isAssignableFrom(typeDocMeta.getType()) && typeDocMeta.getNestTypeDocMetaList().isEmpty();
    }

    protected OptionalThing<Object> doDeriveMapDefalutValue(TypeDocMeta typeDocMeta,
            Map<Class<?>, SwaggerSpecDataType> swaggerDataTypeMap) {
        // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
        // contributed by U-NEXT, thanks.
        //
        // for now, migrated as plain logic first by jflute (2022/04/18)
        // for now, mismatched type is not checked but Map is rare case by jflute (2022/04/19)
        //
        // change, delete exception info log, already improved exception handling of this handler by jflute (2022/04/19)
        // change, use linked hash map to order by default values by jflute (2022/04/19)
        // change, trim and unquoted for all types by jflute (2022/04/19)
        // change, e.g. {} is allowed as empty map by jflute (2022/04/19)
        // change, e.g. "{...}" is allowed (unquoted) for genba special supported by jflute (2022/04/19)
        // change, e.g. {dockside:{...}}, use last '}' by jflute (2022/04/19)
        // change, support map style e.g. map:{dockside={over=waves;table=waiting};hangar=mystic} by jflute (2022/04/19)
        //
        // for now again, no check mismatched type as map is rare and to keep compatible by jflute (2022/04/19)
        // _/_/_/_/_/_/_/_/_/_/

        final String[] keyValueArray = extractMapGenericTypeAsArray(typeDocMeta); // e.g. [String, Integer]
        if (keyValueArray == null || keyValueArray.length <= 1) { // non generic (or extended class of map?)
            return OptionalThing.empty();
        }
        final String keyTypeName = keyValueArray[0].trim(); // e.g. java.lang.String
        final String valueTypeName = keyValueArray[1].trim(); // e.g. java.lang.Integer
        final String comment = typeDocMeta.getComment(); // javadoc that may contain eg-default-value
        return OptionalThing.ofNullable(deriveMapDefaultValueByComment(comment, keyTypeName, valueTypeName), () -> {
            throw new IllegalStateException("not found default value: " + typeDocMeta.getName() + ", " + comment);
        });
    }

    protected String[] extractMapGenericTypeAsArray(TypeDocMeta typeDocMeta) {
        final String typeName = typeDocMeta.getTypeName(); // e.g. java.util.Map<java.lang.String, java.lang.Integer>
        if (typeName == null || !typeName.contains("<")) { // e.g. java.util.Map (non generic)
            return null;
        }
        final String genericPart = Srl.substringFirstFront(Srl.substringFirstRear(typeName, "<"), ">");
        return genericPart.split(","); // e.g. <java.lang.String, java.lang.Integer> to [java.lang.String, java.lang.Integer]
    }

    protected Object deriveMapDefaultValueByComment(String comment, String keyTypeName, String valueTypeName) {
        if (DfStringUtil.is_Null_or_Empty(comment)) {
            // attention: maybe javaparser behavior
            // if both javadoc and line comment exist, null comment here
            // e.g.
            //  /** javadoc comment e.g. ... */
            //  public String sea; // line comment
            return null;
        }
        final String egMark = "e.g.";
        if (!comment.contains(egMark)) {
            return null;
        }
        String egRearPart = Srl.substringFirstRear(comment, egMark).trim();
        if (egRearPart.startsWith("\"") && Srl.count(egRearPart, "\"") >= 2) { // may be e.g. "{...}" ...
            // genba special supported e.g. "{...}"
            egRearPart = Srl.substringLastFront(Srl.substringFirstRear(egRearPart, "\""), "\"").trim();
        }
        if (egRearPart.startsWith("{")) {
            // if "}" does not exist, use until end point e.g. {dockside:over
            final String defaultValue = Srl.substringLastFront(Srl.substringFirstRear(egRearPart, "{"), "}");
            final Map<Object, Object> map = DfCollectionUtil.newLinkedHashMap(); // as order of default values
            if (defaultValue.isEmpty()) { // e.g. {}
                return map;
            }
            for (String keyValueEntry : defaultValue.split(",")) {
                final String[] entry = keyValueEntry.split(":");
                final String key = adjustMapKeyValueFormat(entry[0]);
                final String value = adjustMapKeyValueFormat(entry[1]);
                map.put(key, value);
            }
            return map;
        } else if (egRearPart.startsWith(DfMapStyle.MAP_PREFIX + DfMapStyle.BEGIN_BRACE)) { // final weapon
            final String mapStyle = Srl.substringLastFront(egRearPart, DfMapStyle.END_BRACE) + DfMapStyle.END_BRACE;
            try {
                return new DfMapStyle().fromMapString(mapStyle); // map style challenge
            } catch (RuntimeException continued) {
                logger.debug("Failed to challenge it as map style: mapStyle=" + mapStyle, continued);
                return null; // as nothing
            }
        } else {
            return null; // e.g. dockside
        }
    }

    private String adjustMapKeyValueFormat(String exp) {
        // trim and unquote both double and single quatations
        // e.g. { "dockside" : "over", "hangar" : "mystic"} to {dockside:over,hangar:mystic}
        final String trimmed = exp.trim();
        final String filtered;
        if (Srl.isQuotedDouble(trimmed)) {
            filtered = Srl.unquoteDouble(trimmed); // keep side spaces in quotation
        } else if (Srl.isQuotedSingle(trimmed)) {
            // genba special support (single handling is only for map)
            filtered = Srl.unquoteSingle(trimmed); // me too
        } else {
            filtered = trimmed;
        }
        return filtered;
    }

    // ===================================================================================
    //                                                                               Enum
    //                                                                              ======
    protected OptionalThing<Object> doDeriveEnumDefaultValue(TypeDocMeta typeDocMeta,
            Map<Class<?>, SwaggerSpecDataType> swaggerDataTypeMap) {
        final Object defaultValue = extractDefaultValueFromComment(typeDocMeta.getComment());
        if (defaultValue != null) {
            return OptionalThing.of(defaultValue);
        }
        // use first Enum element as default
        @SuppressWarnings("unchecked")
        final Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) typeDocMeta.getType();
        final List<Map<String, String>> enumMapList = enumHandler.buildEnumMapList(enumClass);
        final Optional<Object> firstEnumElement = enumMapList.stream().map(e -> (Object) e.get("code")).findFirst();
        return OptionalThing.migratedFrom(firstEnumElement, () -> {
            throw new IllegalStateException("not found enum value.");
        });
    }

    // ===================================================================================
    //                                                                   Extract egDefault
    //                                                                   =================
    /**
     * Extract default value from comment plainly in spite of property type. 
     * @param comment The plain comment on property JavaDoc, may contain default value. (NullAllowed)
     * @return The extracted default value from the comment simply. (NullAllowed: null comment or "null")
     */
    protected Object extractDefaultValueFromComment(String comment) { // except map type
        if (DfStringUtil.is_Null_or_Empty(comment)) {
            // attention: maybe javaparser behavior
            // if both javadoc and line comment exist, null comment here
            // e.g.
            //  /** javadoc comment e.g. ... */
            //  public String sea; // line comment
            return null;
        }
        String parsedComment = comment.replaceAll("\r?\n", " ").trim();

        // adjust mark rear space (but simple support)
        parsedComment = Srl.replace(parsedComment, "e.g.  ", "e.g.").trim(); // allowed e.g.  "sea"
        parsedComment = Srl.replace(parsedComment, "e.g. ", "e.g.").trim(); // allowed e.g."sea"

        if (parsedComment.contains("e.g.\"")) {
            return Srl.substringFirstFront(Srl.substringFirstRear(parsedComment, "e.g.\""), "\"").trim();
        }
        if (parsedComment.contains("e.g.[")) {
            final String defaultValue = Srl.substringFirstFront(Srl.substringFirstRear(parsedComment, "e.g.["), "]").trim();
            return Arrays.stream(defaultValue.split(", *")).map(value -> value.trim()).map(value -> {
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    return value.substring(1, value.length() - 1);
                }
                return "null".equals(value) ? null : value;
            }).collect(Collectors.toList());
        }
        final Pattern pattern = Pattern.compile("e\\.g\\.([^ ]+)");
        final Matcher matcher = pattern.matcher(parsedComment);
        if (matcher.find()) {
            final String value = matcher.group(1);
            return "null".equals(value) ? null : value;
        }
        return null;
    }
}
