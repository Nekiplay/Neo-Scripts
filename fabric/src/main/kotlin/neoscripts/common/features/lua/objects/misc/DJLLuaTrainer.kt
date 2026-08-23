package com.nekiplay.neoscripts.common.features.lua.objects.misc

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
import ai.djl.training.tracker.Tracker
import org.luaj.vm2.*
import org.luaj.vm2.lib.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.Path
import ai.djl.ndarray.types.DataType
import ai.djl.nn.convolutional.Conv2d
import ai.djl.nn.norm.BatchNorm
import ai.djl.nn.norm.Dropout
import ai.djl.nn.pooling.Pool
import ai.djl.translate.NoopTranslator
import com.nekiplay.neoscripts.common.features.lua.LuaManager
import java.nio.file.Paths

/**
 * Библиотека-обертка для доступа к DJL из LuaJ на Kotlin
 */
class DJLLuaTrainer(val lmanager: LuaManager) : LuaValue() {
    private var currentDevice: Device = Device.cpu()
    init {
        val djlDir = lmanager.configDir?.resolve("neoscripts/djl_cache/").toString() + "/";
        System.setProperty("DJL_CACHE_DIR", djlDir)
        System.setProperty("ENGINE_CACHE_DIR", djlDir)
        System.setProperty("DJL_OFFLINE", "true")
        System.setProperty("PYTORCH_FLAVOR", "cpu");
        System.setProperty("ai.djl.device", "cpu");
    }

    val models = ConcurrentHashMap<String, Model>()
    val predictors = ConcurrentHashMap<String, Predictor<NDList, NDList>>()
    private var manager: NDManager = NDManager.newBaseManager()
    val inputShapes = ConcurrentHashMap<String, LongArray>()
    val modelModes = ConcurrentHashMap<String, String>()

    override fun typename(): String = "djl"
    override fun tojstring(): String = "DjlObject"
    override fun isnil(): Boolean = false
    override fun type(): Int {
        return TUSERDATA
    }

    override fun call(): LuaValue {
        return this
    }

    override fun get(key: LuaValue): LuaValue {
        return when (key.tojstring()) {
            "create_model" -> CreateModelFunction()
            "train" -> TrainFunction()
            "save_model" -> SaveModelFunction()
            "load_model" -> LoadModelFunction()
            "predict" -> PredictFunction()
            "close" -> CloseFunction()
            "get_model_info" -> GetModelInfoFunction()
            else -> super.get(key)
        }
    }

    private fun luaTableToNDArray(table: LuaValue, shape: LongArray, manager: NDManager): NDArray {
        var totalSize = 1
        for (s in shape) totalSize *= s.toInt()

        val data = FloatArray(totalSize)
        fillFlatArray(table, data, intArrayOf(0))

        return manager.create(data, Shape(*shape))
    }

