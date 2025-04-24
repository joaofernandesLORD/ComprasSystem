import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

public class ComprasSystem extends JFrame {
    private static final String Itens_FILE = "Itens.txt";
    private Timer timer;
    private JTextField barraPesquisa;
    private JLabel statusPesquisa;
    private JList<String> listaSugestoes;
    private JPopupMenu popupMenu;
    private DefaultListModel<String> listModel;
    private javax.swing.Timer sugestoesTimer;
    private DefaultListModel<String> ItensSelecionadosModel;
    private JList<String> listaItensSelecionados;
    private JScrollPane scrollItensSelecionados;
    private Stack<ItemExcluido> historicoExclusoes = new Stack<>();
    private JButton btnDesfazer;
    private JLabel lblTotalCompra;
    private JLabel lblTotalAtacado;
    private double totalCompra = 0;
    private double totalAtacado = 0;
    private JButton btnFinalizarCompra;

    private static final Color VERMELHO = new Color(112, 19, 19);
    private static final Color VERDE = new Color(16, 145, 1);

    private boolean modoSelecaoAtivo = false;

    public ComprasSystem() {
        super("Lista");
        setSize(900, 700);
        setResizable(false);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setIconImage(new ImageIcon("Resource_Image/icon.png").getImage());
        setLocationRelativeTo(null);

        getContentPane().setBackground(CorFundo.FUNDO_PRINCIPAL);
        setLayout(null);
        configurarBarraPesquisa();
        configurarAreaItensSelecionados();
        setVisible(true);
    }

