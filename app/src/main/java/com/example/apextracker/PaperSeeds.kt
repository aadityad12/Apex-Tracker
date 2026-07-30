package com.example.apextracker

/**
 * The bundled starter list (Plan.md decision 4): foundational CS/ML papers with metadata baked
 * in, so importing works on day one with no network. The tldr lines here are hand-written
 * "why this matters" hooks, not the S2 machine summaries. Identity for dedup is [SeedPaper.url]
 * (seeds carry no s2Id) — see PaperDao.getByUrl.
 */
data class SeedPaper(
    val title: String,
    val authors: String,
    val year: Int,
    val venue: String,
    val tldr: String,
    val url: String,
    val pdfUrl: String
)

val PAPER_SEEDS = listOf(
    SeedPaper(
        "Attention Is All You Need",
        "Ashish Vaswani, Noam Shazeer, Niki Parmar, Jakob Uszkoreit, Llion Jones, Aidan N. Gomez, Łukasz Kaiser, Illia Polosukhin",
        2017, "NeurIPS",
        "The Transformer: attention replaces recurrence entirely. The architecture behind essentially every modern language model.",
        "https://arxiv.org/abs/1706.03762", "https://arxiv.org/pdf/1706.03762"
    ),
    SeedPaper(
        "Deep Residual Learning for Image Recognition",
        "Kaiming He, Xiangyu Zhang, Shaoqing Ren, Jian Sun",
        2015, "CVPR",
        "ResNet: skip connections let networks go from ~20 layers to 150+. Why 'just add the input back' fixed deep-network training.",
        "https://arxiv.org/abs/1512.03385", "https://arxiv.org/pdf/1512.03385"
    ),
    SeedPaper(
        "Adam: A Method for Stochastic Optimization",
        "Diederik P. Kingma, Jimmy Ba",
        2014, "ICLR",
        "The default optimizer of deep learning — adaptive per-parameter learning rates from running moment estimates.",
        "https://arxiv.org/abs/1412.6980", "https://arxiv.org/pdf/1412.6980"
    ),
    SeedPaper(
        "BERT: Pre-training of Deep Bidirectional Transformers for Language Understanding",
        "Jacob Devlin, Ming-Wei Chang, Kenton Lee, Kristina Toutanova",
        2018, "NAACL",
        "Masked-language-model pretraining + fine-tuning: the recipe that made one pretrained model transfer to every NLP task.",
        "https://arxiv.org/abs/1810.04805", "https://arxiv.org/pdf/1810.04805"
    ),
    SeedPaper(
        "Language Models are Few-Shot Learners",
        "Tom B. Brown, Benjamin Mann, Nick Ryder, Melanie Subbiah, et al.",
        2020, "NeurIPS",
        "GPT-3: scale alone yields in-context learning — a model that picks up tasks from a prompt with no gradient updates.",
        "https://arxiv.org/abs/2005.14165", "https://arxiv.org/pdf/2005.14165"
    ),
    SeedPaper(
        "Generative Adversarial Networks",
        "Ian J. Goodfellow, Jean Pouget-Abadie, Mehdi Mirza, Bing Xu, David Warde-Farley, Sherjil Ozair, Aaron Courville, Yoshua Bengio",
        2014, "NeurIPS",
        "Two networks in a game: a generator forging samples and a discriminator calling fakes. Adversarial training, from first principles.",
        "https://arxiv.org/abs/1406.2661", "https://arxiv.org/pdf/1406.2661"
    ),
    SeedPaper(
        "Scaling Laws for Neural Language Models",
        "Jared Kaplan, Sam McCandlish, Tom Henighan, Tom B. Brown, et al.",
        2020, "arXiv",
        "Loss falls as a smooth power law in model size, data, and compute — the empirical result that justified training ever-larger models.",
        "https://arxiv.org/abs/2001.08361", "https://arxiv.org/pdf/2001.08361"
    ),
    SeedPaper(
        "Chain-of-Thought Prompting Elicits Reasoning in Large Language Models",
        "Jason Wei, Xuezhi Wang, Dale Schuurmans, Maarten Bosma, Brian Ichter, Fei Xia, Ed Chi, Quoc Le, Denny Zhou",
        2022, "NeurIPS",
        "Asking a model to show its work — intermediate reasoning steps in the prompt — unlocks problems it otherwise fails.",
        "https://arxiv.org/abs/2201.11903", "https://arxiv.org/pdf/2201.11903"
    ),
    SeedPaper(
        "Training Language Models to Follow Instructions with Human Feedback",
        "Long Ouyang, Jeff Wu, Xu Jiang, Diogo Almeida, et al.",
        2022, "NeurIPS",
        "InstructGPT: RLHF turns a raw language model into an assistant. The alignment recipe behind modern chat models.",
        "https://arxiv.org/abs/2203.02155", "https://arxiv.org/pdf/2203.02155"
    ),
    SeedPaper(
        "Constitutional AI: Harmlessness from AI Feedback",
        "Yuntao Bai, Saurav Kadavath, Sandipan Kundu, Amanda Askell, et al.",
        2022, "arXiv",
        "Replace human harmlessness labels with AI self-critique against a written constitution — alignment supervision that scales.",
        "https://arxiv.org/abs/2212.08073", "https://arxiv.org/pdf/2212.08073"
    ),
    SeedPaper(
        "In Search of an Understandable Consensus Algorithm (Raft)",
        "Diego Ongaro, John Ousterhout",
        2014, "USENIX ATC",
        "Consensus designed for humans: leader election, log replication, and safety, decomposed so you can actually hold it in your head.",
        "https://raft.github.io/raft.pdf", "https://raft.github.io/raft.pdf"
    ),
    SeedPaper(
        "MapReduce: Simplified Data Processing on Large Clusters",
        "Jeffrey Dean, Sanjay Ghemawat",
        2004, "OSDI",
        "Two functions — map and reduce — hide fault tolerance and distribution for cluster-scale computation. The paper that started big data.",
        "https://research.google/pubs/pub62/", "https://static.googleusercontent.com/media/research.google.com/en//archive/mapreduce-osdi04.pdf"
    ),
)
