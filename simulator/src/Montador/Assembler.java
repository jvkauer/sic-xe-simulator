package Montador;

import java.io.*;
import java.util.*;

/**
 * Montador de Duas Passagens (Two-Pass Assembler) para a Arquitetura SIC/XE.
 * Realiza o cálculo do LOCCTR, geração da SYMTAB no Passo 1 e emissão do Código Objeto no Passo 2.
 */
public class Assembler {

    public static void main(String[] args) {
        try {
            // Chama a função para carregar as instruções do arquivo
            Map<String, Instruction> instructionSet = loadInstructionsFromFile("simulator/src/utils/instructions.txt");

            ArrayList<Lines> input = readInputFile(instructionSet);

            writeIntermediateFile(input);

            Map<String, Integer> symbolTable = readSymbolTable("simulator/src/utils/pass1_symbol_table.txt");

            File sourceFile = new File("simulator/src/utils/MASMAPRG.asm");

            int finalPosition = secondPass(sourceFile, instructionSet, symbolTable);

            String intermediateFile = "simulator/src/utils/pass2_intermediate_file.txt";
            String outputFile = "simulator/src/utils/object_code.txt";

            generateObjectCode(intermediateFile, outputFile, finalPosition);

            // Exemplo de como acessar uma instrução específica
            Instruction addInstruction = instructionSet.get("ADD");
            if (addInstruction != null) {
                System.out.println("ADD - Formato: " + addInstruction.getFormat() + ", Opcode: " + addInstruction.getOpcode());
            }

        } catch (FileNotFoundException e) {
            System.out.println("Arquivo não encontrado: " + e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static ArrayList<Lines> readInputFile(Map<String, Instruction> instructionSet) throws FileNotFoundException {
        File file = new File("simulator/src/utils/MASMAPRG.asm");
        ArrayList<Lines> lines = new ArrayList<>();
        Map<String, Integer> symbolTable = new LinkedHashMap<>();

        int position = 0;

        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();

                // Ignorar comentários e linhas vazias
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("\\s+");
                String label = (parts.length == 3) ? parts[0] : "     ";
                String mnemonic = (parts.length == 3) ? parts[1] : parts[0];
                String value = (parts.length == 3) ? parts[2] : (parts.length == 2 ? parts[1] : "");

                // Diretiva START
                if (mnemonic.equalsIgnoreCase("START")) {
                    position = Integer.parseInt(value, 16);
                    tryInsertLabel(label, position, symbolTable);
                    lines.add(new Lines(position, label, "START", value));
                    continue;
                }

                // Diretiva END
                if (mnemonic.equalsIgnoreCase("END")) {
                    lines.add(new Lines(position, label, "END", value));
                    continue;
                }

                // Inserir label antes de atualizar o position
                tryInsertLabel(label, position, symbolTable);

                // Diretiva WORD
                if (mnemonic.equalsIgnoreCase("WORD")) {
                    lines.add(new Lines(position, label, "WORD", value));
                    position += 3;
                    continue;
                }

                // Diretiva RESW
                if (mnemonic.equalsIgnoreCase("RESW")) {
                    lines.add(new Lines(position, label, "RESW", value));
                    position += 3 * Integer.parseInt(value);
                    continue;
                }

                // Instrução RSUB (sem operando)
                if (mnemonic.equalsIgnoreCase("RSUB")) {
                    lines.add(new Lines(position, label, "RSUB", ""));
                    position += 3; // formato fixo
                    continue;
                }

                // Instruções padrão
                Instruction instruction = instructionSet.get(mnemonic);
                if (instruction != null) {
                    lines.add(new Lines(position, label, mnemonic, value));
                    position += instruction.getFormat();
                } else {
                    System.out.println("Erro: Instrução desconhecida ou mal formatada - " + mnemonic);
                }
            }

            writeSymbolTable(symbolTable);

        } catch (NumberFormatException e) {
            System.out.println("Erro ao converter número: " + e.getMessage());
        }

        return lines;
    }

    // Função auxiliar para adicionar rótulos
    private static void tryInsertLabel(String label, int address, Map<String, Integer> symbolTable) {
        if (!label.isBlank() && !label.equals("     ")) {
            if (symbolTable.containsKey(label)) {
                System.out.println("Erro: Rótulo duplicado - " + label);
            } else {
                symbolTable.put(label, address);
            }
        }
    }



    // Função para carregar as instruções do arquivo e preencher o HashMap
    private static Map<String, Instruction> loadInstructionsFromFile(String fileName) throws FileNotFoundException {
        Map<String, Instruction> instructionSet = new HashMap<>();

        // Abrindo o arquivo
        File file = new File(fileName);
        try (Scanner scanner = new Scanner(file)) {
            // Lendo cada linha do arquivo
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();

                // Ignorar linhas vazias ou comentários (caso haja)
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Dividir a linha por espaços ou tabulações
                String[] parts = line.split("\\s+");

                if (parts.length == 3) {
                    String instructionName = parts[0];
                    int format = Integer.parseInt(parts[1]);
                    String opcode = parts[2];

                    // Adicionar ao HashMap
                    instructionSet.put(instructionName, new Instruction(format, opcode));
                }
            }
        }

        return instructionSet;
    }

