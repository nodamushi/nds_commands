package com.github.nodamushi.ndscommands

import com.intellij.lang.LanguageCommenters
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.util.TextRange

/**
 * Command to comment out lines.
 * I created this command because `CommentByLineComment` did not like the way it moved lines.
 */
class CommentLine : AnAction() {

  private fun runCommentLine(e: AnActionEvent): Boolean {
    val action = ActionManager.getInstance().getAction("CommentByLineComment") ?: return false
    action.actionPerformed(e)
    return true
  }

  private fun runCommentLine2(e: AnActionEvent) {
    val editor = e.getRequiredData(CommonDataKeys.EDITOR)
    val primary = editor.caretModel.primaryCaret
    val pos = primary.offset
    ApplicationManager.getApplication().runWriteAction {
      CommandProcessor.getInstance().runUndoTransparentAction {
        if (runCommentLine(e)) {
          primary.moveToOffset(pos)
        }
      }
    }
  }

  override fun actionPerformed(e: AnActionEvent) {
    val editor = e.getData(CommonDataKeys.EDITOR) ?: return
    // I don't mind the way `CommentByLineComment` behaves when there is a selection or when there are multiple carets.
    if (editor.selectionModel.hasSelection() || editor.caretModel.allCarets.size > 1) {
      runCommentLine(e)
      return
    }

    val lang = e.getData(CommonDataKeys.LANGUAGE) ?: return runCommentLine2(e)
    val commenters = LanguageCommenters.INSTANCE.forLanguage(lang) ?: return runCommentLine2(e)
    val prefix = commenters.lineCommentPrefix ?: return runCommentLine2(e)

    val caret = editor.caretModel.primaryCaret
    val pos = caret.offset
    val document = editor.document
    val lineStart = document.getLineStartOffset(document.getLineNumber(pos))
    val isCommented = document.getText(TextRange(lineStart, lineStart + prefix.length)) == prefix
    val newPos = pos + if (isCommented) -prefix.length else prefix.length

    ApplicationManager.getApplication().runWriteAction {
      CommandProcessor.getInstance().runUndoTransparentAction {
        if (runCommentLine(e) && lineStart <= pos) {
          caret.moveToOffset(newPos)
        }
      }
    }
  }
}
