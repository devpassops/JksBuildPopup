package com.siruoren.buildpopup;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Pure unit tests for BuildPopupAction (static methods, no JenkinsRule needed).
 */
public class BuildPopupActionTest {

    @Before
    public void setUp() {
        // Clean up any leftover state between tests
        BuildPopupAction.cleanupExpired();
    }

    @Test
    public void testSetAndGetLatestPopup() {
        BuildPopupResult result = BuildPopupResult.success(true, false, "hello");
        BuildPopupAction.setLatestPopup("test-job", result);

        BuildPopupResult retrieved = BuildPopupAction.getLatestPopupResult("test-job");
        assertNotNull("Should retrieve the popup that was set", retrieved);
        assertEquals("hello", retrieved.getPopupContent());
        assertTrue(retrieved.isShowPopup());
        assertFalse(retrieved.isBlockBuild());
    }

    @Test
    public void testGetLatestPopupNull() {
        BuildPopupResult retrieved = BuildPopupAction.getLatestPopupResult("non-existent-job");
        assertNull("Should return null for non-existent job", retrieved);
    }

    @Test
    public void testDismissPopup() {
        BuildPopupResult result = BuildPopupResult.success(true, false, "dismiss me");
        BuildPopupAction.setLatestPopup("dismiss-job", result);

        String popupId = result.getId();
        BuildPopupAction.dismissPopup("dismiss-job", popupId);

        BuildPopupResult retrieved = BuildPopupAction.getLatestPopupResult("dismiss-job");
        assertNull("Dismissed popup should return null", retrieved);
    }

    @Test
    public void testDismissWrongId() {
        BuildPopupResult result = BuildPopupResult.success(true, false, "dismiss me");
        BuildPopupAction.setLatestPopup("wrong-id-job", result);

        // Dismiss with a wrong ID — should NOT dismiss the popup
        BuildPopupAction.dismissPopup("wrong-id-job", "wrong-id");

        BuildPopupResult retrieved = BuildPopupAction.getLatestPopupResult("wrong-id-job");
        assertNotNull("Popup should still be present when dismissed with wrong ID", retrieved);
        assertEquals("dismiss me", retrieved.getPopupContent());
    }

    @Test
    public void testCleanupExpired() {
        BuildPopupResult result = BuildPopupResult.success(true, false, "expired content");
        // Set timestamp to 4 minutes ago — exceeds the 3-minute validity window
        result.setTimestamp(System.currentTimeMillis() - 4 * 60 * 1000);
        BuildPopupAction.setLatestPopup("expired-job", result);

        BuildPopupAction.cleanupExpired();

        BuildPopupResult retrieved = BuildPopupAction.getLatestPopupResult("expired-job");
        assertNull("Expired popup should be cleaned up", retrieved);
    }

    @Test
    public void testOverwritePopup() {
        BuildPopupResult result1 = BuildPopupResult.success(true, false, "first");
        BuildPopupAction.setLatestPopup("overwrite-job", result1);

        BuildPopupResult result2 = BuildPopupResult.success(true, true, "second");
        BuildPopupAction.setLatestPopup("overwrite-job", result2);

        BuildPopupResult retrieved = BuildPopupAction.getLatestPopupResult("overwrite-job");
        assertNotNull("Should retrieve the overwritten popup", retrieved);
        assertEquals("second", retrieved.getPopupContent());
        assertTrue("Overwritten popup should have blockBuild=true", retrieved.isBlockBuild());
    }
}
