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
package org.lastaflute.meta.document.docmeta.reference;

import java.util.List;
import java.util.Map;

import org.lastaflute.meta.document.docmeta.TypeDocMeta;

/**
 * @author jflute
 * @since 0.6.1 (2023/02/21 Wednesday at ichihara)
 */
public interface JobDocReference {

    // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
    // see the implementation class javadoc for the property detail
    // _/_/_/_/_/_/_/_/_/_/

    String getJobKey();

    String getJobUnique();

    String getJobTitle();

    String getJobDescription();

    String getCronExp();

    String getTypeName();

    String getSimpleTypeName();

    String getDescription();

    String getTypeComment();

    // #for_now jflute not reference interface yet for compatible (may have setting internally?) (2024/02/21)
    List<TypeDocMeta> getFieldTypeDocMetaList();

    String getMethodName();

    String getMethodComment();

    Map<String, Object> getParams();

    String getNoticeLogLevel();

    String getConcurrentExec();

    List<String> getTriggeredJobKeyList();

    Integer getFileLineCount();

    Integer getMethodLineCount();
}
