package com.nekiplay.neoscripts.client.events.main

import java.awt.Color
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.get

object EventBus {

    val callbacks = HashMap<Class<Event>, MutableList<CallbackMethod>>()

    fun init(classes : Set<Class<out Any>>) {
        for (clazz in classes) {
            if (
                clazz.isInterface || Modifier.isAbstract(clazz.modifiers) || clazz.isAnnotation
            ) continue
            val classInstance = clazz.kotlin.objectInstance
            if (classInstance != null) register(classInstance)
        }
    }

    fun register(instance : Any) {
        for (method in instance.javaClass.methods) {
            val annotation = method.getAnnotation<Callback>(Callback::class.java) ?: continue
            val priority = annotation.priority
            if (method.parameterCount != 1) throw RuntimeException("Parameters is empty. In: ${instance.javaClass.name}, method: ${method.name}")
            val eventType = method.parameterTypes.first()
            if (!Event::class.java.isAssignableFrom(eventType)) throw RuntimeException("Cannot find event in parameters. In: ${instance.javaClass.name}, method: ${method.name}, parameters: ${method.parameterTypes.map { it.simpleName }}")
            callbacks.getOrPut(eventType as Class<Event>) { ArrayList() } .add(CallbackMethod(method, instance, priority, false))
            callbacks[eventType]?.sortWith(Comparator { o1, o2 -> return@Comparator o2.priority - o1.priority })
        }
    }

    fun send(event : Event) {
        val lastMethod = AtomicReference<CallbackMethod?>()

        try {
            callbacks[event.javaClass]?.forEach {
                lastMethod.set(it)
                it.method.invoke(it.instance, event)
            }
        } catch (e : Exception) {
            e.printStackTrace()
        }
    }

    fun setIgnore(value : Boolean, type : Class<out Event>, vararg instances : Any) {
        val list = callbacks[type] ?: return

        for (callback in list) {
            if (instances.isEmpty() || instances.contains(callback.instance)) callback.ignored = value
        }
    }

    fun sendCancellable(event : CancellableEvent) : Boolean {
        send(event)
        return event.cancelled
    }

    data class CallbackMethod(val method : Method, val instance : Any, val priority : Short, var ignored : Boolean)

}