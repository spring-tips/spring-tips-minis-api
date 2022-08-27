package com.joshlong.springtips.bites;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;

import java.awt.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Handles registering the custom fonts used in the {@code svg} XML stanza
 * <p>
 * TODO the fonts should be in a zip file which in turn should be encrypted with a
 * password. Once encrypted, only the password should be able to decrypt it.
 * <p>
 * to make this work, i'll need a password, Spring Security Encryptors, code to read the
 * unencrypt the zip file, read it, store it to a disk, and then load the fonts
 */
@Slf4j
@Configuration
class FontConfiguration implements ApplicationListener<ApplicationReadyEvent> {

	private final BytesEncryptor encryptor;

	private final File outputDirectory;

	FontConfiguration(SpringTipsProperties properties) {
		this.outputDirectory = properties.outputDirectory();
		ensureDirectory(this.outputDirectory);
		Assert.hasText(properties.fonts().encryption().password(), "the password is empty");
		Assert.hasText(properties.fonts().encryption().salt(), "the salt is empty");
		this.encryptor = Encryptors.stronger(properties.fonts().encryption().password(),
				properties.fonts().encryption().salt());
	}

	@SneakyThrows
	private static void registerFonts(Collection<Resource> resources) {
		var localGraphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
		for (var resource : resources) {
			var font = Font.createFont(Font.TRUETYPE_FONT, resource.getInputStream());
			localGraphicsEnvironment.registerFont(font);
		}
		if (log.isDebugEnabled()) {
			var font = localGraphicsEnvironment.getAvailableFontFamilyNames();
			for (var fontName : font)
				log.debug("font: " + fontName);
		}
	}

	private static void ensureDirectory(File directory) {
		Assert.state((directory.exists() && directory.isDirectory()) || (!directory.exists() && directory.mkdirs()),
				"couldn't create directory [" + directory.getAbsolutePath() + "]");
	}

	@SneakyThrows
	private static void extractArchive(Resource archive, File targetDir) {
		try (var fi = archive.getInputStream();
				var bi = new BufferedInputStream(fi);
				var gzi = new GzipCompressorInputStream(bi);
				var i = new TarArchiveInputStream(gzi)) {
			ensureDirectory(targetDir);
			var entry = (ArchiveEntry) null;
			while ((entry = i.getNextEntry()) != null) {
				var f = new File(targetDir, entry.getName());
				if (entry.isDirectory()) {
					ensureDirectory(f);
				} //
				else {
					ensureDirectory(f.getParentFile());
					try (var o = Files.newOutputStream(f.toPath())) {
						IOUtils.copy(i, o);
					}
				}
			}
		}
	}

	// todo extract the encrypted fonts.tgz to a directory, then unzip it, then load the
	// pacakgged file
	@Override
	@SneakyThrows
	public void onApplicationEvent(ApplicationReadyEvent event) {

		var encryptedTgz = new ClassPathResource("/fonts.tgz.encrypted");
		var decryptedTgz = new FileSystemResource(new File(this.outputDirectory, "fonts.tgz"));
		var extractedTgzTargetDirectory = new File(this.outputDirectory, "extracted");
		FileSystemUtils.deleteRecursively(extractedTgzTargetDirectory);
		ensureDirectory(extractedTgzTargetDirectory);

		if (decryptedTgz.exists()) {
			log.debug("somebody needs to do some encrypting!");
			var bytes = this.encryptor.encrypt(FileCopyUtils.copyToByteArray(decryptedTgz.getInputStream()));
			var writtenEncryptedTgz = new File(this.outputDirectory, "fonts.tgz.encrypted");
			ResourceUtils.write(bytes, new FileSystemResource(writtenEncryptedTgz));
			log.debug("wrote the encrypted file to " + writtenEncryptedTgz.getAbsolutePath());
		}

		try (var in = encryptedTgz.getInputStream()) {
			var bytes = FileCopyUtils.copyToByteArray(in);
			var decrypted = this.encryptor.decrypt(bytes);
			FileCopyUtils.copy(decrypted, decryptedTgz.getOutputStream());
			extractArchive(decryptedTgz, extractedTgzTargetDirectory);
			var extractedFontFiles = extractedTgzTargetDirectory.listFiles((dir, name) -> {
				var extensions = "otf,ttf".split(",");
				var lcName = name.toLowerCase(Locale.ROOT);
				for (var ext : extensions)
					if (lcName.endsWith("." + ext) && !lcName.startsWith("._"))
						return true;
				return false;
			});
			var setOfFonts = Arrays.stream(extractedFontFiles).map(f -> (Resource) new FileSystemResource(f))
					.collect(Collectors.toSet());
			registerFonts(setOfFonts);
		}
	}

}
