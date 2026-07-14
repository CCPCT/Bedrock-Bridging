package CCPCT.bedrock_bridging.mixin;

import CCPCT.bedrock_bridging.Bedrock_bridging;
import CCPCT.bedrock_bridging.modConfig.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    @Shadow
    private static HitResult pick(Entity cameraEntity, double blockInteractionRange, double entityInteractionRange, float partialTicks) {
        throw new AssertionError();
    }

    @Shadow
    private static HitResult filterHitResult(HitResult hitResult, Vec3 from, double maxRange) {
        throw new AssertionError();
    }

    @Unique
    private static BlockHitResult traceStraightDown(LocalPlayer player, float partialTicks) {
        // 1. Start exactly at the player's eye level
        Vec3 eyePosition = player.getEyePosition(partialTicks);
        Vec2 eyeRotation = player.getRotationVector();

        // 2. Project the end position exactly 2.0 blocks straight down (subtracting from Y)
        double reach = 2.0D;
        Vec3 traceEnd = new Vec3(eyePosition.x, eyePosition.y - reach, eyePosition.z);

        // 3. Build a ClipContext configuring exactly how to evaluate blocks
        ClipContext context = new ClipContext(
                eyePosition,
                traceEnd,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        );

        // 4. Run the clip search against the level world data
        BlockHitResult raw = player.level().clip(context);

        if (raw.getType() == HitResult.Type.MISS) {
            Bedrock_bridging.magicSelect = false;
            return raw;
        }

        Bedrock_bridging.magicSelect = true;

        Vec3 exact = new Vec3(0,raw.getLocation().y(), 0);

        float playerYaw = (eyeRotation.y + 180) % 360;
        if (playerYaw < 0) {
            playerYaw += 360;
        }

        Direction direction;

        if (playerYaw >= 315 || playerYaw < 45) {
            direction = Direction.NORTH;
            exact = exact.add(player.getX(), 0, player.getBlockZ() + 1);
        } else if (playerYaw >= 45 && playerYaw < 135) {
            direction = Direction.EAST;
            exact = exact.add(player.getBlockX(), 0, player.getZ());
        } else if (playerYaw >= 135 && playerYaw < 225) {
            direction = Direction.SOUTH;
            exact = exact.add(player.getX(), 0, player.getBlockZ());
        } else { // 225 to 315
            direction = Direction.WEST;
            exact = exact.add(player.getBlockX() + 1, 0, player.getZ());
        }

        return new BlockHitResult(
                exact,
                direction,
                raw.getBlockPos(),
                false
        );
    }

    @Inject(method = "raycastHitResult", at = @At("HEAD"), cancellable = true)
    public void raycastHitResult(float a, Entity cameraEntity, CallbackInfoReturnable<HitResult> cir) {
        // A. Grab the standard ranges
        if (!ModConfig.get().modEnabled || !(cameraEntity instanceof LocalPlayer player)) {
            Bedrock_bridging.magicSelect = false;
            return;
        }

        if (Bedrock_bridging.magicDirection != null) {
            // if auto placement is on, update hit result there
            cir.setReturnValue(Minecraft.getInstance().hitResult);
        }

        ItemStack itemStack = player.getActiveItem();

        AttackRange itemAttackRange;

        float reach = ModConfig.get().reach;
        if (reach != -1) {
            itemAttackRange = new AttackRange(0f, reach, 0f, reach, 0f, 1f);
        } else {
            itemAttackRange = (AttackRange) itemStack.get(DataComponents.ATTACK_RANGE);
        }

        double blockInteractionRange = player.blockInteractionRange();
        HitResult hitResult = null;

        if (itemAttackRange != null) {
            hitResult = itemAttackRange.getClosesetHit(cameraEntity, a, EntitySelector.CAN_BE_PICKED);
            if (hitResult instanceof BlockHitResult) {
                hitResult = filterHitResult(hitResult, cameraEntity.getEyePosition(a), blockInteractionRange);
            }
        }


        if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
            double entityInteractionRange = player.entityInteractionRange();
            hitResult = pick(cameraEntity, blockInteractionRange, entityInteractionRange, a);
        }

        if (hitResult.getType() == HitResult.Type.MISS) {

            Vec2 lookAngle = player.getRotationVector();
            if (lookAngle.x < 45) {
                Bedrock_bridging.magicSelect = false;
            } else {
                hitResult = traceStraightDown(player, a);
            }
        } else {
            Bedrock_bridging.magicSelect = false;
        }

        cir.setReturnValue(hitResult);
    }

    @ModifyVariable(
            method = "filterHitResult(Lnet/minecraft/world/phys/HitResult;Lnet/minecraft/world/phys/Vec3;D)Lnet/minecraft/world/phys/HitResult;",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private static double setReach(double maxRange) {
        // If your bedrock bridging mod is turned on, we can fake a longer reach!
        if (ModConfig.get().modEnabled && ModConfig.get().reach!=-1f) {
            return ModConfig.get().reach;
        }
        return maxRange;
    }

}
