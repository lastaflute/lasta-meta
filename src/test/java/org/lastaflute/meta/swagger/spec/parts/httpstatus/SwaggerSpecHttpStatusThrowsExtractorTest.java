package org.lastaflute.meta.swagger.spec.parts.httpstatus;

import java.util.List;
import java.util.Map;

import org.dbflute.utflute.core.PlainTestCase;

/**
 * @author jflute
 * @since 0.6.1 (2024/02/23 Friday at ichihara)
 */
public class SwaggerSpecHttpStatusThrowsExtractorTest extends PlainTestCase {

    // ===================================================================================
    //                                                                               Basic
    //                                                                               =====
    public void test_extractStatusThrowsMap_basic_orthodox() {
        // ## Arrange ##
        SwaggerSpecHttpStatusThrowsExtractor extractor = new SwaggerSpecHttpStatusThrowsExtractor();
        StringBuilder sb = new StringBuilder();
        sb.append("first line").append("\n");
        sb.append("second line").append("\n");
        sb.append("@param sea arekore koreare (NotNull)").append("\n");
        sb.append("@param land koreare arekore (NotNull)").append("\n");
        sb.append("@return koreare arekore (NotNull)").append("\n");
        sb.append("@throws SeaDocksideException When over is over. (400)").append("\n");
        sb.append("@throws SeaHangarException When mystic is over. (400)").append("\n");
        sb.append("@throws LandShowbaseException When oneman is over. (404)").append("\n");
        String methodComment = sb.toString();

        // ## Act ##
        Map<String, List<Map<String, String>>> statusThrowsMap = extractor.extractStatusThrowsMap(methodComment);

        // ## Assert ##
        log("statusThrowsMap: {}", statusThrowsMap);
        {
            List<Map<String, String>> throwList = statusThrowsMap.get("400");
            assertNotNull(throwList);
            assertEquals(2, throwList.size());
            {
                Map<String, String> throwsMap = throwList.get(0);
                assertNotNull(throwsMap);
                assertEquals("SeaDocksideException", throwsMap.get("exception"));
                assertEquals("When over is over.", throwsMap.get("description"));
            }
            {
                Map<String, String> throwsMap = throwList.get(1);
                assertNotNull(throwsMap);
                assertEquals("SeaHangarException", throwsMap.get("exception"));
                assertEquals("When mystic is over.", throwsMap.get("description"));
            }
        }
        {
            List<Map<String, String>> throwList = statusThrowsMap.get("404");
            assertNotNull(throwList);
            assertEquals(1, throwList.size());

            Map<String, String> throwsMap = throwList.get(0);
            assertNotNull(throwsMap);
            assertEquals("LandShowbaseException", throwsMap.get("exception"));
            assertEquals("When oneman is over.", throwsMap.get("description"));
        }
    }

    public void test_extractStatusThrowsMap_basic_short() {
        // ## Arrange ##
        SwaggerSpecHttpStatusThrowsExtractor extractor = new SwaggerSpecHttpStatusThrowsExtractor();
        StringBuilder sb = new StringBuilder();
        sb.append("@throws LandShowbaseException When oneman is over. (404)");
        String methodComment = sb.toString();

        // ## Act ##
        Map<String, List<Map<String, String>>> statusThrowsMap = extractor.extractStatusThrowsMap(methodComment);

        // ## Assert ##
        log("statusThrowsMap: {}", statusThrowsMap);
        {
            List<Map<String, String>> throwList = statusThrowsMap.get("404");
            assertNotNull(throwList);
            assertEquals(1, throwList.size());

            Map<String, String> throwsMap = throwList.get(0);
            assertNotNull(throwsMap);
            assertEquals("LandShowbaseException", throwsMap.get("exception"));
            assertEquals("When oneman is over.", throwsMap.get("description"));
        }
    }

