---
icon: keyboard
---

# Input

## `setSelectedSlot(slot)`

**Parameters:**

* `slot` (number) (0-8 range).

**Returns:**

* (boolean) Return <mark style="color:$success;">**true**</mark> if successfully.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local status = player.input.setSelectedSlot(0)
</strong></code></pre>

## `getSelectedSlot()`

**Returns:**

* (number) Range 0-8.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local slot = player.input.getSelectedSlot()
</strong></code></pre>

## `silentUse(slot)`

**Returns:**

* (boolean) Return <mark style="color:$success;">**true**</mark> if successfully.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local status = player.input.silentUse(0)
</strong></code></pre>

## `leftClick()`

**Returns:**

* (boolean) Return <mark style="color:$success;">**true**</mark> if successfully.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local status = player.input.leftClick()
</strong></code></pre>

## `rightClick()`

**Returns:**

* (boolean) Return <mark style="color:$success;">**true**</mark> if successfully.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local status = player.input.rightClick()
</strong></code></pre>

## `setPressedSprinting(enable)`

**Parameters:**

* `enable` (boolean).

**Returns:**

* (boolean) Return <mark style="color:$success;">**true**</mark> if successfully.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>player.input.setPressedSprinting(true)
</strong></code></pre>

## `setPressedJump(enable)`

**Parameters:**

* `enable` (boolean).

**Returns:**

* (boolean) Return <mark style="color:$success;">**true**</mark> if successfully.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>player.input.setPressedJump(true)
</strong></code></pre>

## `setPressedSneak(enable)`

**Parameters:**

* `enable` (boolean).

**Returns:**

* (boolean) Return <mark style="color:$success;">**true**</mark> if successfully.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>player.input.setPressedSneak(true)
</strong></code></pre>

## `setPressedForward(enable)`

**Parameters:**

* `enable` (boolean).

**Returns:**

* (boolean) Return <mark style="color:$success;">**true**</mark> if successfully.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>player.input.setPressedForward(true)
</strong></code></pre>

## `setPressedBack(enable)`

**Parameters:**

* `enable` (boolean).

**Returns:**

* (boolean) Return <mark style="color:$success;">**true**</mark> if successfully.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>player.input.setPressedBack(true)
</strong></code></pre>

## `setPressedLeft(enable)`

**Parameters:**

* `enable` (boolean).

**Returns:**

* (boolean) Return <mark style="color:$success;">**true**</mark> if successfully.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>player.input.setPressedLeft(true)
</strong></code></pre>

## `setPressedRight(enable)`

**Parameters:**

* `enable` (boolean).

**Returns:**

* (boolean) Return <mark style="color:$success;">**true**</mark> if successfully.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>player.input.setPressedRight(true)
</strong></code></pre>

## `setPressedAttack(enable)`

**Parameters:**

* `enable` (boolean).

**Returns:**

* (boolean) Return <mark style="color:$success;">**true**</mark> if successfully.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>player.input.setPressedAttack(true)
</strong></code></pre>

## `setPressedUse(enable)`

**Parameters:**

* `enable` (boolean).

**Returns:**

* (boolean) Return <mark style="color:$success;">**true**</mark> if successfully.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>player.input.setPressedUse(true)
</strong></code></pre>

## `isPressedSprinting()`

**Returns:**

* (boolean) Return <mark style="color:$success;">**true**</mark> if pressed.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local pressed = player.input.isPressedSprinting()
</strong></code></pre>

## `isPressedJump()`

**Returns:**

* (boolean) Return <mark style="color:$success;">**true**</mark> if pressed.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local pressed = player.input.isPressedJump()
</strong></code></pre>

## `isPressedSneak()`

**Returns:**

* (boolean) Return <mark style="color:$success;">**true**</mark> if pressed.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local pressed = player.input.isPressedSneak()
</strong></code></pre>

## `isPressedForward()`

**Returns:**

* (boolean) Return <mark style="color:$success;">**true**</mark> if pressed.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local pressed = player.input.isPressedForward()
</strong></code></pre>

## `isPressedBack()`

**Returns:**

* (boolean) Return <mark style="color:$success;">**true**</mark> if pressed.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local pressed = player.input.isPressedBack()
</strong></code></pre>

## `isPressedLeft()`

**Returns:**

* (boolean) Return <mark style="color:$success;">**true**</mark> if pressed.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local pressed = player.input.isPressedLeft()
</strong></code></pre>

## `isPressedRight()`

**Returns:**

* (boolean) Return <mark style="color:$success;">**true**</mark> if pressed.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local pressed = player.input.isPressedRight()
</strong></code></pre>

## `isPressedAttack()`

**Returns:**

* (boolean) Return <mark style="color:$success;">**true**</mark> if pressed.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local pressed = player.input.isPressedAttack()
</strong></code></pre>

## `isPressedUse()`

**Returns:**

* (boolean) Return <mark style="color:$success;">**true**</mark> if pressed.

**Example Usage:**

<pre class="language-lua"><code class="lang-lua">-- Example code showing how to use the function
<strong>local pressed = player.input.isPressedUse()
</strong></code></pre>
