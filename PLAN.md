# Plugin: Treasure Dungeon mcMMO – by Novi-ui

## Development Plan

### Etapas de Desenvolvimento:

1. **Setup de projeto Maven** ✅
   - Suporte mcMMO, WorldEdit, MythicMobs (soft dependencies)
   - Paper 1.21.4 API

2. **Configuração Base** ✅
   - config.yml e lang.yml
   - plugin.yml com dependências

3. **Sistema Core** ✅
   - Listener de skill levelup mcMMO
   - Verificação de chance, cooldown e blacklist
   - Geração do item custom de mapa

4. **Sistema de Mapa** ✅
   - Ação ao clicar no mapa
   - TP para mundo configurado
   - Gerar coordenadas aleatórias
   - Guardar em data.yml

5. **Sistema de Dungeon** ✅
   - Detecção de proximidade ao sino
   - Gerar schematic do sino via WorldEdit
   - Evento ao tocar o sino

6. **Sistema de Combat** ✅
   - Spawnar waves de minions via MythicMobs
   - Boss final configurável
   - Top 3 damage tracking
   - Loot system

7. **Comandos** ✅
   - /treasure tp e /treasure reload
   - Permissões adequadas

8. **Integração Externa** ✅
   - Suporte MythicMobs e multiverso
   - Sistema de mensagens traduzíveis
   - WorldEdit para schematics

9. **Otimização** ✅
   - Null safety em todos os métodos
   - Performance otimizada
   - Sistema modular para fácil manutenção

### Funcionalidades Implementadas:

- ✅ Sistema de skills mcMMO configurável
- ✅ Geração de mapas do tesouro com chance customizável
- ✅ Cooldown system por jogador
- ✅ Teleporte para mundo de tesouros
- ✅ Sistema de proximidade e sino
- ✅ Waves configuráveis de mobs
- ✅ Boss final com damage tracking
- ✅ Top 3 damage ranking
- ✅ Sistema de loot
- ✅ Traduções multilíngua
- ✅ Comandos administrativos
- ✅ Integração WorldEdit/MythicMobs

### Testagem:
- Verificação de compilação Maven
- Testes de integração com mcMMO
- Validação de performance
- Teste de null safety