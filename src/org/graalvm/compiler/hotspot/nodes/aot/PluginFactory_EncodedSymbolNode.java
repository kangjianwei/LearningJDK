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

public class PluginFactory_EncodedSymbolNode implements NodeIntrinsicPluginFactory {

    //        class: org.graalvm.compiler.hotspot.nodes.aot.EncodedSymbolNode
    //       method: encode(java.lang.Object)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class EncodedSymbolNode_encode extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            org.graalvm.compiler.core.common.type.Stamp arg0 = stamp;
            ValueNode arg1 = args[0];
            org.graalvm.compiler.hotspot.nodes.aot.EncodedSymbolNode node = new org.graalvm.compiler.hotspot.nodes.aot.EncodedSymbolNode(arg0, arg1);
            b.addPush(JavaKind.Object, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.core.common.type.Stamp stamp;

        private EncodedSymbolNode_encode(InjectionProvider injection) {
            this.stamp = injection.getInjectedStamp(org.graalvm.compiler.word.Word.class, false);
        }
    }

    @Override
    public void registerPlugins(InvocationPlugins plugins, InjectionProvider injection) {
        plugins.register(new EncodedSymbolNode_encode(injection), org.graalvm.compiler.hotspot.nodes.aot.EncodedSymbolNode.class, "encode", java.lang.Object.class);
    }
}
