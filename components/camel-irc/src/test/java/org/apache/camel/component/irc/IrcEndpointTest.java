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
package org.apache.camel.component.irc;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.schwering.irc.lib.IRCConnection;
import org.schwering.irc.lib.IRCEventAdapter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IrcEndpointTest {

    private IrcComponent component;
    private IrcConfiguration configuration;
    private IRCConnection connection;
    private IrcEndpoint endpoint;

    @Before
    public void doSetup() {
        component = mock(IrcComponent.class);
        configuration = mock(IrcConfiguration.class);
        connection = mock(IRCConnection.class);

        List<String> channels = new ArrayList<String>();
        Dictionary<String, String> keys = new Hashtable<String, String>();

        channels.add("chan1");
        channels.add("chan2");

        keys.put("chan1", "");
        keys.put("chan2", "chan2key");

        when(configuration.getChannels()).thenReturn(channels);
        when(configuration.getKey("chan1")).thenReturn("");
        when(configuration.getKey("chan2")).thenReturn("chan2key");


        when(component.getIRCConnection(configuration)).thenReturn(connection);


        endpoint = new IrcEndpoint("foo", component, configuration);
    }

    @Test
    public void doJoinChannelTestNoKey() throws Exception {
        endpoint.joinChannel("chan1");
        verify(connection).doJoin("chan1");
    }

    @Test
    public void doJoinChannelTestKey() throws Exception {
        endpoint.joinChannel("chan2");
        verify(connection).doJoin("chan2", "chan2key");
    }

    @Test
    public void doJoinChannels() throws Exception {
        endpoint.joinChannels();
        verify(connection).doJoin("chan1");
        verify(connection).doJoin("chan2", "chan2key");
    }


    @Test
    public void doHandleIrcErrorNickInUse() throws Exception {
        when(connection.getNick()).thenReturn("nick");

        endpoint.handleIrcError(IRCEventAdapter.ERR_NICKNAMEINUSE, "foo");

        verify(connection).doNick("nick-");

        when(connection.getNick()).thenReturn("nick---");

        // confirm doNick was not called
        verify(connection, never()).doNick("foo");
    }
}
