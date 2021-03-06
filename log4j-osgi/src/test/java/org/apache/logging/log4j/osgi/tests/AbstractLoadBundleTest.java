/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.osgi.tests;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * Tests a basic Log4J 'setup' in an OSGi container.
 */
public abstract class AbstractLoadBundleTest extends AbstractOsgiTest {

    private Bundle getApiBundle() throws BundleException {
        final Path apiPath = getHere().resolveSibling("log4j-api").resolve("target").resolve(getBundleTestInfo().buildJarFileName("log4j-api"));
        return getBundleContext().installBundle(apiPath.toUri().toString());
    }

    private Bundle getPluginsBundle() throws BundleException {
        final Path apiPath = getHere().resolveSibling("log4j-plugins").resolve("target").resolve(getBundleTestInfo().buildJarFileName("log4j-plugins"));
        return getBundleContext().installBundle(apiPath.toUri().toString());
    }


    private Bundle getCoreBundle() throws BundleException {
        final Path corePath = getHere().resolveSibling("log4j-core").resolve("target").resolve(getBundleTestInfo().buildJarFileName("log4j-core"));
        return getBundleContext().installBundle(corePath.toUri().toString());
    }

    private Bundle getDummyBundle() throws BundleException {
        final Path dumyPath = getHere().resolveSibling("log4j-samples").resolve("log4j-samples-configuration").resolve("target").resolve(getBundleTestInfo().buildJarFileName("log4j-samples-configuration"));
        return getBundleContext().installBundle(dumyPath.toUri().toString());
    }

    private Bundle get12ApiBundle() throws BundleException {
        final Path apiPath = getHere().resolveSibling("log4j-1.2-api").resolve("target").resolve(getBundleTestInfo().buildJarFileName("log4j-1.2-api"));
        return getBundleContext().installBundle(apiPath.toUri().toString());
    }


    private void log(final Bundle dummy) throws ReflectiveOperationException {
        // use reflection to log in the context of the dummy bundle

        final Class<?> logManagerClass = dummy.loadClass("org.apache.logging.log4j.LogManager");
        final Method getLoggerMethod = logManagerClass.getMethod("getLogger", Class.class);

        final Class<?> loggerClass = dummy.loadClass("org.apache.logging.log4j.configuration.CustomConfiguration");

        final Object logger = getLoggerMethod.invoke(null, loggerClass);
        final Method errorMethod = logger.getClass().getMethod("error", Object.class);

        errorMethod.invoke(logger, "Test OK");
    }

    private PrintStream setupStream(final Bundle api, final PrintStream newStream) throws ReflectiveOperationException {
        // use reflection to access the classes internals and in the context of the api bundle

        final Class<?> statusLoggerClass = api.loadClass("org.apache.logging.log4j.status.StatusLogger");

        final Field statusLoggerField = statusLoggerClass.getDeclaredField("STATUS_LOGGER");
        statusLoggerField.setAccessible(true);
        final Object statusLoggerFieldValue = statusLoggerField.get(null);

        final Field loggerField = statusLoggerClass.getDeclaredField("logger");
        loggerField.setAccessible(true);
        final Object loggerFieldValue = loggerField.get(statusLoggerFieldValue);

        final Class<?> simpleLoggerClass = api.loadClass("org.apache.logging.log4j.simple.SimpleLogger");

        final Field streamField = simpleLoggerClass.getDeclaredField("stream");
        streamField.setAccessible(true);

        final PrintStream oldStream = (PrintStream) streamField.get(loggerFieldValue);

        streamField.set(loggerFieldValue, newStream);

        return oldStream;
    }

    private void start(final Bundle api, final Bundle plugins, final Bundle core, final Bundle dummy) throws BundleException {
        api.start();
        plugins.start();
        core.start();
        dummy.start();
    }

    private void stop(final Bundle api, final Bundle plugins, final Bundle core, final Bundle dummy) throws BundleException {
        dummy.stop();
        core.stop();
        plugins.stop();
        api.stop();
    }

    private void uninstall(final Bundle api, final Bundle plugins, final Bundle core, final Bundle dummy) throws BundleException {
        dummy.uninstall();
        core.uninstall();
        plugins.uninstall();
        api.uninstall();
    }

    /**
     * Tests starting, then stopping, then restarting, then stopping, and finally uninstalling the API and Core bundles
     */
    @Test
    public void testApiCoreStartStopStartStop() throws BundleException {

        final Bundle api = getApiBundle();
        final Bundle plugins = getPluginsBundle();
        final Bundle core = getCoreBundle();

        Assert.assertEquals("api is not in INSTALLED state", Bundle.INSTALLED, api.getState());
        Assert.assertEquals("plugins is not in INSTALLED state", Bundle.INSTALLED, plugins.getState());
        Assert.assertEquals("core is not in INSTALLED state", Bundle.INSTALLED, core.getState());

        api.start();
        plugins.start();
        core.start();

        Assert.assertEquals("api is not in ACTIVE state", Bundle.ACTIVE, api.getState());
        Assert.assertEquals("plugins is not in ACTIVE state", Bundle.ACTIVE, plugins.getState());
        Assert.assertEquals("core is not in ACTIVE state", Bundle.ACTIVE, core.getState());

        core.stop();
        plugins.stop();
        api.stop();

        Assert.assertEquals("api is not in RESOLVED state", Bundle.RESOLVED, api.getState());
        Assert.assertEquals("plugins is not in RESOLVED state", Bundle.RESOLVED, plugins.getState());
        Assert.assertEquals("core is not in RESOLVED state", Bundle.RESOLVED, core.getState());

        api.start();
        plugins.start();
        core.start();

        Assert.assertEquals("api is not in ACTIVE state", Bundle.ACTIVE, api.getState());
        Assert.assertEquals("plugins is not in ACTIVE state", Bundle.ACTIVE, plugins.getState());
        Assert.assertEquals("core is not in ACTIVE state", Bundle.ACTIVE, core.getState());

        core.stop();
        plugins.stop();
        api.stop();

        Assert.assertEquals("api is not in RESOLVED state", Bundle.RESOLVED, api.getState());
        Assert.assertEquals("plugins is not in RESOLVED state", Bundle.RESOLVED, plugins.getState());
        Assert.assertEquals("core is not in RESOLVED state", Bundle.RESOLVED, core.getState());

        core.uninstall();
        plugins.uninstall();
        api.uninstall();

        Assert.assertEquals("api is not in UNINSTALLED state", Bundle.UNINSTALLED, api.getState());
        Assert.assertEquals("plugins is not in UNINSTALLED state", Bundle.UNINSTALLED, plugins.getState());
        Assert.assertEquals("core is not in UNINSTALLED state", Bundle.UNINSTALLED, core.getState());
    }

