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
package org.lastaflute.meta.agent.yourswagger;

import java.util.List;
import java.util.function.Consumer;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.util.Srl;
import org.lastaflute.meta.agent.outputmeta.OutputMetaAgent;
import org.lastaflute.meta.exception.YourSwaggerDiffException;
import org.lastaflute.meta.swagger.diff.SwaggerDiff;
import org.lastaflute.meta.swagger.diff.SwaggerDiffOption;
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

    public void verifyYourSwaggerSync(String locationPath, Consumer<YourSwaggerSyncOption> opLambda) {
        if (locationPath == null) {
            throw new IllegalArgumentException("The argument 'locationPath' should not be null.");
        }
        if (opLambda == null) {
            throw new IllegalArgumentException("The argument 'opLambda' should not be null.");
        }
        final YourSwaggerSyncOption syncOption = createYourSwaggerSyncOption(opLambda);
        final SwaggerDiff diff = createSwaggerDiff(syncOption);
        final String outputSwaggerJsonPath = newOutputMetaAgent().getSwaggerJsonPath().toString();

        // SwaggerDiff's rule: left means old, right means new
        // master is your swagger here
        logger.debug("...Verifying that your swagger.json is synchronized with source codes: path={}", locationPath);
        final String diffResult = diff.diffFromLocations(outputSwaggerJsonPath, locationPath); // not null, empty allowed
        if (diffResult == null) {
            throw new IllegalStateException("The diffResult (from differ) should not be null: locationPath=" + locationPath);
        }

        // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
        // #for_now jflute improve SwaggerDiff determination (2021/05/29)
        // _/_/_/_/_/_/_/_/_/_/
        if (diffResult.isEmpty()) { // means no diff
            logger.debug("No difference, so your swagger synchronized with server implementation!");
            return;
        }
        final String diffMessage = buildYourSwaggerDiffMessage(diffResult);
        if (isLoggingResult(syncOption, diffResult)) {
            logger.info(diffMessage); // as important notification
        } else {
            throw new YourSwaggerDiffException(diffMessage);
        }
    }

    // -----------------------------------------------------
    //                                                 Ready
    //                                                 -----
    protected YourSwaggerSyncOption createYourSwaggerSyncOption(Consumer<YourSwaggerSyncOption> opLambda) {
        final YourSwaggerSyncOption syncOption = new YourSwaggerSyncOption();
        opLambda.accept(syncOption);
        return syncOption;
    }

    protected SwaggerDiff createSwaggerDiff(YourSwaggerSyncOption syncOption) {
        final List<Consumer<SwaggerDiffOption>> diffOptionSetupperList = syncOption.getSwaggerDiffOptionSetupperList();
        final SwaggerDiff diff = newSwaggerDiff(op -> {
            diffOptionSetupperList.forEach(setupper -> setupper.accept(op));
        });
        return diff;
    }

    protected SwaggerDiff newSwaggerDiff(Consumer<SwaggerDiffOption> opLambda) {
        return new SwaggerDiff(opLambda);
    }

    protected OutputMetaAgent newOutputMetaAgent() {
        return new OutputMetaAgent();
    }

    // -----------------------------------------------------
    //                                     Handle Difference
    //                                     -----------------
    protected String buildYourSwaggerDiffMessage(String diffResult) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Found the differences between your swagger.json and source codes.");
        br.addItem("Advice");
        br.addElement("The application source codes should be synchronized with your swagger.json.");
        br.addElement("So make sure your source codes e.g. Action classes (or your swagger.json).");
        br.addElement("");
        br.addElement("Your swagger.json is treated as master.");
        br.addElement("So, for example, 'New' means 'Add it to source codes'.");
        br.addItem("Diff Result");
        br.addElement(formatDiffResult(diffResult));
        return br.buildExceptionMessage();
    }

    protected String formatDiffResult(String diffResult) {
        // #for_now jflute depends on markdown render logic (2021/07/01)
        String replaced = diffResult;
        replaced = Srl.replace(replaced, "\n\n\n", "\n"); // remove large empty lines
        replaced = Srl.replace(replaced, "\n\n", "\n"); // me too
        replaced = Srl.replace(replaced, "#### What's", "\n\n#### What's"); // e.g. What's Deleted
        final String changedLine = "#### What's Changed";
        if (replaced.contains(changedLine)) {
            // add line separator before e.g. ##### `GET`, so split and restore here
            String front = Srl.substringFirstFront(replaced, changedLine);
            String rear = Srl.substringFirstRear(replaced, changedLine);
            rear = Srl.replace(rear, "\n##### ", "\n\n##### ");
            replaced = front + changedLine + rear;
        }
        return replaced;
    }

    protected boolean isLoggingResult(YourSwaggerSyncOption syncOption, String diffResult) {
        return syncOption.isLoggingIfNewOnly() && isNewOnly(diffResult);
    }

    protected boolean isNewOnly(String diffResult) {
        // #for_now jflute deeply depends on result expression (2021/06/28)
        return !diffResult.contains("Changed") && !diffResult.contains("Deleted");
    }
}