    public static void writeIntermediateFile(ArrayList<Lines> linesList) {
        try (FileWriter writer = new FileWriter("simulator/src/utils/pass1_intermediate_file.txt")) {
            // Percorre cada linha no ArrayList
            for (Lines line : linesList) {
                // Escreve cada campo de cada objeto Lines separando com um espaço
                writer.write(line.address() + " " + line.label() + " " + line.mnemonic() + " " + line.value() + "\n");
            }
            System.out.println("Arquivo 'intermediate_file.txt' foi criado com sucesso.");
        } catch (IOException e) {
            System.out.println("Erro ao escrever o arquivo: " + e.getMessage());
        }
    }

    private static void writeSymbolTable(Map<String, Integer> symbolTable) {
        try (FileWriter writer = new FileWriter("simulator/src/utils/pass1_symbol_table.txt")) {
            for (Map.Entry<String, Integer> entry : symbolTable.entrySet()) {
                writer.write(String.format("%-10s %04X\n", entry.getKey(), entry.getValue()));
            }
            System.out.println("Arquivo 'pass1_symbol_table.txt' foi criado com sucesso.");
        } catch (IOException e) {
            System.out.println("Erro ao escrever o arquivo: " + e.getMessage());
        }
    }


    // Classe para armazenar o formato e opcode de cada instrução
    static class Instruction {
        private int format;   // Formato da instrução (1, 2, 3, 4)
        private String opcode; // Código de operação (opcode)

        public Instruction(int format, String opcode) {
            this.format = format;
            this.opcode = opcode;
        }

        public int getFormat() {
            return format;
        }

        public String getOpcode() {
            return opcode;
        }
    }

    static class Lines {
        private int address;
        private String label;
        private String mnemonic;
        private String value;

        public Lines(
                int address,
                String label,
                String mnemonic,
                String value
        ) {
            this.address = address;
            this.label = label;
            this.mnemonic = mnemonic;
            this.value = value;
        }

        public String address() {
            return String.format("%04X", address).toUpperCase();
        }

        public String label() {
            return label;
        }

        public String mnemonic() {
            return mnemonic;
        }

        public String value() {
            return value;
        }
    }

