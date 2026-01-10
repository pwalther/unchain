import org.yaml.snakeyaml.Yaml
import java.io.File

def yamlFile = new File(project.basedir, 'src/main/resources/api/openapi.yaml')
if (yamlFile.exists()) {
    def yaml = new Yaml()
    def data = yaml.load(yamlFile.text)
    def outputDir = new File(project.build.directory, 'generated-sources/custom-security/ch/redmoon/unchain/security')
    outputDir.mkdirs()

    def sb = new StringBuilder()
    sb.append('package ch.redmoon.unchain.security;\n\n')
    sb.append('import java.util.List;\n')
    sb.append('import java.util.Map;\n')
    sb.append('import java.util.Collections;\n')
    sb.append('import java.util.HashMap;\n\n')
    sb.append('public class GeneratedApiMetadata {\n')
    sb.append('    public static final Map<String, List<String>> PERMISSION_MAP;\n\n')
    sb.append('    static {\n')
    sb.append('        Map<String, List<String>> map = new HashMap<>();\n')

    if (data.paths) {
        data.paths.each { path, methods ->
            methods.each { method, op ->
                if (op instanceof Map && op['x-required-permissions']) {
                    def perms = op['x-required-permissions'].collect { '"' + it + '"' }.join(', ')
                    sb.append('        map.put("' + method.toUpperCase() + ':' + path + '", List.of(' + perms + '));\n')
                }
            }
        }
    }

    sb.append('        PERMISSION_MAP = Collections.unmodifiableMap(map);\n')
    sb.append('    }\n')
    sb.append('}\n')

    new File(outputDir, 'GeneratedApiMetadata.java').text = sb.toString()
    project.addCompileSourceRoot(new File(project.build.directory, 'generated-sources/custom-security').absolutePath)
}
