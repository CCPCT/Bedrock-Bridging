package CCPCT.bedrock_bridging.mixin;


import CCPCT.bedrock_bridging.Bedrock_bridging;
import CCPCT.bedrock_bridging.modConfig.ModConfig;
import com.terraformersmc.modmenu.util.mod.Mod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;
import java.util.Optional;

import static CCPCT.bedrock_bridging.Bedrock_bridging.*;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Shadow
    private int rightClickDelay;

    @Shadow
    @Nullable
    public HitResult hitResult;

    @Shadow
    @Nullable
    public LocalPlayer player;

    @Shadow
    @Nullable
    public MultiPlayerGameMode gameMode;

    @Shadow
    @Final
    public GameRenderer gameRenderer;

    @Unique
    private static Vec3 getHitVecFromPositions(BlockPos lastPlacePos, BlockPos target) {
        Vec3 centre = lastPlacePos.getCenter().lerp(target.getCenter(), 0.5);
        if (lastPlacePos.getY() == target.getY()) {
            return centre.with(Direction.Axis.Y, magicY);
        } else {
            return centre;
        }
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void onUseItem(CallbackInfo ci) {
        if (!ModConfig.get().modEnabled || player==null) return;
        Minecraft client = Minecraft.getInstance();
        if (ModConfig.get().debug) {
//            player.sendSystemMessage(Component.literal("called start use item"+client.getFrameTimeNs()));
//            player.sendOverlayMessage(Component.literal("prep "+prepareMagic+" dire "+(magicDirection!=null)));
        }


        if (!prepareMagic || (hitResult!=null && hitResult.getType() == HitResult.Type.ENTITY)) return;

        if (magicDirection != null) {
            if (ModConfig.get().debug) System.out.println("do 2");
            BlockPos target = lastPlacePos.offset(magicDirection);
            AABB box = new AABB(target.getX(),target.getY(),target.getZ(),target.getX()+1,target.getY()+1,target.getZ()+1);
            float partialTick = client.getDeltaTracker().getGameTimeDeltaTicks();
            Vec3 start = player.getEyePosition(partialTick);
            Vec3 lookDirection = player.getHeadLookAngle();
            Vec3 end = start.add(lookDirection.scale(ModConfig.get().reach==-1f ? 4.5 : ModConfig.get().reach));

            // if (ModConfig.get().debug) player.sendSystemMessage(Component.literal("last: "+lastPlacePos+" dir: "+magicDirection.toShortString()+" target: "+target.toShortString()));

            Optional<Vec3> clip = box.clip(start, end);
            if (clip.isPresent()) {

                BlockHitResult worldClip = player.level().clip(new ClipContext(
                        start,
                        end,
                        ClipContext.Block.OUTLINE,
                        ClipContext.Fluid.NONE,
                        player
                ));

                if (worldClip.getType()== HitResult.Type.MISS || clip.get().closerThan(player.getEyePosition(), worldClip.getLocation().distanceTo(player.getEyePosition()))) {

                } else {
                    // blocked
                    ci.cancel();
                    return;
                }

                BlockHitResult blockHit = new BlockHitResult(getHitVecFromPositions(lastPlacePos,target), Direction.getNearest(magicDirection, null), target, false);

                if (!placeBlock(ci, blockHit)) return;

            }
            ci.cancel();
            return;
        }

        if (ModConfig.get().debug) System.out.println("do 1");

        Vec3 newVec = player.position().add(lastPlacePosRelative);
        BlockPos newBlock = new BlockPos((int)Math.floor(newVec.x),(int)Math.floor(newVec.y),(int)Math.floor(newVec.z));
//        if (ModConfig.get().debug) player.sendSystemMessage(Component.literal("new block "+ newBlock.toShortString()));

        if (lastPlacePos.distManhattan(newBlock) == 1) {
            //gut
            if (!Minecraft.getInstance().level.getBlockState(newBlock).canBeReplaced()) {
                if (ModConfig.get().debug) player.sendSystemMessage(Component.literal("blocked"));
                return;
            }
            if (ModConfig.get().debug) player.sendSystemMessage(Component.literal("gut"));
            magicDirection = newBlock.subtract(lastPlacePos);
            hitResult = new BlockHitResult(getHitVecFromPositions(lastPlacePos,newBlock), Direction.getNearest(magicDirection, null), newBlock, false);
            if (placeBlock(ci, (BlockHitResult) hitResult)) {
                ci.cancel();
            }
        } else if (hitResult instanceof BlockHitResult bhr) {
            newBlock = bhr.getBlockPos().relative(bhr.getDirection());
            if (lastPlacePos.distManhattan(newBlock) == 1) {
                if (ModConfig.get().debug) player.sendSystemMessage(Component.literal("nicht so gut"));
                magicDirection = newBlock.subtract(lastPlacePos);
            } else {
                if (ModConfig.get().debug) player.sendSystemMessage(Component.literal("schlecht"));
            }
            lastPlacePos = newBlock;
        } else {
            prepareMagic = false;
        }
    }

    @Unique
    private boolean placeBlock(CallbackInfo ci, BlockHitResult blockHit) {
        assert gameMode != null;
        assert player != null;

        // return success?
        BlockPos target = blockHit.getBlockPos();

        ItemStack heldItem = player.getMainHandItem();
        if (!(heldItem.getItem() instanceof BlockItem)) {
            ci.cancel();
            return false;
        }

        int oldCount = heldItem.getCount();
        InteractionHand hand = InteractionHand.MAIN_HAND;

        // place block
        InteractionResult useResult = gameMode.useItemOn(player, hand, blockHit);

        if (useResult instanceof InteractionResult.Success success) {
            if (success.swingSource() == InteractionResult.SwingSource.CLIENT) {
                player.swing(hand);
                if (!heldItem.isEmpty() && (heldItem.getCount() != oldCount || player.hasInfiniteMaterials())) {
                    gameRenderer.itemInHandRenderer.itemUsed(hand);
                }
            }

            lastPlacePos = target;

        }
        return true;
    }

    @Inject(method = "startUseItem", at = @At("RETURN"))
    private void endUseItem(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();

        if (client.gameMode == null) return;

        if (!ModConfig.get().modEnabled || client.gameMode.isDestroying()) return;

        System.out.println("do end");

        rightClickDelay = ModConfig.get().placementInterval;


        if (client.player == null || hitResult == null || !(hitResult instanceof BlockHitResult bhr)) {
            return;
        }
        if (magicDirection != null) {
            return;
        }
        lastPlacePos = bhr.getBlockPos().relative(bhr.getDirection());
        lastPlacePosRelative = lastPlacePos.getCenter().subtract(client.player.position());
        prepareMagic = true;
        magicY = hitResult.getLocation().y;
    }

}
