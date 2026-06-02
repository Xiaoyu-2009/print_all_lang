package dev.xiaoyu.print_all_lang.init.mixin.minecraft.client.resources.language;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import dev.xiaoyu.print_all_lang.PrintAllLang;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings({"unchecked"})
@Mixin(LanguageManager.class)
public class LanguageManagerMixin {

    @Inject(method = "onResourceManagerReload", at = @At("RETURN"))
    private void onLanguageReload(ResourceManager p_118973_, CallbackInfo ci) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String outputDirName = "all_lang";
        Map<String, Map<String, Object>> allTranslations = new LinkedHashMap<>();

        Map<ResourceLocation, Resource> resources = p_118973_.listResources("lang",
                path -> path.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            String path = entry.getKey().getPath();
            String langCode = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));

            try (InputStream is = entry.getValue().open()) {
                Map<String, Object> translations = gson.fromJson(
                        new InputStreamReader(is, StandardCharsets.UTF_8), Map.class);
                if (translations != null) {
                    allTranslations.computeIfAbsent(langCode, k -> new LinkedHashMap<>()).putAll(translations);
                }
            } catch (Exception e) {
                PrintAllLang.LOGGER.error("Failed to read language file {}", entry.getKey(), e);
            }
        }

        Path outputDir = Path.of(outputDirName);
        try {
            Files.createDirectories(outputDir);
            for (Map.Entry<String, Map<String, Object>> langEntry : allTranslations.entrySet()) {
                Path outputFile = outputDir.resolve(langEntry.getKey() + ".json");
                Files.writeString(outputFile, gson.toJson(langEntry.getValue()), StandardCharsets.UTF_8);
            }
            PrintAllLang.LOGGER.info("Exported {} language files to {} directory", allTranslations.size(), outputDirName);
        } catch (Exception e) {
            PrintAllLang.LOGGER.error("Failed to export language files", e);
        }
    }
}