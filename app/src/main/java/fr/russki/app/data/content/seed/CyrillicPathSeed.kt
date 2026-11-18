package fr.russki.app.data.content.seed

import fr.russki.app.domain.content.AlphabetLetter
import fr.russki.app.domain.curriculum.CefrLevel
import fr.russki.app.domain.curriculum.CourseUnit
import fr.russki.app.domain.curriculum.LessonOutline
import fr.russki.app.domain.cyrillic.CyrillicReadingItem
import fr.russki.app.domain.cyrillic.CyrillicReadingKind
import fr.russki.app.domain.exercise.InteractiveExercise
import fr.russki.app.domain.exercise.InteractiveExerciseType
import fr.russki.app.domain.lesson.PedagogyStage
import fr.russki.app.domain.lesson.SpeechRegister
import fr.russki.app.domain.lesson.TeachableItem
import fr.russki.app.domain.pronunciation.RussianPronunciation

/**
 * Parcours progressif « Lire le cyrillique » pour francophones.
 *
 * Ordre pédagogique (pas l’ordre alphabétique) :
 * 1. lettres familières → 2. lookalikes → 3. consonnes →
 * 4. voyelles particulières → 5. chuintantes → 6. signes →
 * puis syllabes, mots, bilan.
 *
 * Couverture : les **33** lettres ; aucune prétention CECRL.
 */
object CyrillicPathSeed {

    const val UNIT_ID = "a1-unit-cyrillic"

    const val LESSON_G1 = "a1-lesson-cyr-g1-familiar"
    const val LESSON_G2 = "a1-lesson-cyr-g2-lookalikes"
    const val LESSON_G3 = "a1-lesson-cyr-g3-consonants"
    const val LESSON_SYLLABLES = "a1-lesson-cyr-syllables"
    const val LESSON_G4 = "a1-lesson-cyr-g4-vowels"
    const val LESSON_G5 = "a1-lesson-cyr-g5-sibilants"
    const val LESSON_WORDS = "a1-lesson-cyr-words"
    const val LESSON_G6 = "a1-lesson-cyr-g6-signs"
    const val LESSON_FINAL = "a1-lesson-cyr-final"

    data class LetterGroup(
        val lessonId: String,
        val titleFr: String,
        val objectiveFr: String,
        val introFr: String,
        val summaryFr: String,
        val letterIds: List<String>,
        val focusFr: String,
    )

