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
package com.intellij.debugger.streams.trace.dsl.impl.java

import com.intellij.debugger.streams.trace.dsl.Expression
import com.intellij.debugger.streams.trace.dsl.Lambda
import com.intellij.debugger.streams.trace.dsl.Statement
import com.intellij.debugger.streams.trace.dsl.VariableDeclaration
import com.intellij.debugger.streams.trace.dsl.impl.TextExpression
import com.intellij.debugger.streams.trace.dsl.impl.common.MapVariableBase
import com.intellij.debugger.streams.trace.impl.handler.type.GenericType

/**
 * @author Vitaliy.Bibaev
 */
class JavaMapVariable(keyType: GenericType,
                      valueType: GenericType,
                      name: String,
                      private val linked: Boolean)
  : MapVariableBase(keyType, valueType, "${getMapType(linked)}<${keyType.genericTypeName}, ${valueType.genericTypeName}>", name) {
  companion object {
    fun getMapType(linked: Boolean): String = "java.util.${if (linked) "Linked" else ""}HashMap"
  }

  override fun get(key: Expression): Expression = TextExpression("$name.get(${key.toCode()})")

  override operator fun set(key: Expression, newValue: Expression): Expression =
    TextExpression("$name.put(${key.toCode()}, ${newValue.toCode()})")

  override fun contains(key: Expression): Expression = TextExpression("$name.contains(${key.toCode()})")

  override fun defaultDeclaration(isMutable: Boolean): VariableDeclaration =
    JavaVariableDeclaration(this, false, TextExpression("new ${getMapType(linked)}<>()"))

  override fun keys(): Expression = call("keySet")

  override fun size(): Expression = call("size")

  override fun computeIfAbsent(key: Expression, supplier: Lambda): Statement = call("computeIfAbsent", key, supplier)
}