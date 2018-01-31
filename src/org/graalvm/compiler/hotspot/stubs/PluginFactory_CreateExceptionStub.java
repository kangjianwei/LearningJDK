// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// GENERATORS: org.graalvm.compiler.replacements.processor.ReplacementsAnnotationProcessor, org.graalvm.compiler.replacements.processor.PluginGenerator
package org.graalvm.compiler.hotspot.stubs;

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

public class PluginFactory_CreateExceptionStub implements NodeIntrinsicPluginFactory {

    //        class: org.graalvm.compiler.hotspot.stubs.CreateExceptionStub
    //       method: getInternalClassName(java.lang.Class<?>)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class CreateExceptionStub_getInternalClassName extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            @SuppressWarnings({"rawtypes"})
            java.lang.Class arg0;
            if (args[0].isConstant()) {
                arg0 = snippetReflection.asObject(java.lang.Class.class, args[0].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            java.lang.String result = org.graalvm.compiler.hotspot.stubs.CreateExceptionStub.getInternalClassName(arg0);
            JavaConstant constant = b.getConstantReflection().forString(result);
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

        private CreateExceptionStub_getInternalClassName(InjectionProvider injection) {
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.stubs.CreateExceptionStub
    //       method: throwAndPostJvmtiException(org.graalvm.compiler.core.common.spi.ForeignCallDescriptor,org.graalvm.compiler.word.Word,org.graalvm.compiler.word.Word,org.graalvm.compiler.word.Word)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class CreateExceptionStub_throwAndPostJvmtiException extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            org.graalvm.compiler.core.common.spi.ForeignCallsProvider arg0 = injectedForeignCallsProvider;
            org.graalvm.compiler.core.common.type.Stamp arg1 = stamp;
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
            org.graalvm.compiler.hotspot.nodes.StubForeignCallNode node = new org.graalvm.compiler.hotspot.nodes.StubForeignCallNode(arg0, arg1, arg2, arg3, arg4, arg5);
            b.add(node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.core.common.spi.ForeignCallsProvider injectedForeignCallsProvider;
        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;
        private final org.graalvm.compiler.core.common.type.Stamp stamp;

        private CreateExceptionStub_throwAndPostJvmtiException(InjectionProvider injection) {
            this.injectedForeignCallsProvider = injection.getInjectedArgument(org.graalvm.compiler.core.common.spi.ForeignCallsProvider.class);
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
            this.stamp = injection.getInjectedStamp(void.class, false);
        }
    }

    //        class: org.graalvm.compiler.hotspot.stubs.CreateExceptionStub
    //       method: throwClassCastException(org.graalvm.compiler.core.common.spi.ForeignCallDescriptor,org.graalvm.compiler.word.Word,org.graalvm.compiler.word.Word,org.graalvm.compiler.hotspot.word.KlassPointer,org.graalvm.compiler.hotspot.word.KlassPointer)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class CreateExceptionStub_throwClassCastException extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            org.graalvm.compiler.core.common.spi.ForeignCallsProvider arg0 = injectedForeignCallsProvider;
            org.graalvm.compiler.core.common.type.Stamp arg1 = stamp;
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
            org.graalvm.compiler.hotspot.nodes.StubForeignCallNode node = new org.graalvm.compiler.hotspot.nodes.StubForeignCallNode(arg0, arg1, arg2, arg3, arg4, arg5, arg6);
            b.add(node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.core.common.spi.ForeignCallsProvider injectedForeignCallsProvider;
        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;
        private final org.graalvm.compiler.core.common.type.Stamp stamp;

        private CreateExceptionStub_throwClassCastException(InjectionProvider injection) {
            this.injectedForeignCallsProvider = injection.getInjectedArgument(org.graalvm.compiler.core.common.spi.ForeignCallsProvider.class);
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
            this.stamp = injection.getInjectedStamp(void.class, false);
        }
    }

    //        class: org.graalvm.compiler.hotspot.stubs.CreateExceptionStub
    //       method: throwKlassExternalNameException(org.graalvm.compiler.core.common.spi.ForeignCallDescriptor,org.graalvm.compiler.word.Word,org.graalvm.compiler.word.Word,org.graalvm.compiler.hotspot.word.KlassPointer)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class CreateExceptionStub_throwKlassExternalNameException extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            org.graalvm.compiler.core.common.spi.ForeignCallsProvider arg0 = injectedForeignCallsProvider;
            org.graalvm.compiler.core.common.type.Stamp arg1 = stamp;
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
            org.graalvm.compiler.hotspot.nodes.StubForeignCallNode node = new org.graalvm.compiler.hotspot.nodes.StubForeignCallNode(arg0, arg1, arg2, arg3, arg4, arg5);
            b.add(node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.core.common.spi.ForeignCallsProvider injectedForeignCallsProvider;
        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;
        private final org.graalvm.compiler.core.common.type.Stamp stamp;

        private CreateExceptionStub_throwKlassExternalNameException(InjectionProvider injection) {
            this.injectedForeignCallsProvider = injection.getInjectedArgument(org.graalvm.compiler.core.common.spi.ForeignCallsProvider.class);
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
            this.stamp = injection.getInjectedStamp(void.class, false);
        }
    }

    @Override
    public void registerPlugins(InvocationPlugins plugins, InjectionProvider injection) {
        plugins.register(new CreateExceptionStub_getInternalClassName(injection), org.graalvm.compiler.hotspot.stubs.CreateExceptionStub.class, "getInternalClassName", java.lang.Class.class);
        plugins.register(new CreateExceptionStub_throwAndPostJvmtiException(injection), org.graalvm.compiler.hotspot.stubs.CreateExceptionStub.class, "throwAndPostJvmtiException", org.graalvm.compiler.core.common.spi.ForeignCallDescriptor.class, org.graalvm.compiler.word.Word.class, org.graalvm.compiler.word.Word.class, org.graalvm.compiler.word.Word.class);
        plugins.register(new CreateExceptionStub_throwClassCastException(injection), org.graalvm.compiler.hotspot.stubs.CreateExceptionStub.class, "throwClassCastException", org.graalvm.compiler.core.common.spi.ForeignCallDescriptor.class, org.graalvm.compiler.word.Word.class, org.graalvm.compiler.word.Word.class, org.graalvm.compiler.hotspot.word.KlassPointer.class, org.graalvm.compiler.hotspot.word.KlassPointer.class);
        plugins.register(new CreateExceptionStub_throwKlassExternalNameException(injection), org.graalvm.compiler.hotspot.stubs.CreateExceptionStub.class, "throwKlassExternalNameException", org.graalvm.compiler.core.common.spi.ForeignCallDescriptor.class, org.graalvm.compiler.word.Word.class, org.graalvm.compiler.word.Word.class, org.graalvm.compiler.hotspot.word.KlassPointer.class);
    }
}
