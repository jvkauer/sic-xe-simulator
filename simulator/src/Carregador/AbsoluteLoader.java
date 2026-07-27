package Carregador;

import Mem.Memoria;
import Mem.Palavramem;
import Regs.Registradores;
import java.io.*;

/**
 * Carregador de Código Objeto (Absolute & Relocatable Loader) para o SIC/XE.
 * Processa registros Header, Text, Modification e End, alocando os bytes diretamente na memória principal simulada.
 */
public class AbsoluteLoader {
    private Memoria memoria;
    private Registradores registradores;

    public AbsoluteLoader(Memoria memoria, Registradores registradores) {
        this.memoria = memoria;
        this.registradores = registradores;
    }

    public void execute() {
        System.out.println("Memória antes da execução:");
        memoria.printMemory(10);
        loadModule("teste.txt");
        System.out.println("\nMemória após a execução:");
        memoria.printMemory(200);
    }

    public void executeAtAddress(int address) {
        System.out.println("Executando programa no endereço: " + address);
    }

    public void loadModule(String module) {
        try (BufferedReader file = new BufferedReader(new FileReader(module))) {
            String register;
            while ((register = file.readLine()) != null) {
                String[] parts = register.split("\\^");
                char type = parts[0].charAt(0);

                if (type == 'H') {
                    System.out.println("Header encontrado: " + register);
                } else if (type == 'T') {
                    processTextRecord(parts);
                } else if (type == 'E') {
                    executeAtAddress(findFirstUsedAddress());
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao abrir ou ler o arquivo: " + e.getMessage());
        }
    }

    private void processTextRecord(String[] parts) {
        int address = Integer.parseInt(parts[1], 16);
        String code = parts[3]; 
        moveToMemory(memoria, address, code);
    }
    
    public int findNextFreeMemoryIndex() {
        for (int i = 0; i < memoria.memoria.size(); i++) {
            Palavramem palavra = memoria.memoria.get(i);
            if (palavra.getBytes()[0] == 0 && palavra.getBytes()[1] == 0 && palavra.getBytes()[2] == 0) {
                return i;
            }
        }
        return memoria.memoria.size();
    }

    public void moveToMemory(Memoria memoria, int address, String code) {
        for (int i = 0; i < code.length(); i += 6) {
            int memIndex = findNextFreeMemoryIndex(); // Encontrando o próximo índice livre
            if (memIndex >= memoria.memoria.size()) {
                System.err.println("Erro: Memória insuficiente!");
                return;
            }
            if (i + 6 <= code.length()) {
                byte b1 = (byte) Integer.parseInt(code.substring(i, i + 2), 16);
                byte b2 = (byte) Integer.parseInt(code.substring(i + 2, i + 4), 16);
                byte b3 = (byte) Integer.parseInt(code.substring(i + 4, i + 6), 16);
                memoria.memoria.get(memIndex).setValor(b1, b2, b3);  // Atualizando a memória
            }
        }
    }

    private int findFirstUsedAddress() {
        return 0;
    }
}
