# Proto Schema Compatibility Plugin
The proto-schema-compatibility-plugin is a Maven plugin to run a backwards compatibility check on a set of protobuf schemas.

The plugin utilises ProtoStream to generate a `proto.lock` file for each maven module
that it is applied to. If a `proto.lock` file already exists for that module, then the existing file is utilised to ensure
that any changes to the latest code are backwards compatible with the definitions in the existing lock file. As proto
schemas evolve, it's necessary for a modules `proto.lock` file to be updated by setting `<commitProtoLock>true</commitProtoLock>`
in the plugin's configuration and committing the generated lock files.

To force any breaking changes and reset the current state, it's necessary to delete the offending `proto.lock` file(s),
however this should only be done when fields have had sufficient time to be truly obsolete, i.e. across multiple major
version changes when rolling upgrades are no longer supported.

For more information about the base `proto.lock` format, see https://github.com/nilslice/protolock
