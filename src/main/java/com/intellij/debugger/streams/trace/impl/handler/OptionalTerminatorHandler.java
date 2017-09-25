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

import com.intellij.debugger.streams.trace.TerminatorCallHandler;
import com.intellij.debugger.streams.trace.dsl.CodeBlock;
import com.intellij.debugger.streams.trace.dsl.Expression;
import com.intellij.debugger.streams.trace.dsl.VariableDeclaration;
import com.intellij.debugger.streams.trace.dsl.impl.TextExpression;
import com.intellij.debugger.streams.trace.dsl.impl.java.JavaTypes;
import com.intellij.debugger.streams.trace.impl.handler.type.GenericType;
import com.intellij.debugger.streams.wrapper.IntermediateStreamCall;
import com.intellij.debugger.streams.wrapper.TerminatorStreamCall;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public class OptionalTerminatorHandler implements TerminatorCallHandler {
  private final TerminatorHandler myTerminatorHandler;
  private final GenericType myOptionalType;
  private final String myResultExpression;

  public OptionalTerminatorHandler(@NotNull TerminatorStreamCall call, @NotNull String resultExpression) {
    myTerminatorHandler = new TerminatorHandler(call.getTypeBefore());
    myOptionalType = JavaTypes.INSTANCE.unwrapOptional(call.getResultType());
    myResultExpression = resultExpression;
  }

  @NotNull
  @Override
  public List<VariableDeclaration> additionalVariablesDeclaration() {
    return myTerminatorHandler.additionalVariablesDeclaration();
  }

  @NotNull
  @Override
  public CodeBlock prepareResult() {
    return myTerminatorHandler.prepareResult();
  }

  @NotNull
  @Override
  public Expression getResultExpression() {
    return new TextExpression("new Object[] { " + myTerminatorHandler.getResultExpression() + ", " + buildOptionalExpression() + "}");
  }

  @NotNull
  @Override
  public List<IntermediateStreamCall> additionalCallsBefore() {
    return myTerminatorHandler.additionalCallsBefore();
  }

  @NotNull
  private String buildOptionalExpression() {
    final String optionalContentExpression = String.format("new %s[] { %s.orElse(%s)}", myOptionalType.getVariableTypeName(),
                                                           myResultExpression,
                                                           myOptionalType.getDefaultValue());
    return String.format("new Object[] { new boolean[] { %s.isPresent() }, %s }", myResultExpression, optionalContentExpression);
  }
}
