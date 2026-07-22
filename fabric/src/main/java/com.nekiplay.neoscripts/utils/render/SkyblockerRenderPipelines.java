package com.nekiplay.neoscripts.utils.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.nekiplay.neoscripts.Main;
import com.nekiplay.neoscripts.annotations.Init;
import com.nekiplay.neoscripts.compatibility.IrisCompatibility;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public class SkyblockerRenderPipelines {
    /** Similar to {@link RenderPipelines#DEBUG_FILLED_BOX} */
    public static final RenderPipeline FILLED_INSTANCED = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Main.id("pipeline/debug_filled_box_instanced"))
            .withVertexShader(Main.id("core/filled_box"))
            .withBindGroupLayout(SkyblockerBindGroupLayouts.BOX_DATA)
            .withVertexBinding(0, DefaultVertexFormat.POSITION)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withCull(false)
            .build());
    public static final RenderPipeline FILLED_THROUGH_WALLS_INSTANCED = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Main.id("pipeline/debug_filled_box_through_walls_instanced"))
            .withVertexShader(Main.id("core/filled_box"))
            .withBindGroupLayout(SkyblockerBindGroupLayouts.BOX_DATA)
            .withVertexBinding(0, DefaultVertexFormat.POSITION)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withDepthStencilState(Optional.empty())
            .build());
    /** Similar to {@link RenderPipelines#DEBUG_FILLED_BOX} */
    public static final RenderPipeline FILLED_THROUGH_WALLS = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Main.id("pipeline/debug_filled_box_through_walls"))
            .withDepthStencilState(Optional.empty())
            .build());
    public static final RenderPipeline OUTLINED_BOX_INSTANCED = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(Main.id("pipeline/outlined_box_instanced"))
            .withVertexShader(Main.id("core/outlined_box"))
            .withBindGroupLayout(SkyblockerBindGroupLayouts.OUTLINED_BOX_DATA)
            .withVertexBinding(0, SkyblockerVertexFormats.POSITION_NORMAL)
            .withPrimitiveTopology(PrimitiveTopology.LINES)
            .build());
    public static final RenderPipeline OUTLINED_BOX_THROUGH_WALLS_INSTANCED = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(Main.id("pipeline/outlined_box_through_walls_instanced"))
            .withVertexShader(Main.id("core/outlined_box"))
            .withBindGroupLayout(SkyblockerBindGroupLayouts.OUTLINED_BOX_DATA)
            .withVertexBinding(0, SkyblockerVertexFormats.POSITION_NORMAL)
            .withPrimitiveTopology(PrimitiveTopology.LINES)
            .withDepthStencilState(Optional.empty())
            .build());
    /** Similar to {@link RenderPipelines#LINES} */
    public static final RenderPipeline LINES_THROUGH_WALLS = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(Main.id("pipeline/lines_through_walls"))
            .withDepthStencilState(Optional.empty())
            .build());
    /** Similar to {@link RenderPipelines#DEBUG_QUADS}  */
    public static final RenderPipeline QUADS_THROUGH_WALLS = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Main.id("pipeline/debug_quads_through_walls"))
            .withDepthStencilState(Optional.empty())
            .withCull(false)
            .build());
    /** Similar to {@link RenderPipelines#GUI_TEXTURED} */
    public static final RenderPipeline TEXTURE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("neoscripts", "pipeline/texture"))
            .withCull(false)
            .build());
    public static final RenderPipeline TEXTURE_THROUGH_WALLS = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
            .withLocation(Main.id("pipeline/texture_through_walls"))
            .withDepthStencilState(Optional.empty())
            .withCull(false)
            .build());
    public static final RenderPipeline CYLINDER = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Main.id("pipeline/cylinder"))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_STRIP)
            .withCull(false)
            .build());
    public static final RenderPipeline CYLINDER_THROUGH_WALLS = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Main.id("pipeline/cylinder"))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_STRIP)
            .withCull(false)
            .withDepthStencilState(Optional.empty())
            .build());
    public static final RenderPipeline CIRCLE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Main.id("pipeline/circle"))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_FAN)
            .withCull(false)
            .build());
    public static final RenderPipeline CIRCLE_LINES = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Main.id("pipeline/circle_lines"))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withCull(false)
            .build());
    public static final RenderPipeline CIRCLE_THROUGH_WALLS = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Main.id("pipeline/circle"))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLE_FAN)
            .withCull(false)
            .withDepthStencilState(Optional.empty())
            .build());
    public static final RenderPipeline CIRCLE_LINES_THROUGH_WALLS = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Main.id("pipeline/circle_lines"))
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.QUADS)
            .withCull(false)
            .withDepthStencilState(Optional.empty())
            .build());


    @Init
    public static void init() {
        IrisCompatibility.assignPipelines();
    }
}
