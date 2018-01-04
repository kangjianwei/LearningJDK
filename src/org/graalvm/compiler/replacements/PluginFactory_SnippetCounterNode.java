// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// GENERATORS: org.graalvm.compiler.replacements.processor.ReplacementsAnnotationProcessor, org.graalvm.compiler.replacements.processor.PluginGenerator
package org.graalvm.compiler.replacements;

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

public class PluginFactory_SnippetCounterNode implements NodeIntrinsicPluginFactory {

    //        class: org.graalvm.compiler.replacements.SnippetCounterNode
    //       method: add(org.graalvm.compiler.replacements.SnippetCounter,int)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class SnippetCounterNode_add extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            org.graalvm.compiler.replacements.SnippetCounter arg0;
            if (args[0].isConstant()) {
                arg0 = snippetReflection.asObject(org.graalvm.compiler.replacements.SnippetCounter.class, args[0].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            ValueNode arg1 = args[1];
            org.graalvm.compiler.replacements.SnippetCounterNode node = new org.graalvm.compiler.replacements.SnippetCounterNode(arg0, arg1);
            b.add(node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;

        private SnippetCounterNode_add(InjectionProvider injection) {
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
        }
    }

    //        class: org.graalvm.compiler.replacements.SnippetCounterNode.SnippetCounterSnippets
    //       method: countOffset()
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedFoldPlugin
    private static final class SnippetCounterSnippets_countOffset extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            int result = org.graalvm.compiler.replacements.SnippetCounterNode.SnippetCounterSnippets.countOffset();
            JavaConstant constant = JavaConstant.forInt(result);
            ConstantNode node = ConstantNode.forConstant(constant, b.getMetaAccess(), b.getGraph());
            b.push(JavaKind.Int, node);
            b.notifyReplacedCall(targetMethod, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.api.replacements.Fold.class;
        }
    }

    @Override
    public void registerPlugins(InvocationPlugins plugins, InjectionProvider injection) {
        plugins.register(new SnippetCounterNode_add(injection), org.graalvm.compiler.replacements.SnippetCounterNode.class, "add", org.graalvm.compiler.replacements.SnippetCounter.class, int.class);
        plugins.register(new SnippetCounterSnippets_countOffset(), org.graalvm.compiler.replacements.SnippetCounterNode.SnippetCounterSnippets.class, "countOffset");
    }
}
