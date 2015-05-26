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
package org.foxsly.idea.generator;

import com.intellij.codeInsight.NullableNotNullManager;
import com.intellij.codeInsight.generation.ClassMember;
import com.intellij.codeInsight.generation.ConstructorBodyGenerator;
import com.intellij.codeInsight.generation.GenerateConstructorHandler;
import com.intellij.codeInsight.generation.GenerateMembersUtil;
import com.intellij.codeInsight.generation.GenerationInfo;
import com.intellij.codeInsight.generation.PsiElementClassMember;
import com.intellij.codeInsight.generation.PsiGenerationInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JVMElementFactories;
import com.intellij.psi.JVMElementFactory;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.codeStyle.VariableKind;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.foxsly.idea.util.ImportUtils.addStaticImportToClass;

/**
 * @author belcheti
 */
public class GeneratePreconditionsConstructorHandler extends GenerateConstructorHandler {
    private static final Logger LOG = Logger.getInstance("org.foxsly.idea.generator.GeneratePreconditionsConstructorHandler");

    @Override
    @NotNull
    protected List<? extends GenerationInfo> generateMemberPrototypes(PsiClass aClass, ClassMember[] members) throws IncorrectOperationException {
        List<PsiMethod> baseConstructors = new ArrayList<PsiMethod>();
        List<PsiField> fieldsVector = new ArrayList<PsiField>();
        for (ClassMember member : members) {
            PsiElement element = ((PsiElementClassMember) member).getElement();
            if (element instanceof PsiMethod) {
                baseConstructors.add((PsiMethod) element);
            } else {
                fieldsVector.add((PsiField) element);
            }
        }
        PsiField[] fields = fieldsVector.toArray(new PsiField[fieldsVector.size()]);

        if (!baseConstructors.isEmpty()) {
            List<GenerationInfo> constructors = new ArrayList<GenerationInfo>(baseConstructors.size());
            final PsiClass superClass = aClass.getSuperClass();
            assert superClass != null;
            PsiSubstitutor substitutor = TypeConversionUtil.getSuperClassSubstitutor(superClass, aClass, PsiSubstitutor.EMPTY);
            addStaticImportToClass("com.google.common.base.Preconditions", "checkNotNull", aClass);
            for (PsiMethod baseConstructor : baseConstructors) {
                baseConstructor = GenerateMembersUtil.substituteGenericMethod(baseConstructor, substitutor, aClass);
                constructors.add(new PsiGenerationInfo<PsiMethod>(generateConstructorPrototype(aClass, baseConstructor, fields)));
            }
            return filterOutAlreadyInsertedConstructors(aClass, constructors);
        }
        final List<GenerationInfo> constructors = Collections.<GenerationInfo>singletonList(new PsiGenerationInfo<PsiMethod>(generateConstructorPrototype(aClass, null, fields)));
        return filterOutAlreadyInsertedConstructors(aClass, constructors);
    }

    private static List<? extends GenerationInfo> filterOutAlreadyInsertedConstructors(PsiClass aClass, List<? extends GenerationInfo> constructors) {
        boolean alreadyExist = true;
        for (GenerationInfo constructor : constructors) {
            alreadyExist &= aClass.findMethodBySignature((PsiMethod) constructor.getPsiMember(), false) != null;
        }
        if (alreadyExist) {
            return Collections.emptyList();
        }
        return constructors;
    }

    public static PsiMethod generateConstructorPrototype(PsiClass aClass, PsiMethod baseConstructor, PsiField[] fields) throws IncorrectOperationException {
        PsiManager manager = aClass.getManager();
        JVMElementFactory factory = JVMElementFactories.requireFactory(aClass.getLanguage(), aClass.getProject());
        CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(manager.getProject());

        PsiMethod constructor = factory.createConstructor(aClass.getName(), aClass);
        String modifier = PsiUtil.getMaximumModifierForMember(aClass, false);

        PsiUtil.setModifierProperty(constructor, modifier, true);

        if (baseConstructor != null) {
            PsiJavaCodeReferenceElement[] throwRefs = baseConstructor.getThrowsList().getReferenceElements();
            for (PsiJavaCodeReferenceElement ref : throwRefs) {
                constructor.getThrowsList().add(ref);
            }
        }

        boolean isNotEnum = false;
        if (baseConstructor != null) {
            PsiClass superClass = aClass.getSuperClass();
            LOG.assertTrue(superClass != null);
            if (!CommonClassNames.JAVA_LANG_ENUM.equals(superClass.getQualifiedName())) {
                isNotEnum = true;
                if (baseConstructor instanceof PsiCompiledElement) { // to get some parameter names
                    PsiClass dummyClass = JVMElementFactories.requireFactory(baseConstructor.getLanguage(), baseConstructor.getProject()).createClass("Dummy");
                    baseConstructor = (PsiMethod) dummyClass.add(baseConstructor);
                }
                PsiParameter[] params = baseConstructor.getParameterList().getParameters();
                for (PsiParameter param : params) {
                    PsiParameter newParam = factory.createParameter(param.getName(), param.getType(), aClass);
                    GenerateMembersUtil.copyOrReplaceModifierList(param, newParam);
                    constructor.getParameterList().add(newParam);
                }
            }
        }

        JavaCodeStyleManager javaStyle = JavaCodeStyleManager.getInstance(aClass.getProject());

        final PsiMethod dummyConstructor = factory.createConstructor(aClass.getName());
        dummyConstructor.getParameterList().replace(constructor.getParameterList().copy());
        List<PsiParameter> fieldParams = new ArrayList<PsiParameter>();
        for (PsiField field : fields) {
            String fieldName = field.getName();
            String name = javaStyle.variableNameToPropertyName(fieldName, VariableKind.FIELD);
            String parmName = javaStyle.propertyNameToVariableName(name, VariableKind.PARAMETER);
            parmName = javaStyle.suggestUniqueVariableName(parmName, dummyConstructor, true);
            PsiParameter parm = factory.createParameter(parmName, field.getType(), aClass);

            final NullableNotNullManager nullableManager = NullableNotNullManager.getInstance(field.getProject());
            final String notNull = nullableManager.getNotNull(field);
            if (notNull != null) {
                parm.getModifierList().addAfter(factory.createAnnotationFromText("@" + notNull, field), null);
            }

            constructor.getParameterList().add(parm);
            dummyConstructor.getParameterList().add(parm.copy());
            fieldParams.add(parm);
        }

        ConstructorBodyGenerator generator = new JavaConstructorBodyWithPreconditionsNotNullGenerator();
        @NonNls StringBuilder buffer = new StringBuilder();
        generator.start(buffer, constructor.getName(), PsiParameter.EMPTY_ARRAY);
        if (isNotEnum) {
            generator.generateSuperCallIfNeeded(buffer, baseConstructor.getParameterList().getParameters());
        }
        generator.generateFieldInitialization(buffer, fields, fieldParams.toArray(new PsiParameter[fieldParams.size()]));
        generator.finish(buffer);
        PsiMethod stub = factory.createMethodFromText(buffer.toString(), aClass);
        constructor.getBody().replace(stub.getBody());

        constructor = (PsiMethod) codeStyleManager.reformat(constructor);
        return constructor;
    }
}