    val groups: List<LetterGroup> = listOf(
        LetterGroup(
            LESSON_G1, "Lettres familières",
            "Reconnaître А О У М Т К et les lire dans des syllabes simples.",
            "On commence par des lettres proches du latin ou très fréquentes, " +
                "pour pouvoir bientôt lire мама, кот, etc.",
            "Vous reconnaissez six lettres de base et leur nom russe.",
            listOf("letter-01-a", "letter-16-o", "letter-21-u", "letter-14-m", "letter-20-t", "letter-12-k"),
            "Ancrage : formes et sons relativement accessibles.",
        ),
        LetterGroup(
            LESSON_G2, "Pièges lookalikes",
            "Ne plus confondre В Н Р С Е И avec le latin.",
            "Plusieurs lettres russes ressemblent à des lettres françaises mais " +
                "se lisent autrement. On sépare aussi le nom de la lettre et son rôle dans un mot.",
            "Vous évitez les confusions В/B, Н/H, Р/P, С/C et vous situez Е et И.",
            listOf("letter-03-v", "letter-15-n", "letter-18-r", "letter-19-s", "letter-06-e", "letter-10-i"),
            "Lookalikes + voyelles Е / И.",
        ),
        LetterGroup(
            LESSON_G3, "Consonnes utiles",
            "Lire Б Д Г Л П Ф et former des mots courts.",
            "Ces consonnes permettent déjà beaucoup de mots débutants " +
                "(папа, дом, лампа…).",
            "Vous associez six consonnes à leur nom et à un exemple.",
            listOf("letter-02-b", "letter-05-d", "letter-04-g", "letter-13-l", "letter-17-p", "letter-22-f"),
            "Consonnes du quotidien.",
        ),
        LetterGroup(
            LESSON_G4, "Voyelles particulières",
            "Distinguer Ы Й Э Ю Я Ё (et le contraste Е / Ё).",
            "Ces lettres n’ont pas toujours d’équivalent simple en français. " +
                "Ё est toujours sous l’accent ; Ь/Ъ viendront ensuite.",
            "Vous savez nommer ces voyelles et repérer Ё vs Е.",
            listOf("letter-29-y", "letter-11-j", "letter-31-e", "letter-32-yu", "letter-33-ya", "letter-07-yo"),
            "Voyelles « difficiles » pour francophones.",
        ),
        LetterGroup(
            LESSON_G5, "Chuintantes et sifflantes",
            "Reconnaître Ж Ш Щ Ч Ц З sans les fusionner.",
            "Sons souvent absents du français courant : on apprend le nom, " +
                "une approximation prudente, et un mot d’exemple.",
            "Vous distinguez notamment Ш et Щ, Ц et Ч.",
            listOf("letter-08-zh", "letter-26-sh", "letter-27-shch", "letter-25-ch", "letter-24-ts", "letter-09-z"),
            "Chuintantes / affriquées / sifflantes.",
        ),
        LetterGroup(
            LESSON_G6, "Х et signes Ъ Ь",
            "Comprendre Х, le signe mou et le signe dur.",
            "Ь et Ъ ne sont pas des voyelles : ils modifient ou séparent. " +
                "Х n’est ni un h français muet ni un x.",
            "Vous savez expliquer le rôle de Ь et Ъ et reconnaître Х.",
            listOf("letter-23-kh", "letter-30-soft", "letter-28-hard"),
            "Frottement + signes orthographiques.",
        ),
    )

    val unit: CourseUnit by lazy {
        CourseUnit(
            id = UNIT_ID,
            cefrLevel = CefrLevel.A1,
            titleFr = "Lire le cyrillique",
            descriptionFr = "Les 33 lettres en groupes progressifs, puis syllabes et mots — " +
                "pour francophones, hors ligne. Parcours de lecture, pas une certification.",
            sortOrder = 0,
            lessons = listOf(
                outline(LESSON_G1, 1, groups[0]),
                outline(LESSON_G2, 2, groups[1]),
                outline(LESSON_G3, 3, groups[2]),
                LessonOutline(
                    id = LESSON_SYLLABLES,
                    unitId = UNIT_ID,
                    titleFr = "Syllabes simples",
                    objectiveFr = "Lire des syllabes formées des lettres déjà vues (groupes 1–3).",
                    introductionFr = "On ne mémorise pas l’alphabet isolément : on lit ма, то, ну… " +
                        "uniquement avec des lettres déjà enseignées.",
                    summaryFr = "Vous lisez des syllabes CV sans indices au test final.",
                    sortOrder = 4,
                    exerciseCount = 5,
                    legacyRouteHint = "lesson/alphabet",
                ),
                outline(LESSON_G4, 5, groups[3]),
                outline(LESSON_G5, 6, groups[4]),
                LessonOutline(
                    id = LESSON_WORDS,
                    unitId = UNIT_ID,
                    titleFr = "Mots à lire",
                    objectiveFr = "Lire des mots courts dont toutes les lettres ont été présentées.",
                    introductionFr = "Chaque mot n’utilise que des lettres des groupes 1–5. " +
                        "L’aide de lecture n’est pas de l’IPA.",
                    summaryFr = "Vous associez des mots simples à leur sens.",
                    sortOrder = 7,
                    exerciseCount = 5,
                    legacyRouteHint = "lesson/alphabet",
                ),
                outline(LESSON_G6, 8, groups[5]),
                LessonOutline(
                    id = LESSON_FINAL,
                    unitId = UNIT_ID,
                    titleFr = "Bilan lecture cyrillique",
                    objectiveFr = "Réviser lookalikes, Ё/Е, signes, et lire sans indices.",
                    introductionFr = "Test de consolidation : reconnaissance et lecture. " +
                        "Ce n’est pas un examen CECRL.",
                    summaryFr = "Vous avez parcouru les 33 lettres et de premières lectures.",
                    sortOrder = 9,
                    exerciseCount = 6,
                    legacyRouteHint = "lesson/alphabet",
                ),
            ),
        )
    }

