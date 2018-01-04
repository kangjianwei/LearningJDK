// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// GENERATORS: org.graalvm.compiler.replacements.processor.ReplacementsAnnotationProcessor, org.graalvm.compiler.replacements.processor.PluginGenerator
package org.graalvm.compiler.hotspot.replacements;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import java.lang.annotation.Annotation;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.graphbuilderconf.GraphBuilderContext;
import org.graalvm.compiler.nodes.graphbuilderconf.GeneratedInvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugins;
import org.graalvm.compiler.nodes.graphbuilderconf.NodeIntrinsicPluginFactory;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaKind;
import org.graalvm.compiler.nodes.ConstantNode;

public class PluginFactory_CipherBlockChainingSubstitutions implements NodeIntrinsicPluginFactory {

    //        class: org.graalvm.compiler.hotspot.replacements.CipherBlockChainingSubstitutions
    //       method: decryptAESCryptStub(org.graalvm.compiler.core.common.spi.ForeignCallDescriptor,org.graalvm.compiler.word.Word,org.graalvm.compiler.word.Word,jdk.internal.vm.compiler.word.Pointer,jdk.internal.vm.compiler.word.Pointer,int)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$CustomFactoryPlugin
    private static final class CipherBlockChainingSubstitutions_decryptAESCryptStub extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            org.graalvm.compiler.core.common.type.Stamp arg0 = stamp;
            org.graalvm.compiler.core.common.spi.ForeignCallsProvider arg1 = injectedForeignCallsProvider;
            org.graalvm.compiler.core.common.spi.ForeignCallDescriptor arg2;
            if (args[0].isConstant()) {
                arg2 = snippetReflection.asObject(org.graalvm.compiler.core.common.spi.ForeignCallDescriptor.class, args[0].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            ValueNode arg3 = args[1];
            ValueNode arg4 = args[2];
            ValueNode arg5 = args[3];
            ValueNode arg6 = args[4];
            ValueNode arg7 = args[5];
            return org.graalvm.compiler.nodes.extended.ForeignCallNode.intrinsify(b, targetMethod, arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.core.common.spi.ForeignCallsProvider injectedForeignCallsProvider;
        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;
        private final org.graalvm.compiler.core.common.type.Stamp stamp;

        private CipherBlockChainingSubstitutions_decryptAESCryptStub(InjectionProvider injection) {
            this.injectedForeignCallsProvider = injection.getInjectedArgument(org.graalvm.compiler.core.common.spi.ForeignCallsProvider.class);
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
            this.stamp = injection.getInjectedStamp(void.class, false);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.CipherBlockChainingSubstitutions
    //       method: decryptAESCryptWithOriginalKeyStub(org.graalvm.compiler.core.common.spi.ForeignCallDescriptor,org.graalvm.compiler.word.Word,org.graalvm.compiler.word.Word,jdk.internal.vm.compiler.word.Pointer,jdk.internal.vm.compiler.word.Pointer,int,jdk.internal.vm.compiler.word.Pointer)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$CustomFactoryPlugin
    private static final class CipherBlockChainingSubstitutions_decryptAESCryptWithOriginalKeyStub extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            org.graalvm.compiler.core.common.type.Stamp arg0 = stamp;
            org.graalvm.compiler.core.common.spi.ForeignCallsProvider arg1 = injectedForeignCallsProvider;
            org.graalvm.compiler.core.common.spi.ForeignCallDescriptor arg2;
            if (args[0].isConstant()) {
                arg2 = snippetReflection.asObject(org.graalvm.compiler.core.common.spi.ForeignCallDescriptor.class, args[0].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            ValueNode arg3 = args[1];
            ValueNode arg4 = args[2];
            ValueNode arg5 = args[3];
            ValueNode arg6 = args[4];
            ValueNode arg7 = args[5];
            ValueNode arg8 = args[6];
            return org.graalvm.compiler.nodes.extended.ForeignCallNode.intrinsify(b, targetMethod, arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.core.common.spi.ForeignCallsProvider injectedForeignCallsProvider;
        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;
        private final org.graalvm.compiler.core.common.type.Stamp stamp;

        private CipherBlockChainingSubstitutions_decryptAESCryptWithOriginalKeyStub(InjectionProvider injection) {
            this.injectedForeignCallsProvider = injection.getInjectedArgument(org.graalvm.compiler.core.common.spi.ForeignCallsProvider.class);
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
            this.stamp = injection.getInjectedStamp(void.class, false);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.CipherBlockChainingSubstitutions
    //       method: encryptAESCryptStub(org.graalvm.compiler.core.common.spi.ForeignCallDescriptor,org.graalvm.compiler.word.Word,org.graalvm.compiler.word.Word,jdk.internal.vm.compiler.word.Pointer,jdk.internal.vm.compiler.word.Pointer,int)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$CustomFactoryPlugin
    private static final class CipherBlockChainingSubstitutions_encryptAESCryptStub extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            org.graalvm.compiler.core.common.type.Stamp arg0 = stamp;
            org.graalvm.compiler.core.common.spi.ForeignCallsProvider arg1 = injectedForeignCallsProvider;
            org.graalvm.compiler.core.common.spi.ForeignCallDescriptor arg2;
            if (args[0].isConstant()) {
                arg2 = snippetReflection.asObject(org.graalvm.compiler.core.common.spi.ForeignCallDescriptor.class, args[0].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            ValueNode arg3 = args[1];
            ValueNode arg4 = args[2];
            ValueNode arg5 = args[3];
            ValueNode arg6 = args[4];
            ValueNode arg7 = args[5];
            return org.graalvm.compiler.nodes.extended.ForeignCallNode.intrinsify(b, targetMethod, arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.core.common.spi.ForeignCallsProvider injectedForeignCallsProvider;
        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;
        private final org.graalvm.compiler.core.common.type.Stamp stamp;

        private CipherBlockChainingSubstitutions_encryptAESCryptStub(InjectionProvider injection) {
            this.injectedForeignCallsProvider = injection.getInjectedArgument(org.graalvm.compiler.core.common.spi.ForeignCallsProvider.class);
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
            this.stamp = injection.getInjectedStamp(void.class, false);
        }
    }

    //        class: org.graalvm.compiler.hotspot.replacements.CipherBlockChainingSubstitutions
    //       method: getAESCryptClass()
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class CipherBlockChainingSubstitutions_getAESCryptClass extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            @SuppressWarnings({"rawtypes"})
            java.lang.Class result = org.graalvm.compiler.hotspot.replacements.CipherBlockChainingSubstitutions.getAESCryptClass();
            JavaConstant constant = snippetReflection.forObject(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Object, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }

        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;

        private CipherBlockChainingSubstitutions_getAESCryptClass(InjectionProvider injection) {
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
        }
    }

    @Override
    public void registerPlugins(InvocationPlugins plugins, InjectionProvider injection) {
        plugins.register(new CipherBlockChainingSubstitutions_decryptAESCryptStub(injection), org.graalvm.compiler.hotspot.replacements.CipherBlockChainingSubstitutions.class, "decryptAESCryptStub", org.graalvm.compiler.core.common.spi.ForeignCallDescriptor.class, org.graalvm.compiler.word.Word.class, org.graalvm.compiler.word.Word.class, jdk.internal.vm.compiler.word.Pointer.class, jdk.internal.vm.compiler.word.Pointer.class, int.class);
        plugins.register(new CipherBlockChainingSubstitutions_decryptAESCryptWithOriginalKeyStub(injection), org.graalvm.compiler.hotspot.replacements.CipherBlockChainingSubstitutions.class, "decryptAESCryptWithOriginalKeyStub", org.graalvm.compiler.core.common.spi.ForeignCallDescriptor.class, org.graalvm.compiler.word.Word.class, org.graalvm.compiler.word.Word.class, jdk.internal.vm.compiler.word.Pointer.class, jdk.internal.vm.compiler.word.Pointer.class, int.class, jdk.internal.vm.compiler.word.Pointer.class);
        plugins.register(new CipherBlockChainingSubstitutions_encryptAESCryptStub(injection), org.graalvm.compiler.hotspot.replacements.CipherBlockChainingSubstitutions.class, "encryptAESCryptStub", org.graalvm.compiler.core.common.spi.ForeignCallDescriptor.class, org.graalvm.compiler.word.Word.class, org.graalvm.compiler.word.Word.class, jdk.internal.vm.compiler.word.Pointer.class, jdk.internal.vm.compiler.word.Pointer.class, int.class);
        plugins.register(new CipherBlockChainingSubstitutions_getAESCryptClass(injection), org.graalvm.compiler.hotspot.replacements.CipherBlockChainingSubstitutions.class, "getAESCryptClass");
    }
}
