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
package org.lastaflute.meta.swagger.spec.parts.enumtype;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dbflute.jdbc.Classification;
import org.dbflute.util.DfCollectionUtil;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from SwaggerGenerator (2021/06/23 Wednesday at roppongi japanese)
 */
public class SwaggerSpecEnumHandler {

    public List<Map<String, String>> buildEnumMapList(Class<? extends Enum<?>> typeClass) {
        // cannot resolve type by maven compiler, explicitly cast it
        final List<Map<String, String>> enumMapList = Arrays.stream(typeClass.getEnumConstants()).map(enumConstant -> {
            Map<String, String> map = DfCollectionUtil.newLinkedHashMap("name", enumConstant.name());
            if (enumConstant instanceof Classification) {
                map.put("code", ((Classification) enumConstant).code());
                map.put("alias", ((Classification) enumConstant).alias());
            } else {
                map.put("code", enumConstant.name());
                map.put("alias", "");
            }
            return map;
        }).collect(Collectors.toList());
        return enumMapList;
    }
}
