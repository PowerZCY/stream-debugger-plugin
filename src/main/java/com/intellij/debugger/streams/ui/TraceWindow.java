/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.debugger.streams.ui;

import com.intellij.debugger.engine.evaluation.EvaluationContextImpl;
import com.intellij.debugger.streams.resolve.ResolvedTrace;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public class TraceWindow extends DialogWrapper {
  private final List<ResolvedTrace> myTrace;
  private final EvaluationContextImpl myEvaluationContext;

  public TraceWindow(@NotNull EvaluationContextImpl evaluationContext,
                     @Nullable Project project,
                     @NotNull List<ResolvedTrace> trace) {
    super(project, false);
    setModal(false);
    setTitle("Stream Trace");
    myTrace = trace;
    myEvaluationContext = evaluationContext;

    init();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    final JPanel panel = new JPanel(new GridLayout(1, myTrace.size()));
    CollectionView prev = null;
    for (final ResolvedTrace trace : myTrace) {
      final CollectionView collectionView = new CollectionView(myEvaluationContext, trace);
      Disposer.register(myDisposable, collectionView);
      if (prev != null) {
        prev.setForwardListener(collectionView);
        collectionView.setBackwardListener(prev);
      }
      panel.add(collectionView);
      prev = collectionView;
    }

    return panel;
  }
}
