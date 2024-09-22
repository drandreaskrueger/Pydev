/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.tdd;

import java.util.List;

import org.eclipse.jface.action.IAction;
import org.python.pydev.ast.refactoring.RefactoringInfo;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;

public abstract class AbstractPyCreateAction {

    public static final int LOCATION_STRATEGY_BEFORE_CURRENT = 0; //before the current method (in the same level)
    public static final int LOCATION_STRATEGY_END = 1; //end of file or end of class
    public static final int LOCATION_STRATEGY_FIRST_METHOD = 2; //In a class as the first method

    protected IPyEdit targetEditor;

    public void setActiveEditor(IAction action, IPyEdit edit) {
        this.targetEditor = edit;
    }

    public abstract ICompletionProposalHandle createProposal(RefactoringInfo refactoringInfo, String actTok,
            int locationStrategy, List<String> parametersAfterCall);

}
