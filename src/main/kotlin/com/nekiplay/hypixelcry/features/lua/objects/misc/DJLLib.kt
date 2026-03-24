package com.nekiplay.hypixelcry.features.lua.objects.misc

import ai.djl.Model
import ai.djl.inference.Predictor
import ai.djl.ndarray.NDArrays
import ai.djl.ndarray.NDArray
import ai.djl.ndarray.NDList
import ai.djl.ndarray.NDManager
import ai.djl.ndarray.types.Shape
import ai.djl.nn.*
import ai.djl.nn.core.Linear
import ai.djl.training.DefaultTrainingConfig
import ai.djl.training.EasyTrain
import ai.djl.training.Trainer
import ai.djl.training.dataset.ArrayDataset
import ai.djl.training.dataset.Dataset
import ai.djl.training.evaluator.Accuracy
import ai.djl.training.listener.TrainingListener
import ai.djl.training.loss.Loss
import ai.djl.training.optimizer.Adam
import ai.djl.training.tracker.Tracker
import java.util.concurrent.ConcurrentHashMap
import ai.djl.ndarray.types.DataType
import com.nekiplay.hypixelcry.features.lua.LuaManager
import party.iroiro.luajava.JFunction
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import java.nio.file.Paths

/**
 * Библиотека-обертка для доступа к DJL из LuaJ на Kotlin
 */
class DJLLib(val L: Lua) {

    val models = ConcurrentHashMap<String, Model>()
    val predictors = ConcurrentHashMap<String, Predictor<NDList, NDList>>()
    val manager: NDManager = NDManager.newBaseManager()
    val inputShapes = ConcurrentHashMap<String, LongArray>()
    val modelModes = ConcurrentHashMap<String, String>()

    init {
        val djlDir = LuaManager.configDir.resolve("hypixelcry/djl_cache/").toString() + "/"
        System.setProperty("DJL_CACHE_DIR", djlDir)
        System.setProperty("ENGINE_CACHE_DIR", djlDir)
        System.setProperty("DJL_OFFLINE", "true")
    }

    /**
     * Регистрирует библиотеку djl в глобальной области видимости Lua
     */
    fun register() {
        L.newTable() // Создаем таблицу библиотеки (будущий djl)

        L.push(JFunction { createModel(it) })
        L.setField(-2, "create_model")

        L.push(JFunction { train(it) })
        L.setField(-2, "train")

        L.push(JFunction { saveModel(it) })
        L.setField(-2, "save_model")

        L.push(JFunction { loadModel(it) })
        L.setField(-2, "load_model")

        L.push(JFunction { predict(it) })
        L.setField(-2, "predict")

        L.push(JFunction { close(it) })
        L.setField(-2, "close")

        L.push(JFunction { getModelInfo(it) })
        L.setField(-2, "get_model_info")

        L.setGlobal("djl")
    }

    // Вспомогательная функция: NDArray в таблицу Lua
    private fun luaToNDArray(l: Lua, tableIndex: Int, shape: LongArray): NDArray {
        val flatData = mutableListOf<Float>()

        // Рекурсивно собираем все числа из таблицы в плоский список
        flattenTable(l, tableIndex, flatData)

        // Создаем NDArray через менеджер DJL
        return manager.create(flatData.toFloatArray(), Shape(*shape))
    }

    private fun ndArrayToLua(l: Lua, array: NDArray) {
        val data = array.toFloatArray()
        l.newTable()
        data.forEachIndexed { index, value ->
            l.push(value.toDouble())
            // Используем rawSetI с Int индексом (Lua индексы с 1)
            l.rawSetI(-2, index + 1)
        }
    }

    // Рекурсивный обход таблицы для выравнивания
    private fun flattenTable(l: Lua, index: Int, data: MutableList<Float>) {
        if (!l.isTable(index)) {
            data.add(l.toNumber(index).toFloat())
            return
        }

        val len = l.rawLength(index)
        for (i in 1..len) {
            // Используем rawGetI с Int индексом
            l.rawGetI(index, i)
            flattenTable(l, -1, data)
            l.pop(1)
        }
    }

