// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// GENERATORS: org.graalvm.compiler.replacements.processor.ReplacementsAnnotationProcessor, org.graalvm.compiler.replacements.processor.PluginGenerator
package org.graalvm.compiler.nodes.extended;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import java.lang.annotation.Annotation;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.graphbuilderconf.GraphBuilderContext;
import org.graalvm.compiler.nodes.graphbuilderconf.GeneratedInvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugins;
import org.graalvm.compiler.nodes.graphbuilderconf.NodeIntrinsicPluginFactory;

import jdk.vm.ci.meta.JavaKind;

public class PluginFactory_RawLoadNode implements NodeIntrinsicPluginFactory {

    //        class: org.graalvm.compiler.nodes.extended.RawLoadNode
    //       method: load(java.lang.Object,long,jdk.vm.ci.meta.JavaKind,jdk.internal.vm.compiler.word.LocationIdentity)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class RawLoadNode_load extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            ValueNode arg0 = args[0];
            ValueNode arg1 = args[1];
            jdk.vm.ci.meta.JavaKind arg2;
            if (args[2].isConstant()) {
                arg2 = snippetReflection.asObject(jdk.vm.ci.meta.JavaKind.class, args[2].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            jdk.internal.vm.compiler.word.LocationIdentity arg3;
            if (args[3].isConstant()) {
                arg3 = snippetReflection.asObject(jdk.internal.vm.compiler.word.LocationIdentity.class, args[3].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            org.graalvm.compiler.nodes.extended.RawLoadNode node = new org.graalvm.compiler.nodes.extended.RawLoadNode(arg0, arg1, arg2, arg3);
            b.addPush(JavaKind.Object, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;

        private RawLoadNode_load(InjectionProvider injection) {
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
        }
    }

    @Override
    public void registerPlugins(InvocationPlugins plugins, InjectionProvider injection) {
        plugins.register(new RawLoadNode_load(injection), org.graalvm.compiler.nodes.extended.RawLoadNode.class, "load", java.lang.Object.class, long.class, jdk.vm.ci.meta.JavaKind.class, jdk.internal.vm.compiler.word.LocationIdentity.class);
    }
}
