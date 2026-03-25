package com.nekiplay.hypixelcry.imgui;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.nekiplay.hypixelcry.HypixelCry;
import com.nekiplay.hypixelcry.features.lua.LuaManager;
import com.nekiplay.hypixelcry.features.lua.LuaScript;
import com.nekiplay.hypixelcry.features.lua.objects.misc.imgui.ImDrawCommandQueue;
import imgui.ImFont;
import imgui.ImFontConfig;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiBackendFlags;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.lwjgl.BufferUtils;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

import static com.nekiplay.hypixelcry.HypixelCry.mc;
import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;

public class ImguiLoader {
    private static final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private static final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();

    public static long windowHandle = -1;

    /** Шрифт для ImGui — инициализируется при загрузке */
    private static ImFont font = null;

    /** Путь к шрифту относительно resources/assets/hypixelcry/fonts/ */
    private static final String FONT_PATH = "hypixelcry:fonts/jetbrainsmono-nerd.ttf";
    private static final float FONT_SIZE = 16.0f;

    public static void onGlfwReturn() {
        //loadImage();
    }

    public static void onGlfwInit() {
        ImGui.createContext();

        ImGuiIO io = ImGui.getIO();
        io.setIniFilename(null);
        io.setConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);
        io.addConfigFlags(ImGuiConfigFlags.DockingEnable);
        io.setBackendFlags(ImGuiBackendFlags.HasMouseCursors);

        // Загрузка шрифта из ресурсов Minecraft
        loadFontFromResources(io, FONT_PATH, FONT_SIZE);

