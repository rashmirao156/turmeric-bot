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
package org.apache.camel.spring.config;

import junit.framework.TestCase;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @version $Revision$
 */
public class AnotherCamelProxyTest extends TestCase {

    public void testAnotherCamelProxy() throws Exception {
        // START SNIPPET: e1
        ApplicationContext ac = new ClassPathXmlApplicationContext("org/apache/camel/spring/config/AnotherCamelProxyTest.xml");

        MyProxySender sender = (MyProxySender) ac.getBean("myProxySender");
        String reply = sender.hello("Camel");

        assertEquals("Bye Camel", reply);
        // END SNIPPET: e1
    }

}