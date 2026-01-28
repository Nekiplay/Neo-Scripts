---
icon: user-alien
---

# Player

## Variables

### [input](input.md)

### [inventory](inventory.md)

### [entity](../../datatypes/entity.md)

### [fishHook](../../datatypes/entity.md)

## Functions

### `addMessage(text)`

Add message to chat.

**Returns:**

* (boolean) Return <mark style="color:$success;">**true**</mark> if successfully.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>player.addMessage("Hypixel Cry - Only me see this")
</strong></code></pre>

### `sendMessage(text)`

Send message to server.

**Returns:**

* (boolean) Return <mark style="color:$success;">**true**</mark> if successfully.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>player.sendMessage("Hypixel Cry - All see this")
</strong></code></pre>

### `sendCommand(text)`

Send command to server.

**Returns:**

* (boolean) Return <mark style="color:$success;">**true**</mark> if successfully.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>player.sendCommand("/warp hub")
</strong></code></pre>

### `getProfile()`

Returns the player profile.

**Returns:**

* (string) Profile name.

**Example Usage:**

```lua
-- Example code showing how to use the function
local name = player.getProfile()
```

### `getProfileId()`

Returns the player profile id.

**Returns:**

* (string) Profile id.

**Example Usage:**

```lua
-- Example code showing how to use the function
local id = player.getProfileId()
```

### `getName()`

Returns the player name.

**Returns:**

* (string) Player name.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local name = player.getName()
</strong></code></pre>

### `getRank()`

Returns the player rank.

**Returns:**

* (string) Player rank.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local rank = player.getRank()
</strong></code></pre>

### `getRotation()`

Returns the player rotation.

**Returns:**

* (table) - Position table (yaw, pitch)

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local rotation = player.getRotation()
</strong>local yaw = rotation.yaw -- Number
local pitch = rotation.pitch -- Number
</code></pre>

### `setRotation(yaw, pitch)`

Returns the player rotation.

**Parameters:**

* `yaw` (number) - Player yaw.
* `pitch` (number) - Player yaw.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>player.setRotation(90, 90)
</strong></code></pre>

### `getPos()`

Returns the player position.

**Returns:**

* (table) - Position table (x, y, z)

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local pos = player.getPos()
</strong>local x = pos.x -- Number
local y = pos.y -- Number
local z = pos.z -- Number
</code></pre>

### `getLocation()`

Returns the player location.

**Returns:**

* (string) Player location.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local location = player.getLocation()
</strong></code></pre>

### `getArea()`

Returns the player area.

**Returns:**

* (string) Player area.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local area = player.getArea()
</strong></code></pre>

### `getPurse()`

Returns the player purse.

**Returns:**

* (number) Player purse.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local purse = player.getPurse()
</strong></code></pre>

### `getBits()`

Returns the player bits.

**Returns:**

* (number) Player bits.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local bits = player.getBits()
</strong></code></pre>

### `getHealth()`

Returns the player health.

**Returns:**

* (number) Player health.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local health = player.getHealth()
</strong></code></pre>

### `getMaxHealth()`

Returns the player max health.

**Returns:**

* (number) Player max health.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local maxHealth = player.getMaxHealth()
</strong></code></pre>

### `getMana()`

Returns the player mana.

**Returns:**

* (number) Player mana.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local mana = player.getMana()
</strong></code></pre>

### `getMaxMana()`

Returns the player max mana.

**Returns:**

* (number) Player max mana.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local maxMana = player.getMaxMana()
</strong></code></pre>

### `getDefence()`

Returns the player defence.

**Returns:**

* (number) Player defence.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local defence = player.getDefence()
</strong></code></pre>

### `getSpeed()`

Returns the player speed.

**Returns:**

* (number) Player speed.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local speed = player.getSpeed()
</strong></code></pre>

### `getAir()`

Returns the player oxygen.

**Returns:**

* (number) Player oxygen.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local oxygen = player.getAir()
</strong></code></pre>

### `getMaxAir()`

Returns the player max oxygen.

**Returns:**

