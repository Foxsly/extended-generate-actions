/*
* Copyright 2014 Vertafore, Inc. All rights reserved.
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
package org.foxsly.idea.generator.action;

import com.intellij.codeInsight.generation.actions.BaseGenerateAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilBase;
import com.siyeh.ig.psiutils.ImportUtils;
import org.foxsly.idea.generator.GeneratePreconditionsConstructorHandler;

/**
 * @author belcheti
 */
public class GeneratePreconditionsConstructorAction extends BaseGenerateAction {
    public GeneratePreconditionsConstructorAction() {
        super(new GeneratePreconditionsConstructorHandler());
    }

    @Override
    protected boolean isValidForClass(final PsiClass targetClass) {
        return super.isValidForClass(targetClass) && !(targetClass instanceof PsiAnonymousClass);
    }

    @Override
    public void actionPerformedImpl(Project project, Editor editor) {
        super.actionPerformedImpl(project, editor);
        final PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
        PsiClass psiClass = PsiTreeUtil.getChildOfType(psiFile, PsiClass.class);
        final PsiElement context = psiClass.getFields()[0];
        WriteCommandAction.runWriteCommandAction(project, new Runnable() {
            @Override
            public void run() {
                ImportUtils.addStaticImport("com.google.common.base.Preconditions", "checkNotNull", context);
            }
        });
    }
}
