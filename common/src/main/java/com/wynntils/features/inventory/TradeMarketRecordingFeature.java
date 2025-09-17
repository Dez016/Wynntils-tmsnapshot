package com.wynntils.features.inventory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;

import com.wynntils.core.WynntilsMod;
import com.wynntils.core.components.Models;
import com.wynntils.core.consumers.features.Feature;
import com.wynntils.core.persisted.config.Category;
import com.wynntils.core.persisted.config.ConfigCategory;
import com.wynntils.mc.event.SetSlotEvent;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.encoding.type.EncodingSettings;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.models.trademarket.TradeMarketModel;
import com.wynntils.models.trademarket.type.TradeMarketPriceInfo;
import com.wynntils.utils.EncodedByteBuffer;
import com.wynntils.utils.type.ErrorOr;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;


@ConfigCategory(Category.INVENTORY)
public class TradeMarketRecordingFeature extends Feature{

    private static final Logger LOGGER = WynntilsMod.getLogger();
    private Set<String> uniqueStrings = new HashSet<>();
    

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderSlot(SetSlotEvent.Post e) {
        LOGGER.info("LOGGING e: " + e);
        EncodeAndLog(e.getItemStack());


        // public TradeMarketState getTradeMarketState() {     <STRIGHT FROM THE REPO ,COUDL USE THIS
        // return tradeMarketState;
        // }
    }

    private void EncodeAndLog(ItemStack item) {
            WynnItem wynnItem = Models.Item.getWynnItem(item)
                .orElse(null);
            if (wynnItem == null) return;
            
            // // Don't try to encode unsupported items
            // if (!Models.ItemEncoding.canEncodeItem(wynnItemOpt.get())) return;

            // LOGGER.info("WYNNITEM: " + wynnItem);
            // LOGGER.info("ITEM: " + item);
        
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
            String itemString = Models.ItemEncoding.makeItemString(wynnItem, encodedItem);

            // Special case for unidentified gear (LOGIC BELOW IS REALLY MESSY LMAO BUT ILL CHANGE IT LATER)

            Optional<GearItem> gearItemOpt = Models.Item.asWynnItem(item, GearItem.class);

            if (gearItemOpt.isPresent() && gearItemOpt.get().isUnidentified()) {
                //unid items don't get uniqueness checked (how should you do this? do u need itemstring data or name only?)
                TradeMarketModel tmModel = new TradeMarketModel();
                TradeMarketPriceInfo itemInfo = tmModel.calculateItemPriceInfo(item);
                Log("unid: " + itemString, itemInfo); //i want the name here ideally...
                return;
            }

            if (uniqueStrings.contains(itemString)) {
                return;
            }
            uniqueStrings.add(itemString);

            TradeMarketModel tmModel = new TradeMarketModel();
            TradeMarketPriceInfo itemInfo = tmModel.calculateItemPriceInfo(item);

            Log(itemString, itemInfo);
    }

    private void Log(String itemString, TradeMarketPriceInfo itemInfo) {
    Instant now = Instant.now();
    LocalDate date = now.atZone(ZoneId.systemDefault()).toLocalDate();
    String fileName = "codes-" + date.format(DateTimeFormatter.ISO_LOCAL_DATE) + "-" + now + ".csv";
    Path path = Path.of("common\\run\\logs\\tmbot", fileName);

    try {
        Files.createDirectories(path.getParent()); // create directories if missing

        // Format: timestamp,price,amount,itemString
        String csvLine = String.format(
            "\"%s\",%s,%s,\"%s\"%n",
            now.toString(),
            itemInfo.price(),
            itemInfo.amount(),
            itemString.replace("\"", "\"\"")  // escape quotes
        );

        Files.writeString(
            path,
            csvLine,
            StandardCharsets.UTF_16,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        );

        LOGGER.info("LOGGING COMPLETE: " + path.toAbsolutePath());
    } catch (IOException e) {
        LOGGER.warn("Failed to write encoded item string to CSV file", e);
    }
}



}
