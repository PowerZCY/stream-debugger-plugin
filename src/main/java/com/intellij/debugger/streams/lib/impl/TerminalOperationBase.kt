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
package com.intellij.debugger.streams.lib.impl

import com.intellij.debugger.streams.lib.TerminalOperation
import com.intellij.debugger.streams.resolve.ValuesOrderResolver
import com.intellij.debugger.streams.trace.CallTraceInterpreter
import com.intellij.debugger.streams.trace.TerminatorCallHandler
import com.intellij.debugger.streams.wrapper.TerminatorStreamCall

/**
 * @author Vitaliy.Bibaev
 */
abstract class TerminalOperationBase(override val name: String,
                                     private val handlerFactory: (TerminatorStreamCall, String) -> TerminatorCallHandler,
                                     override val traceInterpreter: CallTraceInterpreter,
                                     override val valuesOrderResolver: ValuesOrderResolver) : TerminalOperation {
  override fun getTraceHandler(call: TerminatorStreamCall, resultExpression: String): TerminatorCallHandler =
    handlerFactory.invoke(call, resultExpression)
}