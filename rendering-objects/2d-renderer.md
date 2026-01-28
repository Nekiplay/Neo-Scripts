---
icon: browser
---

# 2D renderer

## `getWindowScale()`

Return minecraft window scale.

**Returns:**

* (table) (width,  height)

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
register2DRenderer(function(context)
<strong>    local scale = context.getWindowScale()
</strong>    local width = scale.width -- Number
    local height = scale.height -- Number
end)
</code></pre>

## `getTextWidth(str)`

raws a 2D text.

**Parameters:**

* `object` (table (x, y, red, green, blue, text))

**Returns:**

* (number)

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
register2DRenderer(function(context)
    local text = "§6Hypixel Cry §7v1.1.3"
<strong>    local wigth = context.getTextWidth(text)
</strong>end)
</code></pre>

## `renderText(object)`

Draws a 2D text.

**Parameters:**

* `object` (table (x, y, red, green, blue, text))

**Returns:**

* (boolean) Return true if successfully

**Example Usage:**

<pre class="language-lua"><code class="lang-lua"><strong>-- Example code showing how to use the function
</strong>register2DRenderer(function(context)
    local obj2 = {
    	x = 3, y = 3, scale = 1,
    	text = "§6Hypixel Cry §7v1.1.3",
    	red = 0, green = 0, blue = 0
    }
<strong>    context.renderText(obj2)
</strong>    
    local obj3 = {
    	x = 3, y = 13, scale = 0.75,
    	text = "§7by §bNeki_play§7, §bKreedMan",
    	red = 0, green = 0, blue = 0
    }
<strong>    context.renderText(obj3)
</strong>end)
</code></pre>

## `renderImage(object)`

Draws a 2D text.

**Parameters:**

* `object` (table (x, y, path, width, height))

**Returns:**

* (boolean) Return true if successfully

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
register2DRenderer(function(context)
    local obj = {
    	x = 1, y = 4,
    	width = 16, height = 16,
    	path = "config/hypixelcry/scripts/images/logo.png",
    }
<strong>    context.renderImage(obj)
</strong>end)
</code></pre>

## `renderRect(object)`

Draws a 2D rectangle.

**Parameters:**

* `object` (table (x, y, width, height, red, green, blue, alpha))

**Returns:**

* (boolean) Return true if successfully

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
register2DRenderer(function(context)
    local obj = {
    	x = 1, y = 1,
    	width = 16, height = 16,
    	red = 255, green = 0, blue = 0,
    }
<strong>    context.renderRect(obj)
</strong>end)
</code></pre>

## `renderLine(object)`

Draws a 2D line.

**Parameters:**

* `object` (table)

**Returns:**

* (boolean) Return true if successfully

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>register2DRenderer(function(context)
</strong>    local obj = {
    	x1 = 1, y1 = 1,
    	x2 = 10, y2 = 10,
    	red = 255, green = 0, blue = 0,
    }
<strong>    context.renderLine(obj)
</strong>end)
</code></pre>

## `renderPolygon(object)`

Draws a 2D rectangle.

**Parameters:**

* `object` (table)

**Returns:**

* (boolean) Return true if successfully

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
register2DRenderer(function(context)
    local obj = {
    	points = {
            { x = 300, y = 150 },
            { x = 400, y = 150 },
            { x = 350, y = 250 }
        },
    	red = 255, green = 0, blue = 0,
    }
<strong>    context.renderPolygon(obj) -- Render triangle
</strong>end)
</code></pre>

## `renderItemStack(object)`

Draws a 2D Item.

**Parameters:**

* `object` (table)

**Returns:**

* (boolean) Return true if successfully

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
register2DRenderer(function(context)
    local item = player.inventory.getStack(0)
    if item then
<strong>        context.renderItemStack{
</strong><strong>            x = 3, y = 34,
</strong><strong>    	    itemStack = item, scale = 0.75
</strong><strong>        }
</strong>    end
end
</code></pre>
