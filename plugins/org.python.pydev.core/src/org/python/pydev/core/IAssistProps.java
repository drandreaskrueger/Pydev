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
package org.python.pydev.core;

import java.io.File;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.image.IImageCache;

/**
 * @author Fabio Zadrozny
 */
public interface IAssistProps {

    /**
     * Gets the completion proposals related to the common assists
     *
     * @note This method is only called if isValid returned true (and isValid is ALWAYS called before it).
     *
     * @param ps this is the selection
     * @param imageCache this is a cache for images (from the pydev plugin)
     * @param f this is the file related to the editor
     * @param nature this is the nature related to this file
     * @param edit this is the edit where the request took place
     * @param offset this is the offset of the edit
     *
     * @return a list of completions with proposals to fix things
     * @throws BadLocationException
     * @throws MisconfigurationException
     */
    List<ICompletionProposalHandle> getProps(PySelection ps, IImageCache imageCache, File f, IPythonNature nature,
            IPyEdit edit, int offset) throws BadLocationException, MisconfigurationException;

    /**
     * Gets whether this assist proposal is valid to be applied at the current line
     *
     * @param ps the current selection
     * @param sel is the current string without any comments
     * @param edit this is the edit where the request took place
     * @param offset this is the offset of the edit
     *
     * @return whether the assist can be applied at the current line
     */
    boolean isValid(PySelection ps, String sel, IPyEdit edit, int offset);
}
