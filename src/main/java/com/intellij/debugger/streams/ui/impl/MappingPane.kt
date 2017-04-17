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
package com.intellij.debugger.streams.ui.impl

import com.intellij.debugger.streams.ui.LinkedValuesMapping
import com.intellij.debugger.streams.ui.ValueWithPosition
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BasicStroke
import java.awt.BorderLayout
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.JPanel
import javax.swing.SwingConstants

/**
 * @author Vitaliy.Bibaev
 */
class MappingPane(name: String,
                  private val beforeValues: List<ValueWithPosition>,
                  private val mapping: LinkedValuesMapping) : JPanel(BorderLayout()) {
  companion object {
    val SELECTED_LINK_COLOR: JBColor = JBColor.BLUE
    val REGULAR_LINK_COLOR: JBColor = JBColor.GRAY

    val MAX_ANGLE_TO_DRAW_LINK = 4 * Math.PI / 10
    val STROKE = BasicStroke(JBUI.scale(1.toFloat()))
  }

  init {
    add(JBLabel(name, SwingConstants.CENTER), BorderLayout.NORTH)
    add(MyDrawPane(), BorderLayout.CENTER)
  }

  private inner class MyDrawPane : JPanel() {
    override fun paintComponent(g: Graphics?) {
      if (g == null) {
        return
      }

      if (g is Graphics2D) {
        g.stroke = STROKE
      }

      val x1 = x
      val x2 = x + width
      for (value in beforeValues) {
        val position: Int = value.position
        val linkedValues = mapping.getLinkedValues(value) ?: continue
        for (nextValue in linkedValues) {
          if (needToDraw(x1, x2, value, nextValue)) {
            g.color = getLineColor(value, nextValue)
            g.drawLine(x1, position, x2, nextValue.position)
          }
        }
      }
    }

    private fun needToDraw(x1: Int, x2: Int, left: ValueWithPosition, right: ValueWithPosition): Boolean {
      if (left.isVisible && right.isVisible) {
        return true
      }

      return angleToNormal(x1, left.position, x2, right.position) < MAX_ANGLE_TO_DRAW_LINK
    }

    private fun angleToNormal(x1: Int, y1: Int, x2: Int, y2: Int): Double {
      return Math.atan(Math.abs((y2 - y1).toDouble()) / (x2 - x1).toDouble())
    }

    private fun getLineColor(left: ValueWithPosition, right: ValueWithPosition): JBColor {
      if (left.isHighlighted && right.isHighlighted) {
        return SELECTED_LINK_COLOR
      }

      return REGULAR_LINK_COLOR
    }
  }
}
