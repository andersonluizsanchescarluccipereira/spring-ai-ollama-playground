'use strict';
const $ = id => document.getElementById(id);
const answer = $('answer'), status = $('status'), metrics = $('metrics');
let controller;
for (const [id, decimals] of [['temperature', 1], ['topP', 2]]) {
    $(id).addEventListener('input', () => {
        $(id + '-value').value = Number($(id).value).toFixed(decimals);
    });
}
$('stop').addEventListener('click', () => controller?.abort());
$('chat-form').addEventListener('submit', async event => {
    event.preventDefault();
    if (controller) return;
    const payload = {
        prompt: $('prompt').value,
        systemPrompt: $('systemPrompt').value,
        temperature: Number($('temperature').value),
        topK: Number($('topK').value),
        topP: Number($('topP').value),
        maxTokens: Number($('maxTokens').value),
        seed: Number($('seed').value)
    };
    const streaming = $('stream').checked;
    controller = new AbortController();
    $('send').disabled = true;
    $('stop').disabled = false;
    answer.textContent = '';
    metrics.textContent = '';
    status.textContent = 'Gerando… a primeira resposta pode demorar.';
    const start = performance.now();
    try {
        const response = await fetch('/api/chat' + (streaming ? '/stream' : ''), {
            method: 'POST', headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload), signal: controller.signal
        });
        if (!response.ok) {
            const data = await response.json().catch(() => ({}));
            throw new Error(data.error || `HTTP ${response.status}. Verifique o terminal.`);
        }
        if (!streaming) {
            const data = await response.json();
            answer.textContent = data.answer ?? '';
            const n = v => v == null ? 'N/D' : v;
            metrics.textContent = `Tempo no servidor: ${(data.elapsedMs / 1000).toFixed(2)} s | Tokens entrada: ${n(data.promptTokens)} | saída: ${n(data.completionTokens)} | total: ${n(data.totalTokens)}`;
        } else {
            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let buffer = '', doneEvent = false, firstChunkMs = null;
            function consume(frame) {
                const lines = frame.split(/\r?\n/);
                const type = lines.find(l => l.startsWith('event:'))?.slice(6).trim();
                const data = lines.filter(l => l.startsWith('data:')).map(l => l.slice(5).replace(/^ /, '')).join('\n');
                if (!data) return;
                const chunk = JSON.parse(data);
                if (type === 'error') throw new Error(chunk.text);
                if (type === 'done') doneEvent = true;
                if (type === 'chunk') {
                    if (firstChunkMs === null) firstChunkMs = performance.now() - start;
                    answer.textContent += chunk.text;
                }
            }
            try {
                while (true) {
                    const {value, done} = await reader.read();
                    buffer += decoder.decode(value, {stream: !done});
                    let match;
                    while ((match = /\r?\n\r?\n/.exec(buffer))) {
                        consume(buffer.slice(0, match.index));
                        buffer = buffer.slice(match.index + match[0].length);
                    }
                    if (done) break;
                }
                if (buffer.trim()) consume(buffer);
                if (!doneEvent) throw new Error('Streaming interrompido antes do término.');
            } finally { await reader.cancel().catch(() => {}); }
            metrics.textContent = `Tempo total: ${((performance.now()-start)/1000).toFixed(2)} s | Primeiro trecho: ${firstChunkMs === null ? 'N/D' : (firstChunkMs/1000).toFixed(2) + ' s'} | Para métricas de tokens, desative o streaming.`;
        }
        status.textContent = 'Resposta concluída.';
    } catch (error) {
        status.textContent = error.name === 'AbortError' ? 'Recebimento interrompido.' : 'Erro: ' + error.message;
    } finally {
        controller = undefined;
        $('send').disabled = false;
        $('stop').disabled = true;
    }
});