    private fun createModel(l: Lua): Int {
        if (!l.isString(1) || !l.isTable(2)) {
            l.pushNil()
            return 1
        }

        val id = l.toString(1) ?: return 0

        try {
            val model = Model.newInstance(id)

            l.getField(2, "input_size")
            val inputSize = if (l.isNumber(-1)) l.toNumber(-1).toInt() else 10
            l.pop(1)

            l.getField(2, "output_size")
            val outputSize = if (l.isNumber(-1)) l.toNumber(-1).toInt() else 1
            l.pop(1)

            l.getField(2, "mode")
            val mode = if (l.isString(-1)) l.toString(-1) else "classification"
            l.pop(1)

            modelModes[id] = mode!!
            inputShapes[id] = longArrayOf(1, inputSize.toLong())

            val block = SequentialBlock()

            l.getField(2, "layers")
            if (l.isTable(-1)) {
                val layersLen = l.rawLength(-1)
                for (i in 1..layersLen) {
                    l.rawGetI(-1, i) // Используем rawGetI
                    val units = l.toNumber(-1).toLong()
                    l.pop(1)

                    block.add(Linear.builder().setUnits(units).build())
                    block.add(Activation::relu)
                }
            }
            l.pop(1)

            block.add(Linear.builder().setUnits(outputSize.toLong()).build())

            model.block = block
            models[id] = model

            l.newTable()
            l.push(id)
            l.setField(-2, "id")
            l.push(mode)
            l.setField(-2, "mode")

            return 1
        } catch (e: Exception) {
            println("Failed to create model: ${e.message}")
            l.pushNil()
            return 1
        }
    }


    private fun train(l: Lua): Int {
        val modelId = l.toString(1) ?: return 0
        if (!l.isTable(2)) {
            println("Config required")
            l.pushNil()
            return 1
        }
        val configIdx = 2
        val trainDataIdx = 3
        val testDataIdx = 4
        val callbackIdx = 5

        val model = models[modelId] ?: run {
            println("Model not found: $modelId")
            l.pushNil()
            return 1
        }
        val mode = modelModes[modelId] ?: "classification"

        return try {
            // Читаем конфиг
            l.getField(configIdx, "epochs"); val epochs = if(l.isNumber(-1)) l.toNumber(-1).toInt() else 10; l.pop(1)
            l.getField(configIdx, "lr"); val learningRate = if(l.isNumber(-1)) l.toNumber(-1) else 0.001; l.pop(1)
            l.getField(configIdx, "batch_size"); val batchSize = if(l.isNumber(-1)) l.toNumber(-1).toInt() else 32; l.pop(1)
            l.getField(configIdx, "output_size"); val outputSize = if(l.isNumber(-1)) l.toNumber(-1).toInt() else 1; l.pop(1)

            var shapeArray = inputShapes[modelId] ?: longArrayOf(10)
            if (shapeArray.size > 1 && shapeArray[0] == 1L) {
                shapeArray = shapeArray.sliceArray(1 until shapeArray.size)
            }
            val trainingShape = Shape(*shapeArray)

            // Сборка датасетов
            val dataset = buildDataset(l, trainDataIdx, batchSize, shapeArray, outputSize, mode, model.ndManager)
            val testDataset = if (l.isTable(testDataIdx)) {
                buildDataset(l, testDataIdx, batchSize, shapeArray, outputSize, mode, model.ndManager)
            } else null

            val loss = if (mode == "regression") {
                Loss.l2Loss()
            } else {
                if (outputSize == 1) Loss.sigmoidBinaryCrossEntropyLoss() else Loss.softmaxCrossEntropyLoss()
            }

            val trainingConfig = DefaultTrainingConfig(loss)
                .optOptimizer(Adam.builder().optLearningRateTracker(Tracker.fixed(learningRate.toFloat())).build())

            if (mode == "regression") {
                trainingConfig.addEvaluator(loss)
            } else {
                trainingConfig.addEvaluator(Accuracy())
            }

            // Обработка коллбэка
            if (l.isFunction(callbackIdx)) {
                var epochIndex = 1
                trainingConfig.addTrainingListeners(object : TrainingListener {
                    override fun onEpoch(trainer: Trainer) {
                        try {
                            // 1. Копируем функцию из стека наверх, так как pCall её удалит
                            l.pushValue(callbackIdx)

                            // 2. Пушим аргумент (номер эпохи)
                            l.push(epochIndex.toDouble())

                            // 3. Вызываем функцию (1 аргумент, 0 результатов)
                            l.pCall(1, 0)

                            epochIndex++
                        } catch (e: LuaException) {
                            println("Error in DJL training callback: ${e.message}")
                        }
                    }
                    override fun onTrainingBatch(trainer: Trainer?, batchData: TrainingListener.BatchData?) {}
                    override fun onValidationBatch(trainer: Trainer?, batchData: TrainingListener.BatchData?) {}
                    override fun onTrainingBegin(trainer: Trainer?) {}
                    override fun onTrainingEnd(trainer: Trainer?) {}
                })
            }

            model.newTrainer(trainingConfig).use { trainer ->
                trainer.initialize(trainingShape)
                EasyTrain.fit(trainer, epochs, dataset, testDataset)
            }

            l.push(true)
            1
        } catch (e: Exception) {
            e.printStackTrace()
            l.pushNil()
            1
        }
    }

