local t_insert = table.insert
local t_remove = table.remove
local m_floor = math.floor
local m_max = math.max
local m_min = math.min
local bit = require("bit")
local b_lshift = bit.lshift
local b_or = bit.bor

-- Константы
local MAX_RANGE = 22
local SEARCH_RANGE = 20
local SEARCH_HEIGHT = 4
local BLOCKS_PER_Y_LEVEL = 1	 -- Сколько уровней Y сканировать за один тик (для асинхронности)

-- Состояние
local lastPlayerPos = {x = 0, y = 0, z = 0}
local connections = {} -- Текущие данные для отрисовки
local isSearching = false -- Флаг работы корутины

-- Оптимизированный генератор ключа (быстрее строк)
-- Предполагаем координаты в пределах +-1000000
local function getCoordKey(x, y, z)
    return b_or(b_or(b_lshift(x + 1048576, 42), b_lshift(y + 1024, 21)), (z + 1048576))
end

-- Кэш для коробок (чтобы не создавать новые таблицы в рендере)
local renderBox = { red = 255, green = 255, blue = 85, alpha = 170, through_walls = true }
local lineBox = { red = 255, green = 85, blue = 85, alpha = 170, through_walls = true }
local lineBox2 = { red = 255, green = 255, blue = 85, alpha = 170, through_walls = true }

-- Проверка соединения (вынесено для удобства)
local function areHooksConnected(h1, h2, wireMap)
    if h1.y ~= h2.y then return false end
    local dx = h2.x - h1.x
    local dz = h2.z - h1.z
    local absDx = dx < 0 and -dx or dx
    local absDz = dz < 0 and -dz or dz
    
    local steps = m_max(absDx, absDz)
    if steps < 2 or steps > MAX_RANGE then return false end
    
    if (absDx > 0 and absDz == 0) or (absDz > 0 and absDx == 0) then
        local stepX = (dx ~= 0) and (dx / absDx) or 0
        local stepZ = (dz ~= 0) and (dz / absDz) or 0
        for step = 1, steps - 1 do
            if not wireMap[getCoordKey(h1.x + stepX * step, h1.y, h1.z + stepZ * step)] then
                return false
            end
        end
        return true
    end
    return false
end

-- Асинхронная функция поиска и обновления
local function asyncUpdateTask(cx, cy, cz)
    isSearching = true
    local trapwires = {}
    local wireMap = {}
    local hooks = {}
    local twCount = 0
    local hCount = 0

    -- 1. Сканирование мира (разбито на части)
    for dy = -SEARCH_HEIGHT, SEARCH_HEIGHT do
        local y = cy + dy
        for dx = -SEARCH_RANGE, SEARCH_RANGE do
            local x = cx + dx
            for dz = -SEARCH_RANGE, SEARCH_RANGE do
                local z = cz + dz
                
                local blockInfo = world.getBlock(x, y, z)
                if blockInfo then
                    local name = blockInfo.name
                    if name == "block.minecraft.tripwire" then
                        wireMap[getCoordKey(x, y, z)] = true
                    elseif name == "block.minecraft.tripwire_hook" then
                        hCount = hCount + 1
                        hooks[hCount] = { x = x, y = y, z = z, state = blockInfo }
                    end
                end
            end
        end
        -- Каждые BLOCKS_PER_Y_LEVEL уровней высоты "отдыхаем", отдавая приоритет игре
        if dy % BLOCKS_PER_Y_LEVEL == 0 then
            coroutine.yield()
        end
    end

    -- 2. Вычисление соединений
    local newConnections = {}
    for i = 1, hCount do
        local h1 = hooks[i]
        local h1_info = h1.state
        if h1_info and h1_info.facing then
            for j = i + 1, hCount do
                local h2 = hooks[j]
                local h2_info = h2.state
                
                if h2_info and h2_info.facing and areHooksConnected(h1, h2, wireMap) then
                    local f1 = h1_info.facing
                    local f2 = h2_info.facing
                    if f1.name == f2.opposite.name then
                        
                        -- Сразу считаем финальные координаты боксов
                        local isXAxis = h1.z == h2.z
                        local boxLine = { 
                            y = h1.y + 0.06, y2 = h1.y + 0.17,
                            x = 0, x2 = 0, z = 0, z2 = 0
                        }
                        
                        if isXAxis then
                            local minX, maxX = m_min(h1.x, h2.x), m_max(h1.x, h2.x)
                            boxLine.x, boxLine.x2 = minX + 1, maxX
                            boxLine.z, boxLine.z2 = h1.z + 0.02, h1.z + 0.98
                        else
                            local minZ, maxZ = m_min(h1.z, h2.z), m_max(h1.z, h2.z)
                            boxLine.z, boxLine.z2 = minZ + 1, maxZ
                            boxLine.x, boxLine.x2 = h1.x + 0.02, h1.x + 0.98
                        end

                        local w = 0.04
                        local conn1 = { y = h1.y + 0.1, y2 = h1.y + 0.13 }
                        local conn2 = { y = h1.y + 0.1, y2 = h1.y + 0.13 }

                        if isXAxis then
                            local lx, rx = m_min(h1.x, h2.x), m_max(h1.x, h2.x)
                            conn1.x, conn1.x2 = lx + 0.4, boxLine.x
                            conn1.z, conn1.z2 = h1.z + 0.5 - w, h1.z + 0.5 + w
                            conn2.x, conn2.x2 = boxLine.x2, rx + 0.6
                            conn2.z, conn2.z2 = h1.z + 0.5 - w, h1.z + 0.5 + w
                        else
                            local nz, fz = m_min(h1.z, h2.z), m_max(h1.z, h2.z)
                            conn1.z, conn1.z2 = nz + 0.4, boxLine.z
                            conn1.x, conn1.x2 = h1.x + 0.5 - w, h1.x + 0.5 + w
                            conn2.z, conn2.z2 = boxLine.z2, fz + 0.6
                            conn2.x, conn2.x2 = h1.x + 0.5 - w, h1.x + 0.5 + w
                        end

                        t_insert(newConnections, {
                            b1 = world.getOutlineBoxes(h1.x, h1.y, h1.z, h1_info),
                            h1 = {x = h1.x, y = h1.y, z = h1.z},
                            b2 = world.getOutlineBoxes(h2.x, h2.y, h2.z, h2_info),
                            h2 = {x = h2.x, y = h2.y, z = h2.z},
                            line = boxLine,
                            c1 = conn1, c2 = conn2
                        })
                    end
                end
            end
        end
    end

    connections = newConnections
    isSearching = false
