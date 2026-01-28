---
icon: globe
---

# Http

## `get(url, timeout)`

Send get request.

**Parameters:**

* `url` (string).
* `timeout` (number).

**Example Usage:**

```lua
-- Example code showing how to use the function
local response = http.get("http://ip-api.com/json/24.48.0.1", 5000)
local js = json.parse(response)
player.addMessage(js.country)
```

## `get_with_headers(url, headers)`

Send get request.

**Parameters:**

* `url` (string).
* `headers` (table).

**Example Usage:**

```lua
-- Example code showing how to use the function
local headers = {["User-Agent"] = "LuaJ-HTTP-Client/1.0"}
local response = http.get_with_headers("http://ip-api.com/json/24.48.0.1", headers)
local js = json.parse(response)
player.addMessage(js.country)
```

## `get_async_callback(url, function(response, error))`

Send async get request.

**Parameters:**

* `url` (string).
* `function` (function).

**Example Usage:**

```lua
-- Example code showing how to use the function
http.get_async_callback("http://ip-api.com/json/88.88.88.88", function(response, error)
    if error then
        print("Error:", error)
    else
        local js = json.parse(response)
        player.addMessage(js.country)
    end
end)
```

## `get_async_with_headers_callback(url, headers, function(response, error))`

Send async get request.

**Parameters:**

* `url` (string).
* `headers` (table).
* `function` (function).

**Example Usage:**

```lua
-- Example code showing how to use the function
local headers = {["User-Agent"] = "LuaJ-HTTP-Client/1.0"}
http.get_async_with_headers_callback("http://ip-api.com/json/88.88.88.88", headers, function(response, error)
    if error then
        print("Error:", error)
    else
        local js = json.parse(response)
        player.addMessage(js.country)
    end
end)
```

## `post_async_with_headers_callback(url, headers, json_body, function(response, error))`

Send async post request.

**Parameters:**

* `url` (string).
* `headers` (table).
* `json_body` (string).
* `function` (function).

**Example Usage:**

```lua
-- Example code showing how to use the function
local headers = {
    ['Authorization'] = 'Bearer ',
    ['HTTP-Referer'] = 'https://github.com/Nekiplay/Hypixel-Cry',
    ['X-Title'] = 'Hypixel Cry',
    ['Content-Type'] = 'application/json'
}

local body_data = {
    model = 'deepseek/deepseek-r1:free',
    prompt = "Hello"
}

local json_body = json.stringify(body_data)

http.post_async_with_headers_callback(
    'https://openrouter.ai/api/v1/chat/completions',
    headers,
    json_body,
    function(response, error)
        if error then
            player.addMessage(error)
        else
            local result = json.parse(response)
            player.addMessage("AI response: '" .. result.choices[0].message.text .. "'")
        end
    end
)
```
