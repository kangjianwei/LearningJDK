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

public class PluginFactory_LoadMethodCountersIndirectlyNode implements NodeIntrinsicPluginFactory {

    //        class: org.graalvm.compiler.hotspot.nodes.aot.LoadMethodCountersIndirectlyNode
    //       method: loadMethodCounters(org.graalvm.compiler.hotspot.word.MethodPointer)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class LoadMethodCountersIndirectlyNode_loadMethodCounters extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            ValueNode arg0 = args[0];
            org.graalvm.compiler.hotspot.nodes.aot.LoadMethodCountersIndirectlyNode node = new org.graalvm.compiler.hotspot.nodes.aot.LoadMethodCountersIndirectlyNode(arg0);
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
        plugins.register(new LoadMethodCountersIndirectlyNode_loadMethodCounters(), org.graalvm.compiler.hotspot.nodes.aot.LoadMethodCountersIndirectlyNode.class, "loadMethodCounters", org.graalvm.compiler.hotspot.word.MethodPointer.class);
    }
}
