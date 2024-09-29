/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.marker_quick_fixes;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.core.IAssistProps;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.proposals.CompletionProposalFactory;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.code_completion.IPyCompletionProposal;
import org.python.pydev.shared_core.image.IImageCache;
import org.python.pydev.shared_core.image.IImageHandle;
import org.python.pydev.shared_core.image.UIConstants;

import com.python.pydev.analysis.additionalinfo.builders.AnalysisRunner;

public class DontAnalyzeFileMarkerParticipant implements IAssistProps {

    private IImageHandle annotationImage;

    public DontAnalyzeFileMarkerParticipant() {
        IImageCache analysisImageCache = SharedCorePlugin.getImageCache();
        annotationImage = analysisImageCache.get(UIConstants.ASSIST_ANNOTATION);
    }

    @Override
    public List<ICompletionProposalHandle> getProps(PySelection ps, IImageCache imageCache, File f,
            IPythonNature nature,
            IPyEdit edit, int offset) throws BadLocationException {
        List<ICompletionProposalHandle> props = new ArrayList<ICompletionProposalHandle>();
        if (ps.getCursorLine() == 0) {
            String replacementString = '#' + AnalysisRunner.PYDEV_CODE_ANALYSIS_IGNORE + ps.getEndLineDelim();

            ICompletionProposalHandle proposal = CompletionProposalFactory.get().createIgnoreCompletionProposal(
                    replacementString, 0, 0, offset + replacementString.length(), annotationImage,
                    AnalysisRunner.PYDEV_CODE_ANALYSIS_IGNORE, null, null, IPyCompletionProposal.PRIORITY_DEFAULT,
                    edit);
            props.add(proposal);

        }
        return props;
    }

    @Override
    public boolean isValid(PySelection ps, String sel, IPyEdit edit, int offset) {
        return ps.getCursorLine() == 0
                && ps.getCursorLineContents().indexOf(AnalysisRunner.PYDEV_CODE_ANALYSIS_IGNORE) == -1;
    }

}
