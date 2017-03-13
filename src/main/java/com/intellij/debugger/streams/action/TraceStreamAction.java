package com.intellij.debugger.streams.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

/**
 * @author Vitaliy.Bibaev
 */
public class TraceStreamAction extends AnAction {
  private static class Holder {
    private static final JvmStreamDebuggerActionHandler HANDLER = new JvmStreamDebuggerActionHandler();
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    final Project project = e.getProject();
    if (project != null && Holder.HANDLER.isEnabled(project)) {
      Holder.HANDLER.perform(project);
    }
  }
}
