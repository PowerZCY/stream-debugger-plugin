package com.intellij.debugger.streams.wrapper;

import org.jetbrains.annotations.NotNull;

/**
 * @author Vitaliy.Bibaev
 */
public class IntermediateStreamCallImpl extends StreamCallImpl implements IntermediateStreamCall {
  private final IntermediateCallType myType;

  public IntermediateStreamCallImpl(@NotNull String name, @NotNull String args, @NotNull IntermediateCallType type) {
    super(name, args, StreamCallType.INTERMEDIATE);
    myType = type;
  }

  @Override
  public boolean hasPrimitiveSource() {
    return ValueType.PRIMITIVE.equals(myType.getTypeBefore());
  }

  @Override
  public boolean hasPrimitiveResult() {
    return ValueType.PRIMITIVE.equals(myType.getTypeAfter());
  }
}
