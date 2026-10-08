package com.mhss.app.domain

object AiConstants {
    const val OPENAI_BASE_URL = "https://api.openai.com/v1"
    const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta"

    const val OPENAI_DEFAULT_MODEL = "gpt-4o"
    const val GEMINI_DEFAULT_MODEL = "gemini-1.5-pro"

    const val GEMINI_KEY_INFO_URL = "https://ai.google.dev/gemini-api/docs/api-key"
    const val OPENAI_KEY_INFO_URL = "https://platform.openai.com/api-keys"

    const val GEMINI_MODELS_INFO_URL = "https://ai.google.dev/gemini-api/docs/models/gemini"
    const val OPENAI_MODELS_INFO_URL = "https://platform.openai.com/docs/models"
}

val systemMessage = """
    Du är en personlig mediinsk AI-assistent specialist i sjukvård och mediciner.
    Du hjälper användare med deras förfrågningar om sjukvård och mediciner och ger detaljerade förklaringar om det behövs.
    Användare kan bifoga anteckningar, uppgifter eller kalenderhändelser. Använd dessa bifogade data som ett sammanhang för ditt svar.
    Dina svar får aldrig innehålla tecknet "*".
    Replace the word "patient" and use only the words "care recipient" or "care recipient".
""".trimIndent()


val String.summarizeNotePrompt: String
    get() = """
        Sammanfatta texten till bullet points.
        Svara endast med sammanfattningen och säg inget annat. Använd Markdown för formatering.
        Svara med medicinskt fack språk.
        Svara med samma språk som det ursprungliga texten.
        Dina svar får aldrig innehålla tecknet "*".
        Byt ut ordet "patient" och använd endast orden "vårdtagare" eller "vårdtagaren".
        $this
        Summary:
    """.trimIndent()

val String.autoFormatNotePrompt: String
    get() = """
        Omformulera texten till längre och fylldigare medicinskt fackligt korrekt språk för medicinsk dokumentation.
        Svara endast med den formaterade texten och säg inget annat.
        Svara med samma språk som det ursprungliga texten.
        Byt ut ordet "patient" och använd endast orden "vårdtagare" eller "vårdtagaren".
        $this
        Formatted note:
    """.trimIndent()

val String.correctSpellingNotePrompt: String
    get() = """
        Korrigera stav- och grammatikfelen i texten.
        Svara med samma text tillbaka, fast koreckt stavat, och säg inget annat.
        Svara med samma språk som det ursprungliga texten.
        Byt ut ordet "patient" och använd endast orden "vårdtagare" eller "vårdtagaren".
        $this
        Corrected note:
    """.trimIndent()

val visionOcrPrompt: String
    get() = """
        Du ska analysera bilden och avgöra vilken av följande två typer den tillhör.
        Välj EXAKT EN typ. Gissa inte.

        TYP 1 – BLODTRYCKSMÄTARE:
        Bilden visar en digital blodtrycksmätare med värden för systoliskt tryck, diastoliskt tryck och puls.

        OM (och endast om) bilden är en blodtrycksmätare:
        - Returnera ENDAST följande tre rader
        - Ingen annan text får förekomma
        - Formatet måste följas exakt

        vårdtagarens tryck är-
        SYS.mmHg: <värde>
        DIA.mmHg: <värde>
        PULS/min: <värde>

        TYP 2 – TEXTDOKUMENT:
        Bilden visar ett dokument med tryckt eller handskriven text.

        OM (och endast om) bilden är ett textdokument:
        - Läs av all läsbar text exakt som den syns
        - Bevara radbrytningar
        - Tolka inte, sammanfatta inte och korrigera inte
        - Lägg inte till och ta inte bort text

        GEMENSAMMA REGLER (gäller alltid):
        - Returnera aldrig förklaringar eller metadata
        - Dina svar får aldrig innehålla tecknet "*"
        - Använd samma språk som texten i bilden
        - Byt ut ordet "patient" och använd endast "vårdtagare" eller "vårdtagaren"
    """.trimIndent()
