package org.infinispan.plugins.proto.compatibility;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.descriptors.FileDescriptor;
import org.infinispan.protostream.descriptors.ProtoLock;
import org.infinispan.protostream.impl.parser.ProtostreamProtoParser;

/**
 * Protocol Buffers compatibility checks using ProtoStream
 *
 * @author Ryan Emerson
 * @since 10.0
 */
@Mojo(
      name = "proto-schema-compatibility-check",
      defaultPhase = LifecyclePhase.VERIFY,
      requiresDependencyResolution = ResolutionScope.COMPILE,
      threadSafe = true)
public class ProtoCompatibilityMojo extends AbstractMojo {
   @Parameter(defaultValue = "false")
   private boolean commitProtoLock;

   @Parameter(defaultValue = "${basedir}")
   private String protoLockRoot;

   @Parameter(defaultValue = "${project.build.directory}/classes/proto", readonly = true)
   private String protoSourceRoot;

   @Parameter
   private String remoteLockFiles;

   @Parameter(defaultValue = "false")
   private boolean skip;

   /**
    * Execute the plugin.
    *
    * @throws MojoExecutionException thrown when execution of protolock fails.
    */
   public void execute() throws MojoExecutionException {
      try {
         if (skip) {
            getLog().info("Skipping proto compatibility check");
            return;
         }

         Path lockFile = Paths.get(protoLockRoot, "proto.lock");
         boolean lockFileExists = Files.exists(lockFile);
         boolean remoteCheck = !remoteLockFiles.isEmpty();
         if (!commitProtoLock && !lockFileExists && !remoteCheck) {
            getLog().info("Ignoring protolock check as there isn't an existing proto.lock file, commitProtoLock=false and no remoteLockFiles are specified.");
            return;
         }
         ProtoLock protoNew = protoLockFromDir(Paths.get(protoSourceRoot));
         if (!lockFileExists) {
            checkRemoteCompatibility(protoNew);
            try (OutputStream os = Files.newOutputStream(lockFile)) {
               protoNew.writeLockFile(os);
            }
            getLog().info("Initialized protolock.");
         } else {
            ProtoLock protoOld;
            try (InputStream is = Files.newInputStream(lockFile)) {
               protoOld = ProtoLock.readLockFile(is);
            }
            protoOld.checkCompatibility(protoNew, true);
            getLog().info(String.format("Backwards compatibility check against local file '%s' passed.", lockFile));
            checkRemoteCompatibility(protoNew);

            if (commitProtoLock) {
               try (OutputStream os = Files.newOutputStream(lockFile)) {
                  protoNew.writeLockFile(os);
               }
               getLog().info("Schema changes committed to proto.lock.");
            }
         }
      } catch (IOException e) {
         throw new MojoExecutionException("An error occurred while running protolock", e);
      }
   }

   private ProtoLock protoLockFromDir(Path protoRoot) throws IOException {
      List<Path> protoFiles;
      try (Stream<Path> stream = Files.walk(protoRoot)) {
         protoFiles = stream.filter(p -> p.getFileName().toString().endsWith(".proto")).toList();
      }
      FileDescriptorSource fds = new FileDescriptorSource();
      for (Path p : protoFiles) {
         fds.addProtoFile(protoRoot.relativize(p).toString(), p.toFile());
      }
      ProtostreamProtoParser parser = new ProtostreamProtoParser(Configuration.builder().build());
      Map<String, FileDescriptor> descriptors = parser.parse(fds);
      return new ProtoLock(descriptors.values());
   }

   private void checkRemoteCompatibility(ProtoLock currentState) throws IOException {
      if (remoteLockFiles.isEmpty())
         return;

      for (String file : remoteLockFiles.split(",")) {
         getLog().info(String.format("Checking backwards compatibility check against remote file '%s'", file));
         try (InputStream is = new URL(file).openStream()) {
            ProtoLock remoteLockFile = ProtoLock.readLockFile(is);
            currentState.checkCompatibility(remoteLockFile, true);
         }
         getLog().info(String.format("Backwards compatibility check against remote file '%s' passed", file));
      }
   }
}
