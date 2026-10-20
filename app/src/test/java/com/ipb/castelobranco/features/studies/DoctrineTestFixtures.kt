package com.ipb.castelobranco.features.studies

import com.ipb.castelobranco.features.studies.domain.model.BibleReference
import com.ipb.castelobranco.features.studies.domain.model.CatechismQuestion
import com.ipb.castelobranco.features.studies.domain.model.ConfessionChapter
import com.ipb.castelobranco.features.studies.domain.model.ConfessionParagraph
import com.ipb.castelobranco.features.studies.domain.model.DoctrineDocument
import com.ipb.castelobranco.features.studies.domain.model.ParagraphList
import com.ipb.castelobranco.features.studies.domain.model.ProofText
import com.ipb.castelobranco.features.studies.domain.model.VerseSpan

object DoctrineTestFixtures {

    const val SHORTER = "westminster-shorter-catechism"
    const val CONFESSION = "westminster-confession-of-faith"

    /** Shaped like the server's files: Q1 of the Breve Catecismo, with an uninterpreted proof text added. */
    val catechismJson = """
        {
          "slug": "$SHORTER",
          "title": "Breve Catecismo de Westminster",
          "kind": "catechism",
          "position": 1,
          "questions": [
            {
              "number": 2,
              "question": "Que regra deu Deus?",
              "answer": "A Palavra de Deus.\n1ª. Primeira regra",
              "proof_texts": [{"printed": "Sl 86-8", "reference": null}]
            },
            {
              "number": 1,
              "question": "Qual é o fim principal do homem?",
              "answer": "O fim principal do homem é glorificar a Deus, e gozá-lo para sempre.",
              "proof_texts": [
                {"printed": "Rm 11.36", "reference": {"book": "Rm", "chapter": 11, "verses": [{"start": 36, "end": 36}]}},
                {"printed": "Sl 125", "reference": {"book": "Sl", "chapter": 125, "verses": []}}
              ]
            }
          ]
        }
    """.trimIndent()

    val confessionJson = """
        {
          "slug": "$CONFESSION",
          "title": "A Confissão de Fé de Westminster",
          "kind": "confession",
          "position": 3,
          "chapters": [
            {
              "number": 1,
              "title": "DA SAGRADAS ESCRITURAS",
              "paragraphs": [
                {"number": 1, "text": "Ainda que a luz da natureza..."},
                {
                  "number": 2,
                  "text": "Sob o nome de Escritura Sagrada incluem-se:",
                  "lists": [
                    {"heading": "O Velho Testamento", "items": ["Gênesis", "Êxodo"]},
                    {"heading": "O Novo Testamento", "items": ["Mateus", "Marcos"]}
                  ],
                  "text_after": "Todos dados por inspiração de Deus."
                }
              ]
            }
          ]
        }
    """.trimIndent()

    val listJson = """
        {
          "documents": [
            {"slug": "$SHORTER", "title": "Breve Catecismo de Westminster", "kind": "catechism", "item_count": 107},
            {"slug": "future-thing", "title": "Algo novo", "kind": "devotional", "item_count": 5},
            {"slug": "$CONFESSION", "title": "A Confissão de Fé de Westminster", "kind": "confession", "item_count": 33}
          ]
        }
    """.trimIndent()

    val catechism = DoctrineDocument.Catechism(
        slug = SHORTER,
        title = "Breve Catecismo de Westminster",
        questions = listOf(
            CatechismQuestion(
                number = 1,
                question = "Qual é o fim principal do homem?",
                answer = "O fim principal do homem é glorificar a Deus, e gozá-lo para sempre.",
                proofTexts = listOf(ProofText("Rm 11.36", BibleReference("Rm", 11, listOf(VerseSpan(36, 36))))),
            ),
            CatechismQuestion(
                number = 33,
                question = "Que é justificação?",
                answer = "Justificação é um ato da livre graça de Deus.",
                proofTexts = emptyList(),
            ),
            CatechismQuestion(
                number = 34,
                question = "Que é adoção?",
                answer = "Adoção é um ato da livre graça de Deus, pelo qual somos recebidos no número dos filhos.",
                proofTexts = emptyList(),
            ),
        ),
    )

    val confession = DoctrineDocument.Confession(
        slug = CONFESSION,
        title = "A Confissão de Fé de Westminster",
        chapters = listOf(
            ConfessionChapter(
                number = 1,
                title = "Da Sagradas Escrituras",
                paragraphs = listOf(
                    ConfessionParagraph(1, "Ainda que a luz da natureza...", emptyList(), null),
                    ConfessionParagraph(
                        number = 2,
                        text = "Sob o nome de Escritura Sagrada incluem-se:",
                        lists = listOf(ParagraphList("O Velho Testamento", listOf("Gênesis", "Êxodo"))),
                        textAfter = "Todos dados por inspiração de Deus.",
                    ),
                ),
            ),
            ConfessionChapter(
                number = 12,
                title = "Da adoção",
                paragraphs = listOf(ConfessionParagraph(1, "Todos os que são justificados...", emptyList(), null)),
            ),
        ),
    )
}
