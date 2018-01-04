// CheckStyle: stop header check
// CheckStyle: stop line length check
// GENERATED CONTENT - DO NOT EDIT
// GENERATORS: org.graalvm.compiler.replacements.processor.ReplacementsAnnotationProcessor, org.graalvm.compiler.replacements.processor.PluginGenerator
package org.graalvm.compiler.replacements.aarch64;

import jdk.vm.ci.meta.ResolvedJavaMethod;

import java.lang.annotation.Annotation;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.graphbuilderconf.GraphBuilderContext;
import org.graalvm.compiler.nodes.graphbuilderconf.GeneratedInvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugins;
import org.graalvm.compiler.nodes.graphbuilderconf.NodeIntrinsicPluginFactory;

import jdk.vm.ci.meta.JavaKind;

public class PluginFactory_AArch64IntegerArithmeticSnippets implements NodeIntrinsicPluginFactory {

    //        class: org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets
    //       method: safeDiv(int,int)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class AArch64IntegerArithmeticSnippets_safeDiv_int_int extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            ValueNode arg0 = args[0];
            ValueNode arg1 = args[1];
            org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets.SafeSignedDivNode node = new org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets.SafeSignedDivNode(arg0, arg1);
            b.addPush(JavaKind.Int, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }
    }

    //        class: org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets
    //       method: safeDiv(long,long)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class AArch64IntegerArithmeticSnippets_safeDiv_long_long extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            ValueNode arg0 = args[0];
            ValueNode arg1 = args[1];
            org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets.SafeSignedDivNode node = new org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets.SafeSignedDivNode(arg0, arg1);
            b.addPush(JavaKind.Long, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }
    }

    //        class: org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets
    //       method: safeRem(int,int)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class AArch64IntegerArithmeticSnippets_safeRem_int_int extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            ValueNode arg0 = args[0];
            ValueNode arg1 = args[1];
            org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets.SafeSignedRemNode node = new org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets.SafeSignedRemNode(arg0, arg1);
            b.addPush(JavaKind.Int, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }
    }

    //        class: org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets
    //       method: safeRem(long,long)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class AArch64IntegerArithmeticSnippets_safeRem_long_long extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            ValueNode arg0 = args[0];
            ValueNode arg1 = args[1];
            org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets.SafeSignedRemNode node = new org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets.SafeSignedRemNode(arg0, arg1);
            b.addPush(JavaKind.Long, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }
    }

    //        class: org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets
    //       method: safeUDiv(int,int)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class AArch64IntegerArithmeticSnippets_safeUDiv_int_int extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            ValueNode arg0 = args[0];
            ValueNode arg1 = args[1];
            org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets.SafeUnsignedDivNode node = new org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets.SafeUnsignedDivNode(arg0, arg1);
            b.addPush(JavaKind.Int, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }
    }

    //        class: org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets
    //       method: safeUDiv(long,long)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class AArch64IntegerArithmeticSnippets_safeUDiv_long_long extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            ValueNode arg0 = args[0];
            ValueNode arg1 = args[1];
            org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets.SafeUnsignedDivNode node = new org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets.SafeUnsignedDivNode(arg0, arg1);
            b.addPush(JavaKind.Long, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }
    }

    //        class: org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets
    //       method: safeURem(int,int)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class AArch64IntegerArithmeticSnippets_safeURem_int_int extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            ValueNode arg0 = args[0];
            ValueNode arg1 = args[1];
            org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets.SafeUnsignedRemNode node = new org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets.SafeUnsignedRemNode(arg0, arg1);
            b.addPush(JavaKind.Int, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }
    }

    //        class: org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets
    //       method: safeURem(long,long)
    // generated-by: org.graalvm.compiler.replacements.processor.GeneratedNodeIntrinsicPlugin$ConstructorPlugin
    private static final class AArch64IntegerArithmeticSnippets_safeURem_long_long extends GeneratedInvocationPlugin {

        @Override
        public boolean execute(GraphBuilderContext b, ResolvedJavaMethod targetMethod, InvocationPlugin.Receiver receiver, ValueNode[] args) {
            ValueNode arg0 = args[0];
            ValueNode arg1 = args[1];
            org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets.SafeUnsignedRemNode node = new org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets.SafeUnsignedRemNode(arg0, arg1);
            b.addPush(JavaKind.Long, node);
            return true;
        }
        @Override
        public Class<? extends Annotation> getSource() {
            return org.graalvm.compiler.graph.Node.NodeIntrinsic.class;
        }
    }

    @Override
    public void registerPlugins(InvocationPlugins plugins, InjectionProvider injection) {
        plugins.register(new AArch64IntegerArithmeticSnippets_safeDiv_int_int(), org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets.class, "safeDiv", int.class, int.class);
        plugins.register(new AArch64IntegerArithmeticSnippets_safeDiv_long_long(), org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets.class, "safeDiv", long.class, long.class);
        plugins.register(new AArch64IntegerArithmeticSnippets_safeRem_int_int(), org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets.class, "safeRem", int.class, int.class);
        plugins.register(new AArch64IntegerArithmeticSnippets_safeRem_long_long(), org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets.class, "safeRem", long.class, long.class);
        plugins.register(new AArch64IntegerArithmeticSnippets_safeUDiv_int_int(), org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets.class, "safeUDiv", int.class, int.class);
        plugins.register(new AArch64IntegerArithmeticSnippets_safeUDiv_long_long(), org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets.class, "safeUDiv", long.class, long.class);
        plugins.register(new AArch64IntegerArithmeticSnippets_safeURem_int_int(), org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets.class, "safeURem", int.class, int.class);
        plugins.register(new AArch64IntegerArithmeticSnippets_safeURem_long_long(), org.graalvm.compiler.replacements.aarch64.AArch64IntegerArithmeticSnippets.class, "safeURem", long.class, long.class);
    }
}