        imGuiGlfw.init(mc.getWindow().handle(), true);
        imGuiGl3.init();
        windowHandle = mc.getWindow().handle();
    }

    private static void loadFontFromResources(ImGuiIO io, String resourcePath, float fontSize) {
        try {
            String[] parts = resourcePath.split(":", 2);
            if (parts.length != 2) {
                HypixelCry.LOGGER.warn("Invalid font resource path: " + resourcePath);
                return;
            }
            String namespace = parts[0];
            String path = parts[1];

            var resourceLocation = Identifier.fromNamespaceAndPath(namespace, path);
            var resourceOpt = mc.getResourceManager().getResource(resourceLocation);

            if (resourceOpt.isEmpty()) {
                HypixelCry.LOGGER.warn("Font resource not found: " + resourcePath);
                return;
            }

            try (InputStream inputStream = resourceOpt.get().open()) {
                byte[] fontData = inputStream.readAllBytes();

                // === 1. Основной шрифт (JetBrains Mono Nerd) ===
                ImFontConfig mainConfig = new ImFontConfig();
                mainConfig.setMergeMode(false);
                mainConfig.setPixelSnapH(true);

                var builder = new imgui.ImFontGlyphRangesBuilder();

                // Гарантируем наличие популярных символов
                builder.addText("■□▪▫▬▮▰▲▶▼◀◆◇○●◢◣◤◥★☆☀☁☂♠♣♥♦✓✔✕✖➕➖➗");

                // Базовые диапазоны
                builder.addRanges(io.getFonts().getGlyphRangesDefault());
                builder.addRanges(io.getFonts().getGlyphRangesCyrillic());

                // Расширенные диапазоны
                short[] extendedRanges = new short[] {
                        (short)0x2500, (short)0x25FF,  // Box + Geometric (■●▲)
                        (short)0x2600, (short)0x26FF,  // Misc Symbols
                        (short)0x2700, (short)0x27BF,  // Dingbats
                        (short)0x2190, (short)0x21FF,  // Arrows
                        (short)0x2000, (short)0x206F,  // Punctuation
                        // Nerd Font icons
                        (short)0xE0A0, (short)0xE0A3,
                        (short)0xE0B0, (short)0xE0B3,
                        (short)0xF000, (short)0xF2E0,
                        (short)0xE700, (short)0xE7C5,
                        (short)0
                };
                builder.addRanges(extendedRanges);

                short[] glyphRanges = builder.buildRanges();
                font = io.getFonts().addFontFromMemoryTTF(fontData, fontSize, mainConfig, glyphRanges);
                mainConfig.destroy();

                HypixelCry.LOGGER.info("Main font loaded: " + resourcePath);

                // === 2. Fallback (Unifont TTF) ===
                // Загружаем ТОЛЬКО если есть файл unifont-regular.ttf
                loadFallbackFont(io, "hypixelcry:fonts/unifont-16.0.04.ttf", fontSize);

            }

            // === 3. Сборка атласа ===
            boolean built = io.getFonts().build();
            if (!built || font == null || !font.isLoaded()) {
                HypixelCry.LOGGER.error("Font atlas build failed!");
            } else {
                HypixelCry.LOGGER.info("Font atlas built: glyphs=" + font.getFallbackGlyph());
                checkSymbol(font, '■', "U+25A0");
                checkSymbol(font, '●', "U+25CF");
                checkSymbol(font, '★', "U+2605");
            }

        } catch (Exception e) {
            HypixelCry.LOGGER.error("Failed to load font: " + resourcePath, e);
        }
    }

    /**
     * Загружает Unifont TTF как fallback
     */
    private static void loadFallbackFont(ImGuiIO io, String fallbackPath, float fontSize) {
        try {
            String[] parts = fallbackPath.split(":", 2);
            if (parts.length != 2) return;

            var resourceLocation = Identifier.fromNamespaceAndPath(parts[0], parts[1]);
            var resourceOpt = mc.getResourceManager().getResource(resourceLocation);
            if (resourceOpt.isEmpty()) {
                HypixelCry.LOGGER.debug("Fallback font not found: " + fallbackPath);
                return;
            }

            try (InputStream inputStream = resourceOpt.get().open()) {
                byte[] fontData = inputStream.readAllBytes();

                ImFontConfig fallbackConfig = new ImFontConfig();
                fallbackConfig.setMergeMode(true); // Слияние с основным

                // Диапазоны для Unifont (символы, которых нет в JetBrains Mono)
                short[] fallbackRanges = new short[] {
                        (short)0x2500, (short)0x25FF,  // Box + Geometric
                        (short)0x2600, (short)0x26FF,  // Misc Symbols
                        (short)0x2700, (short)0x27BF,  // Dingbats
                        (short)0x0
                };

                io.getFonts().addFontFromMemoryTTF(fontData, fontSize, fallbackConfig, fallbackRanges);
                fallbackConfig.destroy();

                HypixelCry.LOGGER.info("Fallback font (Unifont TTF) merged successfully");
            }
        } catch (Exception e) {
            HypixelCry.LOGGER.debug("Failed to load fallback font: " + fallbackPath, e);
        }
    }

    private static void checkSymbol(ImFont font, char symbol, String description) {
        boolean present = font.findGlyphNoFallback(symbol) != null;
        if (present) {
            HypixelCry.LOGGER.info("✓ " + description + " ('" + symbol + "') in main font");
        } else {
            HypixelCry.LOGGER.warn("✗ " + description + " ('" + symbol + "') missing in main (checking fallback)");
        }
    }


    public static void onFrameRender() {
        if (windowHandle != -1) {
            imGuiGlfw.newFrame();
            imGuiGl3.newFrame();
            ImGui.newFrame();


            // Применяем шрифт только если он загружен
            if (font != null && font.isLoaded()) {
                ImGui.pushFont(font);
            }

            HypixelCry.LUA_MANAGER.getScripts().values().forEach((script) -> {
                Objects.requireNonNull(script.getLibs().getImgui()).getQueue().executeAndClear();
                script.onImGuiRenderEvent();

            });

            if (font != null && font.isLoaded()) {
                ImGui.popFont();
            }

            ImGui.render();
            imGuiGl3.renderDrawData(ImGui.getDrawData());
        }
    }

    /**
     * Пересоздаёт текстуру атласа шрифтов (если шрифты изменились динамически)
     */
    public static void rebuildFontTexture() {
        ImGui.getIO().getFonts().build();
        imGuiGl3.createFontsTexture();
    }

    /**
     * Геттер для текущего шрифта (если нужно использовать в других местах)
     */
    @Nullable
    public static ImFont getFont() {
        return font;
    }
}