    private fun fillFlatArray(table: LuaValue, data: FloatArray, offset: IntArray) {
        if (!table.istable()) {
            data[offset[0]++] = table.tofloat()
            return
        }
        val len = table.length()
        for (i in 1..len) {
            val v = table.get(i)
            if (v.istable()) {
                fillFlatArray(v, data, offset)
            } else {
                data[offset[0]++] = v.tofloat()
            }
        }
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

    private fun ndArrayToLua(array: NDArray): LuaValue {
        val data = array.toFloatArray() // Один переход из натива в JVM
        val t = LuaTable(data.size, 0)
        for (i in data.indices) {
            t.set(i + 1, valueOf(data[i].toDouble()))
        }
        return t
    }

    private fun buildBlock(config: LuaValue): Block {
        val type = config["type"].optjstring("sequential").lowercase()

        return when (type) {
            "sequential" -> {
                val block = SequentialBlock()
                val layers = config["layers"]
                if (layers.istable()) {
                    for (i in 1..layers.length()) {
                        val entry = layers[i]
                        if (entry.istable()) {
                            val entryType = entry["type"].optjstring("linear").lowercase()
                            if (entryType == "sequential" || entryType == "parallel") {
                                block.add(buildBlock(entry))
                            } else {
                                parseLayer(block, entryType, entry)
                            }
                        } else {
                            block.add(Linear.builder().setUnits(entry.tolong()).build())
                            block.add(Activation::relu)
                        }
                    }
                }
                block
            }

            "parallel" -> {
                val mode = config["mode"].optjstring("concat").lowercase()
                val layers = config["layers"]
                val parallelBlock = ParallelBlock { lists ->
                    if (lists.isEmpty()) return@ParallelBlock NDList()
                    val arrays = lists.map { it.head() }
                    val result = when (mode) {
                        "sum" -> {
                            var res = arrays[0]
                            for (i in 1 until arrays.size) res = res.add(arrays[i])
                            res
                        }
                        else -> NDArrays.concat(NDList(arrays))
                    }
                    NDList(result)
                }

                if (layers.istable()) {
                    for (i in 1..layers.length()) {
                        parallelBlock.add(buildBlock(layers[i]))
                    }
                }
                parallelBlock
            }

            else -> SequentialBlock()
        }
    }

    inner class CreateModelFunction : TwoArgFunction() {
        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            val id = arg1.checkstring().tojstring()
            val config = arg2.checktable()

            return try {
                val model = Model.newInstance(id, currentDevice)
                val inputSize = config["input_size"].optint(10)
                val mode = config["mode"].optjstring("classification")

                modelModes[id] = mode
                inputShapes[id] = longArrayOf(1, inputSize.toLong())

                // 1. Строим основной блок рекурсивно
                // buildBlock сам обработает все слои, указанные в config["layers"]
                val mainBlock = buildBlock(config)

                // 2. Обработка финального слоя
                val finalLayerConfig = config["final_layer"]
                if (finalLayerConfig.istable()) {
                    // Если указана таблица: final_layer = {type = "linear", units = 5, activation = "sigmoid"}
                    val fType = finalLayerConfig["type"].optjstring("linear").lowercase()
                    parseLayer(mainBlock, fType, finalLayerConfig)
                } else if (finalLayerConfig.optjstring("") != "none") {
                    // По умолчанию (если не указано "none"), создаем обычный Linear
                    val outputSize = config["output_size"].optint(1)
                    addToBlock(mainBlock, Linear.builder().setUnits(outputSize.toLong()).build())
                }

                model.block = mainBlock
                models[id] = model

                LuaTable().apply {
                    set("id", valueOf(id))
                    set("mode", valueOf(mode))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                error("Failed to create model: ${e.message}")
            }
        }
    }

    private fun addToBlock(parent: Block, child: Block) {
        when (parent) {
            is SequentialBlock -> parent.add(child)
            is ParallelBlock -> parent.add(child)
            else -> throw IllegalArgumentException("Block of type ${parent.javaClass.simpleName} does not support adding layers")
        }
    }

    private fun parseLayer(block: Block, type: String, params: LuaValue) {
        when (type) {
            "linear", "dense" -> {
                val units = params["units"].checklong()
                addToBlock(block, Linear.builder().setUnits(units).build())
                addActivation(block, params["activation"].optjstring("relu"))
            }

            "conv2d" -> {
                val filters = params["filters"].optint(params["channels"].optint(32))
                val kernel = parseShape(params["kernel"], 3)
                val stride = parseShape(params["stride"], 1)
                val padding = parseShape(params["padding"], 0)

                addToBlock(block, Conv2d.builder()
                    .setFilters(filters)
                    .setKernelShape(kernel)
                    .optStride(stride)
                    .optPadding(padding)
                    .build())
                addActivation(block, params["activation"].optjstring("relu"))
            }

            "maxpool2d" -> {
                val kernel = parseShape(params["kernel"], 2)
                val stride = parseShape(params["stride"], 2)
                addToBlock(block, Pool.maxPool2dBlock(kernel, stride))
            }

            "avgpool2d" -> {
                val kernel = parseShape(params["kernel"], 2)
                val stride = parseShape(params["stride"], 2)
                addToBlock(block, Pool.avgPool2dBlock(kernel, stride))
            }

            "dropout" -> {
                val rate = params["rate"].optdouble(0.5).toFloat()
                addToBlock(block, Dropout.builder().optRate(rate).build())
            }

            "batchnorm" -> {
                addToBlock(block, BatchNorm.builder().build())
            }

            "relu" -> addToBlock(block, Activation.reluBlock())
            "sigmoid" -> addToBlock(block, Activation.sigmoidBlock())
            "tanh" -> addToBlock(block, Activation.tanhBlock())
            "leaky_relu" -> {
                val alpha = params["alpha"].optdouble(0.01).toFloat()
                addToBlock(block, Activation.leakyReluBlock(alpha))
            }

            else -> error("Unsupported layer type: $type")
        }
    }

    private fun addActivation(block: Block, name: String) {
        val activationBlock: Block? = when (name.lowercase()) {
            "relu" -> Activation.reluBlock()
            "sigmoid" -> Activation.sigmoidBlock()
            "tanh" -> Activation.tanhBlock()
            "leaky_relu" -> Activation.leakyReluBlock(0.01f)
            else -> null
        }
        activationBlock?.let { addToBlock(block, it) }
    }

    private fun parseShape(value: LuaValue, defaultValue: Long): Shape {
        return when {
            value.isint() -> Shape(value.tolong(), value.tolong())
            value.istable() -> {
                if (value.length() >= 2) {
                    Shape(value[1].tolong(), value[2].tolong())
                } else {
                    Shape(value[1].tolong(), value[1].tolong())
                }
            }

            else -> Shape(defaultValue, defaultValue)
        }
    }

    inner class TrainFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val modelId = args.arg1().checkstring().tojstring()
            val config = args.arg(2).checktable()
            val trainData = args.arg(3).checktable()
            val testData = args.arg(4).opttable(null)
            val callback = args.arg(5).optfunction(null)

            val model = models[modelId] ?: return error("Model not found: $modelId")
            val mode = modelModes[modelId] ?: "classification"

            // Используем sub-manager для всего процесса тренировки
            model.ndManager.newSubManager().use { trainSubManager ->
                val epochs = config["epochs"].optint(10)
                val lr = config["lr"].optdouble(0.001).toFloat()
                val batchSize = config["batch_size"].optint(32)
                val outputSize = config["output_size"].optint(1)

                val inputShapeArr = inputShapes[modelId] ?: longArrayOf(1, 10)
                val trainingShape = Shape(*(if (inputShapeArr[0] == 1L) inputShapeArr.drop(1).toLongArray() else inputShapeArr))

                val dataset = buildDataset(trainData, batchSize, trainingShape.getShape(), outputSize, mode, trainSubManager)
                val testDataset = testData?.let { buildDataset(it, batchSize, trainingShape.getShape(), outputSize, mode, trainSubManager) }

                val loss = if (mode == "regression") Loss.l2Loss()
                else if (outputSize == 1) Loss.sigmoidBinaryCrossEntropyLoss()
                else Loss.softmaxCrossEntropyLoss()

                val trainingConfig = DefaultTrainingConfig(loss)
                    .optOptimizer(Adam.builder().optLearningRateTracker(Tracker.fixed(lr)).build())
                    .optDevices(arrayOf(currentDevice))
                    .addEvaluator(if (mode == "regression") loss else Accuracy())

                if (callback != null) {
                    trainingConfig.addTrainingListeners(object : TrainingListener {
                        var epoch = 1
                        override fun onEpoch(trainer: Trainer) {
                            callback.call(valueOf(epoch++))
                        }
                        override fun onTrainingBatch(trainer: Trainer?, data: TrainingListener.BatchData?) {}
                        override fun onValidationBatch(trainer: Trainer?, data: TrainingListener.BatchData?) {}
                        override fun onTrainingBegin(trainer: Trainer?) {}
                        override fun onTrainingEnd(trainer: Trainer?) {}
                    })
                }

                model.newTrainer(trainingConfig).use { trainer ->
                    trainer.initialize(trainingShape)
                    EasyTrain.fit(trainer, epochs, dataset, testDataset)
                }
            }
            return TRUE
        }

