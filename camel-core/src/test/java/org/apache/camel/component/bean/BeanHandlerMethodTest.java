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
package org.apache.camel.component.bean;

import org.apache.camel.Body;
import org.apache.camel.ContextTestSupport;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.apache.camel.impl.DefaultExchange;

/**
 * @version $Revision$
 */
public class BeanHandlerMethodTest extends ContextTestSupport {

    public void testNoHandleMethod() throws Exception {
        BeanInfo info = new BeanInfo(context, MyNoDummyBean.class);

        Exchange exchange = new DefaultExchange(context);
        MyNoDummyBean pojo = new MyNoDummyBean();
        MethodInvocation mi = info.createInvocation(pojo, exchange);
        assertNotNull(mi);
        assertEquals("hello", mi.getMethod().getName());
    }

    public void testAmbigiousMethod() throws Exception {
        BeanInfo info = new BeanInfo(context, MyAmbigiousBean.class);

        Exchange exchange = new DefaultExchange(context);
        MyAmbigiousBean pojo = new MyAmbigiousBean();
        try {
            info.createInvocation(pojo, exchange);
            fail("Should have thrown an exception");
        } catch (AmbiguousMethodCallException e) {
            assertEquals(2, e.getMethods().size());
        }
    }

    public void testHandleMethod() throws Exception {
        BeanInfo info = new BeanInfo(context, MyDummyBean.class);

        Exchange exchange = new DefaultExchange(context);
        MyDummyBean pojo = new MyDummyBean();
        MethodInvocation mi = info.createInvocation(pojo, exchange);
        assertNotNull(mi);
        assertEquals("hello", mi.getMethod().getName());
    }

    public void testHandleAndBodyMethod() throws Exception {
        BeanInfo info = new BeanInfo(context, MyOtherDummyBean.class);

        Exchange exchange = new DefaultExchange(context);
        MyOtherDummyBean pojo = new MyOtherDummyBean();
        MethodInvocation mi = info.createInvocation(pojo, exchange);
        assertNotNull(mi);
        assertEquals("hello", mi.getMethod().getName());
    }

    public void testHandleAmbigious() throws Exception {
        BeanInfo info = new BeanInfo(context, MyReallyDummyBean.class);

        Exchange exchange = new DefaultExchange(context);
        MyReallyDummyBean pojo = new MyReallyDummyBean();
        try {
            info.createInvocation(pojo, exchange);
            fail("Should throw exception");
        } catch (AmbiguousMethodCallException e) {
            assertEquals(2, e.getMethods().size());
        }
    }

    public static class MyNoDummyBean {

        public String hello(@Body String hi) {
            return "Hello " + hi;
        }

        public String doCompute(String input) {
            fail("Should not invoke me");
            return null;
        }

    }

    public static class MyAmbigiousBean {

        public String hello(String hi) {
            fail("Should not invoke me");
            return "Hello " + hi;
        }

        public String doCompute(String input) {
            fail("Should not invoke me");
            return null;
        }

    }

    public static class MyDummyBean {

        @Handler
        public String hello(String hi) {
            return "Hello " + hi;
        }

        public String doCompute(String input) {
            fail("Should not invoke me");
            return null;
        }

    }

    public static class MyOtherDummyBean {

        @Handler
        public String hello(String hi) {
            return "Hello " + hi;
        }

        public String bye(@Body String input) {
            fail("Should not invoke me");
            return null;
        }

    }

    public static class MyReallyDummyBean {

        @Handler
        public String hello(String hi) {
            return "Hello " + hi;
        }

        @Handler
        public String bye(@Body String input) {
            fail("Should not invoke me");
            return null;
        }

    }

}