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
package org.apache.camel.dataformat.csv;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.csv.writer.CSVConfig;
import org.junit.Test;

/**
 * @version $Revision: $
 */
public class CsvMarshalPipeDelimiterTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    private MockEndpoint result;

    @Test
    public void testCsvMarshal() throws Exception {
        result.expectedMessageCount(1);

        template.sendBody("direct:start", createBody());

        assertMockEndpointsSatisfied();

        String body = result.getReceivedExchanges().get(0).getIn().getBody(
                String.class);
        String[] lines = body.split("\n");
        assertEquals(2, lines.length);
        assertEquals("123|Camel in Action|1", lines[0]);
        assertEquals("124|ActiveMQ in Action|2", lines[1]);
    }

    private List<Map<String, Object>> createBody() {
        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();

        Map<String, Object> row1 = new LinkedHashMap<String, Object>();
        row1.put("orderId", 123);
        row1.put("item", "Camel in Action");
        row1.put("amount", 1);
        data.add(row1);

        Map<String, Object> row2 = new LinkedHashMap<String, Object>();
        row2.put("orderId", 124);
        row2.put("item", "ActiveMQ in Action");
        row2.put("amount", 2);
        data.add(row2);
        return data;
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                CsvDataFormat csv = new CsvDataFormat();
                CSVConfig config = new CSVConfig();
                config.setDelimiter('|');
                csv.setConfig(config);
                
                // also possible
                // CsvDataFormat csv = new CsvDataFormat();
                // csv.setDelimiter("|");

                from("direct:start").marshal(csv).convertBodyTo(String.class)
                        .to("mock:result");
            }
        };
    }
}