* (number) Player max oxygen.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local max_oxygen = player.getMaxAir()
</strong></code></pre>

### `getCold()`

Returns the player cold in glacite tunnels.

**Returns:**

* (number) Player cold.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local cold = player.getCold()
</strong></code></pre>

### `isSneaking()`

Returns the player is sneaking.

**Returns:**

* (boolean) is player sneaking.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local isSneaking = player.isSneaking()
</strong></code></pre>

### `isSprinting()`

Returns the player is sprinting.

**Returns:**

* (boolean) is player sprinting.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local isSprinting = player.isSprinting()
</strong></code></pre>

### `isOnGround()`

Returns the player is on ground.

**Returns:**

* (boolean) is player on ground.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local isOnGround = player.isOnGround()
</strong></code></pre>

### `isOnSkyBlock()`

Returns the player is on skyblock.

**Returns:**

* (boolean) is player on skyblock.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local isOnSkyBlock = player.isOnSkyBlock()
</strong></code></pre>

Returns scoreboard lines.

### `getPet()`

**Returns:**

* (table) pet info.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local pet = player.getPet()
</strong>
local name = pet.name
local exp = pet.exp
local type = pet.type
local item = pet.item
</code></pre>

### `getScoreBoardLines()`

**Returns:**

* (table) list of lines.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local scoreboard = player.getScoreBoardLines()
</strong>
for index, line in ipairs(scoreboard) do
    player.addMessage(line)
end
</code></pre>

### `getTab()`

**Returns:**

* (table) list of lines.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local tab = player.getTab()
</strong>if tab.header then
    player.addMessage(tab.header)
end    

if tab.body then
    for index, line in ipairs(tab.body) do
        player.addMessage(line)
    end
end

if tab.footer then
    player.addMessage(tab.footer)
end
</code></pre>

### `getEyePosition()`

Returns player eye position.

**Returns:**

* (table) eye position (x, y, z).

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local eyePostion = player.getEyePosition()
</strong>local x = eyePostion.x -- Number
local y = eyePostion.y -- Number
local z = eyePostion.z -- Number
</code></pre>

### `addToast(title, description, time)`

Returns player eye position.

**Parameters:**

* title (string)
* description (title)
* time (number milliseconds)

**Example Usage:**

```lua
-- Example code showing how to use the function
player.addToast("Hypixel Cry", "Script has new version", 10000) -- Show toast to 10 seconds
```

### `getLookEndPos(number)`

Returns player eye end position.

**Parameters:**

* `distance` (number) - Distance from the player's view.

**Returns:**

* (table) eye end position (x, y, z).

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local lookEndPosition = player.getLookEndPos(5.0)
</strong>local x = lookEndPosition.x -- Number
local y = lookEndPosition.y -- Number
local z = lookEndPosition.z -- Number
</code></pre>

### `getLookEndPos(table)`

Returns player eye end position.

**Parameters:**

* `table` (x, y, z) - Table with end point coordinates.

**Returns:**

* (table) eye end position (x, y, z).

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
local look = {
   x = 1,
   y = 1,
   z = 1
}

<strong>local lookEndPosition = player.getLookEndPos(look)
</strong>local x = lookEndPosition.x -- Number
local y = lookEndPosition.y -- Number
local z = lookEndPosition.z -- Number
</code></pre>

### `raycast(number)`

Returns raycast result from eye.

**Parameters:**

* `number` - Raycast distances from eyes.

**Returns:**

**Global**

* (table) Result of raycast.

**If block**

* (table) (type, x, y, z) Result of raycast.

**If entity**

* (table) (type, [entityData](../../datatypes/entity.md)) Result of raycast.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local raycastResult = player.raycast(4.5)
</strong>if raycastResult == nil then
   print("MISS")
else
   local resultType = raycastResult.type
   if resultType == "block" then
      local x = raycastResult.blockPos.x -- Number
      local y = raycastResult.blockPos.y -- Number
      local z = raycastResult.blockPos.z -- Number
   elseif resultType == "entity" then
      local entityData = raycastResult.data
   end
end
</code></pre>