    public static Map<String, Integer> readSymbolTable(String fileName) {
        Map<String, Integer> symbolTable = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Divide a linha em partes usando espaços como delimitadores
                String[] parts = line.trim().split("\\s+");
                if (parts.length == 2) {
                    String symbol = parts[0];
                    // Converte o valor hexadecimal para um inteiro
                    int address = Integer.parseInt(parts[1], 16);
                    symbolTable.put(symbol, address);
                } else {
                    System.out.println("Linha mal formatada: " + line);
                }
            }
            System.out.println("Arquivo '" + fileName + "' foi lido com sucesso.");
        } catch (IOException e) {
            System.out.println("Erro ao ler o arquivo: " + e.getMessage());
        }

        return symbolTable;
    }

    public static int secondPass(File file, Map<String, Instruction> instructionSet, Map<String, Integer> symbolTable) throws IOException {
        int position = 0;
        List<String> intermediateTable = new ArrayList<>();

        try (Scanner scanner = new Scanner(file);
             BufferedWriter writer = new BufferedWriter(new FileWriter("simulator/src/utils/pass2_intermediate_file.txt"))) {

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\s+");
                String label = "", mnemonic = "", operand = "", objectCode = "";

                // START
                if (line.contains("START")) {
                    operand = parts[parts.length == 3 ? 2 : 1];
                    position = Integer.parseInt(operand, 16);
                    mnemonic = (parts.length == 3) ? parts[1] : parts[0];
                    label = (parts.length == 3) ? parts[0] : "NOLABEL";

                    intermediateTable.add(String.format("%04X %-8s %-8s %-8s", position, label, mnemonic, operand));
                    continue;
                }

                // END
                if (parts[0].equalsIgnoreCase("END")) {
                    operand = (parts.length > 1) ? parts[1] : "";
                    intermediateTable.add(String.format("%04X %-8s %-8s %-8s", position, "NOLABEL", "END", operand));
                    break;
                }

                // Parsing geral
                if (parts.length == 3) {
                    label = parts[0];
                    mnemonic = parts[1];
                    operand = parts[2];
                } else if (parts.length == 2) {
                    label = "NOLABEL";
                    mnemonic = parts[0];
                    operand = parts[1];
                } else {
                    label = "NOLABEL";
                    mnemonic = parts[0];
                    operand = "";
                }

                // RSUB (sem operando, mas precisa alinhar!)
                if (mnemonic.equalsIgnoreCase("RSUB")) {
                    objectCode = "4F0000";
                    operand = "0000"; // Garante que o campo 4 exista
                    intermediateTable.add(String.format("%04X %-8s %-8s %-8s %s", position, label, mnemonic, operand, objectCode));
                    position += 3;
                    continue;
                }

                // Instruções normais
                Instruction instruction = instructionSet.get(mnemonic);
                if (instruction != null) {
                    int format = instruction.getFormat();
                    String opcode = instruction.getOpcode();
                    int operandAddress = 0;

                    if (!operand.isBlank() && symbolTable.containsKey(operand)) {
                        operandAddress = symbolTable.get(operand);
                    }

                    if (format == 3) {
                        objectCode = String.format("%02X%04X", Integer.parseInt(opcode, 16), operandAddress);
                    } else if (format == 2) {
                        objectCode = opcode + "00";
                    } else if (format == 1) {
                        objectCode = opcode;
                    }

                    intermediateTable.add(String.format("%04X %-8s %-8s %-8s %s", position, label, mnemonic, operand, objectCode));
                    position += format;
                    continue;
                }

                // WORD
                if (mnemonic.equalsIgnoreCase("WORD")) {
                    int wordValue = Integer.parseInt(operand);
                    objectCode = String.format("%06X", wordValue).toUpperCase();
                    intermediateTable.add(String.format("%04X %-8s %-8s %-8s %s", position, label, mnemonic, operand, objectCode));
                    position += 3;
                    continue;
                }

                // RESW
                if (mnemonic.equalsIgnoreCase("RESW")) {
                    int n = Integer.parseInt(operand);
                    position += 3 * n;
                    continue;
                }

                // Instrução desconhecida
                intermediateTable.add(String.format("%04X %-8s %-8s %-8s ??", position, label, mnemonic, operand));
            }

            for (String l : intermediateTable) {
                writer.write(l);
                writer.newLine();
            }

            System.out.println("📍 Segunda passagem finalizada. Último endereço usado: " + String.format("%04X", position));
        }

        return position;
    }

    public static void generateObjectCode(String intermediateFile, String outputFile, int finalPosition) {
        try (BufferedReader reader = new BufferedReader(new FileReader(intermediateFile));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            String programName = "";
            int startAddress = 0;
            int lastAddress = 0;

            List<String> textRecords = new ArrayList<>();
            StringBuilder currentRecord = new StringBuilder();
            int currentRecordStart = -1;
            int currentRecordLength = 0;

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("🔎 Lendo linha: " + line);

                String[] parts = line.trim().split("\\s+", 5);
                if (parts.length < 4) {
                    System.out.println("⚠️ Linha ignorada (menos de 4 partes)");
                    continue;
                }

                int address = Integer.parseInt(parts[0], 16);
                String label = parts[1];
                String mnemonic = parts[2];
                String operand = parts[3];
                String objectCode = (parts.length == 5) ? parts[4] : "";

                System.out.printf("🧩 Parsed: Addr=%04X Label='%s' Mnemonic='%s' Operand='%s' ObjCode='%s'\n",
                        address, label, mnemonic, operand, objectCode);

                // START
                if (mnemonic.equalsIgnoreCase("START")) {
                    programName = label.equals("NOLABEL") ? "" : label;
                    startAddress = address;
                    lastAddress = startAddress;
                    System.out.printf("📦 START detectado. Programa: %s | Início: %04X\n", programName, startAddress);
                    continue;
                }

                // END
                if (mnemonic.equalsIgnoreCase("END")) {
                    if (currentRecordLength > 0) {
                        textRecords.add(String.format("T^%06X^%02X^%s", currentRecordStart, currentRecordLength, currentRecord));
                        System.out.printf("🧱 Text record: T^%06X^%02X^%s\n", currentRecordStart, currentRecordLength, currentRecord);
                    }
                    System.out.printf("✅ Finalizando com End Record: E^%06X\n", startAddress);
                    break;
                }

                if (objectCode.isBlank() || objectCode.equals("??")) {
                    System.out.println("⚠️ Object code ausente ou inválido, pulando.");
                    continue;
                }

                // 🧩 Quebra de text record se endereço não for contínuo
                if (address != lastAddress && currentRecordLength > 0) {
                    textRecords.add(String.format("T^%06X^%02X^%s", currentRecordStart, currentRecordLength, currentRecord));
                    System.out.printf("🧱 Text record: T^%06X^%02X^%s\n", currentRecordStart, currentRecordLength, currentRecord);

                    currentRecord = new StringBuilder();
                    currentRecordStart = address;
                    currentRecordLength = 0;
                    System.out.printf("📌 Novo Text Record iniciado em: %04X\n", address);
                }

                // Início do primeiro Text Record
                if (currentRecordStart == -1) {
                    currentRecordStart = address;
                    System.out.printf("📌 Iniciando novo Text Record em: %04X\n", address);
                }

                int objCodeLength = objectCode.length() / 2;

                // Se passar de 30 bytes, quebra
                if (currentRecordLength + objCodeLength > 30) {
                    textRecords.add(String.format("T^%06X^%02X^%s", currentRecordStart, currentRecordLength, currentRecord));
                    System.out.printf("🧱 Text record: T^%06X^%02X^%s\n", currentRecordStart, currentRecordLength, currentRecord);

                    currentRecord = new StringBuilder();
                    currentRecordStart = address;
                    currentRecordLength = 0;
                    System.out.printf("📌 Novo Text Record iniciado em: %04X\n", address);
                }

                currentRecord.append(objectCode);
                currentRecordLength += objCodeLength;
                lastAddress = address + objCodeLength;
            }

            // Cálculo do tamanho do programa
            int programLength = finalPosition - startAddress;
            System.out.printf("📐 Calculado programLength: %04X (dec %d)\n", programLength, programLength);

            // Header
            writer.write(String.format("H^%-6s^%06X^%06X\n", programName, startAddress, programLength));
            System.out.printf("📤 Gerando Header: H^%-6s^%06X^%06X\n", programName, startAddress, programLength);

            // Text records
            for (String record : textRecords) {
                writer.write(record + "\n");
            }

            // End record
            writer.write(String.format("E^%06X\n", startAddress));
        } catch (IOException e) {
            System.err.println("❌ Erro ao gerar o object code: " + e.getMessage());
        }
    }

}
