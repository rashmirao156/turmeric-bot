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
package org.apache.camel;

/**
 * Exception used for forcing an Exchange to be rolled back.
 *
 * @version $Revision$
 */
public class RollbackExchangeException extends CamelExchangeException {
    private static final long serialVersionUID = -7837446508365767066L;

    public RollbackExchangeException(Exchange exchange) {
        this("Intended rollback", exchange);
    }

    public RollbackExchangeException(Exchange exchange, Throwable cause) {
        this("Intended rollback", exchange, cause);
    }

    public RollbackExchangeException(String message, Exchange exchange) {
        super(message, exchange);
    }

    public RollbackExchangeException(String message, Exchange exchange, Throwable cause) {
        super(message, exchange, cause);
    }
}
