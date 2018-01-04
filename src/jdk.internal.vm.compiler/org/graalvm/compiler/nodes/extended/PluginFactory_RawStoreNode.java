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

public class PluginFactory_RawStoreNode implements NodeIntrinsicPluginFactory {

    //        class: org.graalvm.compiler.nodes.extended.RawStoreNode
    //       method: storeChar(java.lang.Object,long,char,jdk.vm.ci.meta.JavaKind,jdk.internal.vm.compiler.word.LocationIdentity)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class RawStoreNode_storeChar extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            ValueNode arg0 = args[0];
            ValueNode arg1 = args[1];
            ValueNode arg2 = args[2];
            jdk.vm.ci.meta.JavaKind arg3;
            if (args[3].isConstant()) {
                arg3 = snippetReflection.asObject(jdk.vm.ci.meta.JavaKind.class, args[3].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            jdk.internal.vm.compiler.word.LocationIdentity arg4;
            if (args[4].isConstant()) {
                arg4 = snippetReflection.asObject(jdk.internal.vm.compiler.word.LocationIdentity.class, args[4].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            org.graalvm.compiler.nodes.extended.RawStoreNode node = new org.graalvm.compiler.nodes.extended.RawStoreNode(arg0, arg1, arg2, arg3, arg4);
            b.addPush(JavaKind.Object, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;

        private RawStoreNode_storeChar(InjectionProvider injection) {
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
        }
    }

    //        class: org.graalvm.compiler.nodes.extended.RawStoreNode
    //       method: storeObject(java.lang.Object,long,java.lang.Object,jdk.vm.ci.meta.JavaKind,jdk.internal.vm.compiler.word.LocationIdentity,boolean)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class RawStoreNode_storeObject extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            ValueNode arg0 = args[0];
            ValueNode arg1 = args[1];
            ValueNode arg2 = args[2];
            jdk.vm.ci.meta.JavaKind arg3;
            if (args[3].isConstant()) {
                arg3 = snippetReflection.asObject(jdk.vm.ci.meta.JavaKind.class, args[3].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            jdk.internal.vm.compiler.word.LocationIdentity arg4;
            if (args[4].isConstant()) {
                arg4 = snippetReflection.asObject(jdk.internal.vm.compiler.word.LocationIdentity.class, args[4].asJavaConstant());
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            boolean arg5;
            if (args[5].isConstant()) {
                arg5 = args[5].asJavaConstant().asInt() != 0;
            } else {
                assert b.canDeferPlugin(this) : b.getClass().toString();
                return false;
            }
            org.graalvm.compiler.nodes.extended.RawStoreNode node = new org.graalvm.compiler.nodes.extended.RawStoreNode(arg0, arg1, arg2, arg3, arg4, arg5);
            b.addPush(JavaKind.Object, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }

        private final org.graalvm.compiler.api.replacements.SnippetReflectionProvider snippetReflection;

        private RawStoreNode_storeObject(InjectionProvider injection) {
            this.snippetReflection = injection.getInjectedArgument(org.graalvm.compiler.api.replacements.SnippetReflectionProvider.class);
        }
    }

    @Override
    public void registerPlugins(InvocationPlugins plugins, InjectionProvider injection) {
        plugins.register(new RawStoreNode_storeChar(injection), org.graalvm.compiler.nodes.extended.RawStoreNode.class, "storeChar", java.lang.Object.class, long.class, char.class, jdk.vm.ci.meta.JavaKind.class, jdk.internal.vm.compiler.word.LocationIdentity.class);
        plugins.register(new RawStoreNode_storeObject(injection), org.graalvm.compiler.nodes.extended.RawStoreNode.class, "storeObject", java.lang.Object.class, long.class, java.lang.Object.class, jdk.vm.ci.meta.JavaKind.class, jdk.internal.vm.compiler.word.LocationIdentity.class, boolean.class);
    }
}
