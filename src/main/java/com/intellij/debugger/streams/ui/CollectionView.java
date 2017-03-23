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
package com.intellij.debugger.streams.ui;

import com.intellij.debugger.engine.evaluation.EvaluationContextImpl;
import com.intellij.debugger.streams.trace.TraceElement;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public class CollectionView extends JPanel implements Disposable, TraceContainer {
  private final CollectionTree myInstancesTree;
  private final List<ValueWithPosition> myValues = new ArrayList<>();

  CollectionView(@NotNull String header, @NotNull EvaluationContextImpl evaluationContext, @NotNull List<TraceElement> values) {
    super(new BorderLayout());
    add(new JBLabel(header), BorderLayout.NORTH);

    myInstancesTree = new CollectionTree(values, evaluationContext);

    final JBScrollPane scroll = new JBScrollPane(myInstancesTree);

    add(scroll, BorderLayout.CENTER);
    Disposer.register(this, myInstancesTree);
  }

  @Override
  public void dispose() {
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    final Rectangle visibleRect = myInstancesTree.getVisibleRect();
    for (final ValueWithPosition value : myValues) {
      final Rectangle rect = myInstancesTree.getRectByValue(value.getTraceElement());
      if (rect == null || !visibleRect.intersects(rect)) {
        value.setPosition(-1);
      }
      else {
        value.setPosition(rect.x + rect.height / 2);
      }
    }
  }

  @Override
  public void highlight(@NotNull List<TraceElement> elements) {
    myInstancesTree.highlight(elements);
  }

  @Override
  public void select(@NotNull List<TraceElement> elements) {
    myInstancesTree.select(elements);
  }

  @Override
  public void addSelectionListener(@NotNull ValuesSelectionListener listener) {
    myInstancesTree.addSelectionListener(listener);
  }

  @NotNull
  protected CollectionTree getInstancesTree() {
    return myInstancesTree;
  }
}
