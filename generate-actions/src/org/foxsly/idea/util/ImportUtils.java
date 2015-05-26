/*
* Copyright 2015 Vertafore, Inc. All rights reserved.
*
* Disclaimers:
* This software is provided "as is," without warranty of any kind, express or
* implied, including but not limited to the warranties of merchantability,
* fitness for a particular purpose and non-infringement. This source code
* should not be relied upon as the sole basis for solving a problem whose
* incorrect solution could result in injury to person or property. In no
* event shall the author or contributors be held liable for any damages
* arising in any way from the use of this software. The entire risk as to the
* results and performance of this source code is assumed by the user.
*
* Permission is granted to use this software for internal use only, subject
* to the following restrictions:
* 1. This source code MUST retain the above copyright notice, disclaimer,
* and this list of conditions.
* 2. This source code may be used ONLY within the scope of the original
* agreement under which this source code was provided and may not be
* distributed to any third party without the express written consent of
* Vertafore, Inc.
* 3. This source code along with all obligations and rights under the
* original License Agreement may not be assigned to any third party
* without the expressed written consent of Vertafore, Inc., except that
* assignment may be made to a successor to the business or
* substantially all of its assets. All parties bind their successors,
* executors, administrators, and assignees to all covenants of this
* Agreement.
*
* All advertising materials mentioning features or use of this software must
* display the following acknowledgment:
*
* Trademark Disclaimer:
* All patent, copyright, trademark and other intellectual property rights
* included in the source code are owned exclusively by Vertafore, Inc.
*/
package org.foxsly.idea.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiImportStatementBase;
import com.intellij.psi.PsiImportStaticStatement;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.codeStyle.JavaCodeStyleSettingsFacade;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Most code stolen from ImportUtils.java
 *
 * @author belcheti
 */
public class ImportUtils {
    public static boolean addStaticImportToClass(@NotNull String qualifierClass, @NonNls @NotNull String memberName, @NotNull PsiClass context) {
        final PsiFile psiFile = context.getContainingFile();
        if (!(psiFile instanceof PsiJavaFile)) {
            return false;
        }
        final PsiJavaFile javaFile = (PsiJavaFile) psiFile;
        final PsiImportList importList = javaFile.getImportList();
        if (importList == null) {
            return false;
        }
        final PsiImportStatementBase existingImportStatement = importList.findSingleImportStatement(memberName);
        if (existingImportStatement != null) {
            if (existingImportStatement instanceof PsiImportStaticStatement) {
                final PsiImportStaticStatement importStaticStatement = (PsiImportStaticStatement) existingImportStatement;
                if (!memberName.equals(importStaticStatement.getReferenceName())) {
                    return false;
                }
                final PsiClass targetClass = importStaticStatement.resolveTargetClass();
                return targetClass != null && qualifierClass.equals(targetClass.getQualifiedName());
            }
            return false;
        }
        final PsiImportStaticStatement onDemandImportStatement = findOnDemandImportStaticStatement(importList, qualifierClass);
        if (onDemandImportStatement != null && !com.siyeh.ig.psiutils.ImportUtils.hasOnDemandImportConflict(qualifierClass + '.' + memberName, javaFile)) {
            return true;
        }
        final Project project = context.getProject();
        final GlobalSearchScope scope = context.getResolveScope();
        final JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
        final PsiClass aClass = psiFacade.findClass(qualifierClass, scope);
        if (aClass == null) {
            return false;
        }
        final String qualifiedName = aClass.getQualifiedName();
        if (qualifiedName == null) {
            return false;
        }
        final List<PsiImportStaticStatement> imports = getMatchingImports(importList, qualifiedName);
        final int onDemandCount = JavaCodeStyleSettingsFacade.getInstance(project).getNamesCountToUseImportOnDemand();
        final PsiElementFactory elementFactory = psiFacade.getElementFactory();
        if (imports.size() + 1 < onDemandCount) {
            importList.add(elementFactory.createImportStaticStatement(aClass, memberName));
        } else {
            for (PsiImportStaticStatement importStatement : imports) {
                importStatement.delete();
            }
            importList.add(elementFactory.createImportStaticStatement(aClass, "*"));
        }
        return true;
    }

    @Nullable
    private static PsiImportStaticStatement findOnDemandImportStaticStatement(PsiImportList importList, String qualifierClass) {
        final PsiImportStaticStatement[] importStaticStatements = importList.getImportStaticStatements();
        for (PsiImportStaticStatement importStaticStatement : importStaticStatements) {
            if (!importStaticStatement.isOnDemand()) {
                continue;
            }
            final PsiJavaCodeReferenceElement importReference = importStaticStatement.getImportReference();
            if (importReference == null) {
                continue;
            }
            final String text = importReference.getText();
            if (qualifierClass.equals(text)) {
                return importStaticStatement;
            }
        }
        return null;
    }

    private static List<PsiImportStaticStatement> getMatchingImports(@NotNull PsiImportList importList, @NotNull String className) {
        final List<PsiImportStaticStatement> imports = new ArrayList<PsiImportStaticStatement>();
        for (PsiImportStaticStatement staticStatement : importList.getImportStaticStatements()) {
            final PsiClass psiClass = staticStatement.resolveTargetClass();
            if (psiClass == null) {
                continue;
            }
            if (!className.equals(psiClass.getQualifiedName())) {
                continue;
            }
            imports.add(staticStatement);
        }
        return imports;
    }

}