end

local routine = nil

registerClientTick(function()
    -- Если уже ищем, продолжаем выполнение корутины
    if routine and coroutine.status(routine) ~= "dead" then
        local status, err = coroutine.resume(routine)
        if not status then print("[Error]: " .. err) end
        return
    end

    -- Проверка дистанции для запуска нового поиска
    local pos = player.getPos()
    if not pos then return end
    
    local cx, cy, cz = m_floor(pos.x), m_floor(pos.y), m_floor(pos.z)
    local dx, dy, dz = cx - lastPlayerPos.x, cy - lastPlayerPos.y, cz - lastPlayerPos.z
    
    if not isSearching then 
        lastPlayerPos.x, lastPlayerPos.y, lastPlayerPos.z = cx, cy, cz
        routine = coroutine.create(function() asyncUpdateTask(cx, cy, cz) end)
    end
end)

registerWorldRenderer(function(context)
    local currentConns = connections -- Атомарная ссылка
    for i = 1, #currentConns do
        local c = currentConns[i]
        
        -- Рендер основной нити
        local l = c.line
        lineBox.x, lineBox.y, lineBox.z = l.x, l.y, l.z
        lineBox.x2, lineBox.y2, lineBox.z2 = l.x2, l.y2, l.z2
        context.renderFilled(lineBox)
        
        -- Соединители
        local c1, c2 = c.c1, c.c2
        lineBox2.x, lineBox2.y, lineBox2.z = c1.x, c1.y, c1.z
        lineBox2.x2, lineBox2.y2, lineBox2.z2 = c1.x2, c1.y2, c1.z2
        context.renderFilled(lineBox2)
        
        lineBox2.x, lineBox2.y, lineBox2.z = c2.x, c2.y, c2.z
        lineBox2.x2, lineBox2.y2, lineBox2.z2 = c2.x2, c2.y2, c2.z2
        context.renderFilled(lineBox2)
        
        -- Крюки (минимум вычислений в рендере)
        local h1, b1 = c.h1, c.b1
        for j = 1, #b1 do
            local b = b1[j]
            renderBox.x, renderBox.y, renderBox.z = b.minX + h1.x, b.minY + h1.y, b.minZ + h1.z
            renderBox.x2, renderBox.y2, renderBox.z2 = b.maxX + h1.x, b.maxY + h1.y, b.maxZ + h1.z
            context.renderFilled(renderBox)
        end
        
        local h2, b2 = c.h2, c.b2
        for j = 1, #b2 do
            local b = b2[j]
            renderBox.x, renderBox.y, renderBox.z = b.minX + h2.x, b.minY + h2.y, b.minZ + h2.z
            renderBox.x2, renderBox.y2, renderBox.z2 = b.maxX + h2.x, b.maxY + h2.y, b.maxZ + h2.z
            context.renderFilled(renderBox)
        end
    end
end)