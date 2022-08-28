# Spring Tips: "Bites" Engine 

This program discovers, renders, and schedules tweets that provide mini Spring Tips, introducing new things in Twitter-sized "bites." I keep the content for my Spring Tips "Bites" [in a Github repository](https://github.com/spring-tips/spring-tips-twitter-tips.git), which this program monitors. I've configured a webhook from that repository so that this service reindexes each time there's an update there. 


## Previews 
If you want to validate your `tip.xml` manifest, you can use the `/tips/preview` endpoint. It is locked down, however, to anyone but the users in your `stb_users` table.

 
## Building 
You can build the application in the usual way: 

```shell 
mvn clean package
```

If you want to build a new Docker image, then use the built-in support [for Buildpacks](https://buildpacks.io): 

```shell 
mvn spring-boot:build-image
```

This application is configured to use the `full` Buildpack builder because it uses ([Apache Batik](https://xmlgraphics.apache.org/batik/), used for rendering SVG elements) that ultimately depends on Java's AWT subsystems. 

## Formatting
This program uses the Spring Boot JavaFormat Maven plugin to ensure that the source code has the same formatting regardless of both how someone edits and in which IDE they edit it. It'll break the build if code is committed without first running the formatting plugin. Thus, before committing every change, run:

```shell 
mvn spring-javaformat:apply
```


## Fonts

The program uses fonts that I am pretty sure that I can't share with others. I've encrypted an archive of the fonts and stored them in this `Git` repository. To unencrypt the archive, you'll need a _salt_ and a _password_, which I've stored in my secrets manager in the `developer-advocacy` group in a secret note for `spring tips bites`. 

If you want to change the fonts, you need to put them in to a `.tar.gz` archive, such that they are the files that result from the un-archiving. I used the following command to create the archive:

```shell
tar -c * | gzip -9 > fonts.tgz
```

Then I use the following Java code with Apache Commons Compress on the classpath to encrypt it:

```java
  BytesEncryptor encryptor = Encryptors.stronger(password, salt);
  File decryptedTgz = ... // the location of `fonts.tgz` from above
  if (decryptedTgz.exists()) {
      log.debug("somebody needs to do some encrypting!");
      var bytes = this.encryptor.encrypt(FileCopyUtils.copyToByteArray(decryptedTgz.getInputStream()));
      var writtenEncryptedTgz = new File(this.outputDirectory, "fonts.tgz.encrypted");
      ResourceUtils.write(bytes, new FileSystemResource(writtenEncryptedTgz));
      log.debug("wrote the encrypted file to " + writtenEncryptedTgz.getAbsolutePath());
  }
```

Replace `src/main/resources/fonts.tgz.encrypted` with the newly encrypted archive, then `git commit -am polish` and `git push`. 


## Rebuild 

The `Repository` keeps track of all the published Spring Tips from the Git repository. We rebuild that index whenever there's a change in the github repository. I've configured a webhook on that git repository to send an `application/json` content-type `POST` request to `/refresh`.

## Scheduling 

We should have a periodic timer that pulls for everything in the DB that hasn't been scheduled, choosing the oldest of the scheduled items first. We want to make sure that the oldest scheduled items get promoted first.