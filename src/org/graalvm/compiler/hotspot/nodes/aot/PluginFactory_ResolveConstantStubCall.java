// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// GENERATORS: org.graalvm.compiler.replacements.processor.ReplacementsAnnotationProcessor, org.graalvm.compiler.replacements.processor.PluginGenerator
package org.graalvm.compiler.hotspot.nodes.aot;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import java.lang.annotation.Annotation;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.graphbuilderconf.GraphBuilderContext;
import org.graalvm.compiler.nodes.graphbuilderconf.GeneratedInvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugins;
import org.graalvm.compiler.nodes.graphbuilderconf.NodeIntrinsicPluginFactory;

import jdk.vm.ci.meta.JavaKind;

public class PluginFactory_ResolveConstantStubCall implements NodeIntrinsicPluginFactory {

    //        class: org.graalvm.compiler.hotspot.nodes.aot.ResolveConstantStubCall
    //       method: resolveKlass(org.graalvm.compiler.hotspot.word.KlassPointer,java.lang.Object)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class ResolveConstantStubCall_resolveKlass_KlassPointer_Object extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            ValueNode arg0 = args[0];
            ValueNode arg1 = args[1];
            org.graalvm.compiler.hotspot.nodes.aot.ResolveConstantStubCall node = new org.graalvm.compiler.hotspot.nodes.aot.ResolveConstantStubCall(arg0, arg1);
            b.addPush(JavaKind.Object, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }
    }

    //        class: org.graalvm.compiler.hotspot.nodes.aot.ResolveConstantStubCall
    //       method: resolveKlass(org.graalvm.compiler.hotspot.word.KlassPointer,java.lang.Object,org.graalvm.compiler.hotspot.meta.HotSpotConstantLoadAction)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class ResolveConstantStubCall_resolveKlass_KlassPointer_Object_HotSpotConstantLoadAction extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            ValueNode arg0 = args[0];
            ValueNode arg1 = args[1];
            org.graalvm.compiler.hotspot.meta.HotSpotConstantLoadAction arg2;
            if (args[2].isConstant()) {
                arg2 = snippetReflection.asObject(org.graalvm.compiler.hotspot.meta.HotSpotConstantLoadAction.class, args[2].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            org.graalvm.compiler.hotspot.nodes.aot.ResolveConstantStubCall node = new org.graalvm.compiler.hotspot.nodes.aot.ResolveConstantStubCall(arg0, arg1, arg2);
            b.addPush(JavaKind.Object, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;

        private ResolveConstantStubCall_resolveKlass_KlassPointer_Object_HotSpotConstantLoadAction(InjectionProvider injection) {
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.nodes.aot.ResolveConstantStubCall
    //       method: resolveObject(java.lang.Object,java.lang.Object)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class ResolveConstantStubCall_resolveObject extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            ValueNode arg0 = args[0];
            ValueNode arg1 = args[1];
            org.graalvm.compiler.hotspot.nodes.aot.ResolveConstantStubCall node = new org.graalvm.compiler.hotspot.nodes.aot.ResolveConstantStubCall(arg0, arg1);
            b.addPush(JavaKind.Object, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }
    }

    @Override
    public void registerPlugins(InvocationPlugins plugins, InjectionProvider injection) {
        plugins.register(new ResolveConstantStubCall_resolveKlass_KlassPointer_Object(), org.graalvm.compiler.hotspot.nodes.aot.ResolveConstantStubCall.class, "resolveKlass", org.graalvm.compiler.hotspot.word.KlassPointer.class, java.lang.Object.class);
        plugins.register(new ResolveConstantStubCall_resolveKlass_KlassPointer_Object_HotSpotConstantLoadAction(injection), org.graalvm.compiler.hotspot.nodes.aot.ResolveConstantStubCall.class, "resolveKlass", org.graalvm.compiler.hotspot.word.KlassPointer.class, java.lang.Object.class, org.graalvm.compiler.hotspot.meta.HotSpotConstantLoadAction.class);
        plugins.register(new ResolveConstantStubCall_resolveObject(), org.graalvm.compiler.hotspot.nodes.aot.ResolveConstantStubCall.class, "resolveObject", java.lang.Object.class, java.lang.Object.class);
    }
}
