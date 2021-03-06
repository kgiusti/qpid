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

#include "qpid/legacystore/IdSequence.h"

using namespace mrg::msgstore;
using qpid::sys::Mutex;

IdSequence::IdSequence() : id(1) {}

u_int64_t IdSequence::next()
{
    Mutex::ScopedLock guard(lock);
    if (!id) id++; // avoid 0 when folding around
    return id++;
}

void IdSequence::reset(uint64_t value)
{
    //deliberately not threadsafe, used only on recovery
    id = value;
}
