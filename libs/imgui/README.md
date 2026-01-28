---
description: GUI library
icon: browser
---

# ImGUI

## Variables

### [constants](constants.md)

## `begin(title, flags)`

Function to specify the beginning of the window

**Parameters:**

* `title` (string).
* `flags` (imgui.constants)

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
local window_flags = imgui.constants.WindowFlags_NoResize

registerImGuiRenderEvent(function()
<strong>    if imgui.begin("Test", window_flags) then
</strong>
    end
    imgui.endBegin()
end)
</code></pre>

## `endBegin()`

Function to specify the ends of the window

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
local window_flags = imgui.constants.WindowFlags_NoResize

registerImGuiRenderEvent(function()
    if imgui.begin("Test", window_flags) then
<strong>        
</strong>    end
    imgui.endBegin()
end)
</code></pre>

## `text(text)`

Plain text

**Parameters:**

* `text` (string).

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
registerImGuiRenderEvent(function()
    if imgui.begin("Test") then
<strong>        imgui.text("ImGUI from Lua!")
</strong>    end
    imgui.endBegin()
end)
</code></pre>

## `textDisabled(text)`

Text with color turned off

**Parameters:**

* `text` (string).

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
registerImGuiRenderEvent(function()
    if imgui.begin("Test") then
<strong>        imgui.textDisabled("Disabled text")
</strong>    end
    imgui.endBegin()
end)
</code></pre>

## `textColored(red, green, blue, text)`

Colored text

**Parameters:**

* `red` (number).
* `green` (number).
* `blue` (number).
* `text` (string).

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
registerImGuiRenderEvent(function()
    if imgui.begin("Test") then
<strong>        imgui.textColored(1.0, 0.0, 0.0, 1.0, "Red text")
</strong>    end
    imgui.endBegin()
end)
</code></pre>

## `bulletText(text)`

Text with a round piece

**Parameters:**

* `text` (string).

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
registerImGuiRenderEvent(function()
    if imgui.begin("Test") then
<strong>        imgui.bulletText("Bullet text")
</strong>    end
    imgui.endBegin()
end)
</code></pre>

## `checkbox(text, state)`

Text with a round piece

**Parameters:**

* `text` (string)
* `state` (boolean)

**Example Usage:**

<pre class="language-lua"><code class="lang-lua"><strong>-- Example code showing how to use the function
</strong><strong>local checkbox_state = false
</strong><strong>
</strong>registerImGuiRenderEvent(function()
    if imgui.begin("Test") then
<strong>        local checkbox_changed, new_checkbox_state = imgui.checkbox("Check box", checkbox_state)
</strong>        if checkbox_changed then
            checkbox_state  = new_checkbox_state 
        end
    end
    imgui.endBegin()
end)
</code></pre>

## `inputText(text, buffer)`

Text with a round piece

**Parameters:**

* `text` (string)
* `buffer` (string)

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
local text_buffer = "Hello, ImGUI!"

registerImGuiRenderEvent(function()
    if imgui.begin("Test") then
<strong>        local text_changed, new_text = imgui.inputText("Text input", text_buffer)
</strong>        if text_changed then
            text_buffer = new_text
        end
    end
    imgui.endBegin()
end)
</code></pre>

## `inputText(text, buffer, wight, height)`

Text with a round piece

**Parameters:**

* `text` (string)
* `state` (boolean)
* `wight` (bumber)
* `height` (number)

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
local text_buffer = "Hello, ImGUI!"

registerImGuiRenderEvent(function()
    if imgui.begin("Test") then
<strong>        local multiline_changed, new_multiline = imgui.inputTextMultiline("Multi line input", text_buffer, 200, 100)
</strong>        if multiline_changed then
            text_buffer = new_multiline
        end
    end
    imgui.endBegin()
end)
</code></pre>

## `inputInt(text, int_value, step, step_fast)`

Text with a round piece

