---
description: A library for creating various game classes using Lua.
icon: plus
---

# Creator

## `createBox(minX, minY, minZ, maxX, maxY, maxZ)`

Create [box](../datatypes/box.md).

**Parameters:**

* `minX` (double).
* `minY` (double).
* `minZ` (double).
* `maxX` (double).
* `maxY` (double).
* `maxZ` (double).

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local box = creator.createBox(0, 0, 0, 1, 1 ,1)
</strong>registerWorldRenderer(function(context)
    local filled = {
        box = box,
        red = 255, green = 0, blue = 0, alpha = 140,
        through_walls = false
    }
    context.renderFilled(filled)
end)
</code></pre>

## `createItemStackFromId(id)`

Create [item](../datatypes/item.md).

**Parameters:**

* `id` (string).

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local hypa = creator.createItemStackFromId("HYPERION")
</strong>register2DRenderer(function(context)
    if hypa then
        context.renderItemStack{
            x = 5, y = 5,
    	    itemStack = hypa , scale = 0.75
        }
    end
end
</code></pre>
