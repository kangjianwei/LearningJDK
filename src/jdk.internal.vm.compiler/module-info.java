/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

module jdk.internal.vm.compiler {
    // source file: file:///t:/workspace/open/src/jdk.internal.vm.compiler/share/classes/module-info.java
    //              file:///t:/workspace/build/windows-x64-open/support/gensrc/jdk.internal.vm.compiler/module-info.java.extra
    requires java.instrument;
    requires java.management;
    requires jdk.internal.vm.ci;
    requires jdk.management;
    requires jdk.unsupported;   // sun.misc.Unsafe is used
    exports jdk.internal.vm.compiler.collections to jdk.internal.vm.compiler.management;
    exports jdk.internal.vm.compiler.word to jdk.aot;
    exports org.graalvm.compiler.api.directives to jdk.aot;
    exports org.graalvm.compiler.api.replacements to jdk.aot;
    exports org.graalvm.compiler.api.runtime to jdk.aot;
    exports org.graalvm.compiler.asm.aarch64 to jdk.aot;
    exports org.graalvm.compiler.asm.amd64 to jdk.aot;
    exports org.graalvm.compiler.bytecode to jdk.aot;
    exports org.graalvm.compiler.code to jdk.aot;
    exports org.graalvm.compiler.core to jdk.aot;
    exports org.graalvm.compiler.core.common to
        jdk.aot,
        jdk.internal.vm.compiler.management;
    exports org.graalvm.compiler.core.target to jdk.aot;
    exports org.graalvm.compiler.debug to
        jdk.aot,
        jdk.internal.vm.compiler.management;
    exports org.graalvm.compiler.graph to jdk.aot;
    exports org.graalvm.compiler.hotspot to
        jdk.aot,
        jdk.internal.vm.compiler.management;
    exports org.graalvm.compiler.hotspot.meta to jdk.aot;
    exports org.graalvm.compiler.hotspot.replacements to jdk.aot;
    exports org.graalvm.compiler.hotspot.stubs to jdk.aot;
    exports org.graalvm.compiler.hotspot.word to jdk.aot;
    exports org.graalvm.compiler.java to jdk.aot;
    exports org.graalvm.compiler.lir.asm to jdk.aot;
    exports org.graalvm.compiler.lir.phases to jdk.aot;
    exports org.graalvm.compiler.nodes to jdk.aot;
    exports org.graalvm.compiler.nodes.graphbuilderconf to jdk.aot;
    exports org.graalvm.compiler.options to
        jdk.aot,
        jdk.internal.vm.compiler.management;
    exports org.graalvm.compiler.phases to jdk.aot;
    exports org.graalvm.compiler.phases.tiers to jdk.aot;
    exports org.graalvm.compiler.printer to jdk.aot;
    exports org.graalvm.compiler.replacements to jdk.aot;
    exports org.graalvm.compiler.runtime to jdk.aot;
    exports org.graalvm.compiler.serviceprovider to
        jdk.aot,
        jdk.internal.vm.compiler.management;
    exports org.graalvm.compiler.word to jdk.aot;