    private fun buildDataset(l: Lua, dataIdx: Int, batchSize: Int, inputShape: LongArray, outputSize: Int, mode: String, manager: NDManager): Dataset {
        l.getField(dataIdx, "inputs"); val inputsIdx = l.getTop();
        l.getField(dataIdx, "labels"); val labelsIdx = l.getTop();

        val inputList = NDList()
        val labelList = NDList()

        val len = l.rawLength(inputsIdx)
        for (i in 1..len) {
            l.rawGetI(inputsIdx, i)
            inputList.add(luaToNDArray(l, -1, inputShape))
            l.pop(1)

            l.rawGetI(labelsIdx, i)
            val labelValIdx = l.getTop()

            if (mode == "regression") {
                val vals = if (l.isTable(labelValIdx)) {
                    val tLen = l.rawLength(labelValIdx)
                    FloatArray(tLen) { j ->
                        l.rawGetI(labelValIdx, j + 1)
                        val v = l.toNumber(-1).toFloat()
                        l.pop(1)
                        v
                    }
                } else {
                    floatArrayOf(l.toNumber(labelValIdx).toFloat())
                }
                labelList.add(manager.create(vals, Shape(vals.size.toLong())))
            } else {
                if (outputSize > 1) {
                    labelList.add(manager.create(l.toNumber(labelValIdx).toLong()))
                } else {
                    labelList.add(manager.create(floatArrayOf(l.toNumber(labelValIdx).toFloat()), Shape(1)))
                }
            }
            l.pop(1) // pop label
        }
        l.pop(2) // pop inputs and labels tables

        val allInputs = NDArrays.stack(inputList, 0)
        var allLabels = NDArrays.stack(labelList, 0)

        val finalLabels = if (mode == "classification" && outputSize > 1) {
            if (allLabels.shape.dimension() > 1) allLabels = allLabels.squeeze(-1)
            allLabels.toType(DataType.INT64, false)
        } else {
            allLabels.toType(DataType.FLOAT32, false)
        }

        return ArrayDataset.Builder().setData(allInputs).optLabels(finalLabels).setSampling(batchSize, true).build()
    }

