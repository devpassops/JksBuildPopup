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
        
        BuildPopupResult result = BuildPopupService.getInstance().executeGroovy(project, null, new HashMap<>(), new HashMap<>());
        
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
        
        BuildPopupResult result = BuildPopupService.getInstance().executeGroovy(project, null, new HashMap<>(), new HashMap<>());
        
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
        
        BuildPopupResult result = BuildPopupService.getInstance().executeGroovy(project, null, new HashMap<>(), new HashMap<>());
        
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
        
        BuildPopupResult result = BuildPopupService.getInstance().executeGroovy(project, null, new HashMap<>(), new HashMap<>());
        
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
        
        BuildPopupResult result = BuildPopupService.getInstance().executeGroovy(project, null, new HashMap<>(), params);
        
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
        
        BuildPopupResult result = BuildPopupService.getInstance().executeGroovy(project, null, new HashMap<>(), new HashMap<>());
        
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
        
        BuildPopupResult result = BuildPopupService.getInstance().executeGroovy(project, null, new HashMap<>(), new HashMap<>());
        
        assertTrue(result.isError());
        assertTrue(result.getPopupContent().contains("timed out"));
    }
}
