/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.tdd.completions;

import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.python.pydev.ast.codecompletion.ProposalsComparator;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.proposals.PyCompletionProposal;
import org.python.pydev.parser.PyParser;
import org.python.pydev.shared_core.IMiscConstants;
import org.python.pydev.shared_core.code_completion.IPyCompletionProposal;
import org.python.pydev.shared_core.image.IImageHandle;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * @author fabioz
 *
 */
public abstract class AbstractTddRefactorCompletion extends PyCompletionProposal implements
        ICompletionProposalExtension2 {

    protected IPyEdit edit;

    public AbstractTddRefactorCompletion(IPyEdit edit, String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, int priority) {
        this(edit, replacementString, replacementOffset, replacementLength, cursorPosition, null, null, null, null,
                priority);
    }

    public AbstractTddRefactorCompletion(IPyEdit edit, String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            IContextInformation contextInformation, String additionalProposalInfo, int priority) {
        this(edit, replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString,
                contextInformation, additionalProposalInfo, priority, IPyCompletionProposal.ON_APPLY_DEFAULT, "");
    }

    public AbstractTddRefactorCompletion(IPyEdit edit, String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            IContextInformation contextInformation, String additionalProposalInfo, int priority, int onApplyAction,
            String args) {
        super(replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString,
                contextInformation, additionalProposalInfo, priority, new ProposalsComparator.CompareContext(edit));
        this.edit = edit;
    }

    protected void forceReparseInBaseEditorAnd(PyEdit... others) {
        if (edit != null) {
            PyParser parser = (PyParser) edit.getParser();
            parser.forceReparse(
                    new Tuple<String, Boolean>(IMiscConstants.ANALYSIS_PARSER_OBSERVER_FORCE, true));
        }

        for (IPyEdit e : others) {
            PyParser parser = (PyParser) e.getParser();
            parser.forceReparse(
                    new Tuple<String, Boolean>(IMiscConstants.ANALYSIS_PARSER_OBSERVER_FORCE, true));
        }
    }

}
