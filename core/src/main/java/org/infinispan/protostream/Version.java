package org.infinispan.protostream;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Contains version information about this ProtoStream release.
 *
 * @author anistor@redhat.com
 * @since 4.2
 */
public final class Version implements Comparable<Version> {

   private static final Version VERSION = getArtifactVersion();

   /**
    * We try to obtain the Maven pom.properties of the artifact and get the build version from there.
    */
   private static Version getArtifactVersion() {
      int major = 0;
      int minor = 0;
      int patchLevel = 0;

      InputStream res = Version.class.getResourceAsStream("/META-INF/maven/org.infinispan.protostream/protostream/pom.properties");
      if (res != null) {
         try {
            Properties pomProps = new Properties();
            pomProps.load(res);
            String version = pomProps.getProperty("version", "0.0.0-UNKNOWN");
            String[] versionParts = version.split("[.\\-]");
            major = Integer.parseInt(versionParts[0]);
            minor = Integer.parseInt(versionParts[1]);
            patchLevel = Integer.parseInt(versionParts[2]);
         } catch (IOException | NumberFormatException | ArrayIndexOutOfBoundsException e) {
            // ignored
         }
      }

      return new Version(major, minor, patchLevel);
   }

   public static Version getVersion() {
      return VERSION;
   }

   public static void main(String[] args) {
      System.out.println("ProtoStream version " + getVersion());
   }

   private final int major;
   private final int minor;
   private final int patchLevel;
   private final String versionString;

   public Version(int major, int minor, int patchLevel) {
      this.major = major;
      this.minor = minor;
      this.patchLevel = patchLevel;
      versionString = major + "." + minor + "." + patchLevel;
   }

   public int getMajor() {
      return major;
   }

   public int getMinor() {
      return minor;
   }

   public int getPatchLevel() {
      return patchLevel;
   }

   @Override
   public String toString() {
      return versionString;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      }
      if (obj == null || obj.getClass() != getClass()) {
         return false;
      }
      Version other = (Version) obj;
      return other.major == major && other.minor == minor && other.patchLevel == patchLevel;
   }

   @Override
   public int hashCode() {
      return 31 * (31 * major + minor) + patchLevel;
   }

   @Override
   public int compareTo(Version other) {
      int d = major - other.major;
      if (d == 0) {
         d = minor - other.minor;
         if (d == 0) {
            d = patchLevel - other.patchLevel;
         }
      }
      return d;
   }
}
