The '*.java' files under resources/org/infinispan/protostream/integrationtests are really just resource files, not Java
source files. They are _not_ expected to be compiled during build!

Instead, they are used as input for compilation tests with our annotation processor during tests execution phase, so
they might even intentionally contain errors that are to be asserted by the test suite. If you see such errors, do not 
'fix' them, _please_ :)

Also leave their style/formatting as is. These files are not subject to strict checkstyle rules.
