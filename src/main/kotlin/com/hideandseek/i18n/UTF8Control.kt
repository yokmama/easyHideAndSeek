package com.hideandseek.i18n

import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.net.URLConnection
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * Custom ResourceBundle.Control that reads .properties files as UTF-8 instead of ISO-8859-1.
 *
 * This is essential for properly loading Japanese characters from properties files.
 * Java's default properties loader uses ISO-8859-1, which corrupts non-ASCII characters.
 *
 * Usage:
 * ```
 * val bundle = ResourceBundle.getBundle("messages", locale, UTF8Control())
 * ```
 *
 * Performance: This control is cached by ResourceBundle, so the UTF-8 reading overhead
 * only occurs once per bundle load.
 */
class UTF8Control : ResourceBundle.Control() {

    companion object {
        private const val PROPERTIES_FORMAT = "java.properties"
    }

    /**
     * Loads a ResourceBundle from a .properties file using UTF-8 encoding.
     *
     * @param baseName The base name of the resource bundle (e.g., "messages")
     * @param locale The locale for which to load the bundle
     * @param format The format (should be "java.properties")
     * @param loader The ClassLoader to use
     * @param reload Whether to reload the bundle
     * @return A PropertyResourceBundle loaded with UTF-8 encoding
     * @throws IOException If the resource cannot be read
     * @throws IllegalAccessException If the resource format is not supported
     */
    @Throws(IllegalAccessException::class, InstantiationException::class, IOException::class)
    override fun newBundle(
        baseName: String,
        locale: Locale,
        format: String,
        loader: ClassLoader,
        reload: Boolean
    ): ResourceBundle? {
        // Only handle .properties format
        if (format != PROPERTIES_FORMAT) {
            return null
        }

        // Build the resource bundle name (e.g., "messages_ja_JP.properties")
        val bundleName = toBundleName(baseName, locale)
        val resourceName = toResourceName(bundleName, "properties")

        // Get the resource URL
        val url: URL = loader.getResource(resourceName) ?: return null

        // Open connection with reload handling
        val connection: URLConnection = url.openConnection()
        if (reload) {
            connection.useCaches = false
        }

        // Read the properties file using UTF-8 encoding
        return connection.getInputStream().use { inputStream ->
            InputStreamReader(inputStream, StandardCharsets.UTF_8).use { reader ->
                PropertyResourceBundle(reader)
            }
        }
    }
}
