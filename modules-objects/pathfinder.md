---
description: Managing the drawing of paths
icon: bezier-curve
---

# PathFinder

## `isHasPath(id)`

**Parameters:**

* `id` (string)

**Returns:**

* (boolean) Return (true) if has.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local isHasCustom = modules.pathFinder.isHasPath("Custom") -- Custom used for /path command
</strong></code></pre>

## `removePath(id)`

**Parameters:**

* `id` (string)

**Returns:**

* (boolean) Return true.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local status = modules.pathFinder.removePath("Custom") 
</strong></code></pre>

## `addOrUpdatePath(path)`

**Parameters:**

* `path` (table)

**Returns:**

* (boolean) Return true.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
local path = {
    x = 0,
    y = 0,
    z = 0,
    
    red = 255,
    green = 0,
    blue = 0,
    alpha = 140,
    
    id = "Custom",
    end_text = "Lua Path",
    
    smooth = true,
    updater = false
}

<strong>local status = modules.pathFinder.addOrUpdatePath(path) 
</strong></code></pre>

## `getPathBlocks(id)`

**Parameters:**

* `id` (string)

**Returns:**

* (table) Return list of blockpos.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local blocks = modules.pathFinder.getPathBlocks("Custom")
</strong>
for index, block in ipairs(blocks) do
    if block ~= nil then
        local x = block.x
        local y = block.y
        local z = block.z
    end
end
</code></pre>
