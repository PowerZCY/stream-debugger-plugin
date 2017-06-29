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
package com.intellij.debugger.streams.trace.impl.handler;

import com.intellij.debugger.streams.trace.TraceHandler;
import com.intellij.debugger.streams.trace.impl.TraceExpressionBuilderImpl;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public abstract class HandlerBase implements TraceHandler {
  private HandlerBase() {
  }

  private static final String DECLARATION_FORMAT = "final %s %s = %s;" + TraceExpressionBuilderImpl.LINE_SEPARATOR;

  @NotNull
  @Override
  final public String additionalVariablesDeclaration() {
    final StringBuilder stringBuilder = new StringBuilder();
    final List<Variable> variables = getVariables();
    for (final Variable variable : variables) {
      stringBuilder.append(String.format(DECLARATION_FORMAT, variable.getTypeName(), variable.getName(), variable.getInitialExpression()));
    }

    getClassesDeclarations().forEach(stringBuilder::append);

    return stringBuilder.toString();
  }

  @NotNull
  protected abstract List<Variable> getVariables();

  @NotNull
  protected List<String> getClassesDeclarations() {
    return Collections.emptyList();
  }

  static abstract class Producer extends HandlerBase implements TraceExpressionBuilderImpl.ProducerCallTraceHandler {
  }

  public static abstract class Intermediate extends HandlerBase implements TraceExpressionBuilderImpl.IntermediateCallTraceHandler {
  }

  static abstract class Terminator extends HandlerBase implements TraceExpressionBuilderImpl.TerminatorCallTraceHandler {
  }
}
