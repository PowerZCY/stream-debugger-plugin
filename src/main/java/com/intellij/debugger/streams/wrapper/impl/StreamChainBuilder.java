package com.intellij.debugger.streams.wrapper.impl;

import com.intellij.debugger.streams.trace.smart.handler.type.GenericType;
import com.intellij.debugger.streams.wrapper.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Vitaliy.Bibaev
 */
public class StreamChainBuilder {
  // TODO: producer - any method, which returns Stream object. Pay attention - need to be sure, that this method is repeatable
  private static final Set<String> SUPPORTED_PRODUCERS = StreamEx.of("stream", "iterate", "generate", "range", "rangeClosed",
                                                                     "of", "concat", "empty").toSet();
  private static final Set<String> SUPPORTED_INTERMEDIATE =
    StreamEx.of("limit", "flatMap", "flatMapToInt", "flatMapToLong", "flatMapToDouble", "distinct", "map",
                "filter", "mapToInt", "mapToLong", "mapToDouble", "sorted", "boxed", "peek", "onClose").toSet();

  // TODO: termination - is any method which returns regular object (not subclass of Stream)
  private static final Set<String> SUPPORTED_TERMINATION =
    StreamEx.of("collect", "sum", "reduce", "toArray", "anyMatch", "allMatch", "max", "min", "findAny", "close", "count", "forEach",
                "average", "summaryStatistics", "forEachOrdered", "findFirst", "noneMatch", "spliterator", "iterator").toSet();

  private static final ThreadLocal<PsiMethodCallExpression[]> SEARCH_RESULT = new ThreadLocal<>();
  private static final PsiElementVisitor STREAM_CALL_VISITOR = new JavaRecursiveElementWalkingVisitor() {
    @Override
    public void visitLambdaExpression(PsiLambdaExpression expression) {
      // ignore lambda calls if stream call was found
      if (SEARCH_RESULT.get()[0] == null) {
        super.visitLambdaExpression(expression);
      }
    }

    @Override
    public void visitCodeBlock(PsiCodeBlock block) {
      // ignore nested blocks
    }

    @Override
    public void visitMethodCallExpression(PsiMethodCallExpression expression) {
      super.visitMethodCallExpression(expression);
      final PsiMethod method = expression.resolveMethod();
      if (method != null) {
        final PsiType type = method.getReturnType();
        if (type != null && InheritanceUtil.isInheritor(type, CommonClassNames.JAVA_UTIL_STREAM_BASE_STREAM)) {
          SEARCH_RESULT.get()[0] = expression;
        }
      }
    }
  };

  static {
    SEARCH_RESULT.set(new PsiMethodCallExpression[1]);
  }

  @Nullable
  public static StreamChain tryBuildChain(@NotNull PsiElement elementAtCursor) {
    final PsiMethodCallExpression call = tryFindStreamCall(elementAtCursor);
    if (call != null) {
      final List<IntermediateStreamCall> intermediateStreamCalls = new ArrayList<>();
      final String name = resolveProducerCallName(call);
      final String args = resolveArguments(call);
      GenericType prevCallType = resolveType(call);
      if (prevCallType == null) return null;
      final ProducerStreamCall producer = new ProducerStreamCallImpl(name, args, prevCallType);
      PsiElement current = call.getParent();
      while (current != null) {
        if (current instanceof PsiMethodCallExpression) {
          final PsiMethodCallExpression methodCall = (PsiMethodCallExpression)current;
          final String callName = resolveMethodName(methodCall);
          final String callArgs = resolveArguments(methodCall);
          if (callName == null) return null;
          final StreamCallType type = getType(callName);
          if (StreamCallType.INTERMEDIATE.equals(type)) {
            final GenericType currentType = resolveType(call);
            if (currentType == null) return null;
            final IntermediateStreamCall streamCall = new IntermediateStreamCallImpl(callName, callArgs, prevCallType, currentType);
            intermediateStreamCalls.add(streamCall);
            prevCallType = currentType;
          }
          else if (StreamCallType.TERMINATOR.equals(type)) {
            final TerminatorStreamCallImpl terminator = new TerminatorStreamCallImpl(callName, callArgs, prevCallType);
            return new StreamChainImpl(producer, intermediateStreamCalls, terminator);
          }
          else {
            throw new RuntimeException("wrong operation type!");
          }
        }

        current = current.getParent();
      }
    }

    return null;
  }

  @Nullable
  private static GenericType resolveType(@NotNull PsiMethodCallExpression call) {
    return ApplicationManager.getApplication().runReadAction((Computable<GenericType>)() -> {
      final PsiMethod method = call.resolveMethod();
      if (method != null) {
        final PsiType returnType = method.getReturnType();
        if (returnType != null) {
          if (InheritanceUtil.isInheritor(returnType, CommonClassNames.JAVA_UTIL_STREAM_INT_STREAM)) {
            return GenericType.INT;
          }
          if (InheritanceUtil.isInheritor(returnType, CommonClassNames.JAVA_UTIL_STREAM_LONG_STREAM)) {
            return GenericType.LONG;
          }
          if (InheritanceUtil.isInheritor(returnType, CommonClassNames.JAVA_UTIL_STREAM_DOUBLE_STREAM)) {
            return GenericType.DOUBLE;
          }

          return GenericType.OBJECT;
        }
      }
      return null;
    });
  }

  public static boolean checkStreamExists(@NotNull PsiElement elementAtCursor) {
    return tryFindStreamCall(elementAtCursor) != null;
  }

  @Nullable
  private static PsiMethodCallExpression tryFindStreamCall(@NotNull PsiElement startElement) {
    PsiElement current = startElement;

    // find nearest node with children.
    while (current != null && current.getChildren().length == 0) {
      current = current.getNextSibling();
    }

    final PsiElement candidate = current;
    SEARCH_RESULT.get()[0] = null;
    if (candidate != null) {
      // find the deepest call with stream as result
      candidate.accept(STREAM_CALL_VISITOR);
    }

    return SEARCH_RESULT.get()[0];
  }

  @Nullable
  private static String resolveMethodName(@NotNull PsiMethodCallExpression methodCall) {
    return ApplicationManager.getApplication().runReadAction((Computable<String>)() -> {
      final PsiMethod method = methodCall.resolveMethod();
      return method == null ? null : method.getName();
    });
  }

  @NotNull
  private static String resolveProducerCallName(@NotNull PsiMethodCallExpression methodCall) {
    return ApplicationManager.getApplication().runReadAction((Computable<String>)() -> methodCall.getChildren()[0].getText());
  }

  @NotNull
  private static String resolveArguments(@NotNull PsiMethodCallExpression methodCall) {
    return ApplicationManager.getApplication().runReadAction((Computable<String>)() -> methodCall.getArgumentList().getText());
  }

  private static StreamCallType getType(@NotNull String name) {
    if (SUPPORTED_INTERMEDIATE.contains(name)) {
      return StreamCallType.INTERMEDIATE;
    }
    if (SUPPORTED_PRODUCERS.contains(name)) {
      return StreamCallType.PRODUCER;
    }

    return SUPPORTED_TERMINATION.contains(name)
           ? StreamCallType.TERMINATOR
           : StreamCallType.UNKNOWN;
  }
}
