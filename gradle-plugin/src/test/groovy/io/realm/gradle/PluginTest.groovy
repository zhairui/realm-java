package io.realm.gradle
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.internal.artifacts.dsl.dependencies.DefaultDependencyHandler
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue
import static org.junit.Assert.fail

class PluginTest {

    private Project project
    private String currentVersion

    @Before
    public void setUp() {
        project = ProjectBuilder.builder().build()
        currentVersion = new File("../version.txt").text.trim()
    }

    @Test
    public void testPlugin() {
        project.buildscript {
            repositories {
                mavenLocal()
                jcenter()
            }
            dependencies {
                classpath 'com.android.tools.build:gradle:1.5.0'
                classpath 'com.jakewharton.sdkmanager:gradle-plugin:0.12.0'
                classpath "io.realm:realm-gradle-plugin:${currentVersion}"
            }
        }

        project.apply plugin: 'com.android.application'
        project.apply plugin: 'realm-android'

        assertTrue(containsUrl(project.repositories, 'https://jitpack.io'))

        assertTrue(containsDependency(project.dependencies, 'io.realm', 'realm-android-library', currentVersion))
        assertTrue(containsDependency(project.dependencies, 'io.realm', 'realm-annotations', currentVersion))
    }

    @Test
    public void testWithoutAndroidPlugin() {
        project.buildscript {
            repositories {
                mavenLocal()
                jcenter()
            }
            dependencies {
                classpath 'com.android.tools.build:gradle:1.5.0'
                classpath 'com.jakewharton.sdkmanager:gradle-plugin:0.12.0'
                classpath "io.realm:realm-gradle-plugin:${currentVersion}"
            }
        }

        try {
            project.apply plugin: 'realm-android'
            fail()
        } catch (PluginApplicationException e) {
            assertEquals(e.getCause().class, GradleException.class)
            assertTrue(e.getCause().getMessage().contains("'com.android.application' or 'com.android.library' plugin required."))
        }
    }

    private static boolean containsUrl(RepositoryHandler repositories, String url) {
        for (repo in repositories) {
            if (repo.properties.get('url').toString() == url) {
                return true
            }
        }
        return false
    }

    private static boolean containsDependency(DependencyHandler dependencies,
                                              String group, String name, String version) {
        def configurationContainerField = DefaultDependencyHandler.class.getDeclaredField("configurationContainer")
        configurationContainerField.setAccessible(true)
        def configurationContainer = configurationContainerField.get(dependencies)
        def compileConfiguration = configurationContainer.findByName("compile")

        def DependencySet dependencySet = compileConfiguration.getDependencies()
        for (Dependency dependency in dependencySet) {
            if (dependency.properties.group == group
                    && dependency.properties.name == name
                    && dependency.properties.version == version) {
                return true
            }
        }
        return false
    }
}