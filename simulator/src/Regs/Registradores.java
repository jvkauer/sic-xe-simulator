package Regs;

/**
 * Banco de Registradores de Hardware da Arquitetura SIC/XE (A, X, L, B, S, T, F, PC, SW).
 */
public class Registradores {

public Registrador[] registradores;
    
    public Registradores() {
        registradores = new Registrador[8]; 
        for(int i=0; i<8; i++){
            registradores[i]=new Registrador();
        }
    }
    public Registrador getRegistradores(int reg) {
        return registradores[reg];
    }
    public Registrador[] getAllRegistradores() { 
        return registradores;
    }
}
