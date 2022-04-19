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
package org.lastaflute.meta.swagger.spec.parts.defaultvalue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dbflute.optional.OptionalThing;
import org.dbflute.utflute.core.PlainTestCase;
import org.lastaflute.core.json.control.JsonControlMeta;
import org.lastaflute.meta.document.docmeta.TypeDocMeta;
import org.lastaflute.meta.exception.SwaggerDefaultValueParseFailureException;
import org.lastaflute.meta.exception.SwaggerDefaultValueTypeConversionFailureException;
import org.lastaflute.meta.swagger.spec.parts.datatype.SwaggerSpecDataTypeHandler;
import org.lastaflute.meta.swagger.spec.parts.enumtype.SwaggerSpecEnumHandler;
import org.lastaflute.meta.unit.mock.dbflute.MockCDef;

/**
 * @author jflute
 * @since 0.5.4 (2022/04/18 Monday at roppongi japanese)
 */
public class SwaggerSpecDefaultValueHandlerTest extends PlainTestCase {

    // ===================================================================================
    //                                                                              String
    //                                                                              ======
    public void test_deriveDefaultValue_string_basic() {
        assertEquals("SeaOfDreams", handler().deriveDefaultValue(stringMeta("Sea Name e.g. SeaOfDreams")).get());
        assertEquals("SeaOfDreams", handler().deriveDefaultValue(stringMeta("Sea Name e.g. SeaOfDreams (NullAllowed)")).get());
        assertEquals("1", handler().deriveDefaultValue(stringMeta("Sea Name e.g. 1")).get());
        assertEquals("1.2", handler().deriveDefaultValue(stringMeta("Sea Name e.g. 1.2")).get());

        // done jflute allow this case for easy comment (2022/04/18)
        assertEquals("SeaOfDreams", handler().deriveDefaultValue(stringMeta("e.g. SeaOfDreams")).get());
        assertEquals("SeaOfDreams", handler().deriveDefaultValue(stringMeta(" e.g. SeaOfDreams")).get());
    }

    public void test_deriveDefaultValue_string_space() {
        // #thinking jflute non quoted value may be treated as value until comment end... (2022/04/18)
        assertEquals("Sea", handler().deriveDefaultValue(stringMeta("Sea Name e.g. Sea of Dreams")).get());

        // done jflute too strict so allow these cases for easy comment (2022/04/18)
        // supported since 0.5.4
        //assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e.g.  SeaOfDreams")).isPresent());
        //assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e.g.  \"Sea of Dreams\"")).isPresent());
        assertEquals("SeaOfDreams", handler().deriveDefaultValue(stringMeta("Sea Name e.g.  SeaOfDreams")).get());
        assertEquals("Sea of Dreams", handler().deriveDefaultValue(stringMeta("Sea Name e.g.  \"Sea of Dreams\"")).get());
        assertEquals("SeaOfDreams", handler().deriveDefaultValue(stringMeta("Sea Name e.g.   SeaOfDreams")).get());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e.g.    SeaOfDreams")).isPresent()); // four spaces bad
        // supported since 0.5.4
        //assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e.g.SeaOfDreams")).isPresent());
        assertEquals("SeaOfDreams", handler().deriveDefaultValue(stringMeta("Sea Name e.g.SeaOfDreams")).get());
        assertEquals("Sea of Dreams", handler().deriveDefaultValue(stringMeta("Sea Name e.g.\"Sea of Dreams\"")).get());
        assertEquals("SeaOfDreams", handler().deriveDefaultValue(stringMeta("Sea Namee.g. SeaOfDreams")).get());
        assertEquals("Sea of Dreams", handler().deriveDefaultValue(stringMeta("Sea Namee.g. \"Sea of Dreams\"")).get());
        // supported since 0.5.4
        //assertFalse(handler().deriveDefaultValue(stringMeta("Sea Namee.g.SeaOfDreams")).isPresent());
        assertEquals("SeaOfDreams", handler().deriveDefaultValue(stringMeta("Sea Namee.g.SeaOfDreams")).get());
        assertEquals("Sea of Dreams", handler().deriveDefaultValue(stringMeta("Sea Namee.g.\"Sea of Dreams\"")).get());
    }

