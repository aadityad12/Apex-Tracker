package com.example.apextracker

private fun paperAuthorsForBibtex(authors: String): String =
    authors.split(",")
        .map(String::trim)
        .filter(String::isNotEmpty)
        .joinToString(" and ")

internal fun bibtexEscape(value: String): String = buildString {
    value.forEach { character ->
        append(
            when (character) {
                '\\' -> "\\textbackslash{}"
                '{' -> "\\{"
                '}' -> "\\}"
                '%' -> "\\%"
                '$' -> "\\$"
                '&' -> "\\&"
                '#' -> "\\#"
                '_' -> "\\_"
                '^' -> "\\textasciicircum{}"
                '~' -> "\\textasciitilde{}"
                else -> character
            }
        )
    }
}

private fun citationKey(paper: Paper, index: Int): String {
    val source = paper.s2Id.ifBlank { "${paper.id}-${paper.title}" }
    val normalized = source.filter(Char::isLetterOrDigit).take(32)
    return "apex${normalized.ifBlank { index.toString() }}"
}

/** BibTeX serializer for the full Papers log. Each entry gets a deterministic Apex-owned key. */
fun buildPapersBibtex(papers: List<Paper>): String = papers.mapIndexed { index, paper ->
    val fields = buildList {
        add("title" to paper.title)
        paper.authors.takeIf(String::isNotBlank)?.let { add("author" to paperAuthorsForBibtex(it)) }
        paper.year?.let { add("year" to it.toString()) }
        paper.venue.takeIf(String::isNotBlank)?.let { add("journal" to it) }
        paper.url.ifBlank { paper.pdfUrl }.takeIf(String::isNotBlank)?.let { add("url" to it) }
    }
    buildString {
        append("@article{")
        append(citationKey(paper, index + 1))
        if (fields.isNotEmpty()) append(",\n") else append('\n')
        fields.forEachIndexed { fieldIndex, (name, value) ->
            append("  ")
            append(name)
            append(" = {")
            append(bibtexEscape(value))
            append('}')
            if (fieldIndex < fields.lastIndex) append(',')
            append('\n')
        }
        append('}')
    }
}.joinToString("\n\n")

/** CSV serializer for citation metadata plus the reading-log fields users may analyze externally. */
fun buildPapersCsv(papers: List<Paper>): String {
    val header = "title,authors,year,venue,url,pdf_url,s2_id,status,added_date,read_date,memo,signal"
    val rows = papers.map { paper ->
        listOf(
            csvEscape(paper.title),
            csvEscape(paper.authors),
            paper.year?.toString().orEmpty(),
            csvEscape(paper.venue),
            csvEscape(paper.url),
            csvEscape(paper.pdfUrl),
            csvEscape(paper.s2Id),
            csvEscape(paper.status),
            paper.addedDate.toString(),
            paper.readDate?.toString().orEmpty(),
            csvEscape(paper.memo),
            paper.signal?.toString().orEmpty()
        ).joinToString(",")
    }
    return (listOf(header) + rows).joinToString("\n")
}
