// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// GENERATORS: org.graalvm.compiler.replacements.processor.ReplacementsAnnotationProcessor, org.graalvm.compiler.replacements.processor.PluginGenerator
package org.graalvm.compiler.replacements.amd64;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import java.lang.annotation.Annotation;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.graphbuilderconf.GraphBuilderContext;
import org.graalvm.compiler.nodes.graphbuilderconf.GeneratedInvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugins;
import org.graalvm.compiler.nodes.graphbuilderconf.NodeIntrinsicPluginFactory;

import jdk.vm.ci.meta.JavaKind;

public class PluginFactory_AMD64StringIndexOfNode implements NodeIntrinsicPluginFactory {

    //        class: org.graalvm.compiler.replacements.amd64.AMD64StringIndexOfNode
    //       method: optimizedStringIndexPointer(jdk.internal.vm.compiler.word.Pointer,int,jdk.internal.vm.compiler.word.Pointer,int)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class AMD64StringIndexOfNode_optimizedStringIndexPointer extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            ValueNode arg0 = args[0];
            ValueNode arg1 = args[1];
            ValueNode arg2 = args[2];
            ValueNode arg3 = args[3];
            org.graalvm.compiler.replacements.amd64.AMD64StringIndexOfNode node = new org.graalvm.compiler.replacements.amd64.AMD64StringIndexOfNode(arg0, arg1, arg2, arg3);
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
        plugins.register(new AMD64StringIndexOfNode_optimizedStringIndexPointer(), org.graalvm.compiler.replacements.amd64.AMD64StringIndexOfNode.class, "optimizedStringIndexPointer", jdk.internal.vm.compiler.word.Pointer.class, int.class, jdk.internal.vm.compiler.word.Pointer.class, int.class);
    }
}