    public void test_deriveDefaultValue_string_quoted() {
        assertEquals("Sea of Dreams", handler().deriveDefaultValue(stringMeta("Sea Name e.g. \"Sea of Dreams\"")).get());
        assertEquals("Sea of Dreams", handler().deriveDefaultValue(stringMeta("Sea Name e.g. \"Sea of Dreams\" (NullAllowed)")).get());
        assertEquals("Sea of Dreams", handler().deriveDefaultValue(stringMeta("Sea Name e.g. \"Sea of Dreams")).get());
        assertEquals("Sea", handler().deriveDefaultValue(stringMeta("Sea Name e.g. Sea of Dreams\"")).get());
        assertEquals("Dreams", handler().deriveDefaultValue(stringMeta("Sea Name e.g. \"Dreams\"")).get());
        assertEquals("Dreams", handler().deriveDefaultValue(stringMeta("Sea Name e.g. \"Dreams")).get());
        assertEquals("Dreams\"", handler().deriveDefaultValue(stringMeta("Sea Name e.g. Dreams\"")).get());
        assertEquals("Sea", handler().deriveDefaultValue(stringMeta("Sea Name e.g. \"Sea\" of Dreams")).get());
    }

    public void test_deriveDefaultValue_string_none() {
        // supported since 0.5.4
        //assertFalse(handler().deriveDefaultValue(stringMeta("e.g. SeaOfDreams")).isPresent());
        assertEquals("SeaOfDreams", handler().deriveDefaultValue(stringMeta("e.g. SeaOfDreams")).get());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e.g. null")).isPresent());
        assertEquals("NULL", handler().deriveDefaultValue(stringMeta("Sea Name e.g. NULL")).get());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name Sea of Dreams")).isPresent());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e.Sea of Dreams")).isPresent());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e.gSea of Dreams")).isPresent());

        // supported since 0.5.4
        //assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e.g.SeaOfDreams")).isPresent());
        assertEquals("SeaOfDreams", handler().deriveDefaultValue(stringMeta("Sea Name e.g.SeaOfDreams")).get());
        assertEquals("Sea of Dreams", handler().deriveDefaultValue(stringMeta("Sea Name e.g.\"Sea of Dreams\"")).get());

        assertEquals("SeaOfDreams", handler().deriveDefaultValue(stringMeta("Sea Name e.g.  SeaOfDreams")).get());
        assertEquals("Sea of Dreams", handler().deriveDefaultValue(stringMeta("Sea Name e.g.  \"Sea of Dreams\"")).get());
        assertEquals("SeaOfDreams", handler().deriveDefaultValue(stringMeta("Sea Name e.g.   SeaOfDreams")).get());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e.g.    SeaOfDreams")).isPresent()); // four spaces bad
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e.g.")).isPresent());
        assertEquals("SeaOfDreams", handler().deriveDefaultValue(stringMeta("Sea Namee.g. SeaOfDreams")).get());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name eg. Sea of Dreams")).isPresent());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e.g Sea of Dreams")).isPresent());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name eg Sea of Dreams")).isPresent());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e.g . Sea of Dreams")).isPresent());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e. g. Sea of Dreams")).isPresent());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e .g. Sea of Dreams")).isPresent());
        // behavior changed since 0.5.4, no problem
        //assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e.g.\tSeaOfDreams")).isPresent());
        assertEquals("\tSeaOfDreams", handler().deriveDefaultValue(stringMeta("Sea Name e.g.\tSeaOfDreams")).get());
        assertEquals("SeaOfDreams", handler().deriveDefaultValue(stringMeta("Sea Name e.g.\nSeaOfDreams")).get());
        assertEquals("SeaOfDreams", handler().deriveDefaultValue(stringMeta("Sea Name e.g.\n\nSeaOfDreams")).get());
    }

    public void test_deriveDefaultValue_string_various() {
        assertEquals("Sea.of.Dreams", handler().deriveDefaultValue(stringMeta("Sea Name e.g. Sea.of.Dreams")).get());
        assertEquals("Sea.e.g.Dreams", handler().deriveDefaultValue(stringMeta("Sea Name e.g. \"Sea.e.g.Dreams\"")).get());
        assertEquals(Arrays.asList("Sea of Dreams"), handler().deriveDefaultValue(stringMeta("Sea Name e.g. [Sea of Dreams]")).get());
        assertEquals("{Sea", handler().deriveDefaultValue(stringMeta("Sea Name e.g. {Sea of Dreams}")).get());
    }

    private TypeDocMeta stringMeta(String comment) {
        return createMeta("seaName", String.class, null, comment);
    }

    // ===================================================================================
    //                                                                              Number
    //                                                                              ======
    public void test_deriveDefaultValue_number_basic() {
        assertEquals(1, handler().deriveDefaultValue(integerMeta("Sea Count e.g. 1")).get());
        assertEquals(123456789, handler().deriveDefaultValue(integerMeta("Sea Count e.g. 123456789")).get());
        assertEquals(1234567890123456L, handler().deriveDefaultValue(longMeta("Sea Count e.g. 1234567890123456")).get());
        assertEquals(BigDecimal.ONE, handler().deriveDefaultValue(decimalMeta("Sea Count e.g. 1")).get());
        assertEquals(new BigDecimal("1.2"), handler().deriveDefaultValue(decimalMeta("Sea Count e.g. 1.2")).get());

        // none
        assertException(SwaggerDefaultValueTypeConversionFailureException.class, () -> {
            handler().deriveDefaultValue(integerMeta("Sea Count e.g. 1.0")); // integer is not decimal
        });
        assertException(SwaggerDefaultValueTypeConversionFailureException.class, () -> {
            handler().deriveDefaultValue(integerMeta("Sea Count e.g. mystic"));
        });
        assertException(SwaggerDefaultValueTypeConversionFailureException.class, () -> {
            handler().deriveDefaultValue(integerMeta("Sea Count e.g. 1234567890123456")); // too big for integer
        });
    }

    private TypeDocMeta integerMeta(String comment) {
        return createMeta("seaCount", Integer.class, null, comment);
    }

    private TypeDocMeta longMeta(String comment) {
        return createMeta("seaMoney", Long.class, null, comment);
    }

    private TypeDocMeta decimalMeta(String comment) {
        return createMeta("seaPercent", BigDecimal.class, null, comment);
    }

    // ===================================================================================
    //                                                                               Date
    //                                                                              ======
    // scalar types depend on SwaggerSpecDataTypeHandler so enough here

    // ===================================================================================
    //                                                                            Iterable
    //                                                                            ========
    public void test_deriveDefaultValue_iterable_basic() {
        // basic
        assertEquals(Arrays.asList("dockside", "hangar"),
                handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g. [dockside, hangar]")).get());
        assertEquals(Arrays.asList("mystic"), handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g. [mystic]")).get());

        // quoted
        assertEquals(Arrays.asList("dockside", "hangar"),
                handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g. [\"dockside\", \"hangar\"]")).get());
        assertEquals(Arrays.asList("mystic"), handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g. [\"mystic\"]")).get());

        // comma space
        assertEquals(Arrays.asList("dockside", "hangar"),
                handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g. [\"dockside\",\"hangar\"]")).get());

        // mark space
        // supported since 0.5.4
        //assertFalse(handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g.[\"dockside\", \"hangar\"]")).isPresent());
        assertEquals(Arrays.asList("dockside", "hangar"),
                handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g.[\"dockside\", \"hangar\"]")).get());
        assertEquals(Arrays.asList("dockside", "hangar"),
                handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g.  [\"dockside\", \"hangar\"]")).get());

        // rear space
        assertEquals(Arrays.asList("dockside", "hangar"),
                handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g. [dockside, hangar] ")).get());
        assertEquals(Arrays.asList("dockside", "hangar"),
                handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g. [dockside, hangar] rear comment")).get());

        // value space
        assertEquals(Arrays.asList("dock side", "han gar"),
                handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g. [\"dock side\", \"han gar\"]")).get());
        assertEquals(Arrays.asList("dock side", "han gar"),
                handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g. [dock side, han gar]")).get());
        assertEquals(Arrays.asList("\"dockside", "hangar\""), // comma is prior
                handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g. [\"dockside, hangar\"]")).get());

        // done jflute trim side space in List elements (2022/04/19)
        // side space
        assertEquals(Arrays.asList("dockside", "hangar"), // not trimmed => trimmed since 0.5.4
                handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g. [  \"dockside\" , \"hangar\" ]")).get());
        assertEquals(Arrays.asList("dockside", "hangar"), // not trimmed (but only hangar front trimmed) => trimmed since 0.5.4
                handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g. [ dockside,  hangar ]")).get());

        // number
        assertEquals(Arrays.asList(1, 2), handler().deriveDefaultValue(listMeta(Integer.class, "Sea List e.g. [1, 2]")).get());
        assertEquals(Arrays.asList(1, 2), handler().deriveDefaultValue(listMeta(Integer.class, "Sea List e.g. [\"1\", \"2\"]")).get());

        assertException(SwaggerDefaultValueTypeConversionFailureException.class, () -> {
            handler().deriveDefaultValue(listMeta(Integer.class, "Sea List e.g. [\"dockside\", \"hangar\"]"));
        });
        assertException(SwaggerDefaultValueTypeConversionFailureException.class, () -> {
            handler().deriveDefaultValue(listMeta(Integer.class, "Sea List e.g. [\"1\", \"hangar\"]"));
        });

        // non generic (treated as string)
        assertEquals(Arrays.asList("dockside", "hangar"),
                handler().deriveDefaultValue(listMeta(null, "Sea List e.g. [\"dockside\", \"hangar\"]")).get());

        // broken
        assertFalse(handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g. \"dockside\", \"hangar\"")).isPresent());
        assertEquals(Arrays.asList("dock[side", "hangar"),
                handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g. [dock[side, hangar]")).get());
        assertEquals(Arrays.asList("dock"),
                handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g. [dock]side, hangar]")).get());
        assertEquals(Arrays.asList("dockside", "hangar"),
                handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g. [\"dockside\", \"hangar\"")).get());
        assertFalse(handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g. \"dockside\", \"hangar\"]")).isPresent());

        // non
        assertFalse(handler().deriveDefaultValue(listMeta(String.class, "Sea List")).isPresent());
        // supported since 0.5.4
        //assertFalse(handler().deriveDefaultValue(listMeta(String.class, "e.g. [dockside, hangar]")).isPresent());
        assertEquals(Arrays.asList("dockside", "hangar"),
                handler().deriveDefaultValue(listMeta(String.class, "e.g. [dockside, hangar]")).get());

    }

    private TypeDocMeta listMeta(Class<?> genericType, String comment) {
        return createMeta("seaList", List.class, genericType, comment);
    }

    // ===================================================================================
    //                                                                                Map
    //                                                                               =====
    public void test_deriveDefaultValue_map_basic() { // supported since 0.5.4 (unsupported until 0.5.3)
        // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
        // test contributed code
        // that was migrated as plain logic first by jflute (2022/04/18)
        // problem candidates:
        //  o (resolved) not trimmed
        //  o (resolved) not unquoted
        //  o many ArrayIndexOutOfBoundsException cases
        // _/_/_/_/_/_/_/_/_/_/

        // basic
        assertEquals(newHashMap("dockside", "over", "hangar", "mystic"),
                handler().deriveDefaultValue(mapMeta("Sea Map e.g. {dockside:over,hangar:mystic}")).get());
        assertEquals(newHashMap("dockside", "over"), handler().deriveDefaultValue(mapMeta("Sea Map e.g. {dockside:over}")).get());
        assertEquals(newHashMap("dockside", "/over/the/waves.jpg", "hangar", "mys_tic"),
                handler().deriveDefaultValue(mapMeta("Sea Map e.g. {dockside:/over/the/waves.jpg,hangar:mys_tic}")).get());

        // quoted, quotation remained, problem? => resolved
        assertEquals(newHashMap("dockside", "over", "hangar", "mystic"),
                handler().deriveDefaultValue(mapMeta("Sea Map e.g. {\"dockside\":\"over\",\"hangar\":\"mystic\"}")).get());
        assertEquals(newHashMap("dockside", "over"), handler().deriveDefaultValue(mapMeta("Sea Map e.g. {\"dockside\":\"over\"}")).get());
        assertEquals(newHashMap("dockside", "over", "hangar", "mystic"),
                handler().deriveDefaultValue(mapMeta("Sea Map e.g. {'dockside':'over','hangar':'mystic'}")).get());
        assertEquals(newHashMap("dockside", "over", "hangar", "mystic"), // genba (special supported)
                handler().deriveDefaultValue(mapMeta("Sea Map e.g. \"{'dockside':'over','hangar':'mystic'}\"")).get());
        assertEquals(newHashMap("dockside", "over", "hangar", "mystic"), // genba (special supported)
                handler().deriveDefaultValue(mapMeta("Sea Map e.g. \"{'dockside':'over','hangar':'mystic'}\" (NullAllowed)")).get());

        // mark space
        assertEquals(newHashMap("dockside", "over"), // improved
                handler().deriveDefaultValue(mapMeta("e.g.{dockside:over}")).get());
        assertEquals(newHashMap("dockside", "over"), // improved
                handler().deriveDefaultValue(mapMeta("e.g.  {dockside:over}")).get());

        // rear space
        assertEquals(newHashMap("dockside", "over", "hangar", "mystic"), // rear space
                handler().deriveDefaultValue(mapMeta("Sea Map e.g. {dockside:over,hangar:mystic} ")).get());
        assertEquals(newHashMap("dockside", "over", "hangar", "mystic"), // rear space
                handler().deriveDefaultValue(mapMeta("Sea Map e.g. {dockside:over,hangar:mystic} rear comment")).get());
        assertEquals(newHashMap("dockside", "over", "hangar", "mystic"), // genba
                handler().deriveDefaultValue(mapMeta("Sea Map e.g. {dockside:over,hangar:mystic} (NullAllowed)")).get());

        // value space
        assertEquals(newHashMap("dockside", "over", "hangar", "mystic"), // non trimmed, problem? => resolved
                handler().deriveDefaultValue(mapMeta("Sea Map e.g. {dockside : over, hangar : mystic}")).get());
        assertEquals(newHashMap("dock side", "over the waves", "han gar", "mys tic"),
                handler().deriveDefaultValue(mapMeta("Sea Map e.g. {dock side:over the waves,han gar:mys tic}")).get());

        // no description, different from others, however no problem
        assertEquals(newHashMap("dockside", "over"), handler().deriveDefaultValue(mapMeta("e.g. {dockside:over}")).get());

        // none
        assertFalse(handler().deriveDefaultValue(mapMeta("Sea Map")).isPresent());
        assertFalse(handler().deriveDefaultValue(mapMeta("Sea Map e.g.")).isPresent());
        assertEquals(newHashMap(), handler().deriveDefaultValue(mapMeta("Sea Map e.g. {}")).get()); // problem? => resolved
        assertFalse(handler().deriveDefaultValue(mapMeta("e.g. dockside:over")).isPresent());
        assertFalse(handler().deriveDefaultValue(mapMeta("e.g. dockside:over}")).isPresent());
        assertFalse(handler().deriveDefaultValue(mapMeta("e.g. [dockside:over]")).isPresent());

        // broken
        assertEquals(newHashMap("dockside", "over"), // keep contributed result
                handler().deriveDefaultValue(mapMeta("Sea Map e.g. {dockside:over")).get());
        assertEquals(newHashMap("dockside", "over the waves"), // different from others, however no problem
                handler().deriveDefaultValue(mapMeta("Sea Map e.g. {dockside:over the waves")).get());
        assertException(SwaggerDefaultValueParseFailureException.class, () -> {
            handler().deriveDefaultValue(mapMeta("Sea Map e.g. {dockside=over"));
        });
        assertException(SwaggerDefaultValueParseFailureException.class, () -> {
            handler().deriveDefaultValue(mapMeta("Sea Map e.g. {dockside=over, hangar:mystic}"));
        });
        assertEquals(newHashMap("dockside", "over"), // different from others, however no problem
                handler().deriveDefaultValue(mapMeta("Sea Mape.g. {dockside:over")).get());

        // integer
        assertEquals(newHashMap("dockside", "1", "hangar", "2"),
                handler().deriveDefaultValue(mapMetaAsInteger("Sea Map e.g. {dockside:1,hangar:2}")).get());
        assertEquals(newHashMap("dockside", "over", "hangar", "mystic"), // unrelated to integer type, no problem?
                handler().deriveDefaultValue(mapMetaAsInteger("Sea Map e.g. {dockside:over,hangar:mystic}")).get());

        // boolean
        assertEquals(newHashMap("dockside", "true", "hangar", "false"),
                handler().deriveDefaultValue(mapMetaAsBoolean("Sea Map e.g. {dockside:true,hangar:false}")).get());
        assertEquals(newHashMap("dockside", "true", "hangar", "false"), // genba
                handler().deriveDefaultValue(mapMetaAsBoolean("Sea Map e.g. {'dockside': true, 'hangar': false}")).get());

        // non generic
        assertFalse(handler().deriveDefaultValue(mapMetaNonGeneric("e.g. {dockside:over}")).isPresent());

        // nest list (unsupported)
        assertEquals(newHashMap("dockside", "[overthewaves]"),
                handler().deriveDefaultValue(mapMetaAsBoolean("Sea Map e.g. {dockside:[overthewaves]}")).get());
        assertException(SwaggerDefaultValueParseFailureException.class, () -> {
            handler().deriveDefaultValue(mapMeta("Sea Map e.g. {dockside:[over,the]}"));
        });

        // nest map (unsupported)
        {
            Map<String, String> muchaKuchaMap = new LinkedHashMap<>();
            muchaKuchaMap.put("dockside", "{over");
            muchaKuchaMap.put("table", "waiting}");
            muchaKuchaMap.put("hangar", "mystic");
            assertEquals(muchaKuchaMap, // if you need nest map, use map:{} style
                    handler().deriveDefaultValue(mapMeta("Sea Map e.g. {dockside:{over:waves,table:waiting},hangar:mystic}")).get());
        }

        // map style
        {
            Map<String, Object> mapStyleMap = new LinkedHashMap<>();
            mapStyleMap.put("dockside", newLinkedHashMap("over", "waves", "table", "waiting"));
            mapStyleMap.put("hangar", "mystic");
            assertEquals(mapStyleMap, handler().deriveDefaultValue( // if you need nest map, use map:{} style
                    mapMeta("Sea Map e.g. map:{dockside = map:{over =waves;table= waiting}; hangar =mystic} (NullAllowed)")).get());
        }
    }

    private TypeDocMeta mapMeta(String comment) {
        return createMetaForMap("seaMap", String.class, String.class, comment);
    }

    private TypeDocMeta mapMetaAsInteger(String comment) {
        return createMetaForMap("seaMap", String.class, Integer.class, comment);
    }

    private TypeDocMeta mapMetaAsBoolean(String comment) {
        return createMetaForMap("seaMap", String.class, Boolean.class, comment);
    }

    private TypeDocMeta mapMetaNonGeneric(String comment) {
        return createMetaForMap("seaMap", null, null, comment);
    }

    // ===================================================================================
    //                                                                               Enum
    //                                                                              ======
    public void test_deriveDefaultValue_enum_basic() {
        assertEquals("FML", handler().deriveDefaultValue(statusMeta("Sea Status e.g. FML")).get());
        assertEquals("FML", handler().deriveDefaultValue(statusMeta("Sea Status e.g. \"FML\"")).get());
        assertEquals("Formalized", handler().deriveDefaultValue(statusMeta("Sea Status e.g. \"Formalized\"")).get());
        assertEquals("PRV", handler().deriveDefaultValue(statusMeta("Sea Status e.g. PRV")).get());
        assertEquals("NON", handler().deriveDefaultValue(statusMeta("Sea Status e.g. \"NON\"")).get());

        // none (first element of enum)
        assertEquals("FML", handler().deriveDefaultValue(statusMeta("Sea Status")).get());
        assertEquals("DOCKSIDE", handler().deriveDefaultValue(enumMeta(TestBasicEnum.class, "Seanum")).get());
        assertFalse(handler().deriveDefaultValue(enumMeta(TestEmptyEnum.class, "Seanum")).isPresent());
    }

    private TypeDocMeta statusMeta(String comment) {
        return createMeta("seaType", MockCDef.MemberStatus.class, null, comment);
    }

    private TypeDocMeta enumMeta(Class<?> enumType, String comment) {
        return createMeta("seaNum", enumType, null, comment);
    }

    private enum TestBasicEnum {
        DOCKSIDE, HANGAR;
    }

    private enum TestEmptyEnum {
        ;
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    private SwaggerSpecDefaultValueHandler handler() {
        JsonControlMeta appJsonControlMeta = new JsonControlMeta(OptionalThing.empty(), OptionalThing.empty());
        SwaggerSpecDataTypeHandler dataTypeHandler = new SwaggerSpecDataTypeHandler(appJsonControlMeta);
        SwaggerSpecEnumHandler enumHandler = new SwaggerSpecEnumHandler();
        return new SwaggerSpecDefaultValueHandler(dataTypeHandler, enumHandler);
    }

    private TypeDocMeta createMeta(String name, Class<?> type, Class<?> genericType, String comment) {
        // only used in the handler (minimum set)
        TypeDocMeta typeDocMeta = new TypeDocMeta();
        typeDocMeta.setName(name);
        typeDocMeta.setType(type);
        typeDocMeta.setGenericType(genericType);
        typeDocMeta.setComment(comment);
        return typeDocMeta;
    }

    private TypeDocMeta createMetaForMap(String name, Class<?> keyType, Class<?> valueType, String comment) {
        // only used in the handler (minimum set)
        TypeDocMeta typeDocMeta = new TypeDocMeta();
        typeDocMeta.setName(name);
        typeDocMeta.setType(Map.class);
        StringBuilder sb = new StringBuilder();
        sb.append("java.util.Map");
        if (keyType != null && valueType != null) {
            sb.append("<").append(keyType.getName()).append(", ").append(valueType.getName()).append(">");
        }
        typeDocMeta.setTypeName(sb.toString());
        typeDocMeta.setComment(comment);
        return typeDocMeta;
    }
}
