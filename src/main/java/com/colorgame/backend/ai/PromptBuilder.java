// This tells the computer where this file lives in the project's folder structure
package com.colorgame.backend.ai;

// Brings in the List tool so we can accept a list of weaknesses in the new coaching method
import java.util.List;

import org.springframework.stereotype.Component;

// @Component tells the app: "This file is a Worker Bee — a reusable tool other services can inject and call."
// Unlike @Service, @Component is the generic label used for helper classes that aren't themselves
// the "main" business logic — here, PromptBuilder's only job is building text, not making decisions.
@Component
public class PromptBuilder {

    // A fixed list of the only color names Gemini is allowed to use when generating a grid.
    // Keeping this as a constant means both the prompt text AND any future validation code
    // stay in sync — one single source of truth for "what counts as a valid color".
    private static final String[] AVAILABLE_COLORS = {
            "red", "blue", "green", "yellow", "orange", "purple", "pink", "cyan"
    };

    /**
     * Construit le prompt envoyé à Gemini pour générer un pattern de grille.
     *
     * PILIER 1 : builds the instructions sent to Gemini asking it to invent a colored grid
     * pattern for the player to memorize.
     */
    public String buildPatternGenerationPrompt(String gridSize, String difficulty) {
        // Break "3x3" into separate row and column numbers using the shared helper below
        int[] dimensions = parseGridSize(gridSize);
        int rows = dimensions[0];     // How many rows the grid should have
        int cols = dimensions[1];     // How many columns the grid should have

        // A Java "text block" (triple-quoted string) — lets us write multi-line prompt text
        // without needing \n everywhere. .formatted(...) fills in the %d / %s placeholders in order.
        return """
                Tu es un générateur de patterns pour un jeu de mémoire visuelle base sur les couleurs.

                Génère une grille de %d lignes sur %d colonnes, où chaque cellule contient une couleur.
                Couleurs disponibles UNIQUEMENT : %s

                Niveau de difficulté : %s
                - BEGINNER : utilise une structure simple et facile à mémoriser (répétitions, symétrie claire)
                - INTERMEDIATE : structure modérément complexe (dégradé partiel, quelques clusters)
                - ADVANCED : structure plus complexe (moins de répétitions évidentes, plus de couleurs différentes)

                Le pattern doit avoir une structure intentionnelle (dégradé, symétrie, ou clusters de couleurs) — pas un simple tirage aléatoire.

                Réponds UNIQUEMENT avec un objet JSON valide, sans aucun texte avant ou après, au format exact suivant :
                {
                  "grid": [["couleur", "couleur", ...], ["couleur", "couleur", ...], ...],
                  "structureType": "gradient|symmetry|clusters"
                }

                La grille doit avoir exactement %d lignes et chaque ligne exactement %d colonnes.
                """.formatted(
                        rows,                                   // 1st %d — number of rows to generate
                        cols,                                    // 2nd %d — number of columns to generate
                        String.join(", ", AVAILABLE_COLORS),     // %s — the allowed color list, joined into "red, blue, green, ..."
                        difficulty,                              // %s — BEGINNER / INTERMEDIATE / ADVANCED, controls pattern complexity
                        rows,                                    // 3rd %d — repeats row count in the "must have exactly" reminder
                        cols                                     // 4th %d — repeats column count in the "must have exactly" reminder
                );
    }

