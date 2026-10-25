# pebble-forge-cli

Renders one Pebble Forge template with a JSON or YAML model and prints the same
include and extends source-frame diagnostics as the Gradle `renderPebbleTemplate` task.

## Usage

```shell
./gradlew :pebble-forge-cli:run --args="--templates src/main/resources/templates --template page --model-file model.json"
```

| Option | Default | Meaning |
| --- | --- | --- |
| `-t`, `--templates` | `src/main/resources/templates` | Template root |
| `-s`, `--suffix` | `.peb` | Loader suffix |
| `-n`, `--template` | required | Template name without the suffix |
| `-m`, `--model` | empty | Inline JSON model |
| `-f`, `--model-file` | none | JSON or YAML model file |
| `-c`, `--component-root` | `components` | Component root |
| `--context-lines` | `2` | Source lines around an error |
| `--strict` | true | Fail on missing variables |
| `--lenient` | false | Render missing variables as empty values |
| `-o`, `--output` | stdout | Output file |
| `-h`, `--help` | | Print usage |

`--model` and `--model-file` are mutually exclusive. Inline models and `.json` files
use JSON. `.yaml` and `.yml` files use YAML. The model root must be an object.

Exit codes are 0 for success, 1 for a render failure, and 2 for a usage or model error.

The Gradle `run` task resolves relative paths from the repository root. The installed
script resolves them from the current directory. Run
`./gradlew :pebble-forge-cli:installDist` to create the script under
`pebble-forge-cli/build/install/pebble-forge-cli/bin`.

## Trusted templates only

This CLI is a development tool. It builds the engine without a sandbox, so a template
can use the methods and loaders that the JVM allows. Run it only on templates you trust.