        private fun buildDataset(data: LuaValue, batchSize: Int, inShape: LongArray, outSize: Int, mode: String, m: NDManager): Dataset {
            val inputs = data["inputs"]
            val labels = data["labels"]
            val n = inputs.length()

            val allInputs = luaTableToNDArray(inputs, longArrayOf(n.toLong(), *inShape), m)

            val labelCols = if (labels[1].istable()) labels[1].length().toLong() else 1L
            var allLabels = luaTableToNDArray(labels, longArrayOf(n.toLong(), labelCols), m)

            if (mode == "classification" && outSize > 1) {
                allLabels = allLabels.squeeze(-1).toType(DataType.INT64, false)
            }

            return ArrayDataset.Builder()
                .setData(allInputs)
                .optLabels(allLabels)
                .setSampling(batchSize, true)
                .build()
        }
    }

    inner class SaveModelFunction : TwoArgFunction() {
        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            val modelId = arg1.checkstring().tojstring()
            val path = arg2.checkstring().tojstring()

            val model = models[modelId] ?: return error("Model not found: $modelId")

            return try {
                model.save(Path(path), modelId)
                TRUE
            } catch (e: Exception) {
                error("Save failed: ${e.message}")
            }
        }
    }

    inner class LoadModelFunction : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            val id = args.arg1().checkstring().tojstring()
            val path = args.arg(2).checkstring().tojstring()
            val config = args.arg(3).opttable(null)

            return try {
                val model = Model.newInstance(id)

                // Если передана конфигурация, воссоздаем структуру блоков
                if (config != null) {
                    val inputSize = config["input_size"].optint(10)
                    val mode = config["mode"].optjstring("classification")

                    modelModes[id] = mode
                    inputShapes[id] = longArrayOf(1, inputSize.toLong())

                    // 1. Рекурсивно строим блоки (включая все вложенные слои из config["layers"])
                    val mainBlock = buildBlock(config)

                    // 2. Обработка финального слоя (логика должна быть такой же, как при создании)
                    val finalLayerConfig = config["final_layer"]
                    if (finalLayerConfig.istable()) {
                        val fType = finalLayerConfig["type"].optjstring("linear").lowercase()
                        parseLayer(mainBlock, fType, finalLayerConfig)
                    } else if (finalLayerConfig.optjstring("") != "none") {
                        val outputSize = config["output_size"].optint(1)
                        addToBlock(mainBlock, Linear.builder().setUnits(outputSize.toLong()).build())
                    }

                    model.block = mainBlock
                }

                // Загружаем веса. Путь должен указывать на папку или файл .params
                model.load(Paths.get(path), id)
                models[id] = model

                // Создаем предиктор для инференса
                val predictor = model.newPredictor(NoopTranslator(), currentDevice)
                predictors[id] = predictor

                TRUE
            } catch (e: Exception) {
                e.printStackTrace()
                error("Load failed: ${e.message}")
            }
        }
    }

    inner class PredictFunction : TwoArgFunction() {
        override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
            val id = arg1.checkstring().tojstring()
            val inputTable = arg2.checktable()

            val predictor = predictors[id] ?: models[id]?.let {
                val p = it.newPredictor(NoopTranslator(), currentDevice)
                predictors[id] = p
                p
            } ?: return error("Model not found: $id")

            // Важно использовать sub-manager для временных тензоров,
            // чтобы GC мог быстро очистить нативную память
            manager.newSubManager().use { sub ->
                val shape = inputShapes[id] ?: longArrayOf(1, 10)
                val inputArray = luaTableToNDArray(inputTable, shape, sub)

                val output = predictor.predict(NDList(inputArray))
                val result = ndArrayToLua(output[0])

                output.close() // Освобождаем нативный список сразу
                return result
            }
        }
    }

    inner class CloseFunction : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val id = arg.checkstring().tojstring()
            predictors.remove(id)?.close()
            models.remove(id)?.close()
            inputShapes.remove(id)

            // Если моделей больше нет, чистим базовый менеджер
            if (models.isEmpty()) {
                manager.close()
                manager = NDManager.newBaseManager(currentDevice)
            }
            return TRUE
        }
    }

    inner class GetModelInfoFunction : OneArgFunction() {
        override fun call(arg: LuaValue): LuaValue {
            val id = arg.checkstring().tojstring()
            val model = models[id] ?: return NIL
            return LuaTable().apply {
                set("id", valueOf(id))
                inputShapes[id]?.let { shape ->
                    set("input_shape", valueOf(shape[1].toInt()))
                }
            }
        }
    }
}
