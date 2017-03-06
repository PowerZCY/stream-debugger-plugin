package com.intellij.debugger.streams.trace.smart.handler;

import com.intellij.debugger.streams.trace.EvaluateExpressionTracerBase;
import com.intellij.debugger.streams.trace.smart.handler.type.ClassTypeImpl;
import com.intellij.debugger.streams.trace.smart.handler.type.GenericType;
import com.intellij.debugger.streams.wrapper.StreamCall;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public class DistinctHandler extends HandlerBase {
  private final PeekTracerHandler myPeekTracer;
  private final HashMapVariableImpl myStoreMapVariable;
  private final HashMapVariableImpl myResolveDirectMapVariable;
  private final HashMapVariableImpl myResolveReverseMapVariable;

  public DistinctHandler(int callNumber, @NotNull String callName) {
    myPeekTracer = new PeekTracerHandler(callNumber, callName);

    final String variablePrefix = callName + callNumber;
    myStoreMapVariable =
      new HashMapVariableImpl(variablePrefix + "Store", GenericType.OBJECT, new ClassTypeImpl("Map<Integer, Object>"), false);
    myResolveDirectMapVariable = new HashMapVariableImpl(variablePrefix + "ResolveDirect", GenericType.INT, GenericType.INT, false);
    myResolveReverseMapVariable = new HashMapVariableImpl(variablePrefix + "ResolveReverse", GenericType.INT, GenericType.INT, false);
  }

  @NotNull
  @Override
  public List<StreamCall> additionalCallsBefore() {
    final List<StreamCall> result = new ArrayList<>(myPeekTracer.additionalCallsBefore());

    final PeekCall storeCall = new PeekCall(createStoreLambda());
    result.add(storeCall);
    return result;
  }

  @NotNull
  @Override
  public List<StreamCall> additionalCallsAfter() {
    final List<StreamCall> result = new ArrayList<>(myPeekTracer.additionalCallsAfter());
    result.add(new PeekCall(createResolveLambda()));
    return result;
  }

  @NotNull
  @Override
  public String prepareResult() {
    final String newLine = EvaluateExpressionTracerBase.LINE_SEPARATOR;
    final String peekPrepare = myPeekTracer.prepareResult();

    final String resolveReverse2Array = myResolveReverseMapVariable.convertToArray("resolveReverse", true, true);
    final String storeMapName = myStoreMapVariable.getName();
    final String afterMapName = myPeekTracer.getAfterMapName();
    final String prepareDirectMap = "{" + newLine +
                                    "  for (final int timeAfter : " + myResolveReverseMapVariable.getName() + ".keySet()) {" + newLine +
                                    "    final Object afterValue = " + afterMapName + ".get(timeAfter);" + newLine +
                                    "    final Map<Integer, Object> valuesBefore = " + storeMapName + ".get(afterValue);" + newLine +
                                    "    for (final int timeBefore : valuesBefore.keySet()) {" + newLine +
                                    "      " + myResolveDirectMapVariable.getName() + ".put(timeBefore, timeAfter);" + newLine +
                                    "    }" + newLine +
                                    "  }" + newLine +
                                    "}" + newLine;

    final String peekResult =
      "final Object peekResult = " + myPeekTracer.getResultExpression() + ";" + EvaluateExpressionTracerBase.LINE_SEPARATOR;
    final String resolveDirect2Array = myResolveDirectMapVariable.convertToArray("resolveDirect", true, true);
    return peekPrepare + prepareDirectMap + resolveDirect2Array + resolveReverse2Array + peekResult;
  }

  @NotNull
  @Override
  public String getResultExpression() {
    return "new Object[] { peekResult, resolveDirect, resolveReverse }";
  }

  @NotNull
  private String createStoreLambda() {
    final String storeMap = myStoreMapVariable.getName();
    return "x -> " + String.format("%s.computeIfAbsent(x, y -> new LinkedHashMap<>()).put(time.get(), x)", storeMap);
  }

  @NotNull
  private String createResolveLambda() {
    final String newLine = EvaluateExpressionTracerBase.LINE_SEPARATOR;
    final String storeMap = myStoreMapVariable.getName();
    final String resolveReverseMap = myResolveReverseMapVariable.getName();

    return "x -> {" + newLine +
           "  final Map<Integer, Object> objects = " + String.format("%s.get(x);", storeMap) + newLine +
           "  for (final int key: objects.keySet()) {" + newLine +
           "    final Object value = objects.get(key);" + newLine +
           "    if (value == x && !" + resolveReverseMap + ".containsKey(key)) {" + newLine +
           "      " + String.format("%s.put(time.get(), key);", resolveReverseMap) + newLine +
           "    }" + newLine +
           "  }" + newLine +
           "}" + newLine;
  }

  @NotNull
  @Override
  protected List<Variable> getVariables() {
    final List<Variable> variables =
      new ArrayList<>(Arrays.asList(myStoreMapVariable, myResolveDirectMapVariable, myResolveReverseMapVariable));
    variables.addAll(myPeekTracer.getVariables());
    return variables;
  }
}