    // ===================================================================================
    //                                                                               Order
    //                                                                               =====
    public void test_extractStatusThrowsMap_order() {
        // ## Arrange ##
        SwaggerSpecHttpStatusThrowsExtractor extractor = new SwaggerSpecHttpStatusThrowsExtractor();
        StringBuilder sb = new StringBuilder();
        sb.append("first line").append("\n");
        sb.append("@param sea arekore koreare (NotNull)").append("\n");
        sb.append("@return koreare arekore (NotNull)").append("\n");
        sb.append("@throws SeaHangarException When mystic is over. (400)").append("\n");
        sb.append("@param land koreare arekore (NotNull)").append("\n");
        sb.append("@throws LandShowbaseException When oneman is over. (404)").append("\n");
        sb.append("second line").append("\n");
        sb.append("@throws SeaDocksideException When over is over. (400)").append("\n");
        sb.append("last line").append("\n");
        String methodComment = sb.toString();

        // ## Act ##
        Map<String, List<Map<String, String>>> statusThrowsMap = extractor.extractStatusThrowsMap(methodComment);

        // ## Assert ##
        log("statusThrowsMap: {}", statusThrowsMap);
        {
            List<Map<String, String>> throwList = statusThrowsMap.get("400");
            assertNotNull(throwList);
            assertEquals(2, throwList.size());
            {
                Map<String, String> throwsMap = throwList.get(1); // switched
                assertNotNull(throwsMap);
                assertEquals("SeaDocksideException", throwsMap.get("exception"));
                assertEquals("When over is over.", throwsMap.get("description"));
            }
            {
                Map<String, String> throwsMap = throwList.get(0); // switched
                assertNotNull(throwsMap);
                assertEquals("SeaHangarException", throwsMap.get("exception"));
                assertEquals("When mystic is over.", throwsMap.get("description"));
            }
        }
        {
            List<Map<String, String>> throwList = statusThrowsMap.get("404");
            assertNotNull(throwList);
            assertEquals(1, throwList.size());

            Map<String, String> throwsMap = throwList.get(0);
            assertNotNull(throwsMap);
            assertEquals("LandShowbaseException", throwsMap.get("exception"));
            assertEquals("When oneman is over.", throwsMap.get("description"));
        }
    }

    // ===================================================================================
    //                                                                              Spaces
    //                                                                              ======
    public void test_extractStatusThrowsMap_spaces() {
        // ## Arrange ##
        SwaggerSpecHttpStatusThrowsExtractor extractor = new SwaggerSpecHttpStatusThrowsExtractor();
        StringBuilder sb = new StringBuilder();
        sb.append("first line").append("\r\n");
        sb.append("second line").append("\r\n");
        sb.append("@param sea arekore koreare (NotNull)").append("\r\n");
        sb.append("@param land koreare arekore (NotNull)").append("\r\n");
        sb.append("@return koreare arekore (NotNull)").append("\r\n");
        sb.append(" @throws SeaDocksideException When over is over. ( 400)     ").append("\r\n");
        sb.append("@throws SeaHangarException\u3000\u3000When mystic is over.\u3000(400 )").append("\r\n");
        sb.append("\t@throws\tLandShowbaseException When oneman is over.(404)").append("\r\n");
        String methodComment = sb.toString();

        // ## Act ##
        Map<String, List<Map<String, String>>> statusThrowsMap = extractor.extractStatusThrowsMap(methodComment);

        // ## Assert ##
        log("statusThrowsMap: {}", statusThrowsMap);
        {
            List<Map<String, String>> throwList = statusThrowsMap.get("400");
            assertNotNull(throwList);
            assertEquals(2, throwList.size());
            {
                Map<String, String> throwsMap = throwList.get(0);
                assertNotNull(throwsMap);
                assertEquals("SeaDocksideException", throwsMap.get("exception"));
                assertEquals("When over is over.", throwsMap.get("description"));
            }
            {
                Map<String, String> throwsMap = throwList.get(1);
                assertNotNull(throwsMap);
                assertEquals("SeaHangarException", throwsMap.get("exception"));
                assertEquals("When mystic is over.", throwsMap.get("description"));
            }
        }
        {
            List<Map<String, String>> throwList = statusThrowsMap.get("404");
            assertNotNull(throwList);
            assertEquals(1, throwList.size());

            Map<String, String> throwsMap = throwList.get(0);
            assertNotNull(throwsMap);
            assertEquals("LandShowbaseException", throwsMap.get("exception"));
            assertEquals("When oneman is over.", throwsMap.get("description"));
        }
    }

