/*
 * Copyright (c) 2016, Oracle and/or its affiliates.
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
package com.oracle.truffle.llvm.nodes.intrinsics.llvm.arith;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.llvm.runtime.LLVMAddress;
import com.oracle.truffle.llvm.runtime.memory.LLVMMemory;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;

public final class LLVMComplexDiv extends LLVMExpressionNode {

    @Child private LLVMExpressionNode aNode;
    @Child private LLVMExpressionNode bNode;
    @Child private LLVMExpressionNode cNode;
    @Child private LLVMExpressionNode dNode;
    @Child private LLVMExpressionNode alloc;

    public LLVMComplexDiv(LLVMExpressionNode alloc, LLVMExpressionNode a, LLVMExpressionNode b, LLVMExpressionNode c, LLVMExpressionNode d) {
        this.alloc = alloc;
        this.aNode = a;
        this.bNode = b;
        this.cNode = c;
        this.dNode = d;
    }

    @CompilationFinal private LLVMMemory memory;

    private LLVMMemory getMemory() {
        if (memory == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            memory = getLLVMMemory();
        }
        return memory;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        try {
            double a = aNode.executeDouble(frame);
            double b = bNode.executeDouble(frame);
            double c = cNode.executeDouble(frame);
            double d = dNode.executeDouble(frame);

            double denom = c * c + d * d;
            double zReal = (a * c + b * d) / denom;
            double zImag = (b * c - a * d) / denom;

            LLVMAddress allocatedMemory = alloc.executeLLVMAddress(frame);
            getMemory().putDouble(allocatedMemory, zReal);
            getMemory().putDouble(allocatedMemory.getVal() + LLVMExpressionNode.DOUBLE_SIZE_IN_BYTES, zImag);

            return allocatedMemory;
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreter();
            throw new IllegalStateException(e);
        }
    }
}
