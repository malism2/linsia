package com.malism.linsia.classicread.data.epub

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EpubParserTest {
    private var tempFile: File? = null

    @AfterTest
    fun cleanup() {
        tempFile?.delete()
    }

    @Test
    fun parseSampleEpub() {
        val path = createSampleEpub()
        val chapters = EpubParser.parse(path)
        assertEquals(2, chapters.size, chapters.joinToString { it.title })
        assertEquals("Chapter One", chapters[0].title)
        assertTrue(chapters[0].body.contains("First paragraph."))
        assertTrue(chapters[1].body.contains("Second paragraph."))
    }

    private fun createSampleEpub(): String {
        val file = File.createTempFile("sample", ".epub")
        tempFile = file
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            zip.putNextEntry(ZipEntry("mimetype"))
            zip.write("application/epub+zip".toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("META-INF/container.xml"))
            zip.write(
                """
                <?xml version="1.0"?>
                <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                  <rootfiles>
                    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
                  </rootfiles>
                </container>
                """.trimIndent().toByteArray(),
            )
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("OEBPS/content.opf"))
            zip.write(
                """
                <?xml version="1.0" encoding="utf-8"?>
                <package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="uid">
                  <metadata>
                    <dc:title xmlns:dc="http://purl.org/dc/elements/1.1/">Sample</dc:title>
                  </metadata>
                  <manifest>
                    <item id="c1" href="chapter1.xhtml" media-type="application/xhtml+xml"/>
                    <item id="c2" href="chapter2.xhtml" media-type="application/xhtml+xml"/>
                    <item id="colophon" href="colophon.xhtml" media-type="application/xhtml+xml"/>
                  </manifest>
                  <spine>
                    <itemref idref="c1"/>
                    <itemref idref="c2"/>
                    <itemref idref="colophon"/>
                  </spine>
                </package>
                """.trimIndent().toByteArray(),
            )
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("OEBPS/chapter1.xhtml"))
            zip.write(
                """
                <html xmlns="http://www.w3.org/1999/xhtml">
                  <body>
                    <h2>Chapter One</h2>
                    <p>First paragraph.</p>
                  </body>
                </html>
                """.trimIndent().toByteArray(),
            )
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("OEBPS/chapter2.xhtml"))
            zip.write(
                """
                <html xmlns="http://www.w3.org/1999/xhtml">
                  <body>
                    <h3>Chapter Two</h3>
                    <p>Second paragraph.</p>
                  </body>
                </html>
                """.trimIndent().toByteArray(),
            )
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("OEBPS/colophon.xhtml"))
            zip.write("<html><body><p>Colophon</p></body></html>".toByteArray())
            zip.closeEntry()
        }
        return file.absolutePath
    }
}
