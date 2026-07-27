package Ligador;

import Mem.Memoria;
import Mem.Palavramem;
import Carregador.AbsoluteLoader;
import Regs.Registradores;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Ligador Relocável para a Arquitetura SIC/XE.
 * Realiza o ligamento de seções de controle, resolução de símbolos externos (EXTDEF/EXTREF) e relocalização.
 */
public class Ligador {
    
    private static final String OBJECT_FILE = "simulator\\src\\utils\\object_code.txt"; 
    private static int EXECADDR;
    private Map<String, Integer> ESTAB = new HashMap<>(); // Tabela de símbolos externos
    private String programName;


    public void pass1() {
        try (BufferedReader file = new BufferedReader(new FileReader(OBJECT_FILE))) {
            String register;
            
            while ((register = file.readLine()) != null) {
                String[] parts = register.split("\\^");
                char type = parts[0].charAt(0);
    
                if (type == 'H') {  // Cabeçalho
                    programName = parts[1].trim();
                } 
                else if (type == 'D') {  // Definição de símbolo externo
                    String symbol = parts[1].trim();
                    int address = Integer.parseInt(parts[2].trim(), 16) % 1000; // Garantir que está no intervalo de 0 a 999
                    ESTAB.put(symbol, address);
                    System.out.println("Passagem 1 - Definição de símbolo: " + symbol + " -> " + address);
                } 
            }
        } catch (IOException e) {
            System.err.println("Erro na leitura do arquivo objeto: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Erro ao converter número hexadecimal: " + e.getMessage());
        }
    }
    
    public void pass2(Memoria memoria) {
        try (BufferedReader file = new BufferedReader(new FileReader(OBJECT_FILE))) {
            String register;
            Registradores registradores = new Registradores();
            AbsoluteLoader loader = new AbsoluteLoader(memoria, registradores); // Criação do AbsoluteLoader
            
            while ((register = file.readLine()) != null) {
                String[] parts = register.split("\\^");
                char type = parts[0].charAt(0);
                
                if (type == 'H') {  // Cabeçalho
                    int firstFreeAddress = loader.findNextFreeMemoryIndex(); // findNextFreeMemoryIndex do AbsoluteLoader 
                    System.out.println("Passagem 2 - Carregando segmento " + programName + " no primeiro espaço livre: " + firstFreeAddress);
                } 
                else if (type == 'T') { // Trecho de código
                    int address = loader.findNextFreeMemoryIndex(); // Encontrar o próximo endereço livre
                    StringBuilder code = new StringBuilder();
                    for (int i = 3; i < parts.length; i++) {
                        code.append(parts[i]);
                    }
                    loader.moveToMemory(memoria, address, code.toString());  // Passando a memória para o método
                    System.out.println("Passagem 2 - Trecho de código carregado no endereço " + address + ": " + code);
                } 
                else if (type == 'M') { // Modificação
                    int address = Integer.parseInt(parts[1].trim(), 16) % 1000; // Ajuste no intervalo
                    if (parts.length >= 4) {
                        String symbol = parts[3].trim();
                        if (!ESTAB.containsKey(symbol)) {
                            System.err.println("Erro: Símbolo indefinido " + symbol);
                            continue;
                        }
                        int modification = ESTAB.get(symbol);
                        if (address < memoria.memoria.size()) {
                            memoria.updateMemory(address, modification);
                            System.out.println("Passagem 2 - Relocando símbolo " + symbol + " no endereço " + address + " com valor " + modification);
                        } else {
                            System.err.println("Erro: Endereço de memória inválido " + address);
                        }
                    } else {
                        System.err.println("Erro: Formato inválido na linha de modificação!");
                    }
                } 
                else if (type == 'E') { 
                    if (parts.length > 1) {
                        try {
                            EXECADDR = Integer.parseInt(parts[1].trim(), 16) % 1000; 
                        } catch (NumberFormatException e) {
                            System.err.println("Aviso: Endereço de execução inválido, usando padrão.");
                            EXECADDR = 0;
                        }
                    } else {
                        EXECADDR = 0;
                    }
                }
            }
    
            System.out.println("\nTabela de Símbolos Externos:");
            for (Map.Entry<String, Integer> entry : ESTAB.entrySet()) {
                System.out.println(entry.getKey() + " -> " + entry.getValue());
            }
    
            loader.executeAtAddress(EXECADDR); 
            
        } catch (IOException e) {
            System.err.println("Erro na leitura do arquivo objeto: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Erro ao converter número hexadecimal: " + e.getMessage());
        }
    }
    
    public void printMemory(Memoria memoria, int start, int count) {
        for (int i = start; i < start + count && i < memoria.memoria.size(); i++) {
            Palavramem palavra = memoria.memoria.get(i);
            byte[] bytes = palavra.getBytes();
            System.out.printf("Endereço %d: %02X %02X %02X\n", i, bytes[0], bytes[1], bytes[2]);
        }
    }
}
