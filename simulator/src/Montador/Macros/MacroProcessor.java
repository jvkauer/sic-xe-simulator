package Montador.Macros;

import java.io.*;
import java.util.*;

/**
 * Processador de Macros para a Arquitetura SIC/XE.
 * Realiza a expansão de definições de macros, substituição de parâmetros reais e geração do código assembly expandido.
 */
public class MacroProcessor {

    static class Macro {
        String name;
        List<String> body = new ArrayList<>();
        List<String> parameters = new ArrayList<>();

        public Macro(String name) {
            this.name = name;
        }
    }

    static Map<String, Macro> macroTable = new HashMap<>();

    public static List<String> processMacros(String filename) throws IOException {
        List<String> finalCode = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(filename));

        String line;
        boolean isMacroDefinition = false;
        Macro currentMacro = null;

        while ((line = reader.readLine()) != null) {
            line = line.trim();

            if (line.startsWith("MACRO")) { // Início da macro
                isMacroDefinition = true;
                String[] parts = line.split("\\s+");
                currentMacro = new Macro(parts[1]); // Nome da macro
                for (int i = 2; i < parts.length; i++) {
                    // Remove vírgulas, se houver, para que o parâmetro fique correto
                    String param = parts[i].replace(",", "");
                    currentMacro.parameters.add(param);
                }
                continue;
            }

            if (isMacroDefinition) {
                if (line.equals("MEND")) { // Final da macro
                    macroTable.put(currentMacro.name, currentMacro);
                    isMacroDefinition = false;
                    continue;
                }
                currentMacro.body.add(line); // Armazena linha da macro
            } else {
                finalCode.add(expandLine(line)); // Expande macros no código principal
            }
        }
        reader.close();
        return finalCode;
    }

    private static String expandLine(String line) {
        // Divide a linha em no máximo 2 partes: o nome da macro e o restante (argumentos)
        String[] parts = line.split("\\s+", 2);
        if (parts.length == 0) return line;

        String macroName = parts[0];
        if (macroTable.containsKey(macroName)) { // Macro encontrada
            Macro macro = macroTable.get(macroName);
            Map<String, String> argMap = new HashMap<>();

            if (parts.length > 1) {
                // Separa os argumentos utilizando a vírgula como delimitador
                String[] args = parts[1].split(",");
                for (int i = 0; i < macro.parameters.size(); i++) {
                    if (i < args.length) {
                        String arg = args[i].trim();
                        argMap.put(macro.parameters.get(i), arg);
                    }
                }
            }

            StringBuilder expanded = new StringBuilder();
            for (String macroLine : macro.body) {
                String expandedLine = macroLine;
                for (Map.Entry<String, String> entry : argMap.entrySet()) {
                    expandedLine = expandedLine.replace(entry.getKey(), entry.getValue());
                }
                // Verifica se a linha expandida contém chamadas de macro e as expande recursivamente
                expanded.append(expandLine(expandedLine)).append("\n");
            }
            return expanded.toString().trim();
        }
        return line; // Retorna a linha original se não for uma chamada de macro
    }


    public static void writeToFile(List<String> lines, String filename) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        for (String line : lines) {
            writer.write(line);
            writer.newLine();
        }
        writer.close();
    }
}
