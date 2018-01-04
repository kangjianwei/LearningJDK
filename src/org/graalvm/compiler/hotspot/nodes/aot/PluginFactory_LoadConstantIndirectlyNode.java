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

public class PluginFactory_LoadConstantIndirectlyNode implements NodeIntrinsicPluginFactory {

    //        class: org.graalvm.compiler.hotspot.nodes.aot.LoadConstantIndirectlyNode
    //       method: loadKlass(org.graalvm.compiler.hotspot.word.KlassPointer)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class LoadConstantIndirectlyNode_loadKlass_KlassPointer extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            ValueNode arg0 = args[0];
            org.graalvm.compiler.hotspot.nodes.aot.LoadConstantIndirectlyNode node = new org.graalvm.compiler.hotspot.nodes.aot.LoadConstantIndirectlyNode(arg0);
            b.addPush(JavaKind.Object, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }
    }

    //        class: org.graalvm.compiler.hotspot.nodes.aot.LoadConstantIndirectlyNode
    //       method: loadKlass(org.graalvm.compiler.hotspot.word.KlassPointer,org.graalvm.compiler.hotspot.meta.HotSpotConstantLoadAction)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class LoadConstantIndirectlyNode_loadKlass_KlassPointer_HotSpotConstantLoadAction extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            ValueNode arg0 = args[0];
            org.graalvm.compiler.hotspot.meta.HotSpotConstantLoadAction arg1;
            if (args[1].isConstant()) {
                arg1 = snippetReflection.asObject(org.graalvm.compiler.hotspot.meta.HotSpotConstantLoadAction.class, args[1].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            org.graalvm.compiler.hotspot.nodes.aot.LoadConstantIndirectlyNode node = new org.graalvm.compiler.hotspot.nodes.aot.LoadConstantIndirectlyNode(arg0, arg1);
            b.addPush(JavaKind.Object, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;

        private LoadConstantIndirectlyNode_loadKlass_KlassPointer_HotSpotConstantLoadAction(InjectionProvider injection) {
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
        }
    }

    //        class: org.graalvm.compiler.hotspot.nodes.aot.LoadConstantIndirectlyNode
    //       method: loadObject(java.lang.Object)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class LoadConstantIndirectlyNode_loadObject extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            ValueNode arg0 = args[0];
            org.graalvm.compiler.hotspot.nodes.aot.LoadConstantIndirectlyNode node = new org.graalvm.compiler.hotspot.nodes.aot.LoadConstantIndirectlyNode(arg0);
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
        plugins.register(new LoadConstantIndirectlyNode_loadKlass_KlassPointer(), org.graalvm.compiler.hotspot.nodes.aot.LoadConstantIndirectlyNode.class, "loadKlass", org.graalvm.compiler.hotspot.word.KlassPointer.class);
        plugins.register(new LoadConstantIndirectlyNode_loadKlass_KlassPointer_HotSpotConstantLoadAction(injection), org.graalvm.compiler.hotspot.nodes.aot.LoadConstantIndirectlyNode.class, "loadKlass", org.graalvm.compiler.hotspot.word.KlassPointer.class, org.graalvm.compiler.hotspot.meta.HotSpotConstantLoadAction.class);
        plugins.register(new LoadConstantIndirectlyNode_loadObject(), org.graalvm.compiler.hotspot.nodes.aot.LoadConstantIndirectlyNode.class, "loadObject", java.lang.Object.class);
    }
}
