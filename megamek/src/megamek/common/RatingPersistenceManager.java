package megamek.common;

import java.io.*;
import java.util.*;

/**
 * Gestionnaire de persistance pour les ratings des joueurs.
 * La classe permet de sauvegarder et charger les ratings des joueurs
 * entre les différentes sessions de jeu, assurant ainsi que les scores
 * Rating sont conservés même après la fermeture du serveur.
 */

public class RatingPersistenceManager {
    private static final String RATINGS_FILE = "data/player_ratings.dat";
    private Map<Integer, PlayerRating> playerRatings = new HashMap<>();
    private static RatingPersistenceManager instance;

    // Singleton pattern pour s'assurer qu'il y a juste une instance qui gère les ratings.
    private RatingPersistenceManager() {
        loadRatings();
    }

    public static synchronized RatingPersistenceManager getInstance() {
        if (instance == null) {
            instance = new RatingPersistenceManager();
        }
        return instance;
    }

    /**
     * Charge les ratings des joueurs depuis le fichier
     */
    @SuppressWarnings("unchecked")
    private void loadRatings() {
        File file = new File(RATINGS_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                playerRatings = (Map<Integer, PlayerRating>) ois.readObject();
                System.out.println("Ratings chargés pour " + playerRatings.size() + " joueurs.");
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Erreur lors du chargement des ratings: " + e.getMessage());
                e.printStackTrace();
                playerRatings = new HashMap<>();
            }
        } else {
            System.out.println("Aucun fichier de ratings trouvé. Création d'une nouvelle base de ratings.");
            playerRatings = new HashMap<>();
        }
    }

    /**
     * Sauvegarde les ratings des joueurs dans le fichier
     */
    public void saveRatings() {
        try {
            File directory = new File("data");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Sauvegarder les données
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(RATINGS_FILE))) {
                oos.writeObject(playerRatings);
                System.out.println("Ratings sauvegardés pour " + playerRatings.size() + " joueurs.");
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde des ratings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Obtient le rating d'un joueur. Crée un nouveau rating si nécessaire.
     */
    public PlayerRating getPlayerRating(int playerId, Player player) {
        PlayerRating rating = playerRatings.get(playerId);
        if (rating == null) {
            rating = new PlayerRating(playerId, player.getName(), player.isBot());
            playerRatings.put(playerId, rating);
        }
        return rating;
    }

    /**
     * Met à jour le rating d'un joueur
     */
    public void updatePlayerRating(PlayerRating rating) {
        playerRatings.put(rating.getPlayerId(), rating);
        // Sauvegarde après chaque mise à jour
        saveRatings();
    }
}