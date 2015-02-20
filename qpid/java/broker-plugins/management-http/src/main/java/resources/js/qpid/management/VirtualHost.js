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
define(["dojo/_base/xhr",
        "dojo/parser",
        "dojo/query",
        "dojo/_base/connect",
        "dijit/registry",
        "dojox/html/entities",
        "qpid/common/properties",
        "qpid/common/updater",
        "qpid/common/util",
        "qpid/common/formatter",
        "qpid/common/UpdatableStore",
        "qpid/management/addQueue",
        "qpid/management/addExchange",
        "dojox/grid/EnhancedGrid",
        "qpid/management/editVirtualHost",
        "dojo/domReady!"],
       function (xhr, parser, query, connect, registry, entities, properties, updater, util, formatter, UpdatableStore, addQueue, addExchange, EnhancedGrid, editVirtualHost) {

           function VirtualHost(name, parent, controller) {
               this.name = name;
               this.controller = controller;
               this.modelObj = { type: "virtualhost", name: name, parent: parent};
           }

           VirtualHost.prototype.getTitle = function()
           {
               return "VirtualHost: " + this.name;
           };

           VirtualHost.prototype.open = function(contentPane) {
               var that = this;
               this.contentPane = contentPane;
               xhr.get({url: "showVirtualHost.html",
                        sync: true,
                        load:  function(data) {
                            var containerNode = contentPane.containerNode;
                            containerNode.innerHTML = data;
                            parser.parse(containerNode).then(function(instances)
                            {
                            that.vhostUpdater = new Updater(containerNode, that.modelObj, that.controller, that);

                            var addQueueButton = query(".addQueueButton", containerNode)[0];
                            connect.connect(registry.byNode(addQueueButton), "onClick", function(evt){
                                addQueue.show({virtualhost:that.name,virtualhostnode:that.modelObj.parent.name})
                            });

                            var deleteQueueButton = query(".deleteQueueButton", containerNode)[0];
                            connect.connect(registry.byNode(deleteQueueButton), "onClick",
                                    function(evt){
                                        util.deleteGridSelections(
                                                that.vhostUpdater,
                                                that.vhostUpdater.queuesGrid.grid,
                                                "api/latest/queue/" + encodeURIComponent(that.modelObj.parent.name) + "/" + encodeURIComponent(that.name),
                                                "Are you sure you want to delete queue");
                                }
                            );

                            var addExchangeButton = query(".addExchangeButton", containerNode)[0];
                            connect.connect(registry.byNode(addExchangeButton), "onClick", function(evt){ addExchange.show({virtualhost:that.name,virtualhostnode:that.modelObj.parent.name}) });

                            var deleteExchangeButton = query(".deleteExchangeButton", containerNode)[0];
                            connect.connect(registry.byNode(deleteExchangeButton), "onClick",
                                    function(evt)
                                    {
                                        util.deleteGridSelections(
                                                that.vhostUpdater,
                                                that.vhostUpdater.exchangesGrid.grid,
                                                "api/latest/exchange/"+ encodeURIComponent(that.modelObj.parent.name) + "/" + encodeURIComponent(that.name),
                                                "Are you sure you want to delete exchange");
                                    }
                            );

                            that.stopButton = registry.byNode(query(".stopButton", containerNode)[0]);
                            that.startButton = registry.byNode(query(".startButton", containerNode)[0]);
                            that.editButton = registry.byNode(query(".editButton", containerNode)[0]);
                            that.downloadButton = registry.byNode(query(".downloadButton", containerNode)[0]);
                            that.downloadButton.on("click",
                              function(e)
                              {
                                var iframe = document.createElement('iframe');
                                iframe.id = "downloader_" + that.name;
                                document.body.appendChild(iframe);
                                var suggestedAttachmentName = encodeURIComponent(that.name + ".json");
                                iframe.src = "/api/latest/virtualhost/" + encodeURIComponent(that.modelObj.parent.name) + "/" + encodeURIComponent(that.name) + "?extractInitialConfig=true&contentDispositionAttachmentFilename=" + suggestedAttachmentName;
                                // It seems there is no way to remove this iframe in a manner that is cross browser compatible.
                              }
                            );

                            that.deleteButton = registry.byNode(query(".deleteButton", containerNode)[0]);
                            that.deleteButton.on("click",
                                 function(e)
                                 {
                                   if (confirm("Deletion of virtual host will delete message data.\n\n"
                                           + "Are you sure you want to delete virtual host  '" + entities.encode(String(that.name)) + "'?"))
                                   {
                                     if (util.sendRequest("api/latest/virtualhost/" + encodeURIComponent(that.modelObj.parent.name) + "/" + encodeURIComponent( that.name) , "DELETE"))
                                     {
                                       that.destroy();
                                     }
                                   }
                                 }
                            );
                            that.startButton.on("click",
                               function(event)
                               {
                                 that.startButton.set("disabled", true);
                                 util.sendRequest("api/latest/virtualhost/" + encodeURIComponent(that.modelObj.parent.name) + "/" + encodeURIComponent( that.name),
                                         "PUT", {desiredState: "ACTIVE"});
                               });

                            that.stopButton.on("click",
                               function(event)
                               {
                                 if (confirm("Stopping the virtual host will also stop its children. "
                                         + "Are you sure you want to stop virtual host '"
                                         + entities.encode(String(that.name)) +"'?"))
                                 {
                                     that.stopButton.set("disabled", true);
                                     util.sendRequest("api/latest/virtualhost/" + encodeURIComponent(that.modelObj.parent.name) + "/" + encodeURIComponent( that.name),
                                             "PUT", {desiredState: "STOPPED"});
                                 }
                               });

                            that.editButton.on("click",
                                function(event)
                                {
                                    editVirtualHost.show({nodeName:that.modelObj.parent.name,hostName:that.name});
                                });

                            that.vhostUpdater.update();
                            updater.add( that.vhostUpdater );
                            });

                        }});

           };

           VirtualHost.prototype.close = function() {
               updater.remove( this.vhostUpdater );
           };

           VirtualHost.prototype.destroy = function()
           {
             this.close();
             this.contentPane.onClose()
             this.controller.tabContainer.removeChild(this.contentPane);
             this.contentPane.destroyRecursive();
           }

           function Updater(node, vhost, controller, virtualHost)
           {
               this.virtualHost = virtualHost;
               var that = this;

               function findNode(name) {
                   return query("." + name, node)[0];
               }

               function storeNodes(names)
               {
                   for(var i = 0; i < names.length; i++) {
                       that[names[i]] = findNode(names[i]);
                   }
               }

               storeNodes(["name",
                           "type",
                           "state",
                           "durable",
                           "lifetimePolicy",
                           "msgInRate",
                           "bytesInRate",
                           "bytesInRateUnits",
                           "msgOutRate",
                           "bytesOutRate",
                           "bytesOutRateUnits",
                           "virtualHostDetailsContainer",
                           "deadLetterQueueEnabled",
                           "housekeepingCheckPeriod",
                           "housekeepingThreadCount",
                           "storeTransactionIdleTimeoutClose",
                           "storeTransactionIdleTimeoutWarn",
                           "storeTransactionOpenTimeoutClose",
                           "storeTransactionOpenTimeoutWarn",
                           "virtualHostConnections",
                           "virtualHostChildren"
                           ]);

               this.query = "api/latest/virtualhost/"+ encodeURIComponent(vhost.parent.name) + "/" + encodeURIComponent(vhost.name);

               var that = this;

               xhr.get({url: this.query, sync: properties.useSyncGet, handleAs: "json"}).then(function(data) {
                   that.vhostData = data[0];

                       // flatten statistics into attributes
                       util.flattenStatistics( that.vhostData );

                       var gridProperties = {
                               keepSelection: true,
                               plugins: {
                                         pagination: {
                                             pageSizes: [10, 25, 50, 100],
                                             description: true,
                                             sizeSwitch: true,
                                             pageStepper: true,
                                             gotoButton: true,
                                             maxPageStep: 4,
                                             position: "bottom"
                                         },
                                         indirectSelection: true

                                }};

                   that.updateHeader();
                   that.queuesGrid = new UpdatableStore(that.vhostData.queues, findNode("queues"),
                                                        [ { name: "Name",    field: "name",      width: "30%"},
                                                          { name: "Type",    field: "type",      width: "20%"},
                                                          { name: "Consumers", field: "consumerCount", width: "10%"},
                                                          { name: "Depth (msgs)", field: "queueDepthMessages", width: "20%"},
                                                          { name: "Depth (bytes)", field: "queueDepthBytes", width: "20%",
                                                            get: function(rowIndex, item)
                                                             {
                                                               if(!item){
                                                                 return;
                                                               }
                                                               var store = this.grid.store;
                                                               var qdb = store.getValue(item, "queueDepthBytes");
                                                               var bytesFormat = formatter.formatBytes( qdb );
                                                               return bytesFormat.value + " " + bytesFormat.units;
                                                             }
                                                          }
                                                        ],
                                                        function(obj)
                                                        {
                                                            connect.connect(obj.grid, "onRowDblClick", obj.grid,
                                                                         function(evt){
                                                                             var idx = evt.rowIndex,
                                                                                 theItem = this.getItem(idx);
                                                                             var queueName = obj.dataStore.getValue(theItem,"name");
                                                                             controller.show("queue", queueName, vhost, theItem.id);
                                                                         });
                                                        } , gridProperties, EnhancedGrid);

                   that.exchangesGrid = new UpdatableStore(that.vhostData.exchanges, findNode("exchanges"),
                                                           [
                                                             { name: "Name",    field: "name", width: "50%"},
                                                             { name: "Type", field: "type", width: "30%"},
                                                             { name: "Binding Count", field: "bindingCount", width: "20%"}
                                                           ],
                                                           function(obj)
                                                           {
                                                               connect.connect(obj.grid, "onRowDblClick", obj.grid,
                                                                            function(evt){
                                                                                var idx = evt.rowIndex,
                                                                                theItem = this.getItem(idx);
                                                                                var exchangeName = obj.dataStore.getValue(theItem,"name");
                                                                                controller.show("exchange", exchangeName, vhost, theItem.id);
                                                                            });
                                                           } , gridProperties, EnhancedGrid);


                   that.connectionsGrid = new UpdatableStore(that.vhostData.connections,
                                                             findNode("connections"),
                                                             [ { name: "Name",    field: "name",      width: "20%"},
                                                                 { name: "User", field: "principal", width: "10%"},
                                                                 { name: "Port",    field: "port",      width: "10%"},
                                                                 { name: "Transport",    field: "transport",      width: "10%"},
                                                                 { name: "Sessions", field: "sessionCount", width: "10%"},
                                                                 { name: "Msgs In", field: "msgInRate",
                                                                     width: "10%"},
                                                                 { name: "Bytes In", field: "bytesInRate",
                                                                     width: "10%"},
                                                                 { name: "Msgs Out", field: "msgOutRate",
                                                                     width: "10%"},
                                                                 { name: "Bytes Out", field: "bytesOutRate",
                                                                     width: "10%"}
                                                             ],
                                                             function(obj)
                                                             {
                                                                 connect.connect(obj.grid, "onRowDblClick", obj.grid,
                                                                              function(evt){
                                                                                  var idx = evt.rowIndex,
                                                                                      theItem = this.getItem(idx);
                                                                                  var connectionName = obj.dataStore.getValue(theItem,"name");
                                                                                  controller.show("connection", connectionName, vhost, theItem.id);
                                                                              });
                                                             } );



               });

           }

           Updater.prototype.updateHeader = function()
           {
               this.name.innerHTML = entities.encode(String(this.vhostData[ "name" ]));
               this.type.innerHTML = entities.encode(String(this.vhostData[ "type" ]));
               this.state.innerHTML = entities.encode(String(this.vhostData[ "state" ]));
               this.durable.innerHTML = entities.encode(String(this.vhostData[ "durable" ]));
               this.lifetimePolicy.innerHTML = entities.encode(String(this.vhostData[ "lifetimePolicy" ]));
               this.deadLetterQueueEnabled.innerHTML = entities.encode(String(this.vhostData[ "queue.deadLetterQueueEnabled" ]));
               util.updateUI(this.vhostData,
                            ["housekeepingCheckPeriod",
                             "housekeepingThreadCount",
                             "storeTransactionIdleTimeoutClose",
                             "storeTransactionIdleTimeoutWarn",
                             "storeTransactionOpenTimeoutClose",
                             "storeTransactionOpenTimeoutWarn"],
                            this)
           };

           Updater.prototype.update = function()
           {
               var thisObj = this;

               xhr.get({url: this.query, sync: properties.useSyncGet, handleAs: "json"})
                   .then(function(data) {
                       thisObj.vhostData = data[0] || {name: thisObj.virtualHost.name,statistics:{messagesIn:0,bytesIn:0,messagesOut:0,bytesOut:0}};
                       try
                       {
                            thisObj._update();
                       }
                       catch(e)
                       {
                            if (console && console.error)
                            {
                                console.error(e);
                            }
                       }
                   });
           }

           Updater.prototype._update = function()
           {
                var thisObj = this;
                this.virtualHost.startButton.set("disabled", !this.vhostData.state || this.vhostData.state != "STOPPED");
                this.virtualHost.stopButton.set("disabled", !this.vhostData.state || this.vhostData.state != "ACTIVE");
                this.virtualHost.editButton.set("disabled", !this.vhostData.state || this.vhostData.state == "UNAVAILABLE");
                this.virtualHost.downloadButton.set("disabled", !this.vhostData.state || this.vhostData.state != "ACTIVE");
                this.virtualHost.deleteButton.set("disabled", !this.vhostData.state);

                       util.flattenStatistics( thisObj.vhostData );
                       var connections = thisObj.vhostData[ "connections" ];
                       var queues = thisObj.vhostData[ "queues" ];
                       var exchanges = thisObj.vhostData[ "exchanges" ];

                       thisObj.updateHeader();

                       var stats = thisObj.vhostData[ "statistics" ];

                       var sampleTime = new Date();
                       var messageIn = stats["messagesIn"];
                       var bytesIn = stats["bytesIn"];
                       var messageOut = stats["messagesOut"];
                       var bytesOut = stats["bytesOut"];

                       if(thisObj.sampleTime)
                       {
                           var samplePeriod = sampleTime.getTime() - thisObj.sampleTime.getTime();

                           var msgInRate = (1000 * (messageIn - thisObj.messageIn)) / samplePeriod;
                           var msgOutRate = (1000 * (messageOut - thisObj.messageOut)) / samplePeriod;
                           var bytesInRate = (1000 * (bytesIn - thisObj.bytesIn)) / samplePeriod;
                           var bytesOutRate = (1000 * (bytesOut - thisObj.bytesOut)) / samplePeriod;

                           thisObj.msgInRate.innerHTML = msgInRate.toFixed(0);
                           var bytesInFormat = formatter.formatBytes( bytesInRate );
                           thisObj.bytesInRate.innerHTML = "(" + bytesInFormat.value;
                           thisObj.bytesInRateUnits.innerHTML = bytesInFormat.units + "/s)";

                           thisObj.msgOutRate.innerHTML = msgOutRate.toFixed(0);
                           var bytesOutFormat = formatter.formatBytes( bytesOutRate );
                           thisObj.bytesOutRate.innerHTML = "(" + bytesOutFormat.value;
                           thisObj.bytesOutRateUnits.innerHTML = bytesOutFormat.units + "/s)";

                           if(connections && thisObj.connections)
                           {
                               for(var i=0; i < connections.length; i++)
                               {
                                   var connection = connections[i];
                                   for(var j = 0; j < thisObj.connections.length; j++)
                                   {
                                       var oldConnection = thisObj.connections[j];
                                       if(oldConnection.id == connection.id)
                                       {
                                           msgOutRate = (1000 * (connection.messagesOut - oldConnection.messagesOut)) /
                                                        samplePeriod;
                                           connection.msgOutRate = msgOutRate.toFixed(0) + "msg/s";

                                           bytesOutRate = (1000 * (connection.bytesOut - oldConnection.bytesOut)) /
                                                          samplePeriod;
                                           var bytesOutRateFormat = formatter.formatBytes( bytesOutRate );
                                           connection.bytesOutRate = bytesOutRateFormat.value + bytesOutRateFormat.units + "/s";


                                           msgInRate = (1000 * (connection.messagesIn - oldConnection.messagesIn)) /
                                                       samplePeriod;
                                           connection.msgInRate = msgInRate.toFixed(0) + "msg/s";

                                           bytesInRate = (1000 * (connection.bytesIn - oldConnection.bytesIn)) /
                                                         samplePeriod;
                                           var bytesInRateFormat = formatter.formatBytes( bytesInRate );
                                           connection.bytesInRate = bytesInRateFormat.value + bytesInRateFormat.units + "/s";
                                       }


                                   }

                               }
                           }
                       }

                       thisObj.sampleTime = sampleTime;
                       thisObj.messageIn = messageIn;
                       thisObj.bytesIn = bytesIn;
                       thisObj.messageOut = messageOut;
                       thisObj.bytesOut = bytesOut;
                       thisObj.connections = connections;

                        this._updateGrids(thisObj.vhostData)

                        if (thisObj.details)
                        {
                            thisObj.details.update(thisObj.vhostData);
                        }
                        else
                        {
                            require(["qpid/management/virtualhost/" + thisObj.vhostData.type.toLowerCase() + "/show"],
                                 function(VirtualHostDetails)
                                 {
                                   thisObj.details = new VirtualHostDetails({containerNode:thisObj.virtualHostDetailsContainer, parent: thisObj});
                                   thisObj.details.update(thisObj.vhostData);
                                 }
                               );
                        }

           };

            Updater.prototype._updateGrids = function(data)
            {
                this.virtualHostChildren.style.display = data.state == "ACTIVE" ? "block" : "none";
                if (data.state == "ACTIVE" )
                {
                    util.updateUpdatableStore(this.queuesGrid, data.queues);
                    util.updateUpdatableStore(this.exchangesGrid, data.exchanges);

                    var exchangesGrid = this.exchangesGrid.grid;
                    for(var i=0; i< data.exchanges.length; i++)
                    {
                        var item = exchangesGrid.getItem(i);
                        var isStandard = item && item.name && util.isReservedExchangeName(item.name);
                        exchangesGrid.rowSelectCell.setDisabled(i, isStandard);
                    }
                    this.virtualHostConnections.style.display = data.connections ? "block" : "none";
                    util.updateUpdatableStore(this.connectionsGrid, data.connections);
                }
            };


           return VirtualHost;
       });
