<template>
  <pre class="command-lines"><code><span v-for="(line, index) in tokenLines" :key="`${index}-${line.raw}`" class="line"><span class="line-no">{{ index + 1 }}</span><template v-for="(token, tokenIndex) in line.tokens" :key="`${index}-${tokenIndex}`"><span :class="['token', token.kind]">{{ token.text }}</span></template></span></code></pre>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{ commands: string[] }>();

type TokenKind = 'plain' | 'comment' | 'view' | 'action' | 'deny' | 'permit';

interface Token {
  text: string;
  kind: TokenKind;
}

const viewKeywords = ['sysname', 'interface', 'vlan', 'batch', 'ospf', 'area', 'acl', 'return'];
const actionPhrases = ['ip address', 'network', 'port link-type', 'port trunk allow-pass', 'description'];
const actionWords = ['ip', 'address', 'network', 'port', 'link-type', 'trunk', 'allow-pass', 'description', 'source', 'destination'];

const tokenLines = computed(() =>
  props.commands.map((raw) => ({
    raw,
    tokens: tokenize(raw)
  }))
);

function tokenize(line: string): Token[] {
  if (line.trimStart().startsWith('#')) {
    return [{ text: line, kind: 'comment' }];
  }

  const parts = line.split(/(\s+)/);
  const lowered = line.toLowerCase();
  return parts.map((text) => {
    const word = text.toLowerCase();
    if (/^\s+$/.test(text)) return { text, kind: 'plain' };
    if (word === 'deny') return { text, kind: 'deny' };
    if (word === 'permit') return { text, kind: 'permit' };
    if (viewKeywords.includes(word)) return { text, kind: 'view' };
    if (actionWords.includes(word) || actionPhrases.some((phrase) => lowered.includes(phrase) && phrase.includes(word))) {
      return { text, kind: 'action' };
    }
    return { text, kind: 'plain' };
  });
}
</script>

<style scoped>
.command-lines {
  max-height: 438px;
  margin: 0;
  overflow: auto;
  padding: 16px 0;
  border: 1px solid rgba(0, 217, 192, 0.28);
  border-radius: 18px;
  color: var(--mactav-text-main);
  background:
    radial-gradient(circle at 82% 0, rgba(0, 217, 192, 0.18), transparent 34%),
    linear-gradient(180deg, rgba(248, 253, 255, 0.9), rgba(232, 247, 255, 0.74));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.72),
    0 16px 34px rgba(31, 91, 180, 0.08);
  font-family: "Cascadia Code", Consolas, monospace;
  font-size: 12px;
  line-height: 1.75;
}

.line {
  display: block;
  min-width: max-content;
  padding: 0 16px;
  white-space: pre;
}

.line:hover {
  background: rgba(0, 98, 255, 0.06);
}

.line-no {
  display: inline-block;
  width: 28px;
  margin-right: 12px;
  color: rgba(100, 116, 139, 0.55);
  text-align: right;
}

.comment {
  color: #7c8da6;
}

.view {
  color: var(--mactav-cyber-blue);
  font-weight: 800;
}

.action {
  color: #b77900;
}

.deny {
  color: var(--mactav-danger);
  font-weight: 900;
}

.permit {
  color: var(--mactav-success);
  font-weight: 900;
}
</style>
