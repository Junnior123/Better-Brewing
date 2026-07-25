package kr.junior.brewing.mixin;

import kr.junior.brewing.BrewingClickHelper;
import net.minecraft.class_1703;
import net.minecraft.class_1735;
import net.minecraft.class_310;
import net.minecraft.class_437;
import net.minecraft.class_465;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({class_465.class})
public abstract class HandledScreenMixin {
   @Shadow
   @Final
   protected class_1703 field_2797;
   @Shadow
   protected int field_2776;
   @Shadow
   protected int field_2800;

   @Inject(
      method = {"handledScreenTick"},
      at = {@At("TAIL")}
   )
   private void betterbrewing$onHandledScreenTick(CallbackInfo ci) {
      BrewingClickHelper.onClientTick(class_310.method_1551());
   }

   @Inject(
      method = {"mouseClicked"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void betterbrewing$onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
      if (button == 1 && class_437.method_25442()) {
         class_1735 clickedSlot = this.betterbrewing$findSlotAt(mouseX, mouseY);
         if (BrewingClickHelper.handleShiftRightClick(class_310.method_1551(), this.field_2797, clickedSlot, button)) {
            cir.setReturnValue(true);
         }

      }
   }

   private class_1735 betterbrewing$findSlotAt(double mouseX, double mouseY) {
      int size = this.field_2797.method_7602().size();

      for(int slotId = 0; slotId < size; ++slotId) {
         class_1735 slot = this.field_2797.method_7611(slotId);
         if (slot != null && slot.method_7682() && mouseX >= (double)(this.field_2776 + slot.field_7873) && mouseX < (double)(this.field_2776 + slot.field_7873 + 16) && mouseY >= (double)(this.field_2800 + slot.field_7872) && mouseY < (double)(this.field_2800 + slot.field_7872 + 16)) {
            return slot;
         }
      }

      return null;
   }
}