    fun allLessonIds(): List<String> = unit.lessons.map { it.id }

    fun letterIdsTaughtUpTo(lessonId: String): Set<String> {
        val ordered = listOf(
            LESSON_G1, LESSON_G2, LESSON_G3, LESSON_SYLLABLES,
            LESSON_G4, LESSON_G5, LESSON_WORDS, LESSON_G6, LESSON_FINAL,
        )
        val idx = ordered.indexOf(lessonId)
        if (idx < 0) return emptySet()
        val groupLessons = ordered.take(idx + 1).toSet()
        return groups.filter { it.lessonId in groupLessons }.flatMap { it.letterIds }.toSet()
    }

    fun lettersForLesson(lessonId: String): List<AlphabetLetter> {
        val group = groups.find { it.lessonId == lessonId } ?: return emptyList()
        return group.letterIds.mapNotNull { AlphabetLettersSeed.byId(it) }
    }

    fun teachablesFor(lessonId: String): List<TeachableItem> = when (lessonId) {
        in groups.map { it.lessonId } -> lettersForLesson(lessonId).map { letterToTeachable(it) }
        LESSON_SYLLABLES -> syllables.map { readingToTeachable(it) }
        LESSON_WORDS -> words.map { readingToTeachable(it) }
        LESSON_FINAL -> {
            listOf("letter-03-v", "letter-15-n", "letter-18-r", "letter-07-yo", "letter-30-soft", "letter-28-hard")
                .mapNotNull { AlphabetLettersSeed.byId(it) }
                .map { letterToTeachable(it) } +
                words.take(2).map { readingToTeachable(it) }
        }
        else -> emptyList()
    }

    fun exercisesFor(lessonId: String): List<InteractiveExercise> = when (lessonId) {
        in groups.map { it.lessonId } -> groupExercises(lessonId)
        LESSON_SYLLABLES -> syllableExercises()
        LESSON_WORDS -> wordExercises()
        LESSON_FINAL -> finalExercises()
        else -> emptyList()
    }

    /** Toutes les lettres apparaissent dans exactement un groupe. */
    fun assertPartitionCovers33(): Boolean {
        val ids = groups.flatMap { it.letterIds }
        return ids.size == 33 && ids.toSet().size == 33 &&
            ids.toSet() == AlphabetLettersSeed.letters.map { it.id }.toSet()
    }

    val syllables: List<CyrillicReadingItem> by lazy {
        listOf(
            reading("syl-ma", "ма", "syllabe ма", CyrillicReadingKind.Syllable, "ma", "letter-14-m", "letter-01-a"),
            reading("syl-mo", "мо", "syllabe мо", CyrillicReadingKind.Syllable, "mo", "letter-14-m", "letter-16-o"),
            reading("syl-mu", "му", "syllabe му", CyrillicReadingKind.Syllable, "mu", "letter-14-m", "letter-21-u"),
            reading("syl-ta", "та", "syllabe та", CyrillicReadingKind.Syllable, "ta", "letter-20-t", "letter-01-a"),
            reading("syl-to", "то", "syllabe то", CyrillicReadingKind.Syllable, "to", "letter-20-t", "letter-16-o"),
            reading("syl-ka", "ка", "syllabe ка", CyrillicReadingKind.Syllable, "ka", "letter-12-k", "letter-01-a"),
            reading("syl-na", "на", "syllabe на", CyrillicReadingKind.Syllable, "na", "letter-15-n", "letter-01-a"),
            reading("syl-ne", "не", "syllabe не", CyrillicReadingKind.Syllable, "ne", "letter-15-n", "letter-06-e"),
            reading("syl-ra", "ра", "syllabe ра", CyrillicReadingKind.Syllable, "ra", "letter-18-r", "letter-01-a"),
            reading("syl-si", "си", "syllabe си", CyrillicReadingKind.Syllable, "si", "letter-19-s", "letter-10-i"),
            reading("syl-da", "да", "syllabe да", CyrillicReadingKind.Syllable, "da", "letter-05-d", "letter-01-a"),
            reading("syl-pa", "па", "syllabe па", CyrillicReadingKind.Syllable, "pa", "letter-17-p", "letter-01-a"),
        )
    }

