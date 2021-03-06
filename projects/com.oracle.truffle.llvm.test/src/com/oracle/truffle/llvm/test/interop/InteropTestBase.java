/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.truffle.llvm.test.interop;

import java.io.File;
import java.io.IOException;
import org.junit.ClassRule;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.InteropException;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.llvm.test.options.TestOptions;
import com.oracle.truffle.tck.TruffleRunner;
import org.graalvm.polyglot.Value;

public class InteropTestBase {

    @ClassRule public static TruffleRunner.RunWithPolyglotRule runWithPolyglot = new TruffleRunner.RunWithPolyglotRule();

    private static final File testBase = new File(TestOptions.TEST_SUITE_PATH, "interop");

    protected static TruffleObject loadTestBitcodeInternal(String name) {
        File dir = new File(testBase, name);
        File file = new File(dir, "O0_MEM2REG.bc");
        Source source;
        try {
            source = Source.newBuilder(file).language("llvm").build();
        } catch (IOException ex) {
            throw new AssertionError(ex);
        }
        CallTarget target = runWithPolyglot.getTruffleTestEnv().parse(source);
        return (TruffleObject) target.call();
    }

    protected static Value loadTestBitcodeValue(String name) {
        File dir = new File(testBase, name);
        File file = new File(dir, "O0_MEM2REG.bc");
        org.graalvm.polyglot.Source source;
        try {
            source = org.graalvm.polyglot.Source.newBuilder("llvm", file).build();
        } catch (IOException ex) {
            throw new AssertionError(ex);
        }
        return runWithPolyglot.getPolyglotContext().eval(source);
    }

    public static class SulongTestNode extends RootNode {

        private final TruffleObject function;
        @Child private Node execute;

        protected SulongTestNode(TruffleObject testLibrary, String fnName, int argCount) {
            super(null);
            try {
                function = (TruffleObject) ForeignAccess.sendRead(Message.READ.createNode(), testLibrary, fnName);
            } catch (InteropException ex) {
                throw new AssertionError(ex);
            }
            execute = Message.createExecute(argCount).createNode();
        }

        @Override
        public Object execute(VirtualFrame frame) {
            try {
                return ForeignAccess.sendExecute(execute, function, frame.getArguments());
            } catch (InteropException ex) {
                throw new AssertionError(ex);
            }
        }
    }

}
