package com.siruoren.buildpopup;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Pure unit tests for BuildPopupResult (no JenkinsRule needed).
 */
public class BuildPopupResultTest {

    @Test
    public void testDefaultValues() {
        BuildPopupResult result = new BuildPopupResult();
        assertFalse("showPopup should default to false", result.isShowPopup());
        assertFalse("blockBuild should default to false", result.isBlockBuild());
        assertEquals("popupContent should default to empty string", "", result.getPopupContent());
        assertEquals("popupTitle should default to empty string", "", result.getPopupTitle());
        assertFalse("error should default to false", result.isError());
    }

    @Test
    public void testSuccessFactory() {
        BuildPopupResult result = BuildPopupResult.success(true, true, "msg");
        assertTrue("showPopup should be true", result.isShowPopup());
        assertTrue("blockBuild should be true", result.isBlockBuild());
        assertEquals("popupContent should be 'msg'", "msg", result.getPopupContent());
        assertFalse("error should be false for success", result.isError());
    }

    @Test
    public void testErrorFactory() {
        BuildPopupResult result = BuildPopupResult.error("err");
        assertTrue("error should be true", result.isError());
        assertTrue("showPopup should be true for error", result.isShowPopup());
        assertEquals("errorMessage should be 'err'", "err", result.getErrorMessage());
    }

    @Test
    public void testStillValid() {
        BuildPopupResult result = new BuildPopupResult();
        assertTrue("New result should be valid", result.isStillValid());

        // Set timestamp to 4 minutes ago — exceeds the 3-minute validity window
        result.setTimestamp(System.currentTimeMillis() - 4 * 60 * 1000);
        assertFalse("Result with 4-minute-old timestamp should be invalid", result.isStillValid());
    }

    @Test
    public void testSetters() {
        BuildPopupResult result = new BuildPopupResult();

        result.setShowPopup(true);
        assertTrue(result.isShowPopup());

        result.setBlockBuild(true);
        assertTrue(result.isBlockBuild());

        result.setPopupContent("content");
        assertEquals("content", result.getPopupContent());

        result.setPopupTitle("title");
        assertEquals("title", result.getPopupTitle());

        result.setError(true);
        assertTrue(result.isError());

        result.setErrorMessage("errorMsg");
        assertEquals("errorMsg", result.getErrorMessage());

        result.setExecutionTimeMs(1234L);
        assertEquals(1234L, result.getExecutionTimeMs());

        result.setTimestamp(9999L);
        assertEquals(9999L, result.getTimestamp());

        result.setId("test-id");
        assertEquals("test-id", result.getId());
    }

    @Test
    public void testIdNotNull() {
        BuildPopupResult result = new BuildPopupResult();
        assertNotNull("id should not be null", result.getId());
        assertFalse("id should not be empty", result.getId().isEmpty());
    }

    @Test
    public void testToString() {
        BuildPopupResult result = new BuildPopupResult();
        result.setShowPopup(true);
        result.setBlockBuild(false);
        result.setPopupContent("hello");
        String str = result.toString();
        assertTrue("toString should contain 'showPopup'", str.contains("showPopup"));
        assertTrue("toString should contain 'blockBuild'", str.contains("blockBuild"));
        assertTrue("toString should contain 'popupContent'", str.contains("popupContent"));
        assertTrue("toString should contain the id", str.contains(result.getId()));
    }

    @Test
    public void testPopupTitle() {
        BuildPopupResult result = new BuildPopupResult();
        result.setPopupTitle("My Custom Title");
        assertEquals("My Custom Title", result.getPopupTitle());
    }
}
