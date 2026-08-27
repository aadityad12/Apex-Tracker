package com.example.apextracker

/**
 * The cloudId of "the other paper" in [this] link, from the perspective of [paperCloudId]. Every
 * link is undirected (see PaperLink.kt), so this is the only place either side ever needs to know
 * which stored field held which id.
 */
fun PaperLink.otherPaperCloudId(paperCloudId: String): String =
    if (this.paperCloudId == paperCloudId) relatedPaperCloudId else this.paperCloudId

/**
 * Resolves [paper]'s linked papers (in either stored direction) into actual [Paper] rows, newest
 * link first. A link pointing at a since-deleted paper is silently excluded — nothing to clean
 * up, it just stops appearing, the same "dangling reference resolves to nothing" treatment
 * [topicCloudId] gets in PapersDiscoveryScoring.
 */
fun relatedPapersFor(paper: Paper, allLinks: List<PaperLink>, allPapers: List<Paper>): List<Paper> {
    if (paper.cloudId.isEmpty()) return emptyList()
    val byCloudId = allPapers.associateBy { it.cloudId }
    return allLinks
        .filter { it.paperCloudId == paper.cloudId || it.relatedPaperCloudId == paper.cloudId }
        .sortedByDescending { it.createdDate }
        .mapNotNull { link -> byCloudId[link.otherPaperCloudId(paper.cloudId)] }
        .distinctBy { it.cloudId }
}

/**
 * The candidates a paper-picker should offer for linking to [paper]: every other paper, minus
 * itself and minus whichever papers are already linked to it. Order is oldest-added-first,
 * matching the reading queue's own convention (PaperDao.getAllPapers()), so a long list at least
 * has a stable, predictable order rather than shuffling as links change.
 */
fun linkablePapersFor(paper: Paper, allLinks: List<PaperLink>, allPapers: List<Paper>): List<Paper> {
    val alreadyLinked = relatedPapersFor(paper, allLinks, allPapers).mapTo(mutableSetOf()) { it.cloudId }
    return allPapers.filter { it.cloudId != paper.cloudId && it.cloudId !in alreadyLinked }
}

/**
 * Whether a new link between [aCloudId] and [bCloudId] would be valid: not the same paper linked
 * to itself, and not a duplicate of an existing link (checked in either stored direction). Pure
 * mirror of [PaperLinkDao.findExisting] for call sites that already have the link list loaded and
 * want to validate without a DB round trip (e.g. disabling the picker's confirm button live).
 */
fun canLinkPapers(aCloudId: String, bCloudId: String, existingLinks: List<PaperLink>): Boolean {
    if (aCloudId.isEmpty() || bCloudId.isEmpty() || aCloudId == bCloudId) return false
    return existingLinks.none {
        (it.paperCloudId == aCloudId && it.relatedPaperCloudId == bCloudId) ||
            (it.paperCloudId == bCloudId && it.relatedPaperCloudId == aCloudId)
    }
}
