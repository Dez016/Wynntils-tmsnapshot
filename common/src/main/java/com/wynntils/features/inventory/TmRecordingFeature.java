package com.wynntils.features.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.Logger;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.event.SetSlotEvent;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.encoding.type.EncodingSettings;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.utils.EncodedByteBuffer;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.Texture;
import com.wynntils.utils.type.ErrorOr;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@ConfigCategory(Category.INVENTORY)
public class TmRecordingFeature extends Feature{

    private static List<String> OUTPUT = new ArrayList<>();
    private static final Logger LOGGER = LogManager.getLogger(TmRecordingFeature.class);

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRenderSlot(SetSlotEvent e) {
        EncodeAndLog(e.getItemStack());
        LOGGER.info("LOGGING e: " + e);
    }

    private void EncodeAndLog(ItemStack item) {
            // ItemStack item = renderedSlot.getItem();

            // if (renderedSlot == null) return;

            // // Special case for unidentified gear
            // Optional<GearItem> gearItemOpt = Models.Item.asWynnItem(renderedSlot.getItem(), GearItem.class);
            // if (gearItemOpt.isPresent() && gearItemOpt.get().isUnidentified()) {
            //     // We can only send chat encoded gear of identified gear
            //     WynntilsMod.warn("Cannot make chat link of unidentified gear");
            //     McUtils.sendErrorToClient(I18n.get("feature.wynntils.chatItem.chatItemUnidentifiedError"));
            //     return;
            // }

            WynnItem wynnItem = Models.Item.getWynnItem(item)
                .orElse(null);
            if (wynnItem == null) return;
            
            // // Don't try to encode unsupported items
            // if (!Models.ItemEncoding.canEncodeItem(wynnItemOpt.get())) return;

            LOGGER.info("WYNNITEM: " + wynnItem);
            LOGGER.info("ITEM: " + item);
        
            // Encode the item with the selected settings
            EncodingSettings encodingSettings = new EncodingSettings(
                    Models.ItemEncoding.extendedIdentificationEncoding.get(), Models.ItemEncoding.shareItemName.get());
            ErrorOr<EncodedByteBuffer> errorOrEncodedByteBuffer =
                    Models.ItemEncoding.encodeItem(wynnItem, encodingSettings);

            if (errorOrEncodedByteBuffer.hasError()) {
                WynntilsMod.error("Failed to encode item: " + errorOrEncodedByteBuffer.getError());
                return;
            }
            EncodedByteBuffer encodedItem = errorOrEncodedByteBuffer.getValue();
            LOGGER.info("ENCODED: " + encodedItem);
    }
}


//WyntilsMod.LOGGER.info()