    /**
     * Construit le prompt envoyé à Gemini pour évaluer la tentative du joueur.
     *
     * PILIER 1 : builds the instructions sent to Gemini asking it to grade the player's
     * recreated grid against the original, cell by cell.
     */
    public String buildScoringPrompt(String targetGridJson, String playerGridJson) {
        return """
                Tu es un évaluateur pour un jeu de mémoire visuelle basé sur les couleurs.

                Grille originale (ce que le joueur devait mémoriser) — couleurs nommées parmi : %s
                %s

                Grille soumise par le joueur (sa tentative de recréation) — chaque cellule est un code
                couleur hexadécimal (ex: "#e63946"), choisi librement par le joueur sur une roue chromatique continue :
                %s

                Compare les deux grilles cellule par cellule. Pour chaque cellule, convertis mentalement
                le code hexadécimal du joueur vers la teinte (hue) la plus proche, puis détermine :
                - Une correspondance EXACTE si la teinte du joueur est très proche de la couleur nommée
                  originale (écart de teinte faible, perceptuellement "la même couleur")
                - Une correspondance de FAMILLE si la couleur est visuellement proche mais pas identique
                  (ex: le joueur a choisi un bleu-cyan alors que l'original était "blue", ou un rose-rouge
                  alors que l'original était "red")
                - Aucune correspondance si la teinte est clairement différente

                Sois tolérant sur la luminosité et la saturation du hex (un rouge foncé ou un rouge clair
                comptent comme "red") — juge uniquement sur la teinte dominante perçue.

                Calcule un score global de 0 à 100 (100 = grille parfaitement recréée).
                Identifie l'erreur dominante commise par le joueur (ex: "corner_blindspot" si les erreurs sont concentrées dans les coins,
                "diagonal_confusion", "color_family_confusion", "center_blindspot", ou "none" si le score est excellent).
                Rédige un verdict court (1-2 phrases, en français, ton encourageant mais honnête) commentant la performance.

                Réponds UNIQUEMENT avec un objet JSON valide, sans aucun texte avant ou après, au format exact suivant :
                {
                        "score": 85,
                        "dominantMistake": "corner_blindspot",
                        "verdict": "Très bonne mémorisation du centre, attention aux coins la prochaine fois.",
                        "cellResults": [[{"exactMatch": true, "familyMatch": false}, ...], ...]
                }
                """.formatted(
                        String.join(", ", AVAILABLE_COLORS), // %s — reminds Gemini what the named palette is
                        targetGridJson,                        // %s — the answer key, still color names
                        playerGridJson                         // %s — the player's attempt, now hex codes
                );
        }

    /**
     * Construit le prompt envoyé à Gemini pour générer un message de coaching personnalisé.
     *
     * PILIER 3 (Half B) : builds the instructions sent to Gemini asking it to turn a player's
     * raw stats (games played, average score, recurring weaknesses) into a short, warm,
     * encouraging coaching message — the one part of Pilier 3 that genuinely needs an LLM,
     * since a rule-based system can't write natural, personalized encouragement.
     */
    public String buildCoachingPrompt(int totalGames, double averageScore, List<String> weaknesses) {
        // If the player has no recurring weaknesses yet, say so in plain French instead of
        // handing Gemini an empty list — an empty list in the prompt text would read oddly
        // and might tempt the model to invent a weakness that isn't real.
        String weaknessText = weaknesses.isEmpty()
                ? "aucune faiblesse récurrente détectée pour l'instant"
                : String.join(", ", weaknesses); // Turns ["corner_blindspot", "diagonal_confusion"] into "corner_blindspot, diagonal_confusion"

        // The "focusArea" value Gemini must echo back in its JSON response.
        // We decide this ourselves (first weakness in our own detected list, or "none")
        // rather than letting Gemini invent it — this keeps the field trustworthy and
        // consistent with what ProfileService.detectWeaknesses() actually calculated.
        String focusAreaPlaceholder = weaknesses.isEmpty() ? "none" : weaknesses.get(0);

        return """
                Tu es un coach bienveillant pour un jeu de mémoire de couleurs ("Color Game").
                Voici les statistiques réelles d'un joueur :
                - Nombre total de parties jouées : %d
                - Score moyen : %.2f / 100
                - Faiblesses récurrentes détectées : %s

                Écris un court message de coaching (2-4 phrases), chaleureux et encourageant,
                qui commente sa progression et donne UN conseil concret lié à sa principale faiblesse
                (s'il y en a une). Ne mentionne pas de faiblesse si la liste est vide — dans ce cas,
                encourage-le simplement à continuer.

                Réponds UNIQUEMENT en JSON valide, sans aucun texte autour, sous cette forme exacte :
                {
                  "message": "...",
                  "focusArea": "%s"
                }
                """.formatted(
                        totalGames,             // 1st %d — total rounds played, gives Gemini a sense of experience level
                        averageScore,            // %.2f — lifetime average score, rounded to 2 decimals in the prompt text
                        weaknessText,             // %s — either the real weakness list or the "none detected yet" fallback sentence
                        focusAreaPlaceholder      // 2nd %s — the exact focusArea value Gemini must copy into its JSON reply
                );
    }

    // HELPER (shared by other services too, e.g. GameService.startRound): splits a grid size
    // string like "4x4" into its two numeric parts.
    public static int[] parseGridSize(String gridSize) {
        // "3x3" -> [3, 3]
        String[] parts = gridSize.toLowerCase().split("x"); // Lowercase first so "3X3" and "3x3" both work, then split on the "x"
        int rows = Integer.parseInt(parts[0].trim());        // Convert the text before the "x" into a number, trimming stray spaces
        int cols = Integer.parseInt(parts[1].trim());        // Convert the text after the "x" into a number, trimming stray spaces
        return new int[]{rows, cols};                        // Package both numbers into a small 2-item array to return
    }
}