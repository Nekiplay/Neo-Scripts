package com.nekiplay.neoscripts.features.lua

import net.fabricmc.loader.api.FabricLoader
import org.luaj.vm2.lib.jse.LuajavaLib
import java.util.LinkedHashSet

class MinecraftLuajavaLib : LuajavaLib() {

    override fun classForName(name: String): Class<*> {
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
        throw ClassNotFoundException(name)
    }

    private fun classCandidates(name: String): List<String> {
        val names = LinkedHashSet<String>()
        names.add(name)
        try {
            val resolver = FabricLoader.getInstance().mappingResolver
            if (resolver != null) {
                val runtime = resolver.currentRuntimeNamespace
                val available = resolver.getNamespaces().toSet()
                for (source in MAPPING_NAMESPACES) {
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
        loaders.add(LuajavaLib::class.java.classLoader)
        loaders.add(ClassLoader.getSystemClassLoader())
        return loaders.toList() + listOf<ClassLoader?>(null)
    }

    companion object {
        private val MAPPING_NAMESPACES = listOf("official", "named", "intermediary")
    }
}