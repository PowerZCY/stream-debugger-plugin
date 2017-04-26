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
package com.intellij.debugger.streams.psi.impl;

import com.intellij.debugger.streams.psi.StreamChainTransformer;
import com.intellij.debugger.streams.trace.impl.handler.type.GenericType;
import com.intellij.debugger.streams.trace.impl.handler.type.GenericTypeUtil;
import com.intellij.debugger.streams.wrapper.IntermediateStreamCall;
import com.intellij.debugger.streams.wrapper.ProducerStreamCall;
import com.intellij.debugger.streams.wrapper.StreamChain;
import com.intellij.debugger.streams.wrapper.TerminatorStreamCall;
import com.intellij.debugger.streams.wrapper.impl.IntermediateStreamCallImpl;
import com.intellij.debugger.streams.wrapper.impl.ProducerStreamCallImpl;
import com.intellij.debugger.streams.wrapper.impl.StreamChainImpl;
import com.intellij.debugger.streams.wrapper.impl.TerminatorStreamCallImpl;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Vitaliy.Bibaev
 */
public class StreamChainTransformerImpl implements StreamChainTransformer {
  @NotNull
  @Override
  public StreamChain transform(@NotNull List<PsiMethodCallExpression> streamExpressions, @NotNull PsiElement context) {
    // TODO: support variable.sum() where variable has a stream type
    if (streamExpressions.size() < 2) {
      throw new RuntimeException("Wrong length of the stream chain");
    }

    final ProducerStreamCall producer = createProducer(streamExpressions.get(0));

    final List<IntermediateStreamCall> intermediateCalls =
      createIntermediateCalls(producer.getTypeAfter(), streamExpressions.subList(1, streamExpressions.size() - 1));

    final GenericType typeBefore =
      intermediateCalls.isEmpty() ? producer.getTypeAfter() : intermediateCalls.get(intermediateCalls.size() - 1).getTypeAfter();
    final TerminatorStreamCall terminationCall = createTerminationCall(typeBefore, streamExpressions.get(streamExpressions.size() - 1));

    return new StreamChainImpl(producer, intermediateCalls, terminationCall, context);
  }

  @NotNull
  private ProducerStreamCall createProducer(@NotNull PsiMethodCallExpression expression) {
    GenericType prevCallType = resolveType(expression);
    return new ProducerStreamCallImpl(resolveProducerCallName(expression), resolveArguments(expression), prevCallType);
  }

  @NotNull
  private List<IntermediateStreamCall> createIntermediateCalls(@NotNull GenericType producerAfterType,
                                                               @NotNull List<PsiMethodCallExpression> expressions) {
    final List<IntermediateStreamCall> result = new ArrayList<>();

    GenericType typeBefore = producerAfterType;
    for (final PsiMethodCallExpression callExpression : expressions) {
      final String name = resolveMethodName(callExpression);
      final String args = resolveArguments(callExpression);
      final GenericType type = resolveType(callExpression);
      result.add(new IntermediateStreamCallImpl(name, args, typeBefore, type));
      typeBefore = type;
    }

    return result;
  }

  @NotNull
  private TerminatorStreamCall createTerminationCall(@NotNull GenericType typeBefore, @NotNull PsiMethodCallExpression expression) {
    final String name = resolveMethodName(expression);
    final String args = resolveArguments(expression);
    final GenericType resultType = resolveTerminationCallType(expression);
    return new TerminatorStreamCallImpl(name, args, typeBefore, resultType);
  }

  @NotNull
  private static String resolveProducerCallName(@NotNull PsiMethodCallExpression methodCall) {
    return methodCall.getChildren()[0].getText();
  }

  @NotNull
  private static String resolveArguments(@NotNull PsiMethodCallExpression methodCall) {
    return methodCall.getArgumentList().getText();
  }

  @NotNull
  private static String resolveMethodName(@NotNull PsiMethodCallExpression methodCall) {
    final String name = methodCall.getMethodExpression().getReferenceName();
    Objects.requireNonNull(name, "Method reference must be not null" + methodCall.getText());
    return name;
  }

  @NotNull
  private static PsiType extractType(@NotNull PsiMethodCallExpression expression) {
    final PsiType returnType = expression.getType();
    Objects.requireNonNull(returnType, "Method return type must be not null" + expression.getText());
    return returnType;
  }

  @NotNull
  private static GenericType resolveType(@NotNull PsiMethodCallExpression call) {
    return GenericTypeUtil.fromStreamPsiType(extractType(call));
  }

  @NotNull
  private static GenericType resolveTerminationCallType(@NotNull PsiMethodCallExpression call) {
    return GenericTypeUtil.fromPsiType(extractType(call));
  }
}