    /**
     * Tests LOG4J2-1637.
     */
    @Test
    public void testClassNotFoundErrorLogger() throws BundleException {

        final Bundle api = getApiBundle();
        final Bundle plugins = getPluginsBundle();
        final Bundle core = getCoreBundle();

        api.start();
        plugins.start();
        // fails if LOG4J2-1637 is not fixed
        try {
            core.start();
        }
        catch (final BundleException ex) {
            boolean shouldRethrow = true;
            final Throwable t = ex.getCause();
            if (t != null) {
                final Throwable t2 = t.getCause();
                if (t2 != null) {
                    final String cause = t2.toString();
                    final boolean result = cause.equals("java.lang.ClassNotFoundException: org.apache.logging.log4j.Logger") // Equinox
                                  || cause.equals("java.lang.ClassNotFoundException: org.apache.logging.log4j.Logger not found by org.apache.logging.log4j.core [2]"); // Felix
                    Assert.assertFalse("org.apache.logging.log4j package is not properly imported in org.apache.logging.log4j.core bundle, check that the package is exported from api and is not split between api and core", result);
                    shouldRethrow = !result;
                }
            }
            if (shouldRethrow) {
                throw ex; // rethrow if the cause of the exception is something else
            }
        }

        core.stop();
        plugins.stop();
        api.stop();

        core.uninstall();
        plugins.uninstall();
        api.uninstall();
    }

    /**
     * Tests LOG4J2-920.
     */
    @Test
    public void testLoadingOfConfigurableCoreClasses() throws BundleException, ReflectiveOperationException {

        final Bundle api = getApiBundle();
        final Bundle plugins = getPluginsBundle();
        final Bundle core = getCoreBundle();
        final Bundle dummy = getDummyBundle();

        start(api, plugins, core, dummy);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream logStream = new PrintStream(baos);

        final PrintStream bakStream = setupStream(api, logStream);

        log(dummy);

        setupStream(api, bakStream);

        // org.apache.logging.log4j.core.osgi.BundleContextSelector cannot be found by org.apache.logging.log4j.api
        final boolean result = baos.toString().contains("BundleContextSelector cannot be found");
        Assert.assertFalse("Core class BundleContextSelector cannot be loaded in OSGI setup", result);

        stop(api, plugins, core, dummy);
        uninstall(api, plugins, core, dummy);
    }

    /**
     * Tests the log of a simple message in an OSGi container
     */
    @Test
    public void testSimpleLogInAnOsgiContext() throws BundleException, ReflectiveOperationException {

        final Bundle api = getApiBundle();
        final Bundle plugins = getPluginsBundle();
        final Bundle core = getCoreBundle();
        final Bundle dummy = getDummyBundle();

        start(api, plugins, core, dummy);

        final PrintStream bakStream = System.out;
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final PrintStream logStream = new PrintStream(baos);
            System.setOut(logStream);

            log(dummy);

            final String result = baos.toString().substring(
                12).trim(); // remove the instant then the spaces at start and end, that are non constant
            String expected = "[main] ERROR org.apache.logging.log4j.configuration.CustomConfiguration - Test OK";
            Assert.assertTrue("Incorrect string. Expected string ends with: " + expected + " Actual: " + result,
                    result.endsWith(expected));
        } finally {
            System.setOut(bakStream);
        }

        stop(api, plugins, core, dummy);
        uninstall(api, plugins, core, dummy);
    }


    /**
     * Tests the loading of the 1.2 Compatibility API bundle, its classes should be loadable from the Core bundle,
     * and the class loader should be the same between a class from core and a class from compat
     */
    @Test
    public void testLog4J12Fragement() throws BundleException, ReflectiveOperationException {

        final Bundle api = getApiBundle();
        final Bundle plugins = getPluginsBundle();
        final Bundle core = getCoreBundle();
        final Bundle compat = get12ApiBundle();

        api.start();
        plugins.start();
        core.start();

        final Class<?> coreClassFromCore = core.loadClass("org.apache.logging.log4j.core.Core");
        final Class<?> levelClassFrom12API = core.loadClass("org.apache.log4j.Level");
        final Class<?> levelClassFromAPI = core.loadClass("org.apache.logging.log4j.Level");

        Assert.assertEquals("expected 1.2 API Level to have the same class loader as Core", levelClassFrom12API.getClassLoader(), coreClassFromCore.getClassLoader());
        Assert.assertNotEquals("expected 1.2 API Level NOT to have the same class loader as API Level", levelClassFrom12API.getClassLoader(), levelClassFromAPI.getClassLoader());

        core.stop();
        api.stop();

        uninstall(api, plugins, core, compat);
    }

}
