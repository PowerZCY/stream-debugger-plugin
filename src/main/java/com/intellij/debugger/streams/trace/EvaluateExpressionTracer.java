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
package com.intellij.debugger.streams.trace;

import com.intellij.debugger.engine.JavaValue;
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl;
import com.intellij.debugger.streams.wrapper.StreamChain;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.evaluation.EvaluationMode;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XValue;
import com.intellij.xdebugger.impl.breakpoints.XExpressionImpl;
import com.intellij.xdebugger.impl.ui.tree.nodes.XEvaluationCallbackBase;
import com.sun.jdi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vitaliy.Bibaev
 */
public class EvaluateExpressionTracer implements StreamTracer {
  private final XDebugSession mySession;
  private final TraceExpressionBuilder myExpressionBuilder;
  private final TraceResultInterpreter myResultInterpreter;

  public EvaluateExpressionTracer(@NotNull XDebugSession session,
                                  @NotNull TraceExpressionBuilder expressionBuilder,
                                  @NotNull TraceResultInterpreter interpreter) {
    mySession = session;
    myExpressionBuilder = expressionBuilder;
    myResultInterpreter = interpreter;
  }

  @Override
  public void trace(@NotNull StreamChain chain, @NotNull TracingCallback callback) {
    final String streamTraceExpression = myExpressionBuilder.createTraceExpression(chain);

    final XStackFrame stackFrame = mySession.getCurrentStackFrame();
    final XDebuggerEvaluator evaluator = mySession.getDebugProcess().getEvaluator();
    if (stackFrame != null && evaluator != null) {
      evaluator.evaluate(XExpressionImpl.fromText(streamTraceExpression, EvaluationMode.CODE_FRAGMENT), new XEvaluationCallbackBase() {
        @Override
        public void evaluated(@NotNull XValue result) {
          if (result instanceof JavaValue) {
            final Value reference = ((JavaValue)result).getDescriptor().getValue();
            if (reference instanceof ArrayReference) {
              final TracingResult interpretedResult = myResultInterpreter.interpret(chain, (ArrayReference)reference);
              final EvaluationContextImpl context = ((JavaValue)result).getEvaluationContext();
              callback.evaluated(interpretedResult, context);
              return;
            }

            if (reference instanceof ObjectReference) {
              final ReferenceType type = ((ObjectReference)reference).referenceType();
              if (type instanceof ClassType) {
                ClassType classType = (ClassType)type;
                while (classType != null && !"java.lang.Throwable".equals(classType.name())) {
                  classType = classType.superclass();
                }

                if (classType != null) {
                  final String exceptionMessage = tryExtractExceptionMessage((ObjectReference)reference);
                  final String description = "Evaluation failed: " + type.name() + " exception thrown";
                  final String descriptionWithReason = exceptionMessage == null ? description : description + ": " + exceptionMessage;
                  callback.evaluationFailed(streamTraceExpression, descriptionWithReason);
                  return;
                }
              }
            }
          }

          callback.evaluationFailed(streamTraceExpression, "Evaluation failed: unknown type of result value");
        }

        @Override
        public void errorOccurred(@NotNull String errorMessage) {
          callback.compilationFailed(streamTraceExpression, errorMessage);
        }
      }, stackFrame.getSourcePosition());
    }
  }

  @Nullable
  private static String tryExtractExceptionMessage(@NotNull ObjectReference exception) {
    final ReferenceType type = exception.referenceType();
    final Field messageField = type.fieldByName("detailMessage");
    if (messageField == null) return null;
    final Value message = exception.getValue(messageField);
    if (message instanceof StringReference) {
      return ((StringReference)message).value();
    }

    return null;
  }
}
