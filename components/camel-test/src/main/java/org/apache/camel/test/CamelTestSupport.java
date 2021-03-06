/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.test;

import java.io.InputStream;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Message;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Service;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.BreakpointSupport;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultDebugger;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.management.JmxSystemPropertyKeys;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.spi.Language;
import org.apache.camel.spring.CamelBeanPostProcessor;

/**
 * A useful base class which creates a {@link org.apache.camel.CamelContext} with some routes
 * along with a {@link org.apache.camel.ProducerTemplate} for use in the test case
 *
 * @version $Revision$
 */
public abstract class CamelTestSupport extends TestSupport {
    
    protected volatile CamelContext context;
    protected volatile ProducerTemplate template;
    protected volatile ConsumerTemplate consumer;
    private boolean useRouteBuilder = true;
    private Service camelContextService;
    private final DebugBreakpoint breakpoint = new DebugBreakpoint();

    public boolean isUseRouteBuilder() {
        return useRouteBuilder;
    }

    public void setUseRouteBuilder(boolean useRouteBuilder) {
        this.useRouteBuilder = useRouteBuilder;
    }

    public Service getCamelContextService() {
        return camelContextService;
    }

    /**
     * Allows a service to be registered a separate lifecycle service to start
     * and stop the context; such as for Spring when the ApplicationContext is
     * started and stopped, rather than directly stopping the CamelContext
     */
    public void setCamelContextService(Service camelContextService) {
        this.camelContextService = camelContextService;
    }

    @Override
    protected void setUp() throws Exception {
        log.info("********************************************************************************");
        log.info("Testing: " + getTestMethodName() + "(" + getClass().getName() + ")");
        log.info("********************************************************************************");

        log.debug("setUp test");
        if (!useJmx()) {
            disableJMX();
        } else {
            enableJMX();
        }

        context = createCamelContext();
        assertValidContext(context);

        // reduce default shutdown timeout to avoid waiting for 300 seconds
        context.getShutdownStrategy().setTimeout(getShutdownTimeout());

        // set debugger
        context.setDebugger(new DefaultDebugger());
        context.getDebugger().addBreakpoint(breakpoint);
        // note: when stopping CamelContext it will automatic remove the breakpoint

        template = context.createProducerTemplate();
        template.start();
        consumer = context.createConsumerTemplate();
        consumer.start();

        postProcessTest();
        
        if (isUseRouteBuilder()) {
            RouteBuilder[] builders = createRouteBuilders();
            for (RouteBuilder builder : builders) {
                log.debug("Using created route builder: " + builder);
                context.addRoutes(builder);
            }
            startCamelContext();
        } else {
            log.debug("Using route builder from the created context: " + context);
        }
    }

    @Override
    protected void tearDown() throws Exception {
        log.info("Testing done: " + this);

        log.debug("tearDown test: " + getName());
        if (consumer != null) {
            consumer.stop();
        }
        if (template != null) {
            template.stop();
        }
        stopCamelContext();
    }
    
    /**
     * Returns the timeout to use when shutting down (unit in seconds).
     * <p/>
     * Will default use 10 seconds.
     *
     * @return the timeout to use
     */
    protected int getShutdownTimeout() {
        return 10;
    }

    /**
     * Whether or not JMX should be used during testing.
     *
     * @return <tt>false</tt> by default.
     */
    protected boolean useJmx() {
        return false;
    }

    /**
     * Lets post process this test instance to process any Camel annotations.
     * Note that using Spring Test or Guice is a more powerful approach.
     */
    protected void postProcessTest() throws Exception {
        CamelBeanPostProcessor processor = new CamelBeanPostProcessor();
        processor.setCamelContext(context);
        processor.postProcessBeforeInitialization(this, "this");
    }

    protected void stopCamelContext() throws Exception {
        if (camelContextService != null) {
            camelContextService.stop();
        } else {
            if (context != null) {
                context.stop();
            }    
        }
    }

    protected void startCamelContext() throws Exception {
        if (camelContextService != null) {
            camelContextService.start();
        } else {
            if (context instanceof DefaultCamelContext) {
                DefaultCamelContext defaultCamelContext = (DefaultCamelContext)context;
                if (!defaultCamelContext.isStarted()) {
                    defaultCamelContext.start();
                }
            } else {
                context.start();
            }
        }
    }

