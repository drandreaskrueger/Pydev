/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Apr 12, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.correctionassist.heuristics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.source.ISourceViewer;
import org.python.pydev.codingstd.ICodingStd;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.docutils.ParsingUtils;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PyStringUtils;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.proposals.CompletionProposalFactory;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.editor.correctionassist.IAssistProps;
import org.python.pydev.plugin.preferences.PyCodeStylePreferencesPage;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.code_completion.IPyCompletionProposal;
import org.python.pydev.shared_core.image.IImageCache;
import org.python.pydev.shared_core.image.IImageHandle;
import org.python.pydev.shared_core.image.UIConstants;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.string.TextSelectionUtils;

/**
 * @author Fabio Zadrozny
 */
public class AssistAssign implements IAssistProps {

    private ICodingStd std;

    public AssistAssign() {
        this(new ICodingStd() {

            @Override
            public boolean localsAndAttrsCamelcase() {
                return PyCodeStylePreferencesPage.useLocalsAndAttrsCamelCase();
            }

        });
    }

    public AssistAssign(ICodingStd std) {
        this.std = std;
    }

    private IImageHandle getImage(IImageCache imageCache, String c) {
        if (imageCache != null) {
            return imageCache.get(c);
        }
        return null;
    }

    /**
     * @see org.python.pydev.editor.correctionassist.IAssistProps#getProps
     */
    @Override
    public List<ICompletionProposalHandle> getProps(PySelection ps, IImageCache imageCache, File f,
            IPythonNature nature,
            IPyEdit edit, int offset) throws BadLocationException {
        PySourceViewer viewer = null;
        if (edit != null) { //only in tests it's actually null
            viewer = ((PyEdit) edit).getPySourceViewer();
        }

        return this.getProps(ps, imageCache, viewer, offset, TextSelectionUtils.getLineWithoutComments(ps),
                PySelection.getFirstCharPosition(ps.getDoc(), ps.getAbsoluteCursorOffset()));
    }

    /**
     * Actual implementation (receiving a source viewer and only the actually used parameters).
     *
     * @see org.python.pydev.editor.correctionassist.IAssistProps#getProps
     *
     * @param lineWithoutComments the line that should be checked (without any comments)
     */
    public List<ICompletionProposalHandle> getProps(PySelection ps, IImageCache imageCache, ISourceViewer sourceViewer,
            int offset, String lineWithoutComments, int firstCharAbsolutePosition) throws BadLocationException {

        List<ICompletionProposalHandle> l = new ArrayList<>();
        if (lineWithoutComments.trim().length() == 0) {
            return l;
        }

        //go on and make the suggestion.
        //
        //if we have a method call, eg.:
        //  e.methodCall()| would result in the following suggestions:
        //
        //                   methodCall = e.methodCall()
        //                     self.methodCall = e.methodCall()
        //
        // NewClass()| would result in
        //
        //                   newClass = NewClass()
        //                     self.newClass = NewClass()
        //
        //now, if we don't have a method call, eg.:
        // 1+1| would result in
        //
        //                     |result| = 1+1
        //                     self.|result| = 1+1

        String callName = getTokToAssign(ps);

        if (callName.length() > 0) {
            //all that just to change first char to lower case.
            if (callName.toLowerCase().startsWith("get") && callName.length() > 3) {
                callName = callName.substring(3);
            }

            callName = changeToCodingStd(callName);

            for (int i = 0; i < callName.length(); i++) {
                char c = callName.charAt(i);
                if (c != '_') {
                    callName = TextSelectionUtils.lowerChar(callName, i);
                    break;
                }
            }
        } else {
            callName = "result";
        }

        String loc = callName;
        if (loc.startsWith("_")) {
            loc = loc.substring(1);
        }
        l.add(CompletionProposalFactory.get().createAssistAssignCompletionProposal(loc + " = ",
                firstCharAbsolutePosition, 0, 0, getImage(imageCache,
                        UIConstants.ASSIST_ASSIGN_TO_LOCAL),
                "Assign to local (" + loc + ")", null, null, IPyCompletionProposal.PRIORITY_DEFAULT, sourceViewer,
                null));

        l.add(CompletionProposalFactory.get().createAssistAssignCompletionProposal("self." + callName + " = ",
                firstCharAbsolutePosition, 0, 5, getImage(
                        imageCache, UIConstants.ASSIST_ASSIGN_TO_CLASS),
                "Assign to field (self." + callName + ")", null, null, IPyCompletionProposal.PRIORITY_DEFAULT,
                sourceViewer, null));
        return l;
    }

