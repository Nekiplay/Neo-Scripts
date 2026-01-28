---
icon: gears
---

# Methods

## `registerCommand(command, function(commandName, args, player))`

Send get request.

**Parameters:**

* `command` (string).
* `function` (function).

**Example Usage:**

```lua
-- Example code showing how to use the function
registerCommand("hello", function(commandName, args, playerName)
    print("Hello, " .. playerName .. "!")
    print("Command: " .. commandName)
    print("Arguments: " .. table.concat(args, ", "))
end)
```
