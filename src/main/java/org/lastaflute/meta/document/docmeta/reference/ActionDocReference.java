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
package org.lastaflute.meta.document.docmeta.reference;

import java.lang.annotation.Annotation;
import java.util.List;

import org.lastaflute.meta.document.docmeta.TypeDocMeta;
import org.lastaflute.web.ruts.config.ActionExecute;

/**
 * @author jflute
 * @since 0.6.1 (2023/02/21 Wednesday at ichihara)
 */
public interface ActionDocReference {

    // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
    // see the implementation class javadoc for the property detail
    // _/_/_/_/_/_/_/_/_/_/

    // ===================================================================================
    //                                                                         Action Item
    //                                                                         ===========
    String getUrl();

    // ===================================================================================
    //                                                                          Class Item
    //                                                                          ==========
    Class<?> getType(); // means action type

    String getTypeName();

    String getSimpleTypeName();

    String getDescription();

    String getTypeComment();

    // ===================================================================================
    //                                                                          Field Item
    //                                                                          ==========
    // #for_now jflute not reference interface yet for compatible (may have setting internally?) (2024/02/21)
    List<TypeDocMeta> getFieldTypeDocMetaList();

    // ===================================================================================
    //                                                                         Method Item
    //                                                                         ===========
    ActionExecute getActionExecute();

    String getMethodName();

    String getMethodComment();

    // ===================================================================================
    //                                                                     Annotation Item
    //                                                                     ===============
    List<Annotation> getAnnotationTypeList();

    List<String> getAnnotationList(); // list of annotation name (without package)

    // ===================================================================================
    //                                                                         IN/OUT Item
    //                                                                         ===========
    // #for_now jflute not reference interface yet for compatible (may have setting internally?) (2024/02/21)
    List<TypeDocMeta> getParameterTypeDocMetaList();

    TypeDocMeta getFormTypeDocMeta();

    TypeDocMeta getReturnTypeDocMeta();

    // ===================================================================================
    //                                                                         Source Item
    //                                                                         ===========
    Integer getFileLineCount();

    Integer getMethodLineCount();
}
