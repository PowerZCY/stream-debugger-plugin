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

import com.intellij.debugger.streams.trace.dsl.Types
import com.intellij.debugger.streams.trace.impl.handler.type.*
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiType
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.TypeConversionUtil
import one.util.streamex.StreamEx
import org.jetbrains.annotations.Contract

/**
 * @author Vitaliy.Bibaev
 */
object JavaTypes : Types {
  override val anyType: GenericType = ClassTypeImpl("java.lang.Object", "new java.lang.Object()")

  override val integerType: GenericType = GenericTypeImpl("int", "java.lang.Integer", "0")

  override val booleanType: GenericType = GenericTypeImpl("boolean", "java.lang.Boolean", "false")
  override val doubleType: GenericType = GenericTypeImpl("double", "java.lang.Double", "0.")
  override val basicExceptionType: GenericType = ClassTypeImpl("java.lang.Throwable")
  override val voidType: GenericType = GenericTypeImpl("void", "java.lang.Void", "null")

  override val timeVariableType: GenericType = ClassTypeImpl("java.util.concurrent.atomic.AtomicInteger",
                                                             "new java.util.concurrent.atomic.AtomicInteger()")
  override val stringType: GenericType = ClassTypeImpl("java.lang.String", "\"\"")
  override val longType: GenericType = GenericTypeImpl("long", "java.lang.Long", "0L")

  override fun array(elementType: GenericType): ArrayType =
    ArrayTypeImpl(elementType, { "$it[]" }, "new ${elementType.variableTypeName}[] {}")

  override fun map(keyType: GenericType, valueType: GenericType): MapType =
    MapTypeImpl(keyType, valueType, { keys, values -> "java.util.Map<$keys, $values>" }, "new java.util.HashMap<>()")

  override fun linkedMap(keyType: GenericType, valueType: GenericType): MapType =
    MapTypeImpl(keyType, valueType, { keys, values -> "java.util.Map<$keys, $values>" }, "new java.util.LinkedHashMap<>()")

  override fun list(elementsType: GenericType): ListType =
    ListTypeImpl(elementsType, { "java.util.List<$it>" }, "new java.util.ArrayList<>()")

  private val optional: GenericType = ClassTypeImpl("java.util.Optional")
  private val optionalInt: GenericType = ClassTypeImpl("java.util.OptionalInt")
  private val optionalLong: GenericType = ClassTypeImpl("java.util.OptionalLong")
  private val optionalDouble: GenericType = ClassTypeImpl("java.util.OptionalDouble")

  private val OPTIONAL_TYPES = StreamEx.of(optional, optionalInt, optionalLong, optionalDouble).toSet()

  fun fromStreamPsiType(streamPsiType: PsiType): GenericType {
    return when {
      InheritanceUtil.isInheritor(streamPsiType, CommonClassNames.JAVA_UTIL_STREAM_INT_STREAM) -> integerType
      InheritanceUtil.isInheritor(streamPsiType, CommonClassNames.JAVA_UTIL_STREAM_LONG_STREAM) -> longType
      InheritanceUtil.isInheritor(streamPsiType, CommonClassNames.JAVA_UTIL_STREAM_DOUBLE_STREAM) -> doubleType
      PsiType.VOID == streamPsiType -> voidType
      else -> anyType
    }
  }

  fun fromPsiClass(psiClass: PsiClass): GenericType {
    return when {
      InheritanceUtil.isInheritor(psiClass, CommonClassNames.JAVA_UTIL_STREAM_INT_STREAM) -> integerType
      InheritanceUtil.isInheritor(psiClass, CommonClassNames.JAVA_UTIL_STREAM_LONG_STREAM) -> longType
      InheritanceUtil.isInheritor(psiClass, CommonClassNames.JAVA_UTIL_STREAM_DOUBLE_STREAM) -> doubleType
      else -> anyType
    }
  }

  fun fromPsiType(type: PsiType): GenericType {
    return when (type) {
      PsiType.VOID -> voidType
      PsiType.INT -> integerType
      PsiType.DOUBLE -> doubleType
      PsiType.LONG -> longType
      PsiType.BOOLEAN -> booleanType
      else -> ClassTypeImpl(TypeConversionUtil.erasure(type).canonicalText)
    }
  }

  @Contract(pure = true)
  private fun isOptional(type: GenericType): Boolean {
    return OPTIONAL_TYPES.contains(type)
  }

  fun unwrapOptional(type: GenericType): GenericType {
    assert(isOptional(type))

    return when (type) {
      optionalInt -> integerType
      optionalLong -> longType
      optionalDouble -> doubleType
      else -> anyType
    }
  }
}