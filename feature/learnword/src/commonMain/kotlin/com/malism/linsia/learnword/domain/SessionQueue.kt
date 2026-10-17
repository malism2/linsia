package com.malism.linsia.learnword.domain

class SessionQueue(
    initial: List<SessionCard>,
    private val insertOffset: () -> Int = { (LearnWordDefaults.ReinsertMin..LearnWordDefaults.ReinsertMax).random() },
) {
    private val queue = ArrayDeque(initial)

    val remaining: List<SessionCard> get() = queue.toList()
    val isEmpty: Boolean get() = queue.isEmpty()
    val current: SessionCard? get() = queue.firstOrNull()

    fun peek(): QueueEvent = current?.let { QueueEvent.Show(it) } ?: QueueEvent.Empty

    fun know(): KnowResult {
        val card = queue.removeFirstOrNull() ?: return KnowResult(null, QueueEvent.Empty)
        val nextLife = card.life - 1
        val updated = card.copy(
            life = nextLife,
            firstPassKnown = card.firstPassKnown ?: true,
        )
        val graduated = if (nextLife <= 0) updated.copy(life = 0) else null
        if (graduated == null) {
            reinsert(updated)
        }
        return KnowResult(graduated, peek())
    }

    fun markUnknownAndReinsert() {
        val card = queue.removeFirstOrNull() ?: return
        val updated = card.copy(
            life = card.life.coerceAtLeast(LearnWordDefaults.InitialLife),
            firstPassKnown = card.firstPassKnown ?: false,
        )
        reinsert(updated)
    }

    fun replaceAll(cards: List<SessionCard>) {
        queue.clear()
        queue.addAll(cards)
    }

    private fun reinsert(card: SessionCard) {
        val offset = insertOffset().coerceAtLeast(0)
        val index = offset.coerceAtMost(queue.size)
        if (index >= queue.size) {
            queue.addLast(card)
        } else {
            val list = queue.toMutableList()
            list.add(index, card)
            queue.clear()
            queue.addAll(list)
        }
    }
}
