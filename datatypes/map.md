---
icon: map-location
---

# Map

## Variables

### **scale** (_number_)

### **locked** (_boolean_)

### **tracking\_position (**_boolean_**)**

### **unlimited\_tracking** (_boolean_)

### **dimension** (_string_)

### **center\_x** (_number_)

### **center\_z** (_number_)

### **is\_exploration\_map** (_boolean_)

### **tracked\_decoration\_count** (_number_)

### **banners** (_list_) { id, name, color, x, y, z }

### **decorations** (_list_) { type, x, y, rot, name }

### **frame\_markers** (_list_) { id, entity\_id, x, y, z, rotation }

### **color\_data** (_list_)

## Functions

### `getColor(x, z)`

Get color from map.

**Returns:**

* (number) Return color.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
local map_item = player.inventory.getStack(8)
local map_data = map_item.map
if map_data then
<strong>    local color = map_data.getColor(0, 0)
</strong>end
</code></pre>

### `updateColor(x, z, color)`

Update color in map.

**Returns:**

* (number) New color.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
local map_item = player.inventory.getStack(8)
local map_data = map_item.map
if map_data then
<strong>    map_data.updateColor(0, 0, 0) -- 0 color its transparent
</strong>end
</code></pre>
