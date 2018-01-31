// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// GENERATORS: org.graalvm.compiler.replacements.processor.ReplacementsAnnotationProcessor, org.graalvm.compiler.replacements.processor.PluginGenerator
package org.graalvm.compiler.hotspot.replacements;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import java.lang.annotation.Annotation;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.graphbuilderconf.GraphBuilderContext;
import org.graalvm.compiler.nodes.graphbuilderconf.GeneratedInvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugins;
import org.graalvm.compiler.nodes.graphbuilderconf.NodeIntrinsicPluginFactory;

import jdk.vm.ci.meta.JavaKind;

public class PluginFactory_HubGetClassNode implements NodeIntrinsicPluginFactory {

    //        class: org.graalvm.compiler.hotspot.replacements.HubGetClassNode
    //       method: readClass(org.graalvm.compiler.hotspot.word.KlassPointer)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class HubGetClassNode_readClass extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            jdk.vm.ci.meta.MetaAccessProvider arg0 = b.getMetaAccess();
            ValueNode arg1 = args[0];
            org.graalvm.compiler.hotspot.replacements.HubGetClassNode node = new org.graalvm.compiler.hotspot.replacements.HubGetClassNode(arg0, arg1);
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
        plugins.register(new HubGetClassNode_readClass(), org.graalvm.compiler.hotspot.replacements.HubGetClassNode.class, "readClass", org.graalvm.compiler.hotspot.word.KlassPointer.class);
    }
}
