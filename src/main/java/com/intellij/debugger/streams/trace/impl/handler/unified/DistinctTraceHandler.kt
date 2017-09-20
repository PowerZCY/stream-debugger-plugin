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
package com.intellij.debugger.streams.trace.impl.handler.unified

import com.intellij.debugger.streams.trace.dsl.CodeBlock
import com.intellij.debugger.streams.trace.dsl.Dsl
import com.intellij.debugger.streams.trace.dsl.Expression
import com.intellij.debugger.streams.trace.dsl.VariableDeclaration
import com.intellij.debugger.streams.trace.dsl.impl.TextExpression
import com.intellij.debugger.streams.wrapper.IntermediateStreamCall

/**
 * @author Vitaliy.Bibaev
 */
class DistinctTraceHandler(num: Int, private val myCall: IntermediateStreamCall, dsl: Dsl) : HandlerBase.Intermediate(dsl) {
  private val myPeekTracer = PeekTraceHandler(num, "distinct", myCall.typeBefore, myCall.typeAfter, dsl)
  override fun additionalVariablesDeclaration(): List<VariableDeclaration> =
    myPeekTracer.additionalVariablesDeclaration()

  override fun prepareResult(): CodeBlock {
    val before = myPeekTracer.beforeMap
    val after = myPeekTracer.afterMap
    return dsl.block {
      val nestedMapType = types.map(types.INT, myCall.typeBefore)
      val mapping = linkedMap(types.INT, types.INT, "mapping")
      declare(mapping.defaultDeclaration())
      val eqClasses = map(myCall.typeBefore, nestedMapType, "eqClasses")
      declare(eqClasses, TextExpression(eqClasses.type.defaultValue), false)
      forEachLoop(variable(types.INT, "beforeTime"), before.keys()) {
        val beforeValue = declare(variable(myCall.typeBefore, "beforeValue"), before.get(loopVariable), false)
        val computeIfAbsentExpression = eqClasses.computeIfAbsent(beforeValue, lambda("key") {
          doReturn(TextExpression(nestedMapType.defaultValue))
        })
        val classItems = map(types.INT, myCall.typeBefore, "classItems")
        declare(classItems, computeIfAbsentExpression, false)
        +classItems.set(loopVariable, beforeValue)
      }

      forEachLoop(variable(types.INT, "afterTime"), after.keys()) {
        val afterTime = loopVariable
        val afterValue = declare(variable(myCall.typeAfter, "afterValue"), after.get(loopVariable), false)
        val classes = map(types.INT, myCall.typeBefore, "classes")
        declare(classes, eqClasses.get(afterValue), false)
        forEachLoop(variable(types.INT, "classElementTime"), classes.keys()) {
          +mapping.set(loopVariable, afterTime)
        }
      }

      add(mapping.convertToArray(dsl, "resolve"))
      add(myPeekTracer.prepareResult())

      declare(variable(types.ANY, "peekResult"), myPeekTracer.resultExpression, false)
    }
  }

  override fun getResultExpression(): Expression =
    dsl.newArray(dsl.types.ANY, TextExpression("peekResult"), TextExpression("resolve"))

  override fun additionalCallsBefore(): List<IntermediateStreamCall> = myPeekTracer.additionalCallsBefore()


  override fun additionalCallsAfter(): List<IntermediateStreamCall> = myPeekTracer.additionalCallsAfter()
}