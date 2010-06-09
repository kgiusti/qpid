﻿/*
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

using System;
using System.Collections.Generic;
using Org.Apache.Qpid.Messaging;

namespace Org.Apache.Qpid.Messaging.examples
{
    class MapReceiver
    {
        static void Main(string[] args)
        {
            string url = "amqp:tcp:localhost:5672";
            if (args.Length > 0)
                url = args[0];

            //
            // Create and open an AMQP connection to the broker URL
            //
            Connection connection = new Connection(url);
            connection.Open();

            //
            // Create a session and a receiver fir the direct exchange using the
            // routing key "map_example".
            //
            Session session = connection.CreateSession();
            Receiver receiver = session.CreateReceiver("amq.direct/map_example");

            //
            // Fetch the message from the broker (wait indefinitely by default)
            //
            Message message = receiver.Fetch(new Duration(60000));

            //
            // Extract the structured content from the message.
            //
            Dictionary<string, object> content = new Dictionary<string, object>();
            message.GetContent(content);
            Console.WriteLine("Received: {0}", content);

            //
            // Acknowledge the receipt of all received messages.
            //
            session.Acknowledge();

            //
            // Close the receiver and the connection.
            //
            receiver.Close();
            connection.Close();
        }
    }
}

