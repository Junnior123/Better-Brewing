package kr.junior.brewing;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import net.minecraft.class_1703;
import net.minecraft.class_1707;
import net.minecraft.class_1708;
import net.minecraft.class_1713;
import net.minecraft.class_1735;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_310;
import net.minecraft.class_472;

public final class BrewingClickHelper {
   private static final int POTION_SLOT_0 = 0;
   private static final int POTION_SLOT_1 = 1;
   private static final int POTION_SLOT_2 = 2;
   private static final int INGREDIENT_SLOT = 3;
   private static final int PLAYER_INVENTORY_FIRST_SLOT = 5;
   private static int trackedSyncId = -1;
   private static int previousPotionSlotCount = -1;
   private static ContainerPotionMoveTask pendingContainerPotionMove;

   private BrewingClickHelper() {
   }

   public static boolean handleShiftRightClick(class_310 client, class_1703 rawHandler, class_1735 clickedSlot, int button) {
      if (button == 1 && client.field_1724 != null && client.field_1761 != null) {
         if (clickedSlot != null && clickedSlot.method_7681() && rawHandler.method_34255().method_7960()) {
            if (rawHandler instanceof class_1707) {
               class_1707 genericHandler = (class_1707)rawHandler;
               if (tryStartMovingMatchingPotionsToContainer(client, genericHandler, clickedSlot)) {
                  return true;
               }
            }

            if (rawHandler instanceof class_1708) {
               class_1708 handler = (class_1708)rawHandler;
               int slotId = clickedSlot.field_7874;
               if (slotId >= 5 && tryMoveOneIngredient(client, handler, clickedSlot)) {
                  return true;
               } else if (isBrewingPotionSlot(slotId)) {
                  quickMoveAllBrewingBottleSlots(client, handler, slotId);
                  return true;
               } else {
                  return false;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static void onClientTick(class_310 client) {
      if (client.field_1724 != null && client.field_1761 != null) {
         class_1703 currentHandler = client.field_1724.field_7512;
         if (currentHandler instanceof class_1707) {
            class_1707 genericHandler = (class_1707)currentHandler;
            tickPendingContainerPotionMove(client, genericHandler);
         } else {
            pendingContainerPotionMove = null;
         }

         if (!(client.field_1755 instanceof class_472)) {
            resetTracking();
         } else if (currentHandler instanceof class_1708) {
            class_1708 handler = (class_1708)currentHandler;
            int syncId = getSyncId(handler);
            int currentCount = countFilledPotionSlots(handler);
            if (trackedSyncId != syncId) {
               trackedSyncId = syncId;
               previousPotionSlotCount = currentCount;
            } else {
               if (handler.method_34255().method_7960() && currentCount > previousPotionSlotCount && currentCount > 0 && currentCount < 3) {
                  autoFillMatchingPotions(client, handler);
                  currentCount = countFilledPotionSlots(handler);
               }

               previousPotionSlotCount = currentCount;
            }
         } else {
            resetTracking();
         }
      } else {
         resetTracking();
         pendingContainerPotionMove = null;
      }
   }

   private static boolean tryMoveOneIngredient(class_310 client, class_1708 handler, class_1735 sourceSlot) {
      class_1799 sourceStack = sourceSlot.method_7677();
      class_1735 ingredientSlot = handler.method_7611(3);
      if (!ingredientSlot.method_7680(sourceStack)) {
         return false;
      } else {
         class_1799 currentIngredient = ingredientSlot.method_7677();
         if (!currentIngredient.method_7960()) {
            int maxCount = Math.min(currentIngredient.method_7914(), ingredientSlot.method_7676(sourceStack));
            if (!class_1799.method_31577(currentIngredient, sourceStack) || currentIngredient.method_7947() >= maxCount) {
               return false;
            }
         }

         clickSlot(client, handler, sourceSlot.field_7874, 0, class_1713.field_7790);
         clickSlot(client, handler, 3, 1, class_1713.field_7790);
         if (!handler.method_34255().method_7960()) {
            clickSlot(client, handler, sourceSlot.field_7874, 0, class_1713.field_7790);
         }

         return true;
      }
   }

   private static void quickMoveAllBrewingBottleSlots(class_310 client, class_1708 handler, int firstSlotId) {
      if (isBrewingPotionSlot(firstSlotId) && handler.method_7611(firstSlotId).method_7681()) {
         clickSlot(client, handler, firstSlotId, 0, class_1713.field_7794);
      }

      for(int slotId = 0; slotId <= 2; ++slotId) {
         if (slotId != firstSlotId && handler.method_7611(slotId).method_7681()) {
            clickSlot(client, handler, slotId, 0, class_1713.field_7794);
         }
      }

   }

   private static void autoFillMatchingPotions(class_310 client, class_1708 handler) {
      class_1799 template = findPotionTemplate(handler);
      if (!template.method_7960() && isPotionItem(template)) {
         if (allExistingPotionSlotsMatch(handler, template)) {
            for(int targetSlot = 0; targetSlot <= 2; ++targetSlot) {
               if (!handler.method_7611(targetSlot).method_7681()) {
                  int sourceSlot = findMatchingPlayerPotionSlot(handler, template);
                  if (sourceSlot == -1) {
                     return;
                  }

                  clickSlot(client, handler, sourceSlot, 0, class_1713.field_7794);
               }
            }

         }
      }
   }

   private static boolean tryStartMovingMatchingPotionsToContainer(class_310 client, class_1707 handler, class_1735 clickedSlot) {
      class_1799 template = clickedSlot.method_7677();
      if (!isPotionItem(template)) {
         return false;
      } else {
         int containerSlotCount = getGenericContainerSlotCount(handler);
         if (clickedSlot.field_7874 < containerSlotCount) {
            return false;
         } else {
            int emptyContainerSlotCount = countEmptyContainerPotionSlots(handler, template, containerSlotCount);
            if (emptyContainerSlotCount <= 0) {
               return false;
            } else {
               List<Integer> sourceSlots = collectMatchingPlayerPotionSlots(handler, template, containerSlotCount, clickedSlot.field_7874);
               if (sourceSlots.isEmpty()) {
                  return false;
               } else {
                  int movableCount = Math.min(emptyContainerSlotCount, sourceSlots.size());
                  pendingContainerPotionMove = new ContainerPotionMoveTask(getSyncId(handler), template.method_46651(1), containerSlotCount, sourceSlots.subList(0, movableCount));
                  tickPendingContainerPotionMove(client, handler);
                  return true;
               }
            }
         }
      }
   }

   private static void tickPendingContainerPotionMove(class_310 client, class_1707 handler) {
      if (pendingContainerPotionMove != null) {
         if (!pendingContainerPotionMove.tick(client, handler)) {
            pendingContainerPotionMove = null;
         }

      }
   }

   private static int getGenericContainerSlotCount(class_1707 handler) {
      return Math.max(0, handler.method_7602().size() - 36);
   }

   private static int countEmptyContainerPotionSlots(class_1707 handler, class_1799 template, int containerSlotCount) {
      int count = 0;

      for(int slotId = 0; slotId < containerSlotCount; ++slotId) {
         class_1735 slot = handler.method_7611(slotId);
         if (slot.method_7677().method_7960() && slot.method_7680(template)) {
            ++count;
         }
      }

      return count;
   }

   private static List<Integer> collectMatchingPlayerPotionSlots(class_1707 handler, class_1799 template, int containerSlotCount, int clickedSlotId) {
      List<Integer> sourceSlots = new ArrayList();
      int size = handler.method_7602().size();
      if (clickedSlotId >= containerSlotCount && clickedSlotId < size) {
         class_1799 clickedStack = handler.method_7611(clickedSlotId).method_7677();
         if (!clickedStack.method_7960() && class_1799.method_31577(clickedStack, template)) {
            sourceSlots.add(clickedSlotId);
         }
      }

      for(int slotId = containerSlotCount; slotId < size; ++slotId) {
         if (slotId != clickedSlotId) {
            class_1799 stack = handler.method_7611(slotId).method_7677();
            if (!stack.method_7960() && class_1799.method_31577(stack, template)) {
               sourceSlots.add(slotId);
            }
         }
      }

      return sourceSlots;
   }

   private static class_1799 findPotionTemplate(class_1708 handler) {
      for(int slotId = 0; slotId <= 2; ++slotId) {
         class_1799 stack = handler.method_7611(slotId).method_7677();
         if (!stack.method_7960()) {
            return stack;
         }
      }

      return class_1799.field_8037;
   }

   private static boolean allExistingPotionSlotsMatch(class_1708 handler, class_1799 template) {
      for(int slotId = 0; slotId <= 2; ++slotId) {
         class_1799 stack = handler.method_7611(slotId).method_7677();
         if (!stack.method_7960() && !class_1799.method_31577(stack, template)) {
            return false;
         }
      }

      return true;
   }

   private static int findMatchingPlayerPotionSlot(class_1708 handler, class_1799 template) {
      int size = handler.method_7602().size();

      for(int slotId = 5; slotId < size; ++slotId) {
         class_1799 stack = handler.method_7611(slotId).method_7677();
         if (!stack.method_7960() && class_1799.method_31577(stack, template)) {
            return slotId;
         }
      }

      return -1;
   }

   private static boolean isPotionItem(class_1799 stack) {
      return stack.method_31574(class_1802.field_8574) || stack.method_31574(class_1802.field_8436) || stack.method_31574(class_1802.field_8150);
   }

   private static boolean isBrewingPotionSlot(int slotId) {
      return slotId >= 0 && slotId <= 2;
   }

   private static int countFilledPotionSlots(class_1708 handler) {
      int count = 0;

      for(int slotId = 0; slotId <= 2; ++slotId) {
         if (handler.method_7611(slotId).method_7681()) {
            ++count;
         }
      }

      return count;
   }

   private static void clickSlot(class_310 client, class_1703 handler, int slotId, int button, class_1713 actionType) {
      client.field_1761.method_2906(getSyncId(handler), slotId, button, actionType, client.field_1724);
   }

   private static int getSyncId(class_1703 handler) {
      return handler.field_7763;
   }

   private static void resetTracking() {
      trackedSyncId = -1;
      previousPotionSlotCount = -1;
   }

   private static final class ContainerPotionMoveTask {
      private final int syncId;
      private final class_1799 template;
      private final int containerSlotCount;
      private final Queue<Integer> sourceSlots;
      private int waitTicks;

      private ContainerPotionMoveTask(int syncId, class_1799 template, int containerSlotCount, List<Integer> sourceSlots) {
         this.syncId = syncId;
         this.template = template;
         this.containerSlotCount = containerSlotCount;
         this.sourceSlots = new ArrayDeque(sourceSlots);
         this.waitTicks = 0;
      }

      private boolean tick(class_310 client, class_1707 handler) {
         if (client.field_1724 != null && client.field_1761 != null && BrewingClickHelper.getSyncId(handler) == this.syncId) {
            if (this.sourceSlots.isEmpty()) {
               return false;
            } else if (BrewingClickHelper.countEmptyContainerPotionSlots(handler, this.template, this.containerSlotCount) <= 0) {
               return false;
            } else if (this.waitTicks > 0) {
               --this.waitTicks;
               return true;
            } else {
               while(!this.sourceSlots.isEmpty()) {
                  int sourceSlot = (Integer)this.sourceSlots.poll();
                  if (sourceSlot >= this.containerSlotCount && sourceSlot < handler.method_7602().size()) {
                     class_1799 stack = handler.method_7611(sourceSlot).method_7677();
                     if (!stack.method_7960() && class_1799.method_31577(stack, this.template)) {
                        BrewingClickHelper.clickSlot(client, handler, sourceSlot, 0, class_1713.field_7794);
                        this.waitTicks = 1;
                        return !this.sourceSlots.isEmpty();
                     }
                  }
               }

               return false;
            }
         } else {
            return false;
         }
      }
   }
}
