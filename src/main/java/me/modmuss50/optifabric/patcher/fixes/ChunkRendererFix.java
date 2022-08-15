package me.modmuss50.optifabric.patcher.fixes;

import java.util.ListIterator;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import me.modmuss50.optifabric.util.RemappingUtils;

public class ChunkRendererFix implements ClassFixer {

	//This removes the small change that optifine made to ChunkRenderer that is only required by forge
	@Override
	public void fix(ClassNode classNode, ClassNode old) {
		for (MethodNode methodNode : classNode.methods) {
			for (ListIterator<AbstractInsnNode> it = methodNode.instructions.iterator(); it.hasNext();) {
				AbstractInsnNode insnNode = it.next();

				if (insnNode instanceof MethodInsnNode) {
					MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;

					if (methodInsnNode.getOpcode() == Opcodes.INVOKEVIRTUAL) {
						if ("renderModel".equals(methodInsnNode.name) || "renderBatched".equals(methodInsnNode.name)) {
							/* For reference:

							class_2680 - net/minecraft/block/BlockState
							class_2338 - net/minecraft/util/math/BlockPos
							class_1920 - net/minecraft/world/ExtendedBlockView
							class_4587 - net/minecraft/client/util/math/MatrixStack
							class_4588 - net/minecraft/client/render/VertexConsumer
							class_5819 - net/minecraft/util/math/random/Random
							 */

							Type[] args = Type.getArgumentTypes(methodInsnNode.desc);
							boolean trailingRenderLayer = RemappingUtils.getClassName("class_1921").equals(args[args.length - 1].getInternalName());
							assert args.length > (trailingRenderLayer ? 4 : 3) && "Z".equals(args[args.length - (trailingRenderLayer ? 4 : 3)].getDescriptor());
							boolean nativeRandom = "java/util/Random".equals(args[args.length - (trailingRenderLayer ? 3 : 2)].getInternalName());
							String desc = "(Lnet/minecraft/class_2680;"
								+ "Lnet/minecraft/class_2338;"
								+ "Lnet/minecraft/class_1920;"
								+ "Lnet/minecraft/class_4587;"
								+ "Lnet/minecraft/class_4588;"
								+ "ZL" + (!nativeRandom ? RemappingUtils.getClassName("class_5819") + ";)V" : "java/util/Random;)Z");
							String name = RemappingUtils.getMethodName("class_776", "method_3355", desc);

							System.out.println(String.format("Replacement `renderBlock` call:  %s.%s", name, desc));
	
							//Replaces the method call with the vanilla one, this calls down to the same method just without the forge model data
							methodInsnNode.name = name;
							methodInsnNode.desc = RemappingUtils.mapMethodDescriptor(desc);

							it.previous();
							//Remove the model data local load call
							it.previous();
							it.remove();
							if (trailingRenderLayer) {//Remove the additional render layer
								it.previous();
								it.remove();
							}
							it.next();
						}
					}
				}
			}
		}
	}
}
