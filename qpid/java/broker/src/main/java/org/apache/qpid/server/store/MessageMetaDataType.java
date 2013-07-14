/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.qpid.server.store;

import org.apache.qpid.server.protocol.v0_8.MessageMetaData;
import org.apache.qpid.server.protocol.v0_10.MessageMetaData_0_10;
import org.apache.qpid.server.message.MessageMetaData_1_0;

import java.nio.ByteBuffer;

public enum MessageMetaDataType
{
    META_DATA_0_8  {   public Factory<MessageMetaData> getFactory() { return MessageMetaData.FACTORY; } },
    META_DATA_0_10 {   public Factory<MessageMetaData_0_10> getFactory() { return MessageMetaData_0_10.FACTORY; } },
    META_DATA_1_0 {   public Factory<MessageMetaData_1_0> getFactory() { return MessageMetaData_1_0.FACTORY; } };



    public static interface Factory<M extends StorableMessageMetaData>
    {
        M createMetaData(ByteBuffer buf);
    }

    abstract public Factory<? extends StorableMessageMetaData> getFactory();

}
