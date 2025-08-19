package org.infinispan.plugins.proto.compatibility;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.maven.execution.MavenSession;
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

   @Parameter(defaultValue = "${project.build.directory}/classes")
   private String protoSourceRoot;

   @Parameter
   private String remoteLockFiles;

   @Parameter(defaultValue = "${session}")
   private MavenSession session;

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
         if (!commitProtoLock && !lockFileExists && !remoteCheck()) {
            getLog().info("Ignoring protolock check as there isn't an existing proto.lock file, commitProtoLock=false and no remoteLockFiles are specified.");
            return;
         }
         ProtoLock protoNew = protoLockFromDir(Paths.get(protoSourceRoot));
         if (lockFileExists) {
            ProtoLock protoOld;
            try (InputStream is = Files.newInputStream(lockFile)) {
               protoOld = ProtoLock.readLockFile(is);
            }
            protoOld.checkCompatibility(protoNew, true);
            getLog().info(String.format("Backwards compatibility check against local file '%s' passed.", lockFile));
         }

         if (remoteCheck())
            checkRemoteCompatibility(protoNew);

         if (commitProtoLock) {
            try (OutputStream os = Files.newOutputStream(lockFile)) {
               protoNew.writeLockFile(os);
            }
            getLog().info(
                  lockFileExists ?
                        "Schema changes committed to proto.lock." :
                        "Initialized protolock."
            );
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
      if (session.isOffline()) {
         getLog().info("Skipping backwards compatibility check against remote files as maven is in Offline mode");
         return;
      }

      Proxy proxy = configureProxy();
      for (String file : remoteLockFiles.split(",")) {
         getLog().info(String.format("Checking backwards compatibility check against remote file '%s'", file));
         URL url = new URL(file);
         try (InputStream is = url.openConnection(proxy).getInputStream()) {
            ProtoLock remoteLockFile = ProtoLock.readLockFile(is);
            remoteLockFile.checkCompatibility(currentState, true);
         }
         getLog().info(String.format("Backwards compatibility check against remote file '%s' passed", file));
      }
   }

   private boolean remoteCheck() {
      return remoteLockFiles != null && !remoteLockFiles.isEmpty();
   }

   private Proxy configureProxy() {
      String proxy = getEnvVarValue("http_proxy");
      if (proxy == null) {
         proxy = getEnvVarValue("https_proxy");
      }

      if (proxy == null) {
         return Proxy.NO_PROXY;
      }

      URI uri = URI.create(proxy);
      return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(uri.getHost(), uri.getPort()));
   }

   private String getEnvVarValue(String name) {
      String value = System.getenv(name);
      if (value == null || value.isBlank()) {
         value = System.getenv(name.toUpperCase());
      }
      return value;
   }
}