    val words: List<CyrillicReadingItem> by lazy {
        listOf(
            reading("w-mama", "мама", "maman", CyrillicReadingKind.Word, "máma", "letter-14-m", "letter-01-a"),
            reading("w-papa", "папа", "papa", CyrillicReadingKind.Word, "pápa", "letter-17-p", "letter-01-a"),
            reading(
                "w-dom", "дом", "maison", CyrillicReadingKind.Word, "dom",
                "letter-05-d", "letter-16-o", "letter-14-m",
            ),
            reading(
                "w-kot", "кот", "chat (mâle)", CyrillicReadingKind.Word, "kot",
                "letter-12-k", "letter-16-o", "letter-20-t",
            ),
            reading(
                "w-mir", "мир", "monde / paix", CyrillicReadingKind.Word, "mir",
                "letter-14-m", "letter-10-i", "letter-18-r",
            ),
            reading(
                "w-ruka", "рука", "main / bras", CyrillicReadingKind.Word, "ruká",
                "letter-18-r", "letter-21-u", "letter-12-k", "letter-01-a",
            ),
            reading(
                "w-son", "сын", "fils", CyrillicReadingKind.Word, "syn",
                "letter-19-s", "letter-29-y", "letter-15-n",
                note = "ы n’a pas d’équivalent stable en français.",
            ),
            reading(
                "w-eto", "это", "ceci / c’est", CyrillicReadingKind.Word, "éto",
                "letter-31-e", "letter-20-t", "letter-16-o",
            ),
            reading(
                "w-chay", "чай", "thé", CyrillicReadingKind.Word, "chay",
                "letter-25-ch", "letter-01-a", "letter-11-j",
            ),
            reading(
                "w-zhuk", "жук", "scarabée", CyrillicReadingKind.Word, "zhuk",
                "letter-08-zh", "letter-21-u", "letter-12-k",
            ),
        )
    }

    private fun reading(
        id: String,
        ru: String,
        fr: String,
        kind: CyrillicReadingKind,
        aid: String,
        vararg letterIds: String,
        note: String? = null,
    ) = CyrillicReadingItem(
        id = id,
        textRu = ru,
        translationFr = fr,
        kind = kind,
        requiredLetterIds = letterIds.toSet(),
        readingAid = aid,
        noteFr = note,
    )

    private fun outline(id: String, order: Int, g: LetterGroup) = LessonOutline(
        id = id,
        unitId = UNIT_ID,
        titleFr = g.titleFr,
        objectiveFr = g.objectiveFr,
        introductionFr = g.introFr + " Focus : ${g.focusFr}",
        summaryFr = g.summaryFr,
        sortOrder = order,
        exerciseCount = 5,
        legacyRouteHint = "lesson/alphabet",
    )

    private fun letterToTeachable(letter: AlphabetLetter): TeachableItem {
        val form = "${letter.uppercase} ${letter.lowercase}"
        val nameAid = RussianPronunciation.lookupOrTransliterate(letter.nameRu)
        val exampleAid = letter.exampleReadingAid
            ?: RussianPronunciation.lookupOrTransliterate(letter.exampleWordRu).transliteration
        return TeachableItem(
            id = "teach-${letter.id}",
            textRu = form,
            translationFr = "lettre « ${letter.nameRu} »",
            transliteration = "nom : ${nameAid.transliteration} · ex. $exampleAid",
            stressUnknown = nameAid.stressUnknown,
            usageFr = letter.pronunciationGuideFr,
            exampleRu = letter.exampleWordRu,
            exampleFr = letter.exampleWordFr,
            audioAssetPath = letter.audioAssetPath,
            register = SpeechRegister.Neutral,
            confusionFr = letter.confusionFr,
            grammarNoteFr = "Nom de la lettre : « ${letter.nameRu} ». " +
                "Dans les mots : ${letter.soundInWordsFr}",
        )
    }

