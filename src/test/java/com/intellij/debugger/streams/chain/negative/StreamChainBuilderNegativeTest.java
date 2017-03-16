package com.intellij.debugger.streams.chain.negative;

import com.intellij.debugger.streams.chain.StreamChainBuilderTestCase;
import com.intellij.debugger.streams.wrapper.StreamChain;
import com.intellij.debugger.streams.wrapper.impl.StreamChainBuilder;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author Vitaliy.Bibaev
 */
public class StreamChainBuilderNegativeTest extends StreamChainBuilderTestCase {

  public void testFakeStream() {
    doTest();
  }

  public void testWithoutTerminalOperation() {
    doTest();
  }

  public void testNoBreakpoint() {
    doTest();
  }

  public void testBreakpointOnMethod() {
    doTest();
  }

  public void testBreakpointOnIfCondition() {
    doTest();
  }

  public void testBreakpointOnNewScope() {
    doTest();
  }

  public void testBreakpointOnElseBranch() {
    doTest();
  }

  private void doTest() {
    final PsiElement element = configureAndGetElementAtCaret();
    assertNotNull(element);
    final StreamChain chain = StreamChainBuilder.tryBuildChain(element);
    assertNull(chain);
  }

  @NotNull
  @Override
  protected String getRelativeTestPath() {
    return "chain/negative";
  }
}
