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
package org.apache.qpid.server.model.testmodels.hierarchy;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.qpid.server.model.ConfiguredObject;
import org.apache.qpid.server.model.ConfiguredObjectFactory;
import org.apache.qpid.server.model.ConfiguredObjectFactoryImpl;
import org.apache.qpid.server.model.ConfiguredObjectTypeRegistry;
import org.apache.qpid.server.model.Model;
import org.apache.qpid.server.plugin.ConfiguredObjectRegistration;

public class TestModel extends Model
{
    private static final Model INSTANCE = new TestModel();
    private Class<? extends ConfiguredObject>[] _supportedCategories =
            new Class[] {
                    TestCar.class,
                    TestEngine.class
            };

    private final ConfiguredObjectFactory _objectFactory;
    private ConfiguredObjectTypeRegistry _registry;

    private TestModel()
    {
        this(null);
    }

    public TestModel(final ConfiguredObjectFactory objectFactory)
    {
        _objectFactory = objectFactory == null ? new ConfiguredObjectFactoryImpl(this) : objectFactory;

        ConfiguredObjectRegistration configuredObjectRegistration = new ConfiguredObjectRegistrationImpl();

        _registry = new ConfiguredObjectTypeRegistry(Collections.singletonList(configuredObjectRegistration), Collections.EMPTY_LIST);
    }


    @Override
    public Collection<Class<? extends ConfiguredObject>> getSupportedCategories()
    {
        return Arrays.asList(_supportedCategories);
    }

    @Override
    public Collection<Class<? extends ConfiguredObject>> getChildTypes(final Class<? extends ConfiguredObject> parent)
    {
        return TestCar.class.isAssignableFrom(parent)
                ? Collections.<Class<? extends ConfiguredObject>>singleton(TestEngine.class)
                : Collections.<Class<? extends ConfiguredObject>>emptySet();
    }

    @Override
    public Class<? extends ConfiguredObject> getRootCategory()
    {
        return TestCar.class;
    }

    @Override
    public Collection<Class<? extends ConfiguredObject>> getParentTypes(final Class<? extends ConfiguredObject> child)
    {
        return TestEngine.class.isAssignableFrom(child)
                ? Collections.<Class<? extends ConfiguredObject>>singleton(TestCar.class)
                : Collections.<Class<? extends ConfiguredObject>>emptySet();
    }

    @Override
    public int getMajorVersion()
    {
        return 99;
    }

    @Override
    public int getMinorVersion()
    {
        return 99;
    }

    @Override
    public ConfiguredObjectFactory getObjectFactory()
    {
        return _objectFactory;
    }

    @Override
    public ConfiguredObjectTypeRegistry getTypeRegistry()
    {
        return _registry;
    }

    public static Model getInstance()
    {
        return INSTANCE;
    }
}
