---
description: Implementation AABB
icon: square-full
---

# Box

## Variables

### **minX** (_number_)

### **minY** (_number_)

### **minZ** (_number_)

### **maxX** (_number_)

### **maxY** (_number_)

### **maxZ** (_number_)

### **min** (_table_) { x, y, z }

### **max** (_table_) { x, y, z }

## Functions

### `getSize()`

Return box size.

**Returns:**

* (number) Box size.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
local box = player.entity.box
<strong>local size = box.getSize()
</strong></code></pre>

### `getXSize()`

Return box x size.

**Returns:**

* (number) Box x size.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
local box = player.entity.box
<strong>local size = box.getXSize()
</strong></code></pre>

### `getZSize()`

Return box z size.

**Returns:**

* (number) Box z size.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
local box = player.entity.box
<strong>local size = box.getZSize()
</strong></code></pre>

### `getYSize()`

Return box y size.

**Returns:**

* (number) Box y size.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
local box = player.entity.box
<strong>local size = box.getYSize()
</strong></code></pre>

### `getCenter()`

Return box y size.

**Returns:**

* (table) { x, y, z }.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
local box = player.entity.box
<strong>local center = box.getCenter()
</strong>local x = center.x
local y = center.y
local z = center.z
</code></pre>

### `setMinX(number)`

Change box mix x size.

**Returns:**

* ([Box](box.md)) Return new box.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
local box = player.entity.box
<strong>local newBox = box.setMinX(1)
</strong></code></pre>

### `setMinY(number)`

Change box mix y size.

**Returns:**

* ([Box](box.md)) Return new box.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
local box = player.entity.box
<strong>local newBox = box.setMinY(1)
</strong></code></pre>

### `setMinZ(number)`

Change box mix z size.

**Returns:**

* ([Box](box.md)) Return new box.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
local box = player.entity.box
<strong>local newBox = box.setMinZ(1)
</strong></code></pre>

### `setMaxX(number)`

Change box max x size.

**Returns:**

* ([Box](box.md)) Return new box.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
local box = player.entity.box
<strong>local newBox = box.setMaxX(1)
</strong></code></pre>

### `setMaxY(number)`

Change box max y size.

**Returns:**

* ([Box](box.md)) Return new box.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
local box = player.entity.box
<strong>local newBox = box.setMaxY(1)
</strong></code></pre>

### `setMaxZ(number)`

Change box max z size.

**Returns:**

* ([Box](box.md)) Return new box.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
local box = player.entity.box
<strong>local newBox = box.setMaxZ(1)
</strong></code></pre>

### `expand(x, y, z)`

Expand box.

**Returns:**

* ([Box](box.md)) Return new box.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
local box = player.entity.box
<strong>local newBox = box.expand(0.0, 2.0, 0.0)
</strong></code></pre>

### `inflate(x, y, z)`

Inflate box.

**Returns:**

* ([Box](box.md)) Return new box.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
local box = player.entity.box
<strong>local newBox = box.inflate(0.0, 2.0, 0.0)
</strong></code></pre>

### `deflate(x, y, z)`

Deflate box.

**Returns:**

* ([Box](box.md)) Return new box.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
local box = player.entity.box
<strong>local newBox = box.deflate(1.0, 1.0, 1.0)
</strong></code></pre>

### `intersect(x, y, z)`

Intersect box.

**Returns:**

* ([Box](box.md)) Return new box.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
local box = player.entity.box
<strong>local newBox = box.intersect(1.0, 1.0, 1.0)
</strong></code></pre>

### `move(x, y, z)`

Move box.

**Returns:**

* ([Box](box.md)) Return new box.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
local box = player.entity.box
<strong>local newBox = box.move(0.0, 2.0, 0.0)
</strong></code></pre>