    // ===================================================================================
    //                                                                              Tricky
    //                                                                              ======
    public void test_extractStatusThrowsMap_tricky_braces() {
        // ## Arrange ##
        SwaggerSpecHttpStatusThrowsExtractor extractor = new SwaggerSpecHttpStatusThrowsExtractor();
        StringBuilder sb = new StringBuilder();
        sb.append("first line").append("\n");
        sb.append("second line").append("\n");
        sb.append("@param sea arekore koreare (NotNull)").append("\n");
        sb.append("@param land koreare arekore (NotNull)").append("\n");
        sb.append("@return koreare arekore (NotNull)").append("\n");
        sb.append("@throws SeaDocksideException When over is over(1). (400)     ").append("\n");
        sb.append("@throws SeaHangarException When (mystic is over. (400)").append("\n");
        sb.append("@throws\tLandShowbaseException When oneman) is over. (((404)").append("\n");
        String methodComment = sb.toString();

        // ## Act ##
        Map<String, List<Map<String, String>>> statusThrowsMap = extractor.extractStatusThrowsMap(methodComment);

        // ## Assert ##
        log("statusThrowsMap: {}", statusThrowsMap);
        {
            List<Map<String, String>> throwList = statusThrowsMap.get("400");
            assertNotNull(throwList);
            assertEquals(2, throwList.size());
            {
                Map<String, String> throwsMap = throwList.get(0);
                assertNotNull(throwsMap);
                assertEquals("SeaDocksideException", throwsMap.get("exception"));
                assertEquals("When over is over(1).", throwsMap.get("description"));
            }
            {
                Map<String, String> throwsMap = throwList.get(1);
                assertNotNull(throwsMap);
                assertEquals("SeaHangarException", throwsMap.get("exception"));
                assertEquals("When (mystic is over.", throwsMap.get("description"));
            }
        }
        {
            List<Map<String, String>> throwList = statusThrowsMap.get("404");
            assertNotNull(throwList);
            assertEquals(1, throwList.size());

            Map<String, String> throwsMap = throwList.get(0);
            assertNotNull(throwsMap);
            assertEquals("LandShowbaseException", throwsMap.get("exception"));
            assertEquals("When oneman) is over. ((", throwsMap.get("description"));
        }
    }

    public void test_extractStatusThrowsMap_tricky_broken() {
        // ## Arrange ##
        SwaggerSpecHttpStatusThrowsExtractor extractor = new SwaggerSpecHttpStatusThrowsExtractor();
        StringBuilder sb = new StringBuilder();
        sb.append("first line").append("\n");
        sb.append("second line").append("\n");
        sb.append("@param sea arekore koreare (500)").append("\n");
        sb.append("@param land koreare arekore (7000)").append("\n");
        sb.append("@return koreare arekore (NotNull)").append("\n");
        sb.append("@throwsSeaDocksideException When over is over. (400)     ").append("\n");
        sb.append("@throws SeaHangarException When mystic is over. (400").append("\n");
        sb.append("@throws LandShowbaseException When oneman is over. (A404)").append("\n");
        String methodComment = sb.toString();

        // ## Act ##
        Map<String, List<Map<String, String>>> statusThrowsMap = extractor.extractStatusThrowsMap(methodComment);

        // ## Assert ##
        log("statusThrowsMap: {}", statusThrowsMap);
        assertTrue(statusThrowsMap.isEmpty());
    }

    public void test_extractStatusThrowsMap_tricky_duplicate() {
        // ## Arrange ##
        SwaggerSpecHttpStatusThrowsExtractor extractor = new SwaggerSpecHttpStatusThrowsExtractor();
        StringBuilder sb = new StringBuilder();
        sb.append("first line").append("\n");
        sb.append("second line").append("\n");
        sb.append("@param sea arekore koreare (NotNull)").append("\n");
        sb.append("@param land koreare arekore (NotNull)").append("\n");
        sb.append("@return koreare arekore (NotNull)").append("\n");
        sb.append("@throws SeaDocksideException When over is over. (400)").append("\n");
        sb.append("@throws SeaHangarException When mystic is over. (400)").append("\n");
        sb.append("@throws SeaDocksideException When over is over. (400)").append("\n");
        sb.append("@throws LandShowbaseException When oneman is over. (404)").append("\n");
        sb.append("@throws LandShowbaseException When oneman is over. (404)").append("\n");
        String methodComment = sb.toString();

        // ## Act ##
        Map<String, List<Map<String, String>>> statusThrowsMap = extractor.extractStatusThrowsMap(methodComment);

        // ## Assert ##
        log("statusThrowsMap: {}", statusThrowsMap);
        {
            List<Map<String, String>> throwList = statusThrowsMap.get("400");
            assertNotNull(throwList);
            assertEquals(3, throwList.size());
            {
                Map<String, String> throwsMap = throwList.get(0);
                assertNotNull(throwsMap);
                assertEquals("SeaDocksideException", throwsMap.get("exception"));
                assertEquals("When over is over.", throwsMap.get("description"));
            }
            {
                Map<String, String> throwsMap = throwList.get(1);
                assertNotNull(throwsMap);
                assertEquals("SeaHangarException", throwsMap.get("exception"));
                assertEquals("When mystic is over.", throwsMap.get("description"));
            }
            {
                Map<String, String> throwsMap = throwList.get(2);
                assertNotNull(throwsMap);
                assertEquals("SeaDocksideException", throwsMap.get("exception"));
                assertEquals("When over is over.", throwsMap.get("description"));
            }
        }
        {
            List<Map<String, String>> throwList = statusThrowsMap.get("404");
            assertNotNull(throwList);
            assertEquals(2, throwList.size());

            {
                Map<String, String> throwsMap = throwList.get(0);
                assertNotNull(throwsMap);
                assertEquals("LandShowbaseException", throwsMap.get("exception"));
                assertEquals("When oneman is over.", throwsMap.get("description"));
            }
            {
                Map<String, String> throwsMap = throwList.get(1);
                assertNotNull(throwsMap);
                assertEquals("LandShowbaseException", throwsMap.get("exception"));
                assertEquals("When oneman is over.", throwsMap.get("description"));
            }
        }
    }

