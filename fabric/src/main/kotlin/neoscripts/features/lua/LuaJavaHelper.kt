package com.nekiplay.neoscripts.features.lua

import net.fabricmc.loader.api.FabricLoader
import java.util.LinkedHashSet

object LuaJavaHelper {

    @JvmStatic
    fun getJavaClass(name: String): Class<*>? {
        val candidates = classCandidates(name)
        val loaders = candidateClassLoaders()
        for (loader in loaders) {
            for (candidate in candidates) {
                try {
                    return if (loader == null) Class.forName(candidate) else Class.forName(candidate, true, loader)
                } catch (ignored: ClassNotFoundException) {
                }
            }
        }
        return null
    }

    private fun classCandidates(name: String): List<String> {
        val names = LinkedHashSet<String>()
        names.add(name)
        try {
            val resolver = FabricLoader.getInstance().mappingResolver
            if (resolver != null) {
                val runtime = resolver.currentRuntimeNamespace
                val available = resolver.namespaces.toSet()
                for (source in listOf("official", "named", "intermediary")) {
                    if (source == runtime || source !in available) continue
                    try {
                        val mapped = resolver.mapClassName(source, name)
                        if (mapped.isNotBlank() && mapped != name) names.add(mapped)
                    } catch (ignored: Exception) {
                    }
                }
            }
        } catch (ignored: Exception) {
        }
        return names.toList()
    }

    private fun candidateClassLoaders(): List<ClassLoader?> {
        val loaders = LinkedHashSet<ClassLoader?>()
        loaders.add(Thread.currentThread().contextClassLoader)
        loaders.add(javaClass.classLoader)
        loaders.add(FabricLoader::class.java.classLoader) // загрузчик модов
        loaders.add(ClassLoader.getSystemClassLoader())
        return loaders.toList() + listOf<ClassLoader?>(null)
    }
}