package com.joshlong.springtips.bites;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Configuration
class FontConfiguration implements ApplicationListener<ApplicationReadyEvent> {

	private final BytesEncryptor encryptor;

	private final File outputDirectory;

	FontConfiguration(SpringTipsProperties properties) {
		var encryption = properties.fonts().encryption();
		var password = encryption.password();
		var salt = encryption.salt();
		var directory = properties.outputDirectory();

		Assert.hasText(password, "the password is empty");
		Assert.hasText(salt, "the salt is empty");

		this.encryptor = Encryptors.stronger(password, salt);
		this.outputDirectory = ensureDirectory(directory);
	}

	@Override
	@SneakyThrows
	public void onApplicationEvent(ApplicationReadyEvent event) {
		var encryptedTgz = new ClassPathResource("/fonts.tgz.encrypted");
		var decryptedTgz = new FileSystemResource(new File(this.outputDirectory, "fonts.tgz"));
		var extractedTgzTargetDirectory = new File(this.outputDirectory, "extracted");
		FileSystemUtils.deleteRecursively(extractedTgzTargetDirectory);
		ensureDirectory(extractedTgzTargetDirectory);
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
			var setOfFonts = Arrays//
					.stream(Objects.requireNonNull(extractedFontFiles))//
					.map(f -> (Resource) new FileSystemResource(f)) //
					.collect(Collectors.toSet());
			registerFonts(setOfFonts);
		}
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

	private static File ensureDirectory(File directory) {
		Assert.state((directory.exists() && directory.isDirectory()) || (!directory.exists() && directory.mkdirs()),
				"couldn't create directory [" + directory.getAbsolutePath() + "]");
		return directory;
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

}