    public void test_extractStatusThrowsMap_tricky_location() {
        // ## Arrange ##
        SwaggerSpecHttpStatusThrowsExtractor extractor = new SwaggerSpecHttpStatusThrowsExtractor();
        StringBuilder sb = new StringBuilder();
        sb.append("first line").append("\n");
        sb.append("second line").append("\n");
        sb.append("@param sea arekore koreare (NotNull)").append("\n");
        sb.append("@param land koreare arekore (NotNull)").append("\n");
        sb.append("@return koreare arekore (NotNull)").append("\n");
        sb.append("@throws SeaDocksideException When over.").append("\n");
        sb.append("                                  is over. (400)").append("\n");
        sb.append("@throws SeaHangarException (400) When mystic is over.").append("\n");
        sb.append("@throws (404) LandShowbaseException When oneman is over.").append("\n");
        String methodComment = sb.toString();

        // ## Act ##
        Map<String, List<Map<String, String>>> statusThrowsMap = extractor.extractStatusThrowsMap(methodComment);

        // ## Assert ##
        log("statusThrowsMap: {}", statusThrowsMap);
        assertTrue(statusThrowsMap.isEmpty());
    }

    public void test_extractStatusThrowsMap_tricky_omitted() {
        // ## Arrange ##
        SwaggerSpecHttpStatusThrowsExtractor extractor = new SwaggerSpecHttpStatusThrowsExtractor();
        StringBuilder sb = new StringBuilder();
        sb.append("first line").append("\n");
        sb.append("second line").append("\n");
        sb.append("@param sea arekore koreare (NotNull)").append("\n");
        sb.append("@param land koreare arekore (NotNull)").append("\n");
        sb.append("@return koreare arekore (NotNull)").append("\n");
        sb.append("@throws SeaDocksideException (400)").append("\n");
        sb.append("@throws When mystic is over. (400)").append("\n"); // exception "When"
        sb.append("@throws LandShowbaseException(404)").append("\n"); // cannot pickup
        String methodComment = sb.toString();

        // ## Act ##
        Map<String, List<Map<String, String>>> statusThrowsMap = extractor.extractStatusThrowsMap(methodComment);

        // ## Assert ##
        log("statusThrowsMap: {}", statusThrowsMap);
        {
            List<Map<String, String>> throwList = statusThrowsMap.get("400");
            assertNotNull(throwList);
            assertEquals(2, throwList.size());
            {
                Map<String, String> throwsMap = throwList.get(0);
                assertNotNull(throwsMap);
                assertEquals("SeaDocksideException", throwsMap.get("exception"));
                assertEquals("", throwsMap.get("description"));
            }
            {
                Map<String, String> throwsMap = throwList.get(1);
                assertNotNull(throwsMap);
                assertEquals("When", throwsMap.get("exception"));
                assertEquals("mystic is over.", throwsMap.get("description"));
            }
        }
        {
            List<Map<String, String>> throwList = statusThrowsMap.get("404");
            assertNull(throwList);
        }
    }

    // ===================================================================================
    //                                                                               Nulls
    //                                                                               =====
    public void test_extractStatusThrowsMap_nullEmpty() {
        // ## Arrange ##
        SwaggerSpecHttpStatusThrowsExtractor extractor = new SwaggerSpecHttpStatusThrowsExtractor();

        // ## Act ##
        // ## Assert ##
        assertException(IllegalArgumentException.class, () -> extractor.extractStatusThrowsMap(null));
        assertException(IllegalArgumentException.class, () -> extractor.extractStatusThrowsMap(""));
        assertException(IllegalArgumentException.class, () -> extractor.extractStatusThrowsMap(" "));
        assertException(IllegalArgumentException.class, () -> extractor.extractStatusThrowsMap(" \n \n "));
    }
}
