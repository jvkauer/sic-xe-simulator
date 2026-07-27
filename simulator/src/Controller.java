import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.Button;
import Mem.Memoria;
import Mem.Palavramem;
import Regs.Registradores;
import Regs.Registrador;
import javafx.stage.FileChooser;
import java.io.*;

/**
 * Controlador de Interface Gráfica (JavaFX Controller) do Simulador SIC/XE.
 * Gerencia os componentes visuais, inspeção de memória/registradores e ciclo de execução (Run/Step/Reset).
 */
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import Montador.Assembler;
import Montador.Macros.MacroProcessor;
import javafx.stage.Stage;

public class Controller {

    // Elementos da tabela de registradores
    @FXML
    private TableView<Registrador> TabelaReg;

    // Elementos da tabela de memória
    @FXML
    private TableView<Palavramem> TabelaMem;

    @FXML
    private TableColumn<Palavramem, String> memoryIndexColumn;

    @FXML
    private TextField arquivoInput;

    @FXML
    private Button carregarArquivoBtn;

    @FXML
    private Button montarBtn;

    @FXML
    private TextArea fonteTextArea;

    @FXML
    private TextArea saidaTextArea;

    @FXML
    private TableColumn<Palavramem, String> memoryValueColumn;

    @FXML
    private TextArea masmaprgTextArea; //exibir MASMAPRG.ASM

    // Dados das tabelas
    private ObservableList<Registrador> registradores = FXCollections.observableArrayList();
    private ObservableList<Palavramem> memoria = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
    
        // Configurar tabela de memória
        memoryIndexColumn.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(memoria.indexOf(data.getValue())))
        );
        memoryValueColumn.setCellValueFactory(data ->
                new SimpleStringProperty(formatMemoryValue(data.getValue()))
        );
        TabelaMem.setItems(memoria);
    }

    // Método para atualizar os registradores
    public void updateRegistradores(Registradores regs) {
        registradores.clear();
        Registrador[] regsArray = regs.getAllRegistradores();
        registradores.addAll(regsArray);
    }

    // Método para atualizar a memória
    public void updateMemoria(Memoria mem) {
        memoria.clear();
        memoria.addAll(mem.memoria);
    }

    // Formatar valores da memória para exibição
    private String formatMemoryValue(Palavramem palavra) {
        StringBuilder hexValue = new StringBuilder();
        for (byte b : palavra.getBytes()) {
            hexValue.append(String.format("%02X ", b));
        }
        return hexValue.toString().trim();
    }


    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }
    /////
    @FXML
    private void carregarArquivo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arquivos ASM", "*.asm"));
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            arquivoInput.setText(file.getAbsolutePath());
            lerArquivo(file);
        }
    }
    /////
    private void lerArquivo(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder conteudo = new StringBuilder();
            String linha;
            while ((linha = br.readLine()) != null) {
                conteudo.append(linha).append("\n");
            }
            fonteTextArea.setText(conteudo.toString());  // Atualiza a fonteTextArea com o conteúdo
        } catch (IOException e) {
            fonteTextArea.setText("Erro ao ler o arquivo!");
        }
    }
    /////
    @FXML
    private void limparCampos() {
        arquivoInput.clear();
        fonteTextArea.clear();
        saidaTextArea.clear();
        masmaprgTextArea.clear();
    }
    ////
    private void lerArquivoMASMAPRG(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder conteudo = new StringBuilder();
            String linha;
            while ((linha = br.readLine()) != null) {
                conteudo.append(linha).append("\n");
            }
            masmaprgTextArea.setText(conteudo.toString());  // Atualiza
        } catch (IOException e) {
            masmaprgTextArea.setText("Erro ao ler o arquivo!");
        }
    }
    // Método para atualizar a exibição do MASMAPRG.ASM
    private void atualizarCodigoExpandido() {
        File arquivoExpandido = new File("simulator/src/utils/MASMAPRG.asm");
        if (arquivoExpandido.exists()) {
            lerArquivoMASMAPRG(arquivoExpandido);
        } else {
            masmaprgTextArea.setText("Erro: Arquivo MASMAPRG.ASM não encontrado!");
        }
    }

    /////
    @FXML
    private void executarMontador() {
        String arquivoEntrada = arquivoInput.getText();
        String arquivoSaida = "simulator/src/utils/object_code.txt";

        if(arquivoEntrada.isEmpty()){
            saidaTextArea.setText("Erro: Nenhum arquivo .asm selecionado!");
            return;
        }

        try {
            File tempFile = new File("simulator/src/utils/MASMAPRG.asm");
            Files.copy(Paths.get(arquivoEntrada), tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // Exibir o conteúdo do arquivo de entrada na fonteTextArea
            lerArquivo(tempFile);
            try{
                List<String> expandedCode = MacroProcessor.processMacros(tempFile.getAbsolutePath());
                MacroProcessor.writeToFile(expandedCode, "simulator/src/utils/MASMAPRG.asm");
                System.out.println("Expansão de macros concluída com sucesso!");

                // atualizar interface com o codigo expandodo
                atualizarCodigoExpandido();
            }catch (IOException e){
                System.err.println("Erro ao processar macros: " + e.getMessage());
                return; // Se houver erro, encerramos a execução
            }

            // Executar a montagem
            Assembler.main(new String[]{});

            // Ler o arquivo gerado pelo montador e exibir na interface
            File arquivoSaidaFile = new File(arquivoSaida);
            if (!arquivoSaidaFile.exists()) {
                saidaTextArea.setText("Erro: Arquivo de saída não encontrado!");
                return;
            }

            try (BufferedReader br = new BufferedReader(new FileReader(arquivoSaidaFile))) {
                StringBuilder conteudo = new StringBuilder();
                String linha;
                while ((linha = br.readLine()) != null) {
                    conteudo.append(linha).append("\n");
                }
                saidaTextArea.setText(conteudo.toString());
            }

        } catch (Exception e) {
            saidaTextArea.setText("Erro ao executar o montador: " + e.getMessage());
        }
        App.LII();
    }
}
