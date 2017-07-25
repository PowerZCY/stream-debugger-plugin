/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.debugger.streams.trace.impl.handler

import com.intellij.debugger.streams.trace.impl.handler.type.GenericType
import com.intellij.debugger.streams.wrapper.IntermediateStreamCall
import com.intellij.debugger.streams.wrapper.impl.IntermediateStreamCallImpl
import com.intellij.openapi.util.TextRange

/**
 * @author Vitaliy.Bibaev
 */
class ParallelHandler(num: Int, private val call: IntermediateStreamCall)
  : PeekTracerHandler(num, call.name, call.typeBefore, call.typeAfter) {
  override fun additionalCallsAfter(): List<IntermediateStreamCall> {
    val calls = super.additionalCallsAfter().toMutableList()
    calls.add(0, SequentialCall(call.packageName, call.typeBefore))
    return calls
  }

  private class SequentialCall(packageName: String, elementsType: GenericType)
    : IntermediateStreamCallImpl("sequential", emptyList(), elementsType, elementsType, TextRange.EMPTY_RANGE, packageName) {
  }
}