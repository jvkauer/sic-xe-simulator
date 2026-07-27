package Mem;

import java.util.ArrayList;

/**
 * Abstração de Memória Principal Simulada (1MB / 1000 palavras) para a Arquitetura SIC/XE.
 * Fornece métodos de leitura e escrita de bytes e palavras de 24 bits.
 */
public class Memoria {
    
    public ArrayList<Palavramem> memoria;

    public Memoria() {
        memoria = new ArrayList<>();
        for (int i = 0; i < 1000; i++) { 
            memoria.add(new Palavramem());
        }
    }

    public void updateMemory(int address, int value) {
        if (address < 0 || address >= memoria.size()) {
            System.err.println("Erro: Endereço de memória inválido " + address);
            return;
        }

        byte b1 = (byte) ((value >> 16) & 0xFF);
        byte b2 = (byte) ((value >> 8) & 0xFF);
        byte b3 = (byte) (value & 0xFF);

        memoria.get(address).setValor(b1, b2, b3);
    }

    public void printMemory(int n) {
        System.out.println("Memória:");
        for (int i = 0; i < n && i < memoria.size(); i++) {
            byte[] bytes = memoria.get(i).getBytes();
            System.out.printf("Endereço %d: %02X %02X %02X%n", i, bytes[0], bytes[1], bytes[2]);
        }
    }
}
