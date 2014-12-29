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

import java.util.Map;
import java.util.Set;

import org.apache.qpid.server.model.AbstractConfiguredObject;
import org.apache.qpid.server.model.ManagedAttributeField;
import org.apache.qpid.server.model.ManagedObject;
import org.apache.qpid.server.model.ManagedObjectFactoryConstructor;
import org.apache.qpid.server.model.State;

@ManagedObject( category = false, type = TestChildCategoryImpl.TEST_CHILD_TYPE )
public class TestChildCategoryImpl
        extends AbstractConfiguredObject<TestChildCategoryImpl> implements TestChildCategory<TestChildCategoryImpl>
{
    public static final String TEST_CHILD_TYPE = "testchild";


    @ManagedAttributeField
    private String _validValueNotInterpolated;

    @ManagedAttributeField
    private int _intValue;

    @ManagedAttributeField
    private Set<Integer> _integerSet;


    @ManagedObjectFactoryConstructor
    public TestChildCategoryImpl(final Map<String, Object> attributes, TestRootCategory<?> parent)
    {
        super(parentsMap(parent), attributes);
    }

    @Override
    public String getValidValueNotInterpolated()
    {
        return _validValueNotInterpolated;
    }

    @Override
    public int getIntValue()
    {
        return _intValue;
    }

    @Override
    public Set<Integer> getIntegerSet()
    {
        return _integerSet;
    }
}
