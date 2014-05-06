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
package org.foxsly.idea.generator;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReferenceParameterList;
import com.intellij.psi.PsiType;
import org.foxsly.idea.util.IdeaUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * GenerateCollectionSettersHandler
 *
 * @author belcheti
 */
public class GenerateCollectionSettersHandler extends EditorWriteActionHandler {

    @Override
    public void executeWriteAction(Editor editor, DataContext dataContext) {
        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(editor.getProject());
        PsiElementFactory psiElementFactory = psiFacade.getElementFactory();
        PsiClass clazz = IdeaUtil.getCurrentClass(editor);

        List<PsiMethod> clazzMethods = Arrays.asList(clazz.getMethods());
        List<String> clazzMethodNames = new ArrayList<String>(clazzMethods.size());
        for (PsiMethod clazzMethod : clazzMethods) {
            clazzMethodNames.add(clazzMethod.getName());
        }
        for (PsiField field : clazz.getFields()) {
            String methodNameSuffix = IdeaUtil.getCapitalizedPropertyName(field);
            String fieldType = field.getType().getCanonicalText();
            String fieldName = field.getName();

            PsiType genericType = doSomething(field);
            if (genericType != null) {
                String genericTypeString = genericType.getCanonicalText();
                String getterMethodName = "get" + methodNameSuffix;
                StringBuilder sbGetter = new StringBuilder();
                sbGetter.append("public ").append(fieldType).append(" ").append(getterMethodName).append("() {\n");
                if (fieldType.contains("Set")) {
                    sbGetter.append("\treturn ImmutableSet.copyOf(this.").append(fieldName).append(");\n").append("}");
                } else {
                    sbGetter.append("\treturn ImmutableList.copyOf(this.").append(fieldName).append(");\n").append("}");

                }
                if (!clazzMethodNames.contains(getterMethodName)) {
                    clazz.add(psiElementFactory.createMethodFromText(sbGetter.toString(), clazz));
                }

                String addMethodName = "add" + methodNameSuffix;
                StringBuilder sbAdd = new StringBuilder();
                sbAdd.append("public void ").append(addMethodName).append("(").append(genericTypeString).append(" ").append(fieldName).append(") {\n");
                sbAdd.append("\tthis.").append(fieldName).append(".add(").append(fieldName).append(");\n").append("}");

                String addAllMethodName = "addAll" + methodNameSuffix + "s";
                StringBuilder sbAddAll = new StringBuilder();
                sbAddAll.append("public void ").append(addAllMethodName).append("(Collection<").append(genericTypeString).append("> ").append(fieldName).append("s) {\n");
                sbAddAll.append("\tthis.").append(fieldName).append(".addAll(").append(fieldName).append("s);\n").append("}");

                boolean isFinalMethod = true;
                try {
                    isFinalMethod = field.getModifierList().hasModifierProperty("final");
                } catch (Exception e) {
                    /* swallow */
                }
                if (!isFinalMethod && !clazzMethodNames.contains(addMethodName)) {
                    //clazz.add(psiElementFactory.createMethodFromText(field.getType().getDeepComponentType().getCanonicalText(), clazz));
                    clazz.add(psiElementFactory.createMethodFromText(sbAdd.toString(), clazz));
                }
                if (!isFinalMethod && !clazzMethodNames.contains(addAllMethodName)) {
                    clazz.add(psiElementFactory.createMethodFromText(sbAddAll.toString(), clazz));
                }
            }
        }
    }

    private PsiType doSomething(PsiField field) {

        PsiJavaCodeReferenceElement elementType = (PsiJavaCodeReferenceElement) field.getTypeElement().getFirstChild();
        PsiReferenceParameterList genericList = (PsiReferenceParameterList) elementType.getLastChild();
        PsiType genericType = null;
        if (genericList.getTypeArguments().length > 0) {
            genericType = genericList.getTypeArguments()[0];
        }

        return genericType;
    }

}
