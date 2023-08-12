/*
 * This file is part of "SAP Commerce Developers Toolset" plugin for Intellij IDEA.
 * Copyright (C) 2019-2023 EPAM Systems <hybrisideaplugin@epam.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.intellij.idea.plugin.hybris.impex.actions

import com.intellij.idea.plugin.hybris.common.utils.HybrisIcons
import com.intellij.idea.plugin.hybris.impex.psi.ImpexFullHeaderParameter
import com.intellij.idea.plugin.hybris.impex.psi.ImpexHeaderLine
import com.intellij.idea.plugin.hybris.impex.psi.ImpexValueGroup
import com.intellij.idea.plugin.hybris.impex.psi.ImpexValueLine
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.startOffset

abstract class AbstractImpExTableColumnMoveAction(private val direction: MoveDirection) : AbstractImpExTableColumnAction() {

    override fun performCommand(project: Project, editor: Editor, element: PsiElement) {
        when (element) {
            is ImpexFullHeaderParameter -> move(editor, element, element, direction)
            is ImpexValueGroup -> element.fullHeaderParameter
                ?.let { move(editor, it, element, direction) }
        }
    }

    private fun move(editor: Editor, headerParameter: ImpexFullHeaderParameter, elementAtCaret: PsiElement, direction: MoveDirection) {
        val headerLine = headerParameter.headerLine ?: return
        val column = headerParameter.columnNumber

        val previousOffset = editor.caretModel.currentCaret.offset
        val previousElementStartOffset = elementAtCaret.startOffset

        var newElementAtCaret: PsiElement? = null
        moveHeaderParam(headerLine, column, direction)
            ?.let { newElementAtCaret = it }
        moveValueGroups(headerLine.valueLines, elementAtCaret, column, direction)
            ?.let { newElementAtCaret = it }

        newElementAtCaret
            ?.let {
                val caretOffsetInText = previousOffset - previousElementStartOffset
                editor.caretModel.currentCaret.moveToOffset(it.startOffset + caretOffsetInText)
            }
    }

    private fun moveHeaderParam(headerLine: ImpexHeaderLine, column: Int, direction: MoveDirection): ImpexFullHeaderParameter? {
        val newColumn = column + direction.step
        val previous = headerLine.fullHeaderParameterList.getOrNull(newColumn)
            ?: return null
        val current = headerLine.fullHeaderParameterList.getOrNull(column)
            ?: return null

        replacePsiElement(previous, current)

        return headerLine.fullHeaderParameterList.getOrNull(newColumn)
    }

    private fun moveValueGroups(valueLines: Collection<ImpexValueLine>, elementAtCaret: PsiElement, column: Int, direction: MoveDirection): PsiElement? {
        val newColumn = column + direction.step
        var newElementAtCaret: PsiElement? = null
        valueLines.forEach {
            val first = it.getValueGroup(newColumn)
            val second = it.getValueGroup(column)

            if (first != null && second != null) {
                replacePsiElement(first, second)

                if (first == elementAtCaret || second == elementAtCaret) {
                    newElementAtCaret = it.getValueGroup(newColumn)
                }
            }
        }
        return newElementAtCaret
    }

    private fun replacePsiElement(first: PsiElement, second: PsiElement) {
        val firstCopy = first.copy()
        val secondCopy = second.copy()

        first.replace(secondCopy)
        second.replace(firstCopy)
    }

    enum class MoveDirection(val step: Int) {
        LEFT(-1), RIGHT(1)
    }
}

class ImpExTableColumnMoveLeftAction : AbstractImpExTableColumnMoveAction(MoveDirection.LEFT) {

    init {
        with(templatePresentation) {
            text = "Move Column Left"
            description = "Move current column left"
            icon = HybrisIcons.TABLE_COLUMN_MOVE_LEFT
        }
    }

    override fun isActionAllowed(project: Project, editor: Editor, element: PsiElement): Boolean {
        val suitableElement = getSuitableElement(element) ?: return false

        return when (suitableElement) {
            is ImpexFullHeaderParameter -> suitableElement.columnNumber > 0
            is ImpexValueGroup -> suitableElement.columnNumber > 0
            else -> false
        }
    }
}

class ImpExTableColumnMoveRightAction : AbstractImpExTableColumnMoveAction(MoveDirection.RIGHT) {

    init {
        with(templatePresentation) {
            text = "Move Column Right"
            description = "Move current column right"
            icon = HybrisIcons.TABLE_COLUMN_MOVE_RIGHT
        }
    }

    override fun isActionAllowed(project: Project, editor: Editor, element: PsiElement): Boolean {
        val suitableElement = getSuitableElement(element) ?: return false

        return when (suitableElement) {
            is ImpexFullHeaderParameter -> isNotLastColumn(suitableElement)

            is ImpexValueGroup -> suitableElement.fullHeaderParameter
                ?.let { isNotLastColumn(it) }
                ?: false

            else -> false
        }
    }

    private fun isNotLastColumn(suitableElement: ImpexFullHeaderParameter): Boolean {
        val totalColumns = suitableElement.headerLine
            ?.fullHeaderParameterList
            ?.size
            ?: return false
        return suitableElement.columnNumber < (totalColumns - 1)
    }
}
