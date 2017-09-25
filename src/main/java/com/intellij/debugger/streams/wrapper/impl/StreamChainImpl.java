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
package com.intellij.debugger.streams.wrapper.impl;

import com.intellij.debugger.streams.wrapper.*;
import com.intellij.psi.PsiElement;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public class StreamChainImpl implements StreamChain {
  private final QualifierExpression myQualifierExpression;
  private final List<IntermediateStreamCall> myIntermediateCalls;
  private final TerminatorStreamCall myTerminator;
  private final PsiElement myContext;

  public StreamChainImpl(@NotNull QualifierExpression qualifierExpression,
                         @NotNull List<IntermediateStreamCall> intermediateCalls,
                         @NotNull TerminatorStreamCall terminator,
                         @NotNull PsiElement context) {
    myQualifierExpression = qualifierExpression;
    myIntermediateCalls = intermediateCalls;
    myTerminator = terminator;
    myContext = context;
  }

  @NotNull
  @Override
  public QualifierExpression getQualifierExpression() {
    return myQualifierExpression;
  }

  @NotNull
  @Override
  public List<IntermediateStreamCall> getIntermediateCalls() {
    return Collections.unmodifiableList(myIntermediateCalls);
  }

  @NotNull
  @Override
  public StreamCall getCall(int index) {
    if (0 <= index && index < length()) {
      return doGetCall(index);
    }

    throw new IndexOutOfBoundsException("Call index out of bound: " + index);
  }

  @NotNull
  @Override
  public TerminatorStreamCall getTerminationCall() {
    return myTerminator;
  }

  @NotNull
  @Override
  public String getText() {
    final Iterator<StreamCall> iterator = StreamEx.of(myIntermediateCalls).map(x -> (StreamCall)x).append(myTerminator).iterator();
    final StringBuilder builder = new StringBuilder();
    builder.append(myQualifierExpression.getText()).append("\n").append(".");

    while (iterator.hasNext()) {
      final MethodCall call = iterator.next();
      final String args = args2Text(call.getArguments());
      builder.append(call.getName()).append(args);
      if (iterator.hasNext()) {
        builder.append("\n").append(".");
      }
    }

    return builder.toString();
  }

  @NotNull
  @Override
  public String getCompactText() {
    final StringBuilder builder = new StringBuilder();
    builder.append(myQualifierExpression.getText().replaceAll("\\s+", ""));
    for (final StreamCall call : StreamEx.of(myIntermediateCalls).map(x -> (StreamCall)x).append(myTerminator)) {
      builder.append(" -> ").append(call.getName());
    }

    return builder.toString();
  }

  @Override
  public int length() {
    return 1 + myIntermediateCalls.size();
  }

  @NotNull
  @Override
  public PsiElement getContext() {
    return myContext;
  }

  private StreamCall doGetCall(int index) {
    if (index < myIntermediateCalls.size()) {
      return myIntermediateCalls.get(index);
    }

    return myTerminator;
  }

  @NotNull
  private static String args2Text(@NotNull List<CallArgument> args) {
    return StreamEx.of(args).map(CallArgument::getText).joining(", ", "(", ")");
  }
}
