/*
 * This file is part of "SAP Commerce Developers Toolset" plugin for IntelliJ IDEA.
 * Copyright (C) 2019-2024 EPAM Systems <hybrisideaplugin@epam.com> and contributors
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

package com.intellij.idea.plugin.hybris.system.cockpitng.psi.reference

import com.intellij.idea.plugin.hybris.psi.util.PsiUtils
import com.intellij.idea.plugin.hybris.system.cockpitng.meta.CngMetaModelAccess
import com.intellij.idea.plugin.hybris.system.cockpitng.psi.reference.result.WidgetSettingResolveResult
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.*
import com.intellij.psi.xml.XmlTag

/**
 * See https://help.sap.com/docs/SAP_COMMERCE/5c9ea0c629214e42b727bf08800d8dfa/76d2195f994a47f593a2732ef99c91d3.html?locale=en-US&q=socket
 */
class CngWidgetSettingReference(element: PsiElement) : PsiReferenceBase.Poly<PsiElement>(element, true), PsiPolyVariantReference {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> = CachedValuesManager.getManager(element.project)
        .getParameterizedCachedValue(element, CACHE_KEY, provider, false, this)
        .let { PsiUtils.getValidResults(it) }

    companion object {
        val CACHE_KEY = Key.create<ParameterizedCachedValue<Array<ResolveResult>, CngWidgetSettingReference>>("HYBRIS_CNGWIDGETSETTINGREFERENCE")

        private val provider = ParameterizedCachedValueProvider<Array<ResolveResult>, CngWidgetSettingReference> { ref ->
            val element = ref.element
            val lookingForName = ref.value
            val project = element.project
            val metaModel = CngMetaModelAccess.getInstance(project).getMetaModel()

            val widgetDefinitionId = element.parentsOfType<XmlTag>()
                .firstOrNull { it.localName == "widget" }
                ?.getAttributeValue("widgetDefinitionId")

            val result = if (widgetDefinitionId == null) emptyArray()
            else metaModel.widgetDefinitions[widgetDefinitionId]
                ?.settings
                ?.get(lookingForName)
                ?.let { PsiUtils.getValidResults(arrayOf(WidgetSettingResolveResult(it))) }
                ?: emptyArray()

            CachedValueProvider.Result.create(
                result,
                metaModel, PsiModificationTracker.MODIFICATION_COUNT
            )
        }
    }
}