    private String changeToCodingStd(String callName) {
        if (this.std.localsAndAttrsCamelcase()) {
            return StringUtils.asStyleCamelCaseFirstLower(callName);

        } else {
            return StringUtils.asStyleLowercaseUnderscores(callName);
        }
    }

    /**
     * @see org.python.pydev.editor.correctionassist.IAssistProps#isValid
     */
    @Override
    public boolean isValid(PySelection ps, String sel, IPyEdit edit, int offset) {
        return isValid(ps.getTextSelection().getLength(), sel, offset);
    }

    /**
     * @param selectionLength the length of the currently selected text
     * @param lineContents the contents of the line
     * @param offset the offset of the cursor
     * @return true if an assign is available and false otherwise
     */
    public boolean isValid(int selectionLength, String lineContents, int offset) {
        if (!(selectionLength == 0)) {
            return false;
        }

        if (!(lineContents.indexOf("class ") == -1 && lineContents.indexOf("def ") == -1 && lineContents
                .indexOf("import ") == -1)) {

            return false;
        }

        String eqReplaced = lineContents.replaceAll("==", "");
        if (eqReplaced.indexOf("=") != -1) { //we have some equal
            //ok, make analysis taking into account the first parentesis
            if (eqReplaced.indexOf('(') == -1) {
                return false;
            }
            int i = eqReplaced.indexOf('(');
            if (eqReplaced.substring(0, i).indexOf('=') != -1) {
                return false;
            }
        }
        return true;
    }

    private static String getStringToAnalyze(PySelection ps) {
        ParsingUtils parsingUtils = ParsingUtils.create(ps.getDoc());
        FastStringBuffer buf = new FastStringBuffer();
        String string = null;
        try {
            parsingUtils.getFullFlattenedLine(ps.getStartLineOffset(), buf);
            if (buf.length() > 0) {
                string = buf.toString();
            }
        } catch (SyntaxErrorException e) {
            //won't happen (we didn't ask for it)
            Log.log(e);
        }
        if (string == null) {
            string = TextSelectionUtils.getLineWithoutComments(ps);
        }
        return string.trim();
    }

    /**
     * @return string with the token or empty token if not found.
     */
    private static String getBeforeParentesisTok(String string) {
        int i;

        String callName = "";
        //get parenthesis position and go backwards
        if ((i = string.lastIndexOf("(")) != -1) {
            callName = "";

            for (int j = i - 1; j >= 0 && TextSelectionUtils.stillInTok(string, j); j--) {
                callName = string.charAt(j) + callName;
            }

        }
        return callName;
    }

    /**
     * @return the token which should be used to make the assign.
     */
    private String getTokToAssign(PySelection ps) {
        String string = getStringToAnalyze(ps); //it's already trimmed!
        String tokToAssign = getTokToAssign(string);
        if (tokToAssign == null || tokToAssign.length() == 0) {
            return "result";
        }
        return tokToAssign;
    }

    private String changeToLowerUppercaseConstant(String callName) {
        if (StringUtils.isAllUpper(callName)) {
            return callName.toLowerCase();
        }
        return callName;
    }

    public String getTokToAssign(String string) {
        string = string.trim();

        String callName = "";

        String beforeParentesisTok = getBeforeParentesisTok(string);
        if (beforeParentesisTok.length() > 0) {
            callName = beforeParentesisTok;
        } else {
            //otherwise, try to find . (ignore code after #)
            int i;
            if ((i = string.lastIndexOf(".")) != -1) {
                callName = "";

                for (int j = i + 1; j < string.length() && TextSelectionUtils.stillInTok(string, j); j++) {
                    callName += string.charAt(j);
                }
            }
            if (callName.length() == 0) {
                if (PyStringUtils.isPythonIdentifier(string)) {
                    callName = string;
                }
            }
        }
        callName = changeToLowerUppercaseConstant(callName);

        if (callName.length() > 0) {
            //all that just to change first char to lower case.
            if (callName.toLowerCase().startsWith("get") && callName.length() > 3) {
                callName = callName.substring(3);

            } else if (callName.toLowerCase().startsWith("_get") && callName.length() > 4) {
                callName = callName.substring(4);
            }

            callName = changeToCodingStd(callName);

            for (int i = 0; i < callName.length(); i++) {
                char c = callName.charAt(i);
                if (c != '_') {
                    callName = TextSelectionUtils.lowerChar(callName, i);
                    break;
                }
            }
        } else {
            callName = null;
        }
        return callName;
    }

}
