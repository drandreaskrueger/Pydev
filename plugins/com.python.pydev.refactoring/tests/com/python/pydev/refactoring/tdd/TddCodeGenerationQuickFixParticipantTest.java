/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.tdd;

import java.io.File;
import java.util.List;

import org.eclipse.jface.text.Document;
import org.python.pydev.ast.codecompletion.PyCodeCompletion;
import org.python.pydev.ast.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.ast.codecompletion.revisited.PyEditStub;
import org.python.pydev.ast.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.ast.refactoring.AbstractPyRefactoring;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_ui.editor_input.PydevFileEditorInput;

import com.python.pydev.analysis.refactoring.refactorer.Refactorer;
import com.python.pydev.analysis.refactoring.tdd.AbstractPyCreateClassOrMethodOrField;
import com.python.pydev.analysis.refactoring.tdd.TemplateInfo;

/**
 * @author Fabio
 *
 */
public class TddCodeGenerationQuickFixParticipantTest extends CodeCompletionTestsBase {

    public static void main(String[] args) {

        try {
            //DEBUG_TESTS_BASE = true;
            TddCodeGenerationQuickFixParticipantTest test = new TddCodeGenerationQuickFixParticipantTest();
            test.setUp();
            test.testDontCreate();
            test.tearDown();
            System.out.println("Finished");

            junit.textui.TestRunner.run(TddCodeGenerationQuickFixParticipantTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        AbstractPyRefactoring.setPyRefactoring(new Refactorer());
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        this.restorePythonPath(TestDependent.getCompletePythonLib(true, isPython3Test()) +
                "|" + TestDependent.PYTHON2_PIL_PACKAGES +
                "|"
                + TestDependent.TEST_PYSRC_TESTING_LOC +
                "configobj-4.6.0-py2.6.egg", false);

        this.restorePythonPath(false);
        codeCompletion = new PyCodeCompletion();
        TddCodeGenerationQuickFixWithoutMarkersParticipant.onGetTddPropsError = new ICallback<Boolean, Exception>() {

            @Override
            public Boolean call(Exception e) {
                throw new RuntimeException("Error:" + org.python.pydev.shared_core.log.Log.getExceptionStr(e));
            }
        };
        PyCodeCompletion.onCompletionRecursionException = new ICallback<Object, CompletionRecursionException>() {

            @Override
            public Object call(CompletionRecursionException e) {
                throw new RuntimeException(
                        "Recursion error:" + org.python.pydev.shared_core.log.Log.getExceptionStr(e));
            }

        };
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    public void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        super.tearDown();
        AbstractPyRefactoring.setPyRefactoring(null);
        PyCodeCompletion.onCompletionRecursionException = null;
    }

    public void testCreateInSelf1() throws Exception {
        String s = """
                print i
                class Foo(object):

                #comment

                    def m1(self):
                        self.m2
                """;
        String expected = """
                print i
                class Foo(object):

                #comment


                    def m2(self):
                        pass
                ----
                ----
                    def m1(self):
                        self.m2
                """.replace('-', ' '); // Hack to keep whitespaces there
        String label = "Create m2 method at Foo";

        check(s, expected, label);

    }

    public void testCreateInSelf2() throws Exception {
        String s = """
                print i
                class Foo(object):

                #comment

                    def m1(self):
                        self.m2()
                """;
        String expected = """
                print i
                class Foo(object):

                #comment


                    def m2(self):
                        pass
                ----
                ----
                    def m1(self):
                        self.m2()
                """.replace('-', ' '); // Hack to keep whitespaces there
        String label = "Create m2 method at Foo";

        check(s, expected, label);
    }

    private void check(String s, String expected, String label) throws MisconfigurationException {
        Document doc = new Document(s);

        File file = new File(TestDependent.TEST_PYSRC_TESTING_LOC + "extendable/check_analysis_and_tdd.py");
        PydevFileEditorInput editorInputStub = new PydevFileEditorInput(file);
        IPyEdit edit = new PyEditStub(doc, editorInputStub, nature, file);

        List<ICompletionProposalHandle> props = TddCodeGenerationQuickFixWithoutMarkersParticipant.getTddProps(
                new PySelection(doc, s.length() - 1), null, null, nature, edit, s.length() - 1, null);
        ICompletionProposalHandle handle = assertContains(label, props.toArray(new ICompletionProposalHandle[0]));
        TddRefactorCompletion completion = (TddRefactorCompletion) handle;
        TemplateInfo templateInfo = completion.getAsTemplateInfo();
        templateInfo.apply(doc);
        assertEquals(expected, doc.get());
    }

    public void testCreate() throws Exception {
        String s = "" +
                "class MyClass(object):\n" +
                "    pass\n" +
                "\n" +
                "def makeTestObj():\n" +
                "    return MyClass()\n" +
                "\n" +
                "def makeTestObj2():\n" +
                "    return makeTestObj()\n" +
                "\n" +
                "def testName():\n" +
                "    obj = makeTestObj2()\n" +
                "    obj.unimplementedFunction()\n" +
                "";

        Document doc = new Document(s);
        List<ICompletionProposalHandle> props = TddCodeGenerationQuickFixWithoutMarkersParticipant.getTddProps(
                new PySelection(doc, s.length() - 1), null,
                null,
                nature, null, s.length() - 1, null);
        assertContains("Create unimplementedFunction method at MyClass (__module_not_in_the_pythonpath__)",
                props.toArray(new ICompletionProposalHandle[0]));
    }

    public void testCreate2() throws Exception {
        String s = "" +
                "class MyClass(object):\n" +
                "    pass\n" +
                "def testName():\n" +
                "    obj = MyClass()\n" +
                "    obj.unimplementedFunction(a.x, 'Some comment in ticket.')\n" +
                "";

        Document doc = new Document(s);
        List<ICompletionProposalHandle> props = TddCodeGenerationQuickFixWithoutMarkersParticipant.getTddProps(
                new PySelection(doc, s.length() - 1), null,
                null,
                nature, null, s.length() - 1, null);
        TddRefactorCompletionInModule proposal = (TddRefactorCompletionInModule) assertContains(
                "Create unimplementedFunction method at MyClass (__module_not_in_the_pythonpath__)",
                props.toArray(new ICompletionProposalHandle[0]));
        //Todo: check result of apply as string is breaking!
        List<String> parametersAfterCall = proposal.getParametersAfterCall();
        FastStringBuffer createParametersList = AbstractPyCreateClassOrMethodOrField
                .createParametersList(parametersAfterCall);
        assertEquals("${x}, ${param1}", createParametersList.toString());
    }

    public void testDontCreate() throws Exception {
        String s = "" +
                "class MyClass(object):\n" +
                "\n" +
                "    def unimplementedFunction(self):\n" +
                "        pass\n"
                +
                "\n" +
                "def makeTestObj():\n" +
                "    return MyClass()\n" +
                "\n" +
                "def makeTestObj2():\n"
                +
                "    return makeTestObj()\n" +
                "\n" +
                "def testName():\n" +
                "    obj = makeTestObj2()\n"
                +
                "    obj.unimplementedFunction()\n" +
                "";

        Document doc = new Document(s);
        List<ICompletionProposalHandle> props = TddCodeGenerationQuickFixWithoutMarkersParticipant.getTddProps(
                new PySelection(doc, s.length() - 1), null,
                null,
                nature, null, s.length() - 1, null);
        assertNotContains("Create unimplementedFunction method at MyClass (__module_not_in_the_pythonpath__)",
                props.toArray(new ICompletionProposalHandle[0]));
    }

    public void testCreateWithTypeAsParam() throws Exception {
        int usedGrammar = GRAMMAR_TO_USE_FOR_PARSING;
        GRAMMAR_TO_USE_FOR_PARSING = PythonNature.LATEST_GRAMMAR_PY3_VERSION;
        try {
            String s = "" +
                    "class Foo(object):\n" +
                    "    pass\n" +
                    "\n" +
                    "def method(foo: Foo = 'A'):\n" +
                    "    foo.bar()";

            Document doc = new Document(s);
            List<ICompletionProposalHandle> props = TddCodeGenerationQuickFixWithoutMarkersParticipant.getTddProps(
                    new PySelection(doc, s.length() - 1), null,
                    null,
                    nature, null, s.length() - 1, null);
            assertContains("Create bar method at Foo (__module_not_in_the_pythonpath__)",
                    props.toArray(new ICompletionProposalHandle[0]));
        } finally {
            GRAMMAR_TO_USE_FOR_PARSING = usedGrammar;
        }
    }

    public void testCreateWithTypeAsParam2() throws Exception {
        int usedGrammar = GRAMMAR_TO_USE_FOR_PARSING;
        GRAMMAR_TO_USE_FOR_PARSING = PythonNature.LATEST_GRAMMAR_PY3_VERSION;
        try {
            String s = "" +
                    "class Foo(object):\n" +
                    "    pass\n" +
                    "\n" +
                    "def method(foo = Foo()):\n" +
                    "    foo.bar()";

            Document doc = new Document(s);
            List<ICompletionProposalHandle> props = TddCodeGenerationQuickFixWithoutMarkersParticipant.getTddProps(
                    new PySelection(doc, s.length() - 1), null,
                    null,
                    nature, null, s.length() - 1, null);
            assertContains("Create bar method at Foo (__module_not_in_the_pythonpath__)",
                    props.toArray(new ICompletionProposalHandle[0]));
        } finally {
            GRAMMAR_TO_USE_FOR_PARSING = usedGrammar;
        }
    }

    public void testCreateWithTypeAsAnnotation() throws Exception {
        int usedGrammar = GRAMMAR_TO_USE_FOR_PARSING;
        GRAMMAR_TO_USE_FOR_PARSING = PythonNature.LATEST_GRAMMAR_PY3_VERSION;
        try {
            String s = "" +
                    "class Foo(object):\n" +
                    "    pass\n" +
                    "\n" +
                    "def method2() -> Foo:\n" +
                    "    pass\n" +
                    "\n" +
                    "def method():\n" +
                    "    foo = method2()\n" +
                    "    foo.bar()";

            Document doc = new Document(s);
            List<ICompletionProposalHandle> props = TddCodeGenerationQuickFixWithoutMarkersParticipant.getTddProps(
                    new PySelection(doc, s.length() - 1), null,
                    null,
                    nature, null, s.length() - 1, null);
            assertContains("Create bar method at Foo (__module_not_in_the_pythonpath__)",
                    props.toArray(new ICompletionProposalHandle[0]));
        } finally {
            GRAMMAR_TO_USE_FOR_PARSING = usedGrammar;
        }
    }
}
