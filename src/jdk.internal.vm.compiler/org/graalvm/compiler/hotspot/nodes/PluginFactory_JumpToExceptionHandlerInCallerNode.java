// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// GENERATORS: org.graalvm.compiler.replacements.processor.ReplacementsAnnotationProcessor, org.graalvm.compiler.replacements.processor.PluginGenerator
package org.graalvm.compiler.hotspot.nodes;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import java.lang.annotation.Annotation;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.graphbuilderconf.GraphBuilderContext;
import org.graalvm.compiler.nodes.graphbuilderconf.GeneratedInvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugins;
import org.graalvm.compiler.nodes.graphbuilderconf.NodeIntrinsicPluginFactory;

public class PluginFactory_JumpToExceptionHandlerInCallerNode implements NodeIntrinsicPluginFactory {

    //        class: org.graalvm.compiler.hotspot.nodes.JumpToExceptionHandlerInCallerNode
    //       method: jumpToExceptionHandlerInCaller(org.graalvm.compiler.word.Word,java.lang.Object,org.graalvm.compiler.word.Word)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class JumpToExceptionHandlerInCallerNode_jumpToExceptionHandlerInCaller extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            ValueNode arg0 = args[0];
            ValueNode arg1 = args[1];
            ValueNode arg2 = args[2];
            org.graalvm.compiler.hotspot.nodes.JumpToExceptionHandlerInCallerNode node = new org.graalvm.compiler.hotspot.nodes.JumpToExceptionHandlerInCallerNode(arg0, arg1, arg2);
            b.add(node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }
    }

    @Override
    public void registerPlugins(InvocationPlugins plugins, InjectionProvider injection) {
        plugins.register(new JumpToExceptionHandlerInCallerNode_jumpToExceptionHandlerInCaller(), org.graalvm.compiler.hotspot.nodes.JumpToExceptionHandlerInCallerNode.class, "jumpToExceptionHandlerInCaller", org.graalvm.compiler.word.Word.class, java.lang.Object.class, org.graalvm.compiler.word.Word.class);
    }
}
