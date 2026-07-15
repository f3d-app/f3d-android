package app.f3d.F3D.android.Utils

data class OptionSpec(val name: String, val label: String, val group: String)

sealed interface OptionWidget {
    val spec: OptionSpec

    /** Whether the option currently holds a value; false for optional options left unset. */
    val isSet: Boolean

    class Toggle(override val spec: OptionSpec, val value: Boolean, override val isSet: Boolean) : OptionWidget
    class Enum(
        override val spec: OptionSpec,
        val values: List<String>,
        val current: String,
        override val isSet: Boolean,
    ) : OptionWidget
    class Color(override val spec: OptionSpec, val rgb: DoubleArray, override val isSet: Boolean) : OptionWidget
    class Range(
        override val spec: OptionSpec,
        val min: Double,
        val max: Double,
        val increment: Double,
        val current: Double,
        override val isSet: Boolean,
    ) : OptionWidget
}

object OptionsRegistry {
    val v1: List<OptionSpec> = listOf(
        OptionSpec("render.grid.enable", "Grid", "Render"),
        OptionSpec("render.grid.reflection", "Grid reflection", "Render"),
        OptionSpec("render.axes_grid.enable", "Axes grid", "Render"),
        OptionSpec("render.background.color", "Background color", "Render"),
        OptionSpec("render.background.skybox", "Skybox", "Render"),
        OptionSpec("render.background.blur.enable", "Background blur", "Render"),
        OptionSpec("render.background.blur.coc", "Background blur strength", "Render"),
        OptionSpec("render.hdri.ambient", "HDRI ambient light", "Render"),
        OptionSpec("render.light.intensity", "Light intensity", "Render"),
        OptionSpec("render.effect.ambient_occlusion", "Ambient occlusion", "Render"),
        OptionSpec("render.effect.tone_mapping", "Tone mapping", "Render"),
        OptionSpec("render.show_edges", "Show edges", "Render"),
        OptionSpec("render.line_width", "Line width", "Render"),
        OptionSpec("render.armature.enable", "Armature", "Render"),
        OptionSpec("model.color.rgb", "Color", "Model"),
        OptionSpec("model.color.opacity", "Opacity", "Model"),
        OptionSpec("model.material.metallic", "Metallic", "Model"),
        OptionSpec("model.material.roughness", "Roughness", "Model"),
        OptionSpec("model.material.base_ior", "Base IOR", "Model"),
        OptionSpec("model.unlit", "Unlit", "Model"),
        OptionSpec("model.checkerboard.enable", "Checkerboard", "Model"),
        OptionSpec("model.normal_glyphs.enable", "Normal glyphs", "Model"),
        OptionSpec("model.normal_glyphs.scale", "Normal glyphs scale", "Model"),
        OptionSpec("scene.up_direction", "Up direction", "Scene"),
        OptionSpec("scene.camera.orthographic", "Orthographic camera", "Scene"),
        OptionSpec("ui.axis", "Axis", "UI"),
    )
}