    private fun readingToTeachable(item: CyrillicReadingItem): TeachableItem =
        TeachableItem(
            id = "teach-${item.id}",
            textRu = item.textRu,
            translationFr = item.translationFr,
            transliteration = item.readingAid,
            stressUnknown = item.kind == CyrillicReadingKind.Syllable,
            usageFr = when (item.kind) {
                CyrillicReadingKind.Syllable -> "Syllabe de lecture — pas un mot autonome."
                CyrillicReadingKind.Word -> "Mot simple à lire à voix haute (aide de lecture ≠ IPA)."
            },
            exampleRu = item.textRu,
            exampleFr = item.translationFr,
            audioAssetPath = null,
            confusionFr = item.noteFr,
            grammarNoteFr = null,
        )

    private fun groupExercises(lessonId: String): List<InteractiveExercise> {
        val letters = lettersForLesson(lessonId)
        require(letters.size >= 3)
        val out = mutableListOf<InteractiveExercise>()
        // Guided: recognize first letter name
        val a = letters[0]
        out += ex(
            id = "$lessonId-ex-01",
            type = InteractiveExerciseType.MatchTranslation,
            order = 1,
            stage = PedagogyStage.Guided,
            prompt = "Quel est le nom russe de la lettre ${a.uppercase} ?",
            stimulus = "${a.uppercase} ${a.lowercase}",
            choices = nameChoices(a.nameRu),
            accepted = listOf(a.nameRu),
            explanation = "${a.uppercase} s’appelle « ${a.nameRu} ». ${a.confusionFr.orEmpty()}".trim(),
            translation = "lettre « ${a.nameRu} »",
            translit = a.exampleReadingAid,
        )
        // Guided: lowercase recognition
        val b = letters[1]
        out += ex(
            id = "$lessonId-ex-02",
            type = InteractiveExerciseType.MultipleChoice,
            order = 2,
            stage = PedagogyStage.Guided,
            prompt = "Quelle est la minuscule de ${b.uppercase} ?",
            stimulus = b.uppercase,
            choices = listOf(b.lowercase) + letters.drop(2).take(3).map { it.lowercase },
            accepted = listOf(b.lowercase),
            explanation = "${b.uppercase} / ${b.lowercase} — même lettre, deux casse.",
        )
        // Active: lookalike or sound
        val c = letters.getOrElse(2) { letters.last() }
        val lookalikePrompt = c.confusionFr?.let {
            "Attention confusion : que lit-on pour ${c.uppercase} ?"
        } ?: "Quel mot d’exemple pour ${c.uppercase} ?"
        val lookalikeChoices = if (c.confusionFr != null) {
            listOf(
                c.nameRu,
                "comme le latin",
                "muet",
                letters.first { it.id != c.id }.nameRu,
            )
        } else {
            listOf(c.exampleWordRu) + letters.filter { it.id != c.id }.take(3).map { it.exampleWordRu }
        }
        val lookalikeAccepted = if (c.confusionFr != null) listOf(c.nameRu) else listOf(c.exampleWordRu)
        out += ex(
            id = "$lessonId-ex-03",
            type = InteractiveExerciseType.MultipleChoice,
            order = 3,
            stage = PedagogyStage.Active,
            prompt = lookalikePrompt,
            stimulus = "${c.uppercase} ${c.lowercase}",
            choices = lookalikeChoices.distinct().take(4),
            accepted = lookalikeAccepted,
            explanation = c.confusionFr ?: "${c.exampleWordRu} = ${c.exampleWordFr}.",
            translation = c.exampleWordFr,
            translit = c.exampleReadingAid,
        )
        // Active: example word meaning
        val d = letters.getOrElse(3) { letters[0] }
        out += ex(
            id = "$lessonId-ex-04",
            type = InteractiveExerciseType.MatchTranslation,
            order = 4,
            stage = PedagogyStage.Active,
            prompt = "Que signifie le mot d’exemple « ${d.exampleWordRu} » ?",
            stimulus = d.exampleWordRu,
            choices = listOf(d.exampleWordFr) +
                letters.filter { it.id != d.id }.map { it.exampleWordFr }.distinct().take(3),
            accepted = listOf(d.exampleWordFr) +
                d.exampleWordFr.substringBefore("/").trim().let { listOf(it) },
            explanation = "${d.exampleWordRu} = ${d.exampleWordFr}. Aide de lecture : ${d.exampleReadingAid}.",
            translation = d.exampleWordFr,
            translit = d.exampleReadingAid,
        )
        // Final
        val e = letters.last()
        out += ex(
            id = "$lessonId-ex-05",
            type = InteractiveExerciseType.LetterRecognition,
            order = 5,
            stage = PedagogyStage.Final,
            prompt = "Sans indice : nom russe de ${e.uppercase} ?",
            stimulus = e.uppercase,
            choices = nameChoices(e.nameRu),
            accepted = listOf(e.nameRu),
            explanation = "Révision : ${e.uppercase}/${e.lowercase} = « ${e.nameRu} ».",
        )
        return out
    }

