# 📋 Changelog

Todas as mudanças notáveis neste projeto serão documentadas neste arquivo.

O formato é baseado em [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
e este projeto adere ao [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planejado
- Sistema de dungeons em grupo (até 4 jogadores)
- Interface de criação de dungeons in-game
- Integração com PlaceholderAPI
- Dashboard web para administração
- Sistema de achievements e progressão

## [1.0.0] - 2025-01-23

### Adicionado
- **Sistema Core**
  - Integração completa com mcMMO
  - Sistema de mapas do tesouro automáticos
  - Cooldown inteligente por jogador e skill
  - Suporte para múltiplas skills (Mining, Woodcutting, etc.)

- **Sistema de Dungeons**
  - 5 tipos de dungeon únicos com pesos configuráveis
  - Sistema de spawn inteligente para mobs e bosses
  - Integração com WorldEdit para schematics customizados
  - Geração procedural de localizações seguras

- **Sistema de Combate**
  - Waves configuráveis (2-5 ondas por dungeon)
  - Integração com MythicMobs para mobs customizados
  - Sistema de damage tracking para boss fights
  - Ranking dos top 3 jogadores por dano causado

- **Sistema de Recompensas**
  - Loot configurável via comandos
  - Baús temporários com auto-destruição
  - Suporte para plugins de economia
  - Recompensas escaláveis por tipo de dungeon

- **Recursos Técnicos**
  - Sistema de cache otimizado para performance
  - Null safety em todos os métodos críticos
  - Arquitetura modular para fácil manutenção
  - Sistema de backup automático de dados

- **Configuração e Localização**
  - Configuração YAML completa e flexível
  - Suporte para Inglês e Português
  - Sistema de mensagens customizáveis
  - Validação automática de configuração

- **Comandos e Permissões**
  - `/treasure tp` - Teleporte para mundo dos tesouros
  - `/treasure reload` - Recarregar configuração
  - Sistema de permissões granular
  - Bloqueio de comandos no mundo dos tesouros

### Recursos de Segurança
- Validação rigorosa de entrada de dados
- Sistema de cleanup automático de dungeons antigas
- Proteção contra conflitos de localização
- Tratamento robusto de erros

### Integrações
- **mcMMO 2.2.024+** (obrigatório)
- **WorldEdit 7.3.8+** (opcional, para schematics)
- **MythicMobs 5.6.1+** (opcional, para mobs customizados)
- **Multiverse-Core** (opcional, para gerenciamento de mundos)

### Tipos de Dungeon Incluídos
1. **Ancient Mine** (Peso: 30)
   - 3 waves de mobs mineradores
   - Boss: Mining Overlord
   - Loot: Diamantes, Esmeraldas, Dinheiro

2. **Cursed Forest** (Peso: 25)
   - 2 waves de criaturas da floresta
   - Boss: Ancient Treant
   - Loot: Madeira, Maçãs Douradas, Dinheiro

3. **Frozen Cavern** (Peso: 20)
   - 4 waves de criaturas de gelo
   - Boss: Frost King
   - Loot: Gelo, Espada de Diamante, Dinheiro

4. **Desert Tomb** (Peso: 15)
   - 3 waves de guardiões do deserto
   - Boss: Pharaoh King
   - Loot: Ouro, Maçãs Douradas Encantadas, Dinheiro

5. **Volcanic Depths** (Peso: 10)
   - 5 waves de criaturas vulcânicas
   - Boss: Volcano Lord
   - Loot: Netherite, Ancient Debris, Dinheiro

### Arquivos de Configuração
- `config.yml` - Configuração principal do plugin
- `data.yml` - Armazenamento de dados dos jogadores
- `lang/en.yml` - Mensagens em inglês
- `lang/pt.yml` - Mensagens em português

### Requisitos do Sistema
- **Minecraft**: Paper 1.21.4+
- **Java**: 21+
- **RAM**: Mínimo 2GB recomendado
- **Armazenamento**: ~50MB para plugin + schematics

### Notas de Performance
- Sistema de cache otimizado reduz uso de CPU
- Operações assíncronas para I/O de arquivos
- Cleanup automático de dados antigos
- Validação eficiente de localizações

### Limitações Conhecidas
- Máximo de 1 dungeon ativa por jogador por skill
- Schematics limitados pelo tamanho máximo do WorldEdit
- Dependência do mcMMO para funcionamento básico

### Migração de Dados
- Primeira instalação: Configuração automática
- Dados de jogadores preservados entre reloads
- Backup automático antes de saves críticos

---

## Formato das Versões

### [MAJOR.MINOR.PATCH]

- **MAJOR**: Mudanças incompatíveis na API
- **MINOR**: Funcionalidades adicionadas de forma compatível
- **PATCH**: Correções de bugs compatíveis

### Tipos de Mudanças

- **Adicionado** - Para novas funcionalidades
- **Alterado** - Para mudanças em funcionalidades existentes
- **Depreciado** - Para funcionalidades que serão removidas
- **Removido** - Para funcionalidades removidas
- **Corrigido** - Para correções de bugs
- **Segurança** - Para vulnerabilidades corrigidas