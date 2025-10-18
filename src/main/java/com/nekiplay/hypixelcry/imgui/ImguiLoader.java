package com.nekiplay.hypixelcry.imgui;

import com.nekiplay.hypixelcry.HypixelCry;
import com.nekiplay.hypixelcry.features.lua.LuaManager;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiBackendFlags;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;

import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;

public class ImguiLoader {
    private static final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();

    private static final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();

    public static long windowHandle = -1;
    public static void onGlfwReturn(long handle) {
        //loadImage();
    }
    public static void onGlfwInit(long handle)  {
        initializeImGui(handle);
        imGuiGlfw.init(handle, true);
        imGuiGl3.init();
        windowHandle = handle;
        //HypixelCry.LOGGER.info("TextureID: " + HypixelCry.texture);
    }

    public static void onFrameRender() {
        if (windowHandle != -1) {
            imGuiGlfw.newFrame();

            ImGui.newFrame();
            //ImGui.begin("Minecraft Cry");

            HypixelCry.LUA_MANAGER.omImGuiRenderEvent();

            //ImGui.end();
            ImGui.render();
            imGuiGl3.renderDrawData(ImGui.getDrawData());

            if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
                final long backupWindowPtr = glfwGetCurrentContext();
                ImGui.updatePlatformWindows();
                ImGui.renderPlatformWindowsDefault();
                glfwMakeContextCurrent(backupWindowPtr);
            }
        }
    }

    private static void initializeImGui(long glHandle) {
        ImGui.createContext();
        ImGui.styleColorsDark();
        ImGuiIO io = ImGui.getIO();
        io.setIniFilename(null);
        io.setConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);
        io.addConfigFlags(ImGuiConfigFlags.DockingEnable);
        io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
        io.setBackendFlags(ImGuiBackendFlags.HasMouseCursors);
        io.setBackendPlatformName("imgui_java_impl_glfw");
        //io.setKeyMap(ImGuiKey.Tab, GLFW.GLFW_KEY_TAB);
        //io.setKeyMap(ImGuiKey.LeftArrow, GLFW.GLFW_KEY_LEFT);
        //io.setKeyMap(ImGuiKey.RightArrow, GLFW.GLFW_KEY_RIGHT);
        //io.setKeyMap(ImGuiKey.UpArrow, GLFW.GLFW_KEY_UP);
        //io.setKeyMap(ImGuiKey.DownArrow, GLFW.GLFW_KEY_DOWN);
        //io.setKeyMap(ImGuiKey.PageUp, GLFW.GLFW_KEY_PAGE_UP);
        //io.setKeyMap(ImGuiKey.PageDown, GLFW.GLFW_KEY_PAGE_DOWN);
        //io.setKeyMap(ImGuiKey.Home, GLFW.GLFW_KEY_HOME);
        //io.setKeyMap(ImGuiKey.End, GLFW.GLFW_KEY_END);
        //io.setKeyMap(ImGuiKey.Insert, GLFW.GLFW_KEY_INSERT);
        //io.setKeyMap(ImGuiKey.Delete, GLFW.GLFW_KEY_DELETE);
    }
}