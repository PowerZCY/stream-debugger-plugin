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
package com.intellij.debugger.streams.resolve;

import com.intellij.debugger.streams.trace.IntermediateState;
import com.intellij.debugger.streams.trace.NextAwareState;
import com.intellij.debugger.streams.trace.PrevAwareState;
import com.intellij.debugger.streams.wrapper.IntermediateStreamCall;
import com.intellij.debugger.streams.wrapper.StreamCall;
import com.intellij.debugger.streams.wrapper.TerminatorStreamCall;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vitaliy.Bibaev
 */
public interface ResolvedStreamCall<CALL extends StreamCall, STATE_BEFORE extends IntermediateState, STATE_AFTER extends IntermediateState> {
  @NotNull
  CALL getCall();

  @Nullable
  STATE_BEFORE getStateBefore();

  @Nullable
  STATE_AFTER getStateAfter();

  interface Intermediate extends ResolvedStreamCall<IntermediateStreamCall, NextAwareState, PrevAwareState> {
    @NotNull
    @Override
    NextAwareState getStateBefore();

    @NotNull
    @Override
    PrevAwareState getStateAfter();
  }

  interface Terminator extends ResolvedStreamCall<TerminatorStreamCall, NextAwareState, PrevAwareState> {
    @NotNull
    @Override
    NextAwareState getStateBefore();
  }
}
