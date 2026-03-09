package com.nekiplay.hypixelcry.features.lua.objects.misc

import ai.djl.Device
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
import ai.djl.training.optimizer.Sgd
import ai.djl.training.tracker.Tracker
import ai.djl.translate.TranslateException
import net.minecraft.world.phys.shapes.Shapes
import org.luaj.vm2.*
import org.luaj.vm2.lib.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.Path
import net.minecraft.client.Minecraft;
import ai.djl.ndarray.types.DataType
import com.nekiplay.hypixelcry.HypixelCry

/**
 * Библиотека-обертка для доступа к DJL из LuaJ на Kotlin
 */
class DJLLuaTrainer : TwoArgFunction() {

    val models = ConcurrentHashMap<String, Model>()
    val predictors = ConcurrentHashMap<String, Predictor<NDList, NDList>>()
    private val manager: NDManager = NDManager.newBaseManager()
    val inputShapes = ConcurrentHashMap<String, LongArray>()

    override fun call(modname: LuaValue?, env: LuaValue?): LuaValue {
        val djl = LuaTable()

        djl["create_model"] = CreateModelFunction()
        djl["train"] = TrainFunction()
        djl["save_model"] = SaveModelFunction()
        djl["load_model"] = LoadModelFunction()
        djl["predict"] = PredictFunction()
        djl["close"] = CloseFunction()
        djl["get_model_info"] = GetModelInfoFunction()

        env?.set("djl", djl)
        return djl
    }

    // === Конвертация Lua -> NDArray ===
    private fun luaToNDArray(table: LuaValue, shape: LongArray): NDArray {
        val flatData = mutableListOf<Float>()
        flattenTable(table, flatData)
        val data = flatData.toFloatArray()
        return manager.create(data, Shape(*shape))
    }

    private fun flattenTable(table: LuaValue, data: MutableList<Float>) {
        if (!table.istable()) {
            data.add(table.todouble().toFloat())
            return
        }
        for (i in 1..table.length()) {
            flattenTable(table[i], data)
        }
    }

    // === Конвертация NDArray -> Lua ===
    private fun ndArrayToLua(array: NDArray): LuaValue {
        val data = array.toFloatArray()
        val t = LuaTable()
        data.forEachIndexed { index, value ->
            t[index + 1] = LuaValue.valueOf(value.toDouble())
        }
        return t
    }

    // === 1. Создание модели с гибкой архитектурой ===
    inner class CreateModelFunction : TwoArgFunction() {
        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            val id = arg1.checkstring().tojstring()
            val config = arg2.checktable()

            return try {
                val model = Model.newInstance(id)

                val inputSize = config["input_size"].optint(10)
                val outputSize = config["output_size"].optint(1)
                val layersConfig = config["layers"]

                inputShapes[id] = longArrayOf(1, inputSize.toLong())

                val block = SequentialBlock()

                // Скрытые слои
                if (layersConfig != null && layersConfig.istable()) {
                    for (i in 1..layersConfig.length()) {
                        val layerSize = layersConfig[i].toint()
                        block.add(Linear.builder().setUnits(layerSize.toLong()).build())
                        block.add(Activation::relu)
                    }
                }

                // Выходной слой
                block.add(Linear.builder().setUnits(outputSize.toLong()).build())

                model.block = block
                models[id] = model

                // Возвращаем информацию о модели
                LuaTable().apply {
                    set("id", LuaValue.valueOf(id))
                    set("input_size", LuaValue.valueOf(inputSize))
                    set("output_size", LuaValue.valueOf(outputSize))
                    set("layers", layersConfig ?: LuaValue.NIL)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                LuaValue.error("Failed to create model: ${e.message}")
            }
        }
    }

    companion object {
        private var isTrainingRunning = false
    }

    // === 2. Обучение модели ===
    inner class TrainFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            if (isTrainingRunning) {
                return LuaValue.valueOf("Training already in progress")
            }
            isTrainingRunning = true
            val modelId = args.arg1().checkstring().tojstring()
            val config = args.arg(2).opttable(null) ?: return LuaValue.error("Config required")
            val trainData = args.arg(3).opttable(null)
            val testData = args.arg(4).opttable(null)
            val callback = args.arg(5).optfunction(null)
    
            val model = models[modelId] ?: return LuaValue.error("Model not found: $modelId")
    
