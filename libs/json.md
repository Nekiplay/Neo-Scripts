---
icon: file-lines
---

# Json

## `parse(json)`

Parse string to table

**Parameters:**

* `json` (string).

**Return:**&#x20;

* table

**Example Usage:**

```lua
-- Example code showing how to use the function
local response = http.get("http://ip-api.com/json/24.48.0.1", 5000)
local js = json.parse(response)
player.addMessage(js.country)
```

## `stringify(table)`

Converts a table to a json string

**Parameters:**

* table (table).

**Return:**&#x20;

* string

**Example Usage:**

```lua
-- Example code showing how to use the function
local position = {
    x = 0,
    y = 0,
    z = 0
}
local js = json.stringify(position)
player.addMessage(js)
```
