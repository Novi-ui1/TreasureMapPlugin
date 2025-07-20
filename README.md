# 🏴‍☠️ TreasureDungeon Plugin

[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)](https://github.com/novi-ui/treasuredungeon)
[![Minecraft](https://img.shields.io/badge/minecraft-1.21.4-green.svg)](https://papermc.io/)
[![License](https://img.shields.io/badge/license-MIT-yellow.svg)](LICENSE)

> **Uma aventura épica de caça ao tesouro integrada com mcMMO para servidores Paper 1.21.4**

## 🌟 Características Principais

### 🎯 Sistema de Integração mcMMO
- **Mapas do Tesouro Automáticos**: Receba mapas ao atingir níveis específicos de skills
- **Sistema de Cooldown**: Controle inteligente para evitar spam
- **Múltiplas Skills**: Suporte para Mining, Woodcutting, Fishing, Excavation e mais
- **Chances Configuráveis**: Personalize a probabilidade de drop para cada skill

### 🏰 Dungeons Procedurais
- **5 Tipos de Dungeon**: Ancient Mine, Cursed Forest, Frozen Cavern, Desert Tomb, Volcanic Depths
- **Sistema de Pesos**: Dungeons mais raras têm menor chance de aparecer
- **Schematics WorldEdit**: Estruturas customizáveis e detalhadas
- **Spawns Inteligentes**: Sistema avançado de posicionamento de mobs e bosses

### ⚔️ Sistema de Combate Avançado
- **Waves Configuráveis**: 2-5 ondas de mobs antes do boss final
- **MythicMobs Integration**: Mobs customizados com habilidades únicas
- **Damage Tracking**: Ranking dos top 3 jogadores que mais causaram dano
- **Boss Fights Épicos**: Bosses únicos para cada tipo de dungeon

### 🎁 Sistema de Recompensas
- **Loot Customizável**: Configure comandos ou itens como recompensa
- **Baús Temporários**: Baús que desaparecem após um tempo configurável
- **Economia Integrada**: Suporte para plugins de economia
- **Recompensas Escaláveis**: Diferentes recompensas para diferentes dungeons

## 🚀 Instalação Rápida

### Pré-requisitos
- **Paper 1.21.4+** (obrigatório)
- **mcMMO 2.2.024+** (obrigatório)
- **WorldEdit 7.3.8+** (recomendado)
- **MythicMobs 5.6.1+** (recomendado)
- **Multiverse-Core** (opcional)

### Passos de Instalação

1. **Download e Instalação**
   ```bash
   # Baixe o plugin e coloque na pasta plugins/
   # Reinicie o servidor
   ```

2. **Configuração Básica**
   ```yaml
   # Edite plugins/TreasureDungeon/config.yml
   general:
     treasure-world:
       name: "treasure_world"  # Nome do mundo dos tesouros
   ```

3. **Adicionar Schematics** (Opcional)
   ```bash
   # Coloque seus arquivos .schem em:
   plugins/TreasureDungeon/schematics/
   ```

4. **Reload da Configuração**
   ```
   /treasure reload
   ```

## ⚙️ Configuração Detalhada

### Skills Configuration
```yaml
skills:
  mining:
    enabled: true
    level-required: 1000      # Nível necessário
    chance-to-drop: 0.3       # 30% de chance
    cooldown: "6h"            # Cooldown de 6 horas
    
    map-item:
      material: FILLED_MAP
      display-name: "&6Mapa do Tesouro [Mineração]"
      lore:
        - "&7Um velho mapa marcado por explosões."
        - "&eUsa para descobrir uma mina escondida!"
      custom-model-data: 1031
      glowing: true
```

### Dungeon Types
```yaml
dungeon-types:
  ancient_mine:
    weight: 30                # Peso para seleção aleatória
    schematic-bell: "bell-ancient-mine.schem"
    schematic-dungeon: "ancient-mine-dungeon.schem"
    waves:
      count: 3
      mobs:
        wave1: ["SKELETON_MINER", "ZOMBIE_DIGGER"]
        wave2: ["CAVE_SPIDER_ELITE", "SKELETON_MINER"]
        wave3: ["UNDEAD_DWARF", "CRYSTAL_GOLEM"]
    boss:
      id: "MINING_OVERLORD"
      spawn-delay: 8s
    loot:
      type: "commands"
      commands:
        - "give {player} diamond 16"
        - "eco give {player} 1000"
```

## 🎮 Como Jogar

### 1. Obter um Mapa do Tesouro
- Suba de nível em qualquer skill do mcMMO configurada
- Tenha chance de receber um mapa do tesouro
- Cada skill tem seu próprio cooldown

### 2. Usar o Mapa
- Clique direito com o mapa na mão
- Será teleportado para o mundo dos tesouros
- Siga as coordenadas fornecidas

### 3. Encontrar a Dungeon
- Aproxime-se das coordenadas indicadas
- Um sino antigo aparecerá quando estiver próximo
- Clique direito no sino para iniciar a dungeon

### 4. Sobreviver às Waves
- Derrote todas as ondas de mobs
- Cada dungeon tem 2-5 ondas configuráveis
- Prepare-se para o boss final!

### 5. Derrotar o Boss
- Boss único para cada tipo de dungeon
- Sistema de damage tracking
- Recompensas baseadas na performance

## 🛠️ Comandos e Permissões

### Comandos
| Comando | Descrição | Permissão |
|---------|-----------|-----------|
| `/treasure tp` | Teleportar para o spawn dos tesouros | `treasure.tp` |
| `/treasure reload` | Recarregar configuração | `treasure.reload` |

### Permissões
| Permissão | Descrição | Padrão |
|-----------|-----------|---------|
| `treasure.use` | Uso básico do plugin | `true` |
| `treasure.tp` | Comando de teleporte | `op` |
| `treasure.admin` | Comandos administrativos | `op` |
| `treasure.reload` | Recarregar configuração | `op` |

## 🏗️ Estrutura de Arquivos

```
plugins/TreasureDungeon/
├── config.yml              # Configuração principal
├── data.yml                # Dados dos jogadores
├── lang/                   # Arquivos de idioma
│   ├── en.yml             # Inglês
│   └── pt.yml             # Português
└── schematics/            # Arquivos WorldEdit
    ├── bell-ancient-mine.schem
    ├── ancient-mine-dungeon.schem
    └── ...
```

## 🔧 Desenvolvimento e API

### Building
```bash
mvn clean package
```

### API Usage
```java
TreasureDungeonPlugin plugin = TreasureDungeonPlugin.getInstance();
MapManager mapManager = plugin.getMapManager();
DungeonManager dungeonManager = plugin.getDungeonManager();

// Dar mapa customizado
mapManager.giveTreasureMap(player, "mining");

// Verificar dungeon ativa
boolean hasActive = plugin.getDataManager()
    .hasActiveDungeon(player.getUniqueId(), "mining");
```

## 🚀 Futuras Atualizações

### Versão 1.1.0 - Sistema de Clãs
- **Dungeons em Grupo**: Até 4 jogadores por dungeon
- **Loot Compartilhado**: Sistema de distribuição automática
- **Dificuldade Escalável**: Mais jogadores = mais dificuldade
- **Chat de Dungeon**: Comunicação exclusiva durante a aventura

### Versão 1.2.0 - Progressão Avançada
- **Sistema de Níveis**: Dungeons desbloqueiam conforme progressão
- **Achievements**: Conquistas especiais para ações específicas
- **Estatísticas**: Tracking detalhado de performance
- **Leaderboards**: Rankings globais e mensais

### Versão 1.3.0 - Customização Total
- **Dungeon Builder**: Interface in-game para criar dungeons
- **Mob Editor**: Customizar mobs sem MythicMobs
- **Loot Tables**: Sistema avançado de drops
- **Event System**: Eventos especiais temporários

### Versão 1.4.0 - Integração Avançada
- **PlaceholderAPI**: Placeholders para outros plugins
- **Discord Integration**: Notificações no Discord
- **Web Dashboard**: Painel web para administração
- **Database Support**: MySQL/PostgreSQL para grandes servidores

## 🐛 Reportar Bugs

Encontrou um bug? Ajude-nos a melhorar!

1. **Verifique** se já não foi reportado
2. **Colete** informações:
   - Versão do plugin
   - Versão do Paper
   - Logs de erro
   - Passos para reproduzir
3. **Abra** uma issue no GitHub

## 🤝 Contribuindo

Contribuições são bem-vindas! 

1. Fork o repositório
2. Crie uma branch para sua feature
3. Faça suas alterações
4. Teste thoroughly
5. Submeta um pull request

### Diretrizes de Contribuição
- Siga o padrão de código existente
- Adicione testes para novas funcionalidades
- Documente mudanças no README
- Use commits descritivos

## 📄 Licença

Este projeto está licenciado sob a MIT License - veja o arquivo [LICENSE](LICENSE) para detalhes.

## 🙏 Agradecimentos

- **mcMMO Team** - Pela excelente API
- **PaperMC** - Pela plataforma robusta
- **MythicMobs** - Pelo sistema de mobs customizados
- **WorldEdit** - Pelas ferramentas de construção
- **Comunidade Minecraft** - Pelo feedback e suporte

## 📞 Suporte

- **GitHub Issues**: [Reportar problemas](https://github.com/novi-ui/treasuredungeon/issues)
- **Discord**: [Servidor da comunidade](#)
- **Email**: novi.ui.dev@gmail.com

---

**Desenvolvido com ❤️ por [Novi-ui](https://github.com/novi-ui)**

*Transforme seu servidor em uma aventura épica de caça ao tesouro!*