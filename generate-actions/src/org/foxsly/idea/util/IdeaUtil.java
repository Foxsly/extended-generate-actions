/*
* Copyright 2013 Vertafore, Inc. All rights reserved.
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

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;

/**
 * IdeaUtil
 *
 * @author belcheti
 */
public class IdeaUtil {

    public static String getCapitalizedPropertyName(PsiField field) {
        return getCapitalizedPropertyName(field.getName());
    }

    public static String getCapitalizedPropertyName(String fieldName) {
        return fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }

    public static PsiClass getCurrentClass(final Editor editor) {
        if (editor == null) {
            return null;
        }
        PsiManager psiManager = PsiManager.getInstance(editor.getProject());
        VirtualFile vFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
        PsiFile psiFile = psiManager.findFile(vFile);
        if (!(psiFile instanceof PsiJavaFile)) {
            return null;
        }
        PsiJavaFile javaFile = (PsiJavaFile) psiFile;
        PsiElement element = javaFile.findElementAt(editor.getCaretModel().getOffset());
        while (!(element instanceof PsiClass) && element != null) {
            element = element.getParent();
        }
        if (element == null) {
            return null;
        } else {
            return (PsiClass) element;
        }
    }
}