    private void mostrarPopupInfo(int index, int x, int y) {
        String produtoSelecionado = ItensSelecionadosModel.getElementAt(index);
        String[] infoProduto = buscarInfoItemNoCSV(produtoSelecionado);
        int quantidadeAtual = Collections.frequency(Collections.list(ItensSelecionadosModel.elements()),
                produtoSelecionado);
        int quantidadeEstoque = Integer.parseInt(infoProduto[1]);

        JPopupMenu popupInfo = new JPopupMenu();
        popupInfo.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 1));

        JPanel painelInfo = new JPanel(new GridLayout(0, 1, 5, 5));
        painelInfo.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        painelInfo.setBackground(Color.WHITE);

        JLabel titulo = new JLabel(produtoSelecionado);
        titulo.setFont(new Font("Arial", Font.BOLD, 14));
        titulo.setHorizontalAlignment(JLabel.CENTER);
        painelInfo.add(titulo);

        painelInfo.add(new JSeparator(JSeparator.HORIZONTAL));

        adicionarLinhaInfo(painelInfo, "Quantidade no estoque:", infoProduto[1]);
        adicionarLinhaInfo(painelInfo, "Quantidade na lista:", String.valueOf(quantidadeAtual));
        adicionarLinhaInfo(painelInfo, "Preço unitário:", "R$ " + infoProduto[2]);
        adicionarLinhaInfo(painelInfo, "Preço atacado:", "R$ " + infoProduto[3]);
        adicionarLinhaInfo(painelInfo, "Setor:", infoProduto[4]);

        popupInfo.add(painelInfo);
        popupInfo.show(listaItensSelecionados, x, y + 20);
    }

    // Método auxiliar para adicionar linhas de informação formatadas
    private void adicionarLinhaInfo(JPanel painel, String label, String valor) {
        JPanel linha = new JPanel(new BorderLayout());
        linha.setBackground(Color.WHITE);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Arial", Font.BOLD, 12));

        JLabel vlr = new JLabel(valor);
        vlr.setFont(new Font("Arial", Font.PLAIN, 12));
        vlr.setHorizontalAlignment(JLabel.RIGHT);

        linha.add(lbl, BorderLayout.WEST);
        linha.add(vlr, BorderLayout.CENTER);
        painel.add(linha);
    }

    private void atualizarTotais() {
        totalCompra = 0;
        totalAtacado = 0;

        // Conta a frequência de cada item
        Map<String, Integer> contagemItens = new HashMap<>();
        for (int i = 0; i < ItensSelecionadosModel.size(); i++) {
            String produto = ItensSelecionadosModel.getElementAt(i);
            contagemItens.put(produto, contagemItens.getOrDefault(produto, 0) + 1);
        }

        // Calcula totais considerando as quantidades
        for (Map.Entry<String, Integer> entry : contagemItens.entrySet()) {
            String produto = entry.getKey();
            int quantidade = entry.getValue();
            String[] info = buscarInfoItemNoCSV(produto);

            try {
                double preco = Double.parseDouble(info[2]);
                double precoAtacado = Double.parseDouble(info[3]);

                totalCompra += preco * quantidade;
                totalAtacado += precoAtacado * quantidade;
            } catch (NumberFormatException e) {
                System.err.println("Erro ao converter preço para: " + produto);
            }
        }

        lblTotalCompra.setText(String.format("Total: R$ %.2f", totalCompra));
        lblTotalAtacado.setText(String.format("Atacado: R$ %.2f", totalAtacado));
    }

    private static class ItemListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (isSelected) {
                c.setBackground(new Color(184, 207, 229)); // Azul claro para seleção
                c.setForeground(Color.BLACK);
            } else {
                c.setBackground(Color.WHITE);
                c.setForeground(Color.BLACK);
            }

            return c;
        }
    }

    private class ItemExcluido {
        String nome;
        int indice;

        public ItemExcluido(String nome, int indice) {
            this.nome = nome;
            this.indice = indice;
        }
    }

    private String[] buscarInfoItemNoCSV(String itemNome) {
        String[] info = { itemNome, "0", "0.00", "0.00", "S.I" }; // Nome, Quantidade, Preço, PreçoAtacado, Setor

        try (BufferedReader reader = new BufferedReader(new FileReader("Itens.csv"))) {
            String linha;
            reader.readLine(); // Pula cabeçalho

            while ((linha = reader.readLine()) != null) {
                String[] dados = linha.split(",");
                if (dados.length >= 1 && dados[0].trim().equalsIgnoreCase(itemNome.trim())) {
                    info[0] = dados[0].trim();
                    info[1] = dados.length > 1 ? dados[1].trim() : "0";
                    info[2] = dados.length > 2 ? dados[2].trim() : "0.00";
                    info[3] = dados.length > 3 ? dados[3].trim() : "0.00";
                    info[4] = dados.length > 4 ? dados[4].trim() : "S.I";
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler arquivo Itens.csv: " + e.getMessage());
        }
        return info;
    }

    private void configurarAreaItensSelecionados() {

        int larguraBarra = 800;
        int xPos = (getWidth() - larguraBarra - 30) / 2;
        int yPos = 180;
        int alturaArea = 390;

        // Label "Compras"
        JLabel lblItensSelecionados = new JLabel("Compras:");
        lblItensSelecionados.setFont(new Font("Arial", Font.BOLD, 12));
        lblItensSelecionados.setBounds(xPos, yPos - 30, larguraBarra + 40, 20);
        add(lblItensSelecionados);

        // Painel principal que contém todos os elementos
        JPanel painelItens = new JPanel(null);
        painelItens.setBounds(xPos, yPos, larguraBarra - 30, alturaArea + 20);

        lblTotalCompra = new JLabel("Total: R$ 0,00");
        lblTotalCompra.setFont(new Font("Arial", Font.BOLD, 12));
        lblTotalCompra.setBounds(larguraBarra - 200, 5, 120, 20); // Canto superior direito

        lblTotalAtacado = new JLabel("Atacado: R$ 0,00");
        lblTotalAtacado.setFont(new Font("Arial", Font.BOLD, 12));
        lblTotalAtacado.setBounds(larguraBarra - 200, 30, 120, 20); // Ao lado do total normal

        painelItens.add(lblTotalCompra);
        painelItens.add(lblTotalAtacado);

        // Borda cinza escuro (2px) com espaçamento interno
        painelItens.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CorFundo.FUNDO_TERCEIRO, 2),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        add(painelItens);

        ItensSelecionadosModel = new DefaultListModel<>();
        listaItensSelecionados = new JList<>(ItensSelecionadosModel) {
            @Override
            public Dimension getPreferredScrollableViewportSize() {
                int height = Math.min(getModel().getSize() * 20, alturaArea - 40);
                return new Dimension(larguraBarra + 10, height);
            }
        };

        // Botão Selecionar
        JButton btnSelecionar = new JButton("Selecionar");
        btnSelecionar.setBounds(5, 5, 90, 25);
        btnSelecionar.setBackground(VERMELHO); // Cor inicial (inativo)
        btnSelecionar.setForeground(Color.BLACK);
        btnSelecionar.setEnabled(!ItensSelecionadosModel.isEmpty()); // Habilitado se a lista não estiver vazia
        painelItens.add(btnSelecionar);

        // Botão Finalizar Compra
        btnFinalizarCompra = new JButton("Finalizar");
        btnFinalizarCompra.setBounds(180, 5, 80, 25);
        btnFinalizarCompra.setBackground(new Color(0, 150, 0)); // Verde
        btnFinalizarCompra.setForeground(Color.WHITE);
        btnFinalizarCompra.setEnabled(!ItensSelecionadosModel.isEmpty());
        painelItens.add(btnFinalizarCompra);

        btnFinalizarCompra.addActionListener(e -> finalizarCompra());

        btnSelecionar.addActionListener(e -> {
            modoSelecaoAtivo = !modoSelecaoAtivo;

            if (modoSelecaoAtivo) {
                btnSelecionar.setBackground(VERDE);
                listaItensSelecionados.requestFocusInWindow();

                // Usa cores mais suaves para a seleção
                listaItensSelecionados.setSelectionBackground(new Color(184, 207, 229));
                listaItensSelecionados.setSelectionForeground(Color.BLACK);
            } else {
                btnSelecionar.setBackground(VERMELHO);
                listaItensSelecionados.clearSelection();
                // Restaura cores padrão
                listaItensSelecionados.setSelectionBackground(new Color(184, 207, 229));
                listaItensSelecionados.setSelectionForeground(Color.BLACK);
            }
            listaItensSelecionados.repaint();
        });

        listaItensSelecionados.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (modoSelecaoAtivo) {
                    // Mantém o comportamento de seleção se estiver no modo de seleção
                } else {
                    int index = listaItensSelecionados.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        mostrarPopupInfo(index, e.getX(), e.getY());
                    }
                }
            }
        });

        // Botão Excluir (ao lado do Selecionar)
        JButton btnExcluir = new JButton("Excluir");
        btnExcluir.setBounds(5, 30, 90, 25);
        btnExcluir.setBackground(CorFundo.FUNDO_SECUNDARIO);
        btnExcluir.setForeground(Color.BLACK);
        btnExcluir.setEnabled(false);

        btnExcluir.addActionListener(e -> {
            if (modoSelecaoAtivo && listaItensSelecionados.getSelectedIndex() != -1) {
                int selectedIndex = listaItensSelecionados.getSelectedIndex();
                String ItemExcluido = ItensSelecionadosModel.getElementAt(selectedIndex);

                historicoExclusoes.push(new ItemExcluido(ItemExcluido, selectedIndex));
                ItensSelecionadosModel.remove(selectedIndex);
                atualizarTotais(); // Atualiza os totais quando remove item

                btnDesfazer.setEnabled(!historicoExclusoes.isEmpty());
                statusPesquisa.setText("Item '" + ItemExcluido + "' removido");
                statusPesquisa.setForeground(VERMELHO);
                timerBarraPesquisa();
            }
        });
        painelItens.add(btnExcluir);

        // Botão Desfazer (modificado para usar o histórico)
        btnDesfazer = new JButton("Desfazer");
        btnDesfazer.setBounds(95, 30, 80, 25);
        btnDesfazer.setBackground(CorFundo.FUNDO_SECUNDARIO);
        btnDesfazer.setForeground(Color.BLACK);
        btnDesfazer.setEnabled(false);

        btnDesfazer.addActionListener(e -> {
            if (!historicoExclusoes.isEmpty()) {
                ItemExcluido ultimoExcluido = historicoExclusoes.pop();
                ItensSelecionadosModel.add(ultimoExcluido.indice, ultimoExcluido.nome);
                atualizarTotais(); // Atualiza os totais quando desfaz exclusão

                listaItensSelecionados.setSelectedIndex(ultimoExcluido.indice);
                btnDesfazer.setEnabled(!historicoExclusoes.isEmpty());

                statusPesquisa.setText("Desfeito - '" + ultimoExcluido.nome + "' readicionado");
                statusPesquisa.setForeground(VERDE);
                timerBarraPesquisa();
            }
        });
        painelItens.add(btnDesfazer);

        // Botão Limpar Tudo
        JButton btnLimpar = new JButton("Limpar");
        btnLimpar.setBounds(95, 5, 80, 25);
        btnLimpar.setBackground(CorFundo.FUNDO_SECUNDARIO);
        btnLimpar.setForeground(Color.BLACK);
        btnLimpar.setEnabled(!ItensSelecionadosModel.isEmpty());

        btnLimpar.addActionListener(e -> {
            if (!modoSelecaoAtivo) {
                historicoExclusoes.clear();
                ItensSelecionadosModel.clear();
                atualizarTotais(); // Atualiza os totais quando limpa a lista

                btnDesfazer.setEnabled(false);
                statusPesquisa.setText("Lista de Itens foi completamente limpa");
                statusPesquisa.setForeground(VERMELHO);
                timerBarraPesquisa();
            }
        });
        painelItens.add(btnLimpar);

        // listaItensSelecionados:
        listaItensSelecionados.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaItensSelecionados.setFont(new Font("Arial", Font.PLAIN, 12));
        listaItensSelecionados.setFixedCellHeight(20);
        listaItensSelecionados.setVisibleRowCount(0);

        // Configurações específicas para as cores de seleção
        listaItensSelecionados.setSelectionBackground(new Color(184, 207, 229));
        listaItensSelecionados.setBackground(Color.WHITE);

        // Model e lista para os Itens selecionados
        ItensSelecionadosModel = new DefaultListModel<>();
        listaItensSelecionados = new JList<>(ItensSelecionadosModel) {
            @Override
            public Dimension getPreferredScrollableViewportSize() {
                int height = Math.min(getModel().getSize() * 20, alturaArea - 40);
                return new Dimension(larguraBarra + 10, height);
            }
        };

        // Configurações da lista
        listaItensSelecionados.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaItensSelecionados.setFont(new Font("Arial", Font.PLAIN, 12));
        listaItensSelecionados.setFixedCellHeight(20);
        listaItensSelecionados.setVisibleRowCount(0);

        listaItensSelecionados.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!modoSelecaoAtivo) {
                    return; // Só funciona no modo de seleção/movimentação
                }

                int index = listaItensSelecionados.getSelectedIndex();
                if (index == -1) {
                    return; // Nada selecionado
                }

                int newIndex = -1; // Guarda o novo índice após mover

                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    if (index > 0) { // Verifica se não está no topo
                        // Move o item para cima
                        String item = ItensSelecionadosModel.remove(index);
                        ItensSelecionadosModel.add(index - 1, item);
                        newIndex = index - 1;
                    }
                    e.consume(); // Consome o evento para evitar scroll padrão da lista
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (index < ItensSelecionadosModel.getSize() - 1) { // Verifica se não está na base
                        // Move o item para baixo
                        String item = ItensSelecionadosModel.remove(index);
                        ItensSelecionadosModel.add(index + 1, item);
                        newIndex = index + 1;
                    }
                    e.consume(); // Consome o evento
                }

                // Se houve movimentação, atualiza a seleção e garante visibilidade
                if (newIndex != -1) {
                    listaItensSelecionados.setSelectedIndex(newIndex);
                    listaItensSelecionados.ensureIndexIsVisible(newIndex);
                }
            }
        });

        // Listener para popup ao clicar
        listaItensSelecionados.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (modoSelecaoAtivo) {
                } else if (e.getClickCount() == 2) {
                    int index = listaItensSelecionados.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        String Itenselecionado = ItensSelecionadosModel.getElementAt(index);
                        String[] infoItem = buscarInfoItemNoCSV(Itenselecionado);

                        JPopupMenu popupInfo = new JPopupMenu();

                        // Formata as informações para melhor visualização
                        JMenuItem ItemQuant = new JMenuItem("Quantidade: " + infoItem[1]);
                        JMenuItem ItemPreço = new JMenuItem(" Preço: " + "R$ " + infoItem[2]);
                        JMenuItem ItemAtacado = new JMenuItem("PreçoAtacado: " + "R$ " + infoItem[3]);
                        JMenuItem Itensetor = new JMenuItem("Setor: " + infoItem[4]);

                        ItemQuant.setEnabled(false);
                        ItemPreço.setEnabled(false);
                        ItemAtacado.setEnabled(false);
                        Itensetor.setEnabled(false);

                        popupInfo.add(ItemQuant);
                        popupInfo.add(ItemPreço);
                        popupInfo.add(ItemAtacado);
                        popupInfo.add(Itensetor);

                        popupInfo.show(listaItensSelecionados, e.getX(), e.getY() + 20);
                    }
                }
            }
        });

        // ScrollPane para a lista
        scrollItensSelecionados = new JScrollPane(listaItensSelecionados);
        scrollItensSelecionados.setBounds(5, 55, larguraBarra - 40, alturaArea - 40);
        scrollItensSelecionados.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollItensSelecionados.getVerticalScrollBar().setUnitIncrement(16);
        painelItens.add(scrollItensSelecionados);

        // Listener para atualização automática
        ItensSelecionadosModel.addListDataListener(new ListDataListener() {
            private void atualizarEstadoGeral() {
                boolean listaVazia = ItensSelecionadosModel.isEmpty();
                btnFinalizarCompra.setEnabled(!listaVazia);
                boolean Itenselecionado = listaItensSelecionados.getSelectedIndex() != -1;

                btnSelecionar.setEnabled(!listaVazia);
                btnExcluir.setEnabled(!listaVazia && modoSelecaoAtivo && Itenselecionado);
                btnLimpar.setEnabled(!listaVazia);
                btnDesfazer.setEnabled(!historicoExclusoes.isEmpty());

                if (listaVazia) {
                    // Se a lista ficar vazia, desativa o modo de seleção completamente
                    historicoExclusoes.clear(); // Limpa histórico se lista ficar vazia

                    btnDesfazer.setEnabled(false);
                    modoSelecaoAtivo = false;
                    btnSelecionar.setBackground(VERMELHO);
                    listaItensSelecionados.clearSelection();
                    listaItensSelecionados.setSelectionBackground(UIManager.getColor("List.selectionBackground"));
                    listaItensSelecionados.setSelectionForeground(UIManager.getColor("List.selectionForeground"));
                    listaItensSelecionados.repaint();
                }

                // Atualiza a visualização da lista
                SwingUtilities.invokeLater(() -> {
                    listaItensSelecionados.revalidate();
                    listaItensSelecionados.repaint();
                    if (!listaVazia) {
                        listaItensSelecionados.ensureIndexIsVisible(ItensSelecionadosModel.size() - 1);
                    }
                });
            }

            @Override
            public void intervalAdded(ListDataEvent e) {
                atualizarEstadoGeral();
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                atualizarEstadoGeral();
                if (ItensSelecionadosModel.isEmpty()) {
                    listaItensSelecionados.clearSelection(); // Limpa seleção visual
                } else if (e.getIndex0() < ItensSelecionadosModel.getSize()) {
                    listaItensSelecionados.setSelectedIndex(e.getIndex0());
                } else if (e.getIndex0() > 0) {
                    listaItensSelecionados.setSelectedIndex(e.getIndex0() - 1);
                }

            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                atualizarEstadoGeral(); // Garante consistência
            }
        });

        listaItensSelecionados.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean Itenselecionado = listaItensSelecionados.getSelectedIndex() != -1;
                btnExcluir.setEnabled(modoSelecaoAtivo && Itenselecionado);
            }
        });

    }

    // Adicione este método para finalizar a compra
    private void finalizarCompra() {
        if (ItensSelecionadosModel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nenhum item selecionado para finalizar compra!", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Cria a janela de resumo
        JFrame resumoCompra = new JFrame("Resumo da Compra");
        resumoCompra.setSize(400, 500);
        resumoCompra.setLayout(new BorderLayout());
        resumoCompra.setLocationRelativeTo(this);

        // Painel para os itens
        JPanel painelItens = new JPanel();
        painelItens.setLayout(new BoxLayout(painelItens, BoxLayout.Y_AXIS));
        painelItens.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Adiciona o título
        JLabel titulo = new JLabel("Compras");
        titulo.setFont(new Font("Arial", Font.BOLD, 14));
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        painelItens.add(titulo);
        painelItens.add(Box.createVerticalStrut(10));

        // Calcula totais e conta itens
        Map<String, Integer> itensContados = new HashMap<>();
        double totalFinal = 0;

        for (int i = 0; i < ItensSelecionadosModel.size(); i++) {
            String produto = ItensSelecionadosModel.getElementAt(i);
            itensContados.put(produto, itensContados.getOrDefault(produto, 0) + 1);
        }

        // Adiciona os itens ao resumo
        for (Map.Entry<String, Integer> entry : itensContados.entrySet()) {
            String produto = entry.getKey();
            int quantidade = entry.getValue();
            String[] info = buscarInfoItemNoCSV(produto);

            double preco = Double.parseDouble(info[2]);
            double subtotal = preco * quantidade;
            totalFinal += subtotal;

            JPanel linhaItem = new JPanel(new BorderLayout());
            linhaItem.setMaximumSize(new Dimension(Short.MAX_VALUE, 30));

            JLabel lblNome = new JLabel(String.format("%s x %d", produto, quantidade));
            JLabel lblPreco = new JLabel(String.format("R$ %.2f", subtotal));
            lblPreco.setHorizontalAlignment(JLabel.RIGHT);

            linhaItem.add(lblNome, BorderLayout.WEST);
            linhaItem.add(lblPreco, BorderLayout.EAST);
            painelItens.add(linhaItem);
            painelItens.add(Box.createVerticalStrut(5));
        }

        // Adiciona o total
        painelItens.add(Box.createVerticalStrut(10));
        JSeparator separador = new JSeparator();
        separador.setAlignmentX(Component.CENTER_ALIGNMENT);
        painelItens.add(separador);

        JPanel painelTotal = new JPanel(new BorderLayout());
        painelTotal.setMaximumSize(new Dimension(Short.MAX_VALUE, 40));

        JLabel lblTotal = new JLabel("Total:");
        lblTotal.setFont(new Font("Arial", Font.BOLD, 14));

        JLabel lblValorTotal = new JLabel(String.format("R$ %.2f", totalFinal));
        lblValorTotal.setFont(new Font("Arial", Font.BOLD, 14));
        lblValorTotal.setHorizontalAlignment(JLabel.RIGHT);

        painelTotal.add(lblTotal, BorderLayout.WEST);
        painelTotal.add(lblValorTotal, BorderLayout.EAST);
        painelItens.add(painelTotal);

        // Botão para confirmar
        JButton btnConfirmar = new JButton("Confirmar Compra");
        btnConfirmar.addActionListener(e -> {
            atualizarEstoque(itensContados);
            resumoCompra.dispose();
            ItensSelecionadosModel.clear();
            atualizarTotais();
        });

        // Configura a janela
        JScrollPane scroll = new JScrollPane(painelItens);
        resumoCompra.add(scroll, BorderLayout.CENTER);
        resumoCompra.add(btnConfirmar, BorderLayout.SOUTH);
        resumoCompra.setVisible(true);
    }

    // Método para atualizar o estoque no CSV
    private void atualizarEstoque(Map<String, Integer> itensComprados) {
        List<String> linhas = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader("Itens.csv"))) {
            // Lê o cabeçalho
            String linha = reader.readLine();
            linhas.add(linha);

            // Processa cada linha
            while ((linha = reader.readLine()) != null) {
                String[] dados = linha.split(",");
                if (dados.length > 0) {
                    String produto = dados[0].trim();
                    if (itensComprados.containsKey(produto)) {
                        int quantidadeAtual = Integer.parseInt(dados[1].trim());
                        int quantidadeComprada = itensComprados.get(produto);
                        dados[1] = String.valueOf(quantidadeAtual - quantidadeComprada);
                        linha = String.join(",", dados);
                    }
                }
                linhas.add(linha);
            }

            // Escreve de volta no arquivo
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("Itens.csv"))) {
                for (String l : linhas) {
                    writer.write(l);
                    writer.newLine();
                }
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Erro ao atualizar estoque!", "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void configurarBarraPesquisa() {
        int larguraBarra = 600;
        int alturaBarra = 25;
        int xPos = (getWidth() - larguraBarra - 30) / 2;
        int yPos = 10;

        // Barra de pesquisa
        barraPesquisa = new JTextField();
        barraPesquisa.setBounds(xPos - 10, yPos, larguraBarra, alturaBarra);
        barraPesquisa.setFont(new Font("Arial", Font.PLAIN, 12));
        barraPesquisa.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(0, 5, 0, 5)));

        // Configura o timer para atualizar sugestões com delay de 200ms
        sugestoesTimer = new javax.swing.Timer(200, e -> atualizarSugestoes());
        sugestoesTimer.setRepeats(false);

        // Adiciona listeners para a barra de pesquisa
        barraPesquisa.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (popupMenu.isVisible() && listaSugestoes.getSelectedIndex() >= 0) {
                        String selected = listaSugestoes.getSelectedValue();
                        barraPesquisa.setText(selected);
                        selecionarItem(selected);
                        popupMenu.setVisible(false);
                    } else {
                        pesquisarItem(null);
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN) {
                    if (popupMenu.isVisible() && listModel.size() > 0) {
                        listaSugestoes.requestFocusInWindow();
                        if (e.getKeyCode() == KeyEvent.VK_DOWN && listaSugestoes.getSelectedIndex() < 0) {
                            listaSugestoes.setSelectedIndex(0);
                        }
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    popupMenu.setVisible(false);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() != KeyEvent.VK_ENTER &&
                        e.getKeyCode() != KeyEvent.VK_UP &&
                        e.getKeyCode() != KeyEvent.VK_DOWN &&
                        e.getKeyCode() != KeyEvent.VK_ESCAPE &&
                        !e.isActionKey() && e.getKeyCode() != KeyEvent.VK_SHIFT &&
                        e.getKeyCode() != KeyEvent.VK_CONTROL && e.getKeyCode() != KeyEvent.VK_ALT) {
                    sugestoesTimer.restart();
                }
            }
        });

        barraPesquisa.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (!barraPesquisa.getText().isEmpty()) {
                    atualizarSugestoes();
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (!e.isTemporary() && !popupMenu.isFocusOwner() && !listaSugestoes.isFocusOwner()) {
                    SwingUtilities.invokeLater(() -> {
                        if (!barraPesquisa.isFocusOwner() && !popupMenu.isFocusOwner()
                                && !listaSugestoes.isFocusOwner()) {
                            popupMenu.setVisible(false);
                        }
                    });
                }
            }
        });

        add(barraPesquisa);

        // Botão de pesquisa
        JButton btnPesquisar = new JButton();
        btnPesquisar.setBounds(xPos + larguraBarra + 5, yPos, 25, alturaBarra);
        ImageIcon lupaIcon = new ImageIcon("Resource_Image/lupa.png");
        Image img = lupaIcon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
        btnPesquisar.setIcon(new ImageIcon(img));
        btnPesquisar.setMargin(new Insets(0, 0, 0, 0));
        btnPesquisar.setBorderPainted(false);
        btnPesquisar.setContentAreaFilled(false);
        btnPesquisar.setFocusPainted(false);
        btnPesquisar.addActionListener(e -> {
            if (popupMenu.isVisible() && listaSugestoes.getSelectedIndex() >= 0) {
                String selected = listaSugestoes.getSelectedValue();
                barraPesquisa.setText(selected);
                selecionarItem(selected);
                popupMenu.setVisible(false);
            } else {
                pesquisarItem(null);
            }
        });
        add(btnPesquisar);

        // Label de status da pesquisa
        statusPesquisa = new JLabel(" ");
        statusPesquisa.setFont(new Font("Arial", Font.BOLD, 10));
        statusPesquisa.setBounds(xPos, yPos + 20, larguraBarra + 30, 20);
        add(statusPesquisa);

        // Configuração da lista suspensa
        listModel = new DefaultListModel<>();
        listaSugestoes = new JList<>(listModel);
        listaSugestoes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaSugestoes.setVisibleRowCount(5);

        listaSugestoes.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selected = listaSugestoes.getSelectedValue();
                    if (selected != null) {
                        barraPesquisa.setText(selected);
                        selecionarItem(selected);
                        popupMenu.setVisible(false);
                        barraPesquisa.requestFocusInWindow();
                    }
                }
            }
        });

        listaSugestoes.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String selected = listaSugestoes.getSelectedValue();
                    if (selected != null) {
                        barraPesquisa.setText(selected);
                        selecionarItem(selected);
                        popupMenu.setVisible(false);
                        barraPesquisa.requestFocusInWindow();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    popupMenu.setVisible(false);
                    barraPesquisa.requestFocusInWindow();
                }
            }
        });

        listaSugestoes.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (!e.isTemporary() && !barraPesquisa.isFocusOwner()) {
                    popupMenu.setVisible(false);
                }
            }
        });

        popupMenu = new JPopupMenu();
        popupMenu.add(new JScrollPane(listaSugestoes));
        popupMenu.setPreferredSize(new Dimension(larguraBarra, 120));
        popupMenu.setFocusable(false);
        popupMenu.setPreferredSize(new Dimension(larguraBarra, 140));
    }

    private void selecionarItem(String item) {
        if (item.trim().isEmpty())
            return;

        String[] info = buscarInfoItemNoCSV(item);
        int quantidadeEstoque = Integer.parseInt(info[1]);
        int quantidadeAtual = Collections.frequency(Collections.list(ItensSelecionadosModel.elements()), item);

        if (quantidadeAtual < quantidadeEstoque) {
            ItensSelecionadosModel.addElement(item);
            atualizarTotais();

            statusPesquisa
                    .setText("'" + item + "' adicionado (" + (quantidadeAtual + 1) + "/" + quantidadeEstoque + ")");
            statusPesquisa.setForeground(VERDE);
        } else {
            statusPesquisa.setText("Limite do estoque atingido para '" + item + "' (" + quantidadeEstoque + ")");
            statusPesquisa.setForeground(VERMELHO);
        }
        timerBarraPesquisa();
        barraPesquisa.setText("");
    }

    private void atualizarSugestoes() {
        String termo = barraPesquisa.getText().trim();

        try {
            List<String> sugestoes = buscarNoArquivo(termo);
            listModel.clear();

            if (termo.isEmpty() || sugestoes.isEmpty()) {
                popupMenu.setVisible(false);
            } else {
                for (String item : sugestoes) {
                    listModel.addElement(item);
                }
                if (!popupMenu.isVisible()) {
                    popupMenu.show(barraPesquisa, 0, barraPesquisa.getHeight());
                    listaSugestoes.setSelectedIndex(-1);
                }
            }
        } catch (IOException ex) {
            System.err.println("Erro ao ler arquivo de Itens para sugestões: " + ex.getMessage());
            popupMenu.setVisible(false);
        }
    }

    private void pesquisarItem(ActionEvent e) {
        String termo = barraPesquisa.getText().trim();
        popupMenu.setVisible(false);

        if (termo.isEmpty()) {
            statusPesquisa.setText("Digite um termo para pesquisa");
            statusPesquisa.setForeground(VERMELHO);
            return;
        }

        try {
            List<String> resultados = buscarNoArquivo(termo);
            if (resultados.isEmpty()) {
                statusPesquisa.setText("Nenhum Item encontrado com: '" + termo + "'");
                statusPesquisa.setForeground(VERMELHO);
            } else {
                statusPesquisa.setText(" "); // Limpa a mensagem
                atualizarSugestoes();
            }
        } catch (IOException ex) {
            statusPesquisa.setText("Erro ao ler o arquivo de Itens");
            statusPesquisa.setForeground(VERMELHO);
            ex.printStackTrace();
        }

        timerBarraPesquisa();
    }

    private List<String> buscarNoArquivo(String termo) throws IOException {
        List<String> resultados = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(Itens_FILE))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                if (linha.toLowerCase().contains(termo.toLowerCase())) {
                    resultados.add(linha);
                }
            }
        }
        return resultados;
    }

    private Timer timerBarraPesquisa;

    private void timerBarraPesquisa() {
        if (timer != null) {
            timer.cancel();
        }

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    statusPesquisa.setText(" ");
                });
            }
        }, 3000); // Aumentei para 3 segundos para melhor leitura
    }

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // Se Nimbus não estiver disponível, usa o padrão do sistema
            System.err.println("Nimbus Look and Feel não encontrado, usando padrão.");
        }

        SwingUtilities.invokeLater(() -> {
            ComprasSystem tela = new ComprasSystem();
            tela.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    // Limpa a lista de Itens selecionados ao fechar
                    // Garante que o modelo não é nulo antes de tentar limpar
                    if (tela.ItensSelecionadosModel != null) {
                        tela.ItensSelecionadosModel.clear();
                    }
                    // Cancela timers pendentes ao fechar a janela para liberar recursos
                    if (tela.timer != null) {
                        tela.timer.cancel();
                    }
                    if (tela.timerBarraPesquisa != null) {
                        tela.timerBarraPesquisa.cancel();
                    }
                    if (tela.sugestoesTimer != null && tela.sugestoesTimer.isRunning()) {
                        tela.sugestoesTimer.stop();
                    }
                    System.out.println("Janela fechada, timers cancelados.");
                    // O dispose() padrão já está definido em setDefaultCloseOperation
                }
            });
        });
    }
}