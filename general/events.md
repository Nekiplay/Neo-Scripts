---
icon: bell
---

# Events

### `registerUnloadCallback(function)`

Registers a tick handler

**Parameters:**

* `function` (function).

**Example Usage:**

```lua
-- Example code showing how to use the function

registerUnloadCallback(function()
    print("Script unloaded")
end)
```

### `registerClientTick(function)`

Registers a tick handler

**Parameters:**

* `function` (function).

**Example Usage:**

```lua
-- Example code showing how to use the function
local tick = 0
registerClientTick(function()
    tick = tick + 1
end)
```

### `registerWorldRenderer(function(context))`

Registers a world renderer handler

**Parameters:**

* `function` (function ([WorldRendererObject](../rendering-objects/world-renderer.md))).

**Example Usage:**

```lua
-- Example code showing how to use the function
registerWorldRenderer(function(context)
    local filled = {
        x = 0, y = 0, z = 0,
        red = 255, green = 0, blue = 0, alpha = 140,
        through_walls = false
    }
    context.renderFilled(filled)
end)
```

### `register2DRenderer(function(context))`

Registers a world renderer handler

**Parameters:**

* `function` (function ([2DRendererObject](../rendering-objects/2d-renderer.md))).

**Example Usage:**

```lua
-- Example code showing how to use the function
register2DRenderer(function(context)
    local obj2 = {
    	x = 3, y = 3, scale = 1,
    	text = "§6Hypixel Cry §7v1.1.3",
    	red = 0, green = 0, blue = 0
    }
    context.renderText(obj2)
    
    local obj3 = {
    	x = 3, y = 13, scale = 0.75,
    	text = "§7by §bNeki_play§7, §bKreedMan",
    	red = 0, green = 0, blue = 0
    }
    context.renderText(obj3)
end)
```

### `registerLocationChangeEvent(function(location))`

Registers a world renderer handler

**Parameters:**

* `function` (function ([2DRendererObject](../rendering-objects/2d-renderer.md))).

**Example Usage:**

```lua
-- Example code showing how to use the function
registerLocationChangeEvent(function(location)
    print(location)
end)
```

### `registerMessageEvent(function(text, overlay))`

Registers a world renderer handler

**Parameters:**

* `function` (function (text, overlay)).

**Example Usage:**

<pre class="language-lua"><code class="lang-lua"><strong>-- Example code showing how to use the function
</strong>registerMessageEvent(function(text, overlay)
    if text and not overlay then
        print(text)
    end
end)
</code></pre>

### `registerSendMessageEvent(function(text))`

Registers a world renderer handler

**Parameters:**

* `function` (function (text, overlay)).

**Example Usage:**

<pre class="language-lua"><code class="lang-lua"><strong>-- Example code showing how to use the function
</strong>registerSendMessageEvent(function(text)
    if text then
        print(text)
    end
end)
</code></pre>

### `registerSendCommandEvent(function(text))`

Registers a world renderer handler

**Parameters:**

* `function` (function (text, overlay)).

**Example Usage:**

<pre class="language-lua"><code class="lang-lua"><strong>-- Example code showing how to use the function
</strong>registerSendCommandEvent(function(text)
    if text then
        print(text)
    end
end)
</code></pre>

### `registerKeyEvent(function(key, action))`

Registers a keyboard and mouse event

**Parameters:**

* `function` (function (key, action)).

**Example Usage:**

```lua
-- Example code showing how to use the function
registerKeyEvent(function(key, action)
    if action == "Release" then

    elseif action == "Press" then

    end
end)
```

### `registerBlockUpdate(function(update))`

Registers a keyboard and mouse event

**Parameters:**

* `function` (function ([old](../datatypes/block.md), [new](../datatypes/block.md))).

**Example Usage:**

```lua
-- Example code showing how to use the function
registerBlockUpdate(function(update)
    local x = update.x
    local y = update.y
    local z = update.z
    local oldState = update.old
    local newState = update.new
end)
```

### `registerServerSideRotationEvent(function(yaw, pitch))`

Registers a player rotations from server

**Parameters:**

* `function` (function (yaw, pitch)).

**Example Usage:**

```lua
-- Example code showing how to use the function
registerServerSideRotationEvent(function(yaw, pitch)	
	if yaw and pitch then
		if (yaw ~= config.rotations.yaw and pitch ~= config.rotations.pitch) and 
		(pitch ~= 0 and yaw ~= 0) and 
		(pitch ~= 0 and yaw ~= 90) and 
		(pitch ~= 0 and yaw ~= 180) and 
		(pitch ~= 0 and yaw ~= 270)
		then
			notifications.snowNotifty("Farm Macro", "You got rotated!")
		end
	end
end)
```

### `registerServerTeleportEvent(function(yaw, pitch))`

Registers a player rotations from server

**Parameters:**

* `function` (function (yaw, pitch)).

**Example Usage:**

```lua
-- Example code showing how to use the function
registerServerSideTeleportEvent(function(x, y, z)
	if x and y and z then
		notifications.snowNotifty("Farm Macro", "You got teleported!")
	end
end)
```
