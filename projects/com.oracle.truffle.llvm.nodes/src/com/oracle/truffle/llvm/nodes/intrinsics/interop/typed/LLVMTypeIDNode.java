/*
 * Copyright (c) 2018, Oracle and/or its affiliates.
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
package com.oracle.truffle.llvm.nodes.intrinsics.interop.typed;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.llvm.nodes.intrinsics.interop.typed.LLVMTypeIDNodeFactory.ArrayNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.interop.typed.LLVMTypeIDNodeFactory.SizedArrayNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.interop.typed.LLVMTypeIDNodeFactory.StructNodeGen;
import com.oracle.truffle.llvm.runtime.global.LLVMGlobal;
import com.oracle.truffle.llvm.runtime.interop.access.LLVMInteropType;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMNode;

public abstract class LLVMTypeIDNode extends LLVMNode {

    public abstract LLVMInteropType.Structured execute(VirtualFrame frame);

    public static LLVMTypeIDNode createStruct(LLVMExpressionNode child) {
        return StructNodeGen.create(child);
    }

    public static LLVMTypeIDNode createArray(LLVMExpressionNode child) {
        return ArrayNodeGen.create(child);
    }

    public static LLVMTypeIDNode createSizedArray(LLVMExpressionNode type, LLVMExpressionNode len) {
        return SizedArrayNodeGen.create(type, len);
    }

    @NodeChild(type = LLVMExpressionNode.class)
    abstract static class Struct extends LLVMTypeIDNode {

        @Specialization
        LLVMInteropType.Structured doGlobal(LLVMGlobal typeid) {
            LLVMInteropType type = typeid.getInteropType();
            if (type instanceof LLVMInteropType.Array) {
                type = ((LLVMInteropType.Array) type).getElementType();
                if (type instanceof LLVMInteropType.Structured) {
                    return (LLVMInteropType.Structured) type;
                }
            }

            CompilerDirectives.transferToInterpreter();
            return fallback(typeid);
        }

        @Fallback
        LLVMInteropType.Structured fallback(@SuppressWarnings("unused") Object typeid) {
            CompilerDirectives.transferToInterpreter();
            throw new AssertionError("don't call __polyglot_[as|from]_typed functions directly, use POLYGLOT_DECLARE_* macros");
        }
    }

    @NodeChild(type = LLVMExpressionNode.class)
    abstract static class Array extends LLVMTypeIDNode {

        @Specialization
        LLVMInteropType.Structured doGlobal(LLVMGlobal typeid) {
            LLVMInteropType type = typeid.getInteropType();
            if (type instanceof LLVMInteropType.Array) {
                return (LLVMInteropType.Array) type;
            }

            CompilerDirectives.transferToInterpreter();
            return fallback(typeid);
        }

        @Fallback
        LLVMInteropType.Structured fallback(@SuppressWarnings("unused") Object typeid) {
            CompilerDirectives.transferToInterpreter();
            throw new AssertionError("don't call __polyglot_as_typed_array function directly, use POLYGLOT_DECLARE_* macros");
        }
    }

    @NodeChild(value = "type", type = LLVMExpressionNode.class)
    @NodeChild(value = "len", type = LLVMExpressionNode.class)
    abstract static class SizedArray extends LLVMTypeIDNode {

        @Specialization
        LLVMInteropType.Structured doGlobal(LLVMGlobal typeid, long len) {
            LLVMInteropType type = typeid.getInteropType();
            if (type instanceof LLVMInteropType.Array) {
                return ((LLVMInteropType.Array) type).resize(len);
            }

            CompilerDirectives.transferToInterpreter();
            return fallback(typeid, len);
        }

        @Fallback
        LLVMInteropType.Structured fallback(@SuppressWarnings("unused") Object typeid, @SuppressWarnings("unused") Object len) {
            CompilerDirectives.transferToInterpreter();
            throw new AssertionError("don't call __polyglot_from_typed_array function directly, use POLYGLOT_DECLARE_* macros");
        }
    }
}
