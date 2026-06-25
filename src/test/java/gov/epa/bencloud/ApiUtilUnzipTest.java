package gov.epa.bencloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import gov.epa.bencloud.api.util.ApiUtil;

/**
 * Regression tests for the Zip-Slip vulnerability in {@link ApiUtil#unzip}.
 * Each escape-attempt test crafts an in-memory zip whose entries try to escape
 * the destination directory and confirms (a) extraction is aborted and
 * (b) nothing was written outside the destination.
 */
class ApiUtilUnzipTest {

	@Test
	void happyPath_extractsFileAndSubdirectory(@TempDir Path tempDir) throws IOException {
		Path zip = tempDir.resolve("good.zip");
		Path dest = tempDir.resolve("dest");
		writeZip(zip, zos -> {
			addFile(zos, "top.txt", "hello");
			addFile(zos, "sub/inner.txt", "world");
		});

		ApiUtil.unzip(zip.toString(), dest.toString());

		assertTrue(Files.exists(dest.resolve("top.txt")));
		assertTrue(Files.exists(dest.resolve("sub/inner.txt")));
		assertEquals("hello", Files.readString(dest.resolve("top.txt")));
		assertEquals("world", Files.readString(dest.resolve("sub/inner.txt")));
	}

	@Test
	void parentDirEscape_throwsAndWritesNothingOutside(@TempDir Path tempDir) throws IOException {
		Path zip = tempDir.resolve("evil.zip");
		Path dest = tempDir.resolve("dest");
		Path sentinel = tempDir.resolve("evil.txt");
		writeZip(zip, zos -> addFile(zos, "../evil.txt", "pwned"));

		assertThrows(IOException.class, () -> ApiUtil.unzip(zip.toString(), dest.toString()));
		assertFalse(Files.exists(sentinel), "Escaping file must not be written outside destination");
	}

	@Test
	void absolutePathEscape_throwsAndWritesNothingOutside(@TempDir Path tempDir) throws IOException {
		Path zip = tempDir.resolve("evil.zip");
		Path dest = tempDir.resolve("dest");
		// Build an absolute path that lives outside destination but inside the JUnit temp tree
		// (so the test stays sandboxed and won't pollute a real /tmp/evil.txt).
		Path sentinel = tempDir.resolve("absolute-evil.txt");
		String absoluteEntryName = sentinel.toAbsolutePath().toString();
		writeZip(zip, zos -> addFile(zos, absoluteEntryName, "pwned"));

		assertThrows(IOException.class, () -> ApiUtil.unzip(zip.toString(), dest.toString()));
		assertFalse(Files.exists(sentinel), "Absolute-path entry must not be written outside destination");
	}

	@Test
	void nestedTraversal_throwsAndWritesNothingOutside(@TempDir Path tempDir) throws IOException {
		Path zip = tempDir.resolve("evil.zip");
		Path dest = tempDir.resolve("dest");
		Path sentinel = tempDir.resolve("evil.txt");
		writeZip(zip, zos -> addFile(zos, "inner/../../evil.txt", "pwned"));

		assertThrows(IOException.class, () -> ApiUtil.unzip(zip.toString(), dest.toString()));
		assertFalse(Files.exists(sentinel), "Nested-traversal entry must not be written outside destination");
	}

	@Test
	void emptyZip_succeedsWithEmptyDestination(@TempDir Path tempDir) throws IOException {
		Path zip = tempDir.resolve("empty.zip");
		Path dest = tempDir.resolve("dest");
		writeZip(zip, zos -> {});

		ApiUtil.unzip(zip.toString(), dest.toString());

		assertTrue(Files.isDirectory(dest));
		try (var stream = Files.list(dest)) {
			List<Path> contents = stream.toList();
			assertTrue(contents.isEmpty(), "Empty zip should leave destination empty, found: " + contents);
		}
	}

	// --- helpers ---

	private static void writeZip(Path zipPath, Consumer<ZipOutputStream> populator) throws IOException {
		try (var fos = Files.newOutputStream(zipPath); var zos = new ZipOutputStream(fos)) {
			populator.accept(zos);
		}
	}

	private static void addFile(ZipOutputStream zos, String entryName, String content) {
		try {
			zos.putNextEntry(new ZipEntry(entryName));
			zos.write(content.getBytes());
			zos.closeEntry();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/** Quick sanity check that the in-memory zip fixtures are well-formed. */
	@Test
	void fixtureSanity_zipBytesAreReadable(@TempDir Path tempDir) throws IOException {
		Path zip = tempDir.resolve("sanity.zip");
		ByteArrayOutputStream sink = new ByteArrayOutputStream();
		try (ZipOutputStream zos = new ZipOutputStream(sink)) {
			addFile(zos, "a.txt", "x");
		}
		Files.write(zip, sink.toByteArray());
		assertTrue(Files.size(zip) > 0);
	}
}
