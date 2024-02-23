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
package org.lastaflute.meta.swagger.spec.parts.httpstatus;

import java.util.List;
import java.util.Map;

import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.Srl;

/**
 * @author jflute
 * @since 0.6.1 (2024/02/23 Friday at ichihara)
 */
public class SwaggerSpecHttpStatusThrowsExtractor {

    public static final String THROWS_MAP_KEY_EXCEPTION = "exception";
    public static final String THROWS_MAP_KEY_DESCRIPTION = "description";

    public Map<String, List<Map<String, String>>> extractStatusThrowsMap(String methodComment) {
        if (Srl.is_Null_or_TrimmedEmpty(methodComment)) {
            throw new IllegalArgumentException("The argument 'methodComment' should not be null.");
        }
        final Map<String, List<Map<String, String>>> statusThrowsMap = DfCollectionUtil.newLinkedHashMap();
        final List<String> splitList = Srl.splitListTrimmed(methodComment, "\n");
        for (String line : splitList) {
            final String throwsPrefix = "@throws";
            if (!line.startsWith(throwsPrefix)) {
                continue; // not throws line
            }
            final String[] spaces = new String[] { " ", "\t", "\u3000" };
            if (!Srl.startsWith(Srl.substringFirstRear(line, throwsPrefix), spaces)) {
                continue; // throws mock line e.g. @throwWhen...
            }
            // @throws line here
            if (!line.contains("(") || !line.endsWith(")")) {
                continue; // not having () line
            }
            // having rear () here
            final String statusExp = Srl.substringLastFront(Srl.substringLastRear(line, "("), ")").trim();
            // cannot use scope last for the case e.g. ...When the (^^sea is not found (400)
            //final ScopeInfo statusScope = Srl.extractScopeLast(line, "(", ")");
            if (!Srl.isNumberHarfAll(statusExp)) { // strict for now
                continue; // e.g. (A04)
            }

            final String exceptionRear = Srl.substringFirstRear(line, throwsPrefix).trim();
            if (!Srl.containsAny(exceptionRear, spaces)) { // e.g. ...ExceptionWhen...(400)
                continue; // broken throws?
            }
            final String exception = Srl.substringFirstFront(exceptionRear, spaces).trim();
            final String descriptionRear = Srl.substringFirstRear(exceptionRear, spaces).trim();
            final String description = trimAlsoFullWidthSpace(Srl.substringLastFront(descriptionRear, "("));

            List<Map<String, String>> throwsList = statusThrowsMap.get(statusExp);
            if (throwsList == null) {
                throwsList = DfCollectionUtil.newArrayList();
                statusThrowsMap.put(statusExp, throwsList);
            }
            final Map<String, String> throwsMap = DfCollectionUtil.newLinkedHashMap();
            throwsMap.put(THROWS_MAP_KEY_EXCEPTION, exception);
            throwsMap.put(THROWS_MAP_KEY_DESCRIPTION, description);
            throwsList.add(throwsMap);
        }
        return statusThrowsMap;
    }

    protected String trimAlsoFullWidthSpace(String targetStr) {
        // #for_now jflute not perfect, nested spaces cannot be trimmed, but almost no problem (2024/02/23)
        return Srl.removeSuffix(Srl.removePrefix(targetStr.trim(), "\u3000"), "\u3000").trim();
    }
}
