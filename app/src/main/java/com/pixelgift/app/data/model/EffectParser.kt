package com.pixelgift.app.data.model

/**
 * Pure Kotlin parser for protocol/effects.json.
 */
object EffectParser {

    fun fromJson(jsonStr: String): List<EffectItem> {
        val root = AppDataJson.JsonParser(jsonStr).parse() as? AppDataJson.JsonArray
            ?: throw IllegalArgumentException("Expected JSON array of effects")

        return root.list.mapNotNull { item ->
            val obj = item as? AppDataJson.JsonObject ?: return@mapNotNull null
            val id = (obj.map["id"] as? AppDataJson.JsonNumber)?.value?.toInt() ?: return@mapNotNull null
            val name = (obj.map["name"] as? AppDataJson.JsonString)?.value ?: return@mapNotNull null
            val category = (obj.map["category"] as? AppDataJson.JsonString)?.value ?: return@mapNotNull null

            val supportsObj = obj.map["supports"] as? AppDataJson.JsonObject
            val dirSupp = (supportsObj?.map?.get("dir") as? AppDataJson.JsonBoolean)?.value ?: false
            val mirrorSupp = (supportsObj?.map?.get("mirror") as? AppDataJson.JsonBoolean)?.value ?: false
            val colorsSupp = (supportsObj?.map?.get("colors") as? AppDataJson.JsonBoolean)?.value ?: false
            val paletteSupp = (supportsObj?.map?.get("palette") as? AppDataJson.JsonBoolean)?.value ?: false

            val defaultsObj = obj.map["defaults"] as? AppDataJson.JsonObject
            val speedDef = (defaultsObj?.map?.get("speed") as? AppDataJson.JsonNumber)?.value?.toInt() ?: 50
            val dirDef = (defaultsObj?.map?.get("dir") as? AppDataJson.JsonNumber)?.value?.toInt() ?: 0
            val mirrorDef = (defaultsObj?.map?.get("mirror") as? AppDataJson.JsonNumber)?.value?.toInt() ?: 0
            val colorModeDef = (defaultsObj?.map?.get("colorMode") as? AppDataJson.JsonNumber)?.value?.toInt() ?: 0

            EffectItem(
                id = id,
                name = name,
                category = category,
                supports = EffectSupports(
                    dir = dirSupp,
                    mirror = mirrorSupp,
                    colors = colorsSupp,
                    palette = paletteSupp
                ),
                defaults = EffectDefaults(
                    speed = speedDef,
                    dir = dirDef,
                    mirror = mirrorDef,
                    colorMode = colorModeDef
                )
            )
        }
    }
}
