package org.jetbrains.plugins.scala.lang.parser.parsing.expressions{
/**
* @author Ilya Sergey
*/
import com.intellij.lang.PsiBuilder, org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.parser.ScalaElementTypes
import org.jetbrains.plugins.scala.lang.lexer.ScalaElementType
import org.jetbrains.plugins.scala.lang.parser.bnf.BNF
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.tree.IElementType
import org.jetbrains.plugins.scala.lang.parser.util.ParserUtils
import org.jetbrains.plugins.scala.lang.parser.parsing.types._
import org.jetbrains.plugins.scala.lang.parser.parsing.patterns._
import org.jetbrains.plugins.scala.lang.parser.parsing.top.template._

  object BlockExpr {
  /*
  Block expression
  Default grammar
  BlockExpr ::= �{� CaseClauses �}�
                | �{� Block �}�
  */
    def parse(builder : PsiBuilder) : ScalaElementType = {
      val blockExprMarker = builder.mark()

      if (builder.getTokenType.eq(ScalaTokenTypes.tLBRACE)) {
        ParserUtils.eatElement(builder, ScalaTokenTypes.tLBRACE)
        ParserUtils.rollForward(builder)
        if (builder.getTokenType.eq(ScalaTokenTypes.tRBRACE)){
          ParserUtils.eatElement(builder, ScalaTokenTypes.tRBRACE)
          blockExprMarker.done(ScalaElementTypes.BLOCK_EXPR)
          ScalaElementTypes.BLOCK_EXPR
        } else {
          /*  �{� Block �}� */
          var result = Block.parse(builder, true)
          if (result.equals(ScalaElementTypes.BLOCK)) {
            ParserUtils.rollForward(builder)
            if (builder.getTokenType.eq(ScalaTokenTypes.tRBRACE)){
              ParserUtils.eatElement(builder, ScalaTokenTypes.tRBRACE)
              blockExprMarker.done(ScalaElementTypes.BLOCK_EXPR)
              ScalaElementTypes.BLOCK_EXPR
            } else {
              builder.error("} expected")
              blockExprMarker.done(ScalaElementTypes.BLOCK_EXPR)
              ScalaElementTypes.BLOCK_EXPR
            }
          } else {
            /* �{� CaseClauses �}� */
            result = CaseClauses.parse(builder)
            if (result.equals(ScalaElementTypes.CASE_CLAUSES)) {
              ParserUtils.rollForward(builder)
              if (builder.getTokenType.eq(ScalaTokenTypes.tRBRACE)){
                ParserUtils.eatElement(builder, ScalaTokenTypes.tRBRACE)
                blockExprMarker.done(ScalaElementTypes.BLOCK_EXPR)
                ScalaElementTypes.BLOCK_EXPR
              } else {
                builder.error("} expected")
                blockExprMarker.done(ScalaElementTypes.BLOCK_EXPR)
                ScalaElementTypes.BLOCK_EXPR
              }
            } else{
            builder.error("Wrong inner block statement")
            blockExprMarker.done(ScalaElementTypes.BLOCK_EXPR)
            ScalaElementTypes.BLOCK_EXPR
            }
          }
        }
      } else {
         blockExprMarker.rollbackTo()
         ScalaElementTypes.WRONGWAY
      }
    }
  }

  object Block {
  /*
  Block
  Default grammar
  Block ::= {BlockStat StatementSeparator} [ResultExpr]
  */
    def parse(builder : PsiBuilder, withBrace: Boolean) : ScalaElementType = {

      def rollForward: Boolean = {
        var flag1 = true
        var flag2 = false
        while ( !builder.eof() && flag1){
           builder.getTokenType match{
             case ScalaTokenTypes.tLINE_TERMINATOR
                | ScalaTokenTypes.tSEMICOLON => {
                  ParserUtils.eatElement(builder, builder.getTokenType())
                  flag2 = true
                }
             case _ => flag1 = false
           }
        }
        flag2
      }

      var rollbackMarker = builder.mark()
      var result = ScalaElementTypes.BLOCK
      var flag = false
      var flag2 = true
      rollForward
      do {
        result = BlockStat.parse(builder)
        if (flag2 && result.equals(ScalaElementTypes.BLOCK_STAT)) {
          rollbackMarker.drop()
          flag2 = rollForward
          rollbackMarker = builder.mark()
          builder.getTokenType match {
            case ScalaTokenTypes.tRBRACE if withBrace => {
              rollbackMarker.drop()
              result = ScalaElementTypes.BLOCK
              flag = false
            }
            case _ => {
              flag = true
            }
          }
        } else if (!withBrace) {
          flag = false
          rollbackMarker.rollbackTo()
          result = ScalaElementTypes.BLOCK
        } else {
          flag = false
          rollbackMarker.rollbackTo()
          result = ScalaElementTypes.WRONGWAY
        }
      } while (flag)
      result
    }
  }

  /**
  BLOCK STATEMENTS
  BlockStat ::= Import
            | [implicit] Def
            | {LocalModifier} TmplDef
            | Expr1
            |
  **/
  object BlockStat {

    def parse(builder : PsiBuilder) : ScalaElementType = {
      val blockStatMarker = builder.mark()

      var rbMarker = builder.mark() 
      var first = builder.getTokenType
      builder.advanceLexer
      var second = builder.getTokenType
      rbMarker.rollbackTo()
      if (ScalaTokenTypes.kCASE.equals(first) &&
          (ScalaTokenTypes.kCLASS.equals(second) || ScalaTokenTypes.kCLASS.equals(second))){
        Def.parseBody(builder)
        blockStatMarker.drop
        ScalaElementTypes.BLOCK_STAT
      } else if (BNF.firstDef.contains(builder.getTokenType) &&
                !ScalaTokenTypes.kCASE.equals(builder.getTokenType)) {
        Def.parseBody(builder)
        blockStatMarker.drop
        ScalaElementTypes.BLOCK_STAT
      } else {
        var result = CompositeExpr.parse(builder)
        if (!(result == ScalaElementTypes.WRONGWAY)) {
          //blockStatMarker.done(ScalaElementTypes.BLOCK_STAT)
          blockStatMarker.drop
          ScalaElementTypes.BLOCK_STAT
        }
        else {
          blockStatMarker.rollbackTo
          ScalaElementTypes.WRONGWAY
        }
      }
    }

  }





}

