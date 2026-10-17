package com.malism.linsia.learnword.domain

object DailyTaskBuilder {
    fun build(input: TaskInput): List<TaskWord> {
        val newTaken = input.newWords
            .sortedBy { it.ordinal }
            .take(input.dailyNewLimit.coerceAtLeast(0))
            .map { it.copy(source = WordSource.New) }
        val leftovers = input.leftovers.map { it.copy(source = WordSource.Leftover) }
        val review = input.dueReview.map { it.copy(source = WordSource.Review) }

        val seen = LinkedHashSet<Long>()
        fun takeUnique(list: List<TaskWord>): List<TaskWord> =
            list.filter { seen.add(it.wordId) }

        val uniqueLeftover = takeUnique(leftovers)
        val uniqueReview = takeUnique(review)
        val uniqueNew = takeUnique(newTaken)

        return interleave(uniqueReview + uniqueLeftover, uniqueNew)
    }

    internal fun interleave(review: List<TaskWord>, newWords: List<TaskWord>): List<TaskWord> {
        if (review.isEmpty()) return newWords
        if (newWords.isEmpty()) return review

        val reviewHead = review.take((review.size + 1) / 2)
        val reviewTail = review.drop(reviewHead.size)
        val newHead = newWords.take((newWords.size + 1) / 2)
        val newTail = newWords.drop(newHead.size)

        val mixedTail = ArrayList<TaskWord>(reviewTail.size + newTail.size)
        val max = maxOf(reviewTail.size, newTail.size)
        for (i in 0 until max) {
            if (i < newTail.size) mixedTail += newTail[i]
            if (i < reviewTail.size) mixedTail += reviewTail[i]
        }
        return reviewHead + newHead + mixedTail
    }
}
