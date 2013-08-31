package org.jetbrains.plugins.scala
package lang
package psi
package stubs

import api.base.types.ScTypeElement
import api.statements.ScTypeAlias
import com.intellij.psi.stubs.NamedStub

/**
 * User: Alexander Podkhalyuzin
 * Date: 18.10.2008
 */

trait ScTypeAliasStub extends NamedStub[ScTypeAlias] with ScMemberOrLocal {
  def isDeclaration: Boolean

  def getTypeElementText: String

  def getTypeElement: ScTypeElement

  def getLowerBoundElementText: String

  def getLowerBoundTypeElement: ScTypeElement

  def getUpperBoundElementText: String

  def getUpperBoundTypeElement: ScTypeElement

  def isStableQualifier: Boolean
}