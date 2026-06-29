package CCPCT.bedrock_bridging.mixin;


import CCPCT.bedrock_bridging.Bedrock_bridging;
import CCPCT.bedrock_bridging.modConfig.ModConfig;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    @Inject(
            method = "destroyBlock",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onBlockDestroyed(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (Bedrock_bridging.magicSelect) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "startDestroyBlock",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onStartBlockDestroy(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (Bedrock_bridging.magicSelect) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            method = "performUseItemOn",
            at = @At("HEAD")
    )
    private void onUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult blockHit, CallbackInfoReturnable<InteractionResult> cir) {
        if (ModConfig.get().debug) {
            player.sendSystemMessage(Component.literal(blockHit.getLocation().toString()));
        }
    }
}
