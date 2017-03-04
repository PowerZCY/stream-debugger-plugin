package com.intellij.debugger.streams.trace.smart.resolve.impl;

import com.intellij.debugger.streams.trace.smart.TraceElement;
import com.intellij.debugger.streams.trace.smart.TraceElementImpl;
import com.intellij.debugger.streams.trace.smart.resolve.TraceInfo;
import com.intellij.debugger.streams.trace.smart.resolve.TraceResolver;
import com.intellij.debugger.streams.trace.smart.resolve.ex.UnexpectedValueException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.IntegerValue;
import com.sun.jdi.Value;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Vitaliy.Bibaev
 */
public class SimplePeekResolver implements TraceResolver {
  @NotNull
  @Override
  public TraceInfo resolve(@NotNull Value value) {
    if (value instanceof ArrayReference) {
      final ArrayReference trace = (ArrayReference)value;
      final Value before = trace.getValue(0);
      // todo: do something with 'after' trace array
      if (before instanceof ArrayReference) {
        return new ValuesOrderInfo(resolveTrace((ArrayReference)before));
      }
    }

    throw new UnexpectedValueException("peek operation trace is wrong format");
  }

  @NotNull
  private Map<Integer, TraceElement> resolveTrace(@NotNull ArrayReference mapArray) {
    final Value keys = mapArray.getValue(0);
    final Value values = mapArray.getValue(1);
    if (keys instanceof ArrayReference && values instanceof ArrayReference) {
      return resolveTrace((ArrayReference)keys, (ArrayReference)values);
    }

    throw new UnexpectedValueException("key and values must be store in arrays in peek resolver");
  }

  @NotNull
  private Map<Integer, TraceElement> resolveTrace(@NotNull ArrayReference keysArray, @NotNull ArrayReference valuesArray) {
    final LinkedHashMap<Integer, TraceElement> result = new LinkedHashMap<>();
    final List<Value> keyMirrors = keysArray.getValues();
    final List<Value> valueMirrors = valuesArray.getValues();
    if (keyMirrors.size() == valueMirrors.size()) {
      for (int i = 0, size = keyMirrors.size(); i < size; i++) {
        final TraceElement element = resolveTraceElement(keyMirrors.get(i), valueMirrors.get(i));
        result.put(element.getTime(), element);
      }

      return result;
    }

    throw new UnexpectedValueException("keys and values arrays should be with the same sizes");
  }

  @NotNull
  private TraceElement resolveTraceElement(@NotNull Value key, @NotNull Value value) {
    if (key instanceof IntegerValue) {
      return new TraceElementImpl(((IntegerValue)key).value(), value);
    }

    throw new UnexpectedValueException("key must be an integer value");
  }
}
