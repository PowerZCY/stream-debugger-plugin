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
package com.intellij.debugger.streams.trace.impl;

import com.intellij.debugger.streams.resolve.*;
import com.intellij.debugger.streams.resolve.impl.ResolvedIntermediateCallImpl;
import com.intellij.debugger.streams.resolve.impl.ResolvedProducerCallImpl;
import com.intellij.debugger.streams.resolve.impl.ResolvedStreamChainImpl;
import com.intellij.debugger.streams.resolve.impl.ResolvedTerminatorCallImpl;
import com.intellij.debugger.streams.trace.*;
import com.intellij.debugger.streams.wrapper.IntermediateStreamCall;
import com.intellij.debugger.streams.wrapper.StreamCall;
import com.intellij.debugger.streams.wrapper.StreamChain;
import com.intellij.debugger.streams.wrapper.TraceUtil;
import com.sun.jdi.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Vitaliy.Bibaev
 */
public class TracingResultImpl implements TracingResult {
  private final Value myStreamResult;
  private final List<TraceInfo> myTrace;
  private final boolean myIsResultException;
  private final StreamChain mySourceChain;

  TracingResultImpl(@NotNull StreamChain chain, @Nullable Value streamResult, @NotNull List<TraceInfo> trace, boolean isResultException) {
    myStreamResult = streamResult;
    myTrace = trace;
    mySourceChain = chain;
    myIsResultException = isResultException;
  }

  @Nullable
  @Override
  public Value getResult() {
    return myStreamResult;
  }

  @Override
  public boolean exceptionThrown() {
    return myIsResultException;
  }

  @NotNull
  @Override
  public List<TraceInfo> getTrace() {
    return myTrace;
  }

  @NotNull
  @Override
  public ResolvedTracingResult resolve() {
    assert myTrace.size() == mySourceChain.length();

    final ResolverFactory resolverFactory = ResolverFactoryImpl.getInstance();
    List<ValuesOrderResolver.Result> resolvedTraces = myTrace.stream()
      .map(x -> resolverFactory.getResolver(x.getCall().getName()).resolve(x))
      .collect(Collectors.toList());

    final TraceInfo producerTrace = myTrace.get(0);
    final List<IntermediateStreamCall> intermediateCalls = mySourceChain.getIntermediateCalls();

    final ResolvedStreamChainImpl.Builder chainBuilder = new ResolvedStreamChainImpl.Builder();
    final List<TraceElement> valuesAfterProducer = TraceUtil.sortedByTime(producerTrace.getValuesOrderAfter().values());
    final ProducerStateImpl producerState =
      new ProducerStateImpl(valuesAfterProducer, mySourceChain.getProducerCall(), myTrace.get(1).getCall(),
                            resolvedTraces.get(1).getDirectOrder());

    final ResolvedProducerCallImpl resolvedProducer = new ResolvedProducerCallImpl(mySourceChain.getProducerCall(), producerState);
    chainBuilder.setProducer(resolvedProducer);
    if (intermediateCalls.isEmpty()) {
      chainBuilder.setTerminator(buildResolvedTerminationCall(myTrace.get(1), producerState, resolvedTraces.get(1).getReverseOrder()));
    }
    else {
      List<IntermediateCallStateBuilder> builders = Stream.generate(IntermediateCallStateBuilder::new)
        .limit(intermediateCalls.size())
        .collect(Collectors.toList());
      for (int i = 0; i < builders.size() - 1; i++) {
        final IntermediateCallStateBuilder builder = builders.get(i);
        builder.elements = TraceUtil.sortedByTime(myTrace.get(i + 1).getValuesOrderAfter().values());
        builder.prevCall = intermediateCalls.get(i);
        builder.nextCall = intermediateCalls.get(i + 1);
        final ValuesOrderResolver.Result currentResolvedTrace = resolvedTraces.get(i + 1);
        final ValuesOrderResolver.Result nextResolvedTrace = resolvedTraces.get(i + 2);
        builder.toPrev = currentResolvedTrace.getReverseOrder();
        builder.toNext = nextResolvedTrace.getDirectOrder();
      }

      final IntermediateCallStateBuilder lastIntermediateBuilder = builders.get(builders.size() - 1);
      lastIntermediateBuilder.elements = TraceUtil.sortedByTime(myTrace.get(myTrace.size() - 1).getValuesOrderBefore().values());
      lastIntermediateBuilder.prevCall = intermediateCalls.get(intermediateCalls.size() - 1);
      lastIntermediateBuilder.nextCall = mySourceChain.getTerminationCall();
      lastIntermediateBuilder.toPrev = resolvedTraces.get(resolvedTraces.size() - 2).getReverseOrder();
      lastIntermediateBuilder.toNext = resolvedTraces.get(resolvedTraces.size() - 1).getDirectOrder();
      final List<IntermediateStateImpl> states = builders.stream()
        .map(IntermediateCallStateBuilder::build)
        .collect(Collectors.toList());
      chainBuilder.addIntermediate(new ResolvedIntermediateCallImpl(intermediateCalls.get(0), producerState, states.get(0)));
      for (int i = 1; i < states.size(); i++) {
        chainBuilder.addIntermediate(new ResolvedIntermediateCallImpl(intermediateCalls.get(i), states.get(i - 1), states.get(i)));
      }

      chainBuilder.setTerminator(buildResolvedTerminationCall(myTrace.get(myTrace.size() - 1), states.get(states.size() - 1),
                                                              resolvedTraces.get(resolvedTraces.size() - 1).getReverseOrder()));
    }

    return new MyResolvedResult(chainBuilder.build());
  }

  private ResolvedStreamCall.Terminator buildResolvedTerminationCall(@NotNull TraceInfo terminatorTrace,
                                                                     @NotNull BidirectionalAwareState previousState,
                                                                     @NotNull Map<TraceElement, List<TraceElement>>
                                                                       terminationToPrevMapping) {
    final TraceElementImpl resultValue = new TraceElementImpl(Integer.MAX_VALUE, myStreamResult);
    final List<TraceElement> after = TraceUtil.sortedByTime(terminatorTrace.getValuesOrderAfter().values());
    final TerminationStateImpl terminatorState =
      new TerminationStateImpl(resultValue, previousState.getNextCall(), after, terminationToPrevMapping);
    return new ResolvedTerminatorCallImpl(mySourceChain.getTerminationCall(), previousState, terminatorState);
  }

  private class MyResolvedResult implements ResolvedTracingResult {

    @NotNull private final ResolvedStreamChain myChain;

    MyResolvedResult(@NotNull ResolvedStreamChain resolvedStreamChain) {
      myChain = resolvedStreamChain;
    }

    @NotNull
    @Override
    public ResolvedStreamChain getResolvedChain() {
      return myChain;
    }

    @NotNull
    @Override
    public StreamChain getSourceChain() {
      return mySourceChain;
    }

    @Override
    public boolean exceptionThrown() {
      return myIsResultException;
    }

    @Nullable
    @Override
    public Value getResult() {
      return myStreamResult;
    }
  }

  private static class IntermediateCallStateBuilder {
    List<TraceElement> elements;
    StreamCall prevCall;
    StreamCall nextCall;
    Map<TraceElement, List<TraceElement>> toPrev;
    Map<TraceElement, List<TraceElement>> toNext;

    public IntermediateStateImpl build() {
      Objects.requireNonNull(elements);
      Objects.requireNonNull(nextCall);
      Objects.requireNonNull(prevCall);
      Objects.requireNonNull(toPrev);
      Objects.requireNonNull(toNext);
      return new IntermediateStateImpl(elements, nextCall, prevCall, toPrev, toNext);
    }
  }
}