    private fun syllableExercises(): List<InteractiveExercise> {
        val taught = letterIdsTaughtUpTo(LESSON_SYLLABLES)
        val usable = syllables.filter { it.requiredLetterIds.all { id -> id in taught } }
        require(usable.size >= 4) { "Pas assez de syllabes avec prérequis groupes 1–3" }
        return usable.take(5).mapIndexed { index, syl ->
            val stage = when (index) {
                0, 1 -> PedagogyStage.Guided
                2, 3 -> PedagogyStage.Active
                else -> PedagogyStage.Final
            }
            ex(
                id = "${LESSON_SYLLABLES}-ex-${index + 1}",
                type = InteractiveExerciseType.MultipleChoice,
                order = index + 1,
                stage = stage,
                prompt = if (stage == PedagogyStage.Final) {
                    "Lisez (sans aide) : quelle syllabe est écrite ?"
                } else {
                    "Quelle est l’aide de lecture pour « ${syl.textRu} » ?"
                },
                stimulus = syl.textRu,
                choices = if (stage == PedagogyStage.Final) {
                    listOf(syl.textRu) + usable.filter { it.id != syl.id }.take(3).map { it.textRu }
                } else {
                    listOf(syl.readingAid) + usable.filter { it.id != syl.id }.take(3).map { it.readingAid }
                },
                accepted = if (stage == PedagogyStage.Final) listOf(syl.textRu) else listOf(syl.readingAid),
                explanation = "« ${syl.textRu} » — aide de lecture « ${syl.readingAid} » (pas de l’IPA).",
                translation = syl.translationFr,
                translit = syl.readingAid,
            )
        }
    }

    private fun wordExercises(): List<InteractiveExercise> {
        val taught = letterIdsTaughtUpTo(LESSON_WORDS)
        val usable = words.filter { it.requiredLetterIds.all { id -> id in taught } }
        require(usable.size >= 4) { "Mots sans lettres enseignées" }
        return usable.take(5).mapIndexed { index, w ->
            val stage = when (index) {
                0, 1 -> PedagogyStage.Guided
                2, 3 -> PedagogyStage.Active
                else -> PedagogyStage.Final
            }
            ex(
                id = "${LESSON_WORDS}-ex-${index + 1}",
                type = InteractiveExerciseType.MatchTranslation,
                order = index + 1,
                stage = stage,
                prompt = "Que signifie « ${w.textRu} » ?",
                stimulus = w.textRu,
                choices = listOf(w.translationFr) +
                    usable.filter { it.id != w.id }.map { it.translationFr }.distinct().take(3),
                accepted = listOf(w.translationFr) +
                    w.translationFr.substringBefore("/").trim().let { listOf(it) },
                explanation = listOfNotNull(
                    "${w.textRu} = ${w.translationFr}.",
                    "Aide de lecture : ${w.readingAid}.",
                    w.noteFr,
                ).joinToString(" "),
                translation = w.translationFr,
                translit = w.readingAid,
            )
        }
    }