    private fun saveModel(l: Lua): Int {
        val modelId = l.toString(1) ?: return 0
        val path = l.toString(2) ?: return 0

        val model = models[modelId] ?: run {
            l.push(false)
            return 1
        }

        return try {
            model.save(Paths.get(path), modelId)
            l.push(true)
            1
        } catch (e: Exception) {
            l.push(false)
            1
        }
    }

    private fun loadModel(l: Lua): Int {
        val id = l.toString(1) ?: return 0
        val path = l.toString(2) ?: return 0
        val configIdx = 3

        return try {
            val model = Model.newInstance(id)

            if (l.isTable(configIdx)) {
                l.getField(configIdx, "input_size"); val inputSize = if(l.isNumber(-1)) l.toNumber(-1).toInt() else 10; l.pop(1)
                l.getField(configIdx, "output_size"); val outputSize = if(l.isNumber(-1)) l.toNumber(-1).toInt() else 1; l.pop(1)
                l.getField(configIdx, "mode"); val mode = if(l.isString(-1)) l.toString(-1) else "classification"; l.pop(1)

                modelModes[id] = mode!!
                inputShapes[id] = longArrayOf(1, inputSize.toLong())

                val block = SequentialBlock()
                l.getField(configIdx, "layers")
                if (l.isTable(-1)) {
                    val layersLen = l.rawLength(-1)
                    for (i in 1..layersLen) {
                        l.rawGetI(-1, i)
                        val layerSize = l.toNumber(-1).toLong()
                        l.pop(1)
                        block.add(Linear.builder().setUnits(layerSize).build())
                        block.add(Activation::relu)
                    }
                }
                l.pop(1)
                block.add(Linear.builder().setUnits(outputSize.toLong()).build())
                model.block = block
            }

            model.load(Paths.get(path), id)
            models[id] = model

            val predictor = model.newPredictor(ai.djl.translate.NoopTranslator())
            predictors[id] = predictor

            l.push(true)
            1
        } catch (e: Exception) {
            e.printStackTrace()
            l.push(false)
            1
        }
    }

    private fun predict(l: Lua): Int {
        val id = l.toString(1) ?: return 0
        if (!l.isTable(2)) {
            l.pushNil()
            return 1
        }

        var predictor = predictors[id]

        // Если предиктора нет, создаем из модели
        if (predictor == null) {
            val model = models[id]
            if (model != null) {
                predictor = model.newPredictor(ai.djl.translate.NoopTranslator())
                predictors[id] = predictor
            } else {
                println("Model/Predictor not found: $id")
                l.pushNil()
                return 1
            }
        }

        return try {
            val shape = inputShapes[id] ?: longArrayOf(1, 10)

            // Конвертируем таблицу (индекс 2) в NDArray
            val inputArray = luaToNDArray(l, 2, shape)

            val output = predictor!!.predict(NDList(inputArray))

            // Конвертируем результат (первый элемент NDList) обратно в таблицу Lua
            ndArrayToLua(l, output[0])

            inputArray.close()
            output.close()

            // Результат уже на вершине стека после ndArrayToLua
            1
        } catch (e: Exception) {
            println("Predict failed: ${e.message}")
            l.pushNil()
            1
        }
    }

    private fun close(l: Lua): Int {
        val id = l.toString(1) ?: return 0

        predictors.remove(id)?.close()
        models.remove(id)?.close()
        inputShapes.remove(id)

        l.push(true)
        return 1
    }

    private fun getModelInfo(l: Lua): Int {
        val id = l.toString(1) ?: return 0
        val model = models[id] ?: run {
            l.pushNil()
            return 1
        }

        l.newTable() // Создаем результирующую таблицу

        l.push(id)
        l.setField(-2, "id")

        inputShapes[id]?.let { shape ->
            if (shape.size > 1) {
                // Кладем размер входного слоя (второе число в шейпе)
                l.push(shape[1].toDouble())
                l.setField(-2, "input_shape")
            }
        }

        return 1
    }
}
