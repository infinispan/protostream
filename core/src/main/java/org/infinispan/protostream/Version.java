package org.infinispan.protostream;

import java.util.Objects;

/**
 * Provides version information about this ProtoStream release.
 *
 * @author anistor@redhat.com
 * @since 4.2
 */
public final class Version implements Comparable<Version> {

   private static final Version VERSION = getVersion(Version.class);

   /**
    * Try to obtain the version from the manifest.
    */
   public static Version getVersion(Class<?> clazz) {
      String version = clazz.getPackage().getImplementationVersion();

      if (version != null) {
         try {
            String[] versionParts = version.split("[.\\-]");
            int major = Integer.parseInt(versionParts[0]);
            int minor = versionParts.length > 1 ? Integer.parseInt(versionParts[1]) : 0;
            int micro = versionParts.length > 2 ? Integer.parseInt(versionParts[2]) : 0;
            String suffix = versionParts.length > 3 ? versionParts[3] : null;
            return new Version(major, minor, micro, suffix);
         } catch (Exception e) {
            // ignore
         }
      }

      return new Version(0, 0, 0, "UNKNOWN");
   }

   public static Version getVersion() {
      return VERSION;
   }

   public static void main(String[] args) {
      System.out.println("ProtoStream version " + getVersion());
   }

   private final int major;
   private final int minor;
   private final int micro;
   private final String suffix;
   private final String versionString;

   public Version(int major, int minor, int micro) {
      this(major, minor, micro, null);
   }

   public Version(int major, int minor, int micro, String suffix) {
      this.major = major;
      this.minor = minor;
      this.micro = micro;
      this.suffix = suffix;
      if (suffix == null) {
         suffix = "";
      } else if ("SNAPSHOT".equals(suffix)) {
         suffix = "-SNAPSHOT";
      } else {
         suffix = '.' + suffix;
      }
      versionString = major + "." + minor + "." + micro + suffix;
   }

   public int getMajor() {
      return major;
   }

   public int getMinor() {
      return minor;
   }

   public int getMicro() {
      return micro;
   }

   public String getSuffix() {
      return suffix;
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
      return other.major == major && other.minor == minor && other.micro == micro && Objects.equals(suffix, other.suffix);
   }

   @Override
   public int hashCode() {
      return 31 * (31 * (31 * major + minor) + micro) + (suffix != null ? suffix.hashCode() : 0);
   }

   @Override
   public int compareTo(Version other) {
      if (this == other) {
         return 0;
      }
      int d = major - other.major;
      if (d == 0) {
         d = minor - other.minor;
         if (d == 0) {
            d = micro - other.micro;
            if (d == 0) {
               if (suffix == null) {
                  d = other.suffix == null ? 0 : -1;
               } else {
                  d = other.suffix == null ? 1 : suffix.compareTo(other.suffix);
               }
            }
         }
      }
      return d;
   }
}
