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
package org.apache.qpid.server.model.testmodel;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.qpid.server.configuration.updater.CurrentThreadTaskExecutor;
import org.apache.qpid.server.configuration.updater.TaskExecutor;
import org.apache.qpid.server.model.AbstractConfiguredObject;
import org.apache.qpid.server.model.ManagedAttributeField;
import org.apache.qpid.server.model.ManagedObject;
import org.apache.qpid.server.model.ManagedObjectFactoryConstructor;
import org.apache.qpid.server.model.VirtualHost;

@ManagedObject( category = false,
                type = TestRootCategoryImpl.TEST_ROOT_TYPE,
                validChildTypes = "org.apache.qpid.server.model.testmodel.TestRootCategoryImpl#getSupportedChildTypes()")
public class TestRootCategoryImpl extends AbstractConfiguredObject<TestRootCategoryImpl>
        implements TestRootCategory<TestRootCategoryImpl>
{
    public static final String TEST_ROOT_TYPE = "testroot";

    @ManagedAttributeField
    private String _automatedPersistedValue;

    @ManagedAttributeField
    private String _automatedNonPersistedValue;

    @ManagedAttributeField
    private String _defaultedValue;

    @ManagedAttributeField
    private String _stringValue;

    @ManagedAttributeField
    private Map<String,String> _mapValue;

    @ManagedAttributeField
    private String _validValue;

    @ManagedAttributeField
    private TestEnum _enumValue;

    @ManagedAttributeField
    private Set<TestEnum> _enumSetValues;


    @ManagedObjectFactoryConstructor
    public TestRootCategoryImpl(final Map<String, Object> attributes)
    {
        super(parentsMap(), attributes, newTaskExecutor(), TestModel.getInstance());
    }

    private static CurrentThreadTaskExecutor newTaskExecutor()
    {
        CurrentThreadTaskExecutor currentThreadTaskExecutor = new CurrentThreadTaskExecutor();
        currentThreadTaskExecutor.start();
        return currentThreadTaskExecutor;
    }

    public TestRootCategoryImpl(final Map<String, Object> attributes,
                                final TaskExecutor taskExecutor)
    {
        super(parentsMap(), attributes, taskExecutor);
    }


    @Override
    public String getAutomatedPersistedValue()
    {
        return _automatedPersistedValue;
    }

    @Override
    public String getAutomatedNonPersistedValue()
    {
        return _automatedNonPersistedValue;
    }

    @Override
    public String getDefaultedValue()
    {
        return _defaultedValue;
    }

    @Override
    public String getStringValue()
    {
        return _stringValue;
    }

    @Override
    public Map<String, String> getMapValue()
    {
        return _mapValue;
    }

    @Override
    public TestEnum getEnumValue()
    {
        return _enumValue;
    }

    @Override
    public Set<TestEnum> getEnumSetValues()
    {
        return _enumSetValues;
    }

    @Override
    public String getValidValue()
    {
        return _validValue;
    }

    @SuppressWarnings("unused")
    public static Map<String, Collection<String>> getSupportedChildTypes()
    {
        return Collections.singletonMap(TestChildCategory.class.getSimpleName(), (Collection<String>)Collections.singleton(TestChildCategoryImpl.TEST_CHILD_TYPE));
    }
}
