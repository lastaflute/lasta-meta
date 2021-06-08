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
package org.lastaflute.meta.agent.yourswagger;

import java.util.function.Consumer;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.lastaflute.meta.agent.outputmeta.OutputMetaAgent;
import org.lastaflute.meta.diff.SwaggerDiffGenerator;
import org.lastaflute.meta.diff.SwaggerDiffOption;
import org.lastaflute.meta.exception.YourSwaggerDiffException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The agent for synchronization of your swagger.json with Lasta-presents swagger.json. <br>
 * It depends SwaggerDiff classes so you need to set the "openapi-diff-core" library at your build settings.
 * @author jflute
 * @since 0.5.1 (2021/05/29 Saturday at ronppongi japanese)
 */
public class YourSwaggerSyncAgent { // used by e.g. UTFlute

    private static final Logger logger = LoggerFactory.getLogger(YourSwaggerSyncAgent.class);

    public void verifyYourSwaggerSync(String locationPath, Consumer<SwaggerDiffOption> opLambda) {
        if (locationPath == null) {
            throw new IllegalArgumentException("The argument 'locationPath' should not be null.");
        }
        if (opLambda == null) {
            throw new IllegalArgumentException("The argument 'opLambda' should not be null.");
        }
        logger.debug("...Verifying that your swagger.json is synchronized with source codes: path={}", locationPath);
        final SwaggerDiffGenerator diff = newSwaggerDiff(opLambda);
        final String outputSwaggerJsonPath = newOutputMetaAgent().getSwaggerJsonPath().toString();

        // SwaggerDiff's rule: left means old, right means new
        // master is your swagger here
        final String diffResult = diff.diffFromLocations(outputSwaggerJsonPath, locationPath);

        // TODO awaawa improve SwaggerDiff determination by jflute (2021/05/29)
        if (!diffResult.isEmpty()) { // has differences
            throwYourSwaggerDiffException(diffResult);
        }
    }

    protected SwaggerDiffGenerator newSwaggerDiff(Consumer<SwaggerDiffOption> opLambda) {
        return new SwaggerDiffGenerator(opLambda);
    }

    protected OutputMetaAgent newOutputMetaAgent() {
        return new OutputMetaAgent();
    }

    protected void throwYourSwaggerDiffException(String diffResult) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Found the differences between your swagger.json and source codes.");
        br.addItem("Advice");
        br.addElement("The application source codes should be synchronized with your swagger.json.");
        br.addElement("So make sure your source codes e.g. Action classes (or your swagger.json).");
        br.addElement("");
        br.addElement("Your swagger.json is treated as master.");
        br.addElement("So, for example, 'New' means 'Add it to source codes'.");
        br.addItem("Diff Result");
        br.addElement(diffResult);
        final String msg = br.buildExceptionMessage();
        throw new YourSwaggerDiffException(msg);
    }
}