**Parameters:**

* `text` (string)
* `buffer` (string)
* `step` (number)
* `step_fast` (number)

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
local int_value = 0

registerImGuiRenderEvent(function()
    if imgui.begin("Test") then
<strong>        local int_changed, new_int = imgui.inputInt("Number input", int_value, 1, 100)
</strong>        if int_changed then
            int_value = new_int
        end
    end
    imgui.endBegin()
end)
</code></pre>

## `inputFloat(text, float_value, step, step_fast, format)`

Text with a round piece

**Parameters:**

* `text` (string)
* `buffer` (string)
* `step` (number)
* `step_fast` (number)
* `format` (string)

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
local int_value = 0

registerImGuiRenderEvent(function()
    if imgui.begin("Test") then
<strong>        local float_changed, new_float = imgui.inputFloat("Float input", float_value, 0.1, 1.0, "%.3f")
</strong>        if float_changed then
            float_value = new_float
        end
    end
    imgui.endBegin()
end)
</code></pre>

## `listBox(text, current_item, items)`

Text with a round piece

**Parameters:**

* `text` (string)
* `current_item` (number)
* `items` (table)

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
local listbox_current = 0
local listbox_items = {"Apple", "Banana", "Cherry", "Date"}

registerImGuiRenderEvent(function()
    if imgui.begin("Test") then
<strong>        listbox_current = imgui.listBox("ListBox", listbox_current, listbox_items)
</strong>    end
    imgui.endBegin()
end)
</code></pre>

## `button(text, width, height)`

A regular button

**Parameters:**

* `text` (string)
* `width` (number)
* `height` (number)

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
registerImGuiRenderEvent(function()
    if imgui.begin("Test") then
<strong>        if imgui.button("Test") then
</strong><strong>        
</strong><strong>        end
</strong>    end
    imgui.endBegin()
end)
</code></pre>

## `smallButton(text)`

Small button

**Parameters:**

* `text` (string)

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
registerImGuiRenderEvent(function()
    if imgui.begin("Test") then
<strong>        if imgui.smallButton("Test") then
</strong><strong>        
</strong><strong>        end
</strong>    end
    imgui.endBegin()
end)
</code></pre>

## `arrowButton(id, direction)`

Button with arrows

**Parameters:**

* `id` (string)
* `direction` (number)

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
registerImGuiRenderEvent(function()
    if imgui.begin("Test") then
<strong>        if imgui.arrowButton("##left", 0) then
</strong><strong>        
</strong><strong>        end
</strong><strong>        if imgui.arrowButton("##right", 1) then
</strong>        
        end
    end
    imgui.endBegin()
end)
</code></pre>

## `sameLine(offset, spacing)`

Text with a round piece

**Parameters:**

* `offset` (number)
* `spacing` (number)

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
registerImGuiRenderEvent(function()
    if imgui.begin("Test") then
<strong>        imgui.sameLine(0.0, 0.0)
</strong>    end
    imgui.endBegin()
end)
</code></pre>

## `image(id, wigth, height, uv0X, uv0Y, uv1X, uv1Y)`

Draw image

**Parameters:**

* `id` (number)
* `wigth` (number)
* `height` (number)
* `uv0X` (number)
* `uv0Y` (number)
* `uv1X` (number)
* `uv1Y` (number)

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
local compost_image = imgui.createImageObject()
local loading_result = compost_image.loadImage("config/hypixelcry/scripts/images/compost.png")

registerImGuiRenderEvent(function()
    if imgui.begin("Test") then
<strong>        imgui.image(compost_image.getId(), 16, 16, 0, 0, 0, 0) -- Normal
</strong><strong>		imgui.image(compost_image.getId(), 16, 16, 0, 1, 1, 0) -- Rotated Y
</strong>    end
    imgui.endBegin()
end)

registerUnloadCallback(function()
    compost_image.release()
end)
</code></pre>
