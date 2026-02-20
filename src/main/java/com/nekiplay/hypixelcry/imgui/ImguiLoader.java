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
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiBackendFlags;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.jspecify.annotations.Nullable;

import static com.nekiplay.hypixelcry.HypixelCry.mc;
import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;

public class ImguiLoader {
    private static final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();

    private static final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();

    public static long windowHandle = -1;
    public static void onGlfwReturn() {
        //loadImage();
    }
    public static void onGlfwInit()  {
        ImGui.createContext();
        //ImGui.styleColorsDark();

        ImGuiIO io = ImGui.getIO();
        io.setIniFilename(null);
        io.setConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);
        io.addConfigFlags(ImGuiConfigFlags.DockingEnable);
        io.setBackendFlags(ImGuiBackendFlags.HasMouseCursors);

        imGuiGlfw.init(mc.getWindow().handle(), true);
        imGuiGl3.init();
        windowHandle = mc.getWindow().handle();
    }

    public static void onFrameRender() {
        if (windowHandle != -1) {
            //RenderTarget framebuffer = mc.getMainRenderTarget();
            //GlTexture glTexture = (GlTexture) framebuffer.getColorTexture();
            //GlDevice device = (GlDevice) RenderSystem.getDevice();
            //int prevFramebuffer = glTexture.getFbo(device.directStateAccess(), null);

            //GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, prevFramebuffer);

            imGuiGlfw.newFrame();
            imGuiGl3.newFrame();
            ImGui.newFrame();

            HypixelCry.LUA_MANAGER.getScripts().values().forEach(LuaScript::onImGuiRenderEvent);

            ImGui.render();
            imGuiGl3.renderDrawData(ImGui.getDrawData());

            //GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, 0);
        }
    }
}
