package com.github.nodamushi.ndscommands

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager


/**
 * An IntelliJ IDEA action to reformat the current line or selected range of text.
 */
class ReformLine : AnAction() {

  /**
   * Reformat a single line based on the caret position.
   *
   * @param document The document to modify.
   * @param psiFile The PSI file associated with the document.
   * @param manager The CodeStyleManager instance.
   * @param caret The caret object to find the current line.
   */
  private fun singleLine(document: Document, psiFile: PsiFile, manager: CodeStyleManager, caret: Caret) {
    val lineNumber = document.getLineNumber(caret.offset)
    val lineStartOffset = document.getLineStartOffset(lineNumber)
    val lineEndOffset = document.getLineEndOffset(lineNumber)
    val lineText = document.getText(TextRange(lineStartOffset, lineEndOffset))

    // If the line is blank or contains only whitespace
    if (lineText.isBlank()) {
      val tempText = "temp"
      document.replaceString(lineStartOffset, lineEndOffset, tempText)
      manager.reformatText(psiFile, lineStartOffset, lineStartOffset + tempText.length)
      val end = document.getLineEndOffset(lineNumber)
      document.replaceString(end - tempText.length, end, "")
    } else {
      // For a regular line
      manager.reformatText(psiFile, lineStartOffset, lineEndOffset + 1)
    }
  }

  /**
   * The main action performed method.
   *
   * @param e The action event object.
   */
  override fun actionPerformed(e: AnActionEvent) {
    val editor = e.getData(CommonDataKeys.EDITOR) ?: return
    val project = e.getData(CommonDataKeys.PROJECT) ?: return
    val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return

    ApplicationManager.getApplication().runWriteAction {
      CommandProcessor.getInstance().runUndoTransparentAction {
        val selectionModel = editor.selectionModel
        val caretModel = editor.caretModel
        val manager = CodeStyleManager.getInstance(project)
        val document = editor.document

        // If there is a selection
        if (selectionModel.hasSelection()) {
          val start = document.getLineNumber(selectionModel.selectionStart)
          val end = document.getLineNumber(selectionModel.selectionEnd)
          val startOffset = document.getLineStartOffset(start)
          val endOffset = document.getLineEndOffset(end)
          manager.reformatText(psiFile, startOffset, endOffset + 1)
        } else if (caretModel.caretCount == 1) {
          // If there is a single caret
          singleLine(document, psiFile, manager, caretModel.primaryCaret)
        } else {
          // If there are multiple carets
          for (c in caretModel.allCarets) {
            singleLine(document, psiFile, manager, c)
          }
        }
      }
    }
  }
}
