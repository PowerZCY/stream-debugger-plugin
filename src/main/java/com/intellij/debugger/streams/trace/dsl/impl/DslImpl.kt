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
package com.intellij.debugger.streams.trace.dsl.impl

import com.intellij.debugger.streams.trace.dsl.*

/**
 * @author Vitaliy.Bibaev
 */
class DslImpl(override val statementFactory: StatementFactory) : Dsl {
  override val NULL: Expression = TextExpression("null")

  private val myBody: CompositeCodeBlock = statementFactory.createEmptyCompositeCodeBlock()

  override fun lambda(argName: String, init: LambdaBody.(Expression) -> Unit): Lambda {
    val lambdaBody = statementFactory.createEmptyLambdaBody(argName)
    lambdaBody.init(+argName)
    return statementFactory.createLambda(argName, lambdaBody)
  }

  override fun declare(variable: Variable, isMutable: Boolean): Variable {
    val declaration = statementFactory.createVariableDeclaration(variable, isMutable)
    myBody.addStatement(declaration)
    return declaration.variable
  }

  override fun declare(variable: Variable, init: Expression, isMutable: Boolean): Variable {
    val declaration = statementFactory.createVariableDeclaration(variable, init, isMutable)
    myBody.addStatement(declaration)
    return declaration.variable
  }

  override fun ifBranch(condition: Expression, init: CodeBlock.() -> Unit): IfBranch {
    val codeBlock = statementFactory.createEmptyCodeBlock()
    codeBlock.init()
    val ifStatement = statementFactory.createIfBranch(condition, codeBlock)
    myBody.addStatement(ifStatement)
    return object : IfBranch {
      override fun elseBranch(init: CodeBlock.() -> Unit) {
        val block = statementFactory.createEmptyCodeBlock()
        block.init()
        myBody.addStatement(statementFactory.createElseStatement(block))
      }

      override fun elseIfBranch(condition: Expression, init: CodeBlock.() -> Unit): IfBranch {
        val block = statementFactory.createEmptyCodeBlock()
        block.init()
        val elseIfStatement = statementFactory.createElseIfStatement(condition, block)
        myBody.addStatement(elseIfStatement)
        // TODO: possible bug - wrong toCode implementation
        return this
      }

      override fun toCode(): String = ifStatement.toCode()

    }
  }

  override fun Variable.unaryPlus(): Variable = declare(this, false)

  override fun String.unaryPlus(): TextExpression = TextExpression(this)

  override fun call(receiver: Expression, methodName: String, vararg args: Expression): Expression = receiver.call(methodName, *args)

  override fun forEachLoop(iterateVariable: Variable, collection: Expression, init: ForLoopBody.() -> Unit) {
    myBody.forEachLoop(iterateVariable, collection, init)
  }

  override fun forLoop(initialization: VariableDeclaration, condition: Expression, afterThought: Expression, init: ForLoopBody.() -> Unit) {
    myBody.forLoop(initialization, condition, afterThought, init)
  }

  override fun toCode(): String = myBody.toCode()
}