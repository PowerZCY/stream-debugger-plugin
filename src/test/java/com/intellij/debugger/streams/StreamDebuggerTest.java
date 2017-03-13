package com.intellij.debugger.streams;

import com.intellij.debugger.DebuggerTestCase;
import com.intellij.debugger.impl.OutputChecker;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.ex.PathManagerEx;

import java.io.File;

/**
 * @author Vitaliy.Bibaev
 */
public class StreamDebuggerTest extends DebuggerTestCase {
  public static final String TINY_APP = PathManagerEx.getTestDataPath() + File.separator + "stream-debugger" + File.separator + "tinyApp";

  public void testSimple() {
  }

  @Override
  protected OutputChecker initOutputChecker() {
    return new OutputChecker(getTestAppPath(), getAppOutputPath());
  }

  @Override
  protected String getTestAppPath() {
    final String path = PathManager.getBinPath();
    return TINY_APP;
  }
}