    private fun finalExercises(): List<InteractiveExercise> {
        val v = AlphabetLettersSeed.byId("letter-03-v")!!
        val n = AlphabetLettersSeed.byId("letter-15-n")!!
        val yo = AlphabetLettersSeed.byId("letter-07-yo")!!
        val soft = AlphabetLettersSeed.byId("letter-30-soft")!!
        val hard = AlphabetLettersSeed.byId("letter-28-hard")!!
        val mama = words.first { it.id == "w-mama" }
        return listOf(
            ex(
                "${LESSON_FINAL}-ex-01", InteractiveExerciseType.MultipleChoice, 1, PedagogyStage.Guided,
                "В ressemble au B latin. Que lit-on ?",
                "В в", listOf("вэ (≈ v)", "бэ (≈ b)", "гэ", "эр"),
                listOf("вэ (≈ v)", "вэ"),
                "В = вэ — lookalike du B, son différent.",
            ),
            ex(
                "${LESSON_FINAL}-ex-02", InteractiveExerciseType.MultipleChoice, 2, PedagogyStage.Guided,
                "Н ressemble au H latin. Nom russe ?",
                "Н н", nameChoices(n.nameRu), listOf(n.nameRu),
                "Н = эн — jamais « h ».",
            ),
            ex(
                "${LESSON_FINAL}-ex-03", InteractiveExerciseType.MultipleChoice, 3, PedagogyStage.Active,
                "Quelle lettre est toujours sous l’accent ?",
                "…", listOf("Ё", "Е", "Э", "Ы"), listOf("Ё", "ё"),
                "Ё est toujours accentuée en russe standard.",
            ),
            ex(
                "${LESSON_FINAL}-ex-04", InteractiveExerciseType.MatchTranslation, 4, PedagogyStage.Active,
                "Que fait le signe mou Ь ?",
                "Ь ь",
                listOf(
                    "palatalise surtout la consonne précédente",
                    "c’est une voyelle « i »",
                    "c’est un h aspiré",
                    "marque l’accent tonique",
                ),
                listOf("palatalise surtout la consonne précédente"),
                soft.soundInWordsFr,
            ),
            ex(
                "${LESSON_FINAL}-ex-05", InteractiveExerciseType.MatchTranslation, 5, PedagogyStage.Active,
                "Que fait le signe dur Ъ ?",
                "Ъ ъ",
                listOf(
                    "sépare / bloque la palatalisation",
                    "c’est la lettre « o »",
                    "c’est un accent écrit",
                    "muet comme le h français",
                ),
                listOf("sépare / bloque la palatalisation"),
                hard.soundInWordsFr,
            ),
            ex(
                "${LESSON_FINAL}-ex-06", InteractiveExerciseType.MatchTranslation, 6, PedagogyStage.Final,
                "Lisez et traduisez :",
                mama.textRu,
                listOf(mama.translationFr, "papa", "maison", "thé"),
                listOf(mama.translationFr, "maman"),
                "мама = maman. Aide de lecture : ${mama.readingAid}.",
                translation = mama.translationFr,
                translit = mama.readingAid,
            ),
        )
    }

    private fun nameChoices(correct: String): List<String> {
        val pool = AlphabetLettersSeed.letters.map { it.nameRu }.distinct()
        return (listOf(correct) + pool.filter { it != correct }.shuffled()).distinct().take(4)
    }

    private fun ex(
        id: String,
        type: InteractiveExerciseType,
        order: Int,
        stage: PedagogyStage,
        prompt: String,
        stimulus: String,
        choices: List<String>,
        accepted: List<String>,
        explanation: String,
        translation: String? = null,
        translit: String? = null,
    ) = InteractiveExercise(
        id = id,
        type = type,
        promptFr = prompt,
        stimulus = stimulus,
        choices = choices,
        acceptedAnswers = accepted,
        explanationFr = explanation,
        sortOrder = order,
        pedagogyStage = stage,
        translationFr = translation,
        transliteration = translit,
        audioAssetPath = null,
    )
}
