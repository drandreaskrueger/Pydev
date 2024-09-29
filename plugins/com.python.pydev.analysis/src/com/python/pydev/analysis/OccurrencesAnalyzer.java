/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 19/07/2005
 */
package com.python.pydev.analysis;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.ast.analysis.messages.IMessage;
import org.python.pydev.ast.analysis.messages.Message;
import org.python.pydev.ast.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.core.IAnalysisPreferences;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.io.PyUnsupportedEncodingException;

import com.python.pydev.analysis.pep8.Pep8Visitor;
import com.python.pydev.analysis.tabnanny.TabNanny;
import com.python.pydev.analysis.visitors.OccurrencesVisitor;

/**
 * This class is responsible for starting the analysis of a given module.
 *
 * @author Fabio
 */
public class OccurrencesAnalyzer {

    public IMessage[] analyzeDocument(IPythonNature nature, final SourceModule module,
            final IAnalysisPreferences prefs,
            final IDocument document, final IProgressMonitor monitor, IIndentPrefs indentPrefs) {

        //Do pep8 in a thread.
        final List<IMessage> pep8Messages = new ArrayList<>();
        Thread t = new Thread() {
            @Override
            public void run() {
                pep8Messages.addAll(new Pep8Visitor().getMessages(module, document, monitor, prefs));
            }
        };
        t.start();
        OccurrencesVisitor visitor = new OccurrencesVisitor(nature, module.getName(), module, prefs, document, monitor);
        try {
            SimpleNode ast = module.getAst();
            if (ast != null) {
                if (nature.startRequests()) {
                    try {
                        ast.accept(visitor);
                    } finally {
                        nature.endRequests();
                    }
                }
            }
        } catch (OperationCanceledException e) {
            throw e;
        } catch (Exception e) {
            Log.log(IStatus.ERROR, ("Error while visiting " + module.getName() + " (" + module.getFile() + ")"), e);
        }

        List<IMessage> messages = new ArrayList<IMessage>();
        if (!monitor.isCanceled()) {
            messages.addAll(visitor.getMessages());
            try {
                FileUtils.getPythonFileEncoding(document, module.getName());
            } catch (PyUnsupportedEncodingException e) {
                Message m = new Message(IAnalysisPreferences.TYPE_INVALID_ENCODING, e.getMessage(), e.getLine(),
                        e.getLine(), e.getColumn(), e.getColumn() + e.getMessage().length(), prefs);
                messages.add(m);
            }
            try {
                messages.addAll(TabNanny.analyzeDoc(document, prefs, module.getName(), indentPrefs, monitor));
            } catch (Exception e) {
                Log.log(e); //just to be safe... (could happen if the document changes during the process).
            }
        }

        if (!monitor.isCanceled()) {
            try {
                t.join();
                messages.addAll(pep8Messages);
            } catch (InterruptedException e) {
                //If interrupted keep on going as it is.
            }
        }

        return messages.toArray(new IMessage[messages.size()]);
    }

}
