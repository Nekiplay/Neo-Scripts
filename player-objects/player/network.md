---
description: Network functions
icon: wifi
---

# Network

### `getPlayersList()`

Get players list.

**Returns:**

* (table) Return list players.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local list = player.network.getPlayersList()
</strong>for index, pl in pairs(list) do
    if pl.name then
        player.addMessage(pl.name)
    end
    if pl.display_name then
        player.addMessage(pl.display_name)
    end
    player.addMessage(tostring(pl.latency))
    if pl.id then
        player.addMessage(tostring(pl.id))
    end
    if pl.gamemode then
        player.addMessage(pl.gamemode)
    end
    if pl.skin_texture then
        player.addMessage(pl.skin_texture)
    end
end
</code></pre>

### `sendStartDestroyBlockPacket(x, y, z, direction)`

Send block breaking packet.

**Parameters:**

* `x` (number).
* `y` (number).
* `z` (number).
* `direction` (string) <mark style="color:orange;">UP</mark>, <mark style="color:orange;">DOWN</mark>, <mark style="color:orange;">NORTH</mark>, <mark style="color:orange;">SOUTH</mark>, <mark style="color:orange;">WEST</mark>, <mark style="color:orange;">EAST</mark>

**Returns:**

* (boolean) Return <mark style="color:$success;">**true**</mark> if successfully.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>player.network.sendStartDestroyBlockPacket(0, 0, 0, "DOWN")
</strong></code></pre>

### `sendStopDestroyBlockPacket(x, y, z, direction)`

Send block stop breaking packet.

**Parameters:**

* `x` (number).
* `y` (number).
* `z` (number).
* `direction` (string) <mark style="color:orange;">UP</mark>, <mark style="color:orange;">DOWN</mark>, <mark style="color:orange;">NORTH</mark>, <mark style="color:orange;">SOUTH</mark>, <mark style="color:orange;">WEST</mark>, <mark style="color:orange;">EAST</mark>

**Returns:**

* (boolean) Return <mark style="color:$success;">**true**</mark> if successfully.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>player.network.sendStopDestroyBlockPacket(0, 0, 0, "DOWN")
</strong></code></pre>

### `sendStopAbortBlockPacket(x, y, z, direction)`

Send cancel block breaking bpacket.

**Parameters:**

* `x` (number).
* `y` (number).
* `z` (number).
* `direction` (string) <mark style="color:orange;">UP</mark>, <mark style="color:orange;">DOWN</mark>, <mark style="color:orange;">NORTH</mark>, <mark style="color:orange;">SOUTH</mark>, <mark style="color:orange;">WEST</mark>, <mark style="color:orange;">EAST</mark>

**Returns:**

* (boolean) Return <mark style="color:$success;">**true**</mark> if successfully.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>player.network.sendAbortBlockPacket(0, 0, 0, "DOWN")
</strong></code></pre>
