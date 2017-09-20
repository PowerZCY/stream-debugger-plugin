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

import com.intellij.debugger.streams.trace.dsl.CodeBlock;
import com.intellij.debugger.streams.trace.dsl.Expression;
import com.intellij.debugger.streams.trace.dsl.impl.java.JavaTypes;
import com.intellij.debugger.streams.trace.impl.handler.type.GenericType;
import com.intellij.debugger.streams.wrapper.IntermediateStreamCall;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public class TerminatorHandler extends HandlerBase.Terminator {
  private final PeekTracerHandler myPeekTracerHandler;

  public TerminatorHandler(@NotNull GenericType beforeType) {
    myPeekTracerHandler = new PeekTracerHandler(Integer.MAX_VALUE, "terminator", beforeType, JavaTypes.INSTANCE.getAnyType());
  }

  @NotNull
  @Override
  protected List<Variable> getVariables() {
    return myPeekTracerHandler.getVariables();
  }

  @NotNull
  @Override
  public List<IntermediateStreamCall> additionalCallsBefore() {
    return myPeekTracerHandler.additionalCallsBefore();
  }

  @NotNull
  @Override
  public CodeBlock prepareResult() {
    return myPeekTracerHandler.prepareResult();
  }

  @NotNull
  @Override
  public Expression getResultExpression() {
    return myPeekTracerHandler.getResultExpression();
  }
}
