# 🛒 Sistema de Gerenciamento de Compras em Java

![Java](https://img.shields.io/badge/Java-17+-blue) ![Swing](https://img.shields.io/badge/GUI-Java_Swing-orange) ![CSV](https://img.shields.io/badge/Data-CSV-brightgreen) ![License](https://img.shields.io/badge/License-MIT-green)

## 📋 Visão Geral do Sistema
O Sistema de Gerenciamento de Compras é uma solução desktop desenvolvida em Java Swing que oferece um ambiente intuitivo para controle de estoque e processos de compra. Desenvolvido com foco na praticidade, o sistema utiliza arquivos CSV como base de dados, proporcionando uma alternativa leve e eficiente para armazenamento e manipulação de informações de produtos.

Com um visual moderno graças ao uso do tema Nimbus, a aplicação permite desde a simples consulta de itens até operações complexas como finalização de compras com atualização automática de estoque. A escolha pelo formato CSV traz vantagens significativas na portabilidade dos dados e facilidade de integração com outras ferramentas, enquanto mantém a simplicidade de implementação.

O sistema é ideal para pequenos comércios que necessitam de uma solução prática para gerenciar suas operações de compra sem a complexidade de bancos de dados tradicionais, combinando funcionalidades essenciais com uma experiência de usuário fluida e agradável.

## 🌟 Visão Geral
Sistema desktop para gerenciamento de compras com:
- Controle de estoque via CSV
- Interface moderna com tema Nimbus
- Processo completo de vendas
- Atualização automática de estoque

## 🚀 Funcionalidades
### 📦 Gestão de Produtos
- Cadastro direto no CSV
- Busca instantânea
- Visualização de preços/estoque

### 🛒 Processo de Compra
- Adição múltipla de itens
- Cálculo de totais (normal/atacado)
- Histórico de ações

### 📊 Relatórios
- Resumo da compra
- Detalhes por item
- Atualização automática do CSV

## 🛠️ Tecnologias
| Componente       | Função                          |
|------------------|---------------------------------|
| Java Swing       | Interface gráfica               |
| CSV              | Banco de dados simples          |
| Nimbus L&F       | Aparência moderna               |

## 💾 Estrutura do CSV
```csv
Nome,Quantidade,Preço,PreçoAtacado,Setor
Caneta,150,2.50,1.80,Escritório
Caderno,80,25.90,18.00,Escritório
