// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// GENERATORS: org.graalvm.compiler.replacements.processor.ReplacementsAnnotationProcessor, org.graalvm.compiler.replacements.processor.PluginGenerator
package org.graalvm.compiler.replacements.nodes;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import java.lang.annotation.Annotation;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.graphbuilderconf.GraphBuilderContext;
import org.graalvm.compiler.nodes.graphbuilderconf.GeneratedInvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugins;
import org.graalvm.compiler.nodes.graphbuilderconf.NodeIntrinsicPluginFactory;

import jdk.vm.ci.meta.JavaKind;

public class PluginFactory_BitScanForwardNode implements NodeIntrinsicPluginFactory {

    //        class: org.graalvm.compiler.replacements.nodes.BitScanForwardNode
    //       method: unsafeScan(int)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class BitScanForwardNode_unsafeScan_int extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            ValueNode arg0 = args[0];
            org.graalvm.compiler.replacements.nodes.BitScanForwardNode node = new org.graalvm.compiler.replacements.nodes.BitScanForwardNode(arg0);
            b.addPush(JavaKind.Int, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }
    }

    //        class: org.graalvm.compiler.replacements.nodes.BitScanForwardNode
    //       method: unsafeScan(long)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class BitScanForwardNode_unsafeScan_long extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            ValueNode arg0 = args[0];
            org.graalvm.compiler.replacements.nodes.BitScanForwardNode node = new org.graalvm.compiler.replacements.nodes.BitScanForwardNode(arg0);
            b.addPush(JavaKind.Int, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }
    }

    @Override
    public void registerPlugins(InvocationPlugins plugins, InjectionProvider injection) {
        plugins.register(new BitScanForwardNode_unsafeScan_int(), org.graalvm.compiler.replacements.nodes.BitScanForwardNode.class, "unsafeScan", int.class);
        plugins.register(new BitScanForwardNode_unsafeScan_long(), org.graalvm.compiler.replacements.nodes.BitScanForwardNode.class, "unsafeScan", long.class);
    }
}
