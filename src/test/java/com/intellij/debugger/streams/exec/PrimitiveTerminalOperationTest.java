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
package com.intellij.debugger.streams.exec;

import com.intellij.execution.process.ProcessOutputTypes;
import com.sun.jdi.PrimitiveValue;
import com.sun.jdi.Value;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vitaliy.Bibaev
 */
public class PrimitiveTerminalOperationTest extends TraceExecutionTestCase {
  public void testPrimitiveResultBoolean() {
    doTest(false);
  }

  public void testPrimitiveResultInt() {
    doTest(false);
  }

  public void testPrimitiveResultDouble() {
    doTest(false);
  }

  public void testPrimitiveResultLong() {
    doTest(false);
  }

  @Override
  protected void handleResultValue(@Nullable Value result, boolean mustBeNull) {
    assertFalse(mustBeNull);
    assertNotNull(result);
    assertInstanceOf(result, PrimitiveValue.class);
    println("Result type:" + result.type().name(), ProcessOutputTypes.SYSTEM);
    println("value = " + result.toString(), ProcessOutputTypes.SYSTEM);
  }
}
