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
package org.lastaflute.meta.unit.mock.web;

import javax.validation.Valid;

import org.hibernate.validator.constraints.Length;
import org.lastaflute.web.validation.Required;

/**
 * @author jflute
 */
public class SeaForm {

    @Required
    public Integer stageId;

    @Required
    @Length(min = 2, max = 32)
    public String stageName;

    @Valid
    public HangarPart hangar;

    public static class HangarPart {

        @Required
        public String showName;
    }
}
