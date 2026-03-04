package com.lumos.lumosspring.util
import java.text.Normalizer

object CityUtils {

    private val lowercaseWords = setOf("de", "da", "do", "dos", "das")

    /**
     * Remove acentos
     */
    fun removeAccents(text: String): String {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }

    /**
     * Limpa texto bruto vindo do banco
     */
    fun cleanRawCity(raw: String): String {
        return raw
            .trim()
            .replace(Regex("-.*$"), "") // remove sufixos (-MANUTENÇÃO, etc)
            .replace(Regex("(?i)prefeitura\\s*(municipal)?\\s*(de)?"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Chave normalizada (para agrupar)
     */
    fun normalizeCityKey(raw: String): String {
        val cleaned = cleanRawCity(raw)
        val noAccents = removeAccents(cleaned)

        return noAccents.uppercase()
    }

    /**
     * Nome bonito para exibir no relatório
     */
    @JvmStatic
    fun displayCityName(raw: String): String {

        val cleaned = cleanRawCity(raw)

        return cleaned
            .lowercase()
            .split(" ")
            .joinToString(" ") { word ->
                if (word in lowercaseWords) word
                else word.replaceFirstChar { it.uppercase() }
            }
    }

}