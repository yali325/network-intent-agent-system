/**
 * Model Core owns MAC-TAV task state, workspace views, artifact versions, events,
 * execution records, and trace/audit records.
 *
 * <p>It must not call LLMs, generate stage artifacts, run simulations, or depend
 * on web, orchestrator, concrete agent modules, or Spring AI Alibaba APIs.</p>
 */
package com.yali.mactav.modelcore;
