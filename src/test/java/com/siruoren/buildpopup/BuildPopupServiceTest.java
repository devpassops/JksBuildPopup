package com.siruoren.buildpopup;

import hudson.model.FreeStyleProject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.WithTimeout;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for BuildPopupService
 */
public class BuildPopupServiceTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    @WithTimeout(10)
    public void testExecuteGroovy_GlobalDisabled() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("test-job");
        
        // Disable globally
        BuildPopupGlobalConfiguration globalConfig = BuildPopupGlobalConfiguration.get();
        globalConfig.setGloballyEnabled(false);
        
        BuildPopupResult result = BuildPopupService.getInstance().executeGroovy(project, null, new HashMap<>(), new HashMap<>(), "");
        
        assertFalse(result.isShowPopup());
        assertFalse(result.isBlockBuild());
    }

    @Test
    @WithTimeout(10)
    public void testExecuteGroovy_JobPropertyDisabled() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("test-job");
        
        // Enable globally but disable job property
        BuildPopupGlobalConfiguration globalConfig = BuildPopupGlobalConfiguration.get();
        globalConfig.setGloballyEnabled(true);
        
        BuildPopupJobProperty prop = new BuildPopupJobProperty(false);
        project.addProperty(prop);
        
        BuildPopupResult result = BuildPopupService.getInstance().executeGroovy(project, null, new HashMap<>(), new HashMap<>(), "");
        
        assertFalse(result.isShowPopup());
        assertFalse(result.isBlockBuild());
    }

    @Test
    @WithTimeout(10)
    public void testExecuteGroovy_EmptyScript() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("test-job");
        
        BuildPopupGlobalConfiguration globalConfig = BuildPopupGlobalConfiguration.get();
        globalConfig.setGloballyEnabled(true);
        
        BuildPopupJobProperty prop = new BuildPopupJobProperty(true);
        prop.setGroovyScript("");
        project.addProperty(prop);
        
        BuildPopupResult result = BuildPopupService.getInstance().executeGroovy(project, null, new HashMap<>(), new HashMap<>(), "");
        
        assertFalse(result.isShowPopup());
        assertFalse(result.isBlockBuild());
    }

    @Test
    @WithTimeout(10)
    public void testExecuteGroovy_SimpleScript() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("test-job");
        
        BuildPopupGlobalConfiguration globalConfig = BuildPopupGlobalConfiguration.get();
        globalConfig.setGloballyEnabled(true);
        
        BuildPopupJobProperty prop = new BuildPopupJobProperty(true);
        prop.setGroovyScript("return [blockBuild: false, showPopup: true, popupContent: 'Test message', popupTitle: 'Test']");
        project.addProperty(prop);
        
        BuildPopupResult result = BuildPopupService.getInstance().executeGroovy(project, null, new HashMap<>(), new HashMap<>(), "");
        
        assertTrue(result.isShowPopup());
        assertFalse(result.isBlockBuild());
        assertEquals("Test message", result.getPopupContent());
        assertEquals("Test", result.getPopupTitle());
    }

    @Test
    @WithTimeout(10)
    public void testExecuteGroovy_WithParameters() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("test-job");
        
        BuildPopupGlobalConfiguration globalConfig = BuildPopupGlobalConfiguration.get();
        globalConfig.setGloballyEnabled(true);
        
        BuildPopupJobProperty prop = new BuildPopupJobProperty(true);
        prop.setGroovyScript("return [blockBuild: false, showPopup: true, popupContent: 'Env: ' + DEPLOY_ENV, popupTitle: 'Test']");
        project.addProperty(prop);
        
        Map<String, String> params = new HashMap<>();
        params.put("DEPLOY_ENV", "production");
        
        BuildPopupResult result = BuildPopupService.getInstance().executeGroovy(project, null, new HashMap<>(), params, "");
        
        assertTrue(result.isShowPopup());
        assertEquals("Env: production", result.getPopupContent());
    }

    @Test
    @WithTimeout(10)
    public void testExecuteGroovy_ScriptError() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("test-job");
        
        BuildPopupGlobalConfiguration globalConfig = BuildPopupGlobalConfiguration.get();
        globalConfig.setGloballyEnabled(true);
        
        BuildPopupJobProperty prop = new BuildPopupJobProperty(true);
        prop.setGroovyScript("invalid groovy syntax {{{");
        project.addProperty(prop);
        
        BuildPopupResult result = BuildPopupService.getInstance().executeGroovy(project, null, new HashMap<>(), new HashMap<>(), "");
        
        assertTrue(result.isError());
        assertNotNull(result.getPopupContent());
    }

    @Test
    @WithTimeout(10)
    public void testExecuteGroovy_ScriptTimeout() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("test-job");
        
        BuildPopupGlobalConfiguration globalConfig = BuildPopupGlobalConfiguration.get();
        globalConfig.setGloballyEnabled(true);
        
        BuildPopupJobProperty prop = new BuildPopupJobProperty(true);
        prop.setGroovyScript("Thread.sleep(10000); return [:]");
        prop.setScriptTimeout(1); // 1 second timeout
        project.addProperty(prop);
        
        BuildPopupResult result = BuildPopupService.getInstance().executeGroovy(project, null, new HashMap<>(), new HashMap<>(), "");
        
        assertTrue(result.isError());
        assertTrue(result.getErrorMessage().contains("timed out"));
    }

    // ============ New tests below ============

    @Test
    @WithTimeout(10)
    public void testExecuteGroovy_BlockBuildTrue() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("block-job");

        BuildPopupGlobalConfiguration globalConfig = BuildPopupGlobalConfiguration.get();
        globalConfig.setGloballyEnabled(true);

        BuildPopupJobProperty prop = new BuildPopupJobProperty(true);
        prop.setUseSandbox(false);
        prop.setGroovyScript("return [blockBuild: true, showPopup: true, popupContent: 'blocked']");
        project.addProperty(prop);

        BuildPopupResult result = BuildPopupService.getInstance().executeGroovy(project, null, new HashMap<>(), new HashMap<>(), "");

        assertTrue("showPopup should be true", result.isShowPopup());
        assertTrue("blockBuild should be true", result.isBlockBuild());
        assertEquals("blocked", result.getPopupContent());
        assertFalse("should not be error", result.isError());
    }

    @Test
    @WithTimeout(10)
    public void testExecuteGroovy_BooleanReturn() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("bool-job");

        BuildPopupGlobalConfiguration globalConfig = BuildPopupGlobalConfiguration.get();
        globalConfig.setGloballyEnabled(true);

        BuildPopupJobProperty prop = new BuildPopupJobProperty(true);
        prop.setUseSandbox(false);
        prop.setGroovyScript("return true");
        project.addProperty(prop);

        BuildPopupResult result = BuildPopupService.getInstance().executeGroovy(project, null, new HashMap<>(), new HashMap<>(), "");

        // Boolean true → blockBuild=true, showPopup=true
        assertTrue("blockBuild should be true for Boolean return", result.isBlockBuild());
        assertTrue("showPopup should be true for Boolean return", result.isShowPopup());
        assertFalse("should not be error", result.isError());
    }

    @Test
    @WithTimeout(10)
    public void testExecuteGroovy_NullReturn() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("null-job");

        BuildPopupGlobalConfiguration globalConfig = BuildPopupGlobalConfiguration.get();
        globalConfig.setGloballyEnabled(true);

        BuildPopupJobProperty prop = new BuildPopupJobProperty(true);
        prop.setUseSandbox(false);
        prop.setGroovyScript("return null");
        project.addProperty(prop);

        BuildPopupResult result = BuildPopupService.getInstance().executeGroovy(project, null, new HashMap<>(), new HashMap<>(), "");

        assertFalse("showPopup should be false for null return", result.isShowPopup());
        assertFalse("blockBuild should be false for null return", result.isBlockBuild());
    }

    @Test
    @WithTimeout(10)
    public void testExecuteGroovy_StringReturn() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("string-job");

        BuildPopupGlobalConfiguration globalConfig = BuildPopupGlobalConfiguration.get();
        globalConfig.setGloballyEnabled(true);

        BuildPopupJobProperty prop = new BuildPopupJobProperty(true);
        prop.setUseSandbox(false);
        prop.setGroovyScript("return 'please confirm'");
        project.addProperty(prop);

        BuildPopupResult result = BuildPopupService.getInstance().executeGroovy(project, null, new HashMap<>(), new HashMap<>(), "");

        // String return → blockBuild=true, showPopup=true, popupContent=the string
        assertTrue("blockBuild should be true for String return", result.isBlockBuild());
        assertTrue("showPopup should be true for String return", result.isShowPopup());
        assertEquals("please confirm", result.getPopupContent());
    }

    @Test
    @WithTimeout(10)
    public void testExecuteGroovy_PopupTitle() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("title-job");

        BuildPopupGlobalConfiguration globalConfig = BuildPopupGlobalConfiguration.get();
        globalConfig.setGloballyEnabled(true);

        BuildPopupJobProperty prop = new BuildPopupJobProperty(true);
        prop.setUseSandbox(false);
        prop.setGroovyScript("return [blockBuild: false, showPopup: true, popupContent: 'msg', popupTitle: 'Custom Title']");
        project.addProperty(prop);

        BuildPopupResult result = BuildPopupService.getInstance().executeGroovy(project, null, new HashMap<>(), new HashMap<>(), "");

        assertTrue(result.isShowPopup());
        assertEquals("Custom Title", result.getPopupTitle());
    }

    @Test
    @WithTimeout(10)
    public void testExecuteGroovy_ParameterDirectVariable() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("param-direct-job");

        BuildPopupGlobalConfiguration globalConfig = BuildPopupGlobalConfiguration.get();
        globalConfig.setGloballyEnabled(true);

        BuildPopupJobProperty prop = new BuildPopupJobProperty(true);
        prop.setUseSandbox(false);
        // Use DEPLOY_ENV as a direct variable (not params.get('DEPLOY_ENV'))
        prop.setGroovyScript("return [blockBuild: false, showPopup: true, popupContent: 'Direct: ' + DEPLOY_ENV, popupTitle: 'Params']");
        project.addProperty(prop);

        Map<String, String> params = new HashMap<>();
        params.put("DEPLOY_ENV", "staging");

        BuildPopupResult result = BuildPopupService.getInstance().executeGroovy(project, null, new HashMap<>(), params, "");

        assertTrue(result.isShowPopup());
        assertEquals("Direct: staging", result.getPopupContent());
    }

    @Test
    @WithTimeout(10)
    public void testExecuteGroovy_ConcurrencyLimit() throws Exception {
        FreeStyleProject project = jenkinsRule.createFreeStyleProject("concurrency-job");

        BuildPopupGlobalConfiguration globalConfig = BuildPopupGlobalConfiguration.get();
        globalConfig.setGloballyEnabled(true);
        // Set max concurrent per job to 1
        globalConfig.setMaxConcurrentPerJob(1);

        BuildPopupJobProperty prop = new BuildPopupJobProperty(true);
        prop.setUseSandbox(false);
        prop.setGroovyScript("Thread.sleep(3000); return [blockBuild: false, showPopup: true, popupContent: 'done']");
        project.addProperty(prop);

        // Submit one request in a background thread to occupy the concurrency slot
        final BuildPopupResult[] backgroundResult = new BuildPopupResult[1];
        Thread bgThread = new Thread(() -> {
            try {
                backgroundResult[0] = BuildPopupService.getInstance().executeGroovy(
                    project, null, new HashMap<>(), new HashMap<>(), "");
            } catch (Exception e) {
                // ignore
            }
        });
        bgThread.start();

        // Give the background thread a moment to start executing
        Thread.sleep(500);

        // Now try to execute from the main thread — should exceed concurrency limit
        BuildPopupResult result = BuildPopupService.getInstance().executeGroovy(project, null, new HashMap<>(), new HashMap<>(), "");

        assertTrue("Should be an error due to concurrency limit", result.isError());

        // Wait for background thread to finish
        bgThread.join(10000);

        // Restore default
        globalConfig.setMaxConcurrentPerJob(BuildPopupGlobalConfiguration.DEFAULT_MAX_CONCURRENT_PER_JOB);
    }
}
