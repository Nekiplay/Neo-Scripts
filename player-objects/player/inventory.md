---
description: Inventory functions
icon: backpack
---

# Inventory

## `isAnyScreenOpened()`

Returns true if any screen opened.

**Returns:**

* (boolean).

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local isOpened = player.inventory.isAnyScreenOpened()
</strong></code></pre>

## `isSignOpened()`

Returns true if sign opened.

**Returns:**

* (boolean).

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local isOpened = player.inventory.isSignOpened()
</strong></code></pre>

## `getSignText(line)`

Returns line of sign.

**Returns:**

* (string).

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
if player.inventory.isSignOpened() then
<strong>    local line = player.inventory.getSignText(0)
</strong>end
</code></pre>

## `setSignText(line, text)`

Returns true if success.

**Returns:**

* (boolean) return <mark style="color:$success;">**true**</mark> is successfully.

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
if player.inventory.isSignOpened() then
<strong>    local status = player.inventory.setSignText(0, "1000")
</strong>end

</code></pre>

## `getChestTitle()`

Returns chest title.

**Returns:**

* (string).

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local title = player.inventory.getChestTitle()
</strong></code></pre>

## `getStack(slot)`

Returns the item in the slot.

**Parameters:**

* `slot` (number) - Slot id.

**Returns:**

* ([item data](../../datatypes/item.md)).

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local item = player.inventory.getStack(36)
</strong></code></pre>

## `getStackFromContainer(slot)`

Returns the item in the slot.

**Parameters:**

* `slot` (number) - Slot id.

**Returns:**

* ([item data](../../datatypes/item.md)).

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local item = player.inventory.getStackFromContainer(36)
</strong></code></pre>

## `getContainerSlots()`

Returns the number of slots in an open container.

**Returns:**

* (number) - Int.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local slots = player.inventory.getContainerSlots()
</strong></code></pre>

## `leftClick(slot)`

Returns true if successfully.

**Parameters:**

* `slot` (number) - Slot id.

**Returns:**

* (boolean).

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>player.inventory.leftClick(0)
</strong></code></pre>

## `rightClick(slot)`

Returns true if successfully.

**Parameters:**

* `slot` (number) - Slot id.

**Returns:**

* (boolean).

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>player.inventory.rightClick(0)
</strong></code></pre>

## `dropAll(slot)`

Returns true if successfully.

**Parameters:**

* `slot` (number) - Slot id.

**Returns:**

* (boolean).

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>player.inventory.dropAll(0)
</strong></code></pre>

## `closeScreen()`

Closes an open screen (chest, inventory, etc.)

**Returns:**

* (boolean).

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>player.inventory.closeScreen()
</strong></code></pre>

## `openInventory()`

Open player inventory

**Returns:**

* (boolean).

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>player.inventory.openInventory()
</strong></code></pre>
