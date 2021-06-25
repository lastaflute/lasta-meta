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
package org.lastaflute.meta.swagger.spec.parts.produces;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.lastaflute.meta.document.docmeta.ActionDocMeta;
import org.lastaflute.meta.document.docmeta.TypeDocMeta;
import org.lastaflute.meta.swagger.spec.parts.datatype.SwaggerSpecDataType;
import org.lastaflute.meta.swagger.spec.parts.datatype.SwaggerSpecDataTypeHandler;
import org.lastaflute.web.response.HtmlResponse;
import org.lastaflute.web.response.JsonResponse;
import org.lastaflute.web.response.StreamResponse;
import org.lastaflute.web.response.XmlResponse;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from SwaggerGenerator (2021/06/25 Friday at roppongi japanese)
 */
public class SwaggerSpecProducesHandler {

    protected static final Map<Class<?>, List<String>> produceMap;
    static {
        final Map<Class<?>, List<String>> workingMap = DfCollectionUtil.newHashMap();
        workingMap.put(JsonResponse.class, Arrays.asList("application/json"));
        workingMap.put(XmlResponse.class, Arrays.asList("application/xml"));
        workingMap.put(HtmlResponse.class, Arrays.asList("text/html"));
        workingMap.put(StreamResponse.class, Arrays.asList("application/octet-stream"));
        produceMap = Collections.unmodifiableMap(workingMap);
    }

    protected final SwaggerSpecDataTypeHandler dataTypeHandler;

    public SwaggerSpecProducesHandler(SwaggerSpecDataTypeHandler dataTypeHandler) {
        this.dataTypeHandler = dataTypeHandler;
    }

    public OptionalThing<List<String>> deriveProduces(ActionDocMeta actionDocMeta) {
        final TypeDocMeta returnTypeDocMeta = actionDocMeta.getReturnTypeDocMeta();
        if (Arrays.asList(void.class, Void.class).contains(returnTypeDocMeta.getGenericType())) {
            return OptionalThing.empty();
        }
        // #hope jflute should be cached for performance? (2021/06/25)
        final Map<Class<?>, SwaggerSpecDataType> dataTypeMap = dataTypeHandler.createSwaggerDataTypeMap();
        if (dataTypeMap.containsKey(returnTypeDocMeta.getGenericType())) {
            return OptionalThing.of(Arrays.asList("text/plain;charset=UTF-8"));
        }
        final Class<?> produceType = returnTypeDocMeta.getType();
        final List<String> produceList = produceMap.get(produceType);
        return OptionalThing.ofNullable(produceList, () -> {
            String msg = "Not found the produce: type=" + produceType + ", keys=" + produceMap.keySet();
            throw new IllegalStateException(msg);
        });
    }
}