            return try {
                val epochs = config["epochs"].optint(10)
                val learningRate = config["lr"].optdouble(0.001)
                val batchSize = config["batch_size"].optint(32)
                val outputSize = config["output_size"].optint(1)
    
                // ИСПРАВЛЕНИЕ 1: Убираем лишнюю единицу из Shape, если она есть
                // Для обычных нейросетей форма должна быть просто [input_size]
                var shapeArray = inputShapes[modelId] ?: longArrayOf(10)
                if (shapeArray.size > 1 && shapeArray[0] == 1L) {
                    shapeArray = shapeArray.sliceArray(1 until shapeArray.size)
                }
                val trainingShape = Shape(*shapeArray)
    
                HypixelCry.LOGGER.info(HypixelCry.LOG_PREFIX + "Shape adjusted to: $trainingShape")
    
                val dataset = buildDataset(trainData, batchSize, shapeArray, outputSize, model.ndManager)
                val testDataset = testData?.let { buildDataset(it, batchSize, shapeArray, outputSize, model.ndManager) }
    
                val loss = if (outputSize == 1) {
                    Loss.sigmoidBinaryCrossEntropyLoss()
                } else {
                    Loss.softmaxCrossEntropyLoss()
                }
                
                val lrTracker = Tracker.fixed(learningRate.toFloat())
    
                val trainingConfig = DefaultTrainingConfig(loss)
                    .optOptimizer(Adam.builder().optLearningRateTracker(lrTracker).build())
                
                // ИСПРАВЛЕНИЕ 2: Accuracy добавляем ТОЛЬКО если выходов больше 1
                if (outputSize > 1) {
                    trainingConfig.addEvaluator(Accuracy())
                }
    
                // Callback для Lua (TrainingListeners)
                if (callback != null) {
                    trainingConfig.addTrainingListeners(object : TrainingListener {
                        override fun onEpoch(trainer: Trainer) {
                            val epochVal = LuaValue.valueOf(trainer.trainingResult.epoch)
                            val lossKey = trainer.loss.name
                            val lossVal = try { trainer.loss.getAccumulator(lossKey) } catch (e: Exception) { 0f }
                            Minecraft.getInstance().execute {
                                callback.call(epochVal, LuaValue.valueOf(lossVal.toDouble()))
                            }
                        }
                        override fun onTrainingBatch(trainer: Trainer?, batchData: TrainingListener.BatchData?) {}
                        override fun onValidationBatch(trainer: Trainer?, batchData: TrainingListener.BatchData?) {}
                        override fun onTrainingBegin(trainer: Trainer?) {}
                        override fun onTrainingEnd(trainer: Trainer?) {}
                    })
                }
    
                model.newTrainer(trainingConfig).use { trainer ->
                    // Инициализируем модель правильной формой
                    trainer.initialize(trainingShape)
                    EasyTrain.fit(trainer, epochs, dataset, testDataset)
                }
    
                LuaValue.TRUE
            } catch (e: Exception) {
                e.printStackTrace()
                LuaValue.error("Training failed: ${e.message}")
            } finally {
                isTrainingRunning = false
            }
        }
    
        private fun buildDataset(data: LuaValue?, batchSize: Int, inputShape: LongArray, outputSize: Int, manager: NDManager): Dataset {
            require(data != null) { "Dataset is required" }
        
            val inputs = data["inputs"] ?: throw IllegalArgumentException("Dataset must contain 'inputs'")
            val labels = data["labels"] ?: throw IllegalArgumentException("Dataset must contain 'labels'")
        
            val inputList = NDList()
            val labelList = NDList()
        
            for (i in 1..inputs.length()) {
                // Загружаем входные данные (например, [4])
                inputList.add(luaToNDArray(inputs[i], inputShape))
        
                val labelValue = labels[i]
                if (outputSize > 1) {
                    // Классификация: метка — это одно число (Long)
                    val classIdx = if (labelValue.istable()) labelValue[1].tolong() else labelValue.tolong()
                    labelList.add(manager.create(classIdx))
                } else {
                    // Бинарный выход: метка — это массив [1] (Float)
                    val value = if (labelValue.istable()) labelValue[1].tofloat() else labelValue.tofloat()
                    labelList.add(manager.create(floatArrayOf(value), Shape(1)))
                }
            }
        
            val allInputs = NDArrays.stack(inputList, 0)
            var allLabels = NDArrays.stack(labelList, 0)
        
            // ИСПРАВЛЕНИЕ 3: Принудительно корректируем типы и формы для PyTorch
            val finalLabels = if (outputSize > 1) {
                // Softmax ожидает одномерный тензор меток (Batch) типа Long
                if (allLabels.shape.dimension() > 1) allLabels = allLabels.squeeze(-1)
                allLabels.toType(DataType.INT64, false)
            } else {
                // Sigmoid ожидает тензор (Batch, 1) типа Float
                allLabels.toType(DataType.FLOAT32, false)
            }
        
            return ArrayDataset.Builder()
                .setData(allInputs)
                .optLabels(finalLabels)
                .setSampling(batchSize, true)
                .build()
        }
    }

    // === 3. Сохранение модели ===
    inner class SaveModelFunction : TwoArgFunction() {
        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            val modelId = arg1.checkstring().tojstring()
            val path = arg2.checkstring().tojstring()

            val model = models[modelId] ?: return LuaValue.error("Model not found: $modelId")

            return try {
                model.save(Path(path), modelId)
                LuaValue.TRUE
            } catch (e: Exception) {
                LuaValue.error("Save failed: ${e.message}")
            }
        }
    }

    // === 4. Загрузка модели ===
    inner class LoadModelFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val id = args.arg1().checkstring().tojstring()
            val path = args.arg(2).checkstring().tojstring()
            val config = args.arg(3).opttable(null) // ТАБЛИЦА КОНФИГУРАЦИИ

            return try {
                val model = Model.newInstance(id)

                // ЕСЛИ передана конфигурация, строим структуру слоев ПЕРЕД загрузкой
                if (config != null) {
                    val inputSize = config["input_size"].optint(10)
                    val outputSize = config["output_size"].optint(1)
                    val layersConfig = config["layers"]

                    inputShapes[id] = longArrayOf(1, inputSize.toLong())

                    val block = SequentialBlock()
                    if (layersConfig != null && layersConfig.istable()) {
                        for (i in 1..layersConfig.length()) {
                            val layerSize = layersConfig[i].toint()
                            block.add(Linear.builder().setUnits(layerSize.toLong()).build())
                            block.add(Activation::relu)
                        }
                    }
                    block.add(Linear.builder().setUnits(outputSize.toLong()).build())
                    model.block = block
                }

                // Теперь загружаем веса. DJL найдет id-0000.params и вставит их в блоки.
                model.load(Path(path), id)
                models[id] = model

                val predictor = model.newPredictor(ai.djl.translate.NoopTranslator())
                predictors[id] = predictor

                LuaValue.TRUE
            } catch (e: Exception) {
                e.printStackTrace()
                LuaValue.error("Load failed: ${e.message}")
            }
        }
    }

    // === 5. Предикт ===
    inner class PredictFunction : TwoArgFunction() {
        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            val id = arg1.checkstring().tojstring()
            val inputTable = arg2.checktable()

            var predictor = predictors[id]

            // Если предиктора нет, создаем из модели
            if (predictor == null) {
                val model = models[id]
                if (model != null) {
                    predictor = model.newPredictor(ai.djl.translate.NoopTranslator())
                    predictors[id] = predictor
                } else {
                    return LuaValue.error("Model/Predictor not found: $id")
                }
            }

            return try {
                val shape = inputShapes[id] ?: longArrayOf(1, 10)
                val inputArray = luaToNDArray(inputTable, shape)
                val output = predictor.predict(NDList(inputArray))
                val result = ndArrayToLua(output[0])

                inputArray.close()
                output.close()
                result
            } catch (e: TranslateException) {
                LuaValue.error("Predict failed: ${e.message}")
            }
        }
    }

    // === 6. Очистка ===
    inner class CloseFunction : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val id = arg.checkstring().tojstring()

            predictors.remove(id)?.close()
            models.remove(id)?.close()
            inputShapes.remove(id)

            return LuaValue.TRUE
        }
    }

    // === 7. Информация о модели ===
    inner class GetModelInfoFunction : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val id = arg.checkstring().tojstring()
            val model = models[id] ?: return LuaValue.NIL
            return LuaTable().apply {
                set("id", LuaValue.valueOf(id))
                inputShapes[id]?.let { shape ->
                    set("input_shape", LuaValue.valueOf(shape[1].toInt()))
                }
                set("input_size", model.block.inputShapes.size)
                set("output_size", model.block.outputDataTypes.size)
            }
        }
    }
}