    protected CamelContext createCamelContext() throws Exception {
        return new DefaultCamelContext(createRegistry());
    }

    protected JndiRegistry createRegistry() throws Exception {
        return new JndiRegistry(createJndiContext());
    }

    @SuppressWarnings("unchecked")
    protected Context createJndiContext() throws Exception {
        Properties properties = new Properties();

        // jndi.properties is optional
        InputStream in = getClass().getClassLoader().getResourceAsStream("jndi.properties");
        if (in != null) {
            log.debug("Using jndi.properties from classpath root");
            properties.load(in);
        } else {
            // set the default initial factory
            properties.put("java.naming.factory.initial", "org.apache.camel.util.jndi.CamelInitialContextFactory");
        }
        return new InitialContext(new Hashtable(properties));
    }

    /**
     * Factory method which derived classes can use to create a {@link RouteBuilder}
     * to define the routes for testing
     */
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {
                // no routes added by default
            }
        };
    }

    /**
     * Factory method which derived classes can use to create an array of
     * {@link org.apache.camel.builder.RouteBuilder}s to define the routes for testing
     *
     * @see #createRouteBuilder()
     */
    protected RouteBuilder[] createRouteBuilders() throws Exception {
        return new RouteBuilder[] {createRouteBuilder()};
    }

    /**
     * Resolves a mandatory endpoint for the given URI or an exception is thrown
     *
     * @param uri the Camel <a href="">URI</a> to use to create or resolve an endpoint
     * @return the endpoint
     */
    protected Endpoint resolveMandatoryEndpoint(String uri) {
        return resolveMandatoryEndpoint(context, uri);
    }

    /**
     * Resolves a mandatory endpoint for the given URI and expected type or an exception is thrown
     *
     * @param uri the Camel <a href="">URI</a> to use to create or resolve an endpoint
     * @return the endpoint
     */
    protected <T extends Endpoint> T resolveMandatoryEndpoint(String uri, Class<T> endpointType) {
        return resolveMandatoryEndpoint(context, uri, endpointType);
    }

    /**
     * Resolves the mandatory Mock endpoint using a URI of the form <code>mock:someName</code>
     *
     * @param uri the URI which typically starts with "mock:" and has some name
     * @return the mandatory mock endpoint or an exception is thrown if it could not be resolved
     */
    protected MockEndpoint getMockEndpoint(String uri) {
        return resolveMandatoryEndpoint(uri, MockEndpoint.class);
    }

    /**
     * Sends a message to the given endpoint URI with the body value
     *
     * @param endpointUri the URI of the endpoint to send to
     * @param body        the body for the message
     */
    protected void sendBody(String endpointUri, final Object body) {
        template.send(endpointUri, new Processor() {
            public void process(Exchange exchange) {
                Message in = exchange.getIn();
                in.setBody(body);
                in.setHeader("testCase", getName());
            }
        });
    }

    /**
     * Sends a message to the given endpoint URI with the body value and specified headers
     *
     * @param endpointUri the URI of the endpoint to send to
     * @param body        the body for the message
     * @param headers     any headers to set on the message
     */
    protected void sendBody(String endpointUri, final Object body, final Map<String, Object> headers) {
        template.send(endpointUri, new Processor() {
            public void process(Exchange exchange) {
                Message in = exchange.getIn();
                in.setBody(body);
                in.setHeader("testCase", getName());
                for (Map.Entry<String, Object> entry : headers.entrySet()) {
                    in.setHeader(entry.getKey(), entry.getValue());
                }
            }
        });
    }

    /**
     * Sends messages to the given endpoint for each of the specified bodies
     *
     * @param endpointUri the endpoint URI to send to
     * @param bodies      the bodies to send, one per message
     */
    protected void sendBodies(String endpointUri, Object... bodies) {
        for (Object body : bodies) {
            sendBody(endpointUri, body);
        }
    }

    /**
     * Creates an exchange with the given body
     */
    protected Exchange createExchangeWithBody(Object body) {
        return createExchangeWithBody(context, body);
    }

    /**
     * Asserts that the given language name and expression evaluates to the
     * given value on a specific exchange
     */
    protected void assertExpression(Exchange exchange, String languageName, String expressionText, Object expectedValue) {
        Language language = assertResolveLanguage(languageName);

        Expression expression = language.createExpression(expressionText);
        assertNotNull("No Expression could be created for text: " + expressionText + " language: " + language, expression);

        assertExpression(expression, exchange, expectedValue);
    }

    /**
     * Asserts that the given language name and predicate expression evaluates
     * to the expected value on the message exchange
     */
    protected void assertPredicate(String languageName, String expressionText, Exchange exchange, boolean expected) {
        Language language = assertResolveLanguage(languageName);

        Predicate predicate = language.createPredicate(expressionText);
        assertNotNull("No Predicate could be created for text: " + expressionText + " language: " + language, predicate);

        assertPredicate(predicate, exchange, expected);
    }

    /**
     * Asserts that the language name can be resolved
     */
    protected Language assertResolveLanguage(String languageName) {
        Language language = context.resolveLanguage(languageName);
        assertNotNull("No language found for name: " + languageName, language);
        return language;
    }

    /**
     * Asserts that all the expectations of the Mock endpoints are valid
     */
    protected void assertMockEndpointsSatisfied() throws InterruptedException {
        MockEndpoint.assertIsSatisfied(context);
    }

    /**
     * Asserts that all the expectations of the Mock endpoints are valid
     */
    protected void assertMockEndpointsSatisfied(long timeout, TimeUnit unit) throws InterruptedException {
        MockEndpoint.assertIsSatisfied(context, timeout, unit);
    }

    /**
     * Reset all Mock endpoints.
     */
    protected void resetMocks() {
        MockEndpoint.resetMocks(context);
    }

    protected void assertValidContext(CamelContext context) {
        assertNotNull("No context found!", context);
    }

    protected <T extends Endpoint> T getMandatoryEndpoint(String uri, Class<T> type) {
        T endpoint = context.getEndpoint(uri, type);
        assertNotNull("No endpoint found for uri: " + uri, endpoint);
        return endpoint;
    }

    protected Endpoint getMandatoryEndpoint(String uri) {
        Endpoint endpoint = context.getEndpoint(uri);
        assertNotNull("No endpoint found for uri: " + uri, endpoint);
        return endpoint;
    }

    /**
     * Disables the JMX agent. Must be called before the {@link #setUp()} method.
     */
    protected void disableJMX() {
        System.setProperty(JmxSystemPropertyKeys.DISABLED, "true");
    }

    /**
     * Enables the JMX agent. Must be called before the {@link #setUp()} method.
     */
    protected void enableJMX() {
        System.setProperty(JmxSystemPropertyKeys.DISABLED, "false");
    }

    /**
     * Single step debugs and Camel invokes this method before entering the given processor
     *
     * @param exchange     the exchange
     * @param processor    the processor about to be invoked
     * @param definition   the definition for the processor
     * @param id           the id of the definition
     * @param shortName    the short name of the definition
     */
    protected void debugBefore(Exchange exchange, Processor processor, ProcessorDefinition definition,
                               String id, String shortName) {
    }

    /**
     * Single step debugs and Camel invokes this method after processing the given processor
     *
     * @param exchange     the exchange
     * @param processor    the processor that was invoked
     * @param definition   the definition for the processor
     * @param id           the id of the definition
     * @param shortName    the short name of the definition
     * @param timeTaken    time taken to process the processor in millis
     */
    protected void debugAfter(Exchange exchange, Processor processor, ProcessorDefinition definition,
                              String id, String shortName, long timeTaken) {
    }

    /**
     * To easily debug by overriding the <tt>debugBefore</tt> and <tt>debugAfter</tt> methods.
     */
    private class DebugBreakpoint extends BreakpointSupport {

        @Override
        public void beforeProcess(Exchange exchange, Processor processor, ProcessorDefinition definition) {
            CamelTestSupport.this.debugBefore(exchange, processor, definition, definition.getId(), definition.getShortName());
        }

        @Override
        public void afterProcess(Exchange exchange, Processor processor, ProcessorDefinition definition, long timeTaken) {
            CamelTestSupport.this.debugAfter(exchange, processor, definition, definition.getId(), definition.getShortName(), timeTaken);
        }
    }
}