    uses org.graalvm.compiler.code.DisassemblerProvider;
    uses org.graalvm.compiler.core.match.MatchStatementSet;
    uses org.graalvm.compiler.debug.DebugHandlersFactory;
    uses org.graalvm.compiler.debug.TTYStreamProvider;
    uses org.graalvm.compiler.hotspot.CompilerConfigurationFactory;
    uses org.graalvm.compiler.hotspot.HotSpotBackendFactory;
    uses org.graalvm.compiler.hotspot.HotSpotCodeCacheListener;
    uses org.graalvm.compiler.hotspot.HotSpotGraalManagementRegistration;
    uses org.graalvm.compiler.nodes.graphbuilderconf.NodeIntrinsicPluginFactory;
    uses org.graalvm.compiler.options.OptionDescriptors;
    uses org.graalvm.compiler.serviceprovider.GraalServices.JMXService;
    provides jdk.vm.ci.services.JVMCIServiceLocator with org.graalvm.compiler.hotspot.HotSpotGraalJVMCIServiceLocator;
    provides org.graalvm.compiler.code.DisassemblerProvider with
        org.graalvm.compiler.code.HexCodeFileDisassemblerProvider,
        org.graalvm.compiler.hotspot.meta.HotSpotDisassemblerProvider;
    provides org.graalvm.compiler.core.match.MatchStatementSet with
        org.graalvm.compiler.core.amd64.AMD64NodeMatchRules_MatchStatementSet,
        org.graalvm.compiler.core.sparc.SPARCNodeMatchRules_MatchStatementSet;
    provides org.graalvm.compiler.debug.DebugHandlersFactory with org.graalvm.compiler.printer.GraalDebugHandlersFactory;
    provides org.graalvm.compiler.debug.TTYStreamProvider with org.graalvm.compiler.hotspot.HotSpotTTYStreamProvider;
    provides org.graalvm.compiler.hotspot.CompilerConfigurationFactory with
        org.graalvm.compiler.hotspot.CommunityCompilerConfigurationFactory,
        org.graalvm.compiler.hotspot.EconomyCompilerConfigurationFactory;
    provides org.graalvm.compiler.hotspot.HotSpotBackendFactory with
        org.graalvm.compiler.hotspot.aarch64.AArch64HotSpotBackendFactory,
        org.graalvm.compiler.hotspot.amd64.AMD64HotSpotBackendFactory,
        org.graalvm.compiler.hotspot.sparc.SPARCHotSpotBackendFactory;
    provides org.graalvm.compiler.nodes.graphbuilderconf.NodeIntrinsicPluginFactory with
        org.graalvm.compiler.hotspot.nodes.aot.PluginFactory_EncodedSymbolNode,
        org.graalvm.compiler.hotspot.nodes.aot.PluginFactory_InitializeKlassStubCall,
        org.graalvm.compiler.hotspot.nodes.aot.PluginFactory_LoadConstantIndirectlyFixedNode,
        org.graalvm.compiler.hotspot.nodes.aot.PluginFactory_LoadConstantIndirectlyNode,
        org.graalvm.compiler.hotspot.nodes.aot.PluginFactory_LoadMethodCountersIndirectlyNode,
        org.graalvm.compiler.hotspot.nodes.aot.PluginFactory_ResolveConstantStubCall,
        org.graalvm.compiler.hotspot.nodes.aot.PluginFactory_ResolveDynamicStubCall,
        org.graalvm.compiler.hotspot.nodes.aot.PluginFactory_ResolveMethodAndLoadCountersStubCall,
        org.graalvm.compiler.hotspot.nodes.PluginFactory_AcquiredCASLockNode,
        org.graalvm.compiler.hotspot.nodes.PluginFactory_AllocaNode,
        org.graalvm.compiler.hotspot.nodes.PluginFactory_BeginLockScopeNode,
        org.graalvm.compiler.hotspot.nodes.PluginFactory_ComputeObjectAddressNode,
        org.graalvm.compiler.hotspot.nodes.PluginFactory_CurrentJavaThreadNode,
        org.graalvm.compiler.hotspot.nodes.PluginFactory_CurrentLockNode,
        org.graalvm.compiler.hotspot.nodes.PluginFactory_DeoptimizeCallerNode,
        org.graalvm.compiler.hotspot.nodes.PluginFactory_DimensionsNode,
        org.graalvm.compiler.hotspot.nodes.PluginFactory_EndLockScopeNode,
        org.graalvm.compiler.hotspot.nodes.PluginFactory_FastAcquireBiasedLockNode,
        org.graalvm.compiler.hotspot.nodes.PluginFactory_GetObjectAddressNode,
        org.graalvm.compiler.hotspot.nodes.PluginFactory_GraalHotSpotVMConfigNode,
        org.graalvm.compiler.hotspot.nodes.PluginFactory_JumpToExceptionHandlerInCallerNode,
        org.graalvm.compiler.hotspot.nodes.PluginFactory_JumpToExceptionHandlerNode,
        org.graalvm.compiler.hotspot.nodes.PluginFactory_MonitorCounterNode,
        org.graalvm.compiler.hotspot.nodes.PluginFactory_PatchReturnAddressNode,
        org.graalvm.compiler.hotspot.nodes.PluginFactory_VMErrorNode,
        org.graalvm.compiler.hotspot.PluginFactory_HotSpotBackend,
        org.graalvm.compiler.hotspot.replacements.arraycopy.PluginFactory_ArrayCopyCallNode,
        org.graalvm.compiler.hotspot.replacements.arraycopy.PluginFactory_ArrayCopySnippets,
        org.graalvm.compiler.hotspot.replacements.arraycopy.PluginFactory_ArrayCopyWithSlowPathNode,
        org.graalvm.compiler.hotspot.replacements.arraycopy.PluginFactory_CheckcastArrayCopyCallNode,
        org.graalvm.compiler.hotspot.replacements.arraycopy.PluginFactory_GenericArrayCopyCallNode,
        org.graalvm.compiler.hotspot.replacements.PluginFactory_AESCryptSubstitutions,
        org.graalvm.compiler.hotspot.replacements.PluginFactory_AssertionSnippets,
        org.graalvm.compiler.hotspot.replacements.PluginFactory_CipherBlockChainingSubstitutions,
        org.graalvm.compiler.hotspot.replacements.PluginFactory_ClassGetHubNode,
        org.graalvm.compiler.hotspot.replacements.PluginFactory_CRC32CSubstitutions,
        org.graalvm.compiler.hotspot.replacements.PluginFactory_CRC32Substitutions,
        org.graalvm.compiler.hotspot.replacements.PluginFactory_HotSpotReplacementsUtil,
        org.graalvm.compiler.hotspot.replacements.PluginFactory_HubGetClassNode,
        org.graalvm.compiler.hotspot.replacements.PluginFactory_IdentityHashCodeNode,
        org.graalvm.compiler.hotspot.replacements.PluginFactory_MonitorSnippets,
        org.graalvm.compiler.hotspot.replacements.PluginFactory_NewObjectSnippets,
        org.graalvm.compiler.hotspot.replacements.PluginFactory_ObjectSubstitutions,
        org.graalvm.compiler.hotspot.replacements.PluginFactory_StringToBytesSnippets,
        org.graalvm.compiler.hotspot.replacements.PluginFactory_ThreadSubstitutions,
        org.graalvm.compiler.hotspot.replacements.PluginFactory_WriteBarrierSnippets,
        org.graalvm.compiler.hotspot.replacements.profiling.PluginFactory_ProbabilisticProfileSnippets,
        org.graalvm.compiler.hotspot.replacements.profiling.PluginFactory_ProfileSnippets,
        org.graalvm.compiler.hotspot.stubs.PluginFactory_CreateExceptionStub,
        org.graalvm.compiler.hotspot.stubs.PluginFactory_ExceptionHandlerStub,
        org.graalvm.compiler.hotspot.stubs.PluginFactory_NewArrayStub,
        org.graalvm.compiler.hotspot.stubs.PluginFactory_NewInstanceStub,
        org.graalvm.compiler.hotspot.stubs.PluginFactory_StubUtil,
        org.graalvm.compiler.hotspot.stubs.PluginFactory_UnwindExceptionToCallerStub,
        org.graalvm.compiler.nodes.debug.PluginFactory_DynamicCounterNode,
        org.graalvm.compiler.nodes.extended.PluginFactory_BranchProbabilityNode,
        org.graalvm.compiler.nodes.extended.PluginFactory_FixedValueAnchorNode,
        org.graalvm.compiler.nodes.extended.PluginFactory_MembarNode,
        org.graalvm.compiler.nodes.extended.PluginFactory_NullCheckNode,
        org.graalvm.compiler.nodes.extended.PluginFactory_RawLoadNode,
        org.graalvm.compiler.nodes.extended.PluginFactory_RawStoreNode,
        org.graalvm.compiler.nodes.extended.PluginFactory_StoreHubNode,
        org.graalvm.compiler.nodes.extended.PluginFactory_UnsafeCopyNode,
        org.graalvm.compiler.nodes.java.PluginFactory_ArrayLengthNode,
        org.graalvm.compiler.nodes.java.PluginFactory_DynamicNewArrayNode,
        org.graalvm.compiler.nodes.java.PluginFactory_NewArrayNode,
        org.graalvm.compiler.nodes.java.PluginFactory_RegisterFinalizerNode,
        org.graalvm.compiler.nodes.memory.address.PluginFactory_OffsetAddressNode,
        org.graalvm.compiler.nodes.memory.PluginFactory_MemoryAnchorNode,
        org.graalvm.compiler.nodes.PluginFactory_BreakpointNode,
        org.graalvm.compiler.nodes.PluginFactory_DeoptimizeNode,
        org.graalvm.compiler.nodes.PluginFactory_PauseNode,
        org.graalvm.compiler.nodes.PluginFactory_PiArrayNode,
        org.graalvm.compiler.nodes.PluginFactory_PiNode,
        org.graalvm.compiler.nodes.PluginFactory_PrefetchAllocateNode,
        org.graalvm.compiler.nodes.PluginFactory_SnippetAnchorNode,
        org.graalvm.compiler.replacements.aarch64.PluginFactory_AArch64FloatArithmeticSnippets,
        org.graalvm.compiler.replacements.aarch64.PluginFactory_AArch64IntegerArithmeticSnippets,
        org.graalvm.compiler.replacements.amd64.PluginFactory_AMD64MathSubstitutions,
        org.graalvm.compiler.replacements.amd64.PluginFactory_AMD64StringIndexOfNode,
        org.graalvm.compiler.replacements.amd64.PluginFactory_AMD64StringSubstitutions,
        org.graalvm.compiler.replacements.nodes.PluginFactory_ArrayCompareToNode,
        org.graalvm.compiler.replacements.nodes.PluginFactory_ArrayEqualsNode,
        org.graalvm.compiler.replacements.nodes.PluginFactory_AssertionNode,
        org.graalvm.compiler.replacements.nodes.PluginFactory_BinaryMathIntrinsicNode,
        org.graalvm.compiler.replacements.nodes.PluginFactory_BitScanForwardNode,
        org.graalvm.compiler.replacements.nodes.PluginFactory_BitScanReverseNode,
        org.graalvm.compiler.replacements.nodes.PluginFactory_CStringConstant,
        org.graalvm.compiler.replacements.nodes.PluginFactory_DirectStoreNode,
        org.graalvm.compiler.replacements.nodes.PluginFactory_ExplodeLoopNode,
        org.graalvm.compiler.replacements.nodes.PluginFactory_UnaryMathIntrinsicNode,
        org.graalvm.compiler.replacements.PluginFactory_ConstantStringIndexOfSnippets,
        org.graalvm.compiler.replacements.PluginFactory_Log,
        org.graalvm.compiler.replacements.PluginFactory_SnippetCounterNode;
    provides org.graalvm.compiler.options.OptionDescriptors with
        org.graalvm.compiler.core.amd64.AMD64NodeLIRBuilder_OptionDescriptors,
        org.graalvm.compiler.core.common.GraalOptions_OptionDescriptors,
        org.graalvm.compiler.core.common.spi.JavaConstantFieldProvider_OptionDescriptors,
        org.graalvm.compiler.core.common.util.CompilationAlarm_OptionDescriptors,
        org.graalvm.compiler.core.GraalCompilerOptions_OptionDescriptors,
        org.graalvm.compiler.core.phases.HighTier_OptionDescriptors,
        org.graalvm.compiler.core.phases.LowTier_OptionDescriptors,
        org.graalvm.compiler.debug.Assertions_OptionDescriptors,
        org.graalvm.compiler.debug.DebugOptions_OptionDescriptors,
        org.graalvm.compiler.graph.Graph_OptionDescriptors,
        org.graalvm.compiler.hotspot.BootstrapWatchDog_OptionDescriptors,
        org.graalvm.compiler.hotspot.CompilationCounters_OptionDescriptors,
        org.graalvm.compiler.hotspot.CompilationStatistics_OptionDescriptors,
        org.graalvm.compiler.hotspot.CompilationWatchDog_OptionDescriptors,
        org.graalvm.compiler.hotspot.CompilerConfigurationFactory_OptionDescriptors,
        org.graalvm.compiler.hotspot.debug.BenchmarkCounters_OptionDescriptors,
        org.graalvm.compiler.hotspot.HotSpotBackend_OptionDescriptors,
        org.graalvm.compiler.hotspot.HotSpotGraalCompilerFactory_OptionDescriptors,
        org.graalvm.compiler.hotspot.HotSpotTTYStreamProvider_OptionDescriptors,
        org.graalvm.compiler.hotspot.meta.HotSpotAOTProfilingPlugin_OptionDescriptors,
        org.graalvm.compiler.hotspot.meta.HotSpotProfilingPlugin_OptionDescriptors,
        org.graalvm.compiler.hotspot.nodes.profiling.ProfileNode_OptionDescriptors,
        org.graalvm.compiler.hotspot.phases.aot.AOTInliningPolicy_OptionDescriptors,
        org.graalvm.compiler.hotspot.phases.OnStackReplacementPhase_OptionDescriptors,
        org.graalvm.compiler.hotspot.phases.profiling.FinalizeProfileNodesPhase_OptionDescriptors,
        org.graalvm.compiler.hotspot.replacements.HotspotSnippetsOptions_OptionDescriptors,
        org.graalvm.compiler.hotspot.stubs.StubOptions_OptionDescriptors,
        org.graalvm.compiler.java.BytecodeParserOptions_OptionDescriptors,
        org.graalvm.compiler.lir.alloc.lsra.LinearScan_OptionDescriptors,
        org.graalvm.compiler.lir.alloc.lsra.LinearScanEliminateSpillMovePhase_OptionDescriptors,
        org.graalvm.compiler.lir.alloc.lsra.OptimizingLinearScanWalker_OptionDescriptors,
        org.graalvm.compiler.lir.alloc.trace.DefaultTraceRegisterAllocationPolicy_OptionDescriptors,
        org.graalvm.compiler.lir.alloc.trace.lsra.TraceLinearScanPhase_OptionDescriptors,
        org.graalvm.compiler.lir.alloc.trace.TraceBuilderPhase_OptionDescriptors,
        org.graalvm.compiler.lir.alloc.trace.TraceRegisterAllocationPhase_OptionDescriptors,
        org.graalvm.compiler.lir.amd64.phases.StackMoveOptimizationPhase_OptionDescriptors,
        org.graalvm.compiler.lir.asm.CompilationResultBuilder_OptionDescriptors,
        org.graalvm.compiler.lir.BailoutAndRestartBackendException_OptionDescriptors,
        org.graalvm.compiler.lir.constopt.ConstantLoadOptimization_OptionDescriptors,
        org.graalvm.compiler.lir.gen.LIRGenerator_OptionDescriptors,
        org.graalvm.compiler.lir.phases.LIRPhase_OptionDescriptors,
        org.graalvm.compiler.lir.phases.PostAllocationOptimizationStage_OptionDescriptors,
        org.graalvm.compiler.lir.profiling.MoveProfilingPhase_OptionDescriptors,
        org.graalvm.compiler.lir.stackslotalloc.LSStackSlotAllocator_OptionDescriptors,
        org.graalvm.compiler.loop.DefaultLoopPolicies_OptionDescriptors,
        org.graalvm.compiler.nodes.util.GraphUtil_OptionDescriptors,
        org.graalvm.compiler.phases.BasePhase_OptionDescriptors,
        org.graalvm.compiler.phases.common.DeadCodeEliminationPhase_OptionDescriptors,
        org.graalvm.compiler.phases.common.inlining.InliningPhase_OptionDescriptors,
        org.graalvm.compiler.phases.common.NodeCounterPhase_OptionDescriptors,
        org.graalvm.compiler.phases.common.UseTrappingNullChecksPhase_OptionDescriptors,
        org.graalvm.compiler.printer.NoDeadCodeVerifyHandler_OptionDescriptors,
        org.graalvm.compiler.replacements.PEGraphDecoder_OptionDescriptors,
        org.graalvm.compiler.replacements.SnippetTemplate_OptionDescriptors,
        org.graalvm.compiler.virtual.phases.ea.PartialEscapePhase_OptionDescriptors;